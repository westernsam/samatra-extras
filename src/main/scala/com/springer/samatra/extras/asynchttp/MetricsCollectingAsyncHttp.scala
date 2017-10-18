package com.springer.samatra.extras.asynchttp

import java.util.concurrent.atomic.AtomicReference

import com.springer.samatra.extras.metrics.MetricsStatsdClient
import org.asynchttpclient._


class MetricsCollectingAsyncHttp(underlying: AsyncHttpClient, statsd: MetricsStatsdClient, dependencyNamingStrategy: Request => String = r => r.getUri.getHost + "-" + r.getUri.getPath.replace("/", "_"), disableEncodingInBoundedRequest : Boolean = false) extends AsyncHttpClient {
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

  private def requestBuilder(method: String, url: String): BoundRequestBuilder = new BoundRequestBuilder(self, method, disableEncodingInBoundedRequest) //sorry!
    .setUrl(url).setSignatureCalculator(signatureCalculatorRef.get())

  private def requestBuilder(prototype: Request): BoundRequestBuilder = new BoundRequestBuilder(this, prototype).setSignatureCalculator(signatureCalculatorRef.get)

}

class TimerAsyncHandler[T](statsD: MetricsStatsdClient, metricName: String, delegate: AsyncHandler[T], private val startTime: Long, dependencyNamingStrategy: Request => String) extends AsyncHandler[T] {
  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): AsyncHandler.State = delegate.onBodyPartReceived(bodyPart)
  override def onHeadersReceived(headers: HttpResponseHeaders): AsyncHandler.State = delegate.onHeadersReceived(headers)

  override def onStatusReceived(responseStatus: HttpResponseStatus): AsyncHandler.State = {
    delegate.onStatusReceived(responseStatus)
  }

  override def onCompleted(): T = {
    statsD.recordExecutionTime(s"dependency.responsetime.$metricName", System.currentTimeMillis() - startTime)
    delegate.onCompleted()
  }
  override def onThrowable(t: Throwable): Unit = delegate.onThrowable(t)
}
