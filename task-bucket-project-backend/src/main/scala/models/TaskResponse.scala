package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.TaskRow

import java.time.LocalDateTime

case class TaskResponse(
                         id: Long = 0L,
                         bucketId: Long,
                         isTaskDone: Boolean,
                         TaskName: String,
                         TaskDesc: Option[String] = None,
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
      TaskName = row.TaskName,
      TaskDesc = row.TaskDesc,
      createdByUserId = row.createdByUserId,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
    )

  def toJson(task: TaskResponse): JsValue =
    Json.obj(
      "id" -> task.id,
      "bucketId" -> task.bucketId,
      "isTaskDone" -> task.isTaskDone,
      "TaskName" -> task.TaskName,
      "TaskDesc" -> task.TaskDesc,
      "createdByUserId" -> task.createdByUserId,
      "createdAt" -> task.createdAt,
      "updatedAt" -> task.updatedAt
    )
}