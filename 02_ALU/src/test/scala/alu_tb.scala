// ADS I Class Project
// Assignment 02: Arithmetic Logic Unit and UVM Testbench
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 10/31/2025 by Tobias Jauch (tobias.jauch@rptu.de)

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import Assignment02._

// ============================================================
// Helper: sign-extend a 32-bit Int to Long so expected values
// wrap correctly when we convert them back to UInt comparisons.
// ============================================================

// ============================================================
// ADD
// ============================================================
// Addition wraps around (two's-complement, 32-bit).
class ALUAddTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Add_Tester" should "test ADD operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // Basic positive + positive
      dut.io.operandA.poke(10.U)
      dut.io.operandB.poke(10.U)
      dut.io.operation.poke(ALUOp.ADD)
      dut.io.aluResult.expect(20.U)
      dut.clock.step(1)

      // Add zero (identity element)
      dut.io.operandA.poke(42.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.ADD)
      dut.io.aluResult.expect(42.U)
      dut.clock.step(1)

      // Wraparound: 0xFFFF_FFFF + 1 == 0
      dut.io.operandA.poke(0xFFFFFFFFL.U)
      dut.io.operandB.poke(1.U)
      dut.io.operation.poke(ALUOp.ADD)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // Large values
      dut.io.operandA.poke(0x7FFFFFFFL.U)
      dut.io.operandB.poke(1.U)
      dut.io.operation.poke(ALUOp.ADD)
      dut.io.aluResult.expect(0x80000000L.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// SUB
// ============================================================
class ALUSubTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Sub_Tester" should "test SUB operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // Basic subtraction
      dut.io.operandA.poke(20.U)
      dut.io.operandB.poke(10.U)
      dut.io.operation.poke(ALUOp.SUB)
      dut.io.aluResult.expect(10.U)
      dut.clock.step(1)

      // Result zero
      dut.io.operandA.poke(5.U)
      dut.io.operandB.poke(5.U)
      dut.io.operation.poke(ALUOp.SUB)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // Wraparound: 0 - 1 == 0xFFFF_FFFF
      dut.io.operandA.poke(0.U)
      dut.io.operandB.poke(1.U)
      dut.io.operation.poke(ALUOp.SUB)
      dut.io.aluResult.expect(0xFFFFFFFFL.U)
      dut.clock.step(1)

      // Subtract from itself
      dut.io.operandA.poke(0xABCDEF01L.U)
      dut.io.operandB.poke(0xABCDEF01L.U)
      dut.io.operation.poke(ALUOp.SUB)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// AND
// ============================================================
class ALUAndTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_And_Tester" should "test AND operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.operandA.poke(0xFFFF0000L.U)
      dut.io.operandB.poke(0x0000FFFFL.U)
      dut.io.operation.poke(ALUOp.AND)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      dut.io.operandA.poke(0xFFFFFFFFL.U)
      dut.io.operandB.poke(0xA5A5A5A5L.U)
      dut.io.operation.poke(ALUOp.AND)
      dut.io.aluResult.expect(0xA5A5A5A5L.U)
      dut.clock.step(1)

      // AND with zero → 0
      dut.io.operandA.poke(0xDEADBEEFL.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.AND)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // AND with self → self
      dut.io.operandA.poke(0x12345678.U)
      dut.io.operandB.poke(0x12345678.U)
      dut.io.operation.poke(ALUOp.AND)
      dut.io.aluResult.expect(0x12345678.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// OR
// ============================================================
class ALUOrTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Or_Tester" should "test OR operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.operandA.poke(0xFFFF0000L.U)
      dut.io.operandB.poke(0x0000FFFFL.U)
      dut.io.operation.poke(ALUOp.OR)
      dut.io.aluResult.expect(0xFFFFFFFFL.U)
      dut.clock.step(1)

      // OR with zero → self
      dut.io.operandA.poke(0xCAFEBABEL.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.OR)
      dut.io.aluResult.expect(0xCAFEBABEL.U)
      dut.clock.step(1)

      // OR with all-ones → all-ones
      dut.io.operandA.poke(0.U)
      dut.io.operandB.poke(0xFFFFFFFFL.U)
      dut.io.operation.poke(ALUOp.OR)
      dut.io.aluResult.expect(0xFFFFFFFFL.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// XOR
// ============================================================
class ALUXorTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Xor_Tester" should "test XOR operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // XOR with self → 0
      dut.io.operandA.poke(0xDEADBEEFL.U)
      dut.io.operandB.poke(0xDEADBEEFL.U)
      dut.io.operation.poke(ALUOp.XOR)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // XOR with all-ones → bitwise NOT
      dut.io.operandA.poke(0x0F0F0F0F.U)
      dut.io.operandB.poke(0xFFFFFFFFL.U)
      dut.io.operation.poke(ALUOp.XOR)
      dut.io.aluResult.expect(0xF0F0F0F0L.U)
      dut.clock.step(1)

      // XOR with zero → self
      dut.io.operandA.poke(0x12345678.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.XOR)
      dut.io.aluResult.expect(0x12345678.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// SLL  (Shift Left Logical)
// ============================================================
class ALUSllTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Sll_Tester" should "test SLL operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // 1 << 4 = 16
      dut.io.operandA.poke(1.U)
      dut.io.operandB.poke(4.U)
      dut.io.operation.poke(ALUOp.SLL)
      dut.io.aluResult.expect(16.U)
      dut.clock.step(1)

      // Shift by 0 → unchanged
      dut.io.operandA.poke(0xABCDEF01L.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.SLL)
      dut.io.aluResult.expect(0xABCDEF01L.U)
      dut.clock.step(1)

      // Only lower 5 bits of operandB used: shamt = 1 (33 mod 32)
      dut.io.operandA.poke(1.U)
      dut.io.operandB.poke(33.U)   // lower 5 bits = 1
      dut.io.operation.poke(ALUOp.SLL)
      dut.io.aluResult.expect(2.U)
      dut.clock.step(1)

      // Shift all the way: top bit falls off
      dut.io.operandA.poke(0x80000000L.U)
      dut.io.operandB.poke(1.U)
      dut.io.operation.poke(ALUOp.SLL)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// SRL  (Shift Right Logical)
// ============================================================
class ALUSrlTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Srl_Tester" should "test SRL operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // 16 >> 2 = 4
      dut.io.operandA.poke(16.U)
      dut.io.operandB.poke(2.U)
      dut.io.operation.poke(ALUOp.SRL)
      dut.io.aluResult.expect(4.U)
      dut.clock.step(1)

      // MSB is NOT sign-extended (logical shift)
      dut.io.operandA.poke(0x80000000L.U)
      dut.io.operandB.poke(1.U)
      dut.io.operation.poke(ALUOp.SRL)
      dut.io.aluResult.expect(0x40000000L.U)
      dut.clock.step(1)

      // Shift by 0 → unchanged
      dut.io.operandA.poke(0xDEADBEEFL.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.SRL)
      dut.io.aluResult.expect(0xDEADBEEFL.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// SRA  (Shift Right Arithmetic)
// ============================================================
class ALUSraTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Sra_Tester" should "test SRA operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // Positive: sign bit stays 0
      dut.io.operandA.poke(16.U)
      dut.io.operandB.poke(2.U)
      dut.io.operation.poke(ALUOp.SRA)
      dut.io.aluResult.expect(4.U)
      dut.clock.step(1)

      // Negative number (MSB=1): sign bit IS extended
      // 0x80000000 >> 1 = 0xC0000000 (arithmetic)
      dut.io.operandA.poke(0x80000000L.U)
      dut.io.operandB.poke(1.U)
      dut.io.operation.poke(ALUOp.SRA)
      dut.io.aluResult.expect(0xC0000000L.U)
      dut.clock.step(1)

      // Shift -1 (0xFFFFFFFF) by any amount → still 0xFFFFFFFF
      dut.io.operandA.poke(0xFFFFFFFFL.U)
      dut.io.operandB.poke(15.U)
      dut.io.operation.poke(ALUOp.SRA)
      dut.io.aluResult.expect(0xFFFFFFFFL.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// SLT  (Set Less Than – signed)
// ============================================================
class ALUSltTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_Slt_Tester" should "test SLT operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // 5 < 10 → 1
      dut.io.operandA.poke(5.U)
      dut.io.operandB.poke(10.U)
      dut.io.operation.poke(ALUOp.SLT)
      dut.io.aluResult.expect(1.U)
      dut.clock.step(1)

      // 10 < 5 → 0
      dut.io.operandA.poke(10.U)
      dut.io.operandB.poke(5.U)
      dut.io.operation.poke(ALUOp.SLT)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // Equal → 0
      dut.io.operandA.poke(7.U)
      dut.io.operandB.poke(7.U)
      dut.io.operation.poke(ALUOp.SLT)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // -1 (0xFFFFFFFF) < 0 in signed → 1
      dut.io.operandA.poke(0xFFFFFFFFL.U)   // -1 signed
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.SLT)
      dut.io.aluResult.expect(1.U)
      dut.clock.step(1)

      // 0 < -1 (signed) → 0
      dut.io.operandA.poke(0.U)
      dut.io.operandB.poke(0xFFFFFFFFL.U)   // -1 signed
      dut.io.operation.poke(ALUOp.SLT)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// SLTU  (Set Less Than Unsigned)
// ============================================================
class ALUSltUTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_SltU_Tester" should "test SLTU operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // 5 < 10 unsigned → 1
      dut.io.operandA.poke(5.U)
      dut.io.operandB.poke(10.U)
      dut.io.operation.poke(ALUOp.SLTU)
      dut.io.aluResult.expect(1.U)
      dut.clock.step(1)

      // 0xFFFFFFFF is the largest unsigned 32-bit value.
      // 0 < 0xFFFFFFFF unsigned → 1
      dut.io.operandA.poke(0.U)
      dut.io.operandB.poke(0xFFFFFFFFL.U)
      dut.io.operation.poke(ALUOp.SLTU)
      dut.io.aluResult.expect(1.U)
      dut.clock.step(1)

      // 0xFFFFFFFF < 0 unsigned → 0
      dut.io.operandA.poke(0xFFFFFFFFL.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.SLTU)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      // Equal → 0
      dut.io.operandA.poke(42.U)
      dut.io.operandB.poke(42.U)
      dut.io.operation.poke(ALUOp.SLTU)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)
    }
  }
}

// ============================================================
// PASSB
// ============================================================
class ALUPassBTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU_PassB_Tester" should "test PASSB operation" in {
    test(new ALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      // operandA is irrelevant; output must equal operandB
      dut.io.operandA.poke(0xDEADBEEFL.U)
      dut.io.operandB.poke(0x12345678.U)
      dut.io.operation.poke(ALUOp.PASSB)
      dut.io.aluResult.expect(0x12345678.U)
      dut.clock.step(1)

      dut.io.operandA.poke(0xFFFFFFFFL.U)
      dut.io.operandB.poke(0.U)
      dut.io.operation.poke(ALUOp.PASSB)
      dut.io.aluResult.expect(0.U)
      dut.clock.step(1)

      dut.io.operandA.poke(0.U)
      dut.io.operandB.poke(0xFFFFFFFFL.U)
      dut.io.operation.poke(ALUOp.PASSB)
      dut.io.aluResult.expect(0xFFFFFFFFL.U)
      dut.clock.step(1)
    }
  }
}
