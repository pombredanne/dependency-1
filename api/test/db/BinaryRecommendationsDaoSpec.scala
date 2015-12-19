package db

import com.bryzek.dependency.v0.models.{Binary, Project, BinaryForm, BinaryVersion, Organization}
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class BinaryRecommendationsDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createBinaryWithMultipleVersions(
    org: Organization
  ) (
    implicit versions: Seq[String] = Seq("1.0.0", "1.0.1", "1.0.2")
  ): (Binary, Seq[BinaryVersion]) = {
    val binary = createBinary(org)(createBinaryForm(org))
    versions.map { version =>
      createBinaryVersion(org)(
        binary = binary,
        version = version
      )
    }
    (
      binary,
      BinaryVersionsDao.findAll(binaryGuid = Some(binary.guid), limit = versions.size).reverse
    )
  }

  def addBinaryVersion(project: Project, binaryVersion: BinaryVersion) {
    val projectBinary = create(
      ProjectBinariesDao.upsert(
        systemUser,
        ProjectBinaryForm(
          projectGuid = project.guid,
          name = binaryVersion.binary.name,
          version = binaryVersion.version,
          path = "test.sbt"
        )
      )
    )
    ProjectBinariesDao.setBinary(systemUser, projectBinary, binaryVersion.binary)
  }

  lazy val org = createOrganization()

  "no-op if nothing to upgrade" in {
    val project = createProject(org)
    BinaryRecommendationsDao.forProject(project) must be(Nil)
  }

  "ignores earlier versions of binary" in {
    val (binary, binaryVersions) = createBinaryWithMultipleVersions(org)
    val project = createProject(org)
    addBinaryVersion(project, binaryVersions.last)
    BinaryRecommendationsDao.forProject(project) must be(Nil)
  }

  "with binary to upgrade" in {
    val (binary, binaryVersions) = createBinaryWithMultipleVersions(org)
    val project = createProject(org)
    addBinaryVersion(project, binaryVersions.find(_.version == "1.0.0").get)
    verify(
      BinaryRecommendationsDao.forProject(project),
      Seq(
        BinaryRecommendation(
          binary = binary,
          from = "1.0.0",
          to = binaryVersions.find(_.version == "1.0.2").get,
          latest = binaryVersions.find(_.version == "1.0.2").get
        )
      )
    )
  }

  "Prefers latest production release even when more recent beta release is available" in {
    val (binary, binaryVersions) = createBinaryWithMultipleVersions(org)(
      versions = Seq("1.0.0", "1.0.2-RC1", "1.0.1")
    )
    val project = createProject(org)
    addBinaryVersion(project, binaryVersions.find(_.version == "1.0.0").get)
    verify(
      BinaryRecommendationsDao.forProject(project),
      Seq(
        BinaryRecommendation(
          binary = binary,
          from = "1.0.0",
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
                  sys.error(s"Expected[${b.from} => ${b.to.version}] but got[${a.from} => ${a.to.version}]. For latest version, expected[${b.latest.version}] but got[${a.latest.version}]")
                }
              }
            }
          }
        }
      }
    }
  }

}
