package XlinxS6BB

import java.nio.file.{Files, Paths}

import spinal.core._


class RAM_BB(Name: String, AddrDepth: Int = 8, DataDepth: Int = 8, WriteDepth: Int = 1, DualPort: Boolean = false, clkDomain: ClockDomain = null, OutputEnable: Boolean = false) extends BlackBox {
    val io = new Bundle {
        val clka    = in  Bool
        val ena     = ifGen(OutputEnable)(in Bool)
        val wea     = in  Bits(WriteDepth bit)
        val addra   = in Bits(AddrDepth bit)
        val douta   = out Bits(DataDepth bit)
        val dina    = in Bits(DataDepth bit)

        val clkb    = ifGen(DualPort)(in  Bool)
        val enb     = ifGen(OutputEnable && DualPort)(in Bool)
        val web     = ifGen(DualPort)(in  Bits(WriteDepth bit))
        val addrb   = ifGen(DualPort)(in Bits(AddrDepth bit))
        val doutb   = ifGen(DualPort)(out Bits(DataDepth bit))
        val dinb    = ifGen(DualPort)(in Bits(DataDepth bit))
    }
    noIoPrefix()

    mapClockDomain(clock=io.clka)
    ifGen(DualPort)(mapClockDomain(clkDomain, io.clkb))

    setBlackBoxName(Name)
}


class MakeCOE(Size: Int) {
    val content = new Array[Int](Size+1);

    def write(address: Long, data: Int): Unit = {
        content(address.toInt & 0xFFFF) = data & 0xff
    }

    def read(address: Long): Int = {
        content(address.toInt & 0xFFFF) & 0xff
    }

    def saveMEM(file:String): Unit = {
        var outContent = "@00000000\n"
        var fs = ""

        for (byteId <- 0 until content.size-1) {
            fs = "%02x\n".format(read(byteId))
            outContent = outContent.concat(fs)
        }

        Files.write(Paths.get(file), outContent.getBytes)
    }

    def saveCOE(file:String): Unit = {
        var outContent = "memory_initialization_radix=16;\nmemory_initialization_vector=\n"
        var fs = ""

        for (byteId <- 0 until content.size-2) {
            fs = "%02x,\n".format(read(byteId))
            outContent = outContent.concat(fs)
        }
        fs = "%02x".format(read(content.size-1))
        outContent = outContent.concat(fs)

        Files.write(Paths.get(file), outContent.getBytes)
    }

    def saveMIF(file:String): Unit = {
        var outContent = ""
        var fs = ""
        var bin = List(
            "0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111",
            "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111"
        )

        var b = 0
        for (byteId <- 0 until content.size-1) {
            b = read(byteId)
            fs = "%s%s\n".format( bin((b & 0xf0)>>4), bin(b & 0x0f))
            outContent = outContent.concat(fs)
        }

        Files.write(Paths.get(file), outContent.getBytes)
    }

    def loadBin(offset: Long, file: String): Unit = {
        val bin = Files.readAllBytes(Paths.get(file))
        for (byteId <- 0 until bin.size) {
            write(offset + byteId, bin(byteId))
        }
    }
}