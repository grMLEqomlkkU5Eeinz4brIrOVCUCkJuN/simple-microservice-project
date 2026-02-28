package auth

import com.greenfossil.thorium.*
import com.greenfossil.commons.json.Json
import services.JwtService

case class AuthenticatedUser(id: Long, email: String)

object Authenticated:
  def apply(fn: (Request, AuthenticatedUser) => ActionResponse): Action =
    Action { implicit request =>
      extractUser(request) match
        case Some(user) => fn(request, user)
        case None =>
          Unauthorized(Json.obj(
            "error" -> "UNAUTHORIZED",
            "message" -> "Missing or invalid authentication token"
          ))
    }

  def withUser(request: Request)(fn: AuthenticatedUser => ActionResponse): ActionResponse =
    extractUser(request) match
      case Some(user) => fn(user)
      case None =>
        Unauthorized(Json.obj(
          "error" -> "UNAUTHORIZED",
          "message" -> "Missing or invalid authentication token"
        ))

  private def extractUser(request: Request): Option[AuthenticatedUser] =
    request.authorization.flatMap { header =>
      if header.startsWith("Bearer ") then
        val token = header.substring(7)
        JwtService.validateToken(token).toOption.map((id, email) => AuthenticatedUser(id, email))
      else None
    }