# FPGA Cosmac ELF
This re-creation of a Cosmac ELF computer, Coded in SpinalHDL.

The goal of this project is to end up with a cycle-accurate 1802 processor.
When I start the project I was new to SpinalHDL but I had attempted in the past to write the same processor in VHDL.
However, language complexity and the sheer amount of code and time needed to write and debug that project ended the work on it.

To make verification of the CPU easier, I made the simulation capable of reading the [Emma](https://www.emma02.hobby-site.com/) debug trace log format.
The theory is that the CPU should follow that same path as the debug log from Emma.  
If the Address or D register doesn't match the one in the log, then there is an issue and it will halt the simulation at that point.
Using [GTKWave](http://gtkwave.sourceforge.net/) you can view the output of the simulation and debug the issue.

The test program included uses the Floating Point Subroutines by Paul Wasserman.
I copied the subroutine listings by hand into a hex editor and checked them by overlaying an image of the hex code I copied, with the scanned images.
Included is a copy of the scans, and a copy of the binary file for the subroutines.  Along with the assemble for the program used to test the CPU.

If you would like to learn more about the 1802 processor here's a video about the designer [Josh Bensadon](https://www.youtube.com/watch?v=xwUrGlYN8eo), For information about the [Cosmac Elf](https://en.wikipedia.org/wiki/COSMAC_ELF) computer there's the wiki page.

#### Way build an RCA1802 processor
Josh Bensadon had a goal when designing computers, and that was they had to be fun.
The computer that led to the development of 1802 processor was called FRED.
<br/>[<img src="https://cdn.discordapp.com/attachments/664986544284631040/666853909029060618/unknown.png" width="200" />](https://cdn.discordapp.com/attachments/664986544284631040/666853909029060618/unknown.png)
[<img src="https://cdn.discordapp.com/attachments/664986544284631040/666848639355715587/unknown.png" width="200" />](https://cdn.discordapp.com/attachments/664986544284631040/666848639355715587/unknown.png)<br/><br/>
The assembly language for the 1802 process is easy to understand, and the data flow of the cpu is easy to follow.
<br/>[<img src="https://cdn.discordapp.com/attachments/664986544284631040/666855126354624522/unknown.png" width="200" />](https://cdn.discordapp.com/attachments/664986544284631040/666855126354624522/unknown.png)<br><br>
Using SpinalHDL made this project fun to work on.
The Verilog code genrated by SpinalHDL for just the cpu is 1500 lines, and cpu1802.scala file is 700 lines.
I highly suggest installing and playing with SpinalHDL, it very powerful and the code is maintainable and reusable, and just works without the headaches. 
  
#### Features:
* Able to load programs over the serial interface
* Processor can send serial data with opcode OUT 1 and can check if the serial FIFO is full with opcode BN1  

#### How to set up your project:
1. You will need to have [SpinalHDL](https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Getting%20Started/getting_started.html) 
setup and working on your system to generate the HDL code.
2. For simulation, you will need to have [Verilator](https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Simulation/install.html)
setup.
3. In your FPGA project, you will need to generate a PPL with a clock out of 8mhz to have the serial baud rate be 115200
4. You will also need to generate a block ram.
5. You may have to edit the BlackBoxes for the PLL and Ram to match your targets.
* or you can just generate the CPU core by itself to get the HDL/Verilog.
* or copy the cpu1802.scala file to your own SpinalHDL project.

#### TODOs:
* Finish interrupt cycle.
* Add support to allow CPU access to the RX serial data.   

## Pictures
![alt text](https://cdn.discordapp.com/attachments/664986544284631040/666808880688398336/gtkwave_dtyV29rqxF.webp "GTKWave Showing Timings")

![alt text](https://cdn.discordapp.com/attachments/664986544284631040/666808588471369729/IMG_20200114_165614.webp "Target FPGA Board")
