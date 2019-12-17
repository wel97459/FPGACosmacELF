package mylib

import spinal.core.{Bundle, _}
import spinal.lib.fsm.{EntryPoint, State, StateDelay, StateMachine}
import spinal.lib.{Counter}

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
    val None, Load, LoadDec, LoadNoInc, Write, WriteDec, WriteNoInc, LongLoad, LongContinue, DMA_In, DMA_Out  = newElement()
}

object BusControlModes extends SpinalEnum {
    val DataIn, DReg, TReg, PXReg, RLower, RUpper  = newElement()
}

object DRegControlModes extends SpinalEnum {
    val None, BusIn, ALU_OR, ALU_XOR, ALU_AND, ALU_RSH, ALU_LSH, ALU_RSHR, ALU_LSHR, ALU_Add, ALU_AddCarry, ALU_SubD, ALU_SubDBorrow, ALU_SubM, ALU_SubMBorrow  = newElement()
}

class cpu1802() extends Component {
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

    val outN = Reg(UInt(3 bit)) init(0) //Holds Low-Order Instruction Digit
    val N = Reg(UInt(4 bit)) //Holds Low-Order Instruction Digit
    val I = Reg(UInt(4 bit)) //Holds High-Order Instruction Digit
    val P = Reg(UInt(4 bit)) //Designates which register is Program Counters
    val X = Reg(UInt(4 bit)) //Designates which register is Data Pointer
    val T = Reg(UInt(8 bit)) init(0)//Holds old X, P after Interrupt (X is high nibble)

    val IE = Reg(Bool) init(True) //Interrupt Enable
    val DF = Reg(Bool) init(False) //Data Flag (ALU Carry)
    val Idle = Reg(Bool) init(False)
    //ALU Operations
    val ALU_Add = UInt(9 bit)
    val ALU_AddCarry = UInt(9 bit)
    val ALU_SubD = UInt(9 bit)
    val ALU_SubM = UInt(9 bit)
    val ALU_SubDB = UInt(9 bit)
    val ALU_SubMB = UInt(9 bit)

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
    io.N := outN.asBits
    io.TPA := TPA
    io.TPB := TPB
    io.MRD := MRD
    io.MWR := MWR
    io.DataOut := Bus.asBits

    //Counter Control
    when(StartCounting && Mode =/= CPUModes.Pause) {
        StateCounter.increment()
    }


    //Mode Logic
    when(!io.Clear_n && !io.Wait_n) {
        Mode := CPUModes.Load
    } elsewhen (!io.Clear_n && io.Wait_n) {
        Mode := CPUModes.Reset
    } elsewhen (io.Clear_n && !io.Wait_n) {
        Mode := CPUModes.Pause
    } otherwise (Mode := CPUModes.Run)

    //TPA & TPB Logic
    when(StateCounter === 1 && Mode =/= CPUModes.Reset) {
        TPA := True
    } otherwise (TPA := False)

    when(StateCounter === 6 && Mode =/= CPUModes.Reset) {
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
        } elsewhen ((SC === 1 || SC === 2) && (
                ExeMode === ExecuteModes.Load || ExeMode === ExecuteModes.LongLoad ||
                ExeMode === ExecuteModes.LoadDec || ExeMode === ExecuteModes.LoadNoInc ||
                ExeMode === ExecuteModes.LongLoad || ExeMode === ExecuteModes.LongContinue || ExeMode === ExecuteModes.DMA_Out
            )) {
            MRD := False
        }
    } otherwise(MRD := True)

    //Memory Write Control Lines
    when(StateCounter >= 5 && StateCounter < 7) {
        when ((SC === 1 || SC === 2) && (ExeMode === ExecuteModes.Write || ExeMode === ExecuteModes.WriteDec || ExeMode === ExecuteModes.WriteNoInc || ExeMode === ExecuteModes.DMA_In)){
            MWR := False
        }
    } otherwise(MWR := True)

    // D Register Logic / ALU Logic
    ALU_Add := Bus + D.resize(9);
    ALU_AddCarry := ALU_Add + DF.asUInt
    ALU_SubD := Bus - D.resize(9);
    ALU_SubM := D.resize(9) - Bus
    ALU_SubDB := ALU_SubD - ~DF.asUInt
    ALU_SubMB := ALU_SubM - ~DF.asUInt

    when(DRegControl === DRegControlModes.BusIn){
        D := Bus
    }elsewhen(DRegControl === DRegControlModes.ALU_OR){
        D := Bus | D
    }elsewhen(DRegControl === DRegControlModes.ALU_XOR){
        D := Bus ^ D
    }elsewhen(DRegControl === DRegControlModes.ALU_AND){
        D := Bus & D
    }elsewhen(DRegControl === DRegControlModes.ALU_RSH){
        DF := D.lsb
        D := D |>> 1
    }elsewhen(DRegControl === DRegControlModes.ALU_RSHR) {
        DF := D.lsb
        D := D.rotateRight(1)
    }elsewhen(DRegControl === DRegControlModes.ALU_LSH){
        DF := D.msb
        D := D |<< 1
    }elsewhen(DRegControl === DRegControlModes.ALU_LSHR) {
        DF := D.msb
        D := D.rotateLeft(1)
    }elsewhen(DRegControl === DRegControlModes.ALU_Add) {
        DF := ALU_Add.msb
        D := ALU_Add.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_AddCarry) {
        DF := ALU_AddCarry.msb
        D := ALU_AddCarry.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_SubD){
        DF := ALU_SubD.msb
        D := ALU_SubD.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_SubM){
        DF := ALU_SubM.msb
        D := ALU_SubM.resize(8)
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
                when(Mode =/= CPUModes.Reset) {
                    goto(S1_Init)
                }
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
                Idle := False
                IE := False
                DF := False
                outN := 0
                T := 0
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
                when(Mode === CPUModes.Load) {
                    goto(S1_Execute)
                }otherwise(goto(S0_Fetch))
            }
        }

        val S0_Fetch: State = new State {
            whenIsActive{
                SC := 0
                when(StateCounter === 0) {
                    ExeMode := ExecuteModes.None
                    BusControl := BusControlModes.DataIn
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
                            when(N === 0){
                                Idle := True
                            }elsewhen(N >= 1){ //LOAD VIA N
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
                            ExeMode := ExecuteModes.Load
                        }
                        is(0x4){ //Tested - LOAD ADVANCE
                            ExeMode := ExecuteModes.Load
                            RegSelMode := mylib.RegSelectModes.NSel
                        }
                        is(0x5){ //STORE VIA N
                            ExeMode := ExecuteModes.WriteNoInc
                            RegSelMode := mylib.RegSelectModes.NSel
                        }
                        is(0x6){
                            RegSelMode := mylib.RegSelectModes.XSel
                            when(N > 0 && N <= 7){
                                ExeMode := ExecuteModes.Load
                            }elsewhen(N >= 9){
                                ExeMode := ExecuteModes.WriteNoInc
                            }
                        }
                        is(0x7){
                            when(N >= 0x0 && N <= 0x2) {
                                RegSelMode := mylib.RegSelectModes.XSel
                                ExeMode := ExecuteModes.Load
                            }elsewhen(N === 0x3){
                                RegSelMode := mylib.RegSelectModes.XSel
                                ExeMode := ExecuteModes.WriteDec
                            }elsewhen(N === 0x4 || N === 0x5 || N === 0x7) {
                                RegSelMode := mylib.RegSelectModes.XSel
                                ExeMode := ExecuteModes.LoadNoInc
                            }elsewhen(N === 0x8){
                                RegSelMode := mylib.RegSelectModes.XSel
                                ExeMode := ExecuteModes.WriteNoInc
                            }elsewhen(N === 0x9){
                                T := Cat(X, P).asUInt
                                RegSelMode := mylib.RegSelectModes.Stack2
                                ExeMode := ExecuteModes.WriteDec
                            }elsewhen(N === 0xC || N === 0xD || N === 0xF){
                                ExeMode := ExecuteModes.Load
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
                        is(0xC){
                            ExeMode := ExecuteModes.Load
                        }
                        is(0xF){
                            when(N <= 0x5 || N === 0x7){//
                                RegSelMode := mylib.RegSelectModes.XSel
                                ExeMode := ExecuteModes.LoadNoInc
                            } elsewhen(N >= 0x8 && N <= 0xd || N === 0xF) { //
                                ExeMode := ExecuteModes.Load
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
                    when(ExeMode === ExecuteModes.Load || ExeMode === ExecuteModes.Write || ExeMode === ExecuteModes.LongLoad)
                    {
                        RegOpMode := RegOperationModes.Inc //INC PC
                    }elsewhen(ExeMode === ExecuteModes.LoadDec || ExeMode === ExecuteModes.WriteDec || ExeMode === ExecuteModes.LongContinue){//Long Skip Move us back one place since we do a load for all 0xC Ops
                        RegOpMode := RegOperationModes.Dec
                    }

                    when(I === 6 & N > 0){
                        outN := N.resize(3)
                    }
                }

                when(StateCounter === 2){
                    RegOpMode := RegOperationModes.None
                }

                when(StateCounter === 4){
                    switch(I) {
                        is(0x5) {
                            BusControl := BusControlModes.DReg
                        }
                        is(0x7) {
                            when(N === 8 || N === 9) {
                                BusControl := BusControlModes.TReg //I know it says X,P to bus, but this the same data
                            }elsewhen(N === 3){
                                BusControl := BusControlModes.DReg
                            }
                        }
                    }
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
                            when(N === 0x0) {
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N === 0x1 && Q){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N === 0x2 && D === 0){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N === 0x3 && DF){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N >= 0x4 && N <= 0x7 && io.EF_n(N(1 downto 0))){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N === 0x9 && !Q){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N === 0xA && D =/= 0){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N === 0xB && !DF){
                                RegOpMode := RegOperationModes.LoadLower
                            }elsewhen(N >= 0xC && !io.EF_n(N(1 downto 0))){
                                RegOpMode := RegOperationModes.LoadLower
                            }
                        }
                        is(0x4){ //LOAD ADVANCE
                            DRegControl := DRegControlModes.BusIn
                        }
                        is(0x6){
                            when(N === 0){
                                RegOpMode := RegOperationModes.Inc
                            }
                        }
                        is(0x7){
                            when(N === 0 || N === 1) {// RET and DIS
                                IE := N.lsb
                                X := Bus(7 downto 4)
                                P := Bus(3 downto 0)
                            }elsewhen(N === 0x2) { //LOAD VIA X AND ADVANCE
                                DRegControl := DRegControlModes.BusIn
                            }elsewhen(N === 0x4 || N === 0xC) {
                                DRegControl := DRegControlModes.ALU_AddCarry
                            }elsewhen(N === 0x5 || N === 0xD) {
                                DRegControl := DRegControlModes.ALU_SubDBorrow
                            }elsewhen(N === 0x6){
                                DRegControl := DRegControlModes.ALU_RSHR
                            }elsewhen(N === 0x7 || N === 0xF){
                                DRegControl := DRegControlModes.ALU_SubMBorrow
                            }elsewhen(N === 0xA){
                                Q := False
                            }elsewhen(N === 0xB){
                                Q := True
                            }elsewhen(N === 0xE) {
                                DRegControl := DRegControlModes.ALU_LSHR
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

                        is(0xC){
                            when(ExeMode === ExecuteModes.Load) {
                                when(N === 0x0) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                } elsewhen (N === 0x1 && Q) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                } elsewhen (N === 0x2 && D === 0) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                } elsewhen (N === 0x3 && DF) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                } elsewhen (N === 0x9 && !Q) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                } elsewhen (N === 0xA && D =/= 0) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                } elsewhen (N === 0xB && !DF) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                }
                            }elsewhen(ExeMode === ExecuteModes.LongLoad){
                                RegOpMode := RegOperationModes.LoadLower
                            }
                        }

                        is(0xD){ //SET P
                            P := N
                        }

                        is(0xE) { //SET X
                            X := N
                        }

                        is(0xF){
                            when (N === 0x0){ //LOAD VIA X
                                DRegControl := DRegControlModes.BusIn
                            }elsewhen (N === 0x1 || N === 0x9){
                                DRegControl := DRegControlModes.ALU_OR
                            }elsewhen (N === 0x2 || N === 0xA){
                                DRegControl := DRegControlModes.ALU_AND
                            }elsewhen (N === 0x3 || N === 0xB) {
                                DRegControl := DRegControlModes.ALU_XOR
                            }elsewhen(N === 0x4 || N === 0xC) {
                                DRegControl := DRegControlModes.ALU_Add
                            }elsewhen(N === 0x5 || N === 0xD){
                                DRegControl := DRegControlModes.ALU_SubD
                            }elsewhen (N === 0x6) {
                                DRegControl := DRegControlModes.ALU_RSH
                            }elsewhen(N === 0x7 || N === 0xF) {
                                DRegControl := DRegControlModes.ALU_SubM
                            }elsewhen (N === 0x8){ //LOAD IMMEDIATE
                                DRegControl := DRegControlModes.BusIn
                            }elsewhen (N === 0xE){
                                DRegControl := DRegControlModes.ALU_LSH
                            }
                        }
                    }
                }

                when(StateCounter === 6){
                    when(I === 7 && N === 9){ //Mark
                        X := P
                    }

                    when(ExeMode === ExecuteModes.LongLoad || ExeMode === ExecuteModes.LongContinue){
                        ExeMode := ExecuteModes.None
                    }elsewhen(I === 0xc && N === 0x4){
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0xD && !Q) {
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0xE && D =/= 0) {
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0xF && !DF) {
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0x5 && Q) {
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0x6 && D === 0) {
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0x7 && DF) {
                        ExeMode := ExecuteModes.LongContinue
                    } elsewhen (I === 0xc && N === 0xC && !IE) {
                        ExeMode := ExecuteModes.LongContinue
                    }elsewhen(I === 0xc && RegOpMode === RegOperationModes.LoadUpper){
                        ExeMode := ExecuteModes.LongLoad
                    }
                    RegSelMode := mylib.RegSelectModes.PSel
                    RegOpMode := RegOperationModes.None
                    DRegControl := DRegControlModes.None
                }

                when(StateCounter.willOverflow){
                    outN := 0
                    when(Mode === CPUModes.Reset) {
                        goto(S1_Reset)
                    }elsewhen(!io.DMA_In_n) {
                        ExeMode := ExecuteModes.DMA_In
                        goto(S2_DMA)
                    }elsewhen(!io.DMA_Out_n){
                        ExeMode := ExecuteModes.DMA_Out
                        goto(S2_DMA)
                    }elsewhen(Mode === CPUModes.Load) {
                        ExeMode := ExecuteModes.None
                    }otherwise{
                        when(!(ExeMode === ExecuteModes.LongLoad || ExeMode === ExecuteModes.LongContinue) && !Idle) {
                            goto(S0_Fetch)
                        }
                    }
                }
            }
        }

        val S2_DMA: State = new State {
            whenIsActive {
                SC := 2
                when(StateCounter === 0) {
                    BusControl := BusControlModes.DataIn
                    RegSelMode := mylib.RegSelectModes.DMA0
                }

                when(StateCounter === 1) {
                    RegOpMode := RegOperationModes.Inc
                }

                when(StateCounter === 2) {
                    RegOpMode := RegOperationModes.None
                }

                when(StateCounter.willOverflow) {
                    when(io.DMA_In_n && io.DMA_Out_n) {
                        ExeMode := ExecuteModes.None
                        when(Mode === CPUModes.Load) {
                            goto(S1_Execute)
                        }otherwise {
                            Idle := False
                            RegSelMode := mylib.RegSelectModes.PSel
                            goto(S0_Fetch)
                        }
                    }
                }
            }
        }

        val S3_INT: State = new State {
            whenIsActive {
                SC := 3
                when(StateCounter.willOverflow){
                    Idle := False
                    goto(S0_Fetch)
                }
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