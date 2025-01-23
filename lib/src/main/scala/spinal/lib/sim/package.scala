/*                                                                           *\
**        _____ ____  _____   _____    __                                    **
**       / ___// __ \/  _/ | / /   |  / /   HDL Core                         **
**       \__ \/ /_/ // //  |/ / /| | / /    (c) Dolu, All rights reserved    **
**      ___/ / ____// // /|  / ___ |/ /___                                   **
**     /____/_/   /___/_/ |_/_/  |_/_____/                                   **
**                                                                           **
**      This library is free software; you can redistribute it and/or        **
**    modify it under the terms of the GNU Lesser General Public             **
**    License as published by the Free Software Foundation; either           **
**    version 3.0 of the License, or (at your option) any later version.     **
**                                                                           **
**      This library is distributed in the hope that it will be useful,      **
**    but WITHOUT ANY WARRANTY; without even the implied warranty of         **
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU      **
**    Lesser General Public License for more details.                        **
**                                                                           **
**      You should have received a copy of the GNU Lesser General Public     **
**    License along with this library.                                       **
\*                                                                           */
package spinal.lib

import spinal.core.log2Up
import spinal.core.sim._
import spinal.lib.experimental.math.Floating
import spinal.lib.experimental.math.IEEE754Converter

package object sim {

  def pow2(exp: Int) = BigInt(1) << exp

  def nextPow2(n: Int): BigInt = BigInt(1) << log2Up(n)

  def logR(n: Int, radix: Int) = {
    var current = n
    var ret = 0
    while (current > 1) {
      require(current % radix == 0)
      current /= radix
      ret += 1
    }
    ret
  }

  def powR(radix: Int, exp: Int) = Seq.fill(exp)(radix).product

  // TODO: verification for float pimper
  implicit class FloatingPointPimper(float: Floating) {

    private def fromBitString(bitString: String): Unit = {
      float.sign #= bitString.head == '1'
      float.exponent #= BigInt(bitString.slice(1, float.exponentSize + 1), 2)
      float.mantissa #= BigInt(bitString.takeRight(float.mantissaSize), 2)
    }

    def #=(value: Float): Unit = fromBitString(IEEE754Converter.floatToBinary(value))

    def #=(value: Double): Unit = fromBitString(IEEE754Converter.doubleToBinary(value))

    private def getBitString =
      ((float.sign.toBigInt << 31) + (float.exponent.toBigInt << 23) + float.mantissa.toBigInt).toString(2)

    def toFloat: Float = IEEE754Converter.binaryToFloat(getBitString)

    def toDouble: Double = IEEE754Converter.binaryToDouble(getBitString)

    // only normal
    def randNormal(signed: Boolean): Unit = {
      float.exponentSize match {
        case 8 =>
          float.sign #= (if (signed) scala.util.Random.nextBoolean() else false)
          float.exponent #= scala.util.Random.nextInt((pow2(8) - 2).toInt) + 1
          float.mantissa #= scala.util.Random.nextInt(pow2(23).toInt)
        case 11 =>
          float.sign #= (if (signed) scala.util.Random.nextBoolean() else false)
          float.exponent #= scala.util.Random.nextInt((pow2(11) - 2).toInt) + 1
          float.mantissa #= BigInt(52, scala.util.Random)
      }
    }

    def randDenormal() = {
      float.exponentSize match {
        case 8 =>
          float.sign #= scala.util.Random.nextBoolean()
          float.exponent #= 0
          float.mantissa #= (BigInt(23, util.Random) + 1) & (pow2(23) - 1)
        case 11 =>
          float.sign #= scala.util.Random.nextBoolean()
          float.exponent #= 0
          float.mantissa #= (BigInt(52, util.Random) + 1) & (pow2(23) - 1)
      }
    }

    def isInf: Boolean = {
      float.exponentSize match {
        case 8  => (float.exponent.toBigInt == pow2(8) - 1) && (float.mantissa.toBigInt == 0)
        case 11 => (float.exponent.toBigInt == pow2(11) - 1) && (float.mantissa.toBigInt == 0)
      }
    }

    def isNaN: Boolean = {
      float.exponentSize match {
        case 8  => (float.exponent.toBigInt == pow2(8) - 1) && (float.mantissa.toBigInt != 0)
        case 11 => (float.exponent.toBigInt == pow2(11) - 1) && (float.mantissa.toBigInt != 0)
      }
    }
  }

}
