package com.springer.samatra.extras.asynchttp

import com.springer.samatra.extras.metrics.MetricsStatsdClient
import org.asynchttpclient._

class MetricsCollectingAsyncHttp(underlying: AsyncHttpClient, statsd: MetricsStatsdClient, dependencyNamingStrategy : Request => String = r => r.getUri.getHost + "-" + r.getUri.getPath.replace("/", "_")) extends AsyncHttpClient {
  override def preparePatch(url: String): BoundRequestBuilder = underlying.preparePatch(url)
  override def preparePost(url: String): BoundRequestBuilder = underlying.preparePost(url)
  override def prepareDelete(url: String): BoundRequestBuilder = underlying.prepareDelete(url)
  override def prepareGet(url: String): BoundRequestBuilder = underlying.prepareGet(url)
  override def preparePut(url: String): BoundRequestBuilder = underlying.preparePut(url)
  override def setSignatureCalculator(signatureCalculator: SignatureCalculator): AsyncHttpClient = underlying.setSignatureCalculator(signatureCalculator)
  override def isClosed: Boolean = underlying.isClosed
  override def prepareConnect(url: String): BoundRequestBuilder = underlying.prepareConnect(url)
  override def prepareRequest(request: Request): BoundRequestBuilder = underlying.prepareRequest(request)
  override def prepareRequest(requestBuilder: RequestBuilder): BoundRequestBuilder = underlying.prepareRequest(requestBuilder)
  override def prepareHead(url: String): BoundRequestBuilder = underlying.prepareHead(url)
  override def prepareOptions(url: String): BoundRequestBuilder = underlying.prepareOptions(url)
  override def prepareTrace(url: String): BoundRequestBuilder = underlying.prepareTrace(url)
  override def close(): Unit = underlying.close()

  override def executeRequest(requestBuilder: RequestBuilder) : ListenableFuture[Response]= executeRequest(requestBuilder.build())
  override def executeRequest[T](requestBuilder: RequestBuilder, handler: AsyncHandler[T]) : ListenableFuture[T] = executeRequest(requestBuilder.build(), handler)
  override def executeRequest(request: Request) : ListenableFuture[Response]=  executeRequest(request, new AsyncCompletionHandlerBase())

  override def executeRequest[T](request: Request, handler: AsyncHandler[T]) : ListenableFuture[T] = underlying.executeRequest(request, new TimerAsyncHandler[T](statsd, dependencyNamingStrategy(request), handler, startTime = System.currentTimeMillis(), dependencyNamingStrategy))
}

class TimerAsyncHandler[T](statsD: MetricsStatsdClient, metricName: String, delegate : AsyncHandler[T], private val startTime: Long, dependencyNamingStrategy : Request => String) extends AsyncHandler[T] {
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
