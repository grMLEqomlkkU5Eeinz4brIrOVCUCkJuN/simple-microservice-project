package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Put, Delete, Param}

import auth.Authenticated
import models.ProjectResponse
import services.{JwtService, ProjectService, ProjectPermissionService, ProjectAccessService, AuthorizationService, Owner, EditAccess, ReadOnlyAccess, NoAccess, ServiceError, AuthorizationError, NotFoundError, OperationFailed}
import org.mindrot.jbcrypt.BCrypt

object ProjectController:

  private def hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt())

  private def errorResponse(error: ServiceError) = error match
    case NotFoundError(resource) =>
      NotFound(Json.obj("error" -> "NOT_FOUND", "message" -> s"$resource not found"))
    case AuthorizationError(authError) =>
      Forbidden(Json.obj("error" -> "FORBIDDEN", "message" -> AuthorizationService.formatError(authError)))
    case OperationFailed(message) =>
      InternalServerError(Json.obj("error" -> "OPERATION_FAILED", "message" -> message))

  private val createProjectForm = tuple(
    "projectName" -> nonEmptyText,
    "isPublic" -> optional[Boolean],
    "isShared" -> optional[Boolean],
    "viewPassword" -> optional[String],
    "editPassword" -> optional[String]
  )

  private val updateProjectForm = tuple(
    "projectName" -> optional[String],
    "isPublic" -> optional[Boolean],
    "isShared" -> optional[Boolean],
    "viewPassword" -> optional[String],
    "editPassword" -> optional[String]
  )

  @Get("/api/projects")
  def list = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        val ownedProjects = ProjectService.getByOwnerId(user.id)
        val sharedProjects = ProjectAccessService.getAccessibleProjects(user.id)
          .filter(_.ownerId != user.id)

        val allProjects = (ownedProjects ++ sharedProjects).distinctBy(_.id)
        val jsonProjects = allProjects.map(p => ProjectResponse.toJson(p))

        Ok(Json.obj(
          "projects" -> Json.arr(jsonProjects*),
          "count" -> allProjects.length
        ))
      }

  @Get("/api/projects/public")
  def listPublic = Action:
    implicit request =>
      val projects = ProjectService.getPublicProjects()
      val jsonProjects = projects.map(p => ProjectResponse.toJson(p))

      Ok(Json.obj(
        "projects" -> Json.arr(jsonProjects*),
        "count" -> projects.length
      ))

  @Get("/api/projects/shared")
  def listShared = Action:
    implicit request =>
      val projects = ProjectService.getSharedProjects()
      val jsonProjects = projects.map(p => ProjectResponse.toJson(p))

      Ok(Json.obj(
        "projects" -> Json.arr(jsonProjects*),
        "count" -> projects.length
      ))

  @Get("/api/projects/search")
  def search(query: String) = Action:
    implicit request =>
      val projects = ProjectService.searchByName(query)
      val jsonProjects = projects.map(p => ProjectResponse.toJson(p))

      Ok(Json.obj(
        "projects" -> Json.arr(jsonProjects*),
        "count" -> projects.length
      ))

  @Post("/api/projects")
  def create = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        createProjectForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj(
            "error" -> "VALIDATION_ERROR",
            "message" -> "Invalid input",
            "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
          )),
          (projectName, isPublic, isShared, viewPassword, editPassword) =>
            ProjectService.create(
              projectName,
              user.id,
              isPublic.getOrElse(false),
              isShared.getOrElse(false),
              viewPassword.filter(_.nonEmpty).map(hashPassword),
              editPassword.filter(_.nonEmpty).map(hashPassword)
            ).toEither match {
              case Left(errors) =>
                BadRequest(Json.obj(
                  "error" -> "VALIDATION_ERROR",
                  "message" -> "Invalid project data",
                  "details" -> errors.toList.mkString(", ")
                ))
              case Right(project) =>
                Ok(ProjectResponse.toJson(project))
            }
        )
      }

  @Get("/api/projects/:id")
  def get(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(id) match
          case Some(project) =>
            ProjectAccessService.getAccessLevel(project, user.id) match
              case Owner | EditAccess | ReadOnlyAccess =>
                ProjectService.getProjectWithStats(id) match
                  case Some((proj, bucketCount, taskCount)) =>
                    Ok(Json.obj(
                      "project" -> ProjectResponse.toJson(proj),
                      "stats" -> Json.obj(
                        "buckets" -> bucketCount,
                        "tasks" -> taskCount
                      )
                    ))
                  case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
              case NoAccess =>
                Forbidden(Json.obj(
                  "error" -> "FORBIDDEN",
                  "message" -> "You don't have access to this project"
                ))
          case None =>
            NotFound(Json.obj(
              "error" -> "NOT_FOUND",
              "message" -> s"Project with id $id not found"
            ))
      }

  @Put("/api/projects/:id")
  def update(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        updateProjectForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj(
            "error" -> "VALIDATION_ERROR",
            "message" -> "Invalid input",
            "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
          )),
          (projectName, isPublic, isShared, viewPassword, editPassword) =>
            ProjectService.updateWithAuth(id, projectName, isPublic, isShared, viewPassword.map(p => Some(hashPassword(p))), editPassword.map(p => Some(hashPassword(p))), user.id) match {
              case Right(updatedProject) =>
                Ok(ProjectResponse.toJson(updatedProject))
              case Left(error) => errorResponse(error)
            }
        )
      }

  @Delete("/api/projects/:id")
  def delete(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.deleteWithAuth(id, user.id) match
          case Right(_) =>
            Ok(Json.obj("message" -> "Project deleted successfully"))
          case Left(error) => errorResponse(error)
      }