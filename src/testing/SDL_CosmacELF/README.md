## This is a full simulation of the Cosmac VIP computer.
### To compile you will need to have the following installed:
1. Cmake
1. Verilator
2. Java JDK-8
3. Scala
4. SBT https://www.scala-sbt.org/
5. Library SDL2 and SDL_tff
6. If you want a Windows executable you will need Mingw32

You may have to edit the fixed paths in the CMakeLists.txt file if it not finding verilator.
If your running Windows, use WSL running Ubuntu and build it inside of that, which is how I was compiling it.

#### Linux
```bash
sudo apt install cmake make build-essential
sudo apt install openjdk-8-jdk
sudo apt install scala
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt  update
sudo apt install sbt
sudo apt install libsdl2-dev
sudo apt install libsdl2-ttf-dev

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

#### Test Rom
```
The tests/Chip8_Tetris2.bin file is a combination of three things
0000-01FF. The Chip8 Interpreter
0200-02D9. Tetris game http://www.awfuljams.com/octojam-iv/games/tetris
1E00-1FF0. Cosmac VIP boot rom. 
Hold 4 on your keyborad and hit the reset button in the UI, 
if it works right you will see a screen that is mostly blank.
And then enter the four hex numbers of the address you wish to read & write press slowly.
After you should see <addess> <byte> at bottom of screen.
Press 0(X) for Write and A(Z) for Read.
Try wrting to 0F00 it will show up on the screen.
```

#### The Cosmac VIP hex keypad mapping
```
1 2 3 C    1 2 3 4
4 5 6 D    Q W E R
7 8 9 E    A S D F
A 0 B F    Z X C V
```

#### Dedication
The code in this part of the project was mosly worked on in a week while on vatction at my brother in laws place.
And I want to dedicate it to his father who passed away a few mouths later after my visit. RIP папа салат(papa salad). 
This is the loving name my sister gave him, after he made a huge salad for her.  And the name I can remember off the top of my head.
