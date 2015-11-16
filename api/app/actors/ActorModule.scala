package com.bryzek.dependency.actors

import akka.actor.ActorRef
import com.google.inject.AbstractModule
import com.google.inject.name.Named
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {

  def configure = {
    bindActor[MainActor]("mainActor")
  }

}

@Singleton
class Actors @Inject() (@Named("mainActor") mainActor: ActorRef)  


