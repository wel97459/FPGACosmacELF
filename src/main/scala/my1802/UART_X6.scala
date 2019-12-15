package mylib

import spinal.core._

class uart_rx6 extends BlackBox{
    val io = new Bundle {
        val clk = in Bool
        val buffer_reset = in Bool
        val serial_in = in Bool
        val en_16_x_baud = in Bool
        val buffer_read = in Bool

        val buffer_data_present = out Bool
        val buffer_half_full = out Bool
        val buffer_full = out Bool
        val data_out = out Bits(8 bits)
    }
    noIoPrefix()
    mapClockDomain(clock=io.clk)
}

class uart_tx6 extends BlackBox{
    val io = new Bundle {
        val clk = in Bool
        val buffer_reset = in Bool
        val data_in = in Bits(8 bits)
        val en_16_x_baud = in Bool
        val buffer_write = in Bool

        val serial_out= out Bool
        val buffer_data_present = out Bool
        val buffer_half_full = out Bool
        val buffer_full = out Bool
    }
    mapClockDomain(clock=io.clk)
}
