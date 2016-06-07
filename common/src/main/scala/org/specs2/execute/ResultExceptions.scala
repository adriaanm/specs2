package org.specs2
package execute

/** this class allows to throw a failure result in an Exception */
case class FailureException(f: Failure) extends Exception {
  override def getMessage = f.message
  override def getCause = f.exception
  override def getStackTrace = f.exception.getStackTrace
}
/** this class allows to throw a skipped result in an Exception */
case class SkipException(f: Skipped) extends Exception {
  /** create a SkipException from a Failure */
  def this(f: Failure) = this(f.skip)

  override def getMessage = f.message
}

/** this class allows to throw a pending result in an Exception */
case class PendingException(f: Pending) extends Exception {
  override def getMessage = f.message
}

/** this class allows to throw an Error result in an Exception */
case class ErrorException(f: Error) extends Exception {
  override def getMessage = f.message
  override def getCause = f.exception
  override def getStackTrace = f.exception.getStackTrace
}

/** this class allows to throw a result that's decorated with additional information in an Exception */
case class DecoratedResultException(result: DecoratedResult[_]) extends Exception


case class MyClass(list: List[_])
