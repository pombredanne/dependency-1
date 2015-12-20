package com.bryzek.dependency.actors

import com.bryzek.dependency.api.lib.{DefaultLibraryArtifactProvider, Dependencies, GithubDependencyProviderClient}
import com.bryzek.dependency.v0.models.{Binary, BinaryForm, BinaryType, Library, LibraryForm, Project, ProjectBinary, ProjectLibrary, VersionForm, WatchProjectForm}
import io.flow.play.postgresql.Pager
import db.{Authorization, BinariesDao, LibrariesDao, LibraryVersionsDao, ProjectBinariesDao, ProjectLibrariesDao}
import db.{ProjectsDao, RecommendationsDao, SyncsDao, UsersDao, WatchProjectsDao}
import play.api.Logger
import play.libs.Akka
import akka.actor.Actor
import java.util.UUID
import scala.concurrent.ExecutionContext

object ProjectActor {

  trait Message

  object Messages {
    case class Data(guid: UUID) extends Message
    case object Deleted extends Message
    case object Sync extends Message
    case object SyncCompleted extends Message
    case object Watch extends Message

    case class ProjectLibraryCreated(guid: UUID) extends Message
    case class ProjectLibrarySync(guid: UUID) extends Message
    case class ProjectBinaryCreated(guid: UUID) extends Message

    case class LibrarySynced(guid: UUID) extends Message
    case class BinarySynced(guid: UUID) extends Message
  }

}

class ProjectActor extends Actor with Util {

  implicit val projectExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("project-actor-context")

  var dataProject: Option[Project] = None
  var pendingSync: Option[Boolean] = None

  def receive = {

    case m @ ProjectActor.Messages.Data(guid) => withVerboseErrorHandler(m.toString) {
      dataProject = ProjectsDao.findByGuid(Authorization.All, guid)
    }

    case m @ ProjectActor.Messages.Watch => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        UsersDao.findByGuid(project.audit.createdBy.guid).map { createdBy =>
          WatchProjectsDao.upsert(
            createdBy,
            WatchProjectForm(
              userGuid = project.audit.createdBy.guid,
              projectGuid = project.guid
            )
          )
        }
      }
    }

    case m @ ProjectActor.Messages.ProjectLibraryCreated(guid) => withVerboseErrorHandler(m.toString) {
      syncProjectLibrary(guid)
    }

    case m @ ProjectActor.Messages.ProjectLibrarySync(guid) => withVerboseErrorHandler(m.toString) {
      syncProjectLibrary(guid)
    }

    case m @ ProjectActor.Messages.ProjectBinaryCreated(guid) => withVerboseErrorHandler(m.toString) {
      SyncsDao.withStartedAndCompleted(MainActor.SystemUser, "project_binary", guid) {
        dataProject.foreach { project =>
          ProjectBinariesDao.findByGuid(Authorization.All, guid).map { projectBinary =>
            resolveBinary(projectBinary).map { binary =>
              ProjectBinariesDao.setBinary(MainActor.SystemUser, projectBinary, binary)
            }
          }
          processPendingSync(project)
        }
      }
    }
  
    case m @ ProjectActor.Messages.LibrarySynced(guid) => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        processPendingSync(project)
      }
    }

    case m @ ProjectActor.Messages.BinarySynced(guid) => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        processPendingSync(project)
      }
    }

    case m @ ProjectActor.Messages.Sync => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        SyncsDao.recordStarted(MainActor.SystemUser, "project", project.guid)

        val user = UsersDao.findByGuid(project.audit.createdBy.guid).map { user =>
          val summary = ProjectsDao.toSummary(project)

          UsersDao.findByGuid(project.audit.createdBy.guid).map { user =>
            GithubDependencyProviderClient.instance(summary, user).dependencies(project).map { dependencies =>
              println(s" - project[${project.guid}] name[${project.name}] dependencies: $dependencies")

              dependencies.binaries.map(_.map { form =>
                println(s" -- project binaries dao upsert")
                ProjectBinariesDao.upsert(user, form) match {
                  case Left(errors) => {
                    Logger.error(s"Project[${project.name}] guid[${project.guid}] Error storing binary[$form]: " + errors.mkString(", "))
                  }
                  case Right(_) => {}
                }
              })

              dependencies.librariesAndPlugins.map(_.map { artifact =>
                println(s" -- project artifact upsert: " + artifact)
                ProjectLibrariesDao.upsert(
                  user,
                  artifact.toProjectLibraryForm(
                    crossBuildVersion = dependencies.crossBuildVersion()
                  )
                ) match {
                  case Left(errors) => {
                    Logger.error(s"Project[${project.name}] guid[${project.guid}] Error storing artifact[$artifact]: " + errors.mkString(", "))
                  }
                  case Right(_) => {}
                }
              })

              pendingSync = Some(true)
              processPendingSync(project)
            }.recover {
              case e => {
                e.printStackTrace(                System.err)
                Logger.error(s"Error fetching dependencies for project[${project.guid}] name[${project.name}]: $e")
              }
            }
          }

        }
      }
    }

    case m @ ProjectActor.Messages.Deleted => withVerboseErrorHandler(m.toString) {
      dataProject.foreach { project =>
        Pager.eachPage { offset =>
          RecommendationsDao.findAll(projectGuid = Some(project.guid), offset = offset)
        } { rec =>
          RecommendationsDao.softDelete(MainActor.SystemUser, rec)
        }
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

  /**
    * Attempts to resolve the library. If successful, sets the
    * project_libraries.library_guid
    */
  def syncProjectLibrary(guid: UUID) {
    SyncsDao.withStartedAndCompleted(MainActor.SystemUser, "project_library", guid) {
      dataProject.foreach { project =>
        ProjectLibrariesDao.findByGuid(Authorization.All, guid).map { projectLibrary =>
          resolveLibrary(projectLibrary).map { lib =>
            ProjectLibrariesDao.setLibrary(MainActor.SystemUser, projectLibrary, lib)
          }
        }
        processPendingSync(project)
      }
    }
  }

  def processPendingSync(project: Project) {
    pendingSync.foreach { _ =>
      dependenciesPendingCompletion(project) match {
        case Nil => {
          RecommendationsDao.sync(MainActor.SystemUser, project)
          SyncsDao.recordCompleted(MainActor.SystemUser, "project", project.guid)
          pendingSync = None
        }
        case deps => {
          println(s" -- project[${project.name}] guid[${project.guid}] waiting on dependencies to sync: " + deps.mkString(", "))
        }
      }
    }
  }

  // NB: We don't return ALL dependencies
  private[this] def dependenciesPendingCompletion(project: Project): Seq[String] = {
    ProjectLibrariesDao.findAll(
      Authorization.All,
      projectGuid = Some(project.guid),
      isSynced = Some(false)
    ).map( lib => s"Library ${lib.groupId}.${lib.artifactId}" ) ++ 
    ProjectBinariesDao.findAll(
      Authorization.All,
      projectGuid = Some(project.guid),
      isSynced = Some(false)
    ).map( bin => s"Binary ${bin.name}" )
  }

  private[this] def resolveLibrary(projectLibrary: ProjectLibrary): Option[Library] = {
    LibrariesDao.findByGroupIdAndArtifactId(Authorization.All, projectLibrary.groupId, projectLibrary.artifactId) match {
      case Some(lib) => {
        Some(lib)
      }
      case None => {
        DefaultLibraryArtifactProvider().resolve(
          organization = projectLibrary.project.organization,
          groupId = projectLibrary.groupId,
          artifactId = projectLibrary.artifactId
        ) match {
          case None => {
            None
          }
          case Some(resolution) => {
            LibrariesDao.upsert(
              MainActor.SystemUser,
              form = LibraryForm(
                organizationGuid = projectLibrary.project.organization.guid,
                groupId = projectLibrary.groupId,
                artifactId = projectLibrary.artifactId,
                resolverGuid = resolution.resolver.guid
              )
            ) match {
              case Left(errors) => {
                Logger.error(s"Project[${projectLibrary.project.guid}] name[${projectLibrary.project.name}] - error upserting library: " + errors.mkString(", "))
                None
              }
              case Right(library) => {
                Some(library)
              }
            }
          }
        }
      }
    }
  }

  private[this] def resolveBinary(projectBinary: ProjectBinary): Option[Binary] = {
    println(s"project guid[${projectBinary.project.guid}] projectBinaryCreated[${projectBinary.guid}] name[${projectBinary.name}]")
    BinaryType(projectBinary.name) match {
      case BinaryType.Scala | BinaryType.Sbt => {
        BinariesDao.upsert(
          MainActor.SystemUser,
          BinaryForm(
            organizationGuid = projectBinary.project.organization.guid,
            name = BinaryType(projectBinary.name)
          )
        ) match {
          case Left(errors) => {
            Logger.error(s"Project[${projectBinary.project.guid}] name[${projectBinary.project.name}] - error upserting binary[$projectBinary]: " + errors.mkString(", "))
            None
          }
          case Right(binary) => {
            Some(binary)
          }
        }
      }
      case BinaryType.UNDEFINED(_) => {
        Logger.warn(s"Project[${projectBinary.guid}] name[${projectBinary.name}] references an unknown binary[${projectBinary.name}]")
        None
      }
    }
  }

}
