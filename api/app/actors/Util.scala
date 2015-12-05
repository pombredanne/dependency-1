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
        Logger.error(msg(s"$description: ${t}") , t)
      }
    }
  }

  def withVerboseErrorHandler[T](
    description: Any
  ) (
    f: => T
  ) {
    Logger.info(msg(description.toString))
    withErrorHandler(description)(f)
  }

  private[this] def msg(value: String) = {
    "${getClass.name}: $value"
  }

}
