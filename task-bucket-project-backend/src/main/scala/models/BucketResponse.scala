package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.BucketRow

import java.time.LocalDateTime

case class BucketResponse(
                           id: Long = 0L,
                           bucketName: String,
                           projectId: Long,
                           isDoneBucket: Boolean,
                           createdByUserId: Long,
                           createdAt: LocalDateTime = LocalDateTime.now(),
                           updatedAt: LocalDateTime = LocalDateTime.now()
                         )

object BucketResponse {
  def fromRow(row: BucketRow): BucketResponse =
    BucketResponse(
      id = row.id,
      bucketName = row.bucketName,
      projectId = row.projectId,
      isDoneBucket = row.isDoneBucket,
      createdByUserId = row.createdByUserId,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
    )

  def toJson(bucket: BucketResponse): JsValue =
    Json.obj(
      "id" -> bucket.id,
      "bucketName" -> bucket.bucketName,
      "projectId" -> bucket.projectId,
      "isDoneBucket" -> bucket.isDoneBucket,
      "createdByUserId" -> bucket.createdByUserId,
      "createdAt" -> bucket.createdAt,
      "updatedAt" -> bucket.updatedAt
    )
}