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
    regFile: Register file according to the RISC-V 32I specification

Ports:
    req_1, resp_1: first read port
        req_1.addr: read address for register x[0-31]
        resp_1.data: register data output
    req_2, resp_2: second read port
        req_2.addr: read address for register x[0-31]
        resp_2.data: register data output
    req_3: write port
        req_3.addr: write destination address
        req_3.data: data to write
        req_3.wr_en: write enable signal

Functionality:
    Two read ports allow simultaneous reading of two operands
    Synchronous write updates register if wr_en is asserted


Special Case for hazard resolution:    
    If a register is read and written in the same clock cycle, send the new data to data output!
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
  val addr = Input(UInt(5.W))
  val data = Input(UInt(32.W))
  val wr_en = Input(Bool())
}

class regFile extends Module {
  val io = IO(new Bundle {
    val req_1 = new regFileReadReq
    val resp_1 = new regFileReadResp
    val req_2 = new regFileReadReq
    val resp_2 = new regFileReadResp
    val req_3 = new regFileWriteReq
  })

  // 32 registers, each 32 bits, initialized to 0
  val regFile = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  // Read port 1: x0 is always 0
  io.resp_1.data := Mux(io.req_1.addr === 0.U, 0.U, regFile(io.req_1.addr))

  // Read port 2: x0 is always 0
  io.resp_2.data := Mux(io.req_2.addr === 0.U, 0.U, regFile(io.req_2.addr))

  // Write port: synchronous write, x0 cannot be written
  when(io.req_3.wr_en && io.req_3.addr =/= 0.U) {
    regFile(io.req_3.addr) := io.req_3.data
  }
}
