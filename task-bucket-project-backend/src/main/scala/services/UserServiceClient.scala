package services

import com.greenfossil.commons.json.{Json, JsValue}
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.HttpStatus
import com.typesafe.config.ConfigFactory

object UserServiceClient:
  private val config = ConfigFactory.load()
  private val baseUrl = config.getString("userService.baseUrl")
  private val client = WebClient.of(baseUrl)

  def userExists(userId: Long): Boolean =
    getUser(userId).isDefined

  def getUser(userId: Long): Option[JsValue] =
    val cacheKey = s"user:$userId"

    RedisService.getUser(cacheKey) match
      case Some("miss") => None
      case Some(cached) =>
        try Some(Json.parse(cached))
        catch case _: Exception => None
      case None =>
        fetchUser(userId)

  private def fetchUser(userId: Long): Option[JsValue] =
    val cacheKey = s"user:$userId"
    try
      val response = client.blocking().get(s"/api/user/$userId")
      if response.status() == HttpStatus.OK then
        val json = response.contentUtf8()
        RedisService.setUser(cacheKey, json)
        Some(Json.parse(json))
      else
        RedisService.setUser(cacheKey, "miss")
        None
    catch
      case _: Exception => None
