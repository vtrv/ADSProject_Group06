// ADS I Class Project
// Pipelined RISC-V Core - Register File
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

package core_tile

import chisel3._

/*
Register File Module: 32x32-bit dual-read single-write register file

Memory:
    regFileMem: Register file according to the RISC-V 32I specification

Ports:
    req_1, resp_1: first read port
        req_1.addr:  read address for register x[0-31]
        resp_1.data: register data output
    req_2, resp_2: second read port
        req_2.addr:  read address for register x[0-31]
        resp_2.data: register data output
    req_3: write port
        req_3.addr:  write destination address
        req_3.data:  data to write
        req_3.wr_en: write enable signal

Functionality:
    Two read ports allow simultaneous reading of two operands.

    Write-before-read (internal forwarding):
        Per ADS I slide 6-24, when the WB stage writes to the same register that the
        ID stage is reading in the SAME clock cycle, the register file must return the
        NEW (to-be-written) value rather than the stale stored value.  This eliminates
        the WB→ID hazard without stalling or an extra forwarding path.

        Implementation: for each read port, if wr_en is asserted AND the write address
        equals the read address (and neither is x0), return req_3.data directly;
        otherwise return the stored register value.

    x0 is hard-wired to zero — writes to address 0 are ignored, reads from address 0
    always return 0.
*/

// -----------------------------------------
// Register File
// -----------------------------------------

class regFileReadReq extends Bundle {
  val addr = Input(UInt(5.W))
}

class regFileReadResp extends Bundle {
  val data = Output(UInt(32.W))
}

class regFileWriteReq extends Bundle {
  val addr  = Input(UInt(5.W))
  val data  = Input(UInt(32.W))
  val wr_en = Input(Bool())
}

class regFile extends Module {
  val io = IO(new Bundle {
    val req_1  = Input(new regFileReadReq)
    val resp_1 = Output(new regFileReadResp)

    val req_2  = Input(new regFileReadReq)
    val resp_2 = Output(new regFileReadResp)

    val req_3  = Input(new regFileWriteReq)
  })

  val regFileMem = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  // ----------------------------------------------------------
  // Synchronous write (at clock edge, guarded against x0)
  // ----------------------------------------------------------
  when (io.req_3.wr_en && io.req_3.addr =/= 0.U) {
    regFileMem(io.req_3.addr) := io.req_3.data
  }

  // ----------------------------------------------------------
  // Read port 1 — write-before-read forwarding
  // When WB writes the same register that ID reads this cycle,
  // bypass the stored value and return the incoming write data.
  // ----------------------------------------------------------
  val fwdMatch1 = io.req_3.wr_en &&
                  (io.req_3.addr === io.req_1.addr) &&
                  (io.req_1.addr =/= 0.U)

  io.resp_1.data := Mux(io.req_1.addr === 0.U, 0.U,
                      Mux(fwdMatch1, io.req_3.data,
                        regFileMem(io.req_1.addr)))

  // ----------------------------------------------------------
  // Read port 2 — write-before-read forwarding
  // ----------------------------------------------------------
  val fwdMatch2 = io.req_3.wr_en &&
                  (io.req_3.addr === io.req_2.addr) &&
                  (io.req_2.addr =/= 0.U)

  io.resp_2.data := Mux(io.req_2.addr === 0.U, 0.U,
                      Mux(fwdMatch2, io.req_3.data,
                        regFileMem(io.req_2.addr)))
}
