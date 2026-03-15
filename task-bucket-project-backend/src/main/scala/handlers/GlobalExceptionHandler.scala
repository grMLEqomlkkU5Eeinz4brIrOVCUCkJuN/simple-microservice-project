package handlers

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{HttpRequest, HttpResponse, HttpStatus, MediaType}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext, SimpleDecoratingHttpService}
import exceptions.AppException
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

class GlobalExceptionHandler(delegate: HttpService) extends SimpleDecoratingHttpService(delegate) {

  private val logger = LoggerFactory.getLogger(classOf[GlobalExceptionHandler])

  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse = {
    try {
      val response = delegate.serve(ctx, req)
      response.recover {
        case ex: AppException =>
          createErrorResponse(ex.httpStatus, getErrorName(ex), ex.getMessage)
        case ex: Throwable =>
          logger.error("Unhandled exception in request handler", ex)
          createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred"
          )
      }
    } catch {
      case ex: AppException =>
        createErrorResponse(ex.httpStatus, getErrorName(ex), ex.getMessage)
      case ex if NonFatal(ex) =>
        logger.error("Unhandled exception in request handler", ex)
        createErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "INTERNAL_SERVER_ERROR",
          "An unexpected error occurred"
        )
    }
  }

  private def getErrorName(ex: AppException): String = {
    ex match {
      case _: exceptions.NotFoundException => "NOT_FOUND"
      case _: exceptions.BadRequestException => "BAD_REQUEST"
      case _: exceptions.ConflictException => "CONFLICT"
      case _: exceptions.UnauthorizedException => "UNAUTHORIZED"
      case _: exceptions.UnprocessableEntityException => "UNPROCESSABLE_ENTITY"
    }
  }

  private def createErrorResponse(status: HttpStatus, errorType: String, message: String): HttpResponse = {
    val json = Json.obj(
      "error" -> errorType,
      "message" -> message,
      "timestamp" -> System.currentTimeMillis()
    )
    HttpResponse.builder()
      .status(status)
      .content(MediaType.JSON, json.stringify)
      .build()
  }
}
