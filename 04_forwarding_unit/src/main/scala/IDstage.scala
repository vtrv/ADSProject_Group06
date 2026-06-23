// ADS I Class Project
// Pipelined RISC-V Core - ID Stage
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Instruction Decode (ID) Stage: decoding and operand fetch

Extracted Fields from 32-bit Instruction (see RISC-V specification for reference):
    opcode: instruction format identifier
    funct3: selects variant within instruction format
    funct7: further specifies operation type (R-type only)
    rd: destination register address
    rs1: first source register address
    rs2: second source register address
    imm: 12-bit immediate value (I-type, sign-extended)

Register File Interfaces:
    regFileReq_A, regFileResp_A: read port for rs1 operand
    regFileReq_B, regFileResp_B: read port for rs2 operand

Internal Signals:
    Combinational decoders for instructions

Functionality:
    Decode opcode to determine instruction and identify operation (ADD, SUB, XOR, ...)
    Output: uop (operation code), rd, operandA (from rs1), operandB (rs2 or immediate)

Outputs:
    uop: micro-operation code (identifies instruction type)
    rd: destination register index
    operandA: first operand
    operandB: second operand 
    XcptInvalid: exception flag for invalid instructions
*/

package core_tile

import chisel3._
import chisel3.util._
import uopc._

// -----------------------------------------
// Decode Stage
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    // Input from IF barrier
    val instr = Input(UInt(32.W))

    // Register file read port A (rs1)
    val regFileReq_A = Flipped(new regFileReadReq)
    val regFileResp_A = Flipped(new regFileReadResp)

    // Register file read port B (rs2)
    val regFileReq_B = Flipped(new regFileReadReq)
    val regFileResp_B = Flipped(new regFileReadResp)

    // Outputs to ID barrier
    val uop = Output(uopc())
    val rd = Output(UInt(5.W))
    val operandA = Output(UInt(32.W))
    val operandB = Output(UInt(32.W))
    val XcptInvalid = Output(Bool())
  })

  // Extract instruction fields
  val opcode = io.instr(6, 0)
  val rd = io.instr(11, 7)
  val funct3 = io.instr(14, 12)
  val rs1 = io.instr(19, 15)
  val rs2 = io.instr(24, 20)
  val funct7 = io.instr(31, 25)

  // Sign-extended 12-bit immediate (I-type)
  val imm = Cat(Fill(20, io.instr(31)), io.instr(31, 20))

  // Send read requests to register file
  io.regFileReq_A.addr := rs1
  io.regFileReq_B.addr := rs2

  // Default outputs
  io.uop := uopc.isNOP
  io.rd := rd
  io.operandA := io.regFileResp_A.data
  io.operandB := io.regFileResp_B.data
  io.XcptInvalid := false.B

  // R-type opcode: 0110011
  val isRType = opcode === "b0110011".U
  // I-type opcode: 0010011
  val isIType = opcode === "b0010011".U

  when(isRType) {
    io.operandB := io.regFileResp_B.data
    switch(funct3) {
      is("b000".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isADD
        }.elsewhen(funct7 === "b0100000".U) {
          io.uop := uopc.isSUB
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b001".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isSLL
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b010".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isSLT
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b011".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isSLTU
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b100".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isXOR
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b101".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isSRL
        }.elsewhen(funct7 === "b0100000".U) {
          io.uop := uopc.isSRA
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b110".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isOR
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b111".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isAND
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
    }
  }.elsewhen(isIType) {
    io.operandB := imm
    switch(funct3) {
      is("b000".U) {
        io.uop := uopc.isADDI
      }
      is("b001".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isSLLI
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b010".U) {
        io.uop := uopc.isSLTI
      }
      is("b011".U) {
        io.uop := uopc.isSLTIU
      }
      is("b100".U) {
        io.uop := uopc.isXORI
      }
      is("b101".U) {
        when(funct7 === "b0000000".U) {
          io.uop := uopc.isSRLI
        }.elsewhen(funct7 === "b0100000".U) {
          io.uop := uopc.isSRAI
        }.otherwise {
          io.XcptInvalid := true.B
        }
      }
      is("b110".U) {
        io.uop := uopc.isORI
      }
      is("b111".U) {
        io.uop := uopc.isANDI
      }
    }
  }.otherwise {
    // Not a valid R-type or I-type instruction
    // Everything else is invalid (except NOP which is encoded as ADDI)
    when(opcode =/= 0.U) {
      io.XcptInvalid := true.B
    }
  }
}
