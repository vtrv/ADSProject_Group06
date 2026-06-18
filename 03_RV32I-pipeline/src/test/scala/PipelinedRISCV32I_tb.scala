// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/15/2023 by Tobias Jauch (@tojauch)

package PipelinedRV32I_Tester

import chisel3._
import chiseltest._
import PipelinedRV32I._
import org.scalatest.flatspec.AnyFlatSpec

class PipelinedRISCV32ITest extends AnyFlatSpec with ChiselScalatestTester {

"RV32I_BasicTester" should "work" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile_pipelined")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.setTimeout(0)
      dut.clock.step(5)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(4.U)     // ADDI x1, x0, 4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(5.U)     // ADDI x2, x0, 5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(9.U)     // ADD x3, x1, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)  // ADDI x4, x0, 2047
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(16.U)    // ADDI x5, x0, 16
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2031.U)  // SUB x6, x4, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2022.U)  // XOR x7, x6, x3
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)  // OR x8, x6, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // AND x9, x6, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // ADDI x0, x0, 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(64704.U) // SLL x10, x7, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(63.U)    // SRL x11, x7, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(63.U)    // SRA x12, x7, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLT x13, x4, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLT x13, x4, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)     // SLT x13, x5, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLTU x13, x4, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)     // SLTU x13, x4, x5
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)     // SLTU x13, x5, x4
      dut.io.exception.expect(false.B)
      dut.clock.step(1) 
      dut.io.exception.expect(true.B) // DIV x3, x2, x1 (NOT IMPLEMENTED!)
      dut.clock.step(1)     
    }
  }

  // ==========================================================================
  // Extended Test: I-type instructions, hazards, corner cases, exceptions
  // ==========================================================================
  "RV32I_ExtendedTester" should "work" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile_pipelined_extended")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.setTimeout(0)

      // --- Fill pipeline (5 cycles) ---
      dut.clock.step(5)

      // ================================================================
      // Section 1: I-type Immediate Instructions (negative/boundary)
      // ================================================================
      dut.io.result.expect("hFFFFFFFF".U)   // ADDI x1, x0, -1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hFFFFF800".U)   // ADDI x2, x0, -2048
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)          // ADDI x3, x0, 2047
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFF00".U)   // XORI x4, x1, 0xFF
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("h00000123".U)   // ORI  x5, x0, 0x123
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("h000000FF".U)   // ANDI x6, x1, 0xFF
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)             // SLTI  x7, x1, 0  → -1 < 0 signed = true
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // SLTIU x8, x1, 1  → 0xFFFFFFFF > 1 unsigned = false
      dut.io.exception.expect(false.B)
      dut.clock.step(1)

      // ================================================================
      // Section 2: Shift Immediate Edge Cases (shift by 0 and 31)
      // ================================================================
      dut.io.result.expect("hFFFFFFFF".U)   // ADDI x9, x0, -1
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFFF".U)   // SLLI x10, x9, 0  → no shift
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("h80000000".U)   // SLLI x11, x9, 31 → 0x80000000
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFFF".U)   // SRLI x12, x9, 0  → no shift
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(1.U)             // SRLI x13, x9, 31 → 0x00000001
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFFF".U)   // SRAI x14, x9, 0  → no shift, sign preserved
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFFF".U)   // SRAI x15, x9, 31 → all sign bits
      dut.io.exception.expect(false.B)
      dut.clock.step(1)

      // ================================================================
      // Section 3: RAW Hazard — Distance 1 (back-to-back)
      // Multi-cycle would give x17=42; pipeline gives x17=0 (stale x16)
      // ================================================================
      dut.io.result.expect(42.U)            // ADDI x16, x0, 42
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // ADD x17, x16, x0 → HAZARD: x16 stale = 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)

      // ================================================================
      // Section 4: WAW — Two Writes to Same Register
      // Second write (222) wins; consumer reads after both complete
      // ================================================================
      dut.io.result.expect(111.U)           // ADDI x22, x0, 111
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(222.U)           // ADDI x22, x0, 222
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(222.U)           // ADD x23, x22, x0 → x22 = 222 (2nd write won)
      dut.io.exception.expect(false.B)
      dut.clock.step(1)

      // ================================================================
      // Section 5: x0 Hardwiring (write to x0 must be discarded)
      // ================================================================
      dut.io.result.expect(999.U)           // ADDI x0, x0, 999 → ALU=999, but x0 stays 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // ADD x24, x0, x0 → x0 still 0
      dut.io.exception.expect(false.B)
      dut.clock.step(1)

      // ================================================================
      // Section 6: Self-Referencing Operation (same reg as src & dst)
      // ================================================================
      dut.io.result.expect(10.U)            // ADDI x25, x0, 10
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // NOP
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(20.U)            // ADD x25, x25, x25 → 10 + 10 = 20
      dut.io.exception.expect(false.B)
      dut.clock.step(1)

    }
  }
}