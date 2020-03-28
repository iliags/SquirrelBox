#define TI83_PLUS
#include "tios.ads"

word a, b, c;
byte d = '@';

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