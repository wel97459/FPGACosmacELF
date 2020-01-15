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

        //set up Ram
        val ram4096 = new Ram("Ram",12, 8)
        ram4096.io.ena := True

        //Setup switch debounce
        val debounce = Debounce(12, 50 ms)
        debounce.write(~io.switches)

        //Setup seven segment display
        val dlatch = Reg(Bool) init(False)
        val alatch = Reg(Bool) init(False)
        val SevenSegmentDriver = new SevenSegment("SevenSegment")
        io.segdis := SevenSegmentDriver.io.SegDis


        //Setup CPU
        val cpu = new cpu1802()
        val step = Reg(Bool) init(False)
        val stepDMAIn = Reg(Bool) init(False)
        cpu.io.DMA_In_n := !stepDMAIn
        cpu.io.DMA_Out_n := True
        cpu.io.Interrupt_n := True
        cpu.io.EF_n(3 downto 1) := 7

        //Setup RX UART
        val serialDataOut = Bits(8 bit)
        val DMADataIN = Reg(Bits(8 bit)) init(0)
        val serialDataPresent = Bool
        val UartRx = new uart_rx6("uart_rx6")
        UartRx.io.en_16_x_baud := True
        serialDataOut := UartRx.io.data_out
        serialDataPresent := UartRx.io.buffer_data_present
        UartRx.io.buffer_reset := clkCtrl.clk8Domain.reset
        UartRx.io.serial_in := io.avr_tx

        //Setup TX UART
        val serialDataSend = Bool
        val DMADataOut = Reg(Bits(8 bit)) init(0)
        val UartTx = new uart_tx6("uart_rx6")
        UartTx.io.en_16_x_baud := True
        UartTx.io.data_in := cpu.io.DataOut
        UartTx.io.buffer_write := serialDataSend
        cpu.io.EF_n(0) := UartTx.io.buffer_full
        UartTx.io.buffer_reset := clkCtrl.clk8Domain.reset
        io.avr_rx := UartTx.io.serial_out
        serialDataSend := (!cpu.io.MRD && cpu.io.N === 1).fall()

        //Handel Ram read and write logic
        when(cpu.io.SC === 2 && !debounce(8)) {
            cpu.io.DataIn := DMADataIN
        }elsewhen(cpu.io.SC === 2) {
            cpu.io.DataIn := ram4096.io.douta
        } otherwise(cpu.io.DataIn := ram4096.io.douta)

        ram4096.io.wea := ~(cpu.io.MWR & !debounce(8)).asBits
        ram4096.io.dina := cpu.io.DataOut
        ram4096.io.addra := cpu.io.Addr16(11 downto 0)

        //Handel Seven Segment Display
        SevenSegmentDriver.io.L1 := !(cpu.io.MRD && cpu.io.MWR) && cpu.io.TPB
        SevenSegmentDriver.io.L2 := !(cpu.io.MRD && cpu.io.MWR) && cpu.io.TPB
        SevenSegmentDriver.io.Dis1 := cpu.io.DataOut
        SevenSegmentDriver.io.Dis2 := cpu.io.Addr16(7 downto 0)

        //Output the upper byte of the Address to the LEDs
        io.LEDs := cpu.io.Addr16(15 downto 8)


        //Switch logic of the Cosmac Elf

        //Control logic for the Wait and Clear lines
        when(debounce(10)){
            cpu.io.Wait_n := True
        } otherwise(cpu.io.Wait_n := step)

        cpu.io.Clear_n := debounce(11)

        //Single step operation
        when(debounce.edge()(9) && debounce(11)){
            step := True
        }elsewhen(cpu.io.SC(0).edge()){
            step := False
        }

        //DMA IN logic for loading switch data to memory
        when(debounce.edge()(9) & !debounce(11)) {
            stepDMAIn := True
            DMADataIN := debounce(7 downto 0)
            UartRx.io.buffer_read := False
        }elsewhen(serialDataPresent.edge() & !debounce(11) ){
            //DMA IN logic for loading serial data to memory
            DMADataIN := serialDataOut
            stepDMAIn := True
            UartRx.io.buffer_read := True
        }elsewhen(cpu.io.SC === 2){
            //Reset DMA IN line and the serial read line
            stepDMAIn := False
            UartRx.io.buffer_read := False
        }otherwise {
            //reset serial read line
            UartRx.io.buffer_read := False
        }
    }
}

//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object TopSpinalConfig extends SpinalConfig(
    targetDirectory = ".",
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