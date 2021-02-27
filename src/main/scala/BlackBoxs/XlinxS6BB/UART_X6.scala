package XlinxS6BB

import spinal.core._
import spinal.lib._

class uart_rx6(Name: String) extends BlackBox{
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
    setBlackBoxName(Name)
}

class uart_tx6(Name: String) extends BlackBox{
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
    noIoPrefix()
    noIoPrefix()
    mapClockDomain(clock=io.clk)
}


class uart_rx6sim(Name: String) extends Component{
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

    io.data_out := 0
    io.buffer_data_present := False
    io.buffer_half_full := False
    io.buffer_full := False
}

class uart_tx6sim(Name: String) extends Component{
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
    noIoPrefix()

    io.serial_out := False
    io.buffer_data_present := False
    io.buffer_half_full := False
    io.buffer_full := False
}


object  BaudRateGen {
  def apply(buad: BigDecimal, frequency: BigDecimal) : Bool = {
    val counter = Counter((frequency / (buad * 16)).toBigInt)
    val pulse = counter === 0
    counter.willIncrement.removeAssignments()
    counter.increment()
    pulse
  }
  
  def apply(buad: BigDecimal) : Bool = {
    val counter = Counter(((ClockDomain.current.frequency.getValue).toBigDecimal / (buad * 16)).toBigInt)
    val pulse = counter === 0
    counter.willIncrement.removeAssignments()
    counter.increment()
    pulse
  }
}