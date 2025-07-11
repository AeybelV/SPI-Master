#include "VSPIMaster.h"
#include <iostream>
#include <random>
#include <verilated.h>
#include <verilated_vcd_c.h>

vluint64_t sim_time = 0;

double sc_time_stamp()
{
    return sim_time;
}

#define TRACE_DEPTH 99
#define MAX_SIM_TIME 10000
#define RANDOM_TESTS 25
#define TEST_DELAY 25

void toggle_clock(VSPIMaster *dut, VerilatedVcdC *m_trace)
{
    // Falling edge
    dut->clock = 0;
    dut->eval();
    if (m_trace)
        m_trace->dump(sim_time);
    sim_time++;

    // Rising edge
    dut->clock = 1;
    dut->eval();
    if (m_trace)
        m_trace->dump(sim_time);
    sim_time++;
}

int simulate_spi_transaction(VSPIMaster *dut, VerilatedVcdC *m_trace, int MOSI, int MISO)
{
    // Expected data to send to slave
    uint8_t master_tx = MOSI;
    dut->io_dataIn = master_tx;

    // Data slave will return on MISO
    uint8_t slave_tx = MISO;
    int slave_bit_idx = 7;

    std::cout << "===============" << std::endl;
    std::cout << "Transmiting Master --> Slave: 0x" << std::hex << MOSI << std::endl;
    std::cout << "Transmiting Slave --> Master: 0x" << std::hex << MISO << std::endl;

    // ========== Start transaction ==========
    dut->io_start = 1;
    toggle_clock(dut, m_trace);
    dut->io_start = 0;

    // To track bits received by slave
    uint8_t slave_received = 0;

    bool prev_sclk = 0;
    int status = 0;

    // Run until master signals done
    while (!dut->io_done)
    {
        if (sim_time > MAX_SIM_TIME)
            break;

        bool sclk_rising = (!prev_sclk && dut->io_spi_sclk);

        // On SCLK rising edge: sample MOSI as slave
        if (sclk_rising && slave_bit_idx >= 0)
        {
            // Read current bit on MOSI
            int mosi_bit = dut->io_spi_mosi;
            slave_received = (slave_received << 1) | mosi_bit;

            slave_bit_idx--;

            if (slave_bit_idx >= 0)
                // Provide MISO bit to master
                dut->io_spi_miso = (slave_tx >> slave_bit_idx) & 1;
        }

        prev_sclk = dut->io_spi_sclk;
        toggle_clock(dut, m_trace);
    }

    // Checks whether the SPI Master was able to succesfully send data over MOSI to simulated slave
    if (slave_received == master_tx)
    {
        std::cout << "✅ MOSI data matched expected!\n";
    }
    else
    {
        status = 1;
        std::cout << "❌ MOSI data mismatch!\n";
        std::cout << "\tMaster sent (MOSI): 0x" << std::hex << (int)slave_received << std::endl;
    }

    // Checks whether the master received data on MISO from slave
    if (dut->io_dataOut == slave_tx)
    {
        std::cout << "✅ Master received correct MISO data!\n";
    }
    else
    {
        status = 1;
        std::cout << "❌ Master received incorrect MISO data!\n";
        std::cout << "\tMaster received (dataOut): 0x" << std::hex << (int)dut->io_dataOut << std::endl;
    }

    for (int i = 0; i < TEST_DELAY; i++)
        toggle_clock(dut, m_trace);

    return status;
}

int main(int argc, char **argv)
{
    int failedTests = 0;
    Verilated::commandArgs(argc, argv);
    VSPIMaster *dut = new VSPIMaster;

    // Enable VCD (waveform) output
    Verilated::traceEverOn(true);
    VerilatedVcdC *m_trace = new VerilatedVcdC;
    dut->trace(m_trace, TRACE_DEPTH);
    m_trace->open("spi_master_waveform.vcd");

    // ========== Initial reset ==========
    dut->reset = 1;
    dut->io_dataIn = 0;
    dut->io_start = 0;
    dut->io_spi_miso = 0;

    for (int i = 0; i < TEST_DELAY; ++i)
    {
        toggle_clock(dut, m_trace);
    }

    dut->reset = 0;

    // Fixed tests
    failedTests += simulate_spi_transaction(dut, m_trace, 0xA5, 0x3C);
    failedTests += simulate_spi_transaction(dut, m_trace, 0xAA, 0xBB);

    // Random tests
    std::random_device rd;
    std::mt19937 gen(rd());

    std::uniform_int_distribution<> dis(0, 255);
    for (int i = 0; i < RANDOM_TESTS; i++)
    {
        uint8_t tx = static_cast<uint8_t>(dis(gen));
        uint8_t rx = static_cast<uint8_t>(dis(gen));

        failedTests += simulate_spi_transaction(dut, m_trace, tx, rx);
    }

    // Cleanup
    dut->final();
    m_trace->close();
    delete m_trace;
    delete dut;
    return failedTests;
}
