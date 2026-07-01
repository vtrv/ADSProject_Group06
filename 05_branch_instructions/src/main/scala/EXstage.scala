// ADS I Class Project
// Pipelined RISC-V Core - EX Stage
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Instruction Execute (EX) Stage: ALU operations and exception detection

Instantiated Modules:
    ALU: Integrate your module from Assignment02 for arithmetic/logical operations

ALU Interface:
    alu.io.operandA: first operand input
    alu.io.operandB: second operand input
    alu.io.operation: operation code controlling ALU function
    alu.io.aluResult: computation result output

Internal Signals:
    Map uopc codes to ALUOp values

Functionality:
    Map instruction uop to ALU operation code
    Pass operands to ALU
    Output results to pipeline

Outputs:
    aluResult: computation result from ALU
    exception: pass exception flag
    branchTarget: calculated branch target address for conditional branch instructions
    flush: control signal to flush pipeline on mispredicted branches
*/

package core_tile

import chisel3._
import chisel3.util._
import Assignment02.{ALU, ALUOp}
import uopc._

// -----------------------------------------
// Execute Stage
// -----------------------------------------

//ToDo: Add your implementation according to the specification above here 
class EX extends Module {
  val io = IO(new Bundle {
    val uop = Input(uopc())
    val operandA = Input(UInt(32.W))
    val operandB = Input(UInt(32.W))
    val pc = Input(UInt(32.W))
    val imm = Input(UInt(32.W))
    val rd = Input(UInt(5.W))
    val XcptInvalid = Input(Bool())

    val aluResult = Output(UInt(32.W))
    val rdOut = Output(UInt(5.W))
    val redirect = Output(Bool())
    val redirectTarget = Output(UInt(32.W))
    val exception = Output(Bool())
  })

  // Instantiate ALU from Assignment02
  val alu = Module(new ALU)

  // Pass operands to ALU
  alu.io.operandA := io.operandA
  alu.io.operandB := io.operandB

  // Default ALU operation
  alu.io.operation := ALUOp.ADD

  // Map uopc to ALUOp
  switch(io.uop) {
    is(uopc.isADD) { alu.io.operation := ALUOp.ADD }
    is(uopc.isADDI) { alu.io.operation := ALUOp.ADD }
    is(uopc.isSUB) { alu.io.operation := ALUOp.SUB }
    is(uopc.isSLL) { alu.io.operation := ALUOp.SLL }
    is(uopc.isSLLI) { alu.io.operation := ALUOp.SLL }
    is(uopc.isSLT) { alu.io.operation := ALUOp.SLT }
    is(uopc.isSLTI) { alu.io.operation := ALUOp.SLT }
    is(uopc.isSLTU) { alu.io.operation := ALUOp.SLTU }
    is(uopc.isSLTIU) { alu.io.operation := ALUOp.SLTU }
    is(uopc.isXOR) { alu.io.operation := ALUOp.XOR }
    is(uopc.isXORI) { alu.io.operation := ALUOp.XOR }
    is(uopc.isSRL) { alu.io.operation := ALUOp.SRL }
    is(uopc.isSRLI) { alu.io.operation := ALUOp.SRL }
    is(uopc.isSRA) { alu.io.operation := ALUOp.SRA }
    is(uopc.isSRAI) { alu.io.operation := ALUOp.SRA }
    is(uopc.isOR) { alu.io.operation := ALUOp.OR }
    is(uopc.isORI) { alu.io.operation := ALUOp.OR }
    is(uopc.isAND) { alu.io.operation := ALUOp.AND }
    is(uopc.isANDI) { alu.io.operation := ALUOp.AND }
    is(uopc.isJAL) { alu.io.operation := ALUOp.ADD }
    is(uopc.isJALR) { alu.io.operation := ALUOp.ADD }
    is(uopc.isNOP) { alu.io.operation := ALUOp.ADD }
  }

  val isBranch = io.uop === uopc.isBEQ || io.uop === uopc.isBNE || io.uop === uopc.isBLT ||
    io.uop === uopc.isBGE || io.uop === uopc.isBLTU || io.uop === uopc.isBGEU
  val isJump = io.uop === uopc.isJAL || io.uop === uopc.isJALR
  val branchTaken = MuxLookup(
    io.uop.asUInt,
    false.B,
    Seq(
      uopc.isBEQ.asUInt -> (io.operandA === io.operandB),
      uopc.isBNE.asUInt -> (io.operandA =/= io.operandB),
      uopc.isBLT.asUInt -> (io.operandA.asSInt < io.operandB.asSInt),
      uopc.isBGE.asUInt -> (io.operandA.asSInt >= io.operandB.asSInt),
      uopc.isBLTU.asUInt -> (io.operandA < io.operandB),
      uopc.isBGEU.asUInt -> (io.operandA >= io.operandB)
    )
  )
  val linkAddress = (io.pc + 4.U)(31, 0)
  val pcRelativeTarget = (io.pc + io.imm)(31, 0)
  val jalrTarget = ((io.operandA + io.imm)(31, 0)) & "hFFFFFFFE".U(32.W) // Clear least significant bit per RISC-V spec

  // Outputs
  io.aluResult := Mux(isJump, linkAddress, alu.io.aluResult)
  io.rdOut := io.rd
  io.redirect := (isBranch && branchTaken) || isJump
  io.redirectTarget := Mux(io.uop === uopc.isJALR, jalrTarget, pcRelativeTarget)
  io.exception := io.XcptInvalid
}
