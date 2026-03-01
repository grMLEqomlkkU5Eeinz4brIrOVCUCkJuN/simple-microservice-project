package services

import com.typesafe.config.ConfigFactory

import java.util.Properties
import javax.mail.*
import javax.mail.internet.{InternetAddress, MimeMessage}

object EmailService:
  private val config = ConfigFactory.load()
  private val smtpHost = config.getString("smtp.host")
  private val smtpPort = config.getInt("smtp.port")
  private val smtpUsername = config.getString("smtp.username")
  private val smtpPassword = config.getString("smtp.password")
  private val fromAddress = config.getString("smtp.from")
  private val starttls = config.getBoolean("smtp.starttls")
  private val verificationBaseUrl = config.getString("verification.baseUrl")

  private def createSession(): Session =
    val props = new Properties()
    props.put("mail.smtp.host", smtpHost)
    props.put("mail.smtp.port", smtpPort.toString)
    props.put("mail.smtp.starttls.enable", starttls.toString)

    if smtpUsername.nonEmpty then
      props.put("mail.smtp.auth", "true")
      Session.getInstance(props, new Authenticator:
        override def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(smtpUsername, smtpPassword)
      )
    else
      props.put("mail.smtp.auth", "false")
      Session.getInstance(props)

  def sendVerificationEmail(to: String, token: String): Unit =
    val verifyUrl = s"$verificationBaseUrl/api/auth/verify?token=$token"

    val session = createSession()
    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(fromAddress))
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to))
    message.setSubject("Verify your Kanbana account")
    message.setContent(
      s"""<html>
         |<body>
         |<h2>Welcome to Kanbana!</h2>
         |<p>Please click the link below to verify your email address and activate your account:</p>
         |<p><a href="$verifyUrl">Verify my email</a></p>
         |<p>If you did not create an account, you can safely ignore this email.</p>
         |</body>
         |</html>""".stripMargin,
      "text/html; charset=utf-8"
    )
    Transport.send(message)
