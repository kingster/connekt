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
import javax.mail.internet.{InternetAddress, MimeMessage}

import akka.stream.scaladsl.Flow
import com.flipkart.connekt.busybees.models.EmailRequestTracker
import com.flipkart.connekt.commons.entities.SimpleCredential
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification

import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.Try

class SMTPDispatcher(host:String, credentials: SimpleCredential, parallelism: Int)(implicit ec: ExecutionContextExecutor) {

  def flow = {

    Flow[(SimpleApnsPushNotification, EmailRequestTracker)].mapAsyncUnordered(parallelism) {
      case (request, userContext) ⇒
        val result = Promise[(Try[Any], EmailRequestTracker)]()

        Future{
          val props = new Properties()
          props.put("mail.smtp.host", host)
          props.put("mail.smtp.port", "25")
//          props.put("mail.smtp.from", Config.getString("email.localsmtp.returnPath", "bounce@flipkart.com"));
          props.put("mail.smtps.auth","true")

          val session = Session.getDefaultInstance(props,
            new javax.mail.Authenticator {
              override protected def getPasswordAuthentication() :PasswordAuthentication = {
                 new PasswordAuthentication(credentials.username,credentials.password)
              }
            })

          try {

            val msg = new MimeMessage(session)

          msg.setFrom(new InternetAddress("hello@flipkart.com"))
          msg.setRecipients(Message.RecipientType.TO, Array(InternetAddress.parse("kinshuk.bairagi@flipkart.com").head.asInstanceOf[Address]))
          msg.setSubject("Testing Subject")
          msg.setText("Dear Mail Crawler," +
            "\n\n No spam to my email, please!")
            Transport.send(msg)

            System.out.println("Done");

          } catch {
            case e:MessagingException => throw new RuntimeException(e);

          }


          // set the from and to address
//          try {
//            msg.setFrom(toAddress(email.getSender(), messageId));
//            EmailID replyToEmail = email.getReplyTo();
//            if(!Util.isNullOrEmpty(replyToEmail) && !Util.isNullOrEmpty(replyToEmail.geteId())){
//              msg.setReplyTo(new Address[]{
//                toAddress(replyToEmail, messageId)
//              });
//            }
//
//
//          } catch (MessagingException e) {
//            failureRate.mark();
//            Log.singleton(FILE.PROVIDER).error("Trans Email Not Sent: Invalid emailid - id:"+messageId+":"+e.getMessage());
//            throw new CInternalException(CExceptionCode.INVALID_EMAIL_ID);
//          }
//
//          //		email.getBcc().add(new EmailID(BACKUP_BCC_EMAILID));
//          if ( Config.getBoolean("core.isProd", false) && Config.getBoolean("email.bccEnabled", true)) {
//            email.getBcc().add(new EmailID(BACK_BCC_LOCALEMAILID));
//          }
//
//          // Adding TO list to Recipients
//          addRecipeintsForMail(Message.RecipientType.TO, (ArrayList<EmailID>)email.getTo(), msg, messageId);
//          addRecipeintsForMail(Message.RecipientType.CC, (ArrayList<EmailID>)email.getCc(), msg, messageId);
//          addRecipeintsForMail(Message.RecipientType.BCC, (ArrayList<EmailID>)email.getBcc(), msg, messageId);
//
//          try {
//            msg.setSubject(email.getSubject(), CharEncoding.UTF_8);
//            Multipart multiPart = createBodyPart(email, messageId);
//            msg.setContent(multiPart);
//            msg.setHeader("MIME-Version", "1.0");
//            msg.setHeader("Content-Type",multiPart.getContentType());
//            msg.setSentDate(Util.now());
//          } catch (MessagingException e) {
//            failureRate.mark();
//            Log.singleton(FILE.PROVIDER).error("Trans Email Not Sent: messageId:"+messageId+":"+e.getMessage());
//            throw new CInternalException(CExceptionCode.UNKNOWN);
//          }
//
//          List<EmailID> allReceivers = new ArrayList<EmailID>();
//          allReceivers.addAll(email.getTo());
//          allReceivers.addAll(email.getCc());
//          // Sending Email
//          com.flipkart.w3.communication.models.Message message = ServiceFactory.getMessageService().getMessage(messageId);
//          try {
//            final Timer.Context context;
//
//            if(email.hasAttachments())
//              context = mssgSendTimeWithAsset.time();
//            else
//              context = mssgSendTime.time();
//
//            Transport.send(msg);
//
//            /* Send Internal Event */
//            if(null != message){
//              message.setStatus(MessageStatus.DELIVERED);
//
//              for(EmailID id: allReceivers)
//              AsyncHandlerUtil.sendInternalEvent(message, id.geteId());
//            }
//
//            context.stop();
//            Log.singleton(FILE.PROVIDER).info("LOCAL_SMTP_EMAIL_SENT:" + email.toString() + ":" + messageId);
//          } catch (MessagingException e) {
//            failureRate.mark();
//
//            if(null != message){
//              message.setStatus(MessageStatus.FAILED_TO_RELAY);
//              for(EmailID id: allReceivers)
//              AsyncHandlerUtil.sendInternalEvent(message, id.geteId());
//            }
//            Log.singleton(FILE.PROVIDER).error("Trans Email Not Sent. [Would be handled for retry, if applicable.] id:"+messageId+":"+e.getMessage());
//            throw new CInternalException(CExceptionCode.EMAIL_COULD_NOT_BE_SENT);
//          }
        }(ec).onComplete(responseTry ⇒ result.success(responseTry -> userContext))(ec)


        result.future
    }
  }

}
