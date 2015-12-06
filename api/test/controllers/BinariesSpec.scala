package controllers

import com.bryzek.dependency.v0.{Authorization, Client}
import com.bryzek.dependency.v0.models.BinaryForm
import io.flow.play.util.Validation

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class BinariesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val binary1 = createBinary()
  lazy val binary2 = createBinary()

  "GET /binaries by guid" in new WithServer(port=port) {
    await(
      client.binaries.get(guid = Some(binary1.guid))
    ).map(_.guid) must beEqualTo(
      Seq(binary1.guid)
    )

    await(
      client.binaries.get(guid = Some(UUID.randomUUID))
    ).map(_.guid) must be(
      Nil
    )
  }

  "GET /binaries by name" in new WithServer(port=port) {
    await(
      client.binaries.get(name = Some(binary1.name.toString))
    ).map(_.name) must beEqualTo(
      Seq(binary1.name)
    )

    await(
      client.binaries.get(name = Some(binary1.name.toString.toUpperCase))
    ).map(_.name) must beEqualTo(
      Seq(binary1.name)
    )

    await(
      client.binaries.get(name = Some(UUID.randomUUID.toString))
    ) must be(
      Nil
    )
  }

  "GET /binaries/:guid" in new WithServer(port=port) {
    await(client.binaries.getByGuid(binary1.guid)).guid must beEqualTo(binary1.guid)
    await(client.binaries.getByGuid(binary2.guid)).guid must beEqualTo(binary2.guid)

    expectNotFound {
      client.binaries.getByGuid(UUID.randomUUID)
    }
  }

  "POST /binaries" in new WithServer(port=port) {
    val form = createBinaryForm()
    val binary = await(client.binaries.post(form))
    binary.name.toString must beEqualTo(form.name)
  }

  "POST /binaries validates duplicate name" in new WithServer(port=port) {
    expectErrors(
      client.binaries.post(createBinaryForm().copy(name = binary1.name.toString))
    ).errors.map(_.message) must beEqualTo(
      Seq("Binary with this name already exists")
    )
  }

  "DELETE /binaries" in new WithServer(port=port) {
    val binary = createBinary()
    await(
      client.binaries.deleteByGuid(binary.guid)
    ) must beEqualTo(())

    expectNotFound(
      client.binaries.getByGuid(binary.guid)
    )

    expectNotFound(
      client.binaries.deleteByGuid(binary.guid)
    )
  }

}
