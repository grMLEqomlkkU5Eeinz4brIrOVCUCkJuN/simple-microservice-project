package services

import cats.data.ValidatedNel
import cats.syntax.all.*
import db.{Database, Tables, UserRow}
import org.mindrot.jbcrypt.BCrypt
import slick.jdbc.MySQLProfile.api.*

import java.util.UUID
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

object UserService:
  private val db = Database.db

  private def validateRegistrationData(email: String, password: String, name: String): ValidatedNel[String, (String, String, String)] = {
    val validEmail =
      if (email.matches("^[\\w.+\\-]+@([\\w\\-]+\\.)+[\\w\\-]{2,}$")) email.validNel
      else "Invalid email format".invalidNel

    val validPassword =
      if (password.length >= 8) password.validNel
      else "Password must be at least 8 characters long".invalidNel

    val validName =
      if (name.nonEmpty) name.validNel
      else "Name cannot be empty".invalidNel

    (validEmail, validPassword, validName).mapN((_, _, _))
  }

  def register(email: String, password: String, name: String): Either[List[String], UserRow] =
    validateRegistrationData(email, password, name).toEither.leftMap(_.toList) match
      case Left(errors) => Left(errors)
      case Right((validEmail, validPassword, validName)) =>
        val existing = Await.result(
          db.run(Tables.users.filter(_.email === validEmail).result.headOption),
          10.seconds
        )
        if existing.isDefined then
          Left(List("A user with this email already exists"))
        else
          val hash = BCrypt.hashpw(validPassword, BCrypt.gensalt())
          val token = UUID.randomUUID().toString
          val tokenExpiry = java.time.LocalDateTime.now().plusHours(24)
          val row = UserRow(email = validEmail, passwordHash = hash, name = validName, verificationToken = Some(token), verificationTokenExpiresAt = Some(tokenExpiry))
          val insertAction = (Tables.users returning Tables.users.map(_.id) into ((user, id) => user.copy(id = id))) += row
          val created = Await.result(db.run(insertAction), 10.seconds)
          EmailService.sendVerificationEmail(validEmail, token)
          Right(created)

  def verifyEmail(token: String): Option[UserRow] =
    val userOpt = Await.result(
      db.run(Tables.users.filter(_.verificationToken === Option(token)).result.headOption),
      10.seconds
    )
    userOpt.flatMap { user =>
      // Check if token has expired
      val isExpired = user.verificationTokenExpiresAt.exists(_.isBefore(java.time.LocalDateTime.now()))
      if isExpired then
        // Clear expired token
        val clearAction = Tables.users
          .filter(_.id === user.id)
          .map(u => (u.verificationToken, u.verificationTokenExpiresAt))
          .update((None, None))
        Await.result(db.run(clearAction), 10.seconds)
        None
      else
        val updateAction = Tables.users
          .filter(_.id === user.id)
          .map(u => (u.emailVerified, u.verificationToken, u.verificationTokenExpiresAt))
          .update((true, None, None))
        Await.result(db.run(updateAction), 10.seconds)
        RedisService.invalidateUser(user.id)
        Some(user.copy(emailVerified = true, verificationToken = None, verificationTokenExpiresAt = None))
    }

  // Pre-computed dummy hash for constant-time comparison when user not found
  private val dummyHash = BCrypt.hashpw("dummy-password-for-timing", BCrypt.gensalt())

  def authenticate(email: String, password: String): Either[String, UserRow] =
    val userOpt = Await.result(
      db.run(Tables.users.filter(_.email === email).result.headOption),
      10.seconds
    )
    userOpt match
      case Some(user) =>
        if !BCrypt.checkpw(password, user.passwordHash) then
          Left("Invalid email or password")
        else if !user.emailVerified then
          Left("Email not verified. Please check your inbox for the verification link.")
        else
          Right(user)
      case None =>
        // Always run BCrypt to prevent timing-based user enumeration
        BCrypt.checkpw(password, dummyHash)
        Left("Invalid email or password")

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

  def findByEmail(email: String): Option[UserRow] =
    Await.result(
      db.run(Tables.users.filter(_.email === email).result.headOption),
      10.seconds
    )

  def searchByName(query: String): Seq[UserRow] =
    Await.result(
      db.run(Tables.users.filter(_.name.toLowerCase like s"%${query.toLowerCase}%").result),
      10.seconds
    )

  def searchByEmail(query: String): Seq[UserRow] =
    Await.result(
      db.run(Tables.users.filter(_.email.toLowerCase like s"%${query.toLowerCase}%").result),
      10.seconds
    )

  def search(query: String): Seq[UserRow] =
    Await.result(
      db.run(Tables.users.filter(u =>
        (u.name.toLowerCase like s"%${query.toLowerCase}%") ||
          (u.email.toLowerCase like s"%${query.toLowerCase}%")
      ).result),
      10.seconds
    )
