#ifndef GENERAL_Z80

	//Standard TI-OS Headers

	#ifdef TI86
		#header ".nolist"
		#header "#include \"ti86.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI85
		#header ".nolist"
		#header "#include \"ti85.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI84_PLUS_SE
		#header ".nolist"
		#header "#include \"ti84plusse.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI84_PLUS
		#header ".nolist"
		#header "#include \"ti84plus.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI83_PLUS_SE
		#header ".nolist"
		#header "#include \"ti83plusse.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI83_PLUS
		#header ".nolist"
		#header "#include \"ti83plus.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI83
		#header ".nolist"
		#header "#include \"ti83.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif
	#ifdef TI82
		#header ".nolist"
		#header "#include \"ti82.inc\""
		#header ".list"
		#header ".ORG userMem-2"
		#header " .DB t2ByteTok,tAsmCmp"
		#endheader
		// #safeRam "SAFE_RAM" <-- This should be a command to designate "safe RAM" areas
	#endif

	// Display Commands & Variables

	byte curCol @ "CurCol"; byte curRow @ "CurRow";
	byte penCol @ "PenCol"; byte penRow @ "PenRow";
	word curPos @ "CurRow"; word penPos @ "PenCol";

	void setCursor(byte row, byte col) { curRow = row; curCol = col; }

	void clrHome() { #asm " B_CALL(_ClrScrnFull)\n B_CALL(_HomeUp)"; }

	void putChar(byte char) { #asm " LD A, ", char, "\n B_CALL(_PutMap)"; }
	void printChar(byte char) { #asm " LD A, ", char, "\n B_CALL(_PutC)"; }
	void printStr(word & str) { #asm " LD HL, ", str, "\n B_CALL(_PutS)"; }
	void printNum(word num) { #asm " LD HL, ", num, "\n B_CALL(_DispHL)"; }
	void println() { #asm " B_CALL(_NewLine)"; }

	void drawChar(byte char) { #asm " LD A, ", char, "\n B_CALL(_VPutMap)"; }
	void drawStr(word & str) { #asm " LD HL, ", str, "\n B_CALL(_VPutS)"; }

	/*
		// "BETTER" Numeric output functions:

		word toString(word num) { // Returns a REFERENCE to the string
			byte index = 5;
			byte[6] str;
			str[5] = 0;

			do {
				index = index-1;
				byte shift = num/10;
				str[index] = num - 10*shift + '0';
				num = shift;
			} while(num > 0 && index < 5);

			this = &str + index;
		}

		void printNum(word num) { printStr(*toString(num)); }
		void drawNum(word num) { drawStr(*toString(num)); }
	*/

	// Key Input

	byte getKey() { #asm " B_CALL(_GetKey)\n LD (", this, "), A"; }
	byte scanKey() { #asm " B_CALL(_GetCSC)\n LD (", this, "), A"; }
	void disableOnKey() { #asm " B_CALL($500B)"; }

	// Textual Input

	/* word getNum() {
		this = 0;
		do {
			byte key = getKey() - KEY_0;
			return if(key == KEY_ENTER);
			if(key == KEY_LEFT - KEY_0) {
				curCol = curCol - 1;
				this = this/10;
				putChar(' ');
			}
			else if(key <= KEY_9 - KEY_0) {
				this = this*10 + key;
				printChar(key);
			}
		} while(this < 6553);
		while(getKey() != ENTER_KEY);
	} */

	/* word getStr() {
		byte[16] string; // This should land in SafeRam eventually
		byte index = 0;
		this = &string;
		do {
			byte key = getKey() - KEY_A;
			goto finish if(key == KEY_ENTER);
			if(index > 0 && key == KEY_LEFT - KEY_A) {
				curCol = curCol - 1;
				index = index-1;
				putChar(' ');
			}
			else if(key <= KEY_Z - KEY_A) {
				string[index] = key;
				index = index+1;
				printChar(key);
			}
		} while(index < 15);
		while(getKey() != ENTER_KEY);

		label finish;
		this[index] = 0;
	} */

#endif
#eof