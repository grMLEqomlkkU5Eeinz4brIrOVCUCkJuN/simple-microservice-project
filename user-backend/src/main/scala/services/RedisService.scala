package services

import com.typesafe.config.ConfigFactory
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import java.net.URI

object RedisService:
  private val config = ConfigFactory.load()
  private val redisUrl = config.getString("redis.url")
  private val pool = new JedisPool(new JedisPoolConfig(), URI(redisUrl))

  def invalidateUser(userId: Long): Unit =
    val jedis = pool.getResource
    try
      jedis.del(s"user:$userId")
    finally
      jedis.close()
