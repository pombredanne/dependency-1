package com.bryzek.dependency.actors

import com.bryzek.dependency.lib.{Email, Person}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object EmailActor {

  object Messages {
    case object SendRecommendationsSummary
  }

}

class EmailActor extends Actor with Util {

  def receive = {

    case m @ EmailActor.Messages.SendRecommendationsSummary => withVerboseErrorHandler(m) {
      println("TODO")
    }

  }

}
