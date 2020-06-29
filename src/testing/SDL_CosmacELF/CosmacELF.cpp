#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>

#include <memory>
#include <vector>


#ifdef TRACE
	#include <verilated_vcd_c.h>
#endif

#include "Elements.h"

#include "VCDP1802.h"

#define RAM_SIZE 8192

using namespace std;
//SDL renderer and single font (leaving global for simplicity)
SDL_Renderer *renderer;
TTF_Font *font;

VCDP1802 top;

#ifdef TRACE
	VerilatedVcdC	*m_trace;
#endif

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
	return 1;
}

void draw()
{
    //clear screen, draw each element, then flip the buffer
    SDL_SetRenderDrawColor(renderer, 100, 100, 100, 255);
    SDL_RenderClear(renderer);

    for (auto Element : Elements)
        Element->draw();

    SDL_RenderPresent(renderer);
}



int main(int argc, char *argv[])
{
	Uint8 ram[RAM_SIZE];
    if(initVideo()==0)return -1;

    FILE *fp = fopen(argv[1], "r");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return -1;
    }
    fseek(fp, 0L, SEEK_END);
    Uint32 fsize = ftell(fp);
    fseek(fp, 0L, SEEK_SET);
    if(fsize > 4096){
        printf("File is to big!\n");
        return -1;
    }

    fread(ram, 1, fsize, fp);
    fclose(fp);

	CData Step=0;
	CData Run=0;
	CData Reset=0;


	Uint64 main_time=0;

	Uint8 Edge=0;
	Uint8 W_Edge=0;
	Uint8 R_Edge=0;
	Uint8 Run_Edge=0;
	Uint8 N_Edge=0;

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

    add(xstart, ystart, top.io_Addr16, "Add");
    add(xstart, ystart + yinc, top.io_DataOut, 7, "Data");
    add(xstart + xinc * 3.8, ystart + yinc, top.io_MRD, 0, "MRD");
    add(xstart + xinc * 5, ystart + yinc, top.io_MWR, 0, "MWR");
    add(xstart, ystart + yinc * 2, top.io_SC, 1, "SC");
    add(xstart + xinc, ystart + yinc * 2, top.io_N, 2, "N");
    add(xstart, ystart + yinc * 3, top.io_Q, 0, "Q");
	add(xstart, ystart + yinc * 4, top.io_Wait_n, 0, "Wait");
	add(xstart + xinc * 1.2, ystart + yinc * 4, top.io_Clear_n, 0, "Clear");
	add(xstart + xinc * 3, ystart + yinc * 4, Step, 0, "Step");
	add(xstart + xinc * 4, ystart + yinc * 4, Run, 0, "Run");
	add(xstart + xinc * 5.2, ystart + yinc * 4, Reset, 0, "Reset");

	#ifdef TRACE
		Verilated::traceEverOn(true);
		m_trace = new VerilatedVcdC;
		top.trace(m_trace, 99);
		m_trace->open ("simx.vcd");
	#endif

    //main loop
    do
    {
        top.reset_1_ = (main_time>100) ? 0 : 1;
        top.io_DMA_In_n = 1;
        top.io_DMA_Out_n = 1;
        top.io_Interrupt_n = 1;
		top.io_EF_n = 0x00;

		if(Run){
			Step = 0;
			top.io_Clear_n = 1;
			top.io_Wait_n = 1;
		} else if(!Run && Run_Edge) {
			top.io_Wait_n = 0;
		}

		Run_Edge = Run;

		if(Step){
			top.io_Clear_n = 1;
			top.io_Wait_n = 1;
		}

		if(Step && top.io_SC == 0 && Edge != 0){
			Step = 0;
			top.io_Wait_n = 0;
		}


		if(Reset){
			top.io_Clear_n = 0;
			Reset = 0;
		}

		if(top.io_Addr16 > RAM_SIZE && !top.io_MRD && !top.io_MWR){
			printf("Accessed RAM outside of RAM area: %04X/%04X\r\n",top.io_Addr16, RAM_SIZE);
			goto done;
		}

		//memory stuff
		if(!top.io_MRD){
			top.io_DataIn = ram[(Uint16)top.io_Addr16];
		}

		if(!top.io_MWR && top.io_TPB && !N_Edge){
			ram[(Uint16)top.io_Addr16] = top.io_DataOut;
		}

		if(top.io_N ==1 && top.io_TPB && !N_Edge){
			printf("%c",(char) top.io_DataOut);
			fflush(stdout);
		}

		Edge = top.io_SC;
		W_Edge = top.io_MWR;
		R_Edge = top.io_MRD;
		N_Edge = top.io_TPB;

        main_time++;
		top.clk = 1;
        top.eval(); //preform model update based on inputs

		#ifdef TRACE
			m_trace->dump (main_time);
		#endif

        main_time++;
		top.clk = 0;
        top.eval(); //preform model update based on inputs

		#ifdef TRACE
			m_trace->dump (main_time);
		#endif
		if(main_time % ((Run) ? 7*1000 : 7) == 0) draw();
    }
    //run until exit requested
    while (handleInput() >= 0);

		done:
		top.final();
		#ifdef TRACE
			m_trace->close();
		#endif

    return 0;
}
