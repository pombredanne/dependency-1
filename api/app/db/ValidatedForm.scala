package db

import io.flow.play.util.{Validated, Validation}
import io.flow.common.v0.models.Error

case class ValidatedForm[T](form: T, errorMessages: Seq[String]) extends Validated {

  override def errors = Validation.errors(errorMessages)

}
