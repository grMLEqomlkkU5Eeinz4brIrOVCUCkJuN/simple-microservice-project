package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Param}

import models.ProjectResponse
import services.{ProjectService, JwtService, AuthorizationService}
import org.mindrot.jbcrypt.BCrypt

object ProjectShareController:

  private val unlockForm = Mapping("password", nonEmptyText)

  /**
   * Unlock a password-protected project and receive a share token
   * POST /api/projects/:id/unlock
   * Body: { "password": "..." }
   * Response: { "token": "...", "permission": "READONLY" | "EDIT" }
   */
  @Post("/api/projects/:projectId/unlock")
  def unlock(@Param projectId: Long) = Action:
    implicit request =>
      ProjectService.getById(projectId) match
        case Some(project) =>
          if !project.isPublic && !project.isShared then
            Forbidden(Json.obj(
              "error" -> "FORBIDDEN",
              "message" -> "This project is not shared"
            ))
          else
            unlockForm.bindFromRequest().fold(
              errorForm => BadRequest(Json.obj(
                "error" -> "VALIDATION_ERROR",
                "message" -> "Password is required"
              )),
              (password) =>
                val editMatch = project.editPasswordHash.exists(hash => BCrypt.checkpw(password, hash))
                val viewMatch = project.viewPasswordHash.exists(hash => BCrypt.checkpw(password, hash))
                (editMatch, viewMatch) match
                  case (true, _) =>
                    val token = JwtService.generateShareToken(projectId, "EDIT", expirationHours = 24)
                    Ok(Json.obj(
                      "token" -> token,
                      "permission" -> "EDIT",
                      "expiresIn" -> 86400  // seconds
                    ))
                  case (false, true) =>
                    val token = JwtService.generateShareToken(projectId, "READONLY", expirationHours = 24)
                    Ok(Json.obj(
                      "token" -> token,
                      "permission" -> "READONLY",
                      "expiresIn" -> 86400
                    ))
                  case (false, false) =>
                    Unauthorized(Json.obj(
                      "error" -> "INVALID_PASSWORD",
                      "message" -> "Invalid password"
                    ))
            )
        case None =>
          NotFound(Json.obj(
            "error" -> "NOT_FOUND",
            "message" -> s"Project with id $projectId not found"
          ))

  /**
   * Get project details with a share token (no authentication required)
   * GET /api/projects/:id/shared?token=...
   */
  @Get("/api/projects/:projectId/shared")
  def getShared(@Param projectId: Long, token: String) = Action:
    implicit request =>
      JwtService.validateShareToken(token).toEither match
        case Left(error) =>
          Unauthorized(Json.obj(
            "error" -> "INVALID_TOKEN",
            "message" -> error
          ))
        case Right((projId, permission)) if projId != projectId =>
          Forbidden(Json.obj(
            "error" -> "FORBIDDEN",
            "message" -> "Token is for a different project"
          ))
        case Right((projId, permission)) =>
          ProjectService.getById(projectId) match
            case Some(project) =>
              ProjectService.getProjectWithStats(projectId) match
                case Some((proj, bucketCount, taskCount)) =>
                  Ok(Json.obj(
                    "project" -> ProjectResponse.toJson(proj),
                    "permission" -> permission,
                    "stats" -> Json.obj(
                      "buckets" -> bucketCount,
                      "tasks" -> taskCount
                    )
                  ))
                case None =>
                  InternalServerError(Json.obj("error" -> "INTERNAL_ERROR"))
            case None =>
              NotFound(Json.obj("error" -> "NOT_FOUND"))

  /**
   * Check if a project is accessible without authentication
   * GET /api/projects/:id/public
   */
  @Get("/api/projects/:projectId/public")
  def checkPublic(@Param projectId: Long) = Action:
    implicit request =>
      ProjectService.getById(projectId) match
        case Some(project) =>
          val isPublic = AuthorizationService.isPubliclyAccessible(project)
          val requiresPassword = AuthorizationService.requiresPasswordForAccess(project)

          Ok(Json.obj(
            "isPublic" -> isPublic,
            "requiresPassword" -> requiresPassword,
            "projectName" -> project.projectName
          ))
        case None =>
          NotFound(Json.obj("error" -> "NOT_FOUND"))