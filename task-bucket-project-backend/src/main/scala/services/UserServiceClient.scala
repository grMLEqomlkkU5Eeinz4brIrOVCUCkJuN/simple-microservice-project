package services

import com.greenfossil.commons.json.{Json, JsValue}
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.{HttpHeaderNames, HttpStatus}
import com.typesafe.config.ConfigFactory

import java.time.Duration

object UserServiceClient:
  private val config = ConfigFactory.load()
  private val baseUrl = config.getString("userService.baseUrl")
  private val apiKey = config.getString("userService.apiKey")
  private val client = WebClient.builder(baseUrl)
    .responseTimeout(Duration.ofSeconds(5))
    .writeTimeout(Duration.ofSeconds(5))
    .build()

  def userExists(userId: Long): Boolean =
    getUser(userId).isDefined

  def getUser(userId: Long): Option[JsValue] =
    val cacheKey = s"taskbucket:user:$userId"

    RedisService.getUser(cacheKey) match
      case Some("miss") => None
      case Some(cached) =>
        try Some(Json.parse(cached))
        catch case _: Exception => None
      case None =>
        fetchUser(userId)

  private def fetchUser(userId: Long): Option[JsValue] =
    val cacheKey = s"taskbucket:user:$userId"
    try
      val response = client.blocking().prepare()
        .get(s"/api/user/$userId")
        .header("X-API-Key", apiKey)
        .execute()
      if response.status() == HttpStatus.OK then
        val json = response.contentUtf8()
        RedisService.setUser(cacheKey, json)
        Some(Json.parse(json))
      else
        RedisService.setUser(cacheKey, "miss")
        None
    catch
      case _: Exception => None
