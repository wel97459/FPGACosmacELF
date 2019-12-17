/*
 * SpinalHDL
 * Copyright (c) Dolu, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package mylib

import spinal.core._
import spinal.lib._

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
        val pll = new PLL_BB(1)
        pll.io.RESET := !io.reset_n
        pll.io.CLK_IN1 := io.clk50Mhz
//        val clk10Domain = ClockDomain.internal(name = "core10",  frequency = FixedFrequency(10 MHz))
        val clk8Domain = ClockDomain.internal(name = "core8",  frequency = FixedFrequency(8 MHz))

//        clk10Domain.clock := pll.io.CLK_OUT1
//        clk10Domain.reset := ResetCtrl.asyncAssertSyncDeassert(
//            input = !io.reset_n || !pll.io.LOCKED,
//            clockDomain = clk10Domain
//        )

        clk8Domain.clock := pll.io.CLK_OUT1
        clk8Domain.reset := ResetCtrl.asyncAssertSyncDeassert(
            input = !io.reset_n || !pll.io.LOCKED,
            clockDomain = clk8Domain
        )


    }

//    val core10 = new ClockingArea(clkCtrl.clk10Domain) {
//
//    }

    val core8 = new ClockingArea(clkCtrl.clk8Domain) {

        val ram4096 = new Ram(12, 8)
        ram4096.io.ena := True

        val debounce = Debounce(12, 50 ms)
        debounce.write(~io.switches)

        val dlatch = Reg(Bool) init(False)
        val alatch = Reg(Bool) init(False)
        val segdis1 = new SevenSegment()
        io.segdis := segdis1.io.SegDis

        val step = Reg(Bool) init(False)
        val stepDMAIn = Reg(Bool) init(False)
        //val stepDMAOut = Reg(Bool) init(False)

        var memTestData = Reg(Bits(8 bit)) init(0)


        val cpu = new cpu1802()
        when(debounce(10)){
            cpu.io.Wait_n := True
        } otherwise(cpu.io.Wait_n := step)

        cpu.io.Clear_n := debounce(11)
        cpu.io.DMA_In_n := !stepDMAIn
        cpu.io.DMA_Out_n := True
        cpu.io.Interrupt_n := True
        cpu.io.EF_n := 15

        val serialDataOut = Bits(8 bit)
        val DMADataIN = Reg(Bits(8 bit)) init(0)
        val serialDataPresent = Bool
        val UartRx = new uart_rx6()
        UartRx.io.en_16_x_baud := True
        serialDataOut := UartRx.io.data_out
        serialDataPresent := UartRx.io.buffer_data_present
        UartRx.io.buffer_reset := clkCtrl.clk8Domain.reset
        UartRx.io.serial_in := io.avr_tx

        val serialDataSend = Bool
        val DMADataOut = Reg(Bits(8 bit)) init(0)
        val UartTx = new uart_tx6()
        UartTx.io.en_16_x_baud := True
        UartTx.io.data_in := cpu.io.DataOut
        UartTx.io.buffer_write := serialDataSend
        UartTx.io.buffer_reset := clkCtrl.clk8Domain.reset
        io.avr_rx := UartTx.io.serial_out
        serialDataSend := (!cpu.io.MRD && cpu.io.N === 1).fall()

        when(cpu.io.SC === 2 && !debounce(8)) {
            cpu.io.DataIn := DMADataIN
        }elsewhen(cpu.io.SC === 2) {
            cpu.io.DataIn := ram4096.io.douta
        } otherwise(cpu.io.DataIn := ram4096.io.douta)

        ram4096.io.wea := ~(cpu.io.MWR & !debounce(8)).asBits
        ram4096.io.dina := cpu.io.DataOut

        when(!cpu.io.MRD || !cpu.io.MWR){
            dlatch := True
            alatch := True
        } otherwise{
            dlatch := False
            alatch := False
        }

        segdis1.io.L1 := dlatch
        segdis1.io.L2 := alatch
        segdis1.io.Dis1 := cpu.io.DataOut
        segdis1.io.Dis2 := cpu.io.Add

        when(!cpu.io.MRD && cpu.io.N === 2){
            memTestData := cpu.io.DataOut
        }


        when(debounce.edge()(9) && debounce(11)){
            step := True
        }elsewhen(cpu.io.SC(0).edge()){
            step := False
        }

        when(debounce.edge()(9) & !debounce(11)) {
            stepDMAIn := True
            DMADataIN := debounce(7 downto 0)
            UartRx.io.buffer_read := False
        }elsewhen(serialDataPresent.edge() & !debounce(11) ){
            DMADataIN := serialDataOut
            stepDMAIn := True
            UartRx.io.buffer_read := True
        }elsewhen(cpu.io.SC === 2){
            stepDMAIn := False
            UartRx.io.buffer_read := False
        }otherwise{
            UartRx.io.buffer_read := False
        }

        ram4096.io.addra := cpu.io.Add.resize(widthOf(ram4096.io.addra))

        io.LEDs := memTestData
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
        TopSpinalConfig.generateVerilog(new TopLevel)
    }
}