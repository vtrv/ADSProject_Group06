// ADS I Class Project
// Pipelined RISC-V Core - Forwarding Unit
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 05/09/2026 by Tobias Jauch (@tojauch)

/*
Forwarding Unit: resolves data hazards by forwarding results from later pipeline stages to the ID stage

Functionality (cf. slide 6-24ff of the lecture slides):
    Detects data hazards by comparing source registers in the EX stage with destination registers in MEM and WB stages (EX and MEM barriers).
    Generates control signals for the multiplexers in the EX stage to select the correct data source for the ALU inputs
    Handles cases where multiple hazards occur simultaneously (e.g., forwarding from both MEM and WB stages)

Inputs:
    rs1_EX: source register 1 in EX stage
    rs2_EX: source register 2 in EX stage
    rd_MEM: destination register in MEM stage
    rd_WB: destination register in WB stage
    wrEn_MEM: write enable signal for MEM stage
    wrEn_WB: write enable signal for WB stage

Outputs:
    forwardA: control signal for selecting source of operand A in EX stage
    forwardB: control signal for selecting source of operand B in EX stage

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
    // Inputs
    val rs1_EX = Input(UInt(5.W)) // source register 1 in EX stage
    val rs2_EX = Input(UInt(5.W)) // source register 2 in EX stage
    val rd_MEM = Input(UInt(5.W)) // destination register in MEM stage (from EX barrier)
    val rd_WB = Input(UInt(5.W)) // destination register in WB stage (from MEM barrier)
    val wrEn_MEM = Input(Bool()) // write enable for MEM stage
    val wrEn_WB = Input(Bool()) // write enable for WB stage

    // Outputs: 2-bit mux select signals
    // 00 = no forwarding (use ID barrier value)
    // 10 = forward from MEM (EX barrier output)
    // 01 = forward from WB (MEM barrier output)
    val forwardA = Output(UInt(2.W))
    val forwardB = Output(UInt(2.W))
  })

  // Default: no forwarding
  io.forwardA := "b00".U
  io.forwardB := "b00".U

  // --- Forward A (operand A / rs1) ---

  // WB hazard (distance-2): check first so MEM can override (MEM has priority)
  when(io.wrEn_WB && io.rd_WB =/= 0.U && io.rd_WB === io.rs1_EX) {
    io.forwardA := "b01".U
  }

  // MEM hazard (distance-1): higher priority, overrides WB
  when(io.wrEn_MEM && io.rd_MEM =/= 0.U && io.rd_MEM === io.rs1_EX) {
    io.forwardA := "b10".U
  }

  // --- Forward B (operand B / rs2) ---

  // WB hazard (distance-2): check first so MEM can override
  when(io.wrEn_WB && io.rd_WB =/= 0.U && io.rd_WB === io.rs2_EX) {
    io.forwardB := "b01".U
  }

  // MEM hazard (distance-1): higher priority, overrides WB
  when(io.wrEn_MEM && io.rd_MEM =/= 0.U && io.rd_MEM === io.rs2_EX) {
    io.forwardB := "b10".U
  }
}