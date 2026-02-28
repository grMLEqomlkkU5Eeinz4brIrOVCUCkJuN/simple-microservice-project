package services

import cats.data.ValidatedNel
import cats.syntax.all.*
import db.{Database, Tables, TaskRow}
import models.TaskResponse
import slick.jdbc.MySQLProfile.api.*

import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.*

object TaskService {
  private val db = Database.db
  private val logger = LoggerFactory.getLogger(getClass)

  private def validateTaskName(name: String): ValidatedNel[String, String] =
    if name.nonEmpty then name.validNel
    else "Task name cannot be empty".invalidNel

  private def validateBucketId(bucketId: Long): ValidatedNel[String, Long] =
    if bucketId > 0 then bucketId.validNel
    else "Bucket ID must be greater than 0".invalidNel

  def createWithAuth(
    bucketId: Long,
    name: String,
    description: Option[String],
    createdByUserId: Long,
    userId: Long  // The user performing the action
  ): Either[ServiceError, TaskResponse] =
    BucketService.getById(bucketId) match
      case Some(bucket) =>
        ProjectService.getById(bucket.projectId) match
          case Some(project) =>
            AuthorizationService.canEditProject(project, userId) match
              case Right(_) =>
                create(bucketId, name, description, createdByUserId).toEither
                  .left.map(errors => OperationFailed(errors.toList.mkString(", ")))
              case Left(error) =>
                Left(AuthorizationError(error))
          case None => Left(NotFoundError("Project"))
      case None => Left(NotFoundError("Bucket"))

  def updateWithAuth(
    id: Long,
    name: Option[String],
    description: Option[Option[String]],
    isTaskDone: Option[Boolean],
    userId: Long  // The user performing the action
  ): Either[ServiceError, TaskResponse] =
    getById(id) match
      case Some(task) =>
        BucketService.getById(task.bucketId) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canEditProject(project, userId) match
                  case Right(_) =>
                    if update(id, name, description, isTaskDone) then
                      getById(id).toRight(OperationFailed("Failed to retrieve updated task"))
                    else
                      Left(OperationFailed("Failed to update task"))
                  case Left(error) =>
                    Left(AuthorizationError(error))
              case None => Left(NotFoundError("Project"))
          case None => Left(NotFoundError("Bucket"))
      case None => Left(NotFoundError("Task"))

  def deleteWithAuth(id: Long, userId: Long): Either[ServiceError, Unit] =
    getById(id) match
      case Some(task) =>
        BucketService.getById(task.bucketId) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canDeleteTask(task, userId, task.bucketId) match
                  case Right(_) =>
                    if delete(id) then Right(()) else Left(OperationFailed("Failed to delete task"))
                  case Left(error) =>
                    Left(AuthorizationError(error))
              case None => Left(NotFoundError("Project"))
          case None => Left(NotFoundError("Bucket"))
      case None => Left(NotFoundError("Task"))

  def markAsDoneWithAuth(id: Long, userId: Long): Either[ServiceError, TaskResponse] =
    getById(id) match
      case Some(task) =>
        BucketService.getById(task.bucketId) match
          case Some(bucket) =>
            ProjectService.getById(bucket.projectId) match
              case Some(project) =>
                AuthorizationService.canEditProject(project, userId) match
                  case Right(_) =>
                    if markAsDone(id) then
                      getById(id).toRight(OperationFailed("Failed to retrieve updated task"))
                    else
                      Left(OperationFailed("Failed to mark task as done"))
                  case Left(error) =>
                    Left(AuthorizationError(error))
              case None => Left(NotFoundError("Project"))
          case None => Left(NotFoundError("Bucket"))
      case None => Left(NotFoundError("Task"))

  def create(bucketId: Long, name: String, description: Option[String], createdByUserId: Long): ValidatedNel[String, TaskResponse] = {
    val validation = (validateBucketId(bucketId), validateTaskName(name)).mapN((_, _))

    validation.andThen { _ =>
      try {
        val now = LocalDateTime.now()
        val row = TaskRow(
          bucketId = bucketId,
          isTaskDone = false,
          TaskName = name,
          TaskDesc = description,
          createdByUserId = createdByUserId,
          createdAt = now,
          updatedAt = now
        )
        val insertAction = (Tables.tasks returning Tables.tasks.map(_.id) into ((task, id) => task.copy(id = id))) += row
        val insertedRow = Await.result(db.run(insertAction), 10.seconds)
        TaskResponse.fromRow(insertedRow).validNel
      } catch {
        case e: Exception => s"Failed to create task: ${e.getMessage}".invalidNel
      }
    }
  }

  def getById(id: Long): Option[TaskResponse] = {
    try {
      val query = Tables.tasks.filter(_.id === id)
      val result = Await.result(db.run(query.result.headOption), 10.seconds)
      result.map(TaskResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        None
    }
  }

  def getAll(): Seq[TaskResponse] = {
    try {
      val query = Tables.tasks.sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(TaskResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getByBucketId(bucketId: Long): Seq[TaskResponse] = {
    try {
      val query = Tables.tasks
        .filter(_.bucketId === bucketId)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(TaskResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def update(
    id: Long,
    name: Option[String] = None,
    description: Option[Option[String]] = None,
    isTaskDone: Option[Boolean] = None
  ): Boolean = {
    try {
      getById(id) match {
        case Some(existing) =>
          val now = LocalDateTime.now()
          val updatedRow = existing.copy(
            TaskName = name.getOrElse(existing.TaskName),
            TaskDesc = description.getOrElse(existing.TaskDesc),
            isTaskDone = isTaskDone.getOrElse(existing.isTaskDone),
            updatedAt = now
          )
          val updateAction = Tables.tasks.filter(_.id === id).update(
            TaskRow(
              id = updatedRow.id,
              bucketId = updatedRow.bucketId,
              isTaskDone = updatedRow.isTaskDone,
              TaskName = updatedRow.TaskName,
              TaskDesc = updatedRow.TaskDesc,
              createdByUserId = updatedRow.createdByUserId,
              createdAt = updatedRow.createdAt,
              updatedAt = now
            )
          )
          val result = Await.result(db.run(updateAction), 10.seconds)
          result > 0
        case None => false
      }
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def delete(id: Long): Boolean = {
    try {
      val query = Tables.tasks.filter(_.id === id).delete
      val result = Await.result(db.run(query), 10.seconds)
      result > 0
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def deleteByBucketId(bucketId: Long): Boolean = {
    try {
      val query = Tables.tasks.filter(_.bucketId === bucketId).delete
      val result = Await.result(db.run(query), 10.seconds)
      result > 0
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def countByBucketId(bucketId: Long): Long = {
    try {
      val query = Tables.tasks.filter(_.bucketId === bucketId).length
      Await.result(db.run(query.result), 10.seconds).toLong
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0L
    }
  }

  def countCompletedByBucketId(bucketId: Long): Long = {
    try {
      val query = Tables.tasks
        .filter(_.bucketId === bucketId)
        .filter(_.isTaskDone === true)
        .length
      Await.result(db.run(query.result), 10.seconds).toLong
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0L
    }
  }

  def markAsDone(id: Long): Boolean = {
    try {
      val query = Tables.tasks.filter(_.id === id)
      val updateAction = query.map(t => (t.isTaskDone, t.updatedAt)).update((true, LocalDateTime.now()))
      val result = Await.result(db.run(updateAction), 10.seconds)
      result > 0
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def getPendingByBucketId(bucketId: Long): Seq[TaskResponse] = {
    try {
      val query = Tables.tasks
        .filter(_.bucketId === bucketId)
        .filter(_.isTaskDone === false)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(TaskResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def searchByName(bucketId: Long, pattern: String): Seq[TaskResponse] = {
    try {
      val query = Tables.tasks
        .filter(_.bucketId === bucketId)
        .filter(_.TaskName.like(s"%$pattern%"))
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(TaskResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getCompletionPercentage(bucketId: Long): Double = {
    try {
      val total = countByBucketId(bucketId)
      if (total == 0) 0.0
      else {
        val completed = countCompletedByBucketId(bucketId)
        (completed.toDouble / total.toDouble) * 100.0
      }
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0.0
    }
  }
}