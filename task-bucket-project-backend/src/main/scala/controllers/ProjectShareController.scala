package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Param}

import models.ProjectResponse
import services.{ProjectService, JwtService, AuthorizationService}
import org.mindrot.jbcrypt.BCrypt
import com.typesafe.config.ConfigFactory

object ProjectShareController:

  private val config = ConfigFactory.load()
  private val shareTokenMaxAge = config.getLong("cookie.shareTokenMaxAgeSeconds")
  private val shareTokenExpirationHours = (shareTokenMaxAge / 3600).toInt

  private val unlockForm = Mapping("password", nonEmptyText)

  /**
   * Unlock a password-protected project and receive a share token cookie
   * POST /api/projects/:id/unlock
   * Body: { "password": "..." }
   * Response: { "permission": "READONLY" | "EDIT" } + Set-Cookie: share_token
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
                    val token = JwtService.generateShareToken(projectId, "EDIT", expirationHours = shareTokenExpirationHours)
                    val cookie = CookieUtil.bakeCookie("share_token", token, Some(shareTokenMaxAge))
                    Ok(Json.obj(
                      "permission" -> "EDIT",
                      "expiresIn" -> shareTokenMaxAge
                    )).withCookies(cookie)
                  case (false, true) =>
                    val token = JwtService.generateShareToken(projectId, "READONLY", expirationHours = shareTokenExpirationHours)
                    val cookie = CookieUtil.bakeCookie("share_token", token, Some(shareTokenMaxAge))
                    Ok(Json.obj(
                      "permission" -> "READONLY",
                      "expiresIn" -> shareTokenMaxAge
                    )).withCookies(cookie)
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
   * Get project details with a share token cookie (no authentication required)
   * GET /api/projects/:id/shared
   */
  @Get("/api/projects/:projectId/shared")
  def getShared(@Param projectId: Long) = Action:
    implicit request =>
      val tokenOpt = request.findCookie("share_token").map(_.value())
      tokenOpt match
        case None =>
          Unauthorized(Json.obj(
            "error" -> "MISSING_TOKEN",
            "message" -> "Share token cookie not found"
          ))
        case Some(token) =>
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