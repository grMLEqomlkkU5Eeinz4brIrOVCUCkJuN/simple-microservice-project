package services

import cats.data.Validated
import cats.syntax.all.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.typesafe.config.ConfigFactory

object JwtService:
  private val config = ConfigFactory.load()
  private val secret = config.getString("jwt.secret")
  private val algorithm = Algorithm.HMAC256(secret)
  private val verifier = JWT.require(algorithm).withIssuer("api-template").build()

  def validateToken(token: String): Validated[String, (Long, String)] =
    try
      val decoded = verifier.verify(token)
      val userId = decoded.getSubject.toLong
      val email = decoded.getClaim("email").asString()
      (userId, email).valid
    catch
      case e: JWTVerificationException => s"Invalid token: ${e.getMessage}".invalid
      case _: NumberFormatException => "Invalid user ID in token".invalid