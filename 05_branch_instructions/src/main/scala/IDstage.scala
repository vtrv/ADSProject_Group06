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
    Handle flushes due to mispredicted branches

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

class ID extends Module {
  val io = IO(new Bundle {
    val instr         = Input(UInt(32.W))
    val pcIn          = Input(UInt(32.W))

    val regFileReq_A  = Output(new regFileReadReq)
    val regFileResp_A = Input(new regFileReadResp)
    val regFileReq_B  = Output(new regFileReadReq)
    val regFileResp_B = Input(new regFileReadResp)

    val uop           = Output(uopc())
    val rd            = Output(UInt(5.W))
    val rs1           = Output(UInt(5.W))   
    val rs2           = Output(UInt(5.W))   
    val operandA      = Output(UInt(32.W))
    val operandB      = Output(UInt(32.W))
    val immOut        = Output(UInt(32.W))
    val pcOut         = Output(UInt(32.W))
    val wrEn          = Output(Bool())
    val XcptInvalid   = Output(Bool())
  })

  val opcode = io.instr(6, 0)
  val rd     = io.instr(11, 7)
  val funct3 = io.instr(14, 12)
  val rs1    = io.instr(19, 15)
  val rs2    = io.instr(24, 20)
  val funct7 = io.instr(31, 25)

  // Immediate Decoding Logic
  val iImm = io.instr(31, 20)
  val bImm = Cat(io.instr(31), io.instr(7), io.instr(30, 25), io.instr(11, 8), 0.U(1.W))
  val jImm = Cat(io.instr(31), io.instr(19, 12), io.instr(20), io.instr(30, 21), 0.U(1.W))

  val iImmSExt = Cat(Fill(20, iImm(11)), iImm).asUInt
  val bImmSExt = Cat(Fill(19, bImm(12)), bImm).asUInt
  val jImmSExt = Cat(Fill(11, jImm(20)), jImm).asUInt

  io.regFileReq_A.addr := rs1
  io.regFileReq_B.addr := rs2
  io.operandA          := io.regFileResp_A.data

  val uopWire      = WireDefault(uopc.isNOP)
  val operandBWire = WireDefault(0.U(32.W))
  val immWire      = WireDefault(0.U(32.W))
  val xcptWire     = WireDefault(false.B)
  val wrEnWire     = WireDefault(false.B)

  val isRType  = opcode === "b0110011".U
  val isIType  = opcode === "b0010011".U
  val isBType  = opcode === "b1100011".U
  val isJal    = opcode === "b1101111".U
  val isJalr   = opcode === "b1100111".U

  when (isRType) {
    operandBWire := io.regFileResp_B.data
    wrEnWire     := true.B
    switch (funct3) {
      is ("b000".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isADD, Mux(funct7 === "b0100000".U, uopc.isSUB, uopc.isInvalid)) }
      is ("b100".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isXOR, uopc.isInvalid) }
      is ("b110".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isOR,  uopc.isInvalid) }
      is ("b111".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isAND, uopc.isInvalid) }
      is ("b001".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isSLL, uopc.isInvalid) }
      is ("b101".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isSRL, Mux(funct7 === "b0100000".U, uopc.isSRA, uopc.isInvalid)) }
      is ("b010".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isSLT, uopc.isInvalid) }
      is ("b011".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isSLTU, uopc.isInvalid) }
    }
    when(uopWire === uopc.isInvalid) { xcptWire := true.B; wrEnWire := false.B }
  } .elsewhen (isIType) {
    operandBWire := iImmSExt
    wrEnWire     := true.B
    switch (funct3) {
      is ("b000".U) { uopWire := uopc.isADDI  }
      is ("b100".U) { uopWire := uopc.isXORI  }
      is ("b110".U) { uopWire := uopc.isORI   }
      is ("b111".U) { uopWire := uopc.isANDI  }
      is ("b010".U) { uopWire := uopc.isSLTI  }
      is ("b011".U) { uopWire := uopc.isSLTIU }
      is ("b001".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isSLLI, uopc.isInvalid) }
      is ("b101".U) { uopWire := Mux(funct7 === "b0000000".U, uopc.isSRLI, Mux(funct7 === "b0100000".U, uopc.isSRAI, uopc.isInvalid)) }
    }
    when(uopWire === uopc.isInvalid) { xcptWire := true.B; wrEnWire := false.B }
  } .elsewhen (isBType) {
    operandBWire := io.regFileResp_B.data
    immWire      := bImmSExt
    switch (funct3) {
      is ("b000".U) { uopWire := uopc.isBEQ  }
      is ("b001".U) { uopWire := uopc.isBNE  }
      is ("b100".U) { uopWire := uopc.isBLT  }
      is ("b101".U) { uopWire := uopc.isBGE  }
      is ("b110".U) { uopWire := uopc.isBLTU }
      is ("b111".U) { uopWire := uopc.isBGEU }
    }
  } .elsewhen (isJal) {
    immWire      := jImmSExt
    uopWire      := uopc.isJAL
    wrEnWire     := true.B
    operandBWire := 4.U // JAL writes PC + 4 to rd
  } .elsewhen (isJalr) {
    immWire      := iImmSExt
    uopWire      := uopc.isJALR
    wrEnWire     := true.B
    operandBWire := 4.U // JALR writes PC + 4 to rd
    switch (funct3) {
      is ("b000".U) { uopWire := uopc.isJALR }
      otherwise     { xcptWire := true.B; wrEnWire := false.B }
    }
  } .otherwise {
    xcptWire := true.B
  }

  io.uop         := uopWire
  io.rd          := rd
  io.rs1         := rs1
  io.rs2         := rs2
  io.immOut      := immWire
  io.pcOut       := io.pcIn
  io.wrEn        := wrEnWire
  io.XcptInvalid := xcptWire
}