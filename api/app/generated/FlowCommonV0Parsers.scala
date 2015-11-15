import anorm._

package io.flow.common.v0.anorm {

  package parsers {

    object Calendar {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.calendar")

      def parser(name: String): RowParser[io.flow.common.v0.models.Calendar] = {
        SqlParser.str(name) map {
          case value => io.flow.common.v0.models.Calendar(value)
        }
      }

    }

    object Capability {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.capability")

      def parser(name: String): RowParser[io.flow.common.v0.models.Capability] = {
        SqlParser.str(name) map {
          case value => io.flow.common.v0.models.Capability(value)
        }
      }

    }

    object ScheduleExceptionStatus {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.schedule_exception_status")

      def parser(name: String): RowParser[io.flow.common.v0.models.ScheduleExceptionStatus] = {
        SqlParser.str(name) map {
          case value => io.flow.common.v0.models.ScheduleExceptionStatus(value)
        }
      }

    }

    object UnitOfMeasurement {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.unit_of_measurement")

      def parser(name: String): RowParser[io.flow.common.v0.models.UnitOfMeasurement] = {
        SqlParser.str(name) map {
          case value => io.flow.common.v0.models.UnitOfMeasurement(value)
        }
      }

    }

    object ValueAddedService {

      def newParser(name: String) = parser(name)

      def parserByTable(table: String) = parser(s"$table.value_added_service")

      def parser(name: String): RowParser[io.flow.common.v0.models.ValueAddedService] = {
        SqlParser.str(name) map {
          case value => io.flow.common.v0.models.ValueAddedService(value)
        }
      }

    }

    object Address {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            address = s"${prefix}_address"
          )
        }
      }

      def parserByTable(table: String) = parser(
        address = s"$table.address"
      )

      def parser(
        address: String
      ): RowParser[io.flow.common.v0.models.Address] = {
        SqlParser.str(address) map {
          case address => {
            io.flow.common.v0.models.Address(
              address = address
            )
          }
        }
      }

    }

    object Audit {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            createdAt = s"${prefix}_created_at",
            createdBy = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_created_by"),
            updatedAt = s"${prefix}_updated_at",
            updatedBy = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${prefix}_updated_by")
          )
        }
      }

      def parserByTable(table: String) = parser(
        createdAt = s"$table.created_at",
        createdBy = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_created_by"),
        updatedAt = s"$table.updated_at",
        updatedBy = me.apidoc.lib.anorm.parsers.util.Config.Prefix(s"${table}_updated_by")
      )

      def parser(
        createdAt: String,
        createdBy: me.apidoc.lib.anorm.parsers.util.Config,
        updatedAt: String,
        updatedBy: me.apidoc.lib.anorm.parsers.util.Config
      ): RowParser[io.flow.common.v0.models.Audit] = {
        SqlParser.get[_root_.org.joda.time.DateTime](createdAt) ~
        io.flow.common.v0.anorm.parsers.Reference.newParser(createdBy) ~
        SqlParser.get[_root_.org.joda.time.DateTime](updatedAt) ~
        io.flow.common.v0.anorm.parsers.Reference.newParser(updatedBy) map {
          case createdAt ~ createdBy ~ updatedAt ~ updatedBy => {
            io.flow.common.v0.models.Audit(
              createdAt = createdAt,
              createdBy = createdBy,
              updatedAt = updatedAt,
              updatedBy = updatedBy
            )
          }
        }
      }

    }

    object DatetimeRange {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            from = s"${prefix}_from",
            to = s"${prefix}_to"
          )
        }
      }

      def parserByTable(table: String) = parser(
        from = s"$table.from",
        to = s"$table.to"
      )

      def parser(
        from: String,
        to: String
      ): RowParser[io.flow.common.v0.models.DatetimeRange] = {
        SqlParser.get[_root_.org.joda.time.DateTime](from) ~
        SqlParser.get[_root_.org.joda.time.DateTime](to) map {
          case from ~ to => {
            io.flow.common.v0.models.DatetimeRange(
              from = from,
              to = to
            )
          }
        }
      }

    }

    object Dimension {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            value = s"${prefix}_value",
            units = s"${prefix}_units"
          )
        }
      }

      def parserByTable(table: String) = parser(
        value = s"$table.value",
        units = s"$table.units"
      )

      def parser(
        value: String,
        units: String
      ): RowParser[io.flow.common.v0.models.Dimension] = {
        SqlParser.get[Double](value) ~
        io.flow.common.v0.anorm.parsers.UnitOfMeasurement.newParser(units) map {
          case value ~ units => {
            io.flow.common.v0.models.Dimension(
              value = value,
              units = units
            )
          }
        }
      }

    }

    object Error {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            code = s"${prefix}_code",
            message = s"${prefix}_message"
          )
        }
      }

      def parserByTable(table: String) = parser(
        code = s"$table.code",
        message = s"$table.message"
      )

      def parser(
        code: String,
        message: String
      ): RowParser[io.flow.common.v0.models.Error] = {
        SqlParser.str(code) ~
        SqlParser.str(message) map {
          case code ~ message => {
            io.flow.common.v0.models.Error(
              code = code,
              message = message
            )
          }
        }
      }

    }

    object Healthcheck {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            status = s"${prefix}_status"
          )
        }
      }

      def parserByTable(table: String) = parser(
        status = s"$table.status"
      )

      def parser(
        status: String
      ): RowParser[io.flow.common.v0.models.Healthcheck] = {
        SqlParser.str(status) map {
          case status => {
            io.flow.common.v0.models.Healthcheck(
              status = status
            )
          }
        }
      }

    }

    object Price {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            amount = s"${prefix}_amount",
            currency = s"${prefix}_currency"
          )
        }
      }

      def parserByTable(table: String) = parser(
        amount = s"$table.amount",
        currency = s"$table.currency"
      )

      def parser(
        amount: String,
        currency: String
      ): RowParser[io.flow.common.v0.models.Price] = {
        SqlParser.get[BigDecimal](amount) ~
        SqlParser.str(currency) map {
          case amount ~ currency => {
            io.flow.common.v0.models.Price(
              amount = amount,
              currency = currency
            )
          }
        }
      }

    }

    object Reference {

      def newParser(config: me.apidoc.lib.anorm.parsers.util.Config) = {
        config match {
          case me.apidoc.lib.anorm.parsers.util.Config.Prefix(prefix) => parser(
            guid = s"${prefix}_guid"
          )
        }
      }

      def parserByTable(table: String) = parser(
        guid = s"$table.guid"
      )

      def parser(
        guid: String
      ): RowParser[io.flow.common.v0.models.Reference] = {
        SqlParser.get[_root_.java.util.UUID](guid) map {
          case guid => {
            io.flow.common.v0.models.Reference(
              guid = guid
            )
          }
        }
      }

    }

  }

}