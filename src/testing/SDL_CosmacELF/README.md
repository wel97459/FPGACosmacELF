## This is a full simulation of the Cosmac VIP computer.
### To compile you will need to have:
1. Verilator
2. Java JDK-8
3. Scala
4. SBT https://www.scala-sbt.org/
5. Library SDL2 and SDL_tff
6. If you want a Windows executable you will need Mingw32

You may have to edit the fixed paths in the CMakeLists.txt file, if it not finding verilator.

#### Linux
```bash
sudo apt-get install openjdk-8-jdk
sudo apt-get install scala
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get update
sudo apt-get install sbt

git clone https://github.com/wel97459/FPGACosmacELF
cd FPGACosmacELF/src/testing/SDL_CosmacELF
cmake .
make
./cosmacELF tests/Chip8_Tetris2.bin
```

#### Windows when compiling under linux with MINGW32
```bash
cmake -DMINGW32=1 .
cosmacELF.exe tests/Chip8_Tetris2.bin
```

