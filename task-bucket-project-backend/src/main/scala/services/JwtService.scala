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
  private val algorithm = Algorithm.HMAC256(secret)
  private val verifier = JWT.require(algorithm).withIssuer("api-template").build()
  private val shareTokenVerifier = JWT.require(algorithm).withIssuer("project-share").build()

  // User tokens
  def validateToken(token: String): Validated[String, (Long, String)] =
    try
      val decoded = verifier.verify(token)
      val userId = decoded.getSubject.toLong
      val email = Option(decoded.getClaim("email").asString()).getOrElse("")
      (userId, email).valid
    catch
      case e: JWTVerificationException => s"Invalid token: ${e.getMessage}".invalid
      case _: NumberFormatException => "Invalid user ID in token".invalid

  // Password-protected project share tokens
  def generateShareToken(projectId: Long, permissionLevel: String, expirationHours: Int = 24): String =
    val now = Instant.now()
    val expiration = now.plus(expirationHours.toLong, ChronoUnit.HOURS)

    JWT.create()
      .withIssuer("project-share")
      .withSubject(projectId.toString)
      .withClaim("permission", permissionLevel)
      .withIssuedAt(now)
      .withExpiresAt(expiration)
      .sign(algorithm)

  def validateShareToken(token: String): Validated[String, (Long, String)] =
    try
      val decoded = shareTokenVerifier.verify(token)
      val projectId = decoded.getSubject.toLong
      val permission = Option(decoded.getClaim("permission").asString()).getOrElse("")
      if permission.isEmpty then "Missing permission claim in token".invalid
      else (projectId, permission).valid
    catch
      case e: JWTVerificationException => s"Invalid share token: ${e.getMessage}".invalid
      case _: NumberFormatException => "Invalid project ID in token".invalid