package com.bryzek.dependency.lib

import io.flow.user.v0.models.{Name, User}
import io.flow.play.util.DefaultConfig
import java.util.UUID
import java.nio.file.{Path, Paths, Files}
import java.nio.charset.StandardCharsets
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sendgrid._

case class Person(email: String, name: Name = Name())

object Person {
  def fromUser(user: User): Option[Person] = {
    user.email.map { email =>
      Person(
        email = email,
        name = user.name
      )
    }
  }
}

object Email {

  private[this] val subjectPrefix = DefaultConfig.requiredString("mail.subject.prefix")

  private[this] val from = Person(
    email = DefaultConfig.requiredString("mail.default.from.email"),
    name = Name(
      Some(DefaultConfig.requiredString("mail.default.name.first")),
      Some(DefaultConfig.requiredString("mail.default.name.last"))
    )
  )

  val localDeliveryDir = DefaultConfig.optionalString("mail.local.delivery.dir").map(Paths.get(_))

  // Initialize sendgrid on startup to verify that all of our settings
  // are here. If using localDeliveryDir, set password to a test
  // string.
  private[this] val sendgrid = {
    localDeliveryDir match {
      case None => new SendGrid(DefaultConfig.requiredString("sendgrid.api.key"))
      case Some(_) => new SendGrid("development")
    }
  }

  def sendHtml(
    to: Person,
    subject: String,
    body: String
  ) {
    val prefixedSubject = subjectPrefix + " " + subject

    val email = new SendGrid.Email()
    email.addTo(to.email)
    fullName(to.name).map { n => email.addToName(n) }
    email.setFrom(from.email)
    fullName(from.name).map { n => email.setFromName(n) }
    email.setSubject(prefixedSubject)
    email.setHtml(body)

    localDeliveryDir match {
      case Some(dir) => {
        localDelivery(dir, to, prefixedSubject, body)
      }

      case None => {
        val response = sendgrid.send(email)
        assert(response.getStatus, "Error sending email: " + response.getMessage())
      }
    }
  }

  private[this] def fullName(name: Name): Option[String] = {
    Seq(name.first, name.last).flatten.toList match {
      case Nil => None
      case names => Some(names.mkString(" "))
    }
  }

  private[this] def localDelivery(dir: Path, to: Person, subject: String, body: String): String = {
    val timestamp = ISODateTimeFormat.dateTimeNoMillis.print(new DateTime())

    Files.createDirectories(dir)
    val target = Paths.get(dir.toString, timestamp + "-" + UUID.randomUUID.toString + ".html")
    val name = fullName(to.name) match {
      case None => to.email
      case Some(name) => s""""$name" <${to.email}">"""
    }

    val bytes = s"""<p>
To: $name<br/>
Subject: $subject
</p>
<hr size="1"/>

$body
""".getBytes(StandardCharsets.UTF_8)
    Files.write(target, bytes)

    println(s"email delivered locally to $target")
    s"local-delivery-to-$target"
  }

}
