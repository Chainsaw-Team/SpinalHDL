package spinal.lib.experimental.math

object IEEE754Converter {

  /** 将 Float 转换为 IEEE 754 二进制字符串 */
  def floatToBinary(value: Float): String = {
    val intBits = java.lang.Float.floatToIntBits(value)
    String.format("%32s", Integer.toBinaryString(intBits)).replace(' ', '0')
  }

  /** 将 Double 转换为 IEEE 754 二进制字符串 */
  def doubleToBinary(value: Double): String = {
    val longBits = java.lang.Double.doubleToLongBits(value)
    String.format("%64s", java.lang.Long.toBinaryString(longBits)).replace(' ', '0')
  }

  def binaryToFloat(binary: String): Float = {
    assert(binary.length <= 32)
    val binary32 = binary.reverse.padTo(32, '0').reverse
    val intBits = Integer.parseUnsignedInt(binary32, 2)
    java.lang.Float.intBitsToFloat(intBits)
  }

  def binaryToDouble(binary: String): Double = {
    assert(binary.length <= 64)
    val binary64 = binary.reverse.padTo(64, '0').reverse
    val longBits = java.lang.Long.parseUnsignedLong(binary64, 2)
    java.lang.Double.longBitsToDouble(longBits)
  }
}


object IEEE754ConverterTest {
  import IEEE754Converter._

  /** 近似比较 (用于比较浮点值时的误差容忍) */
  def nearlyEqual(a: Double, b: Double, epsilon: Double = 1e-7): Boolean =
    math.abs(a - b) <= epsilon

  /** 测试单精度 (Float) */
  def testFloat(): Unit = {
    println("=== 测试 Float ===")
    val testCases: Seq[Float] = Seq(
      0.0f, // 正零
      -0.0f, // 负零
      Float.PositiveInfinity, // 正无穷大
      Float.NegativeInfinity, // 负无穷大
      Float.NaN, // 非数值 (NaN)
      Float.MinValue, // 最小规范化数 (-3.4028235E38)
      Float.MaxValue, // 最大规范化数 (3.4028235E38)
      Float.MinPositiveValue, // 最小正的次正规化数
      -Float.MinPositiveValue, // 最小负的次正规化数
      1.0f,
      -1.0f, // 常见值
      3.1415927f, // π 的近似值
      -2.7182818f // e 的近似值
    )

    testCases.foreach { testValue =>
      // 测试 float -> 二进制 -> float
      val binary = floatToBinary(testValue)
      val recovered = binaryToFloat(binary)

      val isValid =
        if (testValue.isNaN)
          recovered.isNaN // NaN 特殊判断
        else
          testValue == recovered // 精确匹配常规值

      println(s"测试值: $testValue -> 二进制: $binary -> 还原: $recovered, 验证: ${if (isValid) "通过" else "失败"}")
      assert(isValid, s"测试失败: 输入值: $testValue, 还原值: $recovered")
    }
  }

  /** 测试双精度 (Double) */
  def testDouble(): Unit = {
    println("\n=== 测试 Double ===")
    val testCases: Seq[Double] = Seq(
      0.0, // 正零
      -0.0, // 负零
      Double.PositiveInfinity, // 正无穷大
      Double.NegativeInfinity, // 负无穷大
      Double.NaN, // 非数值 (NaN)
      Double.MinValue, // 最小规范化数 (-1.7976931348623157E308)
      Double.MaxValue, // 最大规范化数 (1.7976931348623157E308)
      Double.MinPositiveValue, // 最小正的次正规化数
      -Double.MinPositiveValue, // 最小负的次正规化数
      1.0,
      -1.0, // 常见值
      3.141592653589793, // π 的近似值
      -2.718281828459045 // e 的近似值
    )

    testCases.foreach { testValue =>
      // 测试 double -> 二进制 -> double
      val binary = doubleToBinary(testValue)
      val recovered = binaryToDouble(binary)

      val isValid =
        if (testValue.isNaN)
          recovered.isNaN // NaN 特殊判断
        else if (testValue.isInfinity)
          recovered.isInfinity && testValue.signum == recovered.signum // 检查符号
        else
          nearlyEqual(testValue, recovered) // 允许微小精度误差

      println(s"测试值: $testValue -> 二进制: $binary -> 还原: $recovered, 验证: ${if (isValid) "通过" else "失败"}")
      assert(isValid, s"测试失败: 输入值: $testValue, 还原值: $recovered")
    }
  }

  def main(args: Array[String]): Unit = {
    testFloat() // 测试单精度
    testDouble() // 测试双精度
  }
}