package com.bryzek.dependency.actors

import akka.actor.ActorRef

object MainActorProvider {

  def ref() = {
    play.api.Play.current.injector.instanceOf[MainActorProvider].ref

  }

  private[this] class MainActorProvider @javax.inject.Inject() (
    @javax.inject.Named("main-actor") val ref: akka.actor.ActorRef
  )

}
