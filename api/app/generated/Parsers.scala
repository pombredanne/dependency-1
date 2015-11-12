import anorm._
import org.joda.time.DateTime
import java.util.UUID

package io.flow.common.v0.anorm {

  object Audit {
    def parser(table: String): RowParser[io.flow.common.v0.models.Audit] = {
      SqlParser.get[DateTime](s"$table.created_at") ~
      SqlParser.get[UUID](s"$table.created_by_guid") ~
      SqlParser.get[DateTime](s"$table.updated_at") ~
      SqlParser.get[UUID](s"$table.updated_by_guid") map {
        case createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid => {
          io.flow.common.v0.models.Audit(
            createdAt = createdAt,
            createdBy = io.flow.common.v0.models.Reference(createdByGuid),
            updatedAt = updatedAt,
            updatedBy = io.flow.common.v0.models.Reference(updatedByGuid)
          )
        }
      }
    }
  }

}

package com.bryzek.dependency.v0.anorm {

  object Language {
    def parser(table: String): RowParser[com.bryzek.dependency.v0.models.Language] = {
      SqlParser.get[UUID](s"$table.guid") ~
      SqlParser.get[String](s"$table.name") ~
      io.flow.common.v0.anorm.Audit.parser(table) map {
        case guid ~ name ~ audit => {
          com.bryzek.dependency.v0.models.Language(
            guid = guid,
            name = com.bryzek.dependency.v0.models.ProgrammingLanguage(name),
            audit = audit
          )
        }
      }
    }
  }

}
