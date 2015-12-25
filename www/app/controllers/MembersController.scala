package controllers

import com.bryzek.dependency.v0.errors.UnitResponse
import com.bryzek.dependency.v0.models.{Membership, MembershipForm, Role}
import com.bryzek.dependency.www.lib.DependencyClientProvider
import io.flow.user.v0.models.User
import io.flow.play.clients.UserTokensClient
import io.flow.play.util.{Pagination, PaginatedCollection}
import java.util.UUID
import scala.concurrent.Future

import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

class MembersController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val userTokensClient: UserTokensClient,
  override val dependencyClientProvider: DependencyClientProvider
) extends BaseController(userTokensClient, dependencyClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = Some(com.bryzek.dependency.www.lib.Section.Members)

  def index(orgKey: String, page: Int = 0) = Identified.async { implicit request =>
    withOrganization(request, orgKey) { org =>
      for {
        memberships <- dependencyClient(request).memberships.get(
          organization = Some(org.key),
          limit = Pagination.DefaultLimit+1,
          offset = page * Pagination.DefaultLimit
        )
      } yield {
        Ok(
          views.html.members.index(
            uiData(request).copy(organization = Some(org.key)),
            org,
            PaginatedCollection(page, memberships)
          )
      )
      }
    }
  }

  def create(orgKey: String) = Identified.async { implicit request =>
    withOrganization(request, orgKey) { org =>
      Future {
        Ok(
          views.html.members.create(
            uiData(request).copy(organization = Some(org.key)),
            org,
            MembersController.uiForm
          )
        )
      }
    }
  }

  def postCreate(orgKey: String) = Identified.async { implicit request =>
    withOrganization(request, orgKey) { org =>
      val boundForm = MembersController.uiForm.bindFromRequest

      organizations(request).flatMap { orgs =>
        boundForm.fold (

          formWithErrors => Future {
            Ok(views.html.members.create(uiData(request).copy(organization = Some(org.key)), org, formWithErrors))
          },

          uiForm => {
            dependencyClient(request).users.get(email = Some(uiForm.email)).flatMap { users =>
              users.headOption match {
                case None => Future {
                  Ok(views.html.members.create(uiData(request).copy(
                    organization = Some(org.key)), org, boundForm, Seq("User with specified email not found"))
                  )
                }
                case Some(user) => {
                  dependencyClient(request).memberships.post(
                    MembershipForm(
                      organization = org.key,
                      userGuid = user.guid,
                      role = Role(uiForm.role)
                    )
                  ).map { membership =>
                    Redirect(routes.MembersController.index(org.key)).flashing("success" -> s"User added as ${membership.role}")
                  }.recover {
                    case response: com.bryzek.dependency.v0.errors.ErrorsResponse => {
                      Ok(views.html.members.create(
                        uiData(request).copy(organization = Some(org.key)), org, boundForm, response.errors.map(_.message))
                      )
                    }
                  }
                }
              }
            }
          }
        )
      }
    }
  }

  def postDelete(orgKey: String, guid: UUID) = Identified.async { implicit request =>
    withOrganization(request, orgKey) { org =>
      dependencyClient(request).memberships.deleteByGuid(guid).map { response =>
        Redirect(routes.MembersController.index(org.key)).flashing("success" -> s"Membership deleted")
      }.recover {
        case UnitResponse(404) => {
          Redirect(routes.MembersController.index(org.key)).flashing("warning" -> s"Membership not found")
        }
      }
    }
  }

}

object MembersController {

  case class UiForm(
    role: String,
    email: String
  )

  private val uiForm = Form(
    mapping(
      "role" -> nonEmptyText,
      "email" -> nonEmptyText
    )(UiForm.apply)(UiForm.unapply)
  )

}
