package mylib

import spinal.core._
import spinal.lib._

class Testing extends Component {
    val io = new Bundle  {
        val switches = in Bits(12 bit)
        val LEDs = out Bits(8 bits)
    }
    noIoPrefix()

    val address = Reg(Bits(8 bit)) init(0)
    val rom = new fpga_rom(5)

    val cpu = new cpu1802()
    cpu.io.Wait_n := True
    cpu.io.Clear_n := True
    cpu.io.DMA_In_n := True
    cpu.io.DMA_Out_n := True
    cpu.io.Interrupt_n := True
    cpu.io.EF_n := 0xf
    when(!cpu.io.MRD) {
        cpu.io.DataIn := rom.io.data
        address := cpu.io.Add
    }otherwise(cpu.io.DataIn := 0)

    rom.io.addr := cpu.io.Add.resize(widthOf(rom.io.addr))

    io.LEDs := address
}


//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object TestSpinalConfig extends SpinalConfig(
    targetDirectory = "..",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
    defaultClockDomainFrequency = FixedFrequency(50 MHz)
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object TestingGen {
    def main(args: Array[String]) {
        TestSpinalConfig.generateVerilog(new Testing)
    }
}