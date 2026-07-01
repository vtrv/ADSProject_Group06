// ADS I Class Project
// Pipelined RISC-V Core - MEM Barrier
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
MEM-Barrier: pipeline register between Memory and Writeback stages

Internal Registers:
    aluResult: computation result (or future load data)
    rd:        destination register index
    wrEn:      register-file write enable flag (needed by ForwardingUnit for MEM-EX forward)
    exception: exception flag

Inputs:
    inAluResult: result from MEM stage
    inRD:        destination register from MEM stage
    inWrEn:      write enable from MEM stage
    inException: exception flag from MEM stage

Outputs:
    outAluResult: result to WB stage / ForwardingUnit (MEM-EX forward value)
    outRD:        destination register to WB stage / ForwardingUnit
    outWrEn:      write enable to WB stage / ForwardingUnit
    outException: exception flag to WB stage

Functionality:
    Save all input signals to a register and output them in the following clock cycle
*/

package core_tile

import chisel3._

// -----------------------------------------
// MEM-Barrier
// -----------------------------------------

class MEMBarrier extends Module {
  val io = IO(new Bundle {
    val inAluResult = Input(UInt(32.W))
    val inRD        = Input(UInt(5.W))
    val inWrEn      = Input(Bool())
    val inException = Input(Bool())

    val outAluResult = Output(UInt(32.W))
    val outRD        = Output(UInt(5.W))
    val outWrEn      = Output(Bool())
    val outException = Output(Bool())
  })

  val aluResultReg = RegInit(0.U(32.W))
  val rdReg        = RegInit(0.U(5.W))
  val wrEnReg      = RegInit(false.B)
  val exceptionReg = RegInit(false.B)

  aluResultReg := io.inAluResult
  rdReg        := io.inRD
  wrEnReg      := io.inWrEn
  exceptionReg := io.inException

  io.outAluResult := aluResultReg
  io.outRD        := rdReg
  io.outWrEn      := wrEnReg
  io.outException := exceptionReg
}
