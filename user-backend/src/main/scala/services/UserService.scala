package services

import cats.data.ValidatedNel
import cats.syntax.all.*
import db.{Database, Tables, UserRow}
import org.mindrot.jbcrypt.BCrypt
import slick.jdbc.MySQLProfile.api.*

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

object UserService:
  private val db = Database.db

  private def validateRegistrationData(email: String, password: String, name: String): ValidatedNel[String, (String, String, String)] = {
    val validEmail =
      if (email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) email.validNel
      else "Invalid email format".invalidNel

    val validPassword =
      if (password.length >= 8) password.validNel
      else "Password must be at least 8 characters long".invalidNel

    val validName =
      if (name.nonEmpty) name.validNel
      else "Name cannot be empty".invalidNel

    (validEmail, validPassword, validName).mapN((_, _, _))
  }

  def register(email: String, password: String, name: String): Either[String, UserRow] =
    validateRegistrationData(email, password, name).toEither.leftMap(_.toList.mkString(", ")) match
      case Left(errors) => Left(errors)
      case Right((validEmail, validPassword, validName)) =>
        val existing = Await.result(
          db.run(Tables.users.filter(_.email === validEmail).result.headOption),
          10.seconds
        )
        if existing.isDefined then
          Left("A user with this email already exists")
        else
          val hash = BCrypt.hashpw(validPassword, BCrypt.gensalt())
          val row = UserRow(email = validEmail, passwordHash = hash, name = validName)
          val insertAction = (Tables.users returning Tables.users.map(_.id) into ((user, id) => user.copy(id = id))) += row
          val created = Await.result(db.run(insertAction), 10.seconds)
          Right(created)

  def authenticate(email: String, password: String): Option[UserRow] =
    val userOpt = Await.result(
      db.run(Tables.users.filter(_.email === email).result.headOption),
      10.seconds
    )
    userOpt.filter(u => BCrypt.checkpw(password, u.passwordHash))

  def findById(id: Long): Option[UserRow] =
    Await.result(
      db.run(Tables.users.filter(_.id === id).result.headOption),
      10.seconds
    )

  def findByName(name: String): Option[UserRow] =
    Await.result(
      db.run(Tables.users.filter(_.name === name).result.headOption),
      10.seconds
    )