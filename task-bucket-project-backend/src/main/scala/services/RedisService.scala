package services

import com.typesafe.config.ConfigFactory
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import java.net.URI

object RedisService:
  private val config = ConfigFactory.load()
  private val redisUrl = config.getString("redis.url")
  private val pool = new JedisPool(new JedisPoolConfig(), URI(redisUrl))

  private val cacheTtlSeconds = 300 // 5 minutes

  def getUser(key: String): Option[String] =
    val jedis = pool.getResource
    try
      Option(jedis.get(key))
    finally
      jedis.close()

  def setUser(key: String, value: String): Unit =
    val jedis = pool.getResource
    try
      jedis.setex(key, cacheTtlSeconds, value)
    finally
      jedis.close()
