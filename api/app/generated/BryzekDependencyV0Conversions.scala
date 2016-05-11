/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.4.5
 * apidoc:0.11.24 http://www.apidoc.me/bryzek/dependency/0.4.5/anorm_2_x_parsers
 */
package com.bryzek.dependency.v0.anorm.conversions {

  import anorm.{Column, MetaDataItem, TypeDoesNotMatch}
  import play.api.libs.json.{JsArray, JsObject, JsValue}
  import scala.util.{Failure, Success, Try}

  /**
    * Conversions to collections of objects using JSON.
    */
  object Util {

    def parser[T](
      f: play.api.libs.json.JsValue => T
    ) = anorm.Column.nonNull { (value, meta) =>
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

  }

  object Types {
    import com.bryzek.dependency.v0.models.json._
    implicit val columnToSeqDependencyBinaryType: Column[Seq[_root_.com.bryzek.dependency.v0.models.BinaryType]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.BinaryType]] }
    implicit val columnToMapDependencyBinaryType: Column[Map[String, _root_.com.bryzek.dependency.v0.models.BinaryType]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.BinaryType]] }
    implicit val columnToSeqDependencyPublication: Column[Seq[_root_.com.bryzek.dependency.v0.models.Publication]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Publication]] }
    implicit val columnToMapDependencyPublication: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Publication]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Publication]] }
    implicit val columnToSeqDependencyRecommendationType: Column[Seq[_root_.com.bryzek.dependency.v0.models.RecommendationType]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.RecommendationType]] }
    implicit val columnToMapDependencyRecommendationType: Column[Map[String, _root_.com.bryzek.dependency.v0.models.RecommendationType]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.RecommendationType]] }
    implicit val columnToSeqDependencyRole: Column[Seq[_root_.com.bryzek.dependency.v0.models.Role]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Role]] }
    implicit val columnToMapDependencyRole: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Role]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Role]] }
    implicit val columnToSeqDependencyScms: Column[Seq[_root_.com.bryzek.dependency.v0.models.Scms]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Scms]] }
    implicit val columnToMapDependencyScms: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Scms]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Scms]] }
    implicit val columnToSeqDependencySyncEvent: Column[Seq[_root_.com.bryzek.dependency.v0.models.SyncEvent]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.SyncEvent]] }
    implicit val columnToMapDependencySyncEvent: Column[Map[String, _root_.com.bryzek.dependency.v0.models.SyncEvent]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.SyncEvent]] }
    implicit val columnToSeqDependencyVisibility: Column[Seq[_root_.com.bryzek.dependency.v0.models.Visibility]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Visibility]] }
    implicit val columnToMapDependencyVisibility: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Visibility]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Visibility]] }
    implicit val columnToSeqDependencyBinary: Column[Seq[_root_.com.bryzek.dependency.v0.models.Binary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Binary]] }
    implicit val columnToMapDependencyBinary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Binary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Binary]] }
    implicit val columnToSeqDependencyBinaryForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.BinaryForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.BinaryForm]] }
    implicit val columnToMapDependencyBinaryForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.BinaryForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.BinaryForm]] }
    implicit val columnToSeqDependencyBinarySummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.BinarySummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.BinarySummary]] }
    implicit val columnToMapDependencyBinarySummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.BinarySummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.BinarySummary]] }
    implicit val columnToSeqDependencyBinaryVersion: Column[Seq[_root_.com.bryzek.dependency.v0.models.BinaryVersion]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.BinaryVersion]] }
    implicit val columnToMapDependencyBinaryVersion: Column[Map[String, _root_.com.bryzek.dependency.v0.models.BinaryVersion]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.BinaryVersion]] }
    implicit val columnToSeqDependencyGithubAuthenticationForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.GithubAuthenticationForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.GithubAuthenticationForm]] }
    implicit val columnToMapDependencyGithubAuthenticationForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.GithubAuthenticationForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.GithubAuthenticationForm]] }
    implicit val columnToSeqDependencyGithubUser: Column[Seq[_root_.com.bryzek.dependency.v0.models.GithubUser]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.GithubUser]] }
    implicit val columnToMapDependencyGithubUser: Column[Map[String, _root_.com.bryzek.dependency.v0.models.GithubUser]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.GithubUser]] }
    implicit val columnToSeqDependencyGithubUserForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.GithubUserForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.GithubUserForm]] }
    implicit val columnToMapDependencyGithubUserForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.GithubUserForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.GithubUserForm]] }
    implicit val columnToSeqDependencyGithubWebhook: Column[Seq[_root_.com.bryzek.dependency.v0.models.GithubWebhook]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.GithubWebhook]] }
    implicit val columnToMapDependencyGithubWebhook: Column[Map[String, _root_.com.bryzek.dependency.v0.models.GithubWebhook]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.GithubWebhook]] }
    implicit val columnToSeqDependencyItem: Column[Seq[_root_.com.bryzek.dependency.v0.models.Item]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Item]] }
    implicit val columnToMapDependencyItem: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Item]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Item]] }
    implicit val columnToSeqDependencyLibrary: Column[Seq[_root_.com.bryzek.dependency.v0.models.Library]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Library]] }
    implicit val columnToMapDependencyLibrary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Library]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Library]] }
    implicit val columnToSeqDependencyLibraryForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.LibraryForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.LibraryForm]] }
    implicit val columnToMapDependencyLibraryForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.LibraryForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.LibraryForm]] }
    implicit val columnToSeqDependencyLibrarySummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.LibrarySummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.LibrarySummary]] }
    implicit val columnToMapDependencyLibrarySummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.LibrarySummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.LibrarySummary]] }
    implicit val columnToSeqDependencyLibraryVersion: Column[Seq[_root_.com.bryzek.dependency.v0.models.LibraryVersion]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.LibraryVersion]] }
    implicit val columnToMapDependencyLibraryVersion: Column[Map[String, _root_.com.bryzek.dependency.v0.models.LibraryVersion]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.LibraryVersion]] }
    implicit val columnToSeqDependencyMembership: Column[Seq[_root_.com.bryzek.dependency.v0.models.Membership]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Membership]] }
    implicit val columnToMapDependencyMembership: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Membership]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Membership]] }
    implicit val columnToSeqDependencyMembershipForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.MembershipForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.MembershipForm]] }
    implicit val columnToMapDependencyMembershipForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.MembershipForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.MembershipForm]] }
    implicit val columnToSeqDependencyOrganization: Column[Seq[_root_.com.bryzek.dependency.v0.models.Organization]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Organization]] }
    implicit val columnToMapDependencyOrganization: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Organization]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Organization]] }
    implicit val columnToSeqDependencyOrganizationForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.OrganizationForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.OrganizationForm]] }
    implicit val columnToMapDependencyOrganizationForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.OrganizationForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.OrganizationForm]] }
    implicit val columnToSeqDependencyOrganizationSummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.OrganizationSummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.OrganizationSummary]] }
    implicit val columnToMapDependencyOrganizationSummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.OrganizationSummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.OrganizationSummary]] }
    implicit val columnToSeqDependencyProject: Column[Seq[_root_.com.bryzek.dependency.v0.models.Project]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Project]] }
    implicit val columnToMapDependencyProject: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Project]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Project]] }
    implicit val columnToSeqDependencyProjectBinary: Column[Seq[_root_.com.bryzek.dependency.v0.models.ProjectBinary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ProjectBinary]] }
    implicit val columnToMapDependencyProjectBinary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectBinary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectBinary]] }
    implicit val columnToSeqDependencyProjectDetail: Column[Seq[_root_.com.bryzek.dependency.v0.models.ProjectDetail]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ProjectDetail]] }
    implicit val columnToMapDependencyProjectDetail: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectDetail]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectDetail]] }
    implicit val columnToSeqDependencyProjectForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.ProjectForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ProjectForm]] }
    implicit val columnToMapDependencyProjectForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectForm]] }
    implicit val columnToSeqDependencyProjectLibrary: Column[Seq[_root_.com.bryzek.dependency.v0.models.ProjectLibrary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ProjectLibrary]] }
    implicit val columnToMapDependencyProjectLibrary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectLibrary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectLibrary]] }
    implicit val columnToSeqDependencyProjectPatchForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.ProjectPatchForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ProjectPatchForm]] }
    implicit val columnToMapDependencyProjectPatchForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectPatchForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectPatchForm]] }
    implicit val columnToSeqDependencyProjectSummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.ProjectSummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ProjectSummary]] }
    implicit val columnToMapDependencyProjectSummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectSummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ProjectSummary]] }
    implicit val columnToSeqDependencyRecommendation: Column[Seq[_root_.com.bryzek.dependency.v0.models.Recommendation]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Recommendation]] }
    implicit val columnToMapDependencyRecommendation: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Recommendation]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Recommendation]] }
    implicit val columnToSeqDependencyReference: Column[Seq[_root_.com.bryzek.dependency.v0.models.Reference]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Reference]] }
    implicit val columnToMapDependencyReference: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Reference]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Reference]] }
    implicit val columnToSeqDependencyRepository: Column[Seq[_root_.com.bryzek.dependency.v0.models.Repository]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Repository]] }
    implicit val columnToMapDependencyRepository: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Repository]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Repository]] }
    implicit val columnToSeqDependencyResolver: Column[Seq[_root_.com.bryzek.dependency.v0.models.Resolver]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Resolver]] }
    implicit val columnToMapDependencyResolver: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Resolver]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Resolver]] }
    implicit val columnToSeqDependencyResolverForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.ResolverForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ResolverForm]] }
    implicit val columnToMapDependencyResolverForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ResolverForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ResolverForm]] }
    implicit val columnToSeqDependencyResolverSummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.ResolverSummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ResolverSummary]] }
    implicit val columnToMapDependencyResolverSummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ResolverSummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ResolverSummary]] }
    implicit val columnToSeqDependencySubscription: Column[Seq[_root_.com.bryzek.dependency.v0.models.Subscription]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Subscription]] }
    implicit val columnToMapDependencySubscription: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Subscription]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Subscription]] }
    implicit val columnToSeqDependencySubscriptionForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.SubscriptionForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.SubscriptionForm]] }
    implicit val columnToMapDependencySubscriptionForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.SubscriptionForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.SubscriptionForm]] }
    implicit val columnToSeqDependencySync: Column[Seq[_root_.com.bryzek.dependency.v0.models.Sync]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Sync]] }
    implicit val columnToMapDependencySync: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Sync]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Sync]] }
    implicit val columnToSeqDependencyToken: Column[Seq[_root_.com.bryzek.dependency.v0.models.Token]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Token]] }
    implicit val columnToMapDependencyToken: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Token]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Token]] }
    implicit val columnToSeqDependencyTokenForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.TokenForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.TokenForm]] }
    implicit val columnToMapDependencyTokenForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.TokenForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.TokenForm]] }
    implicit val columnToSeqDependencyUserForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.UserForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.UserForm]] }
    implicit val columnToMapDependencyUserForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.UserForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.UserForm]] }
    implicit val columnToSeqDependencyUserIdentifier: Column[Seq[_root_.com.bryzek.dependency.v0.models.UserIdentifier]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.UserIdentifier]] }
    implicit val columnToMapDependencyUserIdentifier: Column[Map[String, _root_.com.bryzek.dependency.v0.models.UserIdentifier]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.UserIdentifier]] }
    implicit val columnToSeqDependencyUserSummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.UserSummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.UserSummary]] }
    implicit val columnToMapDependencyUserSummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.UserSummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.UserSummary]] }
    implicit val columnToSeqDependencyUsernamePassword: Column[Seq[_root_.com.bryzek.dependency.v0.models.UsernamePassword]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.UsernamePassword]] }
    implicit val columnToMapDependencyUsernamePassword: Column[Map[String, _root_.com.bryzek.dependency.v0.models.UsernamePassword]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.UsernamePassword]] }
    implicit val columnToSeqDependencyVersionForm: Column[Seq[_root_.com.bryzek.dependency.v0.models.VersionForm]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.VersionForm]] }
    implicit val columnToMapDependencyVersionForm: Column[Map[String, _root_.com.bryzek.dependency.v0.models.VersionForm]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.VersionForm]] }
    implicit val columnToSeqDependencyCredentials: Column[Seq[_root_.com.bryzek.dependency.v0.models.Credentials]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.Credentials]] }
    implicit val columnToMapDependencyCredentials: Column[Map[String, _root_.com.bryzek.dependency.v0.models.Credentials]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.Credentials]] }
    implicit val columnToSeqDependencyItemSummary: Column[Seq[_root_.com.bryzek.dependency.v0.models.ItemSummary]] = Util.parser { _.as[Seq[_root_.com.bryzek.dependency.v0.models.ItemSummary]] }
    implicit val columnToMapDependencyItemSummary: Column[Map[String, _root_.com.bryzek.dependency.v0.models.ItemSummary]] = Util.parser { _.as[Map[String, _root_.com.bryzek.dependency.v0.models.ItemSummary]] }
  }

  object Standard {
    implicit val columnToJsObject: Column[play.api.libs.json.JsObject] = Util.parser { _.as[play.api.libs.json.JsObject] }
    implicit val columnToSeqBoolean: Column[Seq[Boolean]] = Util.parser { _.as[Seq[Boolean]] }
    implicit val columnToMapBoolean: Column[Map[String, Boolean]] = Util.parser { _.as[Map[String, Boolean]] }
    implicit val columnToSeqDouble: Column[Seq[Double]] = Util.parser { _.as[Seq[Double]] }
    implicit val columnToMapDouble: Column[Map[String, Double]] = Util.parser { _.as[Map[String, Double]] }
    implicit val columnToSeqInt: Column[Seq[Int]] = Util.parser { _.as[Seq[Int]] }
    implicit val columnToMapInt: Column[Map[String, Int]] = Util.parser { _.as[Map[String, Int]] }
    implicit val columnToSeqLong: Column[Seq[Long]] = Util.parser { _.as[Seq[Long]] }
    implicit val columnToMapLong: Column[Map[String, Long]] = Util.parser { _.as[Map[String, Long]] }
    implicit val columnToSeqLocalDate: Column[Seq[_root_.org.joda.time.LocalDate]] = Util.parser { _.as[Seq[_root_.org.joda.time.LocalDate]] }
    implicit val columnToMapLocalDate: Column[Map[String, _root_.org.joda.time.LocalDate]] = Util.parser { _.as[Map[String, _root_.org.joda.time.LocalDate]] }
    implicit val columnToSeqDateTime: Column[Seq[_root_.org.joda.time.DateTime]] = Util.parser { _.as[Seq[_root_.org.joda.time.DateTime]] }
    implicit val columnToMapDateTime: Column[Map[String, _root_.org.joda.time.DateTime]] = Util.parser { _.as[Map[String, _root_.org.joda.time.DateTime]] }
    implicit val columnToSeqBigDecimal: Column[Seq[BigDecimal]] = Util.parser { _.as[Seq[BigDecimal]] }
    implicit val columnToMapBigDecimal: Column[Map[String, BigDecimal]] = Util.parser { _.as[Map[String, BigDecimal]] }
    implicit val columnToSeqJsObject: Column[Seq[_root_.play.api.libs.json.JsObject]] = Util.parser { _.as[Seq[_root_.play.api.libs.json.JsObject]] }
    implicit val columnToMapJsObject: Column[Map[String, _root_.play.api.libs.json.JsObject]] = Util.parser { _.as[Map[String, _root_.play.api.libs.json.JsObject]] }
    implicit val columnToSeqString: Column[Seq[String]] = Util.parser { _.as[Seq[String]] }
    implicit val columnToMapString: Column[Map[String, String]] = Util.parser { _.as[Map[String, String]] }
    implicit val columnToSeqUUID: Column[Seq[_root_.java.util.UUID]] = Util.parser { _.as[Seq[_root_.java.util.UUID]] }
    implicit val columnToMapUUID: Column[Map[String, _root_.java.util.UUID]] = Util.parser { _.as[Map[String, _root_.java.util.UUID]] }
  }

}