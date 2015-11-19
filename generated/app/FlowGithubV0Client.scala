/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.0.1-dev
 * apidoc:0.9.47 http://www.apidoc.me/flow/github/0.0.1-dev/play_2_4_client
 */
package io.flow.github.v0.models {

  case class Contents(
    `type`: io.flow.github.v0.models.ContentsType,
    encoding: io.flow.github.v0.models.Encoding,
    size: Long,
    name: String,
    path: String,
    content: _root_.scala.Option[String] = None,
    sha: String,
    url: String,
    gitUrl: String,
    htmlUrl: String,
    downloadUrl: String
  )

  case class Owner(
    id: Long,
    login: String,
    avatarUrl: _root_.scala.Option[String] = None,
    gravatarId: _root_.scala.Option[String] = None,
    url: String,
    htmlUrl: String,
    `type`: io.flow.github.v0.models.OwnerType
  )

  case class Repository(
    id: Long,
    owner: io.flow.github.v0.models.Owner,
    name: String,
    fullName: String,
    description: _root_.scala.Option[String] = None,
    url: String,
    htmlUrl: String
  )

  sealed trait ContentsType

  object ContentsType {

    case object File extends ContentsType { override def toString = "file" }
    case object Dir extends ContentsType { override def toString = "dir" }
    case object Symlink extends ContentsType { override def toString = "symlink" }
    case object Submodule extends ContentsType { override def toString = "submodule" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends ContentsType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(File, Dir, Symlink, Submodule)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): ContentsType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[ContentsType] = byName.get(value.toLowerCase)

  }

  sealed trait Encoding

  object Encoding {

    case object Base64 extends Encoding { override def toString = "base64" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Encoding

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Base64)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): Encoding = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[Encoding] = byName.get(value.toLowerCase)

  }

  sealed trait OwnerType

  object OwnerType {

    case object User extends OwnerType { override def toString = "User" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends OwnerType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(User)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): OwnerType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[OwnerType] = byName.get(value.toLowerCase)

  }

  sealed trait Visibility

  object Visibility {

    case object All extends Visibility { override def toString = "all" }
    case object Public extends Visibility { override def toString = "public" }
    case object Private extends Visibility { override def toString = "private" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Visibility

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(All, Public, Private)

    private[this]
    val byName = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): Visibility = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[Visibility] = byName.get(value.toLowerCase)

  }

}

package io.flow.github.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.github.v0.models.json._

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

    implicit val jsonReadsGithubContentsType = __.read[String].map(ContentsType.apply)
    implicit val jsonWritesGithubContentsType = new Writes[ContentsType] {
      def writes(x: ContentsType) = JsString(x.toString)
    }

    implicit val jsonReadsGithubEncoding = __.read[String].map(Encoding.apply)
    implicit val jsonWritesGithubEncoding = new Writes[Encoding] {
      def writes(x: Encoding) = JsString(x.toString)
    }

    implicit val jsonReadsGithubOwnerType = __.read[String].map(OwnerType.apply)
    implicit val jsonWritesGithubOwnerType = new Writes[OwnerType] {
      def writes(x: OwnerType) = JsString(x.toString)
    }

    implicit val jsonReadsGithubVisibility = __.read[String].map(Visibility.apply)
    implicit val jsonWritesGithubVisibility = new Writes[Visibility] {
      def writes(x: Visibility) = JsString(x.toString)
    }

    implicit def jsonReadsGithubContents: play.api.libs.json.Reads[Contents] = {
      (
        (__ \ "type").read[io.flow.github.v0.models.ContentsType] and
        (__ \ "encoding").read[io.flow.github.v0.models.Encoding] and
        (__ \ "size").read[Long] and
        (__ \ "name").read[String] and
        (__ \ "path").read[String] and
        (__ \ "content").readNullable[String] and
        (__ \ "sha").read[String] and
        (__ \ "url").read[String] and
        (__ \ "git_url").read[String] and
        (__ \ "html_url").read[String] and
        (__ \ "download_url").read[String]
      )(Contents.apply _)
    }

    implicit def jsonWritesGithubContents: play.api.libs.json.Writes[Contents] = {
      (
        (__ \ "type").write[io.flow.github.v0.models.ContentsType] and
        (__ \ "encoding").write[io.flow.github.v0.models.Encoding] and
        (__ \ "size").write[Long] and
        (__ \ "name").write[String] and
        (__ \ "path").write[String] and
        (__ \ "content").writeNullable[String] and
        (__ \ "sha").write[String] and
        (__ \ "url").write[String] and
        (__ \ "git_url").write[String] and
        (__ \ "html_url").write[String] and
        (__ \ "download_url").write[String]
      )(unlift(Contents.unapply _))
    }

    implicit def jsonReadsGithubOwner: play.api.libs.json.Reads[Owner] = {
      (
        (__ \ "id").read[Long] and
        (__ \ "login").read[String] and
        (__ \ "avatar_url").readNullable[String] and
        (__ \ "gravatar_id").readNullable[String] and
        (__ \ "url").read[String] and
        (__ \ "html_url").read[String] and
        (__ \ "type").read[io.flow.github.v0.models.OwnerType]
      )(Owner.apply _)
    }

    implicit def jsonWritesGithubOwner: play.api.libs.json.Writes[Owner] = {
      (
        (__ \ "id").write[Long] and
        (__ \ "login").write[String] and
        (__ \ "avatar_url").writeNullable[String] and
        (__ \ "gravatar_id").writeNullable[String] and
        (__ \ "url").write[String] and
        (__ \ "html_url").write[String] and
        (__ \ "type").write[io.flow.github.v0.models.OwnerType]
      )(unlift(Owner.unapply _))
    }

    implicit def jsonReadsGithubRepository: play.api.libs.json.Reads[Repository] = {
      (
        (__ \ "id").read[Long] and
        (__ \ "owner").read[io.flow.github.v0.models.Owner] and
        (__ \ "name").read[String] and
        (__ \ "full_name").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "url").read[String] and
        (__ \ "html_url").read[String]
      )(Repository.apply _)
    }

    implicit def jsonWritesGithubRepository: play.api.libs.json.Writes[Repository] = {
      (
        (__ \ "id").write[Long] and
        (__ \ "owner").write[io.flow.github.v0.models.Owner] and
        (__ \ "name").write[String] and
        (__ \ "full_name").write[String] and
        (__ \ "description").writeNullable[String] and
        (__ \ "url").write[String] and
        (__ \ "html_url").write[String]
      )(unlift(Repository.unapply _))
    }
  }
}

package io.flow.github.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import io.flow.github.v0.models._

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

    // Enum: ContentsType
    private[this] val enumContentsTypeNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${io.flow.github.v0.models.ContentsType.all.mkString(", ")}"

    implicit val pathBindableEnumContentsType = new PathBindable.Parsing[io.flow.github.v0.models.ContentsType] (
      ContentsType.fromString(_).get, _.toString, enumContentsTypeNotFound
    )

    implicit val queryStringBindableEnumContentsType = new QueryStringBindable.Parsing[io.flow.github.v0.models.ContentsType](
      ContentsType.fromString(_).get, _.toString, enumContentsTypeNotFound
    )

    // Enum: Encoding
    private[this] val enumEncodingNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${io.flow.github.v0.models.Encoding.all.mkString(", ")}"

    implicit val pathBindableEnumEncoding = new PathBindable.Parsing[io.flow.github.v0.models.Encoding] (
      Encoding.fromString(_).get, _.toString, enumEncodingNotFound
    )

    implicit val queryStringBindableEnumEncoding = new QueryStringBindable.Parsing[io.flow.github.v0.models.Encoding](
      Encoding.fromString(_).get, _.toString, enumEncodingNotFound
    )

    // Enum: OwnerType
    private[this] val enumOwnerTypeNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${io.flow.github.v0.models.OwnerType.all.mkString(", ")}"

    implicit val pathBindableEnumOwnerType = new PathBindable.Parsing[io.flow.github.v0.models.OwnerType] (
      OwnerType.fromString(_).get, _.toString, enumOwnerTypeNotFound
    )

    implicit val queryStringBindableEnumOwnerType = new QueryStringBindable.Parsing[io.flow.github.v0.models.OwnerType](
      OwnerType.fromString(_).get, _.toString, enumOwnerTypeNotFound
    )

    // Enum: Visibility
    private[this] val enumVisibilityNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${io.flow.github.v0.models.Visibility.all.mkString(", ")}"

    implicit val pathBindableEnumVisibility = new PathBindable.Parsing[io.flow.github.v0.models.Visibility] (
      Visibility.fromString(_).get, _.toString, enumVisibilityNotFound
    )

    implicit val queryStringBindableEnumVisibility = new QueryStringBindable.Parsing[io.flow.github.v0.models.Visibility](
      Visibility.fromString(_).get, _.toString, enumVisibilityNotFound
    )

  }

}


package io.flow.github.v0 {

  object Constants {

    val UserAgent = "apidoc:0.9.47 http://www.apidoc.me/flow/github/0.0.1-dev/play_2_4_client"
    val Version = "0.0.1-dev"
    val VersionMajor = 0

  }

  class Client(
    apiUrl: String,
    auth: scala.Option[io.flow.github.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) {
    import io.flow.github.v0.models.json._

    private[this] val logger = play.api.Logger("io.flow.github.v0.Client")

    logger.info(s"Initializing io.flow.github.v0.Client for url $apiUrl")

    def contents: Contents = Contents

    def repositories: Repositories = Repositories

    object Contents extends Contents {
      override def getReposAndReadmeByOwnerAndRepo(
        owner: String,
        repo: String,
        ref: String = "master"
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.github.v0.models.Contents] = {
        val queryParameters = Seq(
          Some("ref" -> ref)
        ).flatten

        _executeRequest("GET", s"/repos/${play.utils.UriEncoding.encodePathSegment(owner, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(repo, "UTF-8")}/readme", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.github.v0.Client.parseJson("io.flow.github.v0.models.Contents", r, _.validate[io.flow.github.v0.models.Contents])
          case r if r.status == 401 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r if r.status == 404 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.github.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 404")
        }
      }

      override def getReposByOwnerAndRepoAndPath(
        owner: String,
        repo: String,
        path: String,
        ref: String = "master"
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.github.v0.models.Contents] = {
        val queryParameters = Seq(
          Some("ref" -> ref)
        ).flatten

        _executeRequest("GET", s"/repos/${play.utils.UriEncoding.encodePathSegment(owner, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(repo, "UTF-8")}/contents/${play.utils.UriEncoding.encodePathSegment(path, "UTF-8")}", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.github.v0.Client.parseJson("io.flow.github.v0.models.Contents", r, _.validate[io.flow.github.v0.models.Contents])
          case r if r.status == 401 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r if r.status == 404 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.github.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401, 404")
        }
      }
    }

    object Repositories extends Repositories {
      override def getUserAndRepos(
        visibility: io.flow.github.v0.models.Visibility = io.flow.github.v0.models.Visibility("all"),
        affiliation: _root_.scala.Option[String] = None,
        `type`: _root_.scala.Option[String] = None,
        sort: String = "full_name",
        direction: String = "asc"
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.github.v0.models.Repository]] = {
        val queryParameters = Seq(
          Some("visibility" -> visibility.toString),
          affiliation.map("affiliation" -> _),
          `type`.map("type" -> _),
          Some("sort" -> sort),
          Some("direction" -> direction)
        ).flatten

        _executeRequest("GET", s"/user/repos", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.github.v0.Client.parseJson("Seq[io.flow.github.v0.models.Repository]", r, _.validate[Seq[io.flow.github.v0.models.Repository]])
          case r if r.status == 401 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.github.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }

      override def getUsersAndReposByUsername(
        username: String,
        `type`: String = "owner",
        sort: String = "full_name",
        direction: String = "asc"
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.github.v0.models.Repository]] = {
        val queryParameters = Seq(
          Some("type" -> `type`),
          Some("sort" -> sort),
          Some("direction" -> direction)
        ).flatten

        _executeRequest("GET", s"/users/${play.utils.UriEncoding.encodePathSegment(username, "UTF-8")}/repos", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.github.v0.Client.parseJson("Seq[io.flow.github.v0.models.Repository]", r, _.validate[Seq[io.flow.github.v0.models.Repository]])
          case r if r.status == 401 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.github.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
        }
      }

      override def getOrgsAndReposByOrg(
        org: String,
        `type`: String = "all",
        sort: String = "full_name",
        direction: String = "asc"
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.github.v0.models.Repository]] = {
        val queryParameters = Seq(
          Some("type" -> `type`),
          Some("sort" -> sort),
          Some("direction" -> direction)
        ).flatten

        _executeRequest("GET", s"/orgs/${play.utils.UriEncoding.encodePathSegment(org, "UTF-8")}/repos", queryParameters = queryParameters).map {
          case r if r.status == 200 => _root_.io.flow.github.v0.Client.parseJson("Seq[io.flow.github.v0.models.Repository]", r, _.validate[Seq[io.flow.github.v0.models.Repository]])
          case r if r.status == 401 => throw new io.flow.github.v0.errors.UnitResponse(r.status)
          case r => throw new io.flow.github.v0.errors.FailedRequest(r.status, s"Unsupported response code[${r.status}]. Expected: 200, 401")
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
          throw new io.flow.github.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
        }
      }
    }

  }

  sealed trait Authorization
  object Authorization {
    case class Basic(username: String, password: Option[String] = None) extends Authorization
  }

  trait Contents {
    def getReposAndReadmeByOwnerAndRepo(
      owner: String,
      repo: String,
      ref: String = "master"
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.github.v0.models.Contents]

    def getReposByOwnerAndRepoAndPath(
      owner: String,
      repo: String,
      path: String,
      ref: String = "master"
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.flow.github.v0.models.Contents]
  }

  trait Repositories {
    def getUserAndRepos(
      visibility: io.flow.github.v0.models.Visibility = io.flow.github.v0.models.Visibility("all"),
      affiliation: _root_.scala.Option[String] = None,
      `type`: _root_.scala.Option[String] = None,
      sort: String = "full_name",
      direction: String = "asc"
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.github.v0.models.Repository]]

    /**
     * List public repositories for the specified user.
     */
    def getUsersAndReposByUsername(
      username: String,
      `type`: String = "owner",
      sort: String = "full_name",
      direction: String = "asc"
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.github.v0.models.Repository]]

    /**
     * List repositories for the specified org.
     */
    def getOrgsAndReposByOrg(
      org: String,
      `type`: String = "all",
      sort: String = "full_name",
      direction: String = "asc"
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.flow.github.v0.models.Repository]]
  }

  package errors {

    import io.flow.github.v0.models.json._

    case class UnitResponse(status: Int) extends Exception(s"HTTP $status")

    case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends Exception(s"HTTP $responseCode: $message")

  }

}