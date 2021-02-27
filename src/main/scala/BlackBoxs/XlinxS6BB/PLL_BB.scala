package XlinxS6BB

import spinal.core._
import spinal.lib._

class PLL_BB(Name: String, NumberOfOutputs: Int = 1) extends BlackBox {
    val io = new Bundle {
        val CLK_IN1     = in Bool
        val RESET       = in Bool

        val CLK_OUT1    = out Bool
        val CLK_OUT2    = ifGen(NumberOfOutputs > 1)(out Bool)
        val CLK_OUT3    = ifGen(NumberOfOutputs > 2)(out Bool)
        val CLK_OUT4    = ifGen(NumberOfOutputs > 3)(out Bool)
        val CLK_OUT5    = ifGen(NumberOfOutputs > 4)(out Bool)
        val CLK_OUT6    = ifGen(NumberOfOutputs > 5)(out Bool)

        val LOCKED      = out Bool
    }
    noIoPrefix()
    setBlackBoxName(Name)
}

//for sim
class PLL extends Component {
    val io = new Bundle {
        val CLK_IN1 = in Bool
        val RESET = in Bool

        val CLK_OUT1    = out Bool
        val LOCKED      = out Bool
    }
    noIoPrefix()

    io.CLK_OUT1 := io.CLK_IN1
    io.LOCKED := True

}

object PLLConfig extends SpinalConfig(
    targetDirectory = ".",
    oneFilePerComponent = true
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object PLLGen {
    def main(args: Array[String]) {
        PLLConfig.generateVerilog(new PLL).printPruned
    }
}