package controllers

import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.greenfossil.data.mapping.Mapping.*
import com.linecorp.armeria.server.annotation.{Get, Post, Put, Delete, Param}

import auth.Authenticated
import models.TaskResponse
import services.{ProjectService, BucketService, TaskService, AuthorizationService, ServiceError, AuthorizationError, NotFoundError, OperationFailed}

object TaskController:

  private def errorResponse(error: ServiceError) = error match
    case NotFoundError(resource) =>
      NotFound(Json.obj("error" -> "NOT_FOUND", "message" -> s"$resource not found"))
    case AuthorizationError(authError) =>
      Forbidden(Json.obj("error" -> "FORBIDDEN", "message" -> AuthorizationService.formatError(authError)))
    case OperationFailed(message) =>
      InternalServerError(Json.obj("error" -> "OPERATION_FAILED", "message" -> message))

  private val createTaskForm = tuple(
    "TaskName" -> nonEmptyText,
    "TaskDesc" -> optional[String]
  )

  private val updateTaskForm = tuple(
    "TaskName" -> optional[String],
    "TaskDesc" -> optional[String],
    "isTaskDone" -> optional[Boolean]
  )

  @Get("/api/buckets/:bucketId/tasks")
  def listByBucket(@Param bucketId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        BucketService.getById(bucketId) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canViewProject(project, user.id) match
                  case Left(_) =>
                    Forbidden(Json.obj("error" -> "FORBIDDEN"))
                  case Right(_) =>
                    val tasks = TaskService.getByBucketId(bucketId)
                    val completion = TaskService.getCompletionPercentage(bucketId)
                    val jsonTasks = tasks.map(t => TaskResponse.toJson(t))

                    Ok(Json.obj(
                      "tasks" -> Json.arr(jsonTasks*),
                      "count" -> tasks.length,
                      "completionPercentage" -> completion
                    ))
              case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Get("/api/buckets/:bucketId/tasks/pending")
  def listPending(@Param bucketId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        BucketService.getById(bucketId) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canViewProject(project, user.id) match
                  case Left(_) =>
                    Forbidden(Json.obj("error" -> "FORBIDDEN"))
                  case Right(_) =>
                    val tasks = TaskService.getPendingByBucketId(bucketId)
                    val jsonTasks = tasks.map(t => TaskResponse.toJson(t))
                    Ok(Json.obj(
                      "tasks" -> Json.arr(jsonTasks*),
                      "count" -> tasks.length
                    ))
              case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Get("/api/buckets/:bucketId/tasks/search")
  def search(@Param bucketId: Long, query: String) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        BucketService.getById(bucketId) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canViewProject(project, user.id) match
                  case Left(_) =>
                    Forbidden(Json.obj("error" -> "FORBIDDEN"))
                  case Right(_) =>
                    val tasks = TaskService.searchByName(bucketId, query)
                    val jsonTasks = tasks.map(t => TaskResponse.toJson(t))
                    Ok(Json.obj(
                      "tasks" -> Json.arr(jsonTasks*),
                      "count" -> tasks.length
                    ))
              case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Post("/api/buckets/:bucketId/tasks")
  def create(@Param bucketId: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        createTaskForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj("error" -> "VALIDATION_ERROR")),
          (taskName, taskDesc) =>
            TaskService.createWithAuth(bucketId, taskName, taskDesc, user.id, user.id) match {
              case Right(task) =>
                Ok(TaskResponse.toJson(task))
              case Left(error) => errorResponse(error)
            }
        )
      }

  @Get("/api/tasks/:id")
  def get(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        TaskService.getById(id) match
          case Some(task) =>
            BucketService.getById(task.bucketId) match
              case Some(bucket) =>
                ProjectService.getById(bucket.projectId) match
                  case Some(project) =>
                    AuthorizationService.canViewProject(project, user.id) match
                      case Left(_) =>
                        Forbidden(Json.obj("error" -> "FORBIDDEN"))
                      case Right(_) =>
                        Ok(TaskResponse.toJson(task))
                  case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
              case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
          case None => NotFound(Json.obj("error" -> "NOT_FOUND"))
      }

  @Put("/api/tasks/:id")
  def update(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        updateTaskForm.bindFromRequest().fold(
          errorForm => BadRequest(Json.obj("error" -> "VALIDATION_ERROR")),
          (taskName, taskDesc, isTaskDone) =>
            TaskService.updateWithAuth(id, taskName, taskDesc.map(Some(_)), isTaskDone, user.id) match {
              case Right(updated) =>
                Ok(TaskResponse.toJson(updated))
              case Left(error) => errorResponse(error)
            }
        )
      }

  @Put("/api/tasks/:id/done")
  def markDone(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        TaskService.markAsDoneWithAuth(id, user.id) match {
          case Right(updated) =>
            Ok(TaskResponse.toJson(updated))
          case Left(error) => errorResponse(error)
        }
      }

  @Delete("/api/tasks/:id")
  def delete(@Param id: Long) = Action:
    implicit request =>
      Authenticated.withUser(request) { user =>
        TaskService.deleteWithAuth(id, user.id) match {
          case Right(_) =>
            Ok(Json.obj("message" -> "Task deleted successfully"))
          case Left(error) => errorResponse(error)
        }
      }