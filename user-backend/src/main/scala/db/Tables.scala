package db

import slick.jdbc.MySQLProfile.api.*
import java.time.LocalDateTime

case class UserRow(
                    id: Long = 0L,
                    email: String,
                    passwordHash: String,
                    name: String,
                    createdAt: LocalDateTime = LocalDateTime.now()
                  )

class UsersTable(tag: Tag) extends Table[UserRow](tag, "users"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email", O.Unique, O.Length(255))
  def passwordHash = column[String]("password_hash", O.Length(255))
  def name = column[String]("name", O.Length(255))
  def createdAt = column[LocalDateTime]("created_at")
  def * = (id, email, passwordHash, name, createdAt).mapTo[UserRow]

object Tables:
  val users = TableQuery[UsersTable]