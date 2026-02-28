package db

import slick.jdbc.MySQLProfile.api.*

object Database:
  val db: slick.jdbc.MySQLProfile.backend.Database =
    slick.jdbc.MySQLProfile.backend.Database.forConfig("db")

  def initialize(): Unit =
    import scala.concurrent.Await
    import scala.concurrent.duration.*
    val setup = DBIO.seq(
      Tables.items.schema.createIfNotExists,
    )
    Await.result(db.run(setup), 30.seconds)
