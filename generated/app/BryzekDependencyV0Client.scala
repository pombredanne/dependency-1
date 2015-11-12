/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.0.1-dev
 * apidoc:0.9.44 http://www.apidoc.me/bryzek/dependency/0.0.1-dev/play_2_4_client
 */
package com.bryzek.dependency.v0.models {

  case class Language(
    guid: _root_.java.util.UUID,
    name: com.bryzek.dependency.v0.models.ProgrammingLanguage,
    audit: io.flow.common.v0.models.Audit
  )

  case class LanguageForm(
    name: String,
    version: _root_.scala.Option[String] = None
  )

  case class LanguageVersion(
    guid: _root_.java.util.UUID,
    version: String,
    audit: io.flow.common.v0.models.Audit
  )

  case class Library(
    guid: _root_.java.util.UUID,
    resolvers: Seq[String],
    groupId: String,
    artifactId: String,
    audit: io.flow.common.v0.models.Audit
  )

  case class LibraryForm(
    groupId: String,
    resolvers: Seq[String],
    artifactId: String,
    version: _root_.scala.Option[String] = None
  )

  case class LibraryVersion(
    guid: _root_.java.util.UUID,
    version: String,
    audit: io.flow.common.v0.models.Audit
  )

  case class Name(
    first: _root_.scala.Option[String] = None,
    last: _root_.scala.Option[String] = None
  )

  case class NameForm(
    first: _root_.scala.Option[String] = None,
    last: _root_.scala.Option[String] = None
  )

  case class Project(
    guid: _root_.java.util.UUID,
    scms: com.bryzek.dependency.v0.models.Scms,
    name: String,
    audit: io.flow.common.v0.models.Audit
  )

  case class ProjectForm(
    name: String,
    scms: com.bryzek.dependency.v0.models.Scms,
    uri: String
  )

  case class User(
    guid: _root_.java.util.UUID,
    email: String,
    name: _root_.scala.Option[com.bryzek.dependency.v0.models.Name] = None,
    audit: io.flow.common.v0.models.Audit
  )

  case class UserForm(
    guid: _root_.java.util.UUID,
    email: String,
    name: _root_.scala.Option[com.bryzek.dependency.v0.models.NameForm] = None
  )

  sealed trait ProgrammingLanguage

  object ProgrammingLanguage {

    case object Scala extends ProgrammingLanguage { override def toString = "scala" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends ProgrammingLanguage

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Scala)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): ProgrammingLanguage = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[ProgrammingLanguage] = byName.get(value.toLowerCase)

  }

  sealed trait Scms

  object Scms {

    case object GitHub extends Scms { override def toString = "git_hub" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Scms

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(GitHub)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): Scms = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[Scms] = byName.get(value.toLowerCase)

  }

}

package com.bryzek.dependency.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import com.bryzek.dependency.v0.models.json._
    import io.flow.common.v0.models.json._

    private[v0] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[v0] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[v0] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsDependencyProgrammingLanguage = __.read[String].map(ProgrammingLanguage.apply)
    implicit val jsonWritesDependencyProgrammingLanguage = new Writes[ProgrammingLanguage] {
      def writes(x: ProgrammingLanguage) = JsString(x.toString)
    }

    implicit val jsonReadsDependencyScms = __.read[String].map(Scms.apply)
    implicit val jsonWritesDependencyScms = new Writes[Scms] {
      def writes(x: Scms) = JsString(x.toString)
    }

    implicit def jsonReadsDependencyLanguage: play.api.libs.json.Reads[Language] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "name").read[com.bryzek.dependency.v0.models.ProgrammingLanguage] and
        (__ \ "audit").read[io.flow.common.v0.models.Audit]
      )(Language.apply _)
    }

    implicit def jsonWritesDependencyLanguage: play.api.libs.json.Writes[Language] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "name").write[com.bryzek.dependency.v0.models.ProgrammingLanguage] and
        (__ \ "audit").write[io.flow.common.v0.models.Audit]
      )(unlift(Language.unapply _))
    }

    implicit def jsonReadsDependencyLanguageForm: play.api.libs.json.Reads[LanguageForm] = {
      (
        (__ \ "name").read[String] and
        (__ \ "version").readNullable[String]
      )(LanguageForm.apply _)
    }

    implicit def jsonWritesDependencyLanguageForm: play.api.libs.json.Writes[LanguageForm] = {
      (
        (__ \ "name").write[String] and
        (__ \ "version").writeNullable[String]
      )(unlift(LanguageForm.unapply _))
    }

    implicit def jsonReadsDependencyLanguageVersion: play.api.libs.json.Reads[LanguageVersion] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "version").read[String] and
        (__ \ "audit").read[io.flow.common.v0.models.Audit]
      )(LanguageVersion.apply _)
    }

    implicit def jsonWritesDependencyLanguageVersion: play.api.libs.json.Writes[LanguageVersion] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "version").write[String] and
        (__ \ "audit").write[io.flow.common.v0.models.Audit]
      )(unlift(LanguageVersion.unapply _))
    }

    implicit def jsonReadsDependencyLibrary: play.api.libs.json.Reads[Library] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "resolvers").read[Seq[String]] and
        (__ \ "group_id").read[String] and
        (__ \ "artifact_id").read[String] and
        (__ \ "audit").read[io.flow.common.v0.models.Audit]
      )(Library.apply _)
    }

    implicit def jsonWritesDependencyLibrary: play.api.libs.json.Writes[Library] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "resolvers").write[Seq[String]] and
        (__ \ "group_id").write[String] and
        (__ \ "artifact_id").write[String] and
        (__ \ "audit").write[io.flow.common.v0.models.Audit]
      )(unlift(Library.unapply _))
    }

    implicit def jsonReadsDependencyLibraryForm: play.api.libs.json.Reads[LibraryForm] = {
      (
        (__ \ "group_id").read[String] and
        (__ \ "resolvers").read[Seq[String]] and
        (__ \ "artifact_id").read[String] and
        (__ \ "version").readNullable[String]
      )(LibraryForm.apply _)
    }

    implicit def jsonWritesDependencyLibraryForm: play.api.libs.json.Writes[LibraryForm] = {
      (
        (__ \ "group_id").write[String] and
        (__ \ "resolvers").write[Seq[String]] and
        (__ \ "artifact_id").write[String] and
        (__ \ "version").writeNullable[String]
      )(unlift(LibraryForm.unapply _))
    }

    implicit def jsonReadsDependencyLibraryVersion: play.api.libs.json.Reads[LibraryVersion] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "version").read[String] and
        (__ \ "audit").read[io.flow.common.v0.models.Audit]
      )(LibraryVersion.apply _)
    }

    implicit def jsonWritesDependencyLibraryVersion: play.api.libs.json.Writes[LibraryVersion] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "version").write[String] and
        (__ \ "audit").write[io.flow.common.v0.models.Audit]
      )(unlift(LibraryVersion.unapply _))
    }

    implicit def jsonReadsDependencyName: play.api.libs.json.Reads[Name] = {
      (
        (__ \ "first").readNullable[String] and
        (__ \ "last").readNullable[String]
      )(Name.apply _)
    }

    implicit def jsonWritesDependencyName: play.api.libs.json.Writes[Name] = {
      (
        (__ \ "first").writeNullable[String] and
        (__ \ "last").writeNullable[String]
      )(unlift(Name.unapply _))
    }

    implicit def jsonReadsDependencyNameForm: play.api.libs.json.Reads[NameForm] = {
      (
        (__ \ "first").readNullable[String] and
        (__ \ "last").readNullable[String]
      )(NameForm.apply _)
    }

    implicit def jsonWritesDependencyNameForm: play.api.libs.json.Writes[NameForm] = {
      (
        (__ \ "first").writeNullable[String] and
        (__ \ "last").writeNullable[String]
      )(unlift(NameForm.unapply _))
    }

    implicit def jsonReadsDependencyProject: play.api.libs.json.Reads[Project] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "scms").read[com.bryzek.dependency.v0.models.Scms] and
        (__ \ "name").read[String] and
        (__ \ "audit").read[io.flow.common.v0.models.Audit]
      )(Project.apply _)
    }

    implicit def jsonWritesDependencyProject: play.api.libs.json.Writes[Project] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "scms").write[com.bryzek.dependency.v0.models.Scms] and
        (__ \ "name").write[String] and
        (__ \ "audit").write[io.flow.common.v0.models.Audit]
      )(unlift(Project.unapply _))
    }

    implicit def jsonReadsDependencyProjectForm: play.api.libs.json.Reads[ProjectForm] = {
      (
        (__ \ "name").read[String] and
        (__ \ "scms").read[com.bryzek.dependency.v0.models.Scms] and
        (__ \ "uri").read[String]
      )(ProjectForm.apply _)
    }

    implicit def jsonWritesDependencyProjectForm: play.api.libs.json.Writes[ProjectForm] = {
      (
        (__ \ "name").write[String] and
        (__ \ "scms").write[com.bryzek.dependency.v0.models.Scms] and
        (__ \ "uri").write[String]
      )(unlift(ProjectForm.unapply _))
    }

    implicit def jsonReadsDependencyUser: play.api.libs.json.Reads[User] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "email").read[String] and
        (__ \ "name").readNullable[com.bryzek.dependency.v0.models.Name] and
        (__ \ "audit").read[io.flow.common.v0.models.Audit]
      )(User.apply _)
    }

    implicit def jsonWritesDependencyUser: play.api.libs.json.Writes[User] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "email").write[String] and
        (__ \ "name").writeNullable[com.bryzek.dependency.v0.models.Name] and
        (__ \ "audit").write[io.flow.common.v0.models.Audit]
      )(unlift(User.unapply _))
    }

    implicit def jsonReadsDependencyUserForm: play.api.libs.json.Reads[UserForm] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "email").read[String] and
        (__ \ "name").readNullable[com.bryzek.dependency.v0.models.NameForm]
      )(UserForm.apply _)
    }

    implicit def jsonWritesDependencyUserForm: play.api.libs.json.Writes[UserForm] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "email").write[String] and
        (__ \ "name").writeNullable[com.bryzek.dependency.v0.models.NameForm]
      )(unlift(UserForm.unapply _))
    }
  }
}

package com.bryzek.dependency.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import com.bryzek.dependency.v0.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    implicit val queryStringBindableTypeDateTimeIso8601 = new QueryStringBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    implicit val queryStringBindableTypeDateIso8601 = new QueryStringBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    // Enum: ProgrammingLanguage
    private[this] val enumProgrammingLanguageNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${com.bryzek.dependency.v0.models.ProgrammingLanguage.all.mkString(", ")}"

    implicit val pathBindableEnumProgrammingLanguage = new PathBindable.Parsing[com.bryzek.dependency.v0.models.ProgrammingLanguage] (
      ProgrammingLanguage.fromString(_).get, _.toString, enumProgrammingLanguageNotFound
    )

    implicit val queryStringBindableEnumProgrammingLanguage = new QueryStringBindable.Parsing[com.bryzek.dependency.v0.models.ProgrammingLanguage](
      ProgrammingLanguage.fromString(_).get, _.toString, enumProgrammingLanguageNotFound
    )

    // Enum: Scms
    private[this] val enumScmsNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${com.bryzek.dependency.v0.models.Scms.all.mkString(", ")}"

    implicit val pathBindableEnumScms = new PathBindable.Parsing[com.bryzek.dependency.v0.models.Scms] (
      Scms.fromString(_).get, _.toString, enumScmsNotFound
    )

    implicit val queryStringBindableEnumScms = new QueryStringBindable.Parsing[com.bryzek.dependency.v0.models.Scms](
      Scms.fromString(_).get, _.toString, enumScmsNotFound
    )

  }

}


package com.bryzek.dependency.v0 {

  object Constants {

    val UserAgent = "apidoc:0.9.44 http://www.apidoc.me/bryzek/dependency/0.0.1-dev/play_2_4_client"
    val Version = "0.0.1-dev"
    val VersionMajor = 0

  }

  class Client(
    apiUrl: String,
    auth: scala.Option[com.bryzek.dependency.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) {
    import com.bryzek.dependency.v0.models.json._
    import io.flow.common.v0.models.json._

    private[this] val logger = play.api.Logger("com.bryzek.dependency.v0.Client")

    logger.info(s"Initializing com.bryzek.dependency.v0.Client for url $apiUrl")

    def ioFlowCommonV0ModelsHealthchecks: IoFlowCommonV0ModelsHealthchecks = IoFlowCommonV0ModelsHealthchecks

    object IoFlowCommonV0ModelsHealthchecks extends IoFlowCommonV0ModelsHealthchecks {
      override def getInternalAndHealthcheck()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.common.v0.models.Healthcheck] = {
        _executeRequest("GET", s"/_internal_/healthcheck").map {
          case r if r.status == 200 => _root_.com.bryzek.dependency.v0.Client.parseJson("io.flow.common.v0.models.Healthcheck", r, _.validate[io.flow.common.v0.models.Healthcheck])
          case r => throw new com.bryzek.dependency.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200")
        }
      }
    }

    def _requestHolder(path: String): play.api.libs.ws.WSRequest = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path).withHeaders(
        "User-Agent" -> Constants.UserAgent,
        "X-Apidoc-Version" -> Constants.Version,
        "X-Apidoc-Version-Major" -> Constants.VersionMajor.toString
      ).withHeaders(defaultHeaders : _*)
      auth.fold(holder) {
        case Authorization.Basic(username, password) => {
          holder.withAuth(username, password.getOrElse(""), play.api.libs.ws.WSAuthScheme.BASIC)
        }
        case a => sys.error("Invalid authorization scheme[" + a.getClass + "]")
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequest)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequest = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield s"$name=$value"
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      auth.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
      req
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withQueryString(queryParameters:_*).withHeaders("Content-Type" -> "application/json; charset=UTF-8")).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withQueryString(queryParameters:_*).withHeaders("Content-Type" -> "application/json; charset=UTF-8")).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withQueryString(queryParameters:_*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).withQueryString(queryParameters:_*)).head()
        }
         case "OPTIONS" => {
          _logRequest("OPTIONS", _requestHolder(path).withQueryString(queryParameters:_*)).options()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withQueryString(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

  }

  object Client {

    def parseJson[T](
      className: String,
      r: play.api.libs.ws.WSResponse,
      f: (play.api.libs.json.JsValue => play.api.libs.json.JsResult[T])
    ): T = {
      f(play.api.libs.json.Json.parse(r.body)) match {
        case play.api.libs.json.JsSuccess(x, _) => x
        case play.api.libs.json.JsError(errors) => {
          throw new com.bryzek.dependency.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
        }
      }
    }

  }

  sealed trait Authorization
  object Authorization {
    case class Basic(username: String, password: Option[String] = None) extends Authorization
  }

  trait IoFlowCommonV0ModelsHealthchecks {
    def getInternalAndHealthcheck()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.common.v0.models.Healthcheck]
  }

  package errors {

    case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends Exception(s"HTTP $responseCode: $message")

  }

}