package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.UserRow

case class UserResponse(id: Long, email: String, name: String):
  def toJson: JsValue =
    Json.obj(
      "id" -> id,
      "email" -> email,
      "name" -> name
    )

object UserResponse:
  def fromRow(row: UserRow): UserResponse =
    UserResponse(row.id, row.email, row.name)