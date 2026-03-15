package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.{Json, JsArray}
import com.linecorp.armeria.server.annotation.{Get, Param}
import com.typesafe.config.ConfigFactory

import models.UserResponse
import services.{UserService, RedisService}

object UserController:

  private val config = ConfigFactory.load()
  private val apiKeySecret = config.getString("apiKey.secret")

  private def withApiKey(request: Request)(fn: => ActionResponse): ActionResponse =
    val apiKey = request.findHeader("X-API-Key")
    apiKey match
      case Some(key) if key == apiKeySecret => fn
      case _ =>
        Unauthorized(Json.obj(
          "error" -> "UNAUTHORIZED",
          "message" -> "Missing or invalid API key"
        ))

  private def getClientIp(request: Request): String =
    request.findHeader("X-Forwarded-For")
      .map(_.split(",").head.trim)
      .getOrElse("unknown")

  private val rateLimitedResponse = Status(429)(Json.obj(
    "error" -> "RATE_LIMITED",
    "message" -> "Too many requests. Please try again later."
  ))

  @Get("/api/user/:id")
  def getById(@Param id: Long) = Action:
    implicit request =>
      withApiKey(request) {
        UserService.findById(id) match
          case Some(user) =>
            val userResp = UserResponse.fromRow(user)
            Ok(userResp.toJson)
          case None =>
            NotFound(Json.obj(
              "error" -> "USER_NOT_FOUND",
              "message" -> "User with the given ID does not exist"
            ))
      }

  @Get("/api/user/name/:name")
  def getByName(@Param name: String) = Action:
    implicit request =>
      withApiKey(request) {
        UserService.findByName(name) match
          case Some(user) =>
            val userResp = UserResponse.fromRow(user)
            Ok(userResp.toJson)
          case None =>
            NotFound(Json.obj(
              "error" -> "USER_NOT_FOUND",
              "message" -> "User with the given name does not exist"
            ))
      }

  @Get("/api/user/email/:email")
  def getByEmail(@Param email: String) = Action:
    implicit request =>
      withApiKey(request) {
        UserService.findByEmail(email) match
          case Some(user) =>
            val userResp = UserResponse.fromRow(user)
            Ok(userResp.toJson)
          case None =>
            NotFound(Json.obj(
              "error" -> "USER_NOT_FOUND",
              "message" -> "User with the given email does not exist"
            ))
      }

  @Get("/api/user/search")
  def search(@Param query: String) = Action:
    implicit request =>
      val clientIp = getClientIp(request)
      if !RedisService.checkRateLimit(s"search:$clientIp", 30, 60) then
        rateLimitedResponse
      else
        val users = UserService.search(query)
        val results = JsArray(users.map(u => UserResponse.fromRow(u).toPublicJson))
        Ok(Json.obj(
          "results" -> results,
          "count" -> users.size
        ))

  @Get("/api/user/search/name")
  def searchByName(@Param query: String) = Action:
    implicit request =>
      val clientIp = getClientIp(request)
      if !RedisService.checkRateLimit(s"searchName:$clientIp", 30, 60) then
        rateLimitedResponse
      else
        val users = UserService.searchByName(query)
        val results = JsArray(users.map(u => UserResponse.fromRow(u).toPublicJson))
        Ok(Json.obj(
          "results" -> results,
          "count" -> users.size
        ))

  @Get("/api/user/search/email")
  def searchByEmail(@Param query: String) = Action:
    implicit request =>
      withApiKey(request) {
        val users = UserService.searchByEmail(query)
        val results = JsArray(users.map(u => UserResponse.fromRow(u).toJson))
        Ok(Json.obj(
          "results" -> results,
          "count" -> users.size
        ))
      }
