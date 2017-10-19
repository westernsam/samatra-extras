package com.springer.samatra.extras.asynchttp

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import com.springer.samatra.extras.metrics.{MetricsHandler, MetricsStatsdClient}
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClientConfig, _}

object MetricsCollectingAsyncHttp {
  val defaultMetricNamer: Request => String = r => r.getUri.getHost.split('.').head + "_" + r.getUri.getPath.replace("/", "_")

  def apply(underlying: AsyncHttpClient, statsd: MetricsStatsdClient, dependencyNamingStrategy: Request => String, disableEncodingInBoundedRequest: Boolean): AsyncHttpClient =
    new MetricsCollectingAsyncHttp(underlying, statsd, dependencyNamingStrategy, disableEncodingInBoundedRequest)

  def apply(builder: DefaultAsyncHttpClientConfig.Builder, statsd: MetricsStatsdClient, dependencyNamingStrategy: Request => String = defaultMetricNamer): AsyncHttpClient = {
    val config = builder.build
    new MetricsCollectingAsyncHttp(new DefaultAsyncHttpClient(config), statsd, dependencyNamingStrategy, config.isDisableUrlEncodingForBoundRequests)
  }
}

class TimerAsyncHandler[T](statsD: MetricsStatsdClient, metricName: String, delegate: AsyncHandler[T], private val startTime: Long, dependencyNamingStrategy: Request => String) extends AsyncHandler[T] {

  val statusReceived = new AtomicInteger(200)

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): AsyncHandler.State = delegate.onBodyPartReceived(bodyPart)
  override def onHeadersReceived(headers: HttpResponseHeaders): AsyncHandler.State = delegate.onHeadersReceived(headers)

  override def onStatusReceived(responseStatus: HttpResponseStatus): AsyncHandler.State = {
    statusReceived.getAndSet(responseStatus.getStatusCode)
    delegate.onStatusReceived(responseStatus)
  }

  override def onCompleted(): T = {
    statsD.incrementCounter(s"dependency.$metricName.responses.${MetricsHandler.responseCode(statusReceived.get)}")
    statsD.incrementCounter(s"dependency.$metricName")
    statsD.recordExecutionTime(s"dependency.responsetime.$metricName", System.currentTimeMillis() - startTime)
    delegate.onCompleted()
  }
  override def onThrowable(t: Throwable): Unit = delegate.onThrowable(t)
}

class MetricsCollectingAsyncHttp(underlying: AsyncHttpClient, statsd: MetricsStatsdClient, dependencyNamingStrategy: Request => String, disableEncodingInBoundedRequest: Boolean) extends AsyncHttpClient {
  self =>

  val signatureCalculatorRef: AtomicReference[SignatureCalculator] = new AtomicReference[SignatureCalculator]()

  override def preparePatch(url: String): BoundRequestBuilder = requestBuilder("PATCH", url)
  override def preparePost(url: String): BoundRequestBuilder = requestBuilder("POST", url)
  override def prepareDelete(url: String): BoundRequestBuilder = requestBuilder("DELETE", url)
  override def prepareGet(url: String): BoundRequestBuilder = requestBuilder("GET", url)
  override def preparePut(url: String): BoundRequestBuilder = requestBuilder("PUT", url)
  override def prepareConnect(url: String): BoundRequestBuilder = requestBuilder("CONNECT", url)
  override def prepareHead(url: String): BoundRequestBuilder = requestBuilder("HEAD", url)
  override def prepareOptions(url: String): BoundRequestBuilder = requestBuilder("OPTIONS", url)
  override def prepareTrace(url: String): BoundRequestBuilder = requestBuilder("TRACE", url)

  override def prepareRequest(request: Request): BoundRequestBuilder = requestBuilder(request)
  override def prepareRequest(reqBuilder: RequestBuilder): BoundRequestBuilder = requestBuilder(reqBuilder.build())

  override def close(): Unit = underlying.close()
  override def isClosed: Boolean = underlying.isClosed

  override def setSignatureCalculator(signatureCalculator: SignatureCalculator): AsyncHttpClient = {
    signatureCalculatorRef.getAndSet(signatureCalculator)
    underlying.setSignatureCalculator(signatureCalculator)
    self
  }

  override def executeRequest(requestBuilder: RequestBuilder): ListenableFuture[Response] = executeRequest(requestBuilder.build())
  override def executeRequest[T](requestBuilder: RequestBuilder, handler: AsyncHandler[T]): ListenableFuture[T] = executeRequest(requestBuilder.build(), handler)
  override def executeRequest(request: Request): ListenableFuture[Response] = executeRequest(request, new AsyncCompletionHandlerBase())
  override def executeRequest[T](request: Request, handler: AsyncHandler[T]): ListenableFuture[T] = underlying.executeRequest(request, new TimerAsyncHandler[T](statsd, dependencyNamingStrategy(request), handler, startTime = System.currentTimeMillis(), dependencyNamingStrategy))

  private def requestBuilder(method: String, url: String): BoundRequestBuilder = new BoundRequestBuilder(self, method, disableEncodingInBoundedRequest)
    .setUrl(url).setSignatureCalculator(signatureCalculatorRef.get())

  private def requestBuilder(prototype: Request): BoundRequestBuilder = new BoundRequestBuilder(this, prototype).setSignatureCalculator(signatureCalculatorRef.get)

}
