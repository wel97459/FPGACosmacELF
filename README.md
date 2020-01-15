# FPGA Cosmac ELF
This re-creation of a Cosmac ELF computer, Coded in SpinalHDL.

The goal of this project is to end up with a near cycle accurate 1802 core.
I'm also new to SpinalHDL, as of writing this. So this project gave me something to try and shoot for.

To make verification of the CPU easier, I made the simulation capable of reading a Emma debug trace log format.
The theory is that the CPU should follow that same path as the debug trace log, 
if the Address or D register doesn't match the one in the log then there is some issue and it will halt the simulation at that point.
Using GTKWave you can view the output of the simulation and debug the issue.

The test program included uses the Floating Point Subroutines by Paul Wasserman.
I copied the subroutines listing by hand into a hex editor, and checked them by overlaying a image of the hex code I copied, with the scanned images.
Included is a copy of the scans, and a copy of the binary file for the subroutines.  Along with the assemble for the program used to test the CPU.

####Features:
* Able to load programs over the serial interface
* CPU can send serial data with OUT 1 and with check if serial FIFO is full with BN1

####TODO's:
* Finish interrupt cycle.
* Add support to allow CPU access to the RX serial data.     
* Add a wiki to document the hardware.

####How setup your project:
1. You will need to have [SpinalHDL](https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Getting%20Started/getting_started.html) 
setup and working on your system to generate the HDL code.
2. For simulation you will need to have [Verilator](https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Simulation/install.html)
setup.
3. In your FPGA project you will need to generate a PPL with a clock out of 8mhz to have the serial baud rate be 115200
4. You will also need to generate a block ram.
5. You may have to edit the BlackBoxes for the PLL and Ram to match your targets.
* or you can just generate the CPU core by itself to get the HDL/Verilog.
* or copy the cpu1802.scala file to your own SpinalHDL project.


![alt text](https://cdn.discordapp.com/attachments/664986544284631040/666808880688398336/gtkwave_dtyV29rqxF.webp "GTKWave Showing Timings")

![alt text](https://cdn.discordapp.com/attachments/664986544284631040/666808588471369729/IMG_20200114_165614.webp "Target FPGA Board")
