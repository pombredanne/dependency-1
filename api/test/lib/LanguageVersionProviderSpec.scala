package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.ProgrammingLanguage

import play.api.libs.ws._
import play.api.test._

class LanguageVersionProviderSpec extends PlaySpecification {

  "scala" in {
    val versions = DefaultLanguageVersionProvider.versions(ProgrammingLanguage.Scala).map(_.version)
    versions.contains("2.11.7") must beTrue
    versions.contains("2.9.1.final") must beTrue
    versions.contains("0.11.7") must beFalse
  }

  "undefined" in {
    DefaultLanguageVersionProvider.versions(ProgrammingLanguage.UNDEFINED("other")) must be(Nil)
  }

  "toVersion" in {
    DefaultLanguageVersionProvider.toVersion("Scala 2.11.7").map(_.version) must beEqualTo(Some("2.11.7"))
    DefaultLanguageVersionProvider.toVersion("Scala 2.11.0-M4").map(_.version) must beEqualTo(Some("2.11.0-M4"))
    DefaultLanguageVersionProvider.toVersion("Scala 2.9.1.final").map(_.version) must beEqualTo(Some("2.9.1.final"))
    DefaultLanguageVersionProvider.toVersion("Scala License") must be(None)
  }

}