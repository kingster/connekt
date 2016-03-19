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

import java.util.concurrent.atomic.AtomicInteger

import scala.util.Random

object PasswordGenerator {

  private object CharacterTypes extends Enumeration {
    val LowerCase, UpperCase, Numbers, Symbols = Value
  }

  val LCaseChars = "abcdefgijkmnopqrstwxyz"
  val UCaseChars = "ABCDEFGHJKLMNPQRSTWXYZ"
  val NumericChars = "23456789"
  val SpecialChars = "*$-+_&=!%{}"

  def generate(minLength: Int,
                           maxLength: Int,
                           minLCaseCount: Int,
                           minUCaseCount: Int,
                           minNumCount: Int = 2,
                           minSpecialCount: Int = 0): String = {
    var randomString: Array[Char] = null
    val charGroupsUsed = scala.collection.mutable.Map[CharacterTypes.Value, AtomicInteger]()
    charGroupsUsed.put(CharacterTypes.LowerCase, new AtomicInteger(minLCaseCount))
    charGroupsUsed.put(CharacterTypes.UpperCase, new AtomicInteger(minUCaseCount))
    charGroupsUsed.put(CharacterTypes.Numbers, new AtomicInteger(minUCaseCount))
    charGroupsUsed.put(CharacterTypes.Symbols, new AtomicInteger(minSpecialCount))

    val randomBytes = Array.ofDim[Byte](4)
    new Random().nextBytes(randomBytes)
    val seed = (randomBytes(0) & 0x7f) << 24 | randomBytes(1) << 16 | randomBytes(2) << 8 | randomBytes(3)
    val random = new Random(seed)
    var randomIndex = -1

    if (minLength < maxLength) {
      randomIndex = random.nextInt((maxLength - minLength) + 1) + minLength
      randomString = Array.ofDim[Char](randomIndex)
    } else {
      randomString = Array.ofDim[Char](minLength)
    }

    var requiredCharactersLeft = minLCaseCount + minUCaseCount + minNumCount + minSpecialCount
    for (i <- randomString.indices) {
      var selectableChars = ""
      if (requiredCharactersLeft < randomString.length - i) {
        selectableChars = LCaseChars + UCaseChars + NumericChars + SpecialChars
      } else {
        for ((key, value) <- charGroupsUsed if value.get() > 0) {
          if (CharacterTypes.LowerCase == key) {
            selectableChars += LCaseChars
          } else if (CharacterTypes.UpperCase == key) {
            selectableChars += UCaseChars
          } else if (CharacterTypes.Numbers == key) {
            selectableChars += NumericChars
          } else if (CharacterTypes.Symbols == key) {
            selectableChars += SpecialChars
          }
        }
      }
      randomIndex = random.nextInt(selectableChars.length - 1)
      val nextChar = selectableChars.charAt(randomIndex)
      randomString(i) = nextChar
      if (LCaseChars.indexOf(nextChar) > -1) {
        if (charGroupsUsed(CharacterTypes.LowerCase).decrementAndGet() >= 0) {
          requiredCharactersLeft -= 1
        }
      } else if (UCaseChars.indexOf(nextChar) > -1) {
        if (charGroupsUsed(CharacterTypes.UpperCase).decrementAndGet() >= 0) {
          requiredCharactersLeft -= 1
        }
      } else if (NumericChars.indexOf(nextChar) > -1) {
        if (charGroupsUsed(CharacterTypes.Numbers).decrementAndGet() >= 0) {
          requiredCharactersLeft -= 1
        }
      } else if (SpecialChars.indexOf(nextChar) > -1) {
        if (charGroupsUsed(CharacterTypes.Symbols).decrementAndGet() >= 0) {
          requiredCharactersLeft -= 1
        }
      }
    }
    new String(randomString)
  }

}
