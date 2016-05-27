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
package com.flipkart.connekt.busybees.encryption

import java.security._
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import javax.crypto.KeyAgreement

import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.{ECNamedCurveTable, ECPointUtil}

object EllipticCurveKeyUtils {

  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
  private val cryptoTypeEcdh = "ECDH"
  private val providerBouncyCastle = "BC"
  private val secp256r1 = "secp256r1"
  private val keyFactory = KeyFactory.getInstance(cryptoTypeEcdh, providerBouncyCastle)
  private val ecNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(secp256r1)
  private val ecNamedCurveSpec = new ECNamedCurveSpec(secp256r1, ecNamedCurveParameterSpec.getCurve, ecNamedCurveParameterSpec.getG, ecNamedCurveParameterSpec.getN)
  private val keyPairGenerator = KeyPairGenerator.getInstance(cryptoTypeEcdh, providerBouncyCastle)

  keyPairGenerator.initialize(ECNamedCurveTable.getParameterSpec(secp256r1), new SecureRandom())

  private def hexToBytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def generateServerKeyPair(): KeyPair = keyPairGenerator.generateKeyPair()

  def publicKeyToBytes(publicKey: ECPublicKey): Array[Byte] = {
    val x = publicKey.getW.getAffineX.toString(16)
    val y = publicKey.getW.getAffineY.toString(16)
    hexToBytes("04" + "0" * (64 - x.length) + x + "0" * (64 - y.length) + y)
  }

  def loadP256Dh(p256dh: String): ECPublicKey = {
    val point = ECPointUtil.decodePoint(ecNamedCurveSpec.getCurve, Base64.getUrlDecoder.decode(p256dh))
    val pubKeySpec = new ECPublicKeySpec(point, ecNamedCurveSpec)
    keyFactory.generatePublic(pubKeySpec).asInstanceOf[ECPublicKey]
  }

  def generateSharedSecret(serverKeys: KeyPair, clientPublicKey: PublicKey): Array[Byte] = {
    val keyAgreement = KeyAgreement.getInstance(cryptoTypeEcdh, providerBouncyCastle)
    keyAgreement.init(serverKeys.getPrivate)
    keyAgreement.doPhase(clientPublicKey, true)
    keyAgreement.generateSecret()
  }
}
