// ADS I Class Project
// Pipelined RISC-V Core - WB Stage
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Writeback (WB) Stage: result storage and register file updates

Register File Interface:
    regFileReq: write request bundle
        regFileReq.addr:  destination register index
        regFileReq.data:  data to write
        regFileReq.wr_en: write enable — true for all R-type and I-type instructions

Inputs:
    aluResult: computation result from MEM/WB barrier
    rd:        destination register address
    wrEn:      write enable from MEM/WB barrier
    exception: exception flag

Functionality:
    Forward aluResult to register file write port.
    wrEn is driven by the value carried through the pipeline (always true for R/I-type).
    Output result on check_res for testbench verification.

Outputs:
    check_res: result value for verification
    xcptOut:   exception flag
*/

package core_tile

import chisel3._

// -----------------------------------------
// Writeback Stage
// -----------------------------------------

class WB extends Module {
  val io = IO(new Bundle {
    val aluResult = Input(UInt(32.W))
    val rd        = Input(UInt(5.W))
    val wrEn      = Input(Bool())
    val exception = Input(Bool())

    val regFileReq = Output(new regFileWriteReq)

    val check_res = Output(UInt(32.W))
    val xcptOut   = Output(Bool())
  })

  io.regFileReq.addr  := io.rd
  io.regFileReq.data  := io.aluResult
  io.regFileReq.wr_en := io.wrEn

  io.check_res := io.aluResult
  io.xcptOut   := io.exception
}
