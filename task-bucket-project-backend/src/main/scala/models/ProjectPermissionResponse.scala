package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.ProjectPermissionRow

import java.time.LocalDateTime

case class ProjectPermissionResponse(
                                      id: Long = 0L,
                                      projectId: Long,
                                      userId: Long,
                                      permissionLevel: String,
                                      sharedByUserId: Long,
                                      createdAt: LocalDateTime = LocalDateTime.now(),
                                      revokedAt: Option[LocalDateTime] = None
                                    )

object ProjectPermissionResponse {
  def fromRow(row: ProjectPermissionRow): ProjectPermissionResponse =
    ProjectPermissionResponse(
      id = row.id,
      projectId = row.projectId,
      userId = row.userId,
      permissionLevel = row.permissionLevel,
      sharedByUserId = row.sharedByUserId,
      createdAt = row.createdAt,
      revokedAt = row.revokedAt,
    )

  def toJson(permission: ProjectPermissionResponse): JsValue =
    Json.obj(
      "id" -> permission.id,
      "projectId" -> permission.projectId,
      "userId" -> permission.userId,
      "permissionLevel" -> permission.permissionLevel,
      "sharedByUserId" -> permission.sharedByUserId,
      "createdAt" -> permission.createdAt,
      "revokedAt" -> permission.revokedAt
    )
}