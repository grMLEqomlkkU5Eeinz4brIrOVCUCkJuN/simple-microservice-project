package db

import slick.jdbc.MySQLProfile.api.*
import java.time.LocalDateTime

case class ItemRow(
                    id: Long = 0L,
                    name: String,
                    description: String = "",
                    createdAt: LocalDateTime = LocalDateTime.now()
                  )

class ItemsTable(tag: Tag) extends Table[ItemRow](tag, "items"):
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.Length(255))
  def description = column[String]("description", O.Length(1024))
  def createdAt = column[LocalDateTime]("created_at")
  def * = (id, name, description, createdAt).mapTo[ItemRow]

object Tables:
  val items = TableQuery[ItemsTable]
