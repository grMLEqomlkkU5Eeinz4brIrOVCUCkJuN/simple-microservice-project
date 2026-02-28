package handlers

import com.greenfossil.commons.json.Json
import com.linecorp.armeria.common.{HttpRequest, HttpResponse, HttpStatus, MediaType}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext, SimpleDecoratingHttpService}
import exceptions.AppException
import scala.util.control.NonFatal

class GlobalExceptionHandler(delegate: HttpService) extends SimpleDecoratingHttpService(delegate) {

  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse = {
    try {
      val response = delegate.serve(ctx, req)
      response.recover {
        case ex: AppException =>
          createErrorResponse(ex.httpStatus, getErrorName(ex), ex.getMessage)
        case _: Throwable =>
          createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred"
          )
      }
    } catch {
      case ex: AppException =>
        createErrorResponse(ex.httpStatus, getErrorName(ex), ex.getMessage)
      case NonFatal(_) =>
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