package db

import slick.jdbc.MySQLProfile.api.*

object Database:
  val db: slick.jdbc.MySQLProfile.backend.Database =
    slick.jdbc.MySQLProfile.backend.Database.forConfig("db")

  def initialize(): Unit =
    import scala.concurrent.Await
    import scala.concurrent.duration.*
    val setup = DBIO.seq(
      Tables.users.schema.createIfNotExists,
    )
    Await.result(db.run(setup), 30.seconds)

    // Add new columns for email verification (safe to run repeatedly)
    val addColumns = DBIO.seq(
      sqlu"""ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE""",
      sqlu"""ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255) DEFAULT NULL"""
    )
    Await.result(db.run(addColumns), 30.seconds)
