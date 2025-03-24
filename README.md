# SPI Master

A SPI Master IP Core implemented in Chisel

It supports:

- **Full-duplex SPI communication** (send & receive) (WIP)
- **SPI modes 0, 1, 2, 3** (configurable CPOL & CPHA) (WIP)
- **Configurable clock divider**
- **Verilog generation**
- **Bus Integration with AXI, AHB, Wishbone, as well as standlone module** (WIP)
  - DMA Support for high-speed SPI transfers
- **Interrupts for transaction complete notifications** (WIP)
- **Multiple slave support with multiple chip select lines** (WIP)

## Project Structure

```sh
 SPI-Master
 ├─ project/                # sbt project settings
 ├─ build.sbt               # sbt build script
 ├─ build.mill              # mill build script
 ├─ src
 │  ├─ main/scala/spi       # Chisel sources
 │  └─ test/scala/spi       # Chisel tests
 └─ verilator               # Verilator tests
```

## Dependencies

### JDK 11 or newer

We recommend using Java 11 or later LTS releases. While Chisel itself works with
Java 8, our preferred build tool Mill requires Java 11. You can install the JDK
as your operating system recommends, or use the prebuilt binaries from
[Adoptium](https://adoptium.net/) (formerly AdoptOpenJDK).

### SBT or mill

SBT is the most common build tool in the Scala community. You can download
it [here](https://www.scala-sbt.org/download.html).  
mill is another Scala/Java build tool without obscure DSL like SBT. You can
download it [here](https://github.com/com-lihaoyi/mill/releases)

### Verilator

The test with `svsim` needs Verilator installed.
See Verilator installation instructions [here](https://verilator.org/guide/latest/install.html).

## Generation

Using mill

```sh
mill SPI-Master
```

This will generate a SPI-Master.sv module for use in your designs

## Testing

You can run the included tests with

```sh
sbt test
```

Alternatively, if you use Mill:

```sh
mill SPI-Master.test
```

### Verilator Tests

To run the verilator tests after verilog generation

```sh
verilator --cc SPIMaster.sv
verilator --trace -cc SPIMaster.sv --exe verilator/<test bench name>
make -C obj_dir -f VSPIMaster.mk VSPIMaster
./VSPIMaster
```

To view waveforms

```sh
gtkwave <waveform file>.vcd
```

## License

Licensed with the MIT License
