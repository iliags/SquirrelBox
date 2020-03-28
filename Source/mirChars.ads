/* mchars.ads by Dan Cook (mirageOS version)
 *
 * Prints all the character values in a grid
 * using the small (variable width) font. Note
 * that all variables start with a defualt
 * value of zero unless they are tied to some
 * address (i.e. curRow, penRow, and penCol).
 */

#define TI83_PLUS
#include "mirage.ads"

void main() {
    byte ch;
    clrHome();
    penPos = 0;
    do {
        drawChar(ch);
        ch = ch + 1;
        if(penCol >= 90) {
            penCol = 0;
            penRow = penRow + 6;
        }
    } while(ch < 255);
}