package com.bryzek.dependency.lib

case class PropertiesParser(contents: String) extends SimpleScalaParser {

  private[this] lazy val properties: Map[String, String] = {
    var internal = scala.collection.mutable.Map[String, String]()
    lines.foreach { line =>
      line.split("=").map(_.trim).toList match {
        case key :: value :: Nil => {
          internal += (key -> value)
        }
        case _ => {
        }
      }
    }
    internal.toMap
  }

  def get(name: String): Option[String] = {
    properties.get(name)
  }

}
