package com.bryzek.dependency.api.lib

import io.flow.common.v0.models.{Name, User}
import io.flow.play.util.{Config, IdGenerator}
import java.nio.file.{Path, Paths, Files}
import java.nio.charset.StandardCharsets
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sendgrid._

object Email {

  private[this] val config = play.api.Play.current.injector.instanceOf[Config]

  private[this] val SubjectPrefix = config.requiredString("mail.subject.prefix")

  def subjectWithPrefix(subject: String): String = {
    SubjectPrefix + " " + subject
  }

  private[this] val fromEmail = config.requiredString("mail.default.from.email")
  private[this] val fromName = Name(
    Some(config.requiredString("mail.default.from.name.first")),
    Some(config.requiredString("mail.default.from.name.last"))
  )

  val localDeliveryDir = config.optionalString("mail.local.delivery.dir").map(Paths.get(_))

  // Initialize sendgrid on startup to verify that all of our settings
  // are here. If using localDeliveryDir, set password to a test
  // string.
  private[this] val sendgrid = {
    localDeliveryDir match {
      case None => new SendGrid(config.requiredString("sendgrid.api.key"))
      case Some(_) => new SendGrid("development")
    }
  }

  def sendHtml(
    to: Recipient,
    subject: String,
    body: String
  ) {
    val prefixedSubject = subjectWithPrefix(subject)

    val email = new SendGrid.Email()
    email.addTo(to.email)
    fullName(to.name).map { n => email.addToName(n) }
    email.setFrom(fromEmail)
    fullName(fromName).map { n => email.setFromName(n) }
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

  private[this] def localDelivery(dir: Path, to: Recipient, subject: String, body: String): String = {
    val timestamp = ISODateTimeFormat.dateTimeNoMillis.print(new DateTime())

    Files.createDirectories(dir)
    val target = Paths.get(dir.toString, timestamp + "-" + IdGenerator("eml").randomId() + ".html")
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
