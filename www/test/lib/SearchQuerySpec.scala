package com.bryzek.dependency.lib

import org.specs2.mutable._

class SearchQuerySpec extends Specification {

  "queryString" in {
    SearchQuery().query must beEqualTo("")
    SearchQuery(groupId = Some("me.apidoc")).query must beEqualTo("groupId=me.apidoc")
    SearchQuery(artifactId = Some("util")).query must beEqualTo("artifactId=util")
    SearchQuery(version = Some("1.0.0")).query must beEqualTo("version=1.0.0")
    SearchQuery(
      groupId = Some("me.apidoc"),
      artifactId = Some("util"),
      version = Some("1.0")
    ).query must beEqualTo(
      "groupId=me.apidoc artifactId=util version=1.0"
    )
  }

  "queryString ignores empty strings" in {
    SearchQuery(groupId = Some("")).query must beEqualTo("")
  }

  "queryString trims space" in {
    SearchQuery(groupId = Some("  me.apidoc  ")).query must beEqualTo("groupId=me.apidoc")
  }

  "parse" in {
    SearchQuery.parse("").query must beEqualTo("")
    SearchQuery.parse("groupId=me.apidoc").query must beEqualTo("groupId=me.apidoc")
    SearchQuery.parse("  groupId   =   me.apidoc  ").query must beEqualTo("groupId=me.apidoc")
    SearchQuery.parse("  groupid   =   me.apidoc  ").query must beEqualTo("groupId=me.apidoc")

    SearchQuery.parse("artifactId=util").query must beEqualTo("artifactId=util")
    SearchQuery.parse("artifactid=util").query must beEqualTo("artifactId=util")
    SearchQuery.parse("version = 1.0.0").query must beEqualTo("version=1.0.0")
    SearchQuery.parse("version = 1.0.0-dev").query must beEqualTo("version=1.0.0-dev")
    SearchQuery.parse("groupId=me.apidoc artifactId=util version=1.0.0").query must beEqualTo(
      "groupId=me.apidoc artifactId=util version=1.0.0"
    )
  }

}
