package services

import cats.data.ValidatedNel
import cats.syntax.all.*
import db.{Database, Tables, ProjectRow}
import models.ProjectResponse
import slick.jdbc.MySQLProfile.api.*

import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.*

object ProjectService {
  private val db = Database.db
  private val logger = LoggerFactory.getLogger(getClass)

  private def validateProjectName(name: String): ValidatedNel[String, String] =
    if name.nonEmpty then name.validNel
    else "Project name cannot be empty".invalidNel

  private def validateOwnerId(ownerId: Long): ValidatedNel[String, Long] =
    if ownerId > 0 then ownerId.validNel
    else "Owner ID must be greater than 0".invalidNel

  def updateWithAuth(
    id: Long,
    projectName: Option[String],
    isPublic: Option[Boolean],
    isShared: Option[Boolean],
    viewPasswordHash: Option[Option[String]],
    editPasswordHash: Option[Option[String]],
    userId: Long  // The user performing the action
  ): Either[ServiceError, ProjectResponse] =
    getById(id) match
      case Some(project) =>
        AuthorizationService.canEditProject(project, userId) match
          case Right(_) =>
            if update(id, projectName, isPublic, isShared, viewPasswordHash, editPasswordHash) then
              getById(id).toRight(OperationFailed("Failed to retrieve updated project"))
            else
              Left(OperationFailed("Failed to update project"))
          case Left(error) =>
            Left(AuthorizationError(error))
      case None => Left(NotFoundError("Project"))

  def deleteWithAuth(id: Long, userId: Long): Either[ServiceError, Unit] =
    getById(id) match
      case Some(project) =>
        AuthorizationService.canDeleteProject(project, userId) match
          case Right(_) =>
            if delete(id) then Right(()) else Left(OperationFailed("Failed to delete project"))
          case Left(error) =>
            Left(AuthorizationError(error))
      case None => Left(NotFoundError("Project"))

  def create(
    projectName: String,
    ownerId: Long,
    isPublic: Boolean = false,
    isShared: Boolean = false,
    viewPasswordHash: Option[String] = None,
    editPasswordHash: Option[String] = None
  ): ValidatedNel[String, ProjectResponse] = {
    val validation = (validateProjectName(projectName), validateOwnerId(ownerId)).mapN((_, _))

    validation.andThen { _ =>
      try {
        val now = LocalDateTime.now()
        val row = ProjectRow(
          projectName = projectName,
          ownerId = ownerId,
          isPublic = isPublic,
          isShared = isShared,
          viewPasswordHash = viewPasswordHash,
          editPasswordHash = editPasswordHash,
          createdAt = now,
          updatedAt = now
        )
        val insertAction = (Tables.projects returning Tables.projects.map(_.id) into ((project, id) => project.copy(id = id))) += row
        val insertedRow = Await.result(db.run(insertAction), 10.seconds)
        ProjectResponse.fromRow(insertedRow).validNel
      } catch {
        case e: Exception => s"Failed to create project: ${e.getMessage}".invalidNel
      }
    }
  }

  def getById(id: Long): Option[ProjectResponse] = {
    try {
      val query = Tables.projects.filter(_.id === id)
      val result = Await.result(db.run(query.result.headOption), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        None
    }
  }

  def getAll(): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects.sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getByOwnerId(ownerId: Long): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects
        .filter(_.ownerId === ownerId)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getPublicProjects(): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects
        .filter(_.isPublic === true)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getSharedProjects(): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects
        .filter(_.isShared === true)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def update(
    id: Long,
    projectName: Option[String] = None,
    isPublic: Option[Boolean] = None,
    isShared: Option[Boolean] = None,
    viewPasswordHash: Option[Option[String]] = None,
    editPasswordHash: Option[Option[String]] = None
  ): Boolean = {
    try {
      getById(id) match {
        case Some(existing) =>
          val now = LocalDateTime.now()
          val updatedRow = ProjectRow(
            id = existing.id,
            projectName = projectName.getOrElse(existing.projectName),
            ownerId = existing.ownerId,
            isPublic = isPublic.getOrElse(existing.isPublic),
            isShared = isShared.getOrElse(existing.isShared),
            viewPasswordHash = viewPasswordHash.getOrElse(existing.viewPasswordHash),
            editPasswordHash = editPasswordHash.getOrElse(existing.editPasswordHash),
            createdAt = existing.createdAt,
            updatedAt = now
          )
          val updateAction = Tables.projects.filter(_.id === id).update(updatedRow)
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
      val deleteAction = DBIO.seq(
        // Delete tasks belonging to this project's buckets
        Tables.tasks.filter(_.bucketId in Tables.buckets.filter(_.projectId === id).map(_.id)).delete,
        // Delete buckets belonging to this project
        Tables.buckets.filter(_.projectId === id).delete,
        // Delete all permissions
        Tables.projectPermissions.filter(_.projectId === id).delete,
        // Delete the project itself
        Tables.projects.filter(_.id === id).delete
      ).transactionally
      Await.result(db.run(deleteAction), 10.seconds)
      true
    } catch {
      case e: Exception =>
        logger.error("Failed to delete project with cascade", e)
        false
    }
  }

  def countByOwnerId(ownerId: Long): Long = {
    try {
      val query = Tables.projects.filter(_.ownerId === ownerId).length
      Await.result(db.run(query.result), 10.seconds).toLong
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0L
    }
  }

  def exists(projectId: Long): Boolean = {
    try {
      val query = Tables.projects.filter(_.id === projectId).exists
      Await.result(db.run(query.result), 10.seconds)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def searchByName(pattern: String): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects
        .filter(_.projectName.like(s"%$pattern%"))
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getRecentByOwnerId(ownerId: Long, limit: Int = 10): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects
        .filter(_.ownerId === ownerId)
        .sortBy(_.updatedAt.desc)
        .take(limit)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getProjectWithStats(projectId: Long): Option[(ProjectResponse, Long, Long)] = {
    try {
      getById(projectId) match {
        case Some(project) =>
          val bucketCount = BucketService.countByProjectId(projectId)
          val taskCount = countTasksByProjectId(projectId)
          Some((project, bucketCount, taskCount))
        case None => None
      }
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        None
    }
  }

  private def countTasksByProjectId(projectId: Long): Long = {
    try {
      val query = Tables.tasks
        .filter(_.bucketId in Tables.buckets.filter(_.projectId === projectId).map(_.id))
        .length
      Await.result(db.run(query.result), 10.seconds).toLong
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0L
    }
  }

  def getSharedWithMe(userId: Long): Seq[ProjectResponse] = {
    try {
      val query = Tables.projects
        .filter(_.id in Tables.projectPermissions
          .filter(_.userId === userId)
          .filter(_.revokedAt.isEmpty)
          .map(_.projectId)
        )
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }
}