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
package com.flipkart.connekt.commons.utils

import java.util.{Hashtable => JHashTable}
import javax.naming._
import javax.naming.directory._
import javax.naming.ldap.InitialLdapContext

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


object LdapService {

  val searchbase = "dc=flipkart,dc=com"
  val url = "ldaps://ldap.corp.flipkart.com:636"
  val usersub = "ou=%s,dc=flipkart,dc=com"
  val groupsub = "flipkart"
  val groupattrib = "com"

  protected def env = Map(
    Context.INITIAL_CONTEXT_FACTORY -> "com.sun.jndi.ldap.LdapCtxFactory",
    Context.PROVIDER_URL -> url,
    Context.SECURITY_AUTHENTICATION -> "simple")

  protected def getPrincipal(username: String, ou: String = "People"): String = {
    "uid=%s,%s".format(username, usersub.format(ou))
  }

  def getUserGroup(username: String): Option[String] = {
    val ldapContext = new InitialLdapContext(new JHashTable[String, String](env.asJava), null)
    val searchControls = new SearchControls()
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE)

    val results = ldapContext.search(searchbase, "uid=" + username, searchControls)
    val it = for (
      result <- results;
      parts = result.getName.split(',');
      pairs <- parts;
      kv = pairs.split('=')
      if kv.head.equalsIgnoreCase("ou");
      ou = kv.last
    ) yield ou
    it.toSeq.headOption
  }

  def authenticate(username: String, password: String): Boolean = {

    var initialDirContext: InitialDirContext = null
    try {
      val groupName = getUserGroup(username).getOrElse("People")
      val userEnv = Map(
        Context.SECURITY_PRINCIPAL -> getPrincipal(username, groupName),
        Context.SECURITY_CREDENTIALS -> password) ++ env

      initialDirContext = new InitialDirContext(new JHashTable[String, String](userEnv.asJava))
      val uid = getUid(getPrincipal(username, groupName), initialDirContext)
      require(uid > 0, "Unable to find UID for user")
      true
    } catch {
      case e: AuthenticationException =>
        ConnektLogger(LogFile.SERVICE).info(s"Fail authentication for user $username")
        false
      case e: Throwable =>
        ConnektLogger(LogFile.SERVICE).info(s"Failed authentication info: ${e.getMessage}", e)
        false
    } finally {
      if (initialDirContext != null) initialDirContext.close
    }
  }

  protected def getUid(search: String, ctx: InitialDirContext): Int = {
    val searchControls = new SearchControls
    searchControls.setSearchScope(SearchControls.OBJECT_SCOPE)
    val attribs = ctx.getAttributes(search)
    attribs.get("uidNumber") match {
      case null => -1
      case attrib => attrib.get.asInstanceOf[String].toInt
    }
  }

}
