package Spinal1802

import spinal.core._
import spinal.lib.fsm.{EntryPoint, State, StateDelay, StateMachine}
import spinal.lib.Counter

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

class CDP1802() extends Component {
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
        val Addr = out Bits(8 bit)
        val Addr16 = out Bits(16 bit)
        val DataIn = in Bits(8 bit)
        val DataOut = out Bits(8 bit)
    }
    //External
    val SC = Reg(Bits(2 bit)) //State Code Lines
    val Q = Reg(Bool)  init(False)
    val TPA = Reg(Bool) init(False)
    val TPB = Reg(Bool) init(False)
    val MRD = Reg(Bool) init(True)
    val MWR = Reg(Bool) init(True)

    //Internal
    val StateCounter = Counter(8)
    val StartCounting = Reg(Bool) init(False)
    val Mode = Reg(CPUModes())
    val RegSelMode = Reg(RegSelectModes())
    val RegOpMode = Reg(RegOperationModes())
    val ExeMode = Reg(ExecuteModes())
    val BusControl = Reg(BusControlModes())
    val DRegControl = Reg(DRegControlModes())

    val Addr = Reg(UInt(16 bit)) init(0)//Current Address Register
    val Addr16 = RegNext(Addr) init(0)//gives it a delay by one clock
    val D = Reg(UInt(8 bit)) init(0)//Data Register (Accumulator)
    val Dlast = RegNext(D)//Data Register (Accumulator)

    val outN = Reg(UInt(3 bit)) init(0) //Holds Low-Order Instruction Digit
    val N = Reg(UInt(4 bit)) //Holds Low-Order Instruction Digit
    val I = Reg(UInt(4 bit)) //Holds High-Order Instruction Digit
    val P = Reg(UInt(4 bit)) //Designates which register is Program Counters
    val X = Reg(UInt(4 bit)) //Designates which register is Data Pointer
    val T = Reg(UInt(8 bit)) init(0)//Holds old X, P after Interrupt (X is high nibble)

    val EF = RegNext(~io.EF_n)
    val IE = Reg(Bool) init(True) //Interrupt Enable
    val DF = Reg(Bool) init(False) //Data Flag (ALU Carry)
    val DFLast = RegNext(DF) //For Testing
    val OP = RegNext(Cat(I,N))
    val Idle = Reg(Bool) init(False)
    val Reset = Reg(Bool) init(False)
    val Branch = Reg(Bool) init(False)
    val Skip = RegNext(N === 0x4 || N === 0x5 || N === 0x6 || N === 0x7 || N === 0x8 || N === 0xC || N === 0xD || N === 0xE || N === 0xF)
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
    val R = Vec(Reg(UInt(16 bits)), 16)
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
    io.Addr16 := Addr16.asBits

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
	 when(Reset){
        R(0).setAllTo(false)
    }elsewhen(RegOpMode === RegOperationModes.Inc){
        R(RSel) := A + 1
    } elsewhen (RegOpMode === RegOperationModes.Dec) {
        R(RSel) := A - 1
    } elsewhen (RegOpMode === RegOperationModes.LoadUpper) {
        R(RSel) := Cat (Bus,R(RSel)(7 downto 0).asBits).asUInt
    } elsewhen (RegOpMode === RegOperationModes.LoadLower) {
        R(RSel) := Cat (R(RSel)(15 downto 8).asBits,Bus).asUInt
    }

    //Address Logic
	  when(Reset){
        Addr := 0;
    }elsewhen(StateCounter === 0) {
        Addr := A;
    }

    when(StateCounter >= 1 && StateCounter <= 2) {
        io.Addr := Addr(15 downto 8).asBits
    } otherwise(io.Addr := Addr(7 downto 0).asBits)

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
        when ((SC === 1 || SC === 2) && (
                ExeMode === ExecuteModes.Write || ExeMode === ExecuteModes.WriteDec ||
                ExeMode === ExecuteModes.WriteNoInc || ExeMode === ExecuteModes.DMA_In
            )){
            MWR := False
        }
    } otherwise(MWR := True)

    // D Register Logic / ALU Logic
    ALU_Add := Bus.resize(9) + D.resize(9)
    ALU_AddCarry := ALU_Add + Cat(B"8'h00", DF).asUInt
    ALU_SubD := Bus.resize(9) - D.resize(9)
    ALU_SubM := D.resize(9) - Bus.resize(9)
    ALU_SubDB := ALU_SubD - Cat(B"8'h00", !DF).asUInt
    ALU_SubMB := ALU_SubM - Cat(B"8'h00", !DF).asUInt

	when(Reset){
        DF := False
        D := 0
    }elsewhen(DRegControl === DRegControlModes.BusIn){
        D := Bus
    }elsewhen(DRegControl === DRegControlModes.ALU_OR){
        D := Bus | D
    }elsewhen(DRegControl === DRegControlModes.ALU_XOR){
        D := Bus ^ D
    }elsewhen(DRegControl === DRegControlModes.ALU_AND){
        D := Bus & D
    }elsewhen(DRegControl === DRegControlModes.ALU_RSH){
        DF := Dlast.lsb
        D := D |>> 1
    }elsewhen(DRegControl === DRegControlModes.ALU_RSHR) {
        DF := Dlast.lsb
        D := D |>> 1 | Cat(DFLast, B"7'h00").asUInt
    }elsewhen(DRegControl === DRegControlModes.ALU_LSH){
        DF := Dlast.msb
        D := D |<< 1
    }elsewhen(DRegControl === DRegControlModes.ALU_LSHR) {
        DF := Dlast.msb
        D := D |<< 1 | Cat(B"7'h00", DFLast).asUInt
    }elsewhen(DRegControl === DRegControlModes.ALU_Add) {
        DF := ALU_Add.msb
        D := ALU_Add.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_AddCarry) {
        DF := ALU_AddCarry.msb
        D := ALU_AddCarry.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_SubD){
        DF := !ALU_SubD.msb
        D := ALU_SubD.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_SubM) {
        DF := !ALU_SubM.msb
        D := ALU_SubM.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_SubDBorrow){
        DF := !ALU_SubDB.msb
        D := ALU_SubDB.resize(8)
    }elsewhen(DRegControl === DRegControlModes.ALU_SubMBorrow){
        DF := !ALU_SubMB.msb
        D := ALU_SubMB.resize(8)
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

    //Check for a branch conditions
        when(N === 0x0 || (I === 0xC && N===0x4)) {
            Branch := True
        }elsewhen(N === 0x1 || (I === 0xC && N===0x5)){
            Branch := (Q === True)
        }elsewhen(N === 0x2 || (I === 0xC && N===0x6)){
            Branch := (D === 0x0)
        }elsewhen(N === 0x3 || (I === 0xC && N===0x7)){
            Branch := (DF === True)
        }elsewhen(N === 0x9 || (I === 0xC && N===0xD)){
            Branch := (Q === False)
        }elsewhen(N === 0xA || (I === 0xC && N===0xE)){
            Branch := (D =/= 0x0)
        }elsewhen(N === 0xB || (I === 0xC && N===0xF)){
            Branch := (DF === False)
        }elsewhen((I === 0xC && N===0xC)){
            Branch := (IE === False)
        }elsewhen(I === 0x3 && (N===0x4 || N===0x5 || N===0x6 || N===0x7)){
            Branch := (EF(N(1 downto 0)) === True)
        }elsewhen(I === 0x3 && (N===0xC || N===0xD || N===0xE || N===0xF)) {
            Branch := (EF(N(1 downto 0)) === False)
        }otherwise(Branch := False)


    val CoreFMS = new StateMachine {

        val S1_Reset: State = new State with EntryPoint {
            whenIsActive {
                SC := 1

                when(Mode =/= CPUModes.Reset) {
                    goto(S1_Init)
                }
            }
        }

        val S1_Init: State = new StateDelay(9) {
            whenIsActive {
                StateCounter.clear()
                ExeMode := ExecuteModes.None
                RegSelMode := RegSelectModes.PSel
                RegOpMode := RegOperationModes.None
                DRegControl := DRegControlModes.None
                BusControl := BusControlModes.DataIn
                Reset := True
                Idle := False
                IE := True
                outN := 0
                T := 0
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
                Reset := False
                SC := 0

				when(Mode === CPUModes.Reset) {
                    goto(S1_Reset)
				}

                when(StateCounter === 0) {
                    ExeMode := ExecuteModes.None
                    BusControl := BusControlModes.DataIn
                    RegSelMode := RegSelectModes.PSel
                }
                when(StateCounter === 1) {
                    RegOpMode := RegOperationModes.Inc
                } //INC PC
                when(StateCounter === 2){
                    RegOpMode := RegOperationModes.None
                }
                when(StateCounter === 6) {
                    I := io.DataIn(7 downto 4).asUInt
                    N := io.DataIn(3 downto 0).asUInt
                }

                when(StateCounter === 7) {
                    switch(I) {
                        is(0x0) { //Tested
                            when(N === 0){
                                Idle := True
                                ExeMode := ExecuteModes.LoadNoInc
                                RegSelMode := RegSelectModes.DMA0
                            }elsewhen(N >= 1){ //LOAD VIA N
                                ExeMode := ExecuteModes.LoadNoInc
                                RegSelMode := RegSelectModes.NSel
                            }
                        }
                        is(0x1){ //Tested
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0x2){ //Tested
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0x3) {
                            ExeMode := ExecuteModes.Load
                        }
                        is(0x4){ //Tested - LOAD ADVANCE
                            ExeMode := ExecuteModes.Load
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0x5){ //STORE VIA N
                            ExeMode := ExecuteModes.WriteNoInc
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0x6){
                            RegSelMode := RegSelectModes.XSel
                            when(N > 0 && N <= 7){
                                ExeMode := ExecuteModes.Load
                            }elsewhen(N >= 9){
                                ExeMode := ExecuteModes.WriteNoInc
                            }
                        }
                        is(0x7){
                            when(N === 0x0 || N === 0x1 ||  N === 0x2) {
                                RegSelMode := RegSelectModes.XSel
                                ExeMode := ExecuteModes.Load
                            }elsewhen(N === 0x3){
                                RegSelMode := RegSelectModes.XSel
                                ExeMode := ExecuteModes.WriteDec
                            }elsewhen(N === 0x4 || N === 0x5 || N === 0x7) {
                                RegSelMode := RegSelectModes.XSel
                                ExeMode := ExecuteModes.LoadNoInc
                            }elsewhen(N === 0x8){
                                RegSelMode := RegSelectModes.XSel
                                ExeMode := ExecuteModes.WriteNoInc
                            }elsewhen(N === 0x9){
                                T := Cat(X, P).asUInt
                                RegSelMode := RegSelectModes.Stack2
                                ExeMode := ExecuteModes.WriteDec
                            }elsewhen(N === 0xC || N === 0xD || N === 0xF){
                                ExeMode := ExecuteModes.Load
                            }
                        }
                        is(0x8){ //Tested
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0x9){ //Tested
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0xA){ //Tested
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0xB){ //Tested
                            RegSelMode := RegSelectModes.NSel
                        }
                        is(0xC){
                            ExeMode := ExecuteModes.Load
                        }
                        is(0xF){
                            when(N <= 0x5 || N === 0x7){//
                                RegSelMode := RegSelectModes.XSel
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
                Reset := False
                SC := 1
				when(Mode === CPUModes.Reset) {
                    goto(S1_Reset)
				}
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
                            when(N =/= 0x0) { //LOAD VIA N
                                DRegControl := DRegControlModes.BusIn
                            }
                        }
                        is(0x1) {RegOpMode := RegOperationModes.Inc} //INCREMENT REG N
                        is(0x2) {RegOpMode := RegOperationModes.Dec} //DECREMENT REG N
                        is(0x3) { //SHORT BRANCH
                            when(Branch) {
                                RegOpMode := RegOperationModes.LoadLower
                            }
                        }
                        is(0x4){ //LOAD ADVANCE
                            DRegControl := DRegControlModes.BusIn
                        }
                        is(0x6){
                            when(N === 0){
                                RegOpMode := RegOperationModes.Inc
                            }elsewhen(N >= 9){
                                DRegControl := DRegControlModes.BusIn
                            }
                        }
                        is(0x7){
                            when(N === 0x0 || N === 0x1) {// RET and DIS
                                IE := !N.lsb
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
                                when(Branch && !Skip) {
                                    RegOpMode := RegOperationModes.LoadUpper
                                }
                            }elsewhen(ExeMode === ExecuteModes.LongLoad && !Skip){
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
                    }elsewhen(I === 0xc && (RegOpMode === RegOperationModes.LoadUpper || (Skip && !Branch))){
                        ExeMode := ExecuteModes.LongLoad
                    }elsewhen(I === 0xc && Branch){
                        ExeMode := ExecuteModes.LongContinue
                    }

                    when(Idle){
                        RegSelMode := RegSelectModes.DMA0
                        ExeMode === ExecuteModes.LoadNoInc
                    }otherwise {
                        RegSelMode := RegSelectModes.PSel
                        RegOpMode := RegOperationModes.None
                        DRegControl := DRegControlModes.None
                    }
                }

                when(StateCounter.willOverflow){
                    outN := 0
                    when(!io.DMA_In_n) {
                        RegSelMode := RegSelectModes.DMA0
                        ExeMode := ExecuteModes.DMA_In
                        goto(S2_DMA)
                    }elsewhen(!io.DMA_Out_n){
                        RegSelMode := RegSelectModes.DMA0
                        ExeMode := ExecuteModes.DMA_Out
                        goto(S2_DMA)
                    }elsewhen(!io.Interrupt_n && IE){
                        ExeMode := ExecuteModes.None
                        goto(S3_INT)
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
				when(Mode === CPUModes.Reset) {
                    goto(S1_Reset)
				}

                when(StateCounter === 0) {
                    BusControl := BusControlModes.DataIn
                    RegSelMode := RegSelectModes.DMA0
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
                            RegSelMode := RegSelectModes.PSel
                            goto(S0_Fetch)
                        }
                    }
                }
            }
        }

        val S3_INT: State = new State {
            whenIsActive {
                SC := 3
				when(Mode === CPUModes.Reset) {
                    goto(S1_Reset)
				}

                when(StateCounter === 2) {
                    T := Cat(X, P).asUInt
                }

                when(StateCounter === 3) {
                    P := 1;
                    X := 2;
                    IE := False;
                }

                when(StateCounter.willOverflow){
                    Idle := False
                    when(!io.DMA_In_n) {
                        ExeMode := ExecuteModes.DMA_In
                        goto(S2_DMA)
                    }elsewhen(!io.DMA_Out_n) {
                        ExeMode := ExecuteModes.DMA_Out
                        goto(S2_DMA)
                    } otherwise {
                        RegSelMode := RegSelectModes.PSel
                        goto(S0_Fetch)
                    }
                }
            }
        }
    }
}


//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object CDP1802SpinalConfig extends SpinalConfig(
    targetDirectory = ".",
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)
)

//Generate the MyTopLevel's Verilog using the above custom configuration.
object CDP1802Gen {
    def main(args: Array[String]) {
        CDP1802SpinalConfig.generateVerilog(new CDP1802).printPruned
    }
}
