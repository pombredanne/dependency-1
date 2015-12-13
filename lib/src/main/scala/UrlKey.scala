package com.bryzek.dependency.lib

// adapted drom https://github.com/mbryzek/apidoc
object UrlKey {

  // Random generator
  private[this] val random = new scala.util.Random
  private[this] val Characters = "abcdefghijklmnopqrstuvwxyz"

  // Generate a random string of length n from the given alphabet
  def randomString(alphabet: String)(n: Int): String = {
    Stream.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString
  }
 
  // Generate a random alphabnumeric string of length n - will select
  // only from a-z lower case characters
  def randomAlphanumericString(n: Int) = {
    randomString(Characters)(n)
  }

  private val MinKeyLength = 4

  // Only want lower case letters and dashes
  private val Regexp1 = """([^0-9a-z\-\_\.])""".r

  // Turn multiple dashes into single dashes
  private val Regexp2 = """(\-+)""".r

  // Turn multiple underscores into single underscore
  private val Regexp3 = """(\_+)""".r

  private val RegexpLeadingSpaces = """^\-+""".r
  private val RegexpTrailingSpaces = """\-+$""".r

  /**
    * Deterministically generated a unique key that is URL safe.
    * 
    * @param checkFunction Optionally provide your own function that accepts the 
    *  generated key and returns true / false. If false, we will
    *  iterate to create another key that you can check. This lets you
    *  do things like check uniqueness of the key against an external
    *  source (e.g. database table)
    */
  @scala.annotation.tailrec
  def generate(
    value: String,
    suffix: Option[Int] = None
  ) (
    checkFunction: String => Boolean = { _ => true }
  ): String = {
    val formatted = format(value)
    (formatted.length < MinKeyLength) match {
      case true => {
        generate(value + randomAlphanumericString(MinKeyLength - value.length), suffix)(checkFunction)
      }
      case false => {
        val (key, nextSuffix) = suffix match {
          case None => (formatted, 1)
          case Some(i) => (formatted + "-1", i + 1)
        }

        validate(key) match {
          case Nil => {
            (checkFunction(key)) match {
              case true => key
              case false => generate(value, Some(nextSuffix))(checkFunction)
            }
          }
          case errors => {
            if (nextSuffix > 1000) {
              sys.error(s"Possible infinite loop generating key for value[$value]")
            }
            generate(value, Some(nextSuffix))(checkFunction)
          }
        }
      }
    }
  }

  private def format(value: String): String = {
    RegexpTrailingSpaces.replaceAllIn(
      RegexpLeadingSpaces.replaceAllIn(
        Regexp3.replaceAllIn(
          Regexp2.replaceAllIn(
            Regexp1.replaceAllIn(value.toLowerCase.trim, m => "-"),
            m => "-"
          ), m => "_"
        ), m => ""),
      m => ""
    )
  }

  def validate(key: String): Seq[String] = {
    val generated = UrlKey.format(key)
    if (key.length < MinKeyLength) {
      Seq(s"Key must be at least $MinKeyLength characters")
    } else if (key != generated) {
      Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: $generated")
    } else {
      ReservedKeys.find(_ == generated) match {
        case Some(value) => Seq(s"$key is a reserved word and cannot be used for the key")
        case None => Nil
      }
    }
  }

  val ReservedKeys = Seq(
    "_internal_", "api", "api.json", "account", "admin", "accept", "asset", "bucket",
    "change", "code", "confirm", "config", "doc", "docs", "documentation", "domains",
    "generators", "history", "internal", "login", "logout", "members", "metadatum", "metadata",
    "org", "orgs", "organizations", "password", "private", "reject", "service.json", "sessions",
    "settings", "scms", "search", "source", "subaccounts", "subscriptions", "teams", "types", "users",
    "util", "version", "version", "watch", "watches"
  ).map(UrlKey.format(_))

}
