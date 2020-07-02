#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>

#include <memory>
#include <vector>

#include <verilated_vcd_c.h>

#include "Elements.h"

#include "VCDP1802.h"
#include "VCDP1861.h"

#define RAM_SIZE 8192 * 2
//#define TRACE

//Bit functions
#define BIT_SET(X, Y) 				*(X) |= (1<<Y)
#define BIT_CLEAR(X, Y) 			*(X) &= ~(1<<Y)
#define BIT_CHECK(X, Y)				(*(X) & (1<<Y))
#define BIT_TOGGLE(X, Y)			*(X) ^= (1<<Y)
#define SHIFT(X)                    (1<<X)
#define SHIFT_MSB(X)                (0x8000>>X)

using namespace std;
//SDL renderer and single font (leaving global for simplicity)
SDL_Renderer *renderer;
TTF_Font *font;
SDL_Texture *tex1861;
SDL_Rect tex1861Dest;

VerilatedVcdC	*m_trace;

VCDP1802 cpu;
VCDP1861 dis;

Uint8 ram[RAM_SIZE];

vector<shared_ptr<Element>> Elements;

//simple wrapper for adding 1bit visual elements to our viss vector
shared_ptr<ElementVis1> add(int x, int y, CData &v, Uint8 elements, const char *str = nullptr)
{
	shared_ptr<ElementVis1> s = make_shared<ElementVis1>(x, y, v, elements, renderer, font);
	if (str)
		s->setText(str);
	Elements.push_back(s);
	return s;
}
//simple wrapper for adding 16bit visual elements to our viss vector
shared_ptr<ElementVis16> add(int x, int y, SData &v, const char *str = nullptr)
{
	//use a shared pointer, set the text, and add it to the vector
	shared_ptr<ElementVis16> s = make_shared<ElementVis16>(x, y, v, renderer, font);
	if(str)
		s->setText(str);
	Elements.push_back(s);
	return s;
}

void handleMouse(int x, int y)
{
	//iterate through each visual element and handle mouse click
	for (auto Element : Elements)
		if (Element->click(x, y))
			return;
}

SData Keys = 0;

int handleInput()
{
	SDL_Event event;
	//event handling, check for close window, escape key and mouse clicks
	//return -1 when exit requested
	while (SDL_PollEvent(&event)) {
		switch (event.type) {
		case SDL_QUIT:
			return -1;

		case SDL_KEYDOWN:
			if (event.key.keysym.sym == SDLK_ESCAPE)
				return -1;

		case SDL_MOUSEBUTTONDOWN:
			handleMouse(event.button.x, event.button.y);
			break;
		}
	}

	const Uint8* keystates = SDL_GetKeyboardState(NULL);

    if (keystates[SDL_SCANCODE_X])
        BIT_SET(&Keys, 0);
    else
        BIT_CLEAR(&Keys, 0);

    if (keystates[SDL_SCANCODE_1])
        BIT_SET(&Keys, 1);
    else
        BIT_CLEAR(&Keys, 1);

    if (keystates[SDL_SCANCODE_2])
        BIT_SET(&Keys, 2);
    else
        BIT_CLEAR(&Keys, 2);

    if (keystates[SDL_SCANCODE_3])
        BIT_SET(&Keys, 3);
    else
        BIT_CLEAR(&Keys, 3);

    if (keystates[SDL_SCANCODE_Q])
        BIT_SET(&Keys, 4);
    else
        BIT_CLEAR(&Keys, 4);

    if (keystates[SDL_SCANCODE_W])
        BIT_SET(&Keys, 5);
    else
        BIT_CLEAR(&Keys, 5);

    if (keystates[SDL_SCANCODE_E])
        BIT_SET(&Keys, 6);
    else
        BIT_CLEAR(&Keys, 6);

    if (keystates[SDL_SCANCODE_A])
        BIT_SET(&Keys, 7);
    else
        BIT_CLEAR(&Keys, 7);

    if (keystates[SDL_SCANCODE_S])
        BIT_SET(&Keys, 8);
    else
        BIT_CLEAR(&Keys, 8);

    if (keystates[SDL_SCANCODE_D])
        BIT_SET(&Keys, 9);
    else
        BIT_CLEAR(&Keys, 9);

    if (keystates[SDL_SCANCODE_Z])
        BIT_SET(&Keys, 10);
    else
        BIT_CLEAR(&Keys, 10);

    if (keystates[SDL_SCANCODE_C])
        BIT_SET(&Keys, 11);
    else
        BIT_CLEAR(&Keys, 11);

    if (keystates[SDL_SCANCODE_4])
        BIT_SET(&Keys, 12);
    else
        BIT_CLEAR(&Keys, 12);

    if (keystates[SDL_SCANCODE_R])
        BIT_SET(&Keys, 13);
    else
        BIT_CLEAR(&Keys, 13);

    if (keystates[SDL_SCANCODE_F])
        BIT_SET(&Keys, 14);
    else
        BIT_CLEAR(&Keys, 14);

    if (keystates[SDL_SCANCODE_V])
        BIT_SET(&Keys, 15);
    else
        BIT_CLEAR(&Keys, 15);


	return 0;
}


int initVideo()
{
    //setup SDL with title, 640x480, and load font
	if (SDL_Init(SDL_INIT_VIDEO)) {
		printf("Unable to initialize SDL: %s\n", SDL_GetError());
		return 0;
	}
    SDL_Window *window = SDL_CreateWindow("CosmacELF - SDL", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, 640, 480, 0);
	if (!window) {
		printf("Can't create window: %s\n", SDL_GetError());
		return 0;
	}

    renderer = SDL_CreateRenderer(window, -1, 0);

    /* Initialize the TTF library */
    if (TTF_Init() < 0) {
            fprintf(stderr, "Couldn't initialize TTF: %s\n",SDL_GetError());
            SDL_Quit();
            return 0;
    }

    font = TTF_OpenFont("OpenSans-Regular.ttf", 24);
    if(!font) {
    printf("TTF_OpenFont: %s\n", TTF_GetError());
    // handle error
  }
    assert(font);

	tex1861Dest.x=(640/2) - 64*3;
	tex1861Dest.y=480/1.8;
	tex1861Dest.w=64*6;
	tex1861Dest.h=32*6;

	tex1861 = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STATIC, 64, 32);

	return 1;
}

void Draw_Video(const Uint32 offset){
    Uint32 * pixels = new Uint32[64 * 32];
	Uint8 l;
	for (size_t i = 0; i < 64 * 32; i++) {
		l = (ram[offset+(i >> 3)] & (0x80 >> (0x7 & i))) > 0 ? 231 : 0;
		l += (rand() % 24);
		pixels[i] = l | l << 8 | l << 16;
	}
	SDL_UpdateTexture(tex1861, NULL, pixels, 64 * sizeof(Uint32));
}

void draw()
{
    //clear screen, draw each element, then flip the buffer
    SDL_SetRenderDrawColor(renderer, 100, 100, 100, 255);
    SDL_RenderClear(renderer);

	SDL_RenderCopy(renderer, tex1861, NULL, &tex1861Dest);

	for (auto Element : Elements)
        Element->draw();

    SDL_RenderPresent(renderer);
}

int main(int argc, char *argv[])
{
    if(initVideo()==0) return -1;

    FILE *fp = fopen(argv[1], "r");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return -1;
    }

    fseek(fp, 0L, SEEK_END);
    Uint32 fsize = ftell(fp);
    fseek(fp, 0L, SEEK_SET);

    if(fsize > RAM_SIZE){
        printf("File is to big!\n");
        return -1;
    }

    fread(ram, 1, fsize, fp);
    fclose(fp);

	CData Step=0;
	CData Run=0;
	CData Reset=0;

	Uint64 main_time=0;

	Uint8 Run_Edge=0;
	Uint8 W_Edge=0;
	Uint8 R_Edge=0;
	Uint8 N_Edge=0;
	Uint8 SC_Edge=0;
	Uint8 Int_Edge=0;
	Uint8 EFx_Edge=0;

    Uint8 KeyLatch=0;

	Uint8 Remap = 0;
	Uint16 Remap_Addr = 0;


	Uint8 drawVideo = 0;

    //set up where we'll start drawing the inputs
    int xstart = 110;
    int ystart = 80;

    //set where to draw the outputs
    int ystart2 = ystart + 220;

    //the current x position
    int posx = xstart;

    //how much to increment between the elements
    int xinc = 81;
    int yinc = 40;

    BIT_SET(&Keys, 1);

    add(xstart, ystart - yinc, Keys, "Keys");
    add(xstart, ystart, cpu.io_Addr16, "Add");
    add(xstart, ystart + yinc, cpu.io_DataOut, 7, "Data");
    add(xstart + xinc * 3.8, ystart + yinc, cpu.io_MRD, 0, "MRD");
    add(xstart + xinc * 5, ystart + yinc, cpu.io_MWR, 0, "MWR");
    add(xstart, ystart + yinc * 2, cpu.io_SC, 1, "SC");
    add(xstart + xinc, ystart + yinc * 2, cpu.io_N, 2, "N");
    add(xstart, ystart + yinc * 3, cpu.io_Q, 0, "Q");
	add(xstart, ystart + yinc * 4, cpu.io_Wait_n, 0, "Wait");
	add(xstart + xinc * 1.2, ystart + yinc * 4, dis.io_Reset_, 0, "Clear");
	add(xstart + xinc * 3, ystart + yinc * 4, Step, 0, "Step");
	add(xstart + xinc * 4, ystart + yinc * 4, Run, 0, "Run");
	add(xstart + xinc * 5.2, ystart + yinc * 4, Reset, 0, "Reset");
	draw();

	#ifdef TRACE
		Verilated::traceEverOn(true);
		m_trace = new VerilatedVcdC;
		cpu.trace(m_trace, 99);
		dis.trace(m_trace, 99);
		m_trace->open ("simx.vcd");
	#endif

    //main loop
    do
    {
		dis.reset = (main_time>100) ? 0 : 1;
		dis.io_SC = cpu.io_SC;
		dis.io_TPA = cpu.io_TPA;
		dis.io_TPB = cpu.io_TPB;
		dis.io_DataIn = cpu.io_DataOut;
		dis.io_Disp_On = cpu.io_N == 1 && cpu.io_TPB && !cpu.io_MWR;
		dis.io_Disp_Off = cpu.io_N == 1 && cpu.io_TPB && !cpu.io_MRD;

        cpu.reset_1_ = (main_time>100) ? 0 : 1;
        cpu.io_DMA_In_n = 1;
        cpu.io_DMA_Out_n = dis.io_DMAO;
        cpu.io_Interrupt_n = dis.io_INT;
		cpu.io_EF_n = 0x0A | dis.io_EFx | ((BIT_CHECK(&Keys, KeyLatch) == 0)<<2);

		if(Run){
			Step = 0;
			dis.io_Reset_ = 1;
			cpu.io_Wait_n = 1;
		} else if(!Run && Run_Edge) {
			cpu.io_Wait_n = 0;
		}

		Run_Edge = Run;

		if(Step){
			dis.io_Reset_ = 1;
			cpu.io_Wait_n = 1;
		}

		if(Step && cpu.io_SC == 0 && SC_Edge != 0){
			Step = 0;
			cpu.io_Wait_n = 0;
		}


		if(Reset){
			dis.io_Reset_ = 0;
			Reset = 0;
		}

		cpu.io_Clear_n = dis.io_Clear;

		if(cpu.io_N == 0x4){
			Remap=1;
		}else if(!dis.io_Clear){
			Remap=0;
		}
		
		if(!Remap || (cpu.io_Addr16 & 0x8000))
		{
			Remap_Addr = 0x1e00 | (cpu.io_Addr16 & 0x1FF);
		} else {
			Remap_Addr = cpu.io_Addr16 & 0x1FFF;
		}

		if(Remap_Addr > RAM_SIZE && !cpu.io_MRD && !cpu.io_MWR){
			printf("Accessed RAM outside of RAM area: %04X/%04X\r\n",cpu.io_Addr16, RAM_SIZE);
			goto done;
		}

		//memory stuff
		if(!cpu.io_MRD){
			cpu.io_DataIn = ram[Remap_Addr];
		}

		if(!cpu.io_MWR && cpu.io_TPB && !N_Edge && Remap_Addr < 0x1e00){
			ram[Remap_Addr] = cpu.io_DataOut;
		}

		if(cpu.io_N == 4 && cpu.io_TPB && !N_Edge){
			printf("%c",(char) cpu.io_DataOut);
			fflush(stdout);
		}

		if(dis.io_INT && !Int_Edge && dis.io_EFx && !EFx_Edge){
			Draw_Video(Remap_Addr);
			drawVideo=1;
		} else drawVideo=0;

        if(cpu.io_N == 2 && cpu.io_TPB && !N_Edge){
            KeyLatch = cpu.io_DataOut & 0xf;
        }

		SC_Edge = cpu.io_SC;
		W_Edge = cpu.io_MWR;
		R_Edge = cpu.io_MRD;
		N_Edge = cpu.io_TPB;
		Int_Edge = dis.io_INT;
		EFx_Edge = dis.io_EFx;

        main_time++;
		dis.clk = 1;
		cpu.clk = 1;
		dis.eval();
        cpu.eval(); //preform model update based on inputs

		#ifdef TRACE
			m_trace->dump (main_time);
		#endif

        main_time++;
		dis.clk = 0;
		cpu.clk = 0;
		dis.eval();
        cpu.eval(); //preform model update based on inputs

		#ifdef TRACE
			m_trace->dump (main_time);
		#endif

		if(drawVideo || main_time % 7000 == 0){ draw();}
    }
    //run until exit requested
    while (handleInput() >= 0);

		done:
		cpu.final();
		dis.final();

		#ifdef TRACE
			m_trace->close();
		#endif

    return 0;
}
