package Spinal1802

import spinal.core._

class SevenSegment(Name: String) extends BlackBox {
    val io = new Bundle {
        val clk     = in  Bool
        val reset   = in  Bool
        val L1      = in  Bool
        val Dis1    = in Bits(8 bit)
        val L2      = in  Bool
        val Dis2    = in Bits(8 bit)

        val SegDis  = out Bits(11 bit)
    }
    noIoPrefix()
    mapClockDomain(clock=io.clk, reset = io.reset)
    setBlackBoxName(Name)
}