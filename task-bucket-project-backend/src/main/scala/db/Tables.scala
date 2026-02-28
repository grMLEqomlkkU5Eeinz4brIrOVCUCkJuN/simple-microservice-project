package db

import slick.jdbc.MySQLProfile.api.*
import java.time.LocalDateTime

case class ProjectRow(
                     id: Long = 0L,
                     isPublic: Boolean,
                     isShared: Boolean,
                     ownerId: Long = 0L,
                     viewPasswordHash: Option[String] = None,
                     editPasswordHash: Option[String] = None,
                     projectName: String,
                     createdAt: LocalDateTime = LocalDateTime.now(),
                     updatedAt: LocalDateTime = LocalDateTime.now()
                     )

case class BucketRow(
                    id: Long = 0L,
                    bucketName: String,
                    projectId: Long,
                    isDoneBucket: Boolean,
                    createdByUserId: Long,
                    createdAt: LocalDateTime = LocalDateTime.now(),
                    updatedAt: LocalDateTime = LocalDateTime.now()
                    )

case class TaskRow(
                  id: Long = 0L,
                  bucketId: Long,
                  isTaskDone: Boolean,
                  TaskName: String,
                  TaskDesc: Option[String] = None,
                  createdByUserId: Long,
                  createdAt: LocalDateTime = LocalDateTime.now(),
                  updatedAt: LocalDateTime = LocalDateTime.now()
                  )

class ProjectTable(tag: Tag) extends Table[ProjectRow](tag, "projects"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def isPublic = column[Boolean]("is_public")
  def isShared = column[Boolean]("is_shared")
  def ownerId = column[Long]("owner_id")
  def viewPasswordHash = column[Option[String]]("view_password_hash")
  def editPasswordHash = column[Option[String]]("edit_password_hash")
  def projectName = column[String]("project_name")
  def createdAt = column[LocalDateTime]("created_at")
  def updatedAt = column[LocalDateTime]("updated_at")

  def * = (id, isPublic, isShared, ownerId, viewPasswordHash, editPasswordHash, projectName, createdAt, updatedAt).mapTo[ProjectRow]

class BucketTable(tag: Tag) extends Table[BucketRow](tag, "buckets"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def bucketName = column[String]("bucket_name")
  def projectId = column[Long]("project_id")
  def isDoneBucket = column[Boolean]("is_done_bucket")
  def createdByUserId = column[Long]("created_by_user_id")
  def createdAt = column[LocalDateTime]("created_at")
  def updatedAt = column[LocalDateTime]("updated_at")

  def * = (id, bucketName, projectId, isDoneBucket, createdByUserId, createdAt, updatedAt).mapTo[BucketRow]

class TaskTable(tag: Tag) extends Table[TaskRow](tag, "tasks"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def bucketId = column[Long]("bucket_id")
  def isTaskDone = column[Boolean]("is_task_done")
  def TaskName = column[String]("task_name")
  def TaskDesc = column[Option[String]]("task_desc")
  def createdByUserId = column[Long]("created_by_user_id")
  def createdAt = column[LocalDateTime]("created_at")
  def updatedAt = column[LocalDateTime]("updated_at")

  def * = (id, bucketId, isTaskDone, TaskName, TaskDesc, createdByUserId, createdAt, updatedAt).mapTo[TaskRow]

object Tables:
  val projects = TableQuery[ProjectTable]
  val buckets = TableQuery[BucketTable]
  val tasks = TableQuery[TaskTable]