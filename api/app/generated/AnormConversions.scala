package com.bryzek.dependency.v0.anorm.conversions {

  import anorm.{Column, MetaDataItem, TypeDoesNotMatch}
  import play.api.libs.json.{JsArray, JsObject, JsValue}
  import scala.util.{Failure, Success, Try}

  /**
    * Conversions to collections of objects using JSON.
    */
  object Json {

    private[this] def parser[T](
      f: play.api.libs.json.JsValue => T
    ) = anorm.Column.nonNull1 { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case json: org.postgresql.util.PGobject => {
          Try {
            f(
              play.api.libs.json.Json.parse(
                json.getValue
              )
            )
          } match {
            case Success(result) => Right(result)
            case Failure(ex) => Left(
              TypeDoesNotMatch(
                s"Column[$qualified] error parsing json $value: $ex"
              )
            )
          }
        }
        case _=> {
          Left(
            TypeDoesNotMatch(
              s"Column[$qualified] error converting $value: ${value.asInstanceOf[AnyRef].getClass} to Json"
            )
          )
        }
      }
    }

    implicit val columnToSeqString: Column[Seq[String]] = parser { _.as[Seq[String]] }
    implicit val columnToSeqJsValue: Column[Seq[JsValue]] = parser { _.as[Seq[JsValue]] }

    // Repeat for Map[String, Type]
    implicit val columnToMapStringString: Column[Map[String, String]] = parser { _.as[Map[String, String]] }

  }
}
