import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.json.{JsArray, JsValue}
import play.api.Play.current
import scala.util.{Failure, Success, Try}

package com.bryzek.dependency.v0.anorm.conversions {

  object Json {

    implicit val columnToJsValue: Column[JsValue] = {
      anorm.Column.nonNull1 { (value, meta) =>
        val MetaDataItem(qualified, nullable, clazz) = meta
        value match {
          case json: org.postgresql.util.PGobject => {
            Try(
              play.api.libs.json.Json.parse(
                json.getValue
              )
            ) match {
              case Success(result) => {
                Right(result)
              }
              case Failure(ex) => {
                Left(
                  TypeDoesNotMatch(
                    s"Column[$qualified] error parsing json $value: $ex"
                  )
                )
              }
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
    }

    implicit val columnToJsArray: Column[JsArray] = {
      anorm.Column.nonNull1 { (value, meta) =>
        val MetaDataItem(qualified, nullable, clazz) = meta
        value match {
          case json: org.postgresql.util.PGobject => {
            Try(
              play.api.libs.json.Json.parse(
                json.getValue
              ).as[JsArray]
            ) match {
              case Success(result) => {
                Right(result)
              }
              case Failure(ex) => {
                Left(
                  TypeDoesNotMatch(
                    s"Column[$qualified] error parsing json $value: $ex"
                  )
                )
              }
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
    }

  }

}
