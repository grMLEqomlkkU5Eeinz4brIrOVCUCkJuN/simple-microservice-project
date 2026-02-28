package services

import db.{Database, Tables, ItemRow}
import slick.jdbc.MySQLProfile.api.*

import scala.concurrent.duration.*
import scala.concurrent.Await

object ItemService:
  private val db = Database.db

  def create(name: String, description: String): ItemRow =
    val row = ItemRow(name = name, description = description)
    val insertAction = (Tables.items returning Tables.items.map(_.id) into ((item, id) => item.copy(id = id))) += row
    Await.result(db.run(insertAction), 10.seconds)

  def findAll(): Seq[ItemRow] =
    Await.result(
      db.run(Tables.items.result),
      10.seconds
    )

  def findById(id: Long): Option[ItemRow] =
    Await.result(
      db.run(Tables.items.filter(_.id === id).result.headOption),
      10.seconds
    )

  def update(id: Long, name: String, description: String): Option[ItemRow] =
    val updated = Await.result(
      db.run(Tables.items.filter(_.id === id).map(i => (i.name, i.description)).update((name, description))),
      10.seconds
    )
    if updated > 0 then findById(id) else None

  def delete(id: Long): Boolean =
    val deleted = Await.result(
      db.run(Tables.items.filter(_.id === id).delete),
      10.seconds
    )
    deleted > 0
