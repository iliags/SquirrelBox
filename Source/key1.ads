/* keytest.ads - tests the getKey function
 * in stuff.ads and prints out the character
 * and numeric value. Pressing CLEAR clears
 * the screen or quits the program if the
 * screen has already been cleared.
 */

#define TI83_PLUS
#include "tios.ads"

void main() {
	label start;

	clrHome();
	curCol = 2;

	label top;

	byte key = getKey();
	printNum(key);
	curCol = curCol-5;
	printChar(key);
	putChar('=');
	curCol = curCol+6;

	if(curCol > 10) {
		println();
		curCol = 2;
	}

	goto top if(key != 9);
	goto start if(curRow != 0);
	goto start if(curCol == 2);
}