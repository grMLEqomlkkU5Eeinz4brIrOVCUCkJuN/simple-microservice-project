package exceptions

import com.linecorp.armeria.common.HttpStatus

sealed abstract class AppException(message: String) extends Exception(message) {
  def httpStatus: HttpStatus
}

case class NotFoundException(message: String) extends AppException(message) {
  override def httpStatus: HttpStatus = HttpStatus.NOT_FOUND
}

case class BadRequestException(message: String) extends AppException(message) {
  override def httpStatus: HttpStatus = HttpStatus.BAD_REQUEST
}

case class ConflictException(message: String) extends AppException(message) {
  override def httpStatus: HttpStatus = HttpStatus.CONFLICT
}

case class UnauthorizedException(message: String) extends AppException(message) {
  override def httpStatus: HttpStatus = HttpStatus.UNAUTHORIZED
}

case class UnprocessableEntityException(message: String) extends AppException(message) {
  override def httpStatus: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY
}