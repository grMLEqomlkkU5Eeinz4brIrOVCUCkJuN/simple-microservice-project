package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Param}

import models.UserResponse
import services.{JwtService, UserService}

object AuthController:

  private val registerForm = tuple(
    "email" -> email,
    "password" -> nonEmptyText,
    "name" -> nonEmptyText
  )

  private val loginForm = tuple(
    "email" -> email,
    "password" -> nonEmptyText
  )

  @Post("/api/auth/register")
  def register = Action:
    implicit request =>
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
              Ok(Json.obj(
                "token" -> token,
                "user" -> userResp.toJson
              ))
            case Left(error) =>
              Unauthorized(Json.obj(
                "error" -> "AUTH_FAILED",
                "message" -> error
              ))
      )

  @Get("/api/auth/verify")
  def verify(@Param("token") token: String) = Action:
    implicit request =>
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
