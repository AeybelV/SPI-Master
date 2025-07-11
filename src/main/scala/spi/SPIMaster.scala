// See LICENSE for license details

package spi

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class SPIBundle extends Bundle {
  val mosi = Output(Bool())
  val miso = Input(Bool())
  val sclk = Output(Bool())
  val cs = Output(Bool())
}

class SPIMaster(val dataWidth: Int = 8, val clkDiv: Int = 4) extends Module {
  val io = IO(new Bundle {
    val spi = new SPIBundle() // SPI Lines
    val dataIn = Input(UInt(8.W)) // Byte to send
    val start = Input(Bool())
    val busy = Output(Bool())
    val dataOut = Output(UInt(8.W)) // Received data
    val done = Output(Bool())
  })

  // Clock divider for SCLK generation
  val clkCnt = RegInit(0.U(log2Ceil(clkDiv).W))
  val sclkReg = RegInit(false.B)

  // Bit counters
  val bitCnt = RegInit(0.U(3.W)) // 0 to 7
  val shiftRegOut = Reg(UInt(8.W))
  val shiftRegIn = Reg(UInt(8.W))

  val active = RegInit(false.B)

  // Initialize SPI lines
  io.spi.sclk := sclkReg
  io.spi.mosi := shiftRegOut(7)
  io.spi.cs := !active

  // Initialize module flags and data i/o
  io.done := false.B
  io.busy := active
  io.dataOut := shiftRegIn

  // When instructed to start operation
  when(io.start && !active) {
    active := true.B
    clkCnt := 0.U
    sclkReg := false.B
    bitCnt := 0.U
    shiftRegOut := io.dataIn
    shiftRegIn := io.spi.miso
  }

  // During a active transmission or operation
  when(active) {
    clkCnt := clkCnt + 1.U
    when(clkCnt === (clkDiv - 1).U) {
      clkCnt := 0.U
      sclkReg := ~sclkReg

      when(sclkReg) {
        // On rising edge of SCLK: capture MISO
        shiftRegIn := Cat(shiftRegIn(6, 0), io.spi.miso)

        when(bitCnt === 7.U) {
          active := false.B
          io.done := true.B
        }.otherwise {
          bitCnt := bitCnt + 1.U
          shiftRegOut := Cat(shiftRegOut(6, 0), 0.U(1.W))
        }
      }
    }
  }
}

/** Generate Verilog sources and save it in file SPIMaster.sv
  */
object SPIMaster extends App {
  ChiselStage.emitSystemVerilogFile(
    new SPIMaster,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
