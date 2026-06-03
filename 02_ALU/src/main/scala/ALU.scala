// ADS I Class Project
// Assignment 02: Arithmetic Logic Unit and UVM Testbench
//
// Chair of Electronic Design Automation, RPTU University Kaiserslautern-Landau
// File created on 09/21/2025 by Tharindu Samarakoon (gug75kex@rptu.de)
// File updated on 10/29/2025 by Tobias Jauch (tobias.jauch@rptu.de)

package Assignment02

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

// ============================================================
// ALUOp Enum
// ============================================================
// Each value maps to one ALU operation.
// The encoding (0..10) matches the alu_tb_config_pkg.sv enum
// so that the generated Verilog port io_operation is directly
// compatible with the UVM testbench.
object ALUOp extends ChiselEnum {
  val ADD   = Value(0.U)   // addition
  val SUB   = Value(1.U)   // subtraction
  val AND   = Value(2.U)   // bitwise AND
  val OR    = Value(3.U)   // bitwise OR
  val XOR   = Value(4.U)   // bitwise XOR
  val SLL   = Value(5.U)   // shift left logical
  val SRL   = Value(6.U)   // shift right logical
  val SRA   = Value(7.U)   // shift right arithmetic
  val SLT   = Value(8.U)   // set less than (signed)
  val SLTU  = Value(9.U)   // set less than unsigned
  val PASSB = Value(10.U)  // pass operandB unchanged
}

// ============================================================
// ALU Module
// ============================================================
class ALU extends Module {

  val io = IO(new Bundle {
    val operandA  = Input(UInt(32.W))       // first operand
    val operandB  = Input(UInt(32.W))       // second operand
    val operation = Input(ALUOp())          // operation selector
    val aluResult = Output(UInt(32.W))      // computed result
  })

  // Default assignment – avoids "unconnected wire" errors if
  // none of the when/elsewhen branches match (should never
  // happen with a well-formed input, but Chisel requires it).
  io.aluResult := 0.U

  // Cast operands to signed where arithmetic sign matters.
  // Chisel's SInt lets us use the >>> arithmetic-shift operator.
  val a_signed = io.operandA.asSInt
  val b_signed = io.operandB.asSInt

  // Only the lower 5 bits of operandB are used as a shift amount
  // (matches the RV32I ISA specification).
  val shamt = io.operandB(4, 0)

  switch(io.operation) {

    is(ALUOp.ADD) {
      // Two's-complement addition; wraps around naturally in 32 bits.
      io.aluResult := io.operandA + io.operandB
    }

    is(ALUOp.SUB) {
      // Two's-complement subtraction.
      io.aluResult := io.operandA - io.operandB
    }

    is(ALUOp.AND) {
      io.aluResult := io.operandA & io.operandB
    }

    is(ALUOp.OR) {
      io.aluResult := io.operandA | io.operandB
    }

    is(ALUOp.XOR) {
      io.aluResult := io.operandA ^ io.operandB
    }

    is(ALUOp.SLL) {
      // Shift left logical: vacated bits filled with 0.
      io.aluResult := io.operandA << shamt
    }

    is(ALUOp.SRL) {
      // Shift right logical: vacated bits filled with 0.
      io.aluResult := io.operandA >> shamt
    }

    is(ALUOp.SRA) {
      // Shift right arithmetic: vacated bits filled with the sign bit.
      // We operate on a signed view and cast back to UInt for the output.
      io.aluResult := (a_signed >> shamt).asUInt
    }

    is(ALUOp.SLT) {
      // Signed comparison: result is 1 if A < B (signed), else 0.
      io.aluResult := Mux(a_signed < b_signed, 1.U, 0.U)
    }

    is(ALUOp.SLTU) {
      // Unsigned comparison: result is 1 if A < B (unsigned), else 0.
      io.aluResult := Mux(io.operandA < io.operandB, 1.U, 0.U)
    }

    is(ALUOp.PASSB) {
      // Simply forward operandB to the output unchanged.
      io.aluResult := io.operandB
    }
  }
}
