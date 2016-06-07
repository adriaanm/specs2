package org.specs2
package matcher

import org.specs2.concurrent.ExecutionEnv
import scala.concurrent.duration._
import scala.concurrent._
import java.util.concurrent.TimeoutException
import org.specs2.execute._

/**
 * This trait is for transforming matchers of values to matchers of Futures
 */
trait FutureMatchers extends FutureBaseMatchers {
  /**
   * add an `await` method to any matcher `Matcher[T]` so that it can be transformed into a `Matcher[Future[T]]`
   */
  implicit class FutureMatchable[T](m: Matcher[T])(implicit ee: ExecutionEnv) {
    def await: Matcher[Future[T]]                                        = awaitMatcher(m)(retries = 0, timeout = 1.second)
    def await(retries: Int, timeout: FiniteDuration): Matcher[Future[T]] = awaitMatcher(m)(retries, timeout)
    def retryAwait(retries: Int): Matcher[Future[T]]                     = awaitMatcher(m)(retries, timeout = 1.second)
    def awaitFor(timeout: FiniteDuration): Matcher[Future[T]]            = awaitMatcher(m)(retries = 0, timeout)
  }

  /**
   * when a Future contains a result, it can be awaited to return this result
   */
  implicit class futureAsResult[T](f: Future[T])(implicit ee: ExecutionEnv, asResult: AsResult[T]) extends FutureAsResult[T](f)
}

private[specs2]
trait FutureBaseMatchers extends ExpectationsCreation {

  def await[T](m: Matcher[T])(implicit ee: ExecutionEnv): Matcher[Future[T]] = awaitMatcher(m)(retries = 0, timeout = 1.second)
  def await[T](m: Matcher[T])(retries: Int, timeout: FiniteDuration)(implicit ee: ExecutionEnv): Matcher[Future[T]] = awaitMatcher(m)(retries, timeout)
  def awaitFor[T](m: Matcher[T])(timeout: FiniteDuration)(implicit ee: ExecutionEnv): Matcher[Future[T]] = awaitMatcher(m)(retries = 0, timeout)
  def retry[T](m: Matcher[T])(retries: Int)(implicit ee: ExecutionEnv): Matcher[Future[T]] = awaitMatcher(m)(retries, timeout = 1.second)

  private[specs2]
  class FutureAsResult[T](f: Future[T])(implicit ee: ExecutionEnv, asResult: AsResult[T]) {
    def await: Result = await(retries = 0, timeout = 1.second)
    def retry(retries: Int): Result = await(retries, timeout = 1.second)
    def awaitFor(timeout: FiniteDuration): Result = await(retries = 0, timeout)

    def await(retries: Int, timeout: FiniteDuration): Result = {
      val tf = ee.timeFactor
      val appliedTimeout = timeout * tf.toLong

      def awaitFuture(remainingRetries: Int, totalDuration: FiniteDuration): Result = {
        try Await.result(f.map(value => AsResult(value))(ee.executionContext), appliedTimeout)
        catch {
          case e if e.getClass == classOf[TimeoutException] =>
            if (remainingRetries <= 0) Failure(s"Timeout after ${totalDuration + appliedTimeout} (retries = $retries, timeout = $timeout)")
            else                       awaitFuture(remainingRetries - 1, totalDuration + appliedTimeout)
          case other: Throwable                             => throw other
        }
      }
      awaitFuture(retries, 0.second)
    }
  }

  private[specs2] def awaitMatcher[T](m: Matcher[T])(retries: Int, timeout: FiniteDuration)(implicit ee: ExecutionEnv): Matcher[Future[T]] = new Matcher[Future[T]] {
    def apply[S <: Future[T]](a: Expectable[S]) = {
      try {
        val r = new FutureAsResult(a.value.map(v => AsResult(createExpectable(v).applyMatcher(m)))(ee.executionContext)).await(retries, timeout)
        result(r.isSuccess, r.message, r.message, a)
      } catch {
        // if awaiting on the future throws an exception because it was a failed future
        // there try to match again because the matcher can be a `throwA` matcher
        case t: Throwable =>
          val r = createExpectable(throw t).applyMatcher(m).toResult
          result(r.isSuccess, r.message, r.message, a)
      }
    }
  }
}

object FutureMatchers extends FutureMatchers
