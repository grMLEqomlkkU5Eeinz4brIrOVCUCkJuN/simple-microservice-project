import com.greenfossil.thorium.{*, given}
import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.server.cors.CorsService
import com.typesafe.config.ConfigFactory

@main def start: Unit =
  db.Database.initialize()

  val config = ConfigFactory.load()

  Server(8080)
    .addServices(controllers.AuthController)
    .addDocService("/docs")
    .serverBuilderSetup { sb =>
      import scala.jdk.CollectionConverters.*

      val origins = config.getStringList("cors.allowedOrigins").asScala.toSeq
      val methods = config.getStringList("cors.allowedMethods").asScala.toSeq.map(m => HttpMethod.valueOf(m))
      val allowedHeaders: Seq[CharSequence] = config.getStringList("cors.allowedHeaders").asScala.toSeq
      val exposedHeaders: Seq[CharSequence] = config.getStringList("cors.exposedHeaders").asScala.toSeq
      val allowCredentials = config.getBoolean("cors.allowCredentials")
      val maxAge = config.getLong("cors.maxAge")

      val corsBuilder = CorsService.builder(origins*)
        .allowRequestMethods(methods*)
        .allowRequestHeaders(allowedHeaders*)
        .exposeHeaders(exposedHeaders*)
        .maxAge(maxAge)

      if allowCredentials then corsBuilder.allowCredentials()

      sb.decorator(corsBuilder.newDecorator())
    }
    .start()