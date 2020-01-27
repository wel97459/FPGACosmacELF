package Spinal1802

import spinal.core._
import spinal.lib.{Counter, Timeout}

import spinal.core.sim._
import scala.util.control.Breaks

/*
    Segments to bits:
    Bit 0 - a         A
    Bit 1 - b       ____
    Bit 2 - c     F|    |B
    Bit 3 - d       -G--
    Bit 4 - e     E|    |C
    Bit 5 - f       ----
    Bit 6 - g         D  *DP
    Bit 7 - dp
 */

object SevenSegmentDriver {
    def apply(NumberOfDisplays: BigInt, CycleSpeed: BigInt) : SevenSegmentDriver = new SevenSegmentDriver(NumberOfDisplays, CycleSpeed)
    def apply(NumberOfDisplays: BigInt, time: TimeNumber) : SevenSegmentDriver = new SevenSegmentDriver(NumberOfDisplays, (time*ClockDomain.current.frequency.getValue).toBigInt())
}

class SevenSegmentDriver(val NumberOfDisplays: BigInt = 1, val CycleSpeed: BigInt = 1) extends ImplicitArea[Bits] { // extends Component {

    val decPoint = Reg(UInt(log2Up(NumberOfDisplays) bit)) init (0)
    val dp = Bool
    val segments = Bits(8 bit)
    val displays = Bits(NumberOfDisplays + 1 bit)

    val timer = Timeout(CycleSpeed)
    val displayCounter = Counter(0, 1 + (NumberOfDisplays << 1))

    val digits = Vec(Reg(Bits(4 bit)), NumberOfDisplays.toInt + 1)

    val digit = Bits(4 bit)

    val digit2segments = Vec(
        Seq(
            B"0111111", //0
            B"0000110", //1
            B"1011011", //2
            B"1001111", //3
            B"1100110", //4
            B"1101101", //5
            B"1111101", //6
            B"0000111", //7
            B"1111111", //8
            B"1101111", //9
            B"1110111", //A
            B"1111100", //B
            B"0111001", //C
            B"1011110", //D
            B"1111001", //E
            B"1110001" //F
        ).map(c => B(c,7 bit))
    )
    digit := digits(displayCounter >> 1)

    when(decPoint =/= 0){
        dp := decPoint-1 === (displayCounter >> 1)
    }otherwise {
        dp := False
    }

    when(!displayCounter(0)){
        displays := B"1" << (displayCounter >> 1)
        segments := Cat(dp, digit2segments(digit.asUInt))
    } otherwise {
        displays := 0
        segments := 0
    }

    when(timer) {
        timer.clear()
        displayCounter.increment()
    }

    def retset(): Unit = {digits.map(_ := 0)}

    def setDigit(digit: Int, number: UInt): Unit = {
        digits(digit) := number.asBits.resize(4)
    }

    def setDigits(digit: Int, number: UInt): Unit = {
        setDigit(digit, number & 0x0F)
        setDigit(digit+1, (number & 0xF0) >> 4)
    }

    def setDecPoint(digit: Int): Unit ={
        decPoint := digit
    }

    override def implicitValue: Bits = Cat(displays, segments)
}

class SevenSegmentDriverTest extends Component {
    val io = new Bundle {
        val c = in Bits(16 bit)
    }

    val ssd = SevenSegmentDriver(3, 4)
    when(io.c === 0){
        ssd.retset();
    }
    when(io.c === 10){
        ssd.setDecPoint(1)
        ssd.setDigits(0, 255)
        ssd.setDigit(2, 5)
        ssd.setDigit(3, 8)
    }
}

object SevenSegmentTest {
    def main(args: Array[String]) {
        SimConfig.withWave.compile{
            val dut = new SevenSegmentDriverTest()
            dut
        }.doSim { dut =>
            //Fork a process to generate the reset and the clock on the dut
            dut.clockDomain.forkStimulus(period = 10)
            dut.clockDomain.waitRisingEdge()


            var c = 0;
            val loop = new Breaks;
            loop.breakable {
                while (true) {
                    dut.clockDomain.waitRisingEdge()
                    dut.io.c #= c
                    if(c == 500) loop.break;
                    c+=1
                }
            }
        }
    }
}