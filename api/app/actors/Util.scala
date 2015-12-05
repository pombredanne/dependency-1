package com.bryzek.dependency.actors

import play.api.Logger

/**
  * TODO: Extract to lib. Maybe lib-play-actors ??
  */
trait Util {

  def withErrorHandler[T](
    description: Any
  ) (
    f: => T
  ) {
    try {
      f
    } catch {
      case t: Throwable => {
        Logger.error(s"$description: ${t}" , t)
      }
    }
  }

  def withVerboseErrorHandler[T](
    description: Any
  ) (
    f: => T
  ) {
    Logger.info(description.toString)
    withErrorHandler(description)(f)
  }

}
