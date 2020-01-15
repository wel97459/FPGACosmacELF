package Spinal1802

import spinal.core._
import spinal.lib.Timeout

object Debounce {
    def apply(depth: Int, cycles: BigInt) : Debounce = new Debounce(depth, cycles)
    def apply(depth: Int, time: TimeNumber) : Debounce = new Debounce(depth, (time * ClockDomain.current.frequency.getValue).toBigInt())
    def apply(depth: Int, frequency: HertzNumber) : Debounce = Debounce(depth, frequency.toTime)
}

class Debounce(val depth: Int = 1, val delay: BigInt) extends ImplicitArea[Bits] {
    val timer = Timeout(delay)

    val input = Reg(Bits(depth bit)) init(0)
    val debounce = Reg(Bits(depth bit)) init(0)
    val value = RegNext(debounce) init(0)

    def write(inputVal: Bits): Unit = {
        input := inputVal
    }

    def edge(): Bits = {
        return ~value & debounce
    }

    def falling(): Bits = {
        return value & ~debounce
    }

    when(timer){
        debounce := input
        timer.clear()
    }

    override def implicitValue: Bits = this.value & this.debounce
}
