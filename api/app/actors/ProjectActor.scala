package com.bryzek.dependency.actors

import com.bryzek.dependency.api.lib.{DefaultLibraryArtifactProvider, Dependencies, GithubDependencyProviderClient}
import com.bryzek.dependency.v0.models.{LibraryForm, Project, VersionForm, WatchProjectForm}
import io.flow.play.postgresql.Pager
import db.{Authorization, BinariesDao, LibrariesDao, LibraryVersionsDao, ProjectLibrariesDao}
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
      SyncsDao.withStartedAndCompleted(MainActor.SystemUser, guid) {
        dataProject.foreach { project =>
          ProjectLibrariesDao.findByGuid(Authorization.All, guid).map { projectLibrary =>
            println(s"project guid[${project.guid}] projectLibraryCreated[${projectLibrary.guid}] group[${projectLibrary.groupId}] artifact[${projectLibrary.artifactId}]")
            LibrariesDao.findByGroupIdAndArtifactId(Authorization.All, projectLibrary.groupId, projectLibrary.artifactId) match {
              case Some(lib) => {
                println("  -- found existing lib: " + lib)
                ProjectLibrariesDao.setLibrary(MainActor.SystemUser, projectLibrary, lib)
              }
              case None => {
                DefaultLibraryArtifactProvider().resolve(
                  organization = project.organization,
                  groupId = projectLibrary.groupId,
                  artifactId = projectLibrary.artifactId
                ) match {
                  case None => {
                    println(s"  -- Could not resolve library")
                  }
                  case Some(resolution) => {
                    println(s"  -- resolved library: $resolution")
                    LibrariesDao.upsert(
                      MainActor.SystemUser,
                      form = LibraryForm(
                        organizationGuid = project.organization.guid,
                        groupId = projectLibrary.groupId,
                        artifactId = projectLibrary.artifactId,
                        resolverGuid = resolution.resolver.guid
                      )
                    ) match {
                      case Left(errors) => {
                        Logger.error(s"Project[${project.guid}] name[${project.name}] - error upserting library: " + errors.mkString(", "))
                      }
                      case Right(library) => {
                        ProjectLibrariesDao.setLibrary(MainActor.SystemUser, projectLibrary, library)
                        resolution.versions.foreach { version =>
                          LibraryVersionsDao.upsert(
                            createdBy = MainActor.SystemUser,
                            libraryGuid = library.guid,
                            form = VersionForm(version.tag.value, version.crossBuildVersion.map(_.value))
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
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
        SyncsDao.recordStarted(MainActor.SystemUser, project.guid)

        val user = UsersDao.findByGuid(project.audit.createdBy.guid).map { user =>
          val summary = ProjectsDao.toSummary(project)

          UsersDao.findByGuid(project.audit.createdBy.guid).map { user =>
            GithubDependencyProviderClient.instance(summary, user).dependencies(project).map { dependencies =>
              println(s" - project[${project.guid}] name[${project.name}] dependencies: $dependencies")

              ProjectsDao.setDependencies(
                createdBy = MainActor.SystemUser,
                project = project,
                binaries = dependencies.binaries
              )

              dependencies.librariesAndPlugins.map(_.map { artifact =>
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

  def processPendingSync(project: Project) {
    pendingSync.foreach { _ =>
      dependenciesPendingCompletion(project) match {
        case Nil => {
          RecommendationsDao.sync(MainActor.SystemUser, project)
          SyncsDao.recordCompleted(MainActor.SystemUser, project.guid)
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
    BinariesDao.findAll(
      Authorization.All,
      projectGuid = Some(project.guid),
      isSynced = Some(false)
    ).map( bin => s"Binary ${bin.name}" )
  }

}
