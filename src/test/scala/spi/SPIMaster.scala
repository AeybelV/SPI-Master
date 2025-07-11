// See README.md for license details.

package spi
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.experimental.BundleLiterals._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class SPIMasterTest extends AnyFreeSpec with Matchers {

  "SPI Master to test transmit and receive" in {
    simulate(new SPIMaster()) { dut =>

      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      dut.io.dataIn.poke(0xa5.U)
      dut.io.start.poke(true.B)
      dut.clock.step(1)
      dut.io.start.poke(false.B)

      while (dut.io.busy.peek().litToBoolean) {
        // note: could drie miso here
        dut.clock.step(1)
      }

      println(s"Received: ${dut.io.dataOut.peek().litValue}")
    }
  }
}
