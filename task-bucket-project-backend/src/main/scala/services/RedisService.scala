package services

import com.typesafe.config.ConfigFactory
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import java.net.URI

object RedisService:
  private val config = ConfigFactory.load()
  private val redisUrl = config.getString("redis.url")

  private val poolConfig = new JedisPoolConfig()
  poolConfig.setMaxTotal(20)
  poolConfig.setMaxIdle(10)
  poolConfig.setMinIdle(2)
  poolConfig.setTestOnBorrow(true)
  poolConfig.setTimeBetweenEvictionRunsMillis(30000)
  poolConfig.setMinEvictableIdleTimeMillis(60000)

  private val pool = new JedisPool(poolConfig, URI(redisUrl))

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

  /** Check rate limit. Returns true if the request is allowed, false if rate limited. */
  def checkRateLimit(key: String, maxRequests: Int, windowSeconds: Int): Boolean =
    val jedis = pool.getResource
    try
      val fullKey = s"ratelimit:$key"
      val current = Option(jedis.get(fullKey)).map(_.toLong).getOrElse(0L)
      if current >= maxRequests then
        false
      else
        val pipeline = jedis.pipelined()
        pipeline.incr(fullKey)
        pipeline.expire(fullKey, windowSeconds.toLong)
        pipeline.sync()
        true
    finally
      jedis.close()

  def shutdown(): Unit =
    pool.close()
