#ifndef ELEMEANTS_CLASS_H
#define ELEMEANTS_CLASS_H
#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>

#include <memory>
#include <vector>

#include "VCDP1802.h"
//parent class Vis will be inherited by our 16 bit and 1 bit values,
//it sets up common items click the text rendering and interface for
//draw and click
using namespace std;
    class Element {
        protected:
            //stores our Vis elements location and size (not including message)
            SDL_Rect m_rect;
            SDL_Texture *textTexture = nullptr;
            SDL_Renderer *renderer;
            TTF_Font *font;
            //some defaults
            static int box_size, box_space, group_space;

        public:
        	Element(int x, int y, SDL_Renderer *r, TTF_Font *f);
        	virtual void draw();

        	//interface for mouse
        	virtual bool click(int x, int y);
        	void setText(const char *str);
    };

    //Vis1 is the 1bit visualization element
    class ElementVis1 : public Element {
    public:
        ElementVis1(int x, int y, CData &value, Uint8 e, SDL_Renderer *r, TTF_Font *f);
        void draw();
        bool click(int x, int y);

    private:
        CData &m_value; //1bit value in model (stored as 8bit in memory)
        Uint8 elementCount;
    };

    //Vis16 is the 16bit visualization element
    class ElementVis16 : public Element {
    public:
    	ElementVis16(int x, int y, SData &value, SDL_Renderer *r, TTF_Font *f);
    	void draw();
    	bool click(int x, int y);

    private:
    	SData &m_value; //16bit value in model
        Uint8 elementCount;
    };
#endif
