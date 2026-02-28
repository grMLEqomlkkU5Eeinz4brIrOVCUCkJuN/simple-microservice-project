package controllers

import com.linecorp.armeria.server.annotation.{Get, Param}

object SimpleController:

  @Get("/sayHello/:name")
  def sayHello(@Param name: String) = s"Hello $name"