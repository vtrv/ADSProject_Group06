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
      dut.io.result.expect(9.U)     // ADD x3, x1, x2
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2047.U)  // ADDI x4, x0, 2047
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(16.U)    // ADDI x5, x0, 16
      dut.io.exception.expect(false.B)
      dut.clock.step(1)
      dut.io.result.expect(2031.U)  // SUB x6, x4, x5
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
      dut.io.exception.expect(true.B) // DIV x3, x2, x1 → NOT IMPLEMENTED, exception
      dut.clock.step(1)
    }
  }

  // ==========================================================================
  // Extended Test: Comprehensive RAW hazard and forwarding scenarios
  // ==========================================================================
  "RV32I_ExtendedTester" should "work" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile_pipelined_extended"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        // Fill pipeline
        dut.clock.step(5)

        // ================================================================
        // Section 1: Distance-1 RAW hazard (forward from EX barrier / MEM)
        // ================================================================

        // Instr 0: ADDI x1, x0, 4 → x1=4
        dut.io.result.expect(4.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 1: ADD x1, x1, x2 → x1 fwd dist-1(MEM)=4, x2=0(regfile) → x1=4
        dut.io.result.expect(4.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 2: ADDI x2, x0, 5 → x2=5
        dut.io.result.expect(5.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 3: ADD x3, x1, x2 → x1 fwd dist-2(WB)=4, x2 fwd dist-1(MEM)=5 → x3=9
        // Tests double forwarding: both operands forwarded from different stages
        dut.io.result.expect(9.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 2: Distance-2 RAW & chain forwarding
        // ================================================================

        // Instr 4: ADDI x4, x0, 2047
        dut.io.result.expect(2047.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 5: ADDI x5, x0, 16
        dut.io.result.expect(16.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 6: SUB x6, x4, x5 → x4 fwd dist-2(WB)=2047, x5 fwd dist-1(MEM)=16 → 2031
        dut.io.result.expect(2031.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 3: Chain forwarding (3+ dependent instructions)
        // ================================================================

        // Instr 7: XOR x7, x6, x3 → x6 fwd dist-1(MEM)=2031, x3 dist-4(regfile)=9 → 2022
        dut.io.result.expect(2022.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 8: OR x8, x6, x5 → x6 fwd dist-2(WB)=2031, x5 dist-3(regfile bypass)=16 → 2047
        dut.io.result.expect(2047.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 9: AND x9, x6, x5 → x6 dist-3(regfile bypass)=2031, x5 dist-4(regfile)=16 → 0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 4: Shift operations with forwarding
        // ================================================================

        // Instr 10: SLL x10, x7, x2 → x7 dist-3(regfile bypass)=2022, x2=5 → 64704
        dut.io.result.expect(64704.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 11: SRL x11, x7, x2 → x7=2022, x2=5 → 63
        dut.io.result.expect(63.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 12: SRA x12, x7, x2 → x7=2022, x2=5 → 63
        dut.io.result.expect(63.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 5: Comparison ops
        // ================================================================

        // Instr 13: SLT x13, x4, x4 → 0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 14: SLT x13, x4, x5 → 0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 15: SLT x13, x5, x4 → 1
        dut.io.result.expect(1.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 16: SLTU x13, x4, x4 → 0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 17: SLTU x13, x4, x5 → 0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 18: SLTU x13, x5, x4 → 1
        dut.io.result.expect(1.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 6: I-type source hazard (distance-1, operand A forwarding)
        // ================================================================

        // Instr 19: ADDI x28, x0, 20 → x28=20
        dut.io.result.expect(20.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 20: ADDI x28, x28, 1 → x28 fwd dist-1(MEM)=20 + 1 = 21
        dut.io.result.expect(21.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 21: ADDI x28, x28, 1 → x28 fwd dist-1(MEM)=21 + 1 = 22
        dut.io.result.expect(22.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 7: x0 forwarding guard (writing x0 must NOT forward)
        // ================================================================

        // Instr 22: ADDI x0, x0, 999 → ALU result=999, but x0 stays 0
        dut.io.result.expect(999.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 23: ADD x29, x0, x0 → x0 NOT forwarded (guard: rd≠0), x29=0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 8: Same-cycle register file read/write bypass (distance-3)
        // ================================================================

        // Instr 24: ADDI x30, x0, 10 → x30=10
        dut.io.result.expect(10.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 25: NOP → result=0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 26: NOP → result=0
        dut.io.result.expect(0.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // Instr 27: ADD x31, x0, x30 → x30 from same-cycle regfile bypass=10 → x31=10
        dut.io.result.expect(10.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 9: Double forwarding from different stages
        // ================================================================

        // Instr 28: ADD x31, x30, x31 → x30 dist-4(regfile)=10, x31 fwd dist-1(MEM)=10 → 20
        dut.io.result.expect(20.U)
        dut.io.exception.expect(false.B)
        dut.clock.step(1)

        // ================================================================
        // Section 10: Exception test
        // ================================================================

        // Instr 29: DIV x3, x2, x1 → NOT IMPLEMENTED, exception
        dut.io.exception.expect(true.B)
        dut.clock.step(1)
      }
  }

}
