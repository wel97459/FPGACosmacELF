package mylib

import spinal.core._

class fpga_rom(AddrDepth: Int = 0) extends BlackBox {
    val io = new Bundle {
        val clk     = in  Bool
        val addr    = in Bits(AddrDepth bit)
        val data    = out Bits(8 bit)
    }
    noIoPrefix()
    mapClockDomain(clock=io.clk)
}
