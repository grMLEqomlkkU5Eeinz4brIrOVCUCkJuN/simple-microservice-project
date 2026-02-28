package services

import cats.data.{ValidatedNel, Validated}
import cats.syntax.all.*
import db.{Database, Tables, ProjectPermissionRow}
import models.ProjectPermissionResponse
import slick.jdbc.MySQLProfile.api.*

import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.*

object ProjectPermissionService {
  private val db = Database.db
  private val logger = LoggerFactory.getLogger(getClass)

  private def validateProjectId(projectId: Long): ValidatedNel[String, Long] =
    if projectId > 0 then projectId.validNel
    else "Project ID must be greater than 0".invalidNel

  private def validateUserId(userId: Long): ValidatedNel[String, Long] =
    if userId > 0 then userId.validNel
    else "User ID must be greater than 0".invalidNel

  private def validatePermissionLevel(level: String): ValidatedNel[String, String] =
    if level == "READONLY" || level == "EDIT" then level.validNel
    else "Permission level must be READONLY or EDIT".invalidNel

  def share(
    projectId: Long,
    userId: Long,
    permissionLevel: String,
    sharedByUserId: Long
  ): ValidatedNel[String, ProjectPermissionResponse] = {
    val validation = (
      validateProjectId(projectId),
      validateUserId(userId),
      validatePermissionLevel(permissionLevel),
      validateUserId(sharedByUserId)
    ).mapN((_, _, _, _))

    validation.andThen { _ =>
      try {
        val row = ProjectPermissionRow(
          projectId = projectId,
          userId = userId,
          permissionLevel = permissionLevel,
          sharedByUserId = sharedByUserId,
          createdAt = LocalDateTime.now()
        )
        val insertAction = (Tables.projectPermissions returning Tables.projectPermissions.map(_.id) into ((perm, id) => perm.copy(id = id))) += row
        val insertedRow = Await.result(db.run(insertAction), 10.seconds)
        ProjectPermissionResponse.fromRow(insertedRow).validNel
      } catch {
        case e: Exception => s"Failed to share project: ${e.getMessage}".invalidNel
      }
    }
  }

  def getById(id: Long): Option[ProjectPermissionResponse] = {
    try {
      val query = Tables.projectPermissions.filter(_.id === id)
      val result = Await.result(db.run(query.result.headOption), 10.seconds)
      result.map(ProjectPermissionResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        None
    }
  }

  def getPermission(projectId: Long, userId: Long): Option[ProjectPermissionResponse] = {
    try {
      val query = Tables.projectPermissions
        .filter(_.projectId === projectId)
        .filter(_.userId === userId)
        .filter(_.revokedAt.isEmpty)
      val result = Await.result(db.run(query.result.headOption), 10.seconds)
      result.map(ProjectPermissionResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        None
    }
  }

  def getProjectPermissions(projectId: Long): Seq[ProjectPermissionResponse] = {
    try {
      val query = Tables.projectPermissions
        .filter(_.projectId === projectId)
        .filter(_.revokedAt.isEmpty)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectPermissionResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def getUserPermissions(userId: Long): Seq[ProjectPermissionResponse] = {
    try {
      val query = Tables.projectPermissions
        .filter(_.userId === userId)
        .filter(_.revokedAt.isEmpty)
        .sortBy(_.createdAt.desc)
      val result = Await.result(db.run(query.result), 10.seconds)
      result.map(ProjectPermissionResponse.fromRow)
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        Seq.empty
    }
  }

  def updatePermissionLevel(projectId: Long, userId: Long, newLevel: String): Boolean = {
    validatePermissionLevel(newLevel).fold(
      _ => false,
      _ => {
        try {
          val query = Tables.projectPermissions
            .filter(_.projectId === projectId)
            .filter(_.userId === userId)
            .filter(_.revokedAt.isEmpty)
          val updateAction = query.map(_.permissionLevel).update(newLevel)
          val result = Await.result(db.run(updateAction), 10.seconds)
          result > 0
        } catch {
          case e: Exception =>
            logger.error("Operation failed", e)
            false
        }
      }
    )
  }

  def revokePermission(projectId: Long, userId: Long): Boolean = {
    try {
      val query = Tables.projectPermissions
        .filter(_.projectId === projectId)
        .filter(_.userId === userId)
        .filter(_.revokedAt.isEmpty)
      val updateAction = query.map(_.revokedAt).update(Some(LocalDateTime.now()))
      val result = Await.result(db.run(updateAction), 10.seconds)
      result > 0
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def revokeAllProjectPermissions(projectId: Long): Boolean = {
    try {
      val query = Tables.projectPermissions
        .filter(_.projectId === projectId)
        .filter(_.revokedAt.isEmpty)
      val updateAction = query.map(_.revokedAt).update(Some(LocalDateTime.now()))
      val result = Await.result(db.run(updateAction), 10.seconds)
      result >= 0  // Returns number of rows updated
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        false
    }
  }

  def hasPermission(projectId: Long, userId: Long, requiredLevel: String): Boolean = {
    getPermission(projectId, userId) match {
      case Some(perm) =>
        requiredLevel match {
          case "READONLY" => perm.permissionLevel == "READONLY" || perm.permissionLevel == "EDIT"
          case "EDIT" => perm.permissionLevel == "EDIT"
          case _ => false
        }
      case None => false
    }
  }

  def countActivePermissions(projectId: Long): Long = {
    try {
      val query = Tables.projectPermissions
        .filter(_.projectId === projectId)
        .filter(_.revokedAt.isEmpty)
        .length
      Await.result(db.run(query.result), 10.seconds).toLong
    } catch {
      case e: Exception =>
        logger.error("Operation failed", e)
        0L
    }
  }
}
