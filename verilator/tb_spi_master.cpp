#include "VSPIMaster.h"
#include <iostream>
#include <verilated.h>
#include <verilated_vcd_c.h>

#define TRACE_DEPTH 99
#define MAX_SIM_TIME 1000

int main(int argc, char **argv)
{
    VSPIMaster *dut = new VSPIMaster;

    // Enable VCD (waveform) output
    Verilated::traceEverOn(true);
    VerilatedVcdC *m_trace = new VerilatedVcdC;
    dut->trace(m_trace, TRACE_DEPTH);
    m_trace->open("spi_master_waveform.vcd");

    // Reset
    dut->reset = 1;

    // Configure SPI
    dut->io_mode = 0b00; // SPI Mode 0

    // Send Data (0xA5)
    dut->io_tx_data_valid = 1;
    dut->io_tx_data_bits = 0xA5;

    dut->eval();
    m_trace->dump(0);
    dut->reset = 0;

    vluint64_t sim_time = 1;
    while (sim_time < MAX_SIM_TIME)
    {
        if (sim_time > 2)
        {
            dut->io_tx_data_valid = 0;
        }
        dut->clock ^= 1;
        dut->eval();
        m_trace->dump(sim_time);
        sim_time++;
    }
    dut->io_tx_data_valid = 0;

    // Cleanup
    dut->final();
    m_trace->close();
    delete dut;
    return 0;
}
