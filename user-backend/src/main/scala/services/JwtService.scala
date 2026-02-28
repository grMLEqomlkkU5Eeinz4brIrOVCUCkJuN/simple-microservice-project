package services

import cats.data.Validated
import cats.syntax.all.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.typesafe.config.ConfigFactory

import java.time.Instant
import java.time.temporal.ChronoUnit

object JwtService:
  private val config = ConfigFactory.load()
  private val secret = config.getString("jwt.secret")
  private val expirationMinutes = config.getLong("jwt.expirationMinutes")
  private val algorithm = Algorithm.HMAC256(secret)
  private val verifier = JWT.require(algorithm).withIssuer("todo-api").build()

  def generateToken(userId: Long, email: String): String =
    JWT.create()
      .withIssuer("todo-api")
      .withSubject(userId.toString)
      .withClaim("email", email)
      .withIssuedAt(Instant.now())
      .withExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
      .sign(algorithm)

  def validateToken(token: String): Validated[String, (Long, String)] =
    try
      val decoded = verifier.verify(token)
      val userId = decoded.getSubject.toLong
      val email = decoded.getClaim("email").asString()
      (userId, email).valid
    catch
      case e: JWTVerificationException => s"Invalid token: ${e.getMessage}".invalid
      case _: NumberFormatException => "Invalid user ID in token".invalid