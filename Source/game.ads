/* Make the '+' push the '.' onto the 'O'.
 * DO NOT push the '.' against the walls,
 * or else you will not be able to get it
 * onto the 'O', and the program will not
 * exit until you turn off your calculator.
 */

#define TI83_PLUS
#include "tios.ads"

byte irow = 2, icol = 2;
byte urow = 6, ucol = 10;

void erase() {
	curRow = urow;
	curCol = ucol;
	putChar(' ');
}

void main() {
	clrHome();
	curRow = irow;
	curCol = icol;
	putChar('.');
	curRow = urow;
	curCol = ucol;
	putChar('+');
	curRow = 6;
	curCol = 14;
	putChar('O');

	label start;

	byte key = scanKey();

	if(key == 1) { // down
		goto start if(urow == 7);
		if(irow == urow+1) {
			if(icol == ucol) {
				goto start if(irow == 7);
				irow = irow + 1;
			}
		}
		erase();
		urow = urow + 1;
	} else if(key == 2) { // left
		goto start if(ucol == 0);
		if(icol == ucol-1) {
			if(irow == urow) {
				goto start if(icol == 0);
				icol = icol - 1;
			}
		}
		erase();
		ucol = ucol - 1;
	} else if(key == 3) { // right
		goto start if(ucol == 15);
		if(icol == ucol+1) {
			if(irow == urow) {
				goto start if(icol == 15);
				icol = icol + 1;
			}
		}
		erase();
		ucol = ucol + 1;
	} else if(key == 4) { // up
		goto start if(urow == 0);
		if(irow == urow-1) {
			if(icol == ucol) {
				goto start if(irow == 0);
				irow = irow - 1;
			}
		}
		erase();
		urow = urow - 1;
	}

	curRow = urow;
	curCol = ucol;
	putChar('+');
	curRow = irow;
	curCol = icol;
	putChar('.');
	curRow = 6;
	curCol = 14;
	putChar('O');

	goto start if(irow != 6);
	goto start if(icol != 14);
}