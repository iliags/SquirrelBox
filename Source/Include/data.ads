#ifndef DATA_MOVEMENT
	#define DATA_MOVEMENT

	// Stack operations

	#define "PushByte (", byte, ")"
		as @" LD H, 0 \n LD A, ", @byte, @"\n LD L, A\n PUSH HL"
	#define "PopByte (", byte, ")"
		as @" POP HL\n LD A, L\n LD (", @&byte, @"),A"
	#define "PushWord (", word, ")"
		as @" LD HL, (", @&word, @")\n PUSH HL\n LD HL, (", @&word, @"+1)\n PUSH HL"
	#define "PopWord (", word, ")"
		as @" POP HL\n LD (", @&word, @"+1), HL\n POP HL\n LD (", @&word, @"), HL"

	// Insert string and array manipulating functions & macros here

#endif
#eof