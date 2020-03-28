#ifdef TI83_PLUS
	#ifndef MIRAGE_OS
		#define MIRAGE_OS

		#header "#include \"ti83plus.inc\""
		#header "#include \"mirage.inc\""
		#header " .ORG $9D93"
		#header " .DB $BB,$6D"
		#header " RET"
		#header " .DB 1"
		#header " .DB %00000000,%00000000"
		#header " .DB %00100110,%01110100"
		#header " .DB %01010101,%10011100"
		#header " .DB %01010101,%10011100"
		#header " .DB %01010101,%01010100"
		#header " .DB %01010101,%00110100"
		#header " .DB %01010101,%00110100"
		#header " .DB %01110110,%11010100"
		#header " .DB %01010000,%00000000"
		#header " .DB %01010110,%10001100"
		#header " .DB %01010101,%10010000"
		#header " .DB %01010110,%10010100"
		#header " .DB %01010101,%10010100"
		#header " .DB %01010110,%11101100"
		#header " .DB %00000000,%00000000"
		#header " .DB \"AntiDisasSeMBLaGe PRGM\",0"
		#endheader

		#include "tios.ads"

		// Add in macros to access MirageOS stuff

	#endif
#endif
#eof