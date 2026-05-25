// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/** 4-bit adder tester
  *
  * Truth tables are not very efficient for testing more complex components, as they grow
  * exponentially with the number of input bits. Therefore, we have to think of a more clever way to
  * test the 4-bit adder. To test the Basic Adder design in our Chisel Introduction, we used loops
  * to generate a sequence of increasing input values testing the design. To generate test cases for
  * the 4-bit adder, you should also start by using two nested loops. To determine the borders of
  * the loop counter, think about the lowest and the highest unsignes integer that you can represent
  * with four bit. To test the result produced by your design, think about what happens to the
  * result in case of an overflow and at which point this can happen. Hint: It might be helpful to
  * check the expected output behaviour for two different scenarios with the help of a condition.
  */
class FourBitAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "4-bit Adder" should "work" in {
    test(new FourBitAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      /*
       * TODO: Insert your test cases
       */
      // println("Starting 4-bit adder test...")
      for (a <- 0 to 15) {
        for (b <- 0 to 15) {
            val result = a + b

            val sResult = result % 16
            // If the true result is greater than 15, we have an overflow (Carry Out = 1)
            val cResult = if (result > 15) 1 else 0
            // println(s"Testing a=$a, b=$b, expecting s=$sResult and co=$cResult")

            dut.io.aInput.poke(a.U)
            dut.io.bInput.poke(b.U)

            dut.clock.step(1)

            dut.io.sOutput.expect(sResult.U)
            dut.io.cOutput.expect(cResult.B)
        }
      }

    }
  }
}
