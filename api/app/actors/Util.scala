package com.bryzek.dependency.actors

import play.api.Logger

/**
  * TODO: Extract to lib. Maybe lib-play-actors ??
  */
object Util {

  def withErrorHandler[T](
    description: String
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
    description: String
  ) (
    f: => T
  ) {
    Logger.info(description)
    withErrorHandler(description)(f)
  }

}
