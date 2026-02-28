package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Put, Delete, Param}

import auth.Authenticated
import models.ProjectPermissionResponse
import services.{ProjectService, ProjectPermissionService}

object ProjectPermissionController:

  private val shareProjectForm = tuple(
    "userId" -> nonEmptyText,
    "permissionLevel" -> nonEmptyText
  )

  private val updatePermissionForm = Mapping("permissionLevel", nonEmptyText)

  @Get("/api/projects/:projectId/permissions")
  def listPermissions(@Param projectId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) if project.ownerId == user.id =>
            val permissions = ProjectPermissionService.getProjectPermissions(projectId)
            val jsonPermissions = permissions.map(p => ProjectPermissionResponse.toJson(p))

            Ok(Json.obj(
              "permissions" -> Json.arr(jsonPermissions*),
              "count" -> permissions.length
            ))
          case Some(_) =>
            Forbidden(Json.obj(
              "error" -> "FORBIDDEN",
              "message" -> "Only project owner can view permissions"
            ))
          case None =>
            NotFound(Json.obj(
              "error" -> "NOT_FOUND",
              "message" -> s"Project with id $projectId not found"
            ))
      }

  @Post("/api/projects/:projectId/permissions")

  def shareProject(@Param projectId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) if project.ownerId == user.id =>
            shareProjectForm.bindFromRequest().fold(
              errorForm => BadRequest(Json.obj(
                "error" -> "VALIDATION_ERROR",
                "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
              )),
              (userId, permissionLevel) =>
                try {
                  val userIdLong = userId.toLong
                  if permissionLevel != "READONLY" && permissionLevel != "EDIT" then
                    BadRequest(Json.obj(
                      "error" -> "VALIDATION_ERROR",
                      "message" -> "Permission level must be READONLY or EDIT"
                    ))
                  else
                    ProjectPermissionService.share(projectId, userIdLong, permissionLevel, user.id).toEither match {
                      case Left(errors) =>
                        BadRequest(Json.obj(
                          "error" -> "SHARE_FAILED",
                          "details" -> errors.toList.mkString(", ")
                        ))
                      case Right(permission) =>
                        Ok(ProjectPermissionResponse.toJson(permission))
                    }
                } catch {
                  case _: NumberFormatException =>
                    BadRequest(Json.obj(
                      "error" -> "VALIDATION_ERROR",
                      "message" -> "userId must be a valid number"
                    ))
                }
            )
          case Some(_) =>
            Forbidden(Json.obj(
              "error" -> "FORBIDDEN",
              "message" -> "Only project owner can share projects"
            ))
          case None =>
            NotFound(Json.obj(
              "error" -> "NOT_FOUND",
              "message" -> s"Project with id $projectId not found"
            ))
      }

  @Put("/api/projects/:projectId/permissions/:userId")
  def updatePermission(@Param projectId: Long, @Param userId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) if project.ownerId == user.id =>
            updatePermissionForm.bindFromRequest().fold(
              errorForm => BadRequest(Json.obj("error" -> "VALIDATION_ERROR")),
              permissionLevel => {
                try {
                  if permissionLevel != "READONLY" && permissionLevel != "EDIT" then
                    BadRequest(Json.obj(
                      "error" -> "VALIDATION_ERROR",
                      "message" -> "Permission level must be READONLY or EDIT"
                    ))
                  else if ProjectPermissionService.updatePermissionLevel(projectId, userId, permissionLevel) then
                    ProjectPermissionService.getPermission(projectId, userId) match
                      case Some(permission) =>
                        Ok(ProjectPermissionResponse.toJson(permission))
                      case None =>
                        InternalServerError(Json.obj("error" -> "UPDATE_FAILED"))
                  else
                    NotFound(Json.obj(
                      "error" -> "NOT_FOUND",
                      "message" -> "User permission not found"
                    ))
                } catch {
                  case _: Exception =>
                    BadRequest(Json.obj("error" -> "VALIDATION_ERROR"))
                }
              }
            )
          case Some(_) =>
            Forbidden(Json.obj("error" -> "FORBIDDEN"))
          case None =>
            NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Delete("/api/projects/:projectId/permissions/:userId")
  def revokePermission(@Param projectId: Long, @Param userId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) if project.ownerId == user.id =>
            if ProjectPermissionService.revokePermission(projectId, userId) then
              Ok(Json.obj("message" -> "Permission revoked successfully"))
            else
              NotFound(Json.obj(
                "error" -> "NOT_FOUND",
                "message" -> "User permission not found"
              ))
          case Some(_) =>
            Forbidden(Json.obj("error" -> "FORBIDDEN"))
          case None =>
            NotFound(Json.obj("error" -> "NOT_FOUND"))
      }