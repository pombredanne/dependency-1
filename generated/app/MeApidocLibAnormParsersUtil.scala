package me.apidoc.lib.anorm.parsers.util {

  sealed trait Config {
    def name(column: String): String
  }

  object Config {
    case class Prefix(prefix: String) extends Config {
      override def name(column: String): String = s"${prefix}_$column"
    }
  }

}