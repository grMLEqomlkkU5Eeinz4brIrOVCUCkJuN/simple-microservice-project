package services

import cats.data.ValidatedNel
import cats.syntax.all.*
import db.{Database, Tables, BucketRow}
import models.BucketResponse
import slick.jdbc.MySQLProfile.api.*

import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.*

object BucketService {
  private val db = Database.db
  private val logger = LoggerFactory.getLogger(getClass)

  private def validateBucketName(name: String): ValidatedNel[String, String] =
    if name.nonEmpty then name.validNel
    else "Bucket name cannot be empty".invalidNel

  private def validateProjectId(projectId: Long): ValidatedNel[String, Long] =
    if projectId > 0 then projectId.validNel
    else "Project ID must be greater than 0".invalidNel

  def createWithAuth(
    bucketName: String,
    projectId: Long,
    isDoneBucket: Boolean,
    createdByUserId: Long,
    userId: Long  // The user performing the action
  ): Either[ServiceError, BucketResponse] =
    ProjectService.getById(projectId) match
      case Some(project) =>
        AuthorizationService.canEditProject(project, userId) match
          case Right(_) =>
            create(bucketName, projectId, isDoneBucket, createdByUserId).toEither
              .left.map(errors => OperationFailed(errors.toList.mkString(", ")))
          case Left(error) =>
            Left(AuthorizationError(error))
      case None => Left(NotFoundError("Project"))

  def updateWithAuth(
    id: Long,
    bucketName: Option[String],
    isDoneBucket: Option[Boolean],
    userId: Long  // The user performing the action
  ): Either[ServiceError, BucketResponse] =
    getById(id) match
      case Some(bucket) =>
        ProjectService.getById(bucket.projectId) match
          case Some(project) =>
            AuthorizationService.canEditProject(project, userId) match
              case Right(_) =>
                if update(id, bucketName, isDoneBucket) then
                  getById(id).toRight(OperationFailed("Failed to retrieve updated bucket"))
                else
                  Left(OperationFailed("Failed to update bucket"))
              case Left(error) =>
                Left(AuthorizationError(error))
          case None => Left(NotFoundError("Project"))
      case None => Left(NotFoundError("Bucket"))

  def deleteWithAuth(id: Long, userId: Long): Either[ServiceError, Unit] =
    getById(id) match
      case Some(bucket) =>
        ProjectService.getById(bucket.projectId) match
          case Some(project) =>
            AuthorizationService.canDeleteBucket(bucket, userId, bucket.projectId) match
              case Right(_) =>
                if delete(id) then Right(()) else Left(OperationFailed("Failed to delete bucket"))
              case Left(error) =>
                Left(AuthorizationError(error))
          case None => Left(NotFoundError("Project"))
      case None => Left(NotFoundError("Bucket"))

  def create(
    bucketName: String,
    projectId: Long,
    isDoneBucket: Boolean,
    createdByUserId: Long
  ): ValidatedNel[String, BucketResponse] = {
    val validation = (validateBucketName(bucketName), validateProjectId(projectId)).mapN((_, _))

    validation.andThen { _ =>
      try {
        val now = LocalDateTime.now()
        val row = BucketRow(
          bucketName = bucketName,
          projectId = projectId,
          isDoneBucket = isDoneBucket,
          createdByUserId = createdByUserId,
          createdAt = now,
          updatedAt = now
        )
        val insertAction = (Tables.buckets returning Tables.buckets.map(_.id) into ((bucket, id) => bucket.copy(id = id))) += row
        val insertedRow = Await.result(db.run(insertAction), 10.seconds)
        BucketResponse.fromRow(insertedRow).validNel
      } catch {
        case e: Exception => s"Failed to create bucket: ${e.getMessage}".invalidNel
      }
    }
  }

  def getById(id: Long): Option[BucketResponse] = {
    try {
      val query = Tables.buckets.filter(_.id === id)
      val result = Await.result(db.run(query.result.headOption), 10.seconds)
      result.map(BucketResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        None
    }
  }

  def getAll(): Seq[BucketResponse] = {
    try {
      val query = Tables.buckets.sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(BucketResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getByProjectId(projectId: Long): Seq[BucketResponse] = {
    try {
      val query = Tables.buckets
        .filter(_.projectId === projectId)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(BucketResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def update(
    id: Long,
    bucketName: Option[String] = None,
    isDoneBucket: Option[Boolean] = None
  ): Boolean = {
    try {
      getById(id) match {
        case Some(existing) =>
          val now = LocalDateTime.now()
          val updatedRow = BucketRow(
            id = existing.id,
            bucketName = bucketName.getOrElse(existing.bucketName),
            projectId = existing.projectId,
            isDoneBucket = isDoneBucket.getOrElse(existing.isDoneBucket),
            createdByUserId = existing.createdByUserId,
            createdAt = existing.createdAt,
            updatedAt = now
          )
          val updateAction = Tables.buckets.filter(_.id === id).update(updatedRow)
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
      val query = Tables.buckets.filter(_.id === id).delete
      val result = Await.result(db.run(query), 10.seconds)
      result > 0
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def deleteByProjectId(projectId: Long): Boolean = {
    try {
      val query = Tables.buckets.filter(_.projectId === projectId).delete
      val result = Await.result(db.run(query), 10.seconds)
      result > 0
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def countByProjectId(projectId: Long): Long = {
    try {
      val query = Tables.buckets.filter(_.projectId === projectId).length
      Await.result(db.run(query.result), 10.seconds).toLong
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0L
    }
  }

  def getDoneBuckets(projectId: Long): Seq[BucketResponse] = {
    try {
      val query = Tables.buckets
        .filter(_.projectId === projectId)
        .filter(_.isDoneBucket === true)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(BucketResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getPendingBuckets(projectId: Long): Seq[BucketResponse] = {
    try {
      val query = Tables.buckets
        .filter(_.projectId === projectId)
        .filter(_.isDoneBucket === false)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(BucketResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getByProjectIdWithStats(projectId: Long): Seq[(BucketResponse, Long, Long)] = {
    try {
      val buckets = getByProjectId(projectId)
      buckets.map { bucket =>
        val totalTasks = TaskService.countByBucketId(bucket.id)
        val completedTasks = TaskService.countCompletedByBucketId(bucket.id)
        (bucket, totalTasks, completedTasks)
      }
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }
}