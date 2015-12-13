package db

import com.bryzek.dependency.v0.models.{Project, BinaryForm, BinaryVersion, BinaryRecommendation, Organization}
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class BinaryRecommendationsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createBinaryWithMultipleVersions(
    org: Organization
  ) (
    versions: Seq[String] = Seq("1.0.0", "1.0.1", "1.0.2")
  ): Seq[BinaryVersion] = {
    val binary = createBinary(org)(createBinaryForm(org).copy(version = versions.head))
    versions.drop(1).map { version =>
      createBinaryVersion(org)(
        binary = binary,
        version = version
      )
    }
    BinaryVersionsDao.findAll(binaryGuid = Some(binary.guid), limit = versions.size).reverse
  }

  def addBinaryVersion(project: Project, binaryVersion: BinaryVersion) {
    ProjectsDao.setDependencies(
      systemUser,
      project,
      binaries = Some(
        Seq(
          BinaryForm(
            organizationGuid = project.organization.guid,
            name = binaryVersion.binary.name.toString,
            version = binaryVersion.version
          )
        )
      )
    )
  }

  lazy val org = createOrganization()

  "no-op if nothing to upgrade" in {
    val project = createProject(org)()
    BinaryRecommendationsDao.forProject(project) must be(Nil)
  }

  "ignores earlier versions of binary" in {
    val binaryVersions = createBinaryWithMultipleVersions(org)()
    val project = createProject(org)()
    addBinaryVersion(project, binaryVersions.last)
    BinaryRecommendationsDao.forProject(project) must be(Nil)
  }

  "with binary to upgrade" in {
    val binaryVersions = createBinaryWithMultipleVersions(org)()
    val project = createProject(org)()
    addBinaryVersion(project, binaryVersions.find(_.version == "1.0.0").get)
    verify(
      BinaryRecommendationsDao.forProject(project),
      Seq(
        BinaryRecommendation(
          from = binaryVersions.find(_.version == "1.0.0").get,
          to = binaryVersions.find(_.version == "1.0.2").get,
          latest = binaryVersions.find(_.version == "1.0.2").get
        )
      )
    )
  }

  "Prefers latest production release even when more recent beta release is available" in {
    val binaryVersions = createBinaryWithMultipleVersions(
      org
    ) (
      versions = Seq("1.0.0", "1.0.2-RC1", "1.0.1")
    )
    val project = createProject(org)()
    addBinaryVersion(project, binaryVersions.find(_.version == "1.0.0").get)
    verify(
      BinaryRecommendationsDao.forProject(project),
      Seq(
        BinaryRecommendation(
          from = binaryVersions.find(_.version == "1.0.0").get,
          to = binaryVersions.find(_.version == "1.0.1").get,
          latest = binaryVersions.find(_.version == "1.0.2-RC1").get
        )
      )
    )
  }


  def verify(actual: Seq[BinaryRecommendation], expected: Seq[BinaryRecommendation]) {
    (actual == expected) match {
      case true => {}
      case false => {
        (actual.size == expected.size) match {
          case false => {
            sys.error(s"Expected[${expected.size}] recommendations but got [${actual.size}]")
          }
          case true => {
            (actual zip expected).map { case (a, b) =>
              (a == b) match {
                case true => {}
                case false => {
                  sys.error(s"Expected[${b.from.version} => ${b.to.version}] but got[${a.from.version} => ${a.to.version}]. For latest version, expected[${b.latest.version}] but got[${a.latest.version}]")
                }
              }
            }
          }
        }
      }
    }
  }

}
