package services

import com.greenfossil.commons.json.{Json, JsValue}
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.HttpStatus
import com.typesafe.config.ConfigFactory

import java.util.concurrent.ConcurrentHashMap

object UserServiceClient:
  private val config = ConfigFactory.load()
  private val baseUrl = config.getString("userService.baseUrl")
  private val client = WebClient.of(baseUrl)

  private case class CacheEntry(exists: Boolean, timestamp: Long)
  private val cache = new ConcurrentHashMap[Long, CacheEntry]()
  private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes

  def userExists(userId: Long): Boolean =
    val now = System.currentTimeMillis()
    val cached = cache.get(userId)
    if cached != null && (now - cached.timestamp) < cacheTtlMs then
      cached.exists
    else
      val exists = fetchUserExists(userId)
      cache.put(userId, CacheEntry(exists, now))
      exists

  def getUser(userId: Long): Option[JsValue] =
    try
      val response = client.blocking().get(s"/api/user/$userId")
      if response.status() == HttpStatus.OK then
        Some(Json.parse(response.contentUtf8()))
      else
        None
    catch
      case _: Exception => None

  private def fetchUserExists(userId: Long): Boolean =
    try
      val response = client.blocking().get(s"/api/user/$userId")
      response.status() == HttpStatus.OK
    catch
      case _: Exception => false

  def invalidateCache(userId: Long): Unit =
    cache.remove(userId)

  def clearCache(): Unit =
    cache.clear()
