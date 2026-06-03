// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

/** controller class */
class Controller extends Module {

  val io = IO(new Bundle {
    /*
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    val rxdInput = Input(Bool())
    val cntStInput = Input(Bool())
    val cntEnOutput = Output(Bool())
    val validOutput = Output(Bool())
  })

  // internal variables
  /*
   * TODO: Define internal variables (registers and/or wires), if needed
   */
  object State extends ChiselEnum {
    val sIdle, sCount = Value
  }

  val stateReg = RegInit(State.sIdle)
  val validReg = RegInit(false.B)

  // state machine
  /*
   * TODO: Describe functionality if the controller as a state machine
   */

  io.cntEnOutput := false.B
  io.validOutput := validReg
  validReg := false.B // one-cycle pulse when set

  switch(stateReg) {
    is(State.sIdle) {
      // if rxd is 0, then start counting
      when(io.rxdInput === false.B) {
        stateReg := State.sCount
      }
    }
    is(State.sCount) {
      // enable counting and wait until cntStInput is true, then go back to idle state
      io.cntEnOutput := true.B
      when(io.cntStInput === true.B) {
        // Pulse valid for one cycle after the 8th data bit has been shifted in.
        validReg := true.B
        // Always return to idle; a back-to-back start bit will be detected there.
        stateReg := State.sIdle
      }
    }
  }

}

/** counter class */
class Counter extends Module {

  val io = IO(new Bundle {
    /*
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    val cntEnInput = Input(Bool())
    val cntStOutput = Output(Bool())
  })

  // internal variables
  /*
   * TODO: Define internal variables (registers and/or wires), if needed
   */
  val cntReg = RegInit(0.U(3.W))

  // state machine
  /*
   * TODO: Describe functionality if the counter as a state machine
   */

  when(io.cntEnInput === true.B) {
    cntReg := cntReg + 1.U
  }.otherwise {
    cntReg := 0.U
  }

  when(cntReg === 7.U) {
    io.cntStOutput := true.B // Tell the controller we are done
  }.otherwise {
    io.cntStOutput := false.B
  }

}

/** shift register class */
class ShiftRegister extends Module {

  val io = IO(new Bundle {
    /*
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    val rxdInput = Input(Bool())
    val dataOutput = Output(UInt(8.W))
  })

  // internal variables
  /*
   * TODO: Define internal variables (registers and/or wires), if needed
   */
  val shiftReg = RegInit(0.U(8.W))

  // functionality
  /*
   * TODO: Describe functionality if the shift register
   */
  // rxd starts with the MSB, so shift left and add new bit at the end
  shiftReg := Cat(shiftReg(6, 0), io.rxdInput)

  io.dataOutput := shiftReg
}

/** The last warm-up task deals with a more complex component. Your goal is to design a serial
  * receiver. It scans an input line (“serial bus”) named rxd for serial transmissions of data
  * bytes. A transmission begins with a start bit ‘0’ followed by 8 data bits. The most significant
  * bit (MSB) is transmitted first. There is no parity bit and no stop bit. After the last data bit
  * has been transferred a new transmission (beginning with a start bit, ‘0’) may immediately
  * follow. If there is no new transmission the bus line goes high (‘1’, this is considered the
  * “idle” bus signal). In this case the receiver waits until the next transmission begins. The
  * outputs of the design are an 8-bit parallel data signal and a valid signal. The valid signal
  * goes high (‘1’) for one clock cycle after the last serial bit has been transmitted, indicating
  * that a new data byte is ready.
  */
class ReadSerial extends Module {

  val io = IO(new Bundle {
    /*
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    val rxdInput = Input(Bool())
    val dataOutput = Output(UInt(8.W))
    val validOutput = Output(Bool())
  })

  // instanciation of modules
  /*
   * TODO: Instanciate the modules that you need
   */
  val controller = Module(new Controller())
  val counter = Module(new Counter())
  val shiftRegister = Module(new ShiftRegister())

  // connections between modules
  /*
   * TODO: connect the signals between the modules
   */
  controller.io.rxdInput := io.rxdInput
  counter.io.cntEnInput := controller.io.cntEnOutput
  shiftRegister.io.rxdInput := io.rxdInput
  controller.io.cntStInput := counter.io.cntStOutput

  // global I/O
  /*
   * TODO: Describe output behaviour based on the input values and the internal signals
   */
  io.dataOutput := shiftRegister.io.dataOutput
  io.validOutput := controller.io.validOutput

}
