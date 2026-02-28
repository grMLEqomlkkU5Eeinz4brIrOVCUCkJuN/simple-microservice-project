package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.Post

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
          "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
        )),
        (email, password, name) =>
          UserService.register(email, password, name) match
            case Right(user) =>
              val userResp = UserResponse.fromRow(user)
              val token = JwtService.generateToken(user.id, user.email)
              Ok(Json.obj(
                "token" -> token,
                "user" -> UserResponse.toJson(userResp)
              ))
            case Left(msg) =>
              BadRequest(Json.obj(
                "error" -> "REGISTRATION_FAILED",
                "message" -> msg
              ))
      )

  @Post("/api/auth/login")
  def login = Action:
    implicit request =>
      loginForm.bindFromRequest().fold(
        errorForm => BadRequest(Json.obj(
          "error" -> "VALIDATION_ERROR",
          "message" -> "Invalid input",
          "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
        )),
        (email, password) =>
          UserService.authenticate(email, password) match
            case Some(user) =>
              val userResp = UserResponse.fromRow(user)
              val token = JwtService.generateToken(user.id, user.email)
              Ok(Json.obj(
                "token" -> token,
                "user" -> UserResponse.toJson(userResp)
              ))
            case None =>
              Unauthorized(Json.obj(
                "error" -> "AUTH_FAILED",
                "message" -> "Invalid email or password"
              ))
      )