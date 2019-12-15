package mylib

import spinal.core.{Bundle, _}
import spinal.lib.fsm.{EntryPoint, State, StateDelay, StateMachine}
import spinal.lib.{Counter, CounterFreeRun, Timeout}

object CPUModes extends SpinalEnum {
    val Load, Reset, Pause, Run = newElement()
}

object RegSelectModes extends SpinalEnum {
    val PSel, NSel, XSel, DMA0, Stack2 = newElement()
}

object RegOperationModes extends SpinalEnum {
    val None, Inc, Dec, LoadUpper, LoadLower, UpperOnBus, LowerOnBus  = newElement()
}

object ExecuteModes extends SpinalEnum {
    val None, Load, LoadDec, LoadNoInc, Write, WriteDec, WriteNoInc, longLoad  = newElement()
}

object BusControlModes extends SpinalEnum {
    val DataIn, DReg, TReg, PXReg, RLower, RUpper  = newElement()
}

object DRegControlModes extends SpinalEnum {
    val None, BusIn = newElement()
}

class cpu1802 extends Component {
    val io = new Bundle {
        val Wait_n = in Bool
        val Clear_n = in Bool
        val DMA_In_n = in Bool
        val DMA_Out_n = in Bool
        val Interrupt_n = in Bool
        val EF_n = in Bits (4 bit)


        val Q = out Bool
        val SC = out Bits (2 bit)
        val N = out Bits (3 bit)
        val TPA = out Bool
        val TPB = out Bool

        val MRD = out Bool
        val MWR = out Bool
        val Add = out Bits(8 bit)
        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)
    }
    //External
    val SC = Reg(Bits(2 bit)) //State Code Lines
    val Q = Reg(Bool)
    val TPA = Reg(Bool)
    val TPB = Reg(Bool)
    val MRD = Reg(Bool)
    val MWR = Reg(Bool)

    //Internal
    val timeout = Timeout(20 ms)
    val StateCounter = Counter(8)
    val StartCounting = Reg(Bool) init(False)
    val Mode = Reg(CPUModes())
    val RegSelMode = Reg(RegSelectModes())
    val RegOpMode = Reg(RegOperationModes())
    val ExeMode = Reg(ExecuteModes())
    val BusControl = Reg(BusControlModes())
    val DRegControl = Reg(DRegControlModes())

    val Add = Reg(UInt(16 bit)) init(0)//Current Address Register
    val D = Reg(UInt(8 bit)) init(0)//Data Register (Accumulator)
    val B = Reg(UInt(8 bit)) init(0)//Data Flag (ALU Carry)

    val N = Reg(UInt(4 bit)) //Holds Low-Order Instruction Digit
    val I = Reg(UInt(4 bit)) //Holds High-Order Instruction Digit
    val P = Reg(UInt(4 bit)) //Designates which register is Program Counters
    val X = Reg(UInt(4 bit)) //Designates which register is Data Pointer
    val T = Reg(UInt(8 bit)) init(0)//Holds old X, P after Interrupt (X is high nibble)

    val IE = Reg(Bool) init(True) //Interrupt Enable
    val DF = Reg(Bool) init(False) //Data Flag (ALU Carry)

    //Internal Wires
    val Bus = UInt(8 bit) //Current Address
    val A = UInt(16 bit) //Current Address
    val RSel = UInt(4 bit) //Registers Select

    //Scratch Pad Registers
    val R0 = Reg(UInt(16 bits))
    val R1 = Reg(UInt(16 bits))
    val R2 = Reg(UInt(16 bits))
    val R3 = Reg(UInt(16 bits))
    val R4 = Reg(UInt(16 bits))
    val R5 = Reg(UInt(16 bits))
    val R6 = Reg(UInt(16 bits))
    val R7 = Reg(UInt(16 bits))
    val R8 = Reg(UInt(16 bits))
    val R9 = Reg(UInt(16 bits))
    val R10 = Reg(UInt(16 bits))
    val R11 = Reg(UInt(16 bits))
    val R12 = Reg(UInt(16 bits))
    val R13 = Reg(UInt(16 bits))
    val R14 = Reg(UInt(16 bits))
    val R15 = Reg(UInt(16 bits))
    val R = Vec(R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15)
    A := R(RSel)

    //IO Assignments
    io.Q := Q
    io.SC := SC
    io.N := N(2 downto 0).asBits
    io.TPA := TPA
    io.TPB := TPB
    io.MRD := MRD
    io.MWR := MWR
    io.DataOut := Bus.asBits

    //Counter Control
    when(timeout) {
        timeout.clear()
        when(StartCounting && Mode =/= CPUModes.Pause) {
            StateCounter.increment()
        }
    }

    //Mode Logic
    when(!io.Clear_n && !io.Wait_n) {
        Mode := CPUModes.Load
    } elsewhen (!io.Clear_n && io.Wait_n) {
        Mode := CPUModes.Reset
    } elsewhen (io.Clear_n && !io.Wait_n) {
        Mode := CPUModes.Pause
    } otherwise (Mode := CPUModes.Run) //TODO make this function

    //TPA & TPB Logic
    when(StateCounter === 1) { //TODO make this work with the DMA stuff
        TPA := True
    } otherwise (TPA := False)

    when(StateCounter === 6) {
        TPB := True
    } otherwise (TPB := False)

    //Register Array Selection Logic
    when(RegSelMode === RegSelectModes.NSel) {
        RSel := N
    } elsewhen (RegSelMode === RegSelectModes.XSel) {
        RSel := X
    } elsewhen (RegSelMode === RegSelectModes.Stack2) {
        RSel := 2
    } elsewhen (RegSelMode === RegSelectModes.DMA0) {
        RSel := 0
    } otherwise (RSel := P)

    //Register Array Operation Logic
    when(RegOpMode === RegOperationModes.Inc){
        R(RSel) := A + 1
    } elsewhen (RegOpMode === RegOperationModes.Dec) {
        R(RSel) := A - 1
    } elsewhen (RegOpMode === RegOperationModes.LoadUpper) {
        R(RSel) := Cat (Bus,R(RSel)(7 downto 0).asBits).asUInt
    } elsewhen (RegOpMode === RegOperationModes.LoadLower) {
        R(RSel) := Cat (R(RSel)(15 downto 8).asBits,Bus).asUInt
    }

    //Address Logic
    when(StateCounter === 0) {
        Add := A;
    }
    when(StateCounter >= 1 && StateCounter <= 2) {
        io.Add := Add(15 downto 8).asBits
    } otherwise(io.Add := Add(7 downto 0).asBits)

    //Memory Read Control Lines
    when(StateCounter >= 3) {
        when(SC === 0) {
            MRD := False
        } elsewhen (SC === 1 && (ExeMode === ExecuteModes.Load || ExeMode === ExecuteModes.longLoad || ExeMode === ExecuteModes.LoadDec || ExeMode === ExecuteModes.LoadNoInc)){
            MRD := False
        }//TODO Add DMA MODE
    } otherwise(MRD := True)

    //Memory Write Control Lines
    when(StateCounter >= 5) {
        when (SC === 1 && (ExeMode === ExecuteModes.Write || ExeMode === ExecuteModes.WriteDec || ExeMode === ExecuteModes.WriteNoInc)){
            MWR := False
        }//TODO Add DMA MODE
    } otherwise(MWR := True)

    // D Register Logic
    when(DRegControl === DRegControlModes.BusIn){
        D := Bus
    }

    //Data Bus Logic
    when(BusControl === BusControlModes.DataIn){
        Bus := io.DataIn.asUInt
    } elsewhen(BusControl === BusControlModes.DReg){
        Bus := D
    } elsewhen(BusControl === BusControlModes.TReg){
        Bus := T
    } elsewhen(BusControl === BusControlModes.PXReg){
        Bus := Cat(X, P).asUInt
    } elsewhen(BusControl === BusControlModes.RLower){
        Bus := A(7 downto 0)
    } elsewhen(BusControl === BusControlModes.RUpper){
        Bus := A(15 downto 8)
    } otherwise(Bus := 0)

    val CoreFMS = new StateMachine {

        val S1_Reset: State = new State with EntryPoint {
            whenIsActive {
                SC := 1
                R.map(_ := 0)
                goto(S1_Init)
            }
        }

        val S1_Init: State = new StateDelay(9) {
            whenIsActive{
                StateCounter.clear()
                ExeMode := ExecuteModes.None
                RegSelMode := RegSelectModes.PSel
                RegOpMode := RegOperationModes.None
                DRegControl := DRegControlModes.None
                BusControl := BusControlModes.DataIn

                R0 := 0
                P := 0
                X := 0
                I := 0
                N := 0
                Q := False
                SC := 1
            }

            whenCompleted {
                StartCounting := True
                goto(S0_Fetch)
            }
        }

        val S0_Fetch: State = new State {
            whenIsActive{
                SC := 0
                when(Mode =/= CPUModes.Load) {
                    when(StateCounter === 0) {
                        ExeMode := ExecuteModes.None
                        RegSelMode := mylib.RegSelectModes.PSel
                    }
                    when(StateCounter === 1) {
                        RegOpMode := RegOperationModes.Inc
                    } //INC PC
                    when(StateCounter === 2){
                        RegOpMode := RegOperationModes.None
                    }
                    when(StateCounter === 4) {
                        I := io.DataIn(7 downto 4).asUInt
                        N := io.DataIn(3 downto 0).asUInt
                    }

                    when(StateCounter === 5) {
                        switch(I) {
                            is(0x0) { //Tested
                                when(N >= 0){ //LOAD VIA N
                                    ExeMode := ExecuteModes.LoadNoInc
                                    RegSelMode := mylib.RegSelectModes.NSel
                                }
                            }
                            is(0x1){ //Tested
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0x2){ //Tested
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0x3) {
                                switch(N) {
                                    is(0x0) { //Tested
                                        ExeMode := ExecuteModes.Load
                                    }
                                }
                            }
                            is(0x4){ //Tested - LOAD ADVANCE
                                ExeMode := ExecuteModes.Load
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0x5){ //STORE VIA N
                                ExeMode := ExecuteModes.WriteNoInc
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0x7){
                                switch(N) {
                                    is(0x2){
                                        RegSelMode := mylib.RegSelectModes.XSel
                                        ExeMode := ExecuteModes.Load
                                    }
                                }
                            }
                            is(0x8){ //Tested
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0x9){ //Tested
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0xA){ //Tested
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0xB){ //Tested
                                RegSelMode := mylib.RegSelectModes.NSel
                            }
                            is(0xF){
                                switch(N){
                                    is(0x0){ //LOAD VIA X
                                        RegSelMode := mylib.RegSelectModes.NSel
                                        ExeMode := ExecuteModes.LoadNoInc
                                    }
                                    is(0x8){ //LOAD IMMEDIATE
                                        ExeMode := ExecuteModes.Load
                                    }
                                }
                            }
                        }
                    }
                }

                when(StateCounter.willOverflow){
                    goto(S1_Execute)
                }
            }
        }

        val S1_Execute: State = new State {
            whenIsActive{
                SC := 1
                when(StateCounter === 1){
                    when(ExeMode === ExecuteModes.Load || ExeMode === ExecuteModes.Write || ExeMode === ExecuteModes.longLoad)
                    {
                        RegOpMode := RegOperationModes.Inc //INC PC
                    }elsewhen(ExeMode === ExecuteModes.LoadDec || ExeMode === ExecuteModes.WriteDec){
                        RegOpMode := RegOperationModes.Dec
                    }
                }

                when(StateCounter === 2){
                    RegOpMode := RegOperationModes.None
                }

                when(StateCounter === 5){
                    switch(I){
                        is(0x0){
                            when(N >= 0x0) { //LOAD VIA N
                                DRegControl := DRegControlModes.BusIn
                            }
                        }
                        is(0x1) {RegOpMode := RegOperationModes.Inc} //INCREMENT REG N
                        is(0x2) {RegOpMode := RegOperationModes.Dec} //DECREMENT REG N
                        is(0x3) { //SHORT BRANCH
                            switch(N) {
                                is(0x0) {
                                    RegOpMode := RegOperationModes.LoadLower
                                }
                            }
                        }
                        is(0x4){ //LOAD ADVANCE
                            DRegControl := DRegControlModes.BusIn
                        }
                        is(0x5){ //STORE VIA N
                            BusControl := BusControlModes.DReg
                        }
                        is(0x7){
                            switch(N) {
                                is(0x2) { //LOAD VIA X AND ADVANCE
                                    DRegControl := DRegControlModes.BusIn
                                }
                            }
                        }
                        is(0x8){ //GET LOW REG N
                            BusControl := BusControlModes.RLower
                            DRegControl := DRegControlModes.BusIn
                        }
                        is(0x9){ //GET HIGH REG N
                            BusControl := BusControlModes.RUpper
                            DRegControl := DRegControlModes.BusIn
                        }
                        is(0xA){ //PUT LOW REG N
                            BusControl := BusControlModes.DReg
                            RegOpMode := RegOperationModes.LoadLower
                        }
                        is(0xB){ //PUT HIGH REG N
                            BusControl := BusControlModes.DReg
                            RegOpMode := RegOperationModes.LoadUpper
                        }
                        is(0xD){ //SET P
                            P := N
                        }
                        is(0xE) { //SET X
                            X := N
                        }
                        is(0xF){
                            switch(N){
                                is(0x0){ //LOAD VIA X
                                    DRegControl := DRegControlModes.BusIn
                                }
                                is(0x8){ //LOAD IMMEDIATE
                                    DRegControl := DRegControlModes.BusIn
                                }
                            }
                        }
                    }
                }

                when(StateCounter === 6){
                    RegOpMode := RegOperationModes.None
                    RegSelMode := mylib.RegSelectModes.PSel
                    DRegControl := DRegControlModes.None
                }

                when(StateCounter.willOverflow){
                    BusControl := BusControlModes.DataIn
                    when(Mode === CPUModes.Reset) {
                        goto(S1_Reset)
                    }elsewhen(Mode === CPUModes.Load){
                        ExeMode := ExecuteModes.None
                    }otherwise(goto(S0_Fetch))
                }
            }
        }

        val S2_DMA: State = new State {
            whenIsActive {
                SC := 2
            }
        }

        val S3_INT: State = new State {
            whenIsActive {
                SC := 3
            }
        }
    }
}


//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object cpu1802SpinalConfig extends SpinalConfig(
    targetDirectory = "..",
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object cpu1802Gen {
    def main(args: Array[String]) {
        cpu1802SpinalConfig.generateVerilog(new cpu1802)
    }
}