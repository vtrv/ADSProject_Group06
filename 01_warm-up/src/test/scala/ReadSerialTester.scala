// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/** read serial tester
  */
class ReadSerialTester extends AnyFlatSpec with ChiselScalatestTester {
  /*dut.io.rxd.poke(...)
   *dut.clock.step(...)
   *dut.io.valid.expect(...)
   *dut.io.data.expect("b11111111".U)
   *...
   *TODO: Add your testcases here
   */

  private def getBit(value: Int, bitIndex: Int) = ((value >> bitIndex) & 1) == 1

  private def pokeRxdBit(dut: ReadSerial, bit: Boolean): Unit =
    dut.io.rxdInput.poke(bit.B)

  /** Holds the line idle for [cycles] clock steps and verifies valid stays low. */
  private def idleBus(dut: ReadSerial, cycles: Int = 5): Unit = {
    pokeRxdBit(dut, bit = true)
    dut.clock.step(cycles)
    dut.io.validOutput.expect(false.B)
  }

  /** Sends one complete frame (start bit + 8 data bits) and asserts that:
    *   - valid stays low throughout reception
    *   - valid pulses high with the correct data after the last bit
    * Does NOT drive the line idle afterward
    */
  private def sendFrame(dut: ReadSerial, byte: Int): Unit = {
    // Start bit
    pokeRxdBit(dut, bit = false)
    dut.clock.step(1)
    dut.io.validOutput.expect(false.B)

    // 8 data bits, MSB first
    for (i <- 7 to 0 by -1) {
      pokeRxdBit(dut, getBit(byte, i))
      dut.clock.step(1)
      if (i != 0) {
        dut.io.validOutput.expect(false.B)
      }
    }

    // After the last data bit has been clocked in, check valid and data output
    dut.io.validOutput.expect(true.B)
    dut.io.dataOutput.expect(byte.U)
  }

  "ReadSerial" should "receive a single frame correctly" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      idleBus(dut)

      val patterns = Seq(0x00, 0xff, 0xa5, 0x3c, 0x81)
      patterns.foreach { b =>
        sendFrame(dut, b)
        idleBus(dut) // inter-frame gap;
      }
    }
  }

  it should "handle back-to-back frames" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      idleBus(dut)

      val bytes = Seq(0x12, 0x34, 0xab, 0xcd)

      // start the next frame immediately after the first one without idling the bus
      bytes.foreach(sendFrame(dut, _))

      idleBus(dut, cycles = 1) // verify valid de-asserts after the last frame
    }
  }
}
