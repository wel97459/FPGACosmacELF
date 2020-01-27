package Spinal1802

import java.io.{BufferedReader, FileReader}

import spinal.core.sim._

import scala.util.Random
import scala.util.control._
import java.nio.file.{Files, Paths}

case class Memory(Size: Int) {
  val content = new Array[Int](Size+1);

  def write(address: Long, data: Int): Unit = {
    content(address.toInt & 0xFFFF) = data & 0xff
  }

  def read(address: Long): Int = {
    content(address.toInt & 0xFFFF) & 0xff
  }

  def loadBin(offset: Long, file: String): Unit = {
    val bin = Files.readAllBytes(Paths.get(file))
    for (byteId <- 0 until bin.size) {
      write(offset + byteId, bin(byteId))
    }
  }
}

//Lets you test against Emme's trace logs exported from it debugger
case class TraceEmma(file: String) {
  var reader = new BufferedReader(new FileReader(file));
  var currentLine = ""
  var D = 0;

  def getLine(): String = {
    currentLine = reader.readLine();
    currentLine
  }

  def getLastLine(): String ={
    currentLine
  }

  def cmpAddr(Addr: Int): Boolean ={
    currentLine = reader.readLine();
    val r = "D=([0-9a-fA-F]{2})|M\\([0-9a-fA-F]*.\\)=([0-9a-fA-F]{2})".r
    val mi = r.findAllIn(currentLine)
    if(mi.hasNext){
      val dd = mi.next
      if(mi.group(1) == null) {
        D = Integer.parseInt(mi.group(2), 16)
      } else {
        D = Integer.parseInt(mi.group(1), 16)
      }
      //println("hex: " + D.formatted("%02X") + " Line:" + currentLine)
    }
    val test = currentLine.startsWith(Addr.formatted("%04X"));
    test
  }
}

object cpu1802_Testing_Sim {
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

      val ram = new Memory(4096)
      ram.loadBin(0x00000, "verification\\test_FPM_Verification.bin")

      val trace = new TraceEmma("verification\\test_FPM_Verification.log")

      var c = 0;
      val loop = new Breaks;
      loop.breakable {
        while (true) {
          dut.clockDomain.waitRisingEdge()

          if (dut.io.MRD.toBoolean == false) {
            dut.io.DataIn #= ram.read(dut.io.Addr16.toInt)
          } else {
            dut.io.DataIn #= 0x00
          }

          if (dut.io.MWR.toBoolean == false) {
            ram.write(dut.io.Addr16.toInt, dut.io.DataOut.toInt.toByte)
          }

          if (dut.io.SC.toInt == 0x1 && dut.io.TPB.toBoolean) {
            if (dut.Idle.toBoolean) {
              println("Hit A IDIE")
              println("Verification was successful")
              loop.break;
            }
          }

          if(dut.io.SC.toInt == 0x0 && dut.io.TPA.toBoolean) {
            assert(trace.D == dut.D.toInt, "Issue: " + trace.getLastLine() + ",\tD:" + trace.D.formatted("%02X") + "  C:" + c.toString)
            val t = trace.cmpAddr(dut.io.Addr16.toInt)
            assert(t, "Issue: " + trace.getLastLine() +  "  C:" + c.toString)
            c += 1
          }
        }
      }
    }
  }
}
