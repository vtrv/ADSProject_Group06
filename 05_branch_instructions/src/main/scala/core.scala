// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/15/2023 by Tobias Jauch (@tojauch)

/*
The goal of this task is to implement a 5-stage pipeline that features a subset of RV32I (all R-type and I-type instructions). 

    Instruction Memory:
        The CPU has an instruction memory (IMem) with 4096 words, each of 32 bits.
        The content of IMem is loaded from a binary file specified during the instantiation of the MultiCycleRV32Icore module.

    CPU Registers:
        The CPU has a program counter (PC) and a register file (regFile) with 32 registers, each holding a 32-bit value.
        Register x0 is hard-wired to zero.

    Microarchitectural Registers / Wires:
        Various signals are defined as either registers or wires depending on whether they need to be used in the same cycle or in a later cycle.

    Processor Stages:
        The FSM of the processor has five stages: fetch, decode, execute, memory, and writeback.
        All stages are active at the same time and process different instructions simultaneously.

        Fetch Stage:
            The instruction is fetched from the instruction memory based on the current value of the program counter (PC).

        Decode Stage:
            Instruction fields such as opcode, rd, funct3, and rs1 are extracted.
            For R-type instructions, additional fields like funct7 and rs2 are extracted.
            Control signals (isADD, isSUB, etc.) are set based on the opcode and funct3 values.
            Operands (operandA and operandB) are determined based on the instruction type.

        Execute Stage:
            Arithmetic and logic operations, including branch target calculation, are performed based on the control signals and operands.
            The result is stored in the aluResult register.

        Memory Stage:
            No memory operations are implemented in this basic CPU.

        Writeback Stage:
            The result of the operation (writeBackData) is written back to the destination register (rd) in the register file.

    Check Result:
        The final result (writeBackData) is output to the io.check_res signal.
        The exception signal is also passed to the wrapper module. It indicates whether an invalid instruction has been encountered.
        In the fetch stage, a default value of 0 is assigned to io.check_res.
*/

package core_tile

import chisel3._
import chisel3.util._
import Assignment02.{ALU, ALUOp}
import uopc._

class PipelinedRV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
    val exception = Output(Bool())
  })

  // Module Instantiations
  val ifStage  = Module(new IF(BinaryFile))
  val ifBar    = Module(new IFBarrier)
  val idStage  = Module(new ID)
  val idBar    = Module(new IDBarrier)
  val exStage  = Module(new EX)
  val exBar    = Module(new EXBarrier)
  val memStage = Module(new MEM)
  val memBar   = Module(new MEMBarrier)
  val wbStage  = Module(new WB)
  val wbBar    = Module(new WBBarrier)
  val rf       = Module(new regFile)
  val fwdUnit  = Module(new ForwardingUnit)

  // Control Hazard Interconnection Wiring
  val pipelineFlush = exStage.io.redirectValid

  // IF Stage Interconnections
  ifStage.io.redirectValid := exStage.io.redirectValid
  ifStage.io.redirectPC    := exStage.io.redirectPC

  // IF Stage → IF/ID Barrier
  ifBar.io.inInstr := ifStage.io.instr
  ifBar.io.inPC    := ifStage.io.pcOut
  ifBar.io.flush   := pipelineFlush

  // IF/ID Barrier → ID Stage
  idStage.io.instr := ifBar.io.outInstr
  idStage.io.pcIn  := ifBar.io.outPC

  // Register File ↔ ID Stage
  rf.io.req_1              := idStage.io.regFileReq_A
  idStage.io.regFileResp_A := rf.io.resp_1
  rf.io.req_2              := idStage.io.regFileReq_B
  idStage.io.regFileResp_B := rf.io.resp_2

  // ID Stage → ID/EX Barrier
  idBar.io.inUOP         := idStage.io.uop
  idBar.io.inRD          := idStage.io.rd
  idBar.io.inRS1         := idStage.io.rs1
  idBar.io.inRS2         := idStage.io.rs2
  idBar.io.inOperandA    := idStage.io.operandA
  idBar.io.inOperandB    := idStage.io.operandB
  idBar.io.inImm         := idStage.io.immOut
  idBar.io.inPC          := idStage.io.pcOut
  idBar.io.inWrEn        := idStage.io.wrEn
  idBar.io.inXcptInvalid := idStage.io.XcptInvalid
  idBar.io.flush         := pipelineFlush

  // Forwarding Unit Interconnections
  fwdUnit.io.rs1_EX   := idBar.io.outRS1
  fwdUnit.io.rs2_EX   := idBar.io.outRS2
  fwdUnit.io.rd_MEM   := exBar.io.outRD
  fwdUnit.io.wrEn_MEM := exBar.io.outWrEn
  fwdUnit.io.rd_WB    := memBar.io.outRD
  fwdUnit.io.wrEn_WB  := memBar.io.outWrEn

  // ID/EX Barrier → EX Stage
  exStage.io.uop          := idBar.io.outUOP
  exStage.io.rd           := idBar.io.outRD
  exStage.io.wrEn         := idBar.io.outWrEn
  exStage.io.operandA     := idBar.io.outOperandA
  exStage.io.operandB     := idBar.io.outOperandB
  exStage.io.imm          := idBar.io.outImm
  exStage.io.pc           := idBar.io.outPC
  exStage.io.XcptInvalid  := idBar.io.outXcptInvalid

  exStage.io.forwardA     := fwdUnit.io.forwardA
  exStage.io.forwardB     := fwdUnit.io.forwardB
  exStage.io.aluResultMEM := exBar.io.outAluResult   
  exStage.io.aluResultWB  := memBar.io.outAluResult  

  // EX Stage → EX/MEM Barrier
  exBar.io.inAluResult   := exStage.io.aluResult
  exBar.io.inRD          := exStage.io.rdOut
  exBar.io.inWrEn        := exStage.io.wrEnOut
  exBar.io.inXcptInvalid := exStage.io.XcptInvalidOut

  // EX/MEM Barrier → MEM Stage → MEM/WB Barrier
  memBar.io.inAluResult := exBar.io.outAluResult
  memBar.io.inRD        := exBar.io.outRD
  memBar.io.inWrEn      := exBar.io.outWrEn
  memBar.io.inException := exBar.io.outXcptInvalid

  // MEM/WB Barrier → WB Stage
  wbStage.io.aluResult := memBar.io.outAluResult
  wbStage.io.rd        := memBar.io.outRD
  wbStage.io.wrEn      := memBar.io.outWrEn
  wbStage.io.exception := memBar.io.outException

  // WB Stage → Register File Write Port
  rf.io.req_3 := wbStage.io.regFileReq

  // WB Stage → WB Barrier → Top-level Outputs
  wbBar.io.inCheckRes    := wbStage.io.check_res
  wbBar.io.inXcptInvalid := wbStage.io.xcptOut

  io.check_res := wbBar.io.outCheckRes
  io.exception := wbBar.io.outXcptInvalid
}