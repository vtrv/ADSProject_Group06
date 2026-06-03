// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chisel3.util._

/** Half Adder Class
  *
  * Your task is to implement a basic half adder as presented in the lecture. Each signal should
  * only be one bit wide (inputs and outputs). There should be no delay between input and output
  * signals, we want to have a combinational behaviour of the component.
  */
class HalfAdder extends Module {

  val io = IO(new Bundle {
    /* ``
     * TODO: Define IO ports of a half adder as presented in the lecture
     */
    val aInput = Input(Bool())
    val bInput = Input(Bool())
    val sOutput = Output(Bool())
    val cOutput = Output(Bool())
  })

  /*
   * TODO: Describe output behaviour based on the input values
   */
  io.sOutput := io.aInput ^ io.bInput
  io.cOutput := io.aInput & io.bInput
}

/** Full Adder Class
  *
  * Your task is to implement a basic full adder. The component's behaviour should match the
  * characteristics presented in the lecture. In addition, you are only allowed to use two half
  * adders (use the class that you already implemented) and basic logic operators (AND, OR, ...).
  * Each signal should only be one bit wide (inputs and outputs). There should be no delay between
  * input and output signals, we want to have a combinational behaviour of the component.
  */
class FullAdder extends Module {

  val io = IO(new Bundle {
    /*
     * TODO: Define IO ports of a half adder as presented in the lecture
     */
    val aInput = Input(Bool())
    val bInput = Input(Bool())
    val cInput = Input(Bool())
    val sOutput = Output(Bool())
    val cOutput = Output(Bool())
  })

  /*
   * TODO: Instanciate the two half adders you want to use based on your HalfAdder class
   */
  val halfAdder1 = Module(new HalfAdder())
  val halfAdder2 = Module(new HalfAdder())

  /*
   * TODO: Describe output behaviour based on the input values and the internal signals
   */

  // calculate a + b
  halfAdder1.io.aInput := io.aInput
  halfAdder1.io.bInput := io.bInput

  // calculate a + b + ci
  halfAdder2.io.aInput := halfAdder1.io.sOutput
  halfAdder2.io.bInput := io.cInput

  // wire s and carry output
  io.sOutput := halfAdder2.io.sOutput
  io.cOutput := halfAdder1.io.cOutput | halfAdder2.io.cOutput
}

/** 4-bit Adder class
  *
  * Your task is to implement a 4-bit ripple-carry-adder. The component's behaviour should match the
  * characteristics presented in the lecture. Remember: An n-bit adder can be build using one half
  * adder and n-1 full adders. The inputs and the result should all be 4-bit wide, the carry-out
  * only needs one bit. There should be no delay between input and output signals, we want to have a
  * combinational behaviour of the component.
  */
class FourBitAdder extends Module {

  val io = IO(new Bundle {
    /*
     * TODO: Define IO ports of a 4-bit ripple-carry-adder as presented in the lecture
     */
    val aInput = Input(UInt(4.W))
    val bInput = Input(UInt(4.W))
    val sOutput = Output(UInt(4.W))
    val cOutput = Output(Bool())
  })

  /*
   * TODO: Instantiate the full adders and one half adder based on the previously defined classes
   */

  val halfAdder = Module(new HalfAdder())
  val fullAdders = Seq.fill(3) { Module(new FullAdder()) }

  /*
   * TODO: Describe output behaviour based on the input values and the internal
   */

  halfAdder.io.aInput := io.aInput(0)
  halfAdder.io.bInput := io.bInput(0)

  for (i <- 0 until 3) {
    // ith full adder => (i+1)th bit of a and b
    fullAdders(i).io.aInput := io.aInput(i + 1)
    fullAdders(i).io.bInput := io.bInput(i + 1)

    // c0 -> half adder, ci -> full adders
    if (i == 0) {
      fullAdders(i).io.cInput := halfAdder.io.cOutput
    } else {
      fullAdders(i).io.cInput := fullAdders(i - 1).io.cOutput
    }
  }

  // concatenate out bits to 4-bit output
  io.sOutput := Cat(
    fullAdders(2).io.sOutput, // Bit 3
    fullAdders(1).io.sOutput, // Bit 2
    fullAdders(0).io.sOutput, // Bit 1
    halfAdder.io.sOutput // Bit 0
  )

  io.cOutput := fullAdders(2).io.cOutput

}
