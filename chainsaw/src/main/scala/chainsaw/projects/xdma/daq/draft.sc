val bitValue = java.lang.Float.floatToIntBits(-1.5f)
println(bitValue)
println(bitValue.toBinaryString)
println(bitValue >> 31)

//float.sign     #= (bitValue >> 31) == 1
//float.exponent #= (bitValue >> 23) & 0xff
//float.mantissa #= bitValue & 0x7fffff