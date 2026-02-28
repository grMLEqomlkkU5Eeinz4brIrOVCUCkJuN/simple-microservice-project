package handlers

case class ErrorResponse(
                          error: String,
                          message: String,
                          timestamp: Long = System.currentTimeMillis()
                        )
