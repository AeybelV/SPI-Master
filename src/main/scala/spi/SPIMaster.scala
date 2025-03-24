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
    val mode = Input(UInt(2.W)) // SPI Mode (CPOL, CPHA)

    // ransmit and Receive data
    val tx_data = Flipped(Decoupled(UInt(dataWidth.W)))
    val rx_data = Decoupled(UInt(dataWidth.W))

    val busy = Output(Bool())
  })

  // States
  val idle :: transfer :: done :: Nil = Enum(3)
  val state = RegInit(idle)

  val shiftReg = Reg(UInt(dataWidth.W))
  val bitCnt = RegInit(0.U(log2Ceil(dataWidth).W))
  val rxShiftReg = Reg(UInt(dataWidth.W))
  val clkDivCnt = RegInit(0.U(log2Ceil(clkDiv).W))
  val spiClkEn = RegInit(false.B)

  val sclkReg = RegInit(false.B)
  val csReg = RegInit(true.B)

  val cpol = io.mode(0)
  val cpha = io.mode(1)

  io.spi.sclk := sclkReg
  io.spi.cs := csReg
  io.spi.mosi := shiftReg(dataWidth - 1)
  io.busy := (state =/= idle)
  // io.rx_data.bi := rxShiftReg
  io.tx_data.ready := 0.B
  io.rx_data.valid := 0.B
  io.rx_data.bits := 0.U

  // SPI FSM
  switch(state) {
    is(idle) {
      csReg := true.B // Deactivate chip select
      sclkReg := cpol // Set clock polarity
      io.tx_data.ready := 1.B

      when(io.tx_data.valid & io.tx_data.ready) {
        csReg := false.B // Activate chip select
        shiftReg := io.tx_data.bits
        bitCnt := (dataWidth - 1).U
        state := transfer
      }
    }
    is(transfer) {

      when(clkDivCnt === (clkDiv - 1).U) {
        sclkReg := ~sclkReg
        csReg := false.B
        clkDivCnt := 0.U
        when(sclkReg === !cpol) {

          io.tx_data.ready := 1.B
          shiftReg := shiftReg << 1
          when(bitCnt === 0.U) {
            state := done
          }.otherwise {

            bitCnt := bitCnt - 1.U
          }
        }
      }.otherwise {
        clkDivCnt := clkDivCnt + 1.U
      }

    }
    is(done) {
      csReg := true.B // Deactivate chip select
      io.tx_data.ready := 0.U
      state := idle
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
