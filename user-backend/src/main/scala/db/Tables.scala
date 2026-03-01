package db

import slick.jdbc.MySQLProfile.api.*
import java.time.LocalDateTime

case class UserRow(
                    id: Long = 0L,
                    email: String,
                    passwordHash: String,
                    name: String,
                    emailVerified: Boolean = false,
                    verificationToken: Option[String] = None,
                    createdAt: LocalDateTime = LocalDateTime.now()
                  )

class UsersTable(tag: Tag) extends Table[UserRow](tag, "users"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email", O.Unique, O.Length(255))
  def passwordHash = column[String]("password_hash", O.Length(255))
  def name = column[String]("name", O.Length(255))
  def emailVerified = column[Boolean]("email_verified", O.Default(false))
  def verificationToken = column[Option[String]]("verification_token", O.Length(255), O.Default(None))
  def createdAt = column[LocalDateTime]("created_at")
  def * = (id, email, passwordHash, name, emailVerified, verificationToken, createdAt).mapTo[UserRow]

object Tables:
  val users = TableQuery[UsersTable]
