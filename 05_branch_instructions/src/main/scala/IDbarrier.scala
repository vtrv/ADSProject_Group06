// ADS I Class Project
// Pipelined RISC-V Core - ID Barrier
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
ID-Barrier: pipeline register between Decode and Execute stages

Internal Registers:
    uop: micro-operation code (from uopc enum)
    rd: destination register index, initialized to 0
    operandA: first source operand, initialized to 0
    operandB: second operand/immediate, initialized to 0

Inputs:
    inUOP: micro-operation code from ID stage
    inRD: destination register from ID stage
    inOperandA: first operand from ID stage
    inOperandB: second operand/immediate from ID stage
    inXcptInvalid: exception flag from ID stage

Outputs:
    outUOP: micro-operation code to EX stage
    outRD: destination register to EX stage
    outOperandA: first operand to EX stage
    outOperandB: second operand to EX stage
    outXcptInvalid: exception flag to EX stage
Functionality:
    Save all input signals to a register and output them in the following clock cycle
*/

package core_tile

import chisel3._
import uopc._

// -----------------------------------------
// ID-Barrier
// -----------------------------------------

//ToDo: Add your implementation according to the specification above here 
class IDBarrier extends Module {
  val io = IO(new Bundle {
    val flush = Input(Bool())
    val inUOP = Input(uopc())
    val inRD = Input(UInt(5.W))
    val inRS1 = Input(UInt(5.W))
    val inRS2 = Input(UInt(5.W))
    val inOperandA = Input(UInt(32.W))
    val inOperandB = Input(UInt(32.W))
    val inPC = Input(UInt(32.W))
    val inImm = Input(UInt(32.W))
    val inWrEn = Input(Bool())
    val inXcptInvalid = Input(Bool())

    val outUOP = Output(uopc())
    val outRD = Output(UInt(5.W))
    val outRS1 = Output(UInt(5.W))
    val outRS2 = Output(UInt(5.W))
    val outOperandA = Output(UInt(32.W))
    val outOperandB = Output(UInt(32.W))
    val outPC = Output(UInt(32.W))
    val outImm = Output(UInt(32.W))
    val outWrEn = Output(Bool())
    val outXcptInvalid = Output(Bool())
  })

  val uopReg = RegInit(uopc.isNOP)
  val rdReg = RegInit(0.U(5.W))
  val rs1Reg = RegInit(0.U(5.W))
  val rs2Reg = RegInit(0.U(5.W))
  val operandAReg = RegInit(0.U(32.W))
  val operandBReg = RegInit(0.U(32.W))
  val pcReg = RegInit(0.U(32.W))
  val immReg = RegInit(0.U(32.W))
  val wrEnReg = RegInit(false.B)
  val xcptReg = RegInit(false.B)

  when(io.flush) {
    uopReg := uopc.isNOP
    rdReg := 0.U
    rs1Reg := 0.U
    rs2Reg := 0.U
    operandAReg := 0.U
    operandBReg := 0.U
    pcReg := 0.U
    immReg := 0.U
    wrEnReg := false.B
    xcptReg := false.B
  }.otherwise {
    uopReg := io.inUOP
    rdReg := io.inRD
    rs1Reg := io.inRS1
    rs2Reg := io.inRS2
    operandAReg := io.inOperandA
    operandBReg := io.inOperandB
    pcReg := io.inPC
    immReg := io.inImm
    wrEnReg := io.inWrEn
    xcptReg := io.inXcptInvalid
  }

  io.outUOP := uopReg
  io.outRD := rdReg
  io.outRS1 := rs1Reg
  io.outRS2 := rs2Reg
  io.outOperandA := operandAReg
  io.outOperandB := operandBReg
  io.outPC := pcReg
  io.outImm := immReg
  io.outWrEn := wrEnReg
  io.outXcptInvalid := xcptReg
}
