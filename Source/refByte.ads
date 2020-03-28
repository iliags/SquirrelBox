/* References in here are inaccurate
 * because bytes only point to HALF of an
 * address. I made this program just to
 * test how a byte would reference.
 * refWord.ads works prefectly though.
 */

#define TI83_PLUS
#include "tios.ads"

byte a, b, c, d = '@';

void show() {
	printChar(***a);
	printChar(**b);
	printChar(*c);
	printChar(d);
	println();
}

void main() {
	c = &d;
	b = &c;
	a = &b;

	clrHome();

	show();
	d = '&';
	show();
	*c = '#';
	show();
	***a = '%';
	show();
	**b = '$';
	show();
}