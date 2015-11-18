import anorm.{Column, MetaDataItem, TypeDoesNotMatch}
import play.api.libs.json.{JsArray, JsObject, JsValue}
import scala.util.{Failure, Success, Try}

package com.bryzek.dependency.v0.anorm.conversions {

  object Json {

    private[this] def parser[T, U](
      f: org.postgresql.util.PGobject => T
    ) = anorm.Column.nonNull1 { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case json: org.postgresql.util.PGobject => {
          Try {
            f(json)
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

    implicit val columnToSeqString: Column[Seq[String]] = parser { json =>
      play.api.libs.json.Json.parse(
        json.getValue
      ).as[Seq[String]]
    }

    implicit val columnToMapStringString: Column[Map[String, String]] = parser { json =>
      play.api.libs.json.Json.parse(
        json.getValue
      ).as[Map[String, String]]
    }

    implicit val columnToJsValue: Column[JsValue] = parser { json =>
      play.api.libs.json.Json.parse(
        json.getValue
      )
    }

    implicit val columnToJsObject: Column[JsObject] = parser { json =>
      play.api.libs.json.Json.parse(
        json.getValue
      ).as[JsObject]
    }

    implicit val columnToSeqJsValue: Column[Seq[JsValue]] = parser { json =>
      play.api.libs.json.Json.parse(
        json.getValue
      ).as[JsArray].value
    }

  }
}
