package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.ProjectRow

import java.time.LocalDateTime

case class ProjectResponse(
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

object ProjectResponse {
  def fromRow(row: ProjectRow): ProjectResponse =
    ProjectResponse(
      id = row.id,
      isPublic = row.isPublic,
      isShared = row.isShared,
      ownerId = row.ownerId,
      viewPasswordHash = row.viewPasswordHash,
      editPasswordHash = row.editPasswordHash,
      projectName = row.projectName,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
    )

  def toJson(project: ProjectResponse): JsValue =
    Json.obj(
      "id" -> project.id,
      "isPublic" -> project.isPublic,
      "isShared" -> project.isShared,
      "ownerId" -> project.ownerId,
      "viewPasswordHash" -> project.viewPasswordHash,
      "editPasswordHash" -> project.editPasswordHash,
      "projectName" -> project.projectName,
      "createdAt" -> project.createdAt,
      "updatedAt" -> project.updatedAt
    )
}