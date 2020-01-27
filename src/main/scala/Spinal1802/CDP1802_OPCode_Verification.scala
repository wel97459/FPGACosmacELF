package Spinal1802

import scala.util.control._
import spinal.core.{assert, _}
import spinal.sim._
import spinal.core.sim._

import scala.util.Random

//MyTopLevel's testbench
object CDP1802_OPCode_Verification {
  def main(args: Array[String]) {
    SimConfig.withWave.compile{
      val dut = new CDP1802
      dut.OP.simPublic()
      dut.D.simPublic()
      dut.Dlast.simPublic()
      dut.X.simPublic()
      dut.P.simPublic()
      dut.Bus.simPublic()
      dut.DF.simPublic()
      dut.DFLast.simPublic()
      dut.R.simPublic()
      dut.Idle.simPublic()
      dut
      dut
    }.doSim { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.io.Wait_n #= false
      dut.io.Clear_n #= false
      dut.io.DMA_Out_n #= true
      dut.io.DMA_In_n #= true
      dut.io.Interrupt_n #= true
      dut.io.EF_n #= 0x0

      dut.clockDomain.waitRisingEdge()

      dut.io.Wait_n #= true
      dut.io.Clear_n #= true

      val loop = new Breaks;

      var rand1 = 0;
      loop.breakable {
        for (idx <- 0 until 2000) {
          dut.clockDomain.waitRisingEdge()
          if (dut.io.TPA.toBoolean && dut.io.SC.toInt == 0) {
            rand1 = Random.nextInt(0xff) | 0x11;
          }
          if (dut.io.MRD.toBoolean == false) {
            val addr = {
              var i = -1;
              (x: Int) => {
                if(x == -1) {
                  i += 1
                  printf("Addr Debug: %04x\r\n", i);
                } else {i += x}
                i
              }
            }
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x40 //LOAD ADVANCE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xf8 //LOAD IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xB1 //PUT HIGH REG N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xA1 //PUT LOW REG N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x11 //INCREMENT REG N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x21 //DECREMENT REG N

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xE1 //SET X


            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x01 //LOAD VIA N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x60 //INCREMENT REG X

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x72 //LOAD VIA X AND ADVANCE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF0 //LOAD VIA X

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x73 //STORE VIA X AND DECREMENT
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x91 //GET HIGH REG N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x51 //STORE VIA N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x80 //GET LOW REG N

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF1 //OR
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF9 //OR IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x08
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF3 //XOR
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFB //EXCLUSIVE OR IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF3
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF2 //AND
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFA //AND IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x81

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xf8 //LOAD IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x76 //RING SHIFT RIGHT
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFE //SHIFT LEFT
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x7E //RING SHIFT LEFT
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF6 //SHIFT RIGHT

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x81 //GET LOW REG N... Needed something to add to

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFC //ADD IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF4 //ADD
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x74 //ADD WITH CARRY
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x7C //ADD WITH CARRY, IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF5 //SUBTRACT D
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFD //SUBTRACT D IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x75 //SUBTRACT D WITH BORROW
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x7D //SUBTRACT D WITH BORROW, IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xF7 //SUBTRACT MEMORY
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFF //SUBTRACT MEMORY IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x77 //SUBTRACT MEMORY WITH BORROW
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x7F //SUBTRACT MEMORY WITH BORROW, IMMEDIATE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= rand1

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x30 //SHORT BRANCH
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x38 //NO SHORT BRANCH

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc0 //LONG BRANCH
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc8 //NO LONG BRANCH
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x11
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x11

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x90 //GET HIGH REG D = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xFE //SHIFT LEFT DF = 0

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x32 //SHORT BRANCH IF D = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x3B //SHORT BRANCH IF DF = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc2 //LONG BRANCH IF D = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xcb //LONG BRANCH IF DF = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc6 //LONG SKIP IF D NOT 0

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xce //LONG SKIP IF D = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xcf //LONG SKIP IF DF = 1

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xc7 //LONG SKIP IF DF = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xf7 //SUBTRACT MEMORY

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x3A //SHORT BRANCH IF D NOT 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x33 //SHORT BRANCH IF DF = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xca //LONG BRANCH IF D NOT 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc3 //LONG BRANCH IF DF = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc6 //LONG SKIP IF D NOT 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xce //LONG SKIP IF D = 0

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xcf //LONG SKIP IF DF = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xc7 //LONG SKIP IF DF = 0

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xcd //LONG SKIP IF Q = 1

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xc5 //LONG SKIP IF Q = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x39 //SHORT BRANCH IF Q = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc9 //LONG BRANCH IF Q = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x7B //SET Q

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x31 //SHORT BRANCH IF Q = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xc1 //LONG BRANCH IF Q = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= (addr(2) >> 8) & 0xff
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xcd //LONG SKIP IF Q = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xc5 //LONG SKIP IF Q = 0

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x7A //RESET Q

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x34 //SHORT BRANCH IF _EF1 = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x35 //SHORT BRANCH IF _EF2 = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x36 //SHORT BRANCH IF _EF3 = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x37 //SHORT BRANCH IF _EF4 = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff

            if (dut.io.Addr16.toInt > addr(0)) dut.io.EF_n #= 0xf
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x3c //SHORT BRANCH IF _EF1 = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x3d //SHORT BRANCH IF _EF2 = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x3e //SHORT BRANCH IF _EF3 = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0x3f //SHORT BRANCH IF _EF4 = 0
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= addr(2) & 0xff
            if (dut.io.Addr16.toInt > addr(0)) dut.io.EF_n #= 0x0

            if (dut.io.Addr16.toInt == addr(2)) dut.io.DataIn #= 0xcc //LONG SKIP IF IE = 1

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x80 //GET LOW REG N
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xfc //INCREMENT REG X
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x04 //INCREMENT REG X
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xA1 //PUT LOW REG N


            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xc8 //SKP
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x20
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00


            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x70 //RETURN
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xcc //LONG SKIP IF IE = 1
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x00
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x79 //PUSH X, P TO STACK
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x78 //SAVE
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xE1 //SET X
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0x71 //DISABLE

            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xc4 //NO OPERATION NOP C4
            if (dut.io.Addr16.toInt == addr(1)) dut.io.DataIn #= 0xd1 //SET P
          } else {
            dut.io.DataIn #= 0x00
          }
          var DFLast = 0;
          //Since we are checking R(0)/PC at the end of the execute state, R(0)/PC will contain the next address to fetch from, this allows us to verify the PC increments for a given op code
          if (dut.io.SC.toInt == 0x1 && dut.io.TPB.toBoolean) {
            val DFL = if (dut.DFLast.toBoolean) 1 else 0
            val DF = if (dut.DF.toBoolean) 1 else 0

            val ls = (dut.Dlast.toInt << 1) & 0xff
            val rs = (dut.Dlast.toInt >> 1) & 0xff

            val lrsDF = if ((dut.Dlast.toInt & 0x80) != 0) 1 else 0
            val rrsDF = if ((dut.Dlast.toInt & 0x01) != 0) 1 else 0
            val lrs = ls | DFL
            val rrs = rs | (DFL << 7)

            val add = dut.Dlast.toInt + dut.Bus.toInt
            val addDF = if ((add & 0x100) == 0x100) 1 else 0;
            val addc = dut.Dlast.toInt + dut.Bus.toInt + DFL
            val addcDF = if ((addc & 0x100) == 0x100) 1 else 0;

            val subd = (dut.Bus.toInt - dut.Dlast.toInt) & 0x1ff
            val subdDF = if ((subd & 0x100) != 0x100) 1 else 0

            val subdc = (dut.Bus.toInt - dut.Dlast.toInt - (1-DFL)) & 0x1ff
            val subdcDF = if ((subdc & 0x100) != 0x100) 1 else 0

            val subm = (dut.Dlast.toInt - dut.Bus.toInt ) & 0x1ff
            val submDF = if ((subm & 0x100) != 0x100) 1 else 0

            val submc = (dut.Dlast.toInt - dut.Bus.toInt  - (1-DFL)) & 0x1ff
            val submcDF = if ((submc & 0x100) != 0x100) 1 else 0


            val addr = {
              var i = 0;
              (x: Int) => {
                if(x < 0) {
                  printf("Addr Debug: %04x, ", i);
                  i += -x
                  printf("After: %04x\r\n", i);
                } else {i += x}
                i
              }
            }

            val assertTest = {
              (x: Boolean) => {
                if (x == false) {
                  printf("\r\nFailed at %04x\r\n", addr(0) - 1)
                  printf("Addr:%04x OP:%01x Bus:%01x DL:%01x D:%01x DFL:%x\r\n", dut.R(0).toInt, dut.OP.toInt, dut.Bus.toInt, dut.Dlast.toInt, dut.D.toInt, if (dut.DF.toBoolean) 1 else 0);
                  printf("Rand1: %01x\r\n", rand1)

                  val elements = Thread.currentThread.getStackTrace
                  val s = elements(3)
                  System.out.println("Called at " + "(" + s.getFileName + ":" + s.getLineNumber + ")")
                  loop.break;
                }
              }
            }
            val assertTestR = {
              (x: Boolean, y: String) => {
                if (x == false) {

                  printf("\r\nFailed at %04x for %s\r\n", addr(0), y)
                  printf("Addr:%04x OP:%01x Bus:%01x DL:%01x D:%01x DFL:%x\r\n", dut.R(0).toInt, dut.OP.toInt, dut.Bus.toInt, dut.Dlast.toInt, dut.D.toInt, if (dut.DF.toBoolean) 1 else 0);
                  printf("Rand1: %01x\r\n", rand1)
                  printf("Add: %01x,  AddDF: %01x\r\n", add, addDF)
                  val elements = Thread.currentThread.getStackTrace
                  val s = elements(3)
                  System.out.println("Called at " + "(" + s.getFileName + ":" + s.getLineNumber + ")")
                  loop.break;
                }
              }
            }

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == rand1, "LOAD ADVANCE")} //LOAD ADVANCE
            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == 0x00, "LOAD IMMEDIATE")} //LOAD IMMEDIATE

            if (dut.R(0).toInt == addr(1)) {assertTestR((dut.R(1).toInt & 0xff00) == 0x0000,"PUT HIGH REG N")} //PUT HIGH REG N
            if (dut.R(0).toInt == addr(1)) {assertTestR((dut.R(1).toInt & 0x00ff) == 0x0000,"PUT LOW REG N")} //PUT LOW REG N

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.R(1).toInt == 0x0001, "INCREMENT REG N")} //INCREMENT REG N
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.R(1).toInt == 0x0000, "DECREMENT REG N")} //DECREMENT REG N

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.X.toInt == 0x01, "SET X")} //SET X

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == 0x40, "LOAD VIA N")} //LOAD VIA N

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.R(1).toInt == 0x0001, "INCREMENT REG X")} //INCREMENT REG X

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == rand1, "LOAD VIA X AND ADVANCE : D"); assertTestR(dut.R(1).toInt == 0x0002, "LOAD VIA X AND ADVANCE : X")} //LOAD VIA X AND ADVANCE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == 0xf8, "LOAD VIA X")} //LOAD VIA X

            if (dut.io.Addr16.toInt == 0x0002 && !dut.io.MWR.toBoolean) {assertTestR(dut.D.toInt == dut.io.DataOut.toInt, "STORE VIA X AND DECREMENT : DataOut"); assertTestR(dut.R(1).toInt == 0x0001, "STORE VIA X AND DECREMENT")} //STORE VIA X AND DECREMENT

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == 0x00, "GET HIGH REG N")} //GET HIGH REG N

            if (dut.io.Addr16.toInt == 0x0001 && !dut.io.MWR.toBoolean) {assertTestR(dut.D.toInt == dut.io.DataOut.toInt, "STORE VIA N")} //STORE VIA N

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == 0x11, "GET LOW REG N")} //GET LOW REG N

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (dut.Dlast.toInt | dut.Bus.toInt), "OR")} //OR

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (dut.Dlast.toInt | dut.Bus.toInt), "OR IMMEDIATE")} //OR IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (dut.Dlast.toInt ^ dut.Bus.toInt), "XOR")} //XOR

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (dut.Dlast.toInt ^ dut.Bus.toInt), "EXCLUSIVE OR IMMEDIATE")} //EXCLUSIVE OR IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (dut.Dlast.toInt & dut.Bus.toInt), "AND")} //AND

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (dut.Dlast.toInt & dut.Bus.toInt), "AND IMMEDIATE")} //AND IMMEDIATE

            addr(2)//LOAD IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (rrs & 0xff), "RING SHIFT RIGHT"); assertTestR(DF == rrsDF , "RING SHIFT RIGHT : DFL")} //RING SHIFT RIGHT
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (ls & 0xff), "SHIFT LEFT"); assertTestR(DF == lrsDF, "SHIFT LEFT : DFL")} //SHIFT LEFT
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (lrs & 0xff), "RING SHIFT LEFT"); assertTestR(DF == lrsDF, "RING SHIFT LEFT : DFL")} //RING SHIFT LEFT
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (rs & 0xff), "SHIFT RIGHT"); assertTestR(DF == rrsDF, "SHIFT RIGHT : DFL")} //SHIFT RIGHT

            addr(1) //GET LOW REG N... Needed something to add to

            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (add & 0xff), "ADD IMMEDIATE"); assertTestR(DF == addDF, "ADD IMMEDIATE : DFL")} //ADD IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (add & 0xff), "ADD"); assertTestR(DF == addDF, "ADD : DFL")} //ADD
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (addc & 0xff), "ADD WITH CARRY"); assertTestR(DF == addcDF, "ADD WITH CARRY : DFL")} //ADD WITH CARRY
            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (addc & 0xff), "ADD WITH CARRY, IMMEDIATE"); assertTestR(DF == addcDF, "ADD WITH CARRY, IMMEDIATE : DFL")} //ADD WITH CARRY, IMMEDIATE

            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (subd & 0xff), "SUBTRACT D"); assertTestR(DF == subdDF, "SUBTRACT D : DFL")} //SUBTRACT D
            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (subd & 0xff), "SUBTRACT D IMMEDIATE"); assertTestR(DF == subdDF, "SUBTRACT D IMMEDIATE : DFL")} //SUBTRACT D IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (subdc & 0xff), "SUBTRACT D WITH BORROW"); assertTestR(DF == subdcDF, "SUBTRACT D WITH BORROW : DFL")} //SUBTRACT D WITH BORROW
            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (subdc & 0xff), "SUBTRACT D WITH BORROW, IMMEDIATE"); assertTestR(DF == subdcDF, "SUBTRACT D WITH BORROW, IMMEDIATE : DFL")} //SUBTRACT D WITH BORROW, IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (subm & 0xff), "SUBTRACT MEMORY"); assertTestR(DF == submDF, "SUBTRACT MEMORY : DFL")} //SUBTRACT MEMORY
            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (subm & 0xff), "SUBTRACT MEMORY IMMEDIATE"); assertTestR(DF == submDF, "SUBTRACT MEMORY IMMEDIATE : DFL")} //SUBTRACT MEMORY IMMEDIATE
            if (dut.R(0).toInt == addr(1)) {assertTestR(dut.D.toInt == (submc & 0xff), "SUBTRACT MEMORY WITH BORROW"); assertTestR(DF == submcDF, "SUBTRACT MEMORY WITH BORROW : DFL")} //SUBTRACT MEMORY WITH BORROW
            if (dut.R(0).toInt == addr(2)) {assertTestR(dut.D.toInt == (submc & 0xff), "SUBTRACT MEMORY WITH BORROW, IMMEDIATE"); assertTestR(DF == submcDF, "SUBTRACT MEMORY WITH BORROW, IMMEDIATE : DFL")} //SUBTRACT MEMORY WITH BORROW, IMMEDIATE

            if (dut.R(0).toInt == addr(2)) {assertTestR(false, "SHORT BRANCH")} //SHORT BRANCH
            if (dut.io.Addr16.toInt == addr(3)) {assertTestR(false, "NO SHORT BRANCH")} //NO SHORT BRANCH

            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "LONG BRANCH")} //LONG BRANCH
            if (dut.io.Addr16.toInt == addr(4)) {assertTestR(false, "NO LONG BRANCH")} //NO LONG BRANCH

            addr(2) //D=0 DFL=0
            if (dut.R(0).toInt == addr(2)) {assertTestR(false, "SHORT BRANCH IF D = 0")} //SHORT BRANCH IF D = 0
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF DFL = 0")} //SHORT BRANCH IF DFL = 0
            if (dut.R(0).toInt == addr(4)) {assertTestR(false, "LONG BRANCH IF D = 0")} //LONG BRANCH IF D = 0
            if (dut.R(0).toInt == addr(4)) {assertTestR(false, "LONG BRANCH IF D NOT 0")} //LONG BRANCH IF D NOT 0

            addr(3) //LONG SKIP IF D NOT 0
            addr(1) //LONG SKIP IF D = 0

            addr(3) //LONG SKIP IF DFL = 1
            addr(1) //LONG SKIP IF DFL = 0

            addr(1) //D>0 DFL=1
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF D NOT 0")} ////SHORT BRANCH IF D NOT 0
            if (dut.R(0).toInt == addr(2)) {assertTestR(false, "SHORT BRANCH IF DFL = 1")} //SHORT BRANCH IF DFL = 1

            if (dut.R(0).toInt == addr(4)) {assertTestR(false, "LONG BRANCH IF D NOT 0")} //LONG BRANCH IF D NOT 0
            if (dut.R(0).toInt == addr(4)) {assertTestR(false, "LONG BRANCH IF DFL = 1")} //LONG BRANCH IF DFL = 1

            addr(1) //LONG SKIP IF D NOT 0
            addr(3) //LONG SKIP IF D = 0

            addr(1) //LONG SKIP IF DFL = 1
            addr(3) //LONG SKIP IF DFL = 0

            addr(1) //LONG SKIP IF Q = 1
            addr(3) //LONG SKIP IF Q = 0

            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF Q = 0")} //SHORT BRANCH IF Q = 0

            if (dut.R(0).toInt == addr(4)) {assertTestR(false, "LONG BRANCH IF Q = 0")} //LONG BRANCH IF Q = 0

            if (dut.R(0).toInt == addr(3)) {assertTestR(dut.io.Q.toBoolean, "SET Q")} //SET Q

            if (dut.R(0).toInt == addr(1)) {assertTestR(false, "SHORT BRANCH IF Q = 1")} //SHORT BRANCH IF Q = 1

            if (dut.R(0).toInt == addr(4)) {assertTestR(false, "LONG BRANCH IF Q = 1")} //LONG BRANCH IF Q = 1

            addr(5) //LONG SKIP IF Q = 1
            addr(1) //LONG SKIP IF Q = 0

            if (dut.R(0).toInt == addr(1)) {assertTestR(!dut.io.Q.toBoolean, "RESET Q")} //RESET Q

            if (dut.R(0).toInt == addr(1)) {assertTestR(false, "SHORT BRANCH IF _EF1 = 1")} //SHORT BRANCH IF _EF1 = 1
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF2 = 1")} //SHORT BRANCH IF _EF2 = 1
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF3 = 1")} //SHORT BRANCH IF _EF3 = 1
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF4 = 1")} //SHORT BRANCH IF _EF4 = 1
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF1 = 0")} //SHORT BRANCH IF _EF1 = 0
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF2 = 0")} //SHORT BRANCH IF _EF2 = 0
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF3 = 0")} //SHORT BRANCH IF _EF3 = 0
            if (dut.R(0).toInt == addr(3)) {assertTestR(false, "SHORT BRANCH IF _EF4 = 0")} //SHORT BRANCH IF _EF4 = 0
            if (dut.R(0).toInt < addr(0) && dut.Idle.toBoolean) {assertTestR(false, "Hit A IDIE")}

          }
        }
      }
    }
  }
}
