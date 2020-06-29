#include "Elements.h"

int Element::box_size = 20; //size of each bit
int Element::box_space = 5; //space between each bit
int Element::group_space = 10; //space between each 4-bit group (simple for hex)

Element :: Element(int x, int y, SDL_Renderer *r, TTF_Font *f)
{
	//set starting position of rect, the size is unknown at this time
	m_rect = { x, y, 0, 0};
    renderer = r;
    font = f;
}

void Element :: draw()
{
	//if there's a texture set, draw it
	if(textTexture) {
		SDL_Rect dest = m_rect;
		//get the text size
		SDL_QueryTexture(textTexture, NULL, NULL, &dest.w, &dest.h);
		//offset of -5, -10 from the Vis item itself
		dest.x -= dest.w + 5;
		dest.y -= 10;
		//draw the text to the renderer surface
		SDL_RenderCopy(renderer, textTexture, nullptr, &dest);
	}
}

//interface for mouse
bool Element :: click(int x, int y) {return false;}

void Element :: setText(const char *str)
{
	//if there's already a texture, remove it
	if(textTexture) {
		SDL_DestroyTexture(textTexture);
		textTexture = nullptr;
	}
	//white font
	SDL_Color color = {255, 255, 255, 255};
	//create the surface of the font in host space
	SDL_Surface *textSurface = TTF_RenderText_Solid(font, str, color);
	//create the texture of the font in video space
	textTexture = SDL_CreateTextureFromSurface(renderer, textSurface);
	//should probably free the textSurface?
	//SDL_FreeSurface(textSurface);
}

//Vis1 is the 1bit visualization element
ElementVis1 :: ElementVis1(int x, int y, CData &value, Uint8 e, SDL_Renderer *r, TTF_Font *f) : Element(x, y, r, f), m_value(value), elementCount(e) { }
void ElementVis1 :: draw()
{
	Element::draw(); //handles text rendering

	//iterate through each group of 4-bits
	int ipos = 0;
	int c = 0;
	for (int i = 0; i <= elementCount; i++) {
		SDL_Rect rect = { ipos + m_rect.x, m_rect.y, box_size, box_size };

		//each bit value in the value itself (reverses bit order visually)
		//and sets color to white or black accordingly
		if (m_value & 1 << (elementCount - c))
			SDL_SetRenderDrawColor(renderer, 255, 255, 255, 255);
		else
			SDL_SetRenderDrawColor(renderer, 0, 0, 0, 255);

		//display bit
		SDL_RenderFillRect(renderer, &rect);
		ipos += box_size + box_space;
		c++;

		//increase position by group spacing
		if(c % 4 == 0) ipos += group_space;
	}
}

bool ElementVis1 :: click(int x, int y)
{
	SDL_Rect rect = { m_rect.x, m_rect.y, box_size, box_size };
	SDL_Point p = { x, y };
	if (SDL_PointInRect(&p, &rect)) {
		m_value = !m_value;
		return true;
	}
	return false;
}


//Vis16 is the 16bit visualization element
	ElementVis16 :: ElementVis16(int x, int y, SData &value, SDL_Renderer *r, TTF_Font *f) : Element(x, y, r, f), m_value(value) { }
	void ElementVis16 :: draw()
	{
		Element::draw(); //handles text rendering

		//iterate through each group of 4-bits
		int ipos = 0;
		int c = 0;
		for (int g = 0; g < 4; g++) {
			//iterate through each bit in the group
			for (int i = 0; i < 4; i++) {
				SDL_Rect rect = { ipos + m_rect.x, m_rect.y, box_size, box_size };

				//each bit value in the value itself (reverses bit order visually)
				//and sets color to white or black accordingly
				if (m_value & 1 << (15 - c))
					SDL_SetRenderDrawColor(renderer, 255, 255, 255, 255);
				else
					SDL_SetRenderDrawColor(renderer, 0, 0, 0, 255);

				//display bit
				SDL_RenderFillRect(renderer, &rect);
				ipos += box_size + box_space;
				c++;
			}
			//increase position by group spacing
			ipos += group_space;
		}
	}

	bool ElementVis16 :: click(int x, int y)
	{
		//mouse click position
		SDL_Point p = { x, y };

		int ipos = 0;
		int c = 0;
		//iterate each group
		for (int g = 0; g < 4; g++) {
			//iterate each bit
			for (int i = 0; i < 4; i++) {
				SDL_Rect rect = { ipos + m_rect.x, m_rect.y, box_size, box_size };

				//checks for mouse point in bit's rectangle
				if (SDL_PointInRect(&p, &rect)) {
					m_value ^= 1 << (15 - c);
					return true; //hit
				}
				ipos += box_size + box_space;
				c++;
			}
			ipos += group_space;
		}

		return false; //no hit
	}
