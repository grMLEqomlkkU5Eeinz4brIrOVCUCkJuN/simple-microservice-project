package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.UserRow

case class UserResponse(id: Long, email: String, name: String)

object UserResponse:
  def fromRow(row: UserRow): UserResponse =
    UserResponse(row.id, row.email, row.name)

  def toJson(user: UserResponse): JsValue =
    Json.obj(
      "id" -> user.id,
      "email" -> user.email,
      "name" -> user.name
    )