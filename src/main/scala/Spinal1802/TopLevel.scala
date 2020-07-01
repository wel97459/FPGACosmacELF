package Spinal1802

import spinal.core._
import spinal.lib._
import spinal.lib.fsm.{EntryPoint, State, StateMachine}

//Hardware definition
class TopLevel extends Component {
    val io = new Bundle {
        val clk50Mhz = in Bool
        val reset_n = in Bool
        val switches = in Bits(12 bit)
        val LEDs = out Bits(8 bits)
        val segdis = out Bits(11 bits)

        val avr_tx = in Bool
        val avr_rx = out Bool
    }
    noIoPrefix()

    val clkCtrl = new Area {
        val pll = new PLL_BB("PLL")
        pll.io.RESET := !io.reset_n
        pll.io.CLK_IN1 := io.clk50Mhz

        //using 8mhz so that UART running 115200 BAUD
        val clk8Domain = ClockDomain.internal(name = "core8",  frequency = FixedFrequency(8 MHz))

        clk8Domain.clock := pll.io.CLK_OUT1
        clk8Domain.reset := ResetCtrl.asyncAssertSyncDeassert(
            input = !io.reset_n || !pll.io.LOCKED,
            clockDomain = clk8Domain
        )
    }

    val core8 = new ClockingArea(clkCtrl.clk8Domain) {

        //Setup CPU
        val Cpu = new CDP1802()
        val step = Reg(Bool) init(False)
        val stepDMAIn = Reg(Bool) init(False)
        val DMADataIN = Reg(Bits(8 bit)) init(0)
        val interrupt = Reg(Bool) init(False)
        val interruptLast = Reg(Bool) init(False)
        Cpu.io.DMA_In_n := !stepDMAIn
        Cpu.io.DMA_Out_n := True
        Cpu.io.Interrupt_n := !(interrupt && !interruptLast)
        Cpu.io.EF_n(2) := True

        //Setup switch debounce
        val debounce = Debounce(12, 50 ms)
        debounce.write(~io.switches)


        //set up Ram
        val ram4096 = new Ram("Ram",12, 8)
        ram4096.io.ena := True
        ram4096.io.wea := ~(Cpu.io.MWR & !debounce(8)).asBits
        ram4096.io.dina := Cpu.io.DataOut
        ram4096.io.addra := Cpu.io.Addr16(11 downto 0)


        //Setup seven segment display
        val SevenSegment = SevenSegmentDriver(3, 100 us)
        io.segdis := Cat(SevenSegment(9 downto 8), SevenSegment(6 downto 0))
        when(!(Cpu.io.MRD && Cpu.io.MWR) && Cpu.io.TPB){
            SevenSegment.setDigits(0, Cpu.io.DataOut.asUInt)
        }
        SevenSegment.setDigits(2, Cpu.io.Addr16(7 downto 0).asUInt)


        //Setup RX UART
        val serialDataRX = Bits(8 bit)
        val serialDataPresent = Bool
        val buffer_read = Reg(Bool) init(False)
        val buffer_read_last = RegNext(buffer_read)

        val UartRx = new uart_rx6("uart_rx6")
        UartRx.io.en_16_x_baud := True
        serialDataRX := UartRx.io.data_out
        serialDataPresent := UartRx.io.buffer_data_present
        Cpu.io.EF_n(1) := UartRx.io.buffer_data_present
        UartRx.io.buffer_reset := clkCtrl.clk8Domain.reset | debounce.falling()(11)
        UartRx.io.serial_in := io.avr_tx
        UartRx.io.buffer_read := buffer_read & !buffer_read_last

        //Setup TX UART
        val serialDataSend = Bool
        val DMADataOut = Reg(Bits(8 bit)) init(0)
        val UartTx = new uart_tx6("uart_tx6")
        UartTx.io.en_16_x_baud := True
        UartTx.io.data_in := Cpu.io.DataOut
        UartTx.io.buffer_write := serialDataSend
        Cpu.io.EF_n(0) := UartTx.io.buffer_full
        UartTx.io.buffer_reset := clkCtrl.clk8Domain.reset
        io.avr_rx := UartTx.io.serial_out
        serialDataSend := (!Cpu.io.MRD && Cpu.io.N === 1).fall()

        //Handel Ram read and write logic
        when(Cpu.io.SC === 2 && !debounce(8)) {
            Cpu.io.DataIn := DMADataIN
        }elsewhen(Cpu.io.SC === 2 && Cpu.io.N === 0) {
            Cpu.io.DataIn := ram4096.io.douta
        }elsewhen(!Cpu.io.MWR && Cpu.io.N === 2){
            Cpu.io.DataIn := debounce(7 downto 0)
        }elsewhen(!Cpu.io.MWR && Cpu.io.N === 1){
            Cpu.io.DataIn := serialDataRX
        } otherwise(Cpu.io.DataIn := ram4096.io.douta)


        //Output the upper byte of the Address to the LEDs
        io.LEDs := Cat(UartRx.io.buffer_data_present, buffer_read & !buffer_read_last, Cpu.io.Addr16(13 downto 8))


        //Switch logic of the Cosmac Elf

        //Control logic for the Wait and Clear lines
        when(debounce(10)){
            Cpu.io.Wait_n := True
        } otherwise(Cpu.io.Wait_n := step)

        Cpu.io.Clear_n := debounce(11)

        //So we can read the write button.
        Cpu.io.EF_n(3) := debounce(9)

        //Single step operation
        when(debounce.rising()(9) && debounce(11)){
            step := True
        }elsewhen(Cpu.io.SC(0).edge()){
            step := False
        }

        //DMA IN logic for loading switch data to memory
        when(debounce.rising()(9) & !debounce(11)) {
            stepDMAIn := True
            DMADataIN := debounce(7 downto 0)
            buffer_read := False
        }elsewhen(serialDataPresent.rise() & !debounce(11) ){
            //DMA IN logic for loading serial data to memory
            DMADataIN := serialDataRX
            stepDMAIn := True
            buffer_read := True
        }elsewhen(!Cpu.io.MWR && Cpu.io.N === 1 && Cpu.io.TPB){
            buffer_read := True
        }elsewhen(Cpu.io.SC === 2) {
            //Reset DMA IN line and the serial read line
            stepDMAIn := False
            buffer_read:= False
        }otherwise {
            //reset serial read line
            buffer_read := False
        }

        //Interrupts
        when(debounce(11) && debounce(10) && debounce.rising()(9)) {
            interrupt := True
        }elsewhen(debounce(11) && serialDataPresent) {
            interrupt := True
        }elsewhen(Cpu.io.SC === 3) {
            interrupt := False
        }
    }
}

//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object TopSpinalConfig extends SpinalConfig(
    targetDirectory = "..",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
    defaultClockDomainFrequency = FixedFrequency(50 MHz)
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object TopLevelGen {
    def main(args: Array[String]) {
        TopSpinalConfig.generateVerilog(new TopLevel).printPruned
    }
}