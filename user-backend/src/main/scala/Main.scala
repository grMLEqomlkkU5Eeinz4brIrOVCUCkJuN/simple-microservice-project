import com.greenfossil.thorium.{Server, Action}

@main def start: Unit =
  Server()
    .addHttpService("/", Action(request => "Welcome to Thorium!"))
    .addServices(controllers.SimpleController)
    .addDocService("/docs")
    .start()
