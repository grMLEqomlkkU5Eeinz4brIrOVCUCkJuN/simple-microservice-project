package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Param}
import com.typesafe.config.ConfigFactory

import models.UserResponse
import services.{JwtService, UserService, RedisService}

object AuthController:

  private val config = ConfigFactory.load()
  private val cookieMaxAge = config.getLong("cookie.authTokenMaxAgeSeconds")

  private val registerForm = tuple(
    "email" -> email,
    "password" -> nonEmptyText,
    "name" -> nonEmptyText
  )

  private val loginForm = tuple(
    "email" -> email,
    "password" -> nonEmptyText
  )

  private val rateLimitedResponse = Status(429)(Json.obj(
    "error" -> "RATE_LIMITED",
    "message" -> "Too many requests. Please try again later."
  ))

  private def getClientIp(request: Request): String =
    request.findHeader("X-Forwarded-For")
      .map(_.split(",").head.trim)
      .getOrElse("unknown")

  @Post("/api/auth/register")
  def register = Action:
    implicit request =>
      val clientIp = getClientIp(request)
      if !RedisService.checkRateLimit(s"register:$clientIp", 5, 3600) then
        rateLimitedResponse
      else
        registerForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj(
            "error" -> "VALIDATION_ERROR",
            "message" -> "Invalid input",
            "details" -> Json.arr(errorForm.errors.map(e => Json.obj("field" -> e.key, "message" -> e.message))*)
          )),
          (email, password, name) =>
            UserService.register(email, password, name) match
              case Right(user) =>
                Ok(Json.obj(
                  "message" -> "Registration successful. Please check your email to verify your account."
                ))
              case Left(errors) =>
                BadRequest(Json.obj(
                  "error" -> "REGISTRATION_FAILED",
                  "message" -> "Registration failed",
                  "details" -> Json.arr(errors.map(e => Json.obj("message" -> e))*)
                ))
        )

  @Post("/api/auth/login")
  def login = Action:
    implicit request =>
      val clientIp = getClientIp(request)
      if !RedisService.checkRateLimit(s"login:$clientIp", 10, 300) then
        rateLimitedResponse
      else
        loginForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj(
            "error" -> "VALIDATION_ERROR",
            "message" -> "Invalid input",
            "details" -> Json.arr(errorForm.errors.map(e => Json.obj("field" -> e.key, "message" -> e.message))*)
          )),
          (email, password) =>
            UserService.authenticate(email, password) match
              case Right(user) =>
                val userResp = UserResponse.fromRow(user)
                val token = JwtService.generateToken(user.id, user.email)
                val cookie = CookieUtil.bakeCookie("auth_token", token, Some(cookieMaxAge))
                Ok(Json.obj(
                  "user" -> userResp.toJson
                )).withCookies(cookie)
              case Left(error) =>
                Unauthorized(Json.obj(
                  "error" -> "AUTH_FAILED",
                  "message" -> error
                ))
        )

  @Get("/api/auth/verify")
  def verify(@Param("token") token: String) = Action:
    implicit request =>
      val clientIp = getClientIp(request)
      if !RedisService.checkRateLimit(s"verify:$clientIp", 10, 300) then
        rateLimitedResponse
      else
        UserService.verifyEmail(token) match
          case Some(user) =>
            val userResp = UserResponse.fromRow(user)
            Ok(Json.obj(
              "message" -> "Email verified successfully. You can now log in.",
              "user" -> userResp.toJson
            ))
          case None =>
            BadRequest(Json.obj(
              "error" -> "VERIFICATION_FAILED",
              "message" -> "Invalid or expired verification token"
            ))
