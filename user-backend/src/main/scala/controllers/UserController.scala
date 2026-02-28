package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.linecorp.armeria.server.annotation.{Get, Param}

import models.UserResponse
import services.UserService

object UserController:

  @Get("/api/user/:id")
  def getById(@Param id: Long) = Action:
    implicit request =>
      UserService.findById(id) match
        case Some(user) =>
          val userResp = UserResponse.fromRow(user)
          Ok(UserResponse.toJson(userResp))
        case None =>
          NotFound(Json.obj(
            "error" -> "USER_NOT_FOUND",
            "message" -> "User with the given ID does not exist"
          ))

  @Get("/api/user/name/:name")
  def getByName(@Param name: String) = Action:
    implicit request =>
      UserService.findByName(name) match
        case Some(user) =>
          val userResp = UserResponse.fromRow(user)
          Ok(UserResponse.toJson(userResp))
        case None =>
          NotFound(Json.obj(
            "error" -> "USER_NOT_FOUND",
            "message" -> "User with the given name does not exist"
          ))
