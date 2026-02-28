package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Put, Delete, Param}

import models.ItemResponse
import services.ItemService

object ItemController:

  private val itemForm = tuple(
    "name" -> nonEmptyText,
    "description" -> text
  )

  @Get("/api/items")
  def list = Action:
    implicit request =>
      val items = ItemService.findAll().map(ItemResponse.fromRow).map(ItemResponse.toJson)
      Ok(Json.toJson(items))

  @Get("/api/items/:id")
  def getById(@Param id: Long) = Action:
    implicit request =>
      ItemService.findById(id) match
        case Some(item) =>
          Ok(ItemResponse.toJson(ItemResponse.fromRow(item)))
        case None =>
          NotFound(Json.obj(
            "error" -> "NOT_FOUND",
            "message" -> "Item not found"
          ))

  @Post("/api/items")
  def create = Action:
    implicit request =>
      itemForm.bindFromRequest().fold(
        errorForm => BadRequest(Json.obj(
          "error" -> "VALIDATION_ERROR",
          "message" -> "Invalid input",
          "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
        )),
        (name, description) =>
          val item = ItemService.create(name, description)
          Created(ItemResponse.toJson(ItemResponse.fromRow(item)))
      )

  @Put("/api/items/:id")
  def update(@Param id: Long) = Action:
    implicit request =>
      itemForm.bindFromRequest().fold(
        errorForm => BadRequest(Json.obj(
          "error" -> "VALIDATION_ERROR",
          "message" -> "Invalid input",
          "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
        )),
        (name, description) =>
          ItemService.update(id, name, description) match
            case Some(item) =>
              Ok(ItemResponse.toJson(ItemResponse.fromRow(item)))
            case None =>
              NotFound(Json.obj(
                "error" -> "NOT_FOUND",
                "message" -> "Item not found"
              ))
      )

  @Delete("/api/items/:id")
  def delete(@Param id: Long) = Action:
    implicit request =>
      if ItemService.delete(id) then
        Ok(Json.obj("message" -> "Item deleted"))
      else
        NotFound(Json.obj(
          "error" -> "NOT_FOUND",
          "message" -> "Item not found"
        ))
