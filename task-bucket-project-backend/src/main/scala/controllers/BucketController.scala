package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Put, Delete, Param}

import auth.Authenticated
import models.BucketResponse
import services.{ProjectService, BucketService, AuthorizationService, ServiceError, AuthorizationError, NotFoundError, OperationFailed}

object BucketController:

  private def errorResponse(error: ServiceError) = error match
    case NotFoundError(resource) =>
      NotFound(Json.obj("error" -> "NOT_FOUND", "message" -> s"$resource not found"))
    case AuthorizationError(authError) =>
      Forbidden(Json.obj("error" -> "FORBIDDEN", "message" -> AuthorizationService.formatError(authError)))
    case OperationFailed(message) =>
      InternalServerError(Json.obj("error" -> "OPERATION_FAILED", "message" -> message))

  private val createBucketForm = tuple(
    "bucketName" -> nonEmptyText,
    "isDoneBucket" -> optional[Boolean]
  )

  private val updateBucketForm = tuple(
    "bucketName" -> optional[String],
    "isDoneBucket" -> optional[Boolean]
  )

  @Get("/api/projects/:projectId/buckets")
  def listByProject(@Param projectId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) =>
            AuthorizationService.canViewProject(project, user.id) match
              case Left(_) =>
                Forbidden(Json.obj(
                  "error" -> "FORBIDDEN",
                  "message" -> "You don't have access to this project"
                ))
              case Right(_) =>
                val bucketsWithStats = BucketService.getByProjectIdWithStats(projectId)
                val jsonBuckets = bucketsWithStats.map { case (bucket, totalTasks, completedTasks) =>
                  Json.obj(
                    "bucket" -> BucketResponse.toJson(bucket),
                    "stats" -> Json.obj(
                      "totalTasks" -> totalTasks,
                      "completedTasks" -> completedTasks,
                      "completionPercentage" -> {
                        if (totalTasks == 0) 0.0 else (completedTasks.toDouble / totalTasks.toDouble) * 100.0
                      }
                    )
                  )
                }

                Ok(Json.obj(
                  "buckets" -> Json.arr(jsonBuckets*),
                  "count" -> bucketsWithStats.length
                ))
          case None =>
            NotFound(Json.obj(
              "error" -> "NOT_FOUND",
              "message" -> s"Project with id $projectId not found"
            ))
      }

  @Get("/api/projects/:projectId/buckets/pending")
  def listPending(@Param projectId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) =>
            AuthorizationService.canViewProject(project, user.id) match
              case Left(_) =>
                Forbidden(Json.obj("error" -> "FORBIDDEN"))
              case Right(_) =>
                val buckets = BucketService.getPendingBuckets(projectId)
                val jsonBuckets = buckets.map(b => BucketResponse.toJson(b))
                Ok(Json.obj(
                  "buckets" -> Json.arr(jsonBuckets*),
                  "count" -> buckets.length
                ))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Get("/api/projects/:projectId/buckets/done")
  def listDone(@Param projectId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        ProjectService.getById(projectId) match
          case Some(project) =>
            AuthorizationService.canViewProject(project, user.id) match
              case Left(_) =>
                Forbidden(Json.obj("error" -> "FORBIDDEN"))
              case Right(_) =>
                val buckets = BucketService.getDoneBuckets(projectId)
                val jsonBuckets = buckets.map(b => BucketResponse.toJson(b))
                Ok(Json.obj(
                  "buckets" -> Json.arr(jsonBuckets*),
                  "count" -> buckets.length
                ))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Post("/api/projects/:projectId/buckets")
  def create(@Param projectId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        createBucketForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj(
            "error" -> "VALIDATION_ERROR",
            "details" -> errorForm.errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
          )),
          (bucketName, isDoneBucket) =>
            BucketService.createWithAuth(
              bucketName,
              projectId,
              isDoneBucket.getOrElse(false),
              user.id,
              user.id
            ) match {
              case Right(bucket) =>
                Ok(BucketResponse.toJson(bucket))
              case Left(error) => errorResponse(error)
            }
        )
      }

  @Get("/api/buckets/:id")
  def get(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        BucketService.getById(id) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canViewProject(project, user.id) match
                  case Left(_) =>
                    Forbidden(Json.obj("error" -> "FORBIDDEN"))
                  case Right(_) =>
                    Ok(BucketResponse.toJson(bucket))
              case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Put("/api/buckets/:id")
  def update(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        updateBucketForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj("error" -> "VALIDATION_ERROR")),
          (bucketName, isDoneBucket) =>
            BucketService.updateWithAuth(id, bucketName, isDoneBucket, user.id) match {
              case Right(updated) =>
                Ok(BucketResponse.toJson(updated))
              case Left(error) => errorResponse(error)
            }
        )
      }

  @Delete("/api/buckets/:id")
  def delete(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        BucketService.deleteWithAuth(id, user.id) match
          case Right(_) =>
            Ok(Json.obj("message" -> "Bucket deleted successfully"))
          case Left(error) => errorResponse(error)
      }