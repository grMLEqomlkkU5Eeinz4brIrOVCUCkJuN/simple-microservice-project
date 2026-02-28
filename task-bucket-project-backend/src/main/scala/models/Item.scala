package models

import com.greenfossil.commons.json.{Json, JsValue}
import db.ItemRow

case class ItemResponse(id: Long, name: String, description: String)

object ItemResponse:
  def fromRow(row: ItemRow): ItemResponse =
    ItemResponse(row.id, row.name, row.description)

  def toJson(item: ItemResponse): JsValue =
    Json.obj(
      "id" -> item.id,
      "name" -> item.name,
      "description" -> item.description
    )
