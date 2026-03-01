package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.{Json, JsArray}
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
          Ok(userResp.toJson)
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
          Ok(userResp.toJson)
        case None =>
          NotFound(Json.obj(
            "error" -> "USER_NOT_FOUND",
            "message" -> "User with the given name does not exist"
          ))

  @Get("/api/user/email/:email")
  def getByEmail(@Param email: String) = Action:
    implicit request =>
      UserService.findByEmail(email) match
        case Some(user) =>
          val userResp = UserResponse.fromRow(user)
          Ok(userResp.toJson)
        case None =>
          NotFound(Json.obj(
            "error" -> "USER_NOT_FOUND",
            "message" -> "User with the given email does not exist"
          ))

  @Get("/api/user/search")
  def search(@Param query: String) = Action:
    implicit request =>
      val users = UserService.search(query)
      val results = JsArray(users.map(u => UserResponse.fromRow(u).toJson))
      Ok(Json.obj(
        "results" -> results,
        "count" -> users.size
      ))

  @Get("/api/user/search/name")
  def searchByName(@Param query: String) = Action:
    implicit request =>
      val users = UserService.searchByName(query)
      val results = JsArray(users.map(u => UserResponse.fromRow(u).toJson))
      Ok(Json.obj(
        "results" -> results,
        "count" -> users.size
      ))

  @Get("/api/user/search/email")
  def searchByEmail(@Param query: String) = Action:
    implicit request =>
      val users = UserService.searchByEmail(query)
      val results = JsArray(users.map(u => UserResponse.fromRow(u).toJson))
      Ok(Json.obj(
        "results" -> results,
        "count" -> users.size
      ))
