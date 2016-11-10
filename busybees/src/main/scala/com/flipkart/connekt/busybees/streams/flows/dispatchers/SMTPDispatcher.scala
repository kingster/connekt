/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.util.Properties
import javax.mail._
import javax.mail.internet.{MimeBodyPart, MimeMessage, MimeMultipart}

import akka.stream.scaladsl.Flow
import com.flipkart.connekt.busybees.models.EmailRequestTracker
import com.flipkart.connekt.commons.entities.SimpleCredential
import com.flipkart.connekt.commons.iomodels.EmailPayloadEnvelope

import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.Try

class SMTPDispatcher(host: String, credentials: SimpleCredential, parallelism: Int)(implicit ec: ExecutionContextExecutor) {

  def flow = {

    Flow[(EmailPayloadEnvelope, EmailRequestTracker)].mapAsyncUnordered(parallelism) {
      case (request, userContext) ⇒
        val result = Promise[(Try[Any], EmailRequestTracker)]()

        Future {
          val props = new Properties()
          props.put("mail.smtp.host", host)
          props.put("mail.smtp.port", "25")
          props.put("mail.smtp.auth", "true")

          val session = Session.getDefaultInstance(props,
            new javax.mail.Authenticator {
              override protected def getPasswordAuthentication: PasswordAuthentication = {
                new PasswordAuthentication(credentials.username, credentials.password)
              }
            })
          session.setDebug(true)

          val msg = new MimeMessage(session)

          msg.setFrom(request.payload.from.toInternetAddress)
          msg.setReplyTo(Array(request.payload.replyTo.toInternetAddress))
          msg.setRecipients(Message.RecipientType.TO, request.payload.to.map(_.toInternetAddress.asInstanceOf[Address]).toArray)
          msg.setRecipients(Message.RecipientType.CC, request.payload.cc.map(_.toInternetAddress.asInstanceOf[Address]).toArray)
          msg.setRecipients(Message.RecipientType.BCC, request.payload.bcc.map(_.toInternetAddress.asInstanceOf[Address]).toArray)
          msg.setSubject(request.payload.data.subject)

          val multiPart = new MimeMultipart("alternative")

          val textPart = new MimeBodyPart()
          textPart.setText(request.payload.data.text, "utf-8")

          val htmlPart = new MimeBodyPart()
          htmlPart.setContent(request.payload.data.html, "text/html; charset=utf-8")

          multiPart.addBodyPart(htmlPart)
          multiPart.addBodyPart(textPart)
          msg.setContent(multiPart)

          Transport.send(msg)

        }(ec).onComplete(responseTry ⇒ result.success(responseTry -> userContext))(ec)

        result.future
    }
  }

}
