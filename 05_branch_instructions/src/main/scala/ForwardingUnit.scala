// ADS I Class Project
// Pipelined RISC-V Core - Forwarding Unit
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 05/09/2026 by Tobias Jauch (@tojauch)

/*
Forwarding Unit: resolves RAW data hazards by forwarding results from later
pipeline stages back to the EX stage ALU input MUXes — without stalling.

Design reference: ADS I lecture slides 6-24 ff, schematic on slide 6-26.

──────────────────────────────────────────────────────────────────────
Hazard conditions and forwarding priority (EX-EX takes priority over MEM-EX):

  EX-EX forward (from EX/MEM barrier — result computed ONE cycle ago):
      forwardA = 10  if  wrEn_MEM && rd_MEM != 0 && rd_MEM == rs1_EX
      forwardB = 10  if  wrEn_MEM && rd_MEM != 0 && rd_MEM == rs2_EX

  MEM-EX forward (from MEM/WB barrier — result computed TWO cycles ago):
      forwardA = 01  if  wrEn_WB && rd_WB != 0 && rd_WB == rs1_EX
                         && NOT (wrEn_MEM && rd_MEM != 0 && rd_MEM == rs1_EX)
      forwardB = 01  if  wrEn_WB && rd_WB != 0 && rd_WB == rs2_EX
                         && NOT (wrEn_MEM && rd_MEM != 0 && rd_MEM == rs2_EX)

  No hazard:
      forwardA = 00  (use register-file value from ID/EX barrier)
      forwardB = 00

──────────────────────────────────────────────────────────────────────
Inputs:
    rs1_EX:   source register 1 of the instruction currently in EX stage
    rs2_EX:   source register 2 of the instruction currently in EX stage
    rd_MEM:   destination register of the instruction currently in MEM stage (EX/MEM barrier)
    rd_WB:    destination register of the instruction currently in WB  stage (MEM/WB barrier)
    wrEn_MEM: register-file write enable of the MEM-stage instruction
    wrEn_WB:  register-file write enable of the WB-stage  instruction

Outputs:
    forwardA: 2-bit MUX select for ALU operand A
                00 = no forward  (ID/EX operandA)
                01 = MEM-EX fwd  (MEM/WB barrier aluResult)
                10 = EX-EX  fwd  (EX/MEM barrier aluResult)
    forwardB: 2-bit MUX select for ALU operand B  (same encoding)
*/

package core_tile

import chisel3._
import chisel3.util._
import uopc._

// -----------------------------------------
// Forwarding Unit
// -----------------------------------------

class ForwardingUnit extends Module {
  val io = IO(new Bundle {
    // Source registers of the instruction currently in EX stage (from ID/EX barrier)
    val rs1_EX   = Input(UInt(5.W))
    val rs2_EX   = Input(UInt(5.W))

    // Destination register + write enable of instruction in MEM stage (EX/MEM barrier)
    val rd_MEM   = Input(UInt(5.W))
    val wrEn_MEM = Input(Bool())

    // Destination register + write enable of instruction in WB stage (MEM/WB barrier)
    val rd_WB    = Input(UInt(5.W))
    val wrEn_WB  = Input(Bool())

    // MUX select outputs to EX stage
    val forwardA = Output(UInt(2.W))
    val forwardB = Output(UInt(2.W))
  })

  // --------------------------------------------------
  // Default: no forwarding
  // --------------------------------------------------
  io.forwardA := "b00".U
  io.forwardB := "b00".U

  // --------------------------------------------------
  // forwardA — operand A (rs1)
  // Check MEM-EX first (lower priority), then EX-EX
  // so that EX-EX overwrites if both match (EX-EX is fresher).
  // --------------------------------------------------

  // MEM-EX hazard for A
  when (io.wrEn_WB &&
        (io.rd_WB =/= 0.U) &&
        (io.rd_WB === io.rs1_EX)) {
    io.forwardA := "b01".U
  }

  // EX-EX hazard for A (overwrites MEM-EX when both match — higher priority)
  when (io.wrEn_MEM &&
        (io.rd_MEM =/= 0.U) &&
        (io.rd_MEM === io.rs1_EX)) {
    io.forwardA := "b10".U
  }

  // --------------------------------------------------
  // forwardB — operand B (rs2)
  // Same priority logic as forwardA.
  // Note: for I-type instructions rs2_EX is don't-care
  // because operandB is an immediate — the MUX sel=00
  // already picks the immediate, so a spurious forward
  // signal is harmless (EX stage MUX input B for 01/10
  // would be selected only if forwardB != 00).
  // --------------------------------------------------

  // MEM-EX hazard for B
  when (io.wrEn_WB &&
        (io.rd_WB =/= 0.U) &&
        (io.rd_WB === io.rs2_EX)) {
    io.forwardB := "b01".U
  }

  // EX-EX hazard for B (higher priority)
  when (io.wrEn_MEM &&
        (io.rd_MEM =/= 0.U) &&
        (io.rd_MEM === io.rs2_EX)) {
    io.forwardB := "b10".U
  }
}
