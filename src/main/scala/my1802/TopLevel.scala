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

import scala.util.Random

//Hardware definition
class TopLevel extends Component {
    val io = new Bundle {
        val clk50Mhz = in Bool
        val reset_n = in Bool
        val switches = in Bits(10 bit)
        val LEDs = out Bits(8 bits)

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

        clk8Domain.clock := pll.io.CLK_OUT2
        clk8Domain.reset := ResetCtrl.asyncAssertSyncDeassert(
            input = !io.reset_n || !pll.io.LOCKED,
            clockDomain = clk8Domain
        )


    }

//    val core10 = new ClockingArea(clkCtrl.clk10Domain) {
//
//    }

    val core8 = new ClockingArea(clkCtrl.clk8Domain) {
//        val debounce = Debounce(10, 10 ms)
//        debounce.write(~io.switches)
//
//        when(debounce.edge()(9)){ //Raising edge
//            address := debounce(7 downto 0)
//        }

        val address = Reg(Bits(8 bit))
        val rom = new fpga_rom(5)

        val cpu = new cpu1802()
        cpu.io.Wait_n := True
        cpu.io.Clear_n := True
        cpu.io.DMA_In_n := True
        cpu.io.DMA_Out_n := True
        cpu.io.Interrupt_n := True
        cpu.io.EF_n := 15
        when(!cpu.io.MRD) {
            cpu.io.DataIn := rom.io.data
            address := cpu.io.Add
        }otherwise(cpu.io.DataIn := 0)

        rom.io.addr := cpu.io.Add.resize(widthOf(rom.io.addr))

        io.LEDs := address
        io.avr_rx := False
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