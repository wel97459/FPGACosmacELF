package mylib

import spinal.core._

class Ram(AddrDepth: Int = 8, DataDepth: Int = 8) extends BlackBox {
    val io = new Bundle {
        val clka    = in  Bool
        val ena     = in  Bool
        val wea     = in  Bits(1 bit)
        val addra   = in Bits(AddrDepth bit)
        val douta   = out Bits(DataDepth bit)
        val dina    = in Bits(DataDepth bit)
    }
    noIoPrefix()
    mapClockDomain(clock=io.clka)
}
