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
package com.flipkart.connekt.commons.tests.dal

import java.util.{Properties, UUID}

import com.flipkart.connekt.commons.connections.ConnectionProvider
import com.flipkart.connekt.commons.dao.HbaseDao
import com.flipkart.connekt.commons.factories.HTableFactoryWrapper
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.CharEncoding

class HbaseDaoTest extends ConnektUTSpec with HbaseDao {

  var hConnectionHelper = getHBaseConnHelper
  val tblName = "connekt-registry"

  override def afterAll() = {
    println("triggering cleanup afterAll")
    hConnectionHelper.shutdown()
    println("cleanup successful")
  }

  def getHBaseConnHelper = {

    ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-nm", "fk-connekt-receptors", "fk-connekt-busybees", "fk-connekt-busybees-akka"))
    val hConfProps = new Properties()
    hConfProps.setProperty("hbase.zookeeper.quorum", "fk-connekt-hbase-jn-firestorm-397125,fk-connekt-hbase-jn-firestorm-397126,fk-connekt-hbase-jn-firestorm-397127")
    hConfProps.setProperty("hbase.zookeeper.property.clientPort", "2181")
    hConfProps.setProperty("zookeeper.znode.parent", "/hbase-unsecure")

    val hConfig = ConfigFactory.parseProperties(hConfProps)

    new HTableFactoryWrapper(hConfig, new ConnectionProvider)
  }

  "A row put operation for single columnFamily" should "throw no IOException" in {

    implicit var h = hConnectionHelper.getTableInterface(tblName)
    try {
      val rowKey = UUID.randomUUID().toString
      val data = Map[String, Array[Byte]](
          "deviceId" -> "0b6dc5db9fd9f664438f4f9ea03e53d7".getBytes(CharEncoding.UTF_8),
          "make" -> "Motorola".getBytes(CharEncoding.UTF_8),
          "osVersion" -> "4.4.4".getBytes(CharEncoding.UTF_8),
          "app" -> "Retail".getBytes(CharEncoding.UTF_8),
          "platform" -> "Android".getBytes(CharEncoding.UTF_8),
          "deviceId" -> "0b6dc5db9fd9f664438f4f9ea03e53d7".getBytes(CharEncoding.UTF_8)
        )

      addRow(rowKey, Map[String, Map[String, Array[Byte]]]("p" -> data))

      println("inserted hbase table row: %s".format(data.toString()))
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      hConnectionHelper.releaseTableInterface(h)
    }
  }

/*
  var rowKey = UUID.randomUUID().toString
  "A row put operation for multiple columnFamilies" should "throw no IOException" in {
    implicit val h = hConnectionHelper.getTableInterface(tblName)

    try {
      val dataPrimary = Map[String, Array[Byte]](
        "deviceId" -> "0b6dc5db9fd9f664438f4f9ea03e53d7".getBytes(CharEncoding.UTF_8),
        "make" -> "Motorola".getBytes(CharEncoding.UTF_8),
        "osVersion" -> "4.4.4".getBytes(CharEncoding.UTF_8),
        "app" -> "Retail".getBytes(CharEncoding.UTF_8),
        "platform" -> "Android".getBytes(CharEncoding.UTF_8),
        "deviceId" -> "0b6dc5db9fd9f664438f4f9ea03e53d7".getBytes(CharEncoding.UTF_8)
      )

      val dataAuxiliary = Map[String, Array[Byte]](
        ("optIn", "true".getBytes(CharEncoding.UTF_8)),
        ("company", "fkint".getBytes(CharEncoding.UTF_8)),
        ("org", "mp".getBytes(CharEncoding.UTF_8)),
        ("namespace", "ceryx".getBytes(CharEncoding.UTF_8))
      )

      addRow(rowKey, Map[String, Map[String, Array[Byte]]]("p" -> dataPrimary, "a" -> dataAuxiliary))

      println("inserted hbase table row:\np: %s \na: %s ".format(dataPrimary.toString(), dataAuxiliary.toString()))
    } finally {
      hConnectionHelper.releaseTableInterface(h)
    }
  }

  "Get operation for a row" should "throw no IOException" in {
    implicit val h = hConnectionHelper.getTableInterface(tblName)

    try {
      val result = fetchRow(rowKey, List[String]("p", "a"))
      println("result for [%s] is :\n%s".format(rowKey, result.toString()))
    } finally {
      hConnectionHelper.releaseTableInterface(h)
    }
  }

  "Get multi operation for a row" should "throw no IOException" in {
    implicit val h = hConnectionHelper.getTableInterface(tblName)

    try {
      val result = fetchMultiRows(List(rowKey), List[String]("p", "a"))
      println("result for [%s] is :\n%s".format(rowKey, result(rowKey).toString()))
    } finally {
      hConnectionHelper.releaseTableInterface(h)
    }
  }*/

}
