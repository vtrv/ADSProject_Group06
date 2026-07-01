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

class EX extends Module {
  val io = IO(new Bundle {
    val uop         = Input(uopc())
    val operandA    = Input(UInt(32.W))   
    val operandB    = Input(UInt(32.W))   
    val imm         = Input(UInt(32.W))
    val pc          = Input(UInt(32.W))
    val rd          = Input(UInt(5.W))
    val wrEn        = Input(Bool())
    val XcptInvalid = Input(Bool())

    val forwardA    = Input(UInt(2.W))
    val forwardB    = Input(UInt(2.W))

    val aluResultMEM = Input(UInt(32.W))  
    val aluResultWB  = Input(UInt(32.W))  

    val aluResult      = Output(UInt(32.W))
    val rdOut          = Output(UInt(5.W))
    val wrEnOut        = Output(Bool())
    val XcptInvalidOut = Output(Bool())

    // Control Flow Redirection Interface
    val redirectValid  = Output(Bool())
    val redirectPC     = Output(UInt(32.W))
  })

  val alu = Module(new ALU)

  // Forwarding Multiplexer A
  val aluInputA = WireDefault(io.operandA)
  switch (io.forwardA) {
    is ("b01".U) { aluInputA := io.aluResultWB }   
    is ("b10".U) { aluInputA := io.aluResultMEM }  
  }

  // Forwarding Multiplexer B
  val aluInputB = WireDefault(io.operandB)
  switch (io.forwardB) {
    is ("b01".U) { aluInputB := io.aluResultWB }   
    is ("b10".U) { aluInputB := io.aluResultMEM }  
  }

  // Pure Combinational Branch Condition Solver (Pre-ALU Register)
  val isBranch = io.uop === uopc.isBEQ  || io.uop === uopc.isBNE  || 
                 io.uop === uopc.isBLT  || io.uop === uopc.isBGE  || 
                 io.uop === uopc.isBLTU || io.uop === uopc.isBGEU
  val isJump   = io.uop === uopc.isJAL  || io.uop === uopc.isJALR

  val opAsSInt = aluInputA.asSInt
  val opBsSInt = aluInputB.asSInt

  val branchConditionMet = WireDefault(false.B)
  switch (io.uop) {
    is (uopc.isBEQ)  { branchConditionMet := aluInputA === aluInputB }
    is (uopc.isBNE)  { branchConditionMet := aluInputA =/= aluInputB }
    is (uopc.isBLT)  { branchConditionMet := opAsSInt < opBsSInt }
    is (uopc.isBGE)  { branchConditionMet := opAsSInt >= opBsSInt }
    is (uopc.isBLTU) { branchConditionMet := aluInputA < aluInputB }
    is (uopc.isBGEU) { branchConditionMet := aluInputA >= aluInputB }
  }

  val branchTaken = isBranch && branchConditionMet
  io.redirectValid := branchTaken || isJump

  // Target Address Resolution
  val targetPC = WireDefault(0.U(32.W))
  when (io.uop === uopc.isJALR) {
    targetPC := (aluInputA + io.imm) & "hfffffffe".U // Clear least significant bit per RISC-V spec
  } .otherwise {
    targetPC := io.pc + io.imm
  }
  io.redirectPC := targetPC

  // Setup ALU standard execution paths
  val aluOp = WireDefault(ALUOp.NOP)
  val finalAluB = WireDefault(aluInputB)

  // Map control instructions to write execution links (PC + 4 link calculation)
  when (isJump) {
    aluOp := ALUOp.ADD
    alu.io.operandA := io.pc
    alu.io.operandB := 4.U
  } .otherwise {
    alu.io.operandA := aluInputA
    alu.io.operandB := aluInputB
    switch (io.uop) {
      is (uopc.isADD, uopc.isADDI)   { aluOp := ALUOp.ADD  }
      is (uopc.isSUB)                { aluOp := ALUOp.SUB  }
      is (uopc.isXOR, uopc.isXORI)   { aluOp := ALUOp.XOR  }
      is (uopc.isOR,  uopc.isORI)    { aluOp := ALUOp.OR   }
      is (uopc.isAND, uopc.isANDI)   { aluOp := ALUOp.AND  }
      is (uopc.isSLL, uopc.isSLLI)   { aluOp := ALUOp.SLL  }
      is (uopc.isSRL, uopc.isSRLI)   { aluOp := ALUOp.SRL  }
      is (uopc.isSRA, uopc.isSRAI)   { aluOp := ALUOp.SRA  }
      is (uopc.isSLT, uopc.isSLTI)   { aluOp := ALUOp.SLT  }
      is (uopc.isSLTU,uopc.isSLTIU)  { aluOp := ALUOp.SLTU }
    }
  }

  alu.io.operation := aluOp

  io.aluResult      := alu.io.aluResult
  io.rdOut          := io.rd
  io.wrEnOut        := Mux(isBranch, false.B, io.wrEn) // Guard register writes on branches
  io.XcptInvalidOut := io.XcptInvalid || alu.io.exception
}