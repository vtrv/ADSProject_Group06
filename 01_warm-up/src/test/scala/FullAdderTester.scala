// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/** Full adder tester Use the truth table from the exercise sheet to test all possible input
  * combinations and the corresponding results exhaustively
  */
class FullAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "FullAdder" should "work" in {
    test(new FullAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      /*dut.io.a.poke(...)
       *dut.io.b.poke(...)
       *dut.io.ci.poke(...)
       *dut.io.s.expect(...)
       *dut.io.co.expect(...)
       *...
       *TODO: Insert your test cases
       */
      for (a <- 0 to 1) {
        for (b <- 0 to 1) {
          for (ci <- 0 to 1) {
            val result = a + b + ci
            val sResult = result % 2
            val cResult = result / 2
            // println(s"Testing a=$a, b=$b, ci=$ci, expecting s=$sResult and co=$cResult")

            dut.io.aInput.poke(a.B)
            dut.io.bInput.poke(b.B)
            dut.io.cInput.poke(ci.B)
            dut.clock.step(1)
            dut.io.sOutput.expect(sResult.B)
            dut.io.cOutput.expect(cResult.B)
          }
        }
      }
    }
  }
}
