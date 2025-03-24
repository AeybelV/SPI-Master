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

      dut.io.mode.poke(0.U) // SPI Mode 0 (CPOL=0, CPHA=0)

      // Provider test input data to SPI TX
      dut.io.tx_data.bits.poke(0xa5.U)
      dut.io.tx_data.valid.poke(true.B)
      dut.clock.step()
      dut.io.tx_data.valid.poke(false.B)
      dut.clock.step()

    }
  }
}
