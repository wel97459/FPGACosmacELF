package Spinal1802

import spinal.core._
import spinal.lib._

class PLL_BB(Name: String) extends BlackBox {
    val io = new Bundle {
        val CLK_IN1     = in  Bool
        val RESET       = in Bool

        val CLK_OUT1    = out Bool
        val CLK_OUT2    = out Bool
        //val CLK_OUT3    = out Bool
        //val CLK_OUT4    = out Bool
        //val CLK_OUT5    = out Bool
        val LOCKED      = out Bool
    }
    noIoPrefix()
    setBlackBoxName(Name)
}
