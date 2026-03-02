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
    request.findCookie("auth_token").flatMap { cookie =>
      val token = cookie.value()
      JwtService.validateToken(token).toOption.map((id, email) => AuthenticatedUser(id, email))
    }