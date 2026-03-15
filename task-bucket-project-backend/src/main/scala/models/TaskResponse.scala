package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.TaskRow

import java.time.LocalDateTime

case class TaskResponse(
                         id: Long = 0L,
                         bucketId: Long,
                         isTaskDone: Boolean,
                         taskName: String,
                         taskDesc: Option[String] = None,
                         createdByUserId: Long,
                         createdAt: LocalDateTime = LocalDateTime.now(),
                         updatedAt: LocalDateTime = LocalDateTime.now()
                       )

object TaskResponse {
  def fromRow(row: TaskRow): TaskResponse =
    TaskResponse(
      id = row.id,
      bucketId = row.bucketId,
      isTaskDone = row.isTaskDone,
      taskName = row.taskName,
      taskDesc = row.taskDesc,
      createdByUserId = row.createdByUserId,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
    )

  def toJson(task: TaskResponse): JsValue =
    Json.obj(
      "id" -> task.id,
      "bucketId" -> task.bucketId,
      "isTaskDone" -> task.isTaskDone,
      "taskName" -> task.taskName,
      "taskDesc" -> task.taskDesc,
      "createdByUserId" -> task.createdByUserId,
      "createdAt" -> task.createdAt,
      "updatedAt" -> task.updatedAt
    )
}
