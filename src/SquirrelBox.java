/*
SquirrelBox 1.6 alpha 6.
Copyright (C) 2007 Dan Cook

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

/*************************************************************************
**************************************************************************
***                                                                    ***
***  MAY THIS HORRIBLE NIGHTMARE OF CODE STAND AS A WITNESS AS TO WHY  ***
***  IT IS VITAL TO DOCUMENT EVERYTHING IN YOUR CODE WITH COMMENTS!!!  ***
***                                                                    ***
**************************************************************************
*************************************************************************/

/*************************************************************************
**************************************************************************
***                                                                    ***
***  And may the slow performance of this compiler demonstrate why it  ***
***  is unwise not to tokenize source code before compiling it.        ***
***                                                                    ***
**************************************************************************
*************************************************************************/

// ORDER OF OPERATIONS. THIS:  A|B^C&D<<E+F*-&G[H]
// RESOLVES TO:  A|(B^(C&(D<<(E+(F*(-(*(G[H]))))))))

import javax.swing.JOptionPane;

class Result { // Resulting value throughout an expression
	public int bytes;    // result type (byte or word)
	public String value; // numeric value or var address

	public Result(int b, String v) { bytes = b; value = v; }

	public boolean reference() {
		return value.charAt(0) == '(' && value.charAt(value.length()-1) == ')';
	}

	public boolean numeric() {
		try { Integer.parseInt(value); return true;
		} catch(NumberFormatException n){}
		return !reference() && !register();
	}

	private boolean register(char ch) {
		return ch == 'H' || ch == 'L' || ch >= 'A' && ch <= 'F';
	}

	public boolean register() {
		if(value.length()>2) return false;
		if(value.length()>1) {
			if(!register(value.charAt(1))) return false;
		} return register(value.charAt(0));
	}

	public boolean[] loadRegister(SquirrelBox s, boolean[] free) {
		if(register()) return free;
		boolean[] copy = new boolean[8];
		for(int i=0; i<8; i++) copy[i] = free[i];
		String reg = null;
		if(bytes == 2) {
			if(copy[6] && copy[7]) {
				reg = "HL"; copy[6] = false; copy[7] = false;
			} else if(copy[3] && copy[4]) { // DE is better for EX DE,HL
				reg = "DE"; copy[3] = false; copy[4] = false;
			} else if(copy[1] && copy[2]) {
				reg = "BC"; copy[1] = false; copy[2] = false;
			} else ranOutOfRegisters(s,"16",copy);
		} else {
			if(copy[0]) { reg = "A"; copy[0] = false; }
			else if(copy[1]) { reg = "B"; copy[1] = false; }
			else if(copy[2]) { reg = "C"; copy[2] = false; }
			else if(copy[3]) { reg = "D"; copy[3] = false; }
			else if(copy[4]) { reg = "E"; copy[4] = false; }
			else if(copy[6]) { reg = "H"; copy[6] = false; }
			else if(copy[7]) { reg = "L"; copy[7] = false; }
			else ranOutOfRegisters(s, "8", copy);
		} if(reference() && !reg.equals("A") && bytes != 2) {
			char temp = 0;
			if(copy[0]) temp = 'A';
			else if(copy[1]) temp = 'B';
			else if(copy[2]) temp = 'C';
			else if(copy[3]) temp = 'D';
			else if(copy[4]) temp = 'E';
			else if(copy[6]) temp = 'H';
			else if(copy[7]) temp = 'L';
			else ranOutOfRegisters(s, "8", copy);
			s.asmOutln(" LD "+temp+",A");
			s.asmOutln(" LD A,"+value);
			s.asmOutln(" LD "+reg+",A");
			s.asmOutln(" LD A,"+temp);
		} else s.asmOutln(" LD "+reg+","+value);
		value = reg; return copy;
	}

	public boolean[] toWord(SquirrelBox s, boolean[] free) {
		bytes = 2;
		if(value.length() > 1 || !register()) return free;
		String copy;
		if(value.charAt(0) == 'B' && free[2]) {
			copy = "BC"; free[2] = false;
		} else if(value.charAt(0) == 'C' && free[1]) {
			copy = "BC"; free[1] = false;
		} else if(value.charAt(0) == 'D' && free[4]) {
			copy = "DE"; free[4] = false;
		} else if(value.charAt(0) == 'E' && free[3]) {
			copy = "DE"; free[3] = false;
		} else if(value.charAt(0) == 'H' && free[7]) {
			copy = "HL"; free[7] = false;
		} else if(value.charAt(0) == 'L' && free[6]) {
			copy = "HL"; free[6] = false;
		} else {
			free = set(free, value.charAt(0), true);
			copy = unused(s, free, true);
		} bytes = 2;
		if(copy.charAt(1) != value.charAt(0))
			s.asmOutln(" LD "+copy.charAt(1)+','+value);
		s.asmOutln(" LD "+copy.charAt(0)+",0");
		value = copy; return free;
	}

	public String low() {
		try {
			return Integer.toString(Integer.parseInt(value)%256);
		} catch(NumberFormatException n){}
		if(register()) {
			if(value.length() == 2)
				return value.substring(1);
			return value;
		} if(reference()) return value;
		if(value.indexOf('+') >= 0)
			return "("+value+")%256";
		return value+"%256";
	}

	public String high() {
		try {
			return Integer.toString(Integer.parseInt(value)/256);
		} catch(NumberFormatException n){}
		if(register()) {
			if(value.length() == 2)
				return value.substring(0,1);
			return "0";
		} if(reference()) {
			if(bytes != 2) return "0";
			return value.substring(0,value.length()-1)+"+1)";
		} String copy = value;
		if(value.indexOf('+') >= 0)
			return "("+value+")/256";
		return value+"/256";
	}

	public String getByte(int high) {
		if(high > 0) return high();
		return low();
	}

	public static boolean[] freeList() {
		return new boolean[]{true,true,true,true,true,true,true,true};
	}

	public static boolean[] freeList(String use) {
		boolean[] free = freeList();
		for(int i=0; i<use.length(); i++) {
			switch(use.charAt(i)) {
			  case 'A': free[0] = false; break;
			  case 'B': free[1] = false; break;
			  case 'C': free[2] = false; break;
			  case 'D': free[3] = false; break;
			  case 'E': free[4] = false; break;
			  case 'F': free[5] = false; break;
			  case 'H': free[6] = false; break;
			  case 'L': free[7] = false; break;
			}
		} return free;
	}

	public static boolean[] freeList(Result other) {
		if(!other.register()) return freeList();
		return freeList(other.value);
	}

	public static void ranOutOfRegisters(
		SquirrelBox s, String b, boolean[] free
	) {
		String text =
			"Ran out of "+b+"-bit Registers to use :-(\nregisers in use: ";
		String reg = "ABCDEFHL";
		for(int i=0; i<8; i++) if(!free[i]) text += " "+reg.charAt(i);
		s.ErrorMessage(text+"\nTry simplifying some expressions");
	}

	public static String unused(SquirrelBox s, boolean[] free, boolean word) {
		if(word) {
			if(free[6]&&free[7]) return "HL"; if(free[1]&&free[2]) return "BC";
			if(free[3]&&free[4]) return "DE"; ranOutOfRegisters(s,"16",free);
		} if(free[0]) return "A";
		if(free[1]) return "B"; if(free[2]) return "C";
		if(free[3]) return "D"; if(free[4]) return "E";
		if(free[6]) return "H"; if(free[7]) return "L";
		ranOutOfRegisters(s,"8",free); return null;
	}

	public static boolean[] set(boolean[] free, char reg, boolean state) {
		boolean[] copy = new boolean[free.length];
		for(int i=0; i<free.length; i++) copy[i] = free[i];
		if(reg == 'A') copy[0] = state; if(reg == 'B') copy[1] = state;
		if(reg == 'C') copy[2] = state; if(reg == 'D') copy[3] = state;
		if(reg == 'E') copy[4] = state; if(reg == 'F') copy[5] = state;
		if(reg == 'H') copy[6] = state; if(reg == 'L') copy[7] = state;
		return copy;
	}
}

class Context { // Code context within [nested] control-structures
	public int blocks;
	public String trail;
	public Context previous;
	public boolean returned;
	public variable[] locals;
	public int cont, brk;

	public Context() {
		blocks = 0; trail = "."; previous = null; returned = false;
		locals = new variable[0]; cont = -1; brk = -1;
	}

	private Context(String t, Context prev, int c, int b) {
		blocks = 0; trail = t; previous = prev; returned = false;
		locals = new variable[0]; cont = c; brk = b;
	}

	public Context passIf() {
		return new Context(trail+(blocks++)+'.', this, cont, brk);
	}

	public Context passLoop(int c, int b) {
		return new Context(trail+(blocks++)+'.', this, c, b);
	}

	public int offset() {
		if(previous == null) return 0;
		return previous.offset()+previous.locals.length;
	}

	public int length() {
		return offset()+locals.length;
	}

	public int find(String var) {
		int index;
		if(previous != null) {
			index = previous.find(var);
			if(index >= 0) return index;
		}
		index = utils.find(locals, var);
		if(index >= 0) return offset()+index;
		return -1;
	}

	public variable varAt(int index) {
		if(previous == null) {
			if(index >= locals.length) return null;
			return locals[index];
		}
		variable var = previous.varAt(index);
		if(var != null) return var;
		return locals[index-offset()];
	}

	public String nameAt(int index) {
		if(previous == null) {
			if(index >= locals.length) return null;
			return trail+locals[index].name;
		}
		String name = previous.nameAt(index);
		if(name != null) return name;
		return trail+locals[index-offset()].name;
	}

	public void remove(int index) {
		if(index < offset()) {
			previous.remove(index); return;
		}
		index -= offset();
		variable[] temp = new variable[locals.length-1];
		for(int i=0; i<index; i++) temp[i] = locals[i];
		for(int i=index; i<temp.length; i++) temp[i] = locals[i+1];
		locals = temp;
	}
}

public class SquirrelBox extends SquirrelBoxGUI {
	public int Labels;
	public boolean useCpHlDe;
	public boolean useMultHE;
	public boolean useMultDeA;
	public boolean useMultDeAc;
	public boolean useDivHlD;
	public boolean useDivHlDe;
	public String[] literals;
	public variable[] globals;
	public String[] inits;
	public function[] funcs;
	public String[][] memory; // CONVERT TO int[], each being ONE VAR (its size in memory)

	public SquirrelBox() { super(); }

	public int NewLabel() {
		return Labels++;
	}

	public void comment(String com) {
		if(com == null) asmOutln();
		else {
			String text = asmOutput[current].getText();
			int fix = text.lastIndexOf('\n');
			if(fix < 0) fix = text.length()+1;
			else fix = text.length() - fix;
			if(fix > 16) fix = 9 + fix%8;
			asmOutln("                 ; ".substring(fix)+com);
		}
	}

	public void PostLabel(int label, String com) {
		asmOut("ADSMBLG_"+Integer.toString(label)+':');
		comment(com);
	}

	public int PostNewLabel(String comment) {
		PostLabel(Labels, comment);
		return Labels++;
	}

	public void jump(int label, String com) {
		asmOut(" JP ADSMBLG_"+label); comment(com);
	}

	public int[] typeOf(Context context, String token) { // {type, index}
		int index = utils.find(funcs, token);
		if(index >= 0) return new int[]{0, index};
		index = utils.find(globals, token);
		if(index >= 0) return new int[]{1, index};
		if(context != null) {
			index = context.find(token);
			if(index >= 0) return new int[]{2,index};
		} if(token.equals("this") && func != null) {
			if(func.ret.type.bytes > 0) return new int[]{3};
		} return new int[]{-1};
	}

	public int endOfText() {
		return asmOutput[current].getText().length();
	}

	public void insert(int place, String text) {
		String asm = asmOutput[current].getText();
		if(!loading) asmOutput[current].setText(
			asm.substring(0,place)+text+asm.substring(place)
		);
	}

	public void use(Context context, String item) {
		if(loading) func.use(item);
		else func.unUse(context, item);
	}

	public void clearUnused(Context context) {
		if(!loading) func.clearUnused(context);
	}

	public void parseFunctionCall(Context context, function f, boolean[] free)
	throws StopCompiling {
		// assume that function name and "(" are already parsed

		int off = 0;
		variable arg;
		Result result;
		String reg = null;

		if(!free[0] || !free[7]) asmOutln(" PUSH AF ; Save data before function call");
		if(!free[1] || !free[2]) asmOutln(" PUSH BC ; Save data before function call");
		if(!free[3] || !free[4]) asmOutln(" PUSH DE ; Save data before function call");
		if(!free[5] || !free[6]) asmOutln(" PUSH HL ; Save data before function call");

		for(int i=0; i+off < f.args.length; i++) {
			if(i > 0) in.MatchChar(',');

			arg = f.args[i+off];

			result = parseExpression(
				context, arg.type.bytes, Result.freeList(), true, f.addrs[i]
			);

			if( result.value.equals("A") ||
				result.value.equals("BC") || result.value.equals("DE")
			) asmOutln(" LD ("+arg.address+"),"+result.value);
			else if(result.bytes == 2) { // THIS ELSE SHOULD NEVER OCCUR, but just in case
				/*if(!free[6] || !free[7]) { // NO LONGER NEEDED (data is now saved before function calls)
					reg = result.unused(this, free, false);
					if(reg.equals("DE")) asmOutln(" EX DE,HL");
					else {
						asmOutln(" LD "+reg.charAt(0)+",H");
						asmOutln(" LD "+reg.charAt(1)+",L");
					}
				}*/
				asmOutln(" LD HL,"+result.value);
				asmOutln(" LD ("+arg.address+"),HL");
				/*if(!free[6] || !free[7]) { // NO LONGER NEEDED
					if(reg.equals("DE")) asmOutln(" EX DE,HL");
					else {
						asmOutln(" LD H,"+reg.charAt(0));
						asmOutln(" LD L,"+reg.charAt(1));
					}
				}*/
			} else {
				/*if(!free[0]) { // NO LONGER NEEDED
					reg = result.unused(this, free, false);
					asmOutln(" LD "+reg+",A");
				}*/
				asmOutln(" LD A,"+result.value);
				asmOutln(" LD ("+arg.address+"),A");
				//if(!free[0]) asmOutln(" LD A,"+reg); // NO LONGER NEEDED
			}
		}

		asmOutln(" CALL func_"+f.ret.name);
		use(context, f.ret.name);
		in.MatchChar(')');

		if(!free[5] || !free[6]) asmOutln(" POP HL ; Restore data after function call");
		if(!free[3] || !free[4]) asmOutln(" POP DE ; Restore data after function call");
		if(!free[1] || !free[2]) asmOutln(" POP BC ; Restore data after function call");
		if(!free[0] || !free[7]) asmOutln(" POP AF ; Restore data after function call");
	}

	private Result parseFactor(
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs
	) throws StopCompiling {

		boolean plus = false;
		boolean neg = false;
		boolean not = false;
		int refs;

		for(boolean loop = true; loop; loop = !loop) {
			if(in.getCharIf('+')) { // Just because! Though who needs to go "X = +5";
				if(in.next() == '+') ErrorMessage("Sorry, there is no '++' operator :-(");
				if(neg) ErrorMessage("Illegal use of '-' and '+' operators on the same term");
				if(plus) ErrorMessage("Illegal second use of '+' operator on the same term");

				if(!makeFun.isSelected() && loading) {
					String status = statusBar.getText();
					statusBar.setText("Insulting the programmer's coding...");
					revealMakeFunOption();
					madeFunOf = true;

					WarningMessage(
						"\nActually, I just wanted to make fun of you"+(madeFunOf?" AGAIN":"")+" for using\n"+
						"the '+' operator JUST to indicate a positive value.\n\n"+
						"Hey, perhaps there ARE people who would confuse\n"+
						"42 as being a negative number without the '+' sign!\n\n"+
						"And those people would definitely be reading code!\n\n"+
						"...but you can use it if you REALLY want to. Weirdo."
					);

					statusBar.setText(status);
				}

				loop = false;
				plus = true;
			}

			if(in.getCharIf('-')) {
				if(in.next() == '-') ErrorMessage("Sorry, there is no '--' operator :-(");
				if(plus) ErrorMessage("Illegal use of '+' and '-' operators on the same term");
				if(neg) ErrorMessage("Illegal second use of '-' operator on the same term");
				if(not) ErrorMessage("Illegal combination of '~' and '-' (-~ is okay, but not ~-)");
				loop = false;
				neg = true;
			}

			if(in.getCharIf('~')) {
				if(not) ErrorMessage("Illegal second use of '~' operator on the same term");
				loop = false;
				not = true;
			}
		}

		Result res = new Result(bytes, null);

		refs = (addrs)? -1 : 0;
		while(in.next() == '*' || in.next() == '&') {
			if(in.getCharIf('*')) refs++;
			else { in.getChar(); refs--; }
		}

		if(in.next() == '(') {
			in.mark(); in.getChar();
			if(!in.frontChar(in.next())) in.pushBack();
			else {
				if(in.getIdent().equals("byte")) res.bytes = 1;
				else if(in.Matches("word")) res.bytes = 2;
				if(res.bytes == 0) in.pushBack();
				else { in.unMark(); in.MatchChar(')'); }
			}
		}

		if(in.getCharIf('(')) { // check parenthetical stuff SEPARATELY so both can be used
			res = parseExpression(context, bytes, free, true, false);
			in.MatchChar(')');
		} else if(in.numeric()) {
			int num = in.getSingleNum();
			if(num > 65535 || (bytes == 1 && num > 255))
				WarningMessage(
					"This value is too big for its context: "+num+
					".\nIt will be rounded down for you if you continue."
				);
			if(bytes == 1) num = ((num%256)+256)%256;
			else num = ((num%65536)+65536)%65536;
			res.value = Integer.toString(num);
		} else {
			refs++;
			boolean bracket = in.getCharIf('{');
			if(bracket || in.next() == '\"') {
				String text = "";

				do {
					if(text.length() > 0)
						text += ',';
					if(in.next() == '\"')
						text += in.getString(true);
					else text += in.getWholeNum();
				} while(bracket && in.getCharIf(','));

				if(bracket) in.MatchChar('}');
				else text += ",0";

				int index = utils.find(literals, text);
				if(index < 0) {
					index = literals.length;
					literals = utils.add(literals, text);
				} res.value = "ADSMBLG_Lit_"+index;
			} else {
				variable var;
				int[] typeof = typeOf(context, in.getIdent());
				switch(typeof[0]) {
				  case 0:
					if(res.bytes == 0)
						res.bytes = funcs[typeof[1]].ret.type.bytes;
					if(in.getCharIf('(')) {
						if(funcs[typeof[1]].ret.type.bytes == 0) {
							if(noVoidCall) ErrorMessage(
								"Illegal call of a void function in an expression",
								"call to function "+funcs[typeof[1]].ret.name
							); parseFunctionCall(context, funcs[typeof[1]], free);
							if(neg || not || (refs != 1)) ErrorMessage(
								"Illegal use of the "+((neg)?'-':(not)?'~':(refs>1)?'*':'&')+
								"operator on a call to a void function",
								"call to function "+funcs[typeof[1]].ret.name
							);
							if(in.next() != ';') ErrorMessage(
								"Semicolon expected after call to a void function",
								"call to function "+funcs[typeof[1]].ret.name
							); return null;
						}
						parseFunctionCall(context, funcs[typeof[1]], free);
						res.value = funcs[typeof[1]].ret.address;
					} else res.value = "func_"+funcs[typeof[1]].ret.name;
					use(context, funcs[typeof[1]].ret.name);
					break;
				  case 1:
					var = globals[typeof[1]];
					if(res.bytes == 0)
						res.bytes = var.type.bytes;
					res.value = var.address;
					use(context, var.name);
					break;
				  case 2:
					var = context.varAt(typeof[1]);
					if(res.bytes == 0)
						res.bytes = var.type.bytes;
					res.value = var.address;
					use(context, context.nameAt(typeof[1]));
					break;
				  case 3:
					if(res.bytes == 0)
						res.bytes = func.ret.type.bytes;
					res.value = func.ret.address;
					break;
				  default: ErrorMessage("Invalid item in expression", in.token);
				} if(refs == 0) res.bytes = 2;
			}
		}

		if(refs < 0) ErrorMessage(
			"You cannot address a number or another address\n"+
			"(maybe there is an inbalance of & and * operators?)"
		);

		String reg = null;

		if(in.getCharIf('[')) {
			String save = null;
			boolean check = res.numeric();
			if(check) {
				reg = res.value;
				save = asmOutput[current].getText();
			}
			in.mark();
			boolean[] wasFree = free;
			Result wasRes = new Result(res.bytes, res.value);
			String wasText = asmOutput[current].getText();
			free = res.loadRegister(this, free);
			Result index = parseExpression(context, 0, free, true, false);
			if(check && index.numeric()) {
				in.unMark();
				asmOutput[current].setText(save);
				if(!index.value.equals("0")) {
					try {
						int value = Integer.parseInt(reg);
						value += Integer.parseInt(index.value);
						res.value = Integer.toString(value);
					} catch(NumberFormatException nfe) {
						res.value = reg+'+'+index.value;
					}
				} else res.value = reg;
			} else {
				in.pushBack();
				res = wasRes;
				asmOutput[current].setText(wasText);
				free = res.toWord(this, wasFree);
				free = res.loadRegister(this, free);
				index = parseExpression(context, 0, free, true, false);
				free = index.loadRegister(this, free);
				free = index.toWord(this, free);
				if(!res.value.equals("HL")) {
					if(!free[6] || !free[7]) {
						reg = index.unused(this, free, true);
						if(reg.equals("DE")) asmOutln(" EX DE,HL");
						else {
							asmOutln(" LD "+reg.charAt(0)+",H");
							asmOutln(" LD "+reg.charAt(1)+",L");
						}
					} asmOutln(" LD H,"+res.value.charAt(0));
					asmOutln(" LD L,"+res.value.charAt(1));
				} asmOutln(" ADD HL,"+index.value);
				if(!res.value.equals("HL")) {
					asmOutln(" LD "+res.value.charAt(0)+",H");
					asmOutln(" LD "+res.value.charAt(1)+",L");
					if(!free[6] || !free[7]) {
						if(reg.equals("DE")) asmOutln(" EX DE,HL");
						else {
							asmOutln(" LD H,"+reg.charAt(0));
							asmOutln(" LD L,"+reg.charAt(1));
						}
					}
				}
			}
			in.MatchChar(']');
		}

		in.checkForAsm(this, context);
		boolean saved = false;

		if(refs > 0) {
			if(!res.register()) {
				if(!res.reference()) {
					refs--; res.value = "("+res.value+')';
				}
				if(refs > 0) {
					free = res.toWord(this, free); // Recently added. Addresses are word values
					free = res.loadRegister(this, free);
					if(in.next() == '=') refs--;
				}
			}
			if(refs > 0) {
				while(refs-- > 0) {
					if(res.bytes == 1)
						free = res.toWord(this, free);
					if(!free[0] && !saved) {
						reg = res.unused(this, free, false);
						asmOutln(" LD "+reg+",A"); saved = true;
					}
					if(refs == 0 && bytes == 1) { // Not sure if adding this 'if' changed anything important
						asmOutln(" LD A,("+res.value+")"); // Removing it will only decrease asm efficiency
						if(free[0] && ! saved) {
							free[0] = false;
							free = Result.set(
								Result.set(free, res.value.charAt(0), true), res.value.charAt(1), true
							); res = new Result(1, "A");
						} else {
							free = Result.set(free, res.value.charAt(0), true);
							res = new Result(1, res.value.substring(1));
							asmOutln(" LD "+res.value+",A");
						}
					} else if(!res.value.equals("HL")) {
						if(!free[0]) {
							reg = res.unused(this, free, false);
							asmOutln(" LD "+reg+",A");
						}
						String reg2 = Result.unused(this, Result.set(free, reg.charAt(0), false), false);
						asmOutln(" LD A,("+res.value+")\n LD "+reg2+",A");
						asmOutln(" INC "+res.value+"\n LD A,("+res.value+")");
						asmOutln(" LD "+res.value.charAt(0)+",A");
						asmOutln(" LD "+res.value.charAt(1)+','+reg2);
					} else asmOutln(" LD A,(HL)\n INC HL\n LD H,(HL)\n LD L,A");
				}
			}
		}

		if(neg || not) {
			if(res.numeric()) {
				if(neg) {
					if(not) res.value += "+1";
					else res.value = "-("+res.value+')';
				} else res.value = "~("+res.value+')';
			} else {
				free = res.loadRegister(this, free);
				if(neg && not) asmOutln(" INC "+res.value);
				else if(res.bytes == 2) {
					if(!free[0] && !saved) {
						reg = Result.unused(this, free, false);
						asmOutln(" LD "+reg+",A"); saved = true;
					} asmOutln(" LD A,"+res.value.charAt(1));
					if(neg) asmOutln(" NEG");
					else asmOutln(" CPL");
					asmOutln(" LD "+res.value.charAt(1)+",A");
					asmOutln(" LD A,"+res.value.charAt(0)+"\n CPL");
					if(neg) asmOutln(" CCF\n ADC A,0");
					asmOutln(" LD "+res.value.charAt(0)+",A");
				} else {
					if(!res.value.equals("A")) {
						if(!free[0] && !saved) {
							reg = Result.unused(this, free, false);
							asmOutln(" LD "+reg+",A"); saved = true;
						} asmOutln(" LD A,"+res.value);
					}
					if(neg) asmOutln(" NEG");
					else asmOutln(" CPL");
					if(!res.value.equals("A"))
						asmOutln(" LD "+res.value+",A");
				}
			}
		} if(saved) asmOutln(" LD A,"+reg);
		return res;
	}

	public Result parseProduct (
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs
	) throws StopCompiling {
		return parseFactor(context, bytes, free, noVoidCall, addrs);
	}

	public Result parseSum(
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs
	) throws StopCompiling {
		Result lhs = parseProduct(context, bytes, free, noVoidCall, addrs);
		if(lhs == null) return null;

		char op;
		String add;
		Result rhs;
		boolean check;
		boolean[] copy;
		String reg = null;
		String save = null;
		in.mark();

		do {
			in.reset();
			copy = free;
			if(in.eof()) {
				in.unMark(); break;
			} op = in.getChar();
			if(op == '+') add = " ADD ";
			else if(op == '-') add = " SUB ";
			else { in.pushBack(); break; }
			check = lhs.numeric();
			if(check) {
				reg = lhs.value;
				save = asmOutput[current].getText();
			} copy = lhs.loadRegister(this, copy);
			rhs = parseProduct(context, bytes, copy, true, false);
			if(check && rhs.numeric()) {
				asmOutput[current].setText(save);
				if(!rhs.value.equals("0")) {
					try {
						int value = Integer.parseInt(reg);
						if(op == '+') value += Integer.parseInt(rhs.value);
						else value -= Integer.parseInt(rhs.value);
						lhs.value = Integer.toString(value);
					} catch(NumberFormatException nfe) {
						lhs.value = reg+'+'+rhs.value;
					}
				} else lhs.value = reg;
			} else {
				if(rhs.bytes == 2)
					copy = lhs.toWord(this, copy);
				if(
					lhs.bytes == 2 && (
						lhs.value.equals("HL") ||
						rhs.reference() || rhs.register()
					)
				) {
					if(op == '-') add = " SBC ";
					copy = rhs.loadRegister(this, copy);
					copy = rhs.toWord(this, copy);
					if(!lhs.value.equals("HL")) {
						if(!copy[6] || !copy[7]) {
							reg = rhs.unused(this, copy, true);
							if(reg.equals("DE")) asmOutln(" EX DE,HL");
							else {
								asmOutln(" LD "+reg.charAt(0)+",H");
								asmOutln(" LD "+reg.charAt(1)+",L");
							}
						} asmOutln(" LD H,"+lhs.value.charAt(0));
						asmOutln(" LD L,"+lhs.value.charAt(1));
					} asmOutln(add+"HL,"+rhs.value);
					if(!lhs.value.equals("HL")) {
						asmOutln(" LD "+lhs.value.charAt(0)+",H");
						asmOutln(" LD "+lhs.value.charAt(1)+",L");
						if(!copy[6] || !copy[7]) {
							if(reg.equals("DE")) asmOutln(" EX DE,HL");
							else {
								asmOutln(" LD H,"+reg.charAt(0));
								asmOutln(" LD L,"+reg.charAt(1));
							}
						}
					}
				} else {
					if(rhs.reference())
						copy = rhs.loadRegister(this, copy);
					if(!lhs.value.equals("A")) {
						if(!copy[0]) {
							reg = Result.unused(this, copy, false);
							asmOutln(" LD "+reg+",A");
						} asmOutln(" LD A,"+lhs.value);
					}
					for(int i=0; i<lhs.bytes; i++) {
						if(i == 1 && lhs.bytes == 2) {
							if(op == '+') add = " ADC ";
							else add = " SBC ";
						} asmOut(add);
						if(!add.equals(" SUB ")) asmOut("A,");
						asmOutln(rhs.getByte(i));
						if(lhs.bytes == 2)
							asmOutln(" LD "+lhs.getByte(i)+",A");
					}
					if(!lhs.value.equals("A")) {
						asmOutln(" LD "+lhs.value+",A");
						if(!copy[0]) asmOutln(" LD A,"+reg);
					}
				}
			}
			if(lhs.register()) {
				free = Result.set(free, lhs.value.charAt(0), false);
				if(lhs.value.length() > 1)
					free = Result.set(free, lhs.value.charAt(1), false);
			}
		} while(true);

		return lhs;
	}

	public Result parseBitShift (
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs
	) throws StopCompiling {
		return parseSum(context, bytes, free, noVoidCall, addrs);
	}

	private Result parseNextBitwiseOp (
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs, char op
	) throws StopCompiling {
		if(op == '&') return parseBitShift(context, bytes, free, noVoidCall, addrs);
		if(op == '^') return parseBitwiseOp(context, bytes, free, noVoidCall, addrs, '&');
		return parseBitwiseOp(context, bytes, free, noVoidCall, addrs, '^');
	}

	public Result parseBitwiseOp (
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs, char op
	) throws StopCompiling {

		Result lhs = parseNextBitwiseOp(context, bytes, free, noVoidCall, addrs, op);
		if(lhs == null) return null;

		Result rhs;
		boolean check;
		boolean[] copy;
		String save = null;
		String ins, reg = null;

		in.mark();

		do {
			in.reset();
			copy = free;
			if(in.eof()) {
				in.unMark();
				break;
			}
			if(in.getChar() == op) {
				if(op == '&') ins = " AND ";
				else if(op == '|') ins = " OR ";
				else ins = " XOR ";
			} else { in.pushBack(); break; }
			check = lhs.numeric();
			if(check) {
				reg = lhs.value;
				save = asmOutput[current].getText();
			} copy = lhs.loadRegister(this, copy);
			rhs = parseNextBitwiseOp(context, bytes, copy, true, false, op);
			if(check && rhs.numeric()) {
				asmOutput[current].setText(save);
				if(!rhs.value.equals("0")) {
					try {
						int value = Integer.parseInt(reg);
						if(op == '&') value &= Integer.parseInt(rhs.value);
						else if(op == '|') value |= Integer.parseInt(rhs.value);
						else value ^= Integer.parseInt(rhs.value);
						lhs.value = Integer.toString(value);
					} catch(NumberFormatException nfe) {
						lhs.value = "("+reg+')'+op+'('+rhs.value+')';
					}
				} else lhs.value = reg;
			} else {
				if(rhs.bytes == 2) copy = lhs.toWord(this, copy);
				if(lhs.value.equals("A")) asmOutln(ins+rhs.value);
				else {
					if(!copy[0]) {
						reg = rhs.unused(this, copy, false);
						asmOutln(" LD "+reg+",A");
					} for(int i=0; i<lhs.value.length(); i++) {
						asmOutln(" LD A,"+lhs.value.charAt(i));
						asmOutln(ins+rhs.getByte(i));
						asmOutln(" LD "+lhs.value.charAt(i)+",A");
					} if(!copy[0]) asmOutln(" LD A,"+reg);
				}
			}
			if(lhs.register()) {
				free = Result.set(free, lhs.value.charAt(0), false);
				if(lhs.value.length() > 1)
					free = Result.set(free, lhs.value.charAt(1), false);
			}
		} while(true);
		return lhs;
	}

	public Result parseExpression(
		Context context, int bytes, boolean[] free, boolean noVoidCall, boolean addrs
	) throws StopCompiling {
		return parseBitwiseOp(context, bytes, free, noVoidCall, addrs, '|');
	}

	public void parseCondition(
		Context context, String ins, String jpLbl, boolean swap
	) throws StopCompiling {
		String flag;
		boolean not = false;
		boolean[] free = null;
		if(in.next() == '!') {
			in.mark(); in.getChar();
			if(in.getCharIf('(')) {
				in.unMark(); not = true;
			} else in.pushBack();
		} if(in.next() >= 'C' && in.next() <= 'Z') {
			in.mark(); flag = null;
			if(in.getIdent().equals("CFLAG")) flag = "C";
			else if(in.Matches("HFLAG")) flag = "H";
			else if(in.Matches("NCFLAG")) flag = "NC";
			else if(in.Matches("NHFLAG")) flag = "NH";
			else if(in.Matches("NNFLAG")) flag = "NN";
			else if(in.Matches("NFLAG")) flag = "N";
			else if(in.Matches("NSFLAG")) flag = "NS";
			else if(in.Matches("NZFLAG")) flag = "NZ";
			else if(in.Matches("PEFLAG") || in.Matches("POFLAG")) {
				if(in.Matches("PEFLAG")^swap) flag = "PE"; // !!! NOT SURE WHICH SHOULD BE PE OR PO !!!
				else flag = "PO";                          // !!! NOT SURE WHICH SHOULD BE PE OR PO !!!
			} else if(in.Matches("SFLAG")) flag = "S";
			else if(in.Matches("ZFLAG")) flag = "Z";
			else if(in.Matches("TRUE") || in.Matches("FALSE")) {
				if(in.Matches("FALSE")^swap) {
					if(jpLbl == null) asmOutln(ins);
					else asmOutln(ins+jpLbl);
				} return;
			}

			if(flag != null) {
				in.unMark();
				if(not) in.MatchChar(')');
				if(swap == not) {
					if(flag.length() > 1) flag = flag.substring(1);
					else if(flag.charAt(0) != 'P') flag = "N"+flag; // IS THIS CORRECT? No "NNC" bugs?
				}
				if(jpLbl == null) asmOutln(ins+flag);
				else asmOutln(ins+flag+','+jpLbl);
				return;
			} in.pushBack();
		}

		Result lhs = parseExpression(context, 0, Result.freeList(), true, false);
		Result rhs, temp;

		char op = in.next();
		if(op == '=' || op == '<' || op == '>' || op == '!') {
			op = in.get();
			switch(op) { // !!!!!!!!!!!!!!!!!! NOT EVERYTHING IS SWAPPED ACCORDING TO SWAP !!!!!!!!!!!!!!!!!!!!!
			  case '=':
				in.MatchChar('='); op = 0; break;   // ==
			  case '!':
				in.MatchChar('='); op = 1; // !=
				break;
			  case '>':
			  	if(in.getCharIf('=')) op = 2; // >=
			  	else if(swap) op = 5;         // >
			  	else op = 4; break;           // <=
			  case '<':
			  	if(in.getCharIf('=')) {
			  		if(swap) op = 4;  // <=
			  		else op = 5;      // >
			  	} else op = 3; break; // <
			}

			rhs = parseExpression(context, lhs.bytes, Result.freeList(lhs), true, false);
		} else { op = 1; rhs = new Result(0,"0"); } // Compare to 0

		if(not) in.MatchChar(')');
		if(lhs.numeric() && rhs.numeric()) {
			int a = Integer.parseInt(lhs.value);
			int b = Integer.parseInt(rhs.value);
			if( (   (op==0 && a==b) || (op==3 && a<b) || (op==5 && a>b) ||
					(op==1 && a!=b) || (op==4 && a<=b) || (op==2 && a>=b)
				) == swap
			) {
				if(jpLbl == null) asmOutln(ins);
				else asmOutln(ins+jpLbl);
			}
		}

		if(rhs.value.equals("HL") || rhs.value.equals("A") && lhs.bytes != 2) {
			if(op > 1) {
				if(op < 4) op += 2;
				else op -= 2;
			} temp = rhs;
			rhs = lhs;
			lhs = temp;
		} int mid;
		if(op < 2) flag = "Z"; else flag = "C";
		if(swap^(op==0 || op==3)) flag = "N"+flag;
		free = lhs.loadRegister(this, Result.freeList(rhs));
		if(lhs.bytes == 2 || rhs.bytes == 2) {
			rhs.loadRegister(this, free);
			if(!lhs.value.equals("HL") || !rhs.value.equals("DE"))
				ErrorMessage(
					"It was determined to be impossible for the registers to be used\n" +
					"the way that they are being used in this scenario. Technically,\n" +
					"you should not even be seeing this error message!\n\n" +
					"Please report this issue so it can be fixed\n" +
					"('HL' was expected, and you got '"+lhs.value+"';" +
					" 'DE' was expected, and you got '"+rhs.value+"')"
				);
			asmOutln(" CALL cphlde"); useCpHlDe = true;
		} else {
			if(!lhs.value.equals("A"))
				ErrorMessage(
					"It was determined to be impossible for the registers to be used\n" +
					"the way that they are being used in this scenario. Technically,\n" +
					"you should not even be seeing this error message!\n\n" +
					"Please report this issue so it can be fixed\n" +
					"('A' was expected, and you got '"+lhs.value+"')"
				);
			if(rhs.register() || !rhs.reference())
				asmOutln(" CP "+rhs.value);
			else {
				String reg = null;
				if(!free[6] || !free[7]) {
					reg = rhs.unused(this, free, true);
					if(reg.equals("DE")) asmOutln(" EX DE,HL");
					else {
						asmOutln(" LD "+reg.charAt(0)+",H");
						asmOutln(" LD "+reg.charAt(1)+",L");
					}
				}
				asmOutln(" LD HL,"+rhs.value.substring(1,rhs.value.length()-1));
				asmOutln(" CP (HL)");
				if(!free[6] || !free[7]) {
					if(reg.equals("DE")) asmOutln(" EX DE,HL");
					else {
						asmOutln(" LD H,"+reg.charAt(0));
						asmOutln(" LD L,"+reg.charAt(1));
					}
				}
			}
		}

		if(op == 5) {
			mid = NewLabel();
			asmOutln(" JP C, ADSMBLG_"+mid);
			if(jpLbl == null) asmOutln(ins+"NZ");
			else asmOutln(ins+"NZ,"+jpLbl);
			PostLabel(mid,"mid-conditional-pass/fail");
		}

		if(op == 4) {
			if(jpLbl == null) {
				asmOutln(ins+'C');
				asmOutln(ins+'Z');
			} else {
				asmOutln(ins+"C,"+jpLbl);
				asmOutln(ins+"Z,"+jpLbl);
			}
		} else if(jpLbl == null) asmOutln(ins+flag);
		else asmOutln(ins+flag+','+jpLbl);
	}

	public void parseGoto(Context context) throws StopCompiling {
		String label = in.getIdent();

		if(utils.find(func.labels, in.token) == -1) {
			if(utils.find(func.newLabels, in.token) == -1)
				func.newLabels = utils.add(func.newLabels, in.token);
		}

		label = "ADSMBLG_Lbl_"+func.ret.name+'_'+label;

		if(in.frontChar(in.next())) {
			in.mark();
			if(in.getIdent().equals("if")) {
				in.unMark(); in.MatchChar('(');
				parseCondition(context, " JP ", label, true);
				in.MatchChar(')'); in.MatchChar(';'); return;
			} in.pushBack();
		}

		asmOutln(" JP "+label);
		in.MatchChar(';');
	}

	public void parseIf(Context context) throws StopCompiling {
		in.MatchChar('(');
		comment("<IF>");
		int L1 = NewLabel();
		parseCondition(context, " JP ", "ADSMBLG_"+L1, false);
		in.MatchChar(')');
		if(parseBlock(context.passIf(), true)) {
			int L2 = NewLabel();
			jump(L2,"skip else code");
			PostLabel(L1, "<ELSE>");
			parseBlock(context.passIf(), false);
			PostLabel(L2,"<End IF-ELSE>");
			return;
		} PostLabel(L1,"<End IF>");
	}

	public void parseWhile(Context context, boolean until) throws StopCompiling {
		in.MatchChar('(');
		String which;
		if(until) which = "UNTIL";
		else which = "WHILE";
		int L1 = PostNewLabel("<"+which+'>');
		int L2 = NewLabel();
		parseCondition(context, " JP ", "ADSMBLG_"+L2, until);
		in.MatchChar(')');
		parseBlock(context.passLoop(L1, L2), false);
		jump(L1, "continue "+which);
		PostLabel(L2, "<End "+which+'>');
	}

	public void parseDo(Context context) throws StopCompiling {
		String which = null;
		int L1 = PostNewLabel("<DO->");
		int top = endOfText()-2;
		int cond = NewLabel();
		int L2 = NewLabel();
		parseBlock(context.passLoop(cond, L2), false);
		if(in.getIdent().equals("until")) which = "UNTIL";
		else if(in.token.equals("while")) which = "WHILE";
		else ErrorMessage("Missing 'while' (or 'until') condition after 'do'", in.token);
		in.MatchChar('(');
		parseCondition(context, " JP ", "ADSMBLG_"+L1, which.equals("WHILE"));
		in.MatchChar(')');
		insert(top, which);
		PostLabel(L2,"<End "+which+'>');
	}

	public void parseLoop(Context context) throws StopCompiling {
		int L1 = PostNewLabel("<LOOP>");
		int L2 = NewLabel();
		parseBlock(context.passLoop(L1, L2), false);
		jump(L1,"continue LOOP");
		PostLabel(L2,"<End LOOP>");
	}

	public void parseAssignment(Context context, Result[] lhs) throws StopCompiling {
		boolean[] free = Result.freeList();
		if(lhs == null) {
			lhs = new Result[] {
				parseExpression(context, 0, free, false, false)
			}; if(lhs[0] == null) return;
			free = Result.freeList(lhs[0]);
		} else {
			for(int i=0; i<lhs.length; i++) {
				boolean[] copy = Result.freeList(lhs[i]);
				for(int j=0; j<8; j++) free[j] &= copy[j];
			}
		}

		if(!in.getCharIf('='))
			return;
		String reg = null;
		boolean saved = false;
		Result rhs = parseExpression(context, lhs[0].bytes, free, true, false);

		if(in.next() == '=') {
			Result[] copy = new Result[lhs.length+1];
			for(int i=0; i<lhs.length; i++) copy[i+1] = lhs[i];
			copy[0] = rhs; parseAssignment(context, copy); return;
		}

		for(int i=0; i<lhs.length; i++) {
			if(lhs[i].register()) {
				free = lhs[i].toWord(this, free);
				if(lhs[i].value.equals("HL")) {
					if(!rhs.numeric())
						rhs.loadRegister(this, free);
					asmOutln(" LD (HL),"+rhs.low());
					if(lhs[i].bytes == 2)
						asmOutln(" INC HL\n LD (HL),"+rhs.high());
				} else {
					if(!free[0]) {
						if(!saved) {
							reg = Result.unused(this, free, false);
							asmOutln(" LD "+reg+",A");
						} asmOutln(" LD A,"+rhs.low());
					} asmOutln(" LD ("+lhs[i].value+"),A");
					if(lhs[i].bytes == 2) {
						asmOutln(" INC "+lhs[i].value+"\n LD A,"+rhs.high());
						asmOutln(" LD ("+lhs[i].value+"),A");
					}
				}
			} else if(lhs[i].reference()) {
				rhs.loadRegister(this, free);
				if(lhs[i].bytes == 1) {
					if(!rhs.value.equals("A")) {
						if(!free[0] && !saved) {
							reg = Result.unused(this, free, false);
							asmOutln(" LD "+reg+",A");
						} asmOutln(" LD A,"+rhs.low());
					} asmOutln(" LD "+lhs[i].value+",A");
				} else asmOutln(" LD "+lhs[i].value+','+rhs.value);
			} else ErrorMessage(
				"You cannot assign a value to a number (unless you dereference it)",
				lhs[i].value
			);
		} if(saved) asmOutln(" LD A,"+reg);
	}

	public boolean parseBlock(Context context, boolean If) throws StopCompiling {
		context.returned = false;
		boolean bracket = in.getCharIf('{');

		do {
			loop: do {
				do {
					if(in.getCharIf(';')) {
						if(bracket)
							continue;
						clearUnused(context.previous);
						if(If) {
							if(!in.eof()) {
								if(in.frontChar(in.next())) {
									in.mark();
									if(in.getIdent().equals("else")) {
										in.unMark(); return true;
									} in.pushBack();
								}
							} return false;
						} return false;
					}

                    if(!in.frontChar(in.next()))
                    	continue loop;
                    in.mark();
                    if(
                    	!in.getIdent().equals("TRUE") && !in.Matches("FALSE") &&
                    	!in.Matches("SFLAG") && !in.Matches("NSFLAG") &&
                    	!in.Matches("ZFLAG") && !in.Matches("NZFLAG") &&
                    	!in.Matches("HFLAG") && !in.Matches("NHFLAG") &&
                    	!in.Matches("PEFLAG") && !in.Matches("POSFLAG") &&
                    	!in.Matches("NFLAG") && !in.Matches("NNFLAG") &&
                    	!in.Matches("CFLAG") && !in.Matches("NCFLAG")
                    ) break;
                    in.unMark();
				} while(true);
				in.pushBack();
			} while(in.checkForAsm(this, context));
			if(bracket) {
				if(in.getCharIf('}')) {
					clearUnused(context.previous);
					if(If) {
						if(!in.eof()) {
							if(in.frontChar(in.next())) {
								in.mark();
								if(in.getIdent().equals("else")) {
									in.unMark(); return true;
								} in.pushBack();
							}
						}
					} return false;
				}
			} in.mark();
			in.getToken(true);
			boolean unmark = true;
			in.checkForAsm(this, context);
			if(in.Matches("else")) {
				clearUnused(context.previous);
				if(If) return true;
				ErrorMessage("'else' found without a matching an 'if'");
			}
			if(context.returned && loading)
				WarningMessage("There are (seemingly) unreachable statements after a 'return'");
			context.returned = false;
			if(in.Matches("if")) parseIf(context);
			else if(in.Matches("while") || in.Matches("until")) {
				parseWhile(context, in.Matches("until"));
			}
			else if(in.Matches("do")) parseDo(context);
			else if(in.Matches("loop")) parseLoop(context);
			else if(in.Matches("goto")) parseGoto(context);
			else if(in.Matches("label")) {
				in.getIdent();
				if(loading) {
					if(utils.find(func.labels, in.token) >= 0)
						ErrorMessage("Duplicate '"+in.token+"' label found in function "+func.ret.name);
					func.labels = utils.add(func.labels, in.token);
				} asmOutln("ADSMBLG_Lbl_"+func.ret.name+'_'+in.token+':');
				in.MatchChar(';');
			} else if(in.Matches("continue")) {
				if(context.cont < 0)
					ErrorMessage("'continue' found outside of a loop");
				boolean nif = true;
				if(in.frontChar(in.next())) {
					in.mark();
					if(in.getIdent().equals("if")) {
						nif = false;
						in.unMark();
						in.MatchChar('(');
						parseCondition(context, " JP ", "ADSMBLG_"+context.cont, true);
						in.MatchChar(')');
					} else in.pushBack();
				} if(nif) jump(context.cont, "continue");
				in.MatchChar(';');
			} else if(in.Matches("break")) {
				if(context.brk < 0)
					ErrorMessage("'break' found outside of a loop");
				boolean nif = true;
				if(in.frontChar(in.next())) {
					in.mark();
					if(in.getIdent().equals("if")) {
						nif = false;
						in.unMark();
						in.MatchChar('(');
						parseCondition(context, " JP ", "ADSMBLG_"+context.brk, true);
						in.MatchChar(')');
					} else in.pushBack();
				} if(nif) jump(context.brk, "break");
				in.MatchChar(';');
			} else if(in.Matches("return")) {
				boolean nif = true;
				if(in.frontChar(in.next())) {
					in.mark();
					if(in.getIdent().equals("if")) {
						nif = false;
						in.unMark();
						in.MatchChar('(');
						parseCondition(context, " RET ", null, true);
						in.MatchChar(')');
					} else in.pushBack();
				} if(nif) asmOutln(" RET");
				in.MatchChar(';');
				context.returned = true;
			} else {
				String ident = in.token;
				if(Type.valid(ident, false)) {
					variable var = null;
					int[] typeof = null;
					Type type = in.getType(false);
					do {
						ident = in.getIdent();
						typeof = typeOf(context, ident);

						switch(typeof[0]) {
							case 0:
								ErrorMessage(
									"'"+ident+"' is already defined as a function",
									funcs[typeof[1]].toString()
								);
							case 1:
								ErrorMessage(
									"'"+ident+"' is already defined as a global variable",
									globals[typeof[1]].toString()
								);
							case 2:
								ErrorMessage(
									"'"+ident+"' is already defined as a local variable",
									context.varAt(typeof[1]).toString()
								);
							case 3:
								ErrorMessage("You cannot name a variable \"this\"");
						}

						String address = "$0000";
						if(!loading) address =
							"local_vars+"+newAddress(context,type.size());
						var = new variable(type, ident, address);
						context.locals = utils.add(context.locals, var);
						if(in.next() == '(')
							ErrorMessage("Syntax Error", type.toString()+ident+'(');
						if(in.next() == '=')
							parseAssignment(
								context, new Result[] { new Result(type.bytes, "("+address+')') }
							);
					} while(in.getCharIf(','));
				} else {
					unmark = false;
					in.pushBack();
					parseAssignment(context, null);
				} in.MatchChar(';');
			} if(unmark) in.unMark();
		} while(bracket);
		clearUnused(context.previous);
		if(If) {
			if(!in.eof()) {
				if(in.frontChar(in.next())) {
					in.mark();
					if(in.getIdent().equals("else")) {
						in.unMark(); return true;
					} in.pushBack();
				}
			}
		} return false;
	}

	public boolean available(Context context, int address) {
		if(memory[address].length == 0) return true;
		for(int i=0; i<memory[address].length; i++) {
			int index = utils.find(funcs, memory[address][i]);
			if(index >= 0) {
				if(
					funcs[index] != func && (
						utils.find(funcs[index].used, func.ret.name) >= 0 ||
						utils.find(func.used, funcs[index].ret.name) >= 0
					)
				) return false;
			}
		}
		if(utils.find(memory[address], func.ret.name) < 0) return true;
		int size = context.length();
		for(int i=0; i<size; i++) {
			if(
				(context.varAt(i).getAddress() <= address) && (
					context.varAt(i).getAddress() >
					address -context.varAt(i).type.size()
				)
			) return false;
		} return true;
	}

	public int newAddress(Context context, int bytes) {
		for(int i=0; i<memory.length-bytes+1; i++) {
			boolean pass = true;
			for(int j=0; j<bytes && pass; j++)
				pass = available(context, i+j);
			if(pass) {
				for(int j=0; j<bytes && pass; j++) {
					if(utils.find(memory[i+j], func.ret.name) < 0)
						memory[i+j] = utils.add(memory[i+j], func.ret.name);
				} return i;
			}
		} ErrorMessage("Ran out of usable runtime memory ("+memory.length+" bytes wasn't enough?)");
		return -1;
	}

	private int nextValue() throws StopCompiling {
		String str;
		int size = 0;
		if(in.next() == '\"') {
			str = in.getString(true);
			size = str.length()-2;
			for(int i=1; i<str.length()-1; i++) {
				char ch = str.charAt(i);
				if(ch=='\n' || ch=='\t' || ch=='\0') {
					if(i < str.length()-2) {
						str = str.substring(0,i)+"\","+(byte)ch +
							",\""+str.substring(i+1);
						i += 4;
					} else {
						str = str.substring(0,i)+"\","+(byte)ch;
						i += 2;
					}
				}
			}
		} else {
			str = null;
			int value = in.getWholeNum();
			do {
				size++;
				if(str == null) str = Integer.toString(value%256);
				else str += ","+(value%256); // z80 is LITTLE-ENDIAN
				value /= 256;
			} while(value > 0);
		}
		in.token = str;
		return size;
	}

	public String initGlobal(Type type) throws StopCompiling {
		if(type.dim == 0) {
			int size = nextValue();
			if(size > type.bytes)
				ErrorMessage("Value too big for datatype ("+type.toString()+')', in.token);
			return "0,".substring(0,2*(type.bytes-size))+in.token;
		}

		int i = 0;
		String init = "";
		in.MatchChar('{');

		while(!in.getCharIf('}')) {
			if(i > 0) {
				in.MatchChar(',');
				init += ',';
			}
			i += nextValue();
			init += in.token;
		}

		if(type.dim < 0) {
			if(type.bytes == 2 && i%2 == 1)
				ErrorMessage("The size of word arrays must be a multiple of 2", "word["+type.dim);
			type.dim = i;
		} else if(i != type.dim) ErrorMessage(
			"Incorrect amount of bytes in array/n(" +
			type.dim+"bytes expected, "+i+" bytes found)",
			"{"+init+'}'
		);

		return init;
	}

	public String parseSource() throws StopCompiling {
		boolean moreHeaders = true;
		String header = "";
		while(!in.eof()) {
			in.checkForAsm(null, null);
			if(in.getCharIf(';'))
				continue;
			if(in.getIf('#')) {
				in.getIdent();
				if(in.Matches("error"))
					ErrorMessage("#error tag:\n"+in.getString(false));
				if(in.Matches("endheader"))
					moreHeaders = false;
				else {
					if(!in.Matches("header"))
						ErrorMessage("Invalid command here: #"+in.token);
					in.getString(false);
					if(moreHeaders) header += in.token+'\n';
				}
			} else { // Do NOT "use" anything here; things get used in functions
				in.getIdent();
				if(!Type.valid(in.token, true))
					ErrorMessage("Global variable or function expected", in.token);

				variable var = new variable(in.getType(false, false), in.getIdent(), null);

				int[] typeof = typeOf(null, var.name);
				function other;

				if(typeof[0] != 0) other = null;
				else other = funcs[typeof[1]];
				if(typeof[0] == 3)
					ErrorMessage("You cannot name a variable or function 'this'");
				if(typeof[0] > 0)
					ErrorMessage(var.name+" is already defined as a global variable");
				if(in.getCharIf('(')) {
					if(var.type.dim < 0)
						ErrorMessage(
							"You must specify the size of an array. The only exception\n" +
							"is when a global array is initialized with a set of values.",
							var.type.toString()
						);
					boolean[] addrs = new boolean[0];
					variable[] args = new variable[0];
					if(in.next() != ')') {
						do {
							int size = addrs.length;
							boolean[] copy = new boolean[size+1];
							for(int i=0; i<size; i++) {
								copy[i] = addrs[i];
							} addrs = copy;
							Type type = in.getType(true);
							addrs[size] = in.getCharIf('&');
							args = utils.add(args, new variable(type, in.getIdent(), "$0000"));
							typeof = typeOf(null, in.token);
							if(typeof[0] == 0)
								ErrorMessage(in.token+" is already defined as a function");
							if(typeof[0] == 1)
								ErrorMessage(in.token+" is already defined as a global variable");
						} while(in.getChar() == ',');
					} else in.MatchChar(')');
					func = new function(var, args, addrs);
					if(in.getCharIf(';')) {
						if(other != null)
							ErrorMessage(var.name+" is already defined as a function");
						func.code = null;
						funcs = utils.add(funcs, func);
					} else {
						if(other != null) {
							if(other.code != null)
								ErrorMessage(var.name+" is already defined as a function");
							if(
								other.args.length != args.length ||
								other.addrs.length != addrs.length
							) ErrorMessage("Function "+var.name+" does not match its header");
							for(int i=0; i<args.length; i++) {
								if(!args[i].type.equals(other.args[i].type))
									ErrorMessage("Function "+var.name+" does not match its header");
								if(!args[i].name.equals(other.args[i].name))
									WarningMessage(
										"Inconsistant name for argument #"+i+" in function " +
										var.name+".\nUse the latter one ("+args[i].name+")?"
									);
							}

							for(int i=0; i<addrs.length; i++)
								if(addrs[i] != other.addrs[i])
									WarningMessage(
										"Inconsistant use of addressing (&) for "+"argument #"+i+
										"\nin function "+var.name+". Use the latter one ("+addrs[i]+")?"
									);

							other = func;
						} else funcs = utils.add(funcs, func);

						in.clear();
						Context context = new Context();
						context.locals = args;
						for(int j=0; j<func.args.length; j++) {
							if(args[j] != null) {
								func.args[j].address = "local_vars+"+newAddress(
									context, func.args[j].type.size()
								);
							}
						}

						if(func.ret.type.bytes > 0)
							func.ret.address = "local_vars+"+newAddress(context, func.ret.type.size());

						parseBlock(context, false);
						func.code = in.output.toString();
						in.clear();
					}

					for(int j=0; j<func.newLabels.length; j++)
						if(utils.find(func.labels, func.newLabels[j]) == -1)
							ErrorMessage(
								"Attempted to Goto a non-existant label\n" +
								"(\"Goto "+func.newLabels[j]+"\")"
							);

					func = null;
				} else {
					if(other != null)
						ErrorMessage(var.name+" is already defined as a function");

					boolean repeat;
					do {
						String varData = null;
						if(var.type.bytes == 0)
							ErrorMessage("Variables cannot be of the 'void' type", var.toString());
						if(!in.eof()) {
							if(in.getCharIf('@'))
								var.address = in.getString(false);
							else {
								var.address = "global_"+var.name;

								if(in.getCharIf('='))
									varData = initGlobal(var.type);
								else {
									varData = "0";
									for(int i=1; i<var.type.size(); i++)
										varData +=",0";
								}
							}
						} else {
							var.address = "global_"+var.name;
							varData = "0,0".substring(0, 1 + 2*(var.type.dim-1));
						}

						if(var.type.dim < 0)
							ErrorMessage(
								"You must specify the size of an array. The only exception\n" +
								"is when a global array is initialized with a set of values.",
								var.type.toString()
							);

						inits = utils.add(inits, varData);
						globals = utils.add(globals, var);
						repeat = in.getCharIf(',');
						if(repeat) {
							var = new variable(var.type, null, "$0000");
							var.name = in.getIdent();
							typeof = typeOf(null, var.name);

							if(typeof[0] == 0)
								if(funcs[typeof[1]].code != null)
									ErrorMessage(var.name+" is already defined as a function");

							if(typeof[0] > 0)
								ErrorMessage(var.name+" is already defined as a global variable");
						}
					} while(repeat);
					in.MatchChar(';');
				}
			}
		} return header;
	}

	private boolean[][] removeUnused(boolean[][] used, int f) {
		for(int i=0; i<funcs[f].used.length; i++) {
			int index = utils.find(globals, funcs[f].used[i]);
			if(index >= 0) used[1][index] = true;
			else {
				index = utils.find(funcs, funcs[f].used[i]);
				if(index >= 0) {
					if(!used[0][index]) {
						used[0][index] = true;
						used = removeUnused(used, index);
					}
				}
			}
		} return used;
	}

	public void removeUnused() {
		boolean[][] used = new boolean[][] {
			new boolean[funcs.length],
			new boolean[globals.length]
		};

		int index = utils.find(funcs, "main");

		if(index < 0)
			ErrorMessage("No main function was found");

		used[0][index] = true;
		used = removeUnused(used, index);

		for(int i=0; i<used[0].length; i++) {
			if(!used[0][i]) {
				boolean[] temp = new boolean[used[0].length-1];
				function[] tfunc = new function[funcs.length-1];
				for(int j=0; j<i; j++) {
					temp[j] = used[0][j];
					tfunc[j] = funcs[j];
				}
				for(int j=i; j<used[0].length-1; j++) {
					temp[j] = used[0][j+1];
					tfunc[j] = funcs[j+1];
				}
				used[0] = temp;
				funcs = tfunc;
				i--;
			}
		}

		for(int i=0; i<used[1].length; i++) {
			if(!used[1][i]) {
				boolean[] temp = new boolean[used[1].length-1];
				String[] tinits = new String[inits.length-1];
				variable[] tglob = new variable[globals.length-1];
				for(int j=0; j<i; j++) {
					temp[j] = used[1][j];
					tinits[j] = inits[j];
					tglob[j] = globals[j];
				}
				for(int j=i; j<used[1].length-1; j++) {
					temp[j] = used[1][j+1];
					tinits[j] = inits[j+1];
					tglob[j] = globals[j+1];
				}
				used[1] = temp;
				inits = tinits;
				globals = tglob;
				i--;
			}
		}
	}

	public void compile(java.io.File source) throws StopCompiling {
		boolean none = true;
		for(current=0; current<asmUsed.length; current++) {
			if(!asmUsed[current])
				continue;

			Labels = 0;
			none = false;
			useCpHlDe = false;
			useMultHE = false;
			useMultDeA = false;
			useMultDeAc = false;
			useDivHlD = false;
			useDivHlDe = false;
			inits = new String[0];
			funcs = new function[0];
			literals = new String[0];
			globals = new variable[0];
			memory = new String[1000][0];
			in = new Parser(code[current], this);

			if(in.eof()) {
				showMessage(
					"There was nothing left to compile for the "+
					asmNames[current]+" after\npreprocessing, so no [new]"+
					" code will be generated for it", JOptionPane.PLAIN_MESSAGE
				); continue;
			}

			statusBar.setText("Compiling for the "+asmNames[current]+": initial pass...");
			String saveText = asmOutput[current].getText();

			try {
				loading = true;
				String asm = parseSource();
				loading = false;
				removeUnused();
				show(current, true);

				asmOutln(
					"; =============================\n"+
					"; COMPILED WITH "+VERSION+'\n'+
					"; =============================\n"
				);

				if(asm.length() > 0) asmOutln(asm);
				int index = utils.find(funcs, "main");
				if(index < 0) ErrorMessage("No main function was found");

				if(index != 0) {
					func = funcs[0];
					funcs[0] = funcs[index];
					funcs[index] = func;
				}

				if(funcs[0].ret.type.bytes > 0)
					ErrorMessage("The main function must have a void return type");


				for(int i=0; i<funcs.length; i++) {
					func = funcs[i];

					statusBar.setText(
						"Compiling for the "+asmNames[current]+": "+func+"..."
					);

					if(func.code == null)
						ErrorMessage("Missing function body for this function:\n"+func+';');

					Context context = new Context();
					context.locals = func.args;
					in = new Parser(func.code, this);
					asmOutln("\nfunc_"+func.ret.name+':');
					parseBlock(context, false);
					if(!context.returned) asmOutln(" RET");
				}

				statusBar.setText("Outputting additional routines and variable data...");

				if(useCpHlDe) {
					asmOutln("\ncphlde:");
					asmOutln(" OR A");
					asmOutln(" SBC HL,DE");
					asmOutln(" ADD HL,DE");
					asmOutln(" RET");
				}

				if(useMultHE) {
					asmOutln("\nH_Times_E:   ; HL = H * E");
					asmOutln(" LD D,0       ; Zero D and L");
					asmOutln(" LD L,D");
					asmOutln(" LD B,8");
					asmOutln("H_Times_E_loop:");
					asmOutln(" ADD HL,HL    ; Get most-significant bit of HL");
					asmOutln(" JR NC, H_Times_E_skip");
					asmOutln(" ADD HL,DE");
					asmOutln("H_Times_E_skip:");
					asmOutln(" DJNZ H_Times_E_loop");
					asmOutln(" RET");
				}

				if(useMultDeA || useMultDeAc) {
					asmOutln("\nDE_Times_A:  ; HL = DE * A");
					asmOutln(" LD HL,0      ; Use HL to store the product");
					asmOutln(" LD B,8       ; Eight bits to check");
					asmOutln("DE_Times_A_loop:");
					asmOutln(" RRCA         ; Check least-significant bit of accumulator");
					asmOutln(" JR NC, DE_Times_A_skip  ; If zero, skip addition");
					asmOutln(" ADD    HL,DE");
					asmOutln("DE_Times_A_skip:");
					asmOutln(" SLA E        ; Shift DE one bit left");
					asmOutln("RL D");
					asmOutln(" DJNZ DE_Times_A_loop");
					asmOutln(" RET");
				}

				if(useMultDeAc) {
					asmOutln("\nDE_TIMES_AC:    ; HL = DE * AC");
					asmOutln(" CALL DE_TIMES_A ; DO HL*A");
					asmOutln(" PUSH HL         ; SAVE HL");
					asmOutln(" LD A,C          ; DO HL*C");
					asmOutln(" CALL DE_TIMES_A");
					asmOutln(" POP DE          ; Add LSB of 1st * to the MSB");
					asmOutln(" LD D,E");
					asmOutln(" LD E,0");
					asmOutln(" ADD HL,DE");
				}

				if(useDivHlD) {
					asmOutln("\nDiv_HL_D:       ; HL = HL / D, A = remainder (%)");
					asmOutln(" XOR A           ; Clear upper eight bits of AHL");
					asmOutln(" LD B,16         ; Sixteen bits in dividend");
					asmOutln("Div_HL_D_loop:");
					asmOutln(" ADD HL,HL       ; Do a SLA HL");
					asmOutln(" RLA             ; Moves upper bits of dividend into A");
					asmOutln(" JR C,Div_HL_D_overflow");
					asmOutln(" CP D            ; Check if we can subtract the divisor");
					asmOutln(" JR C,Div_HL_D_skip   ; Carry means D > A");
					asmOutln("Div_HL_D_overflow:");
					asmOutln(" SUB D           ; Subtract from dividend");
					asmOutln(" INC L           ; Set bit 0 of quotient");
					asmOutln("Div_HL_D_skip:");
					asmOutln(" DJNZ Div_HL_D_loop");
					asmOutln(" RET");
				}

				if(useDivHlDe) {
					asmOutln("\nDiv_HL_DE:      ; HL = HL / DE, BC = remainder (%)");
					asmOutln(" XOR A           ; Clear upper 16 bits of ACHL");
					asmOutln(" LD C,0");
					asmOutln(" LD B,16         ; Sixteen bits in dividend");
					asmOutln("Div_HL_DE_loop:");
					asmOutln(" ADD HL,HL       ; Do a SLA HL");
					asmOutln(" RL C            ; Moves upper bits of dividend into C");
					asmOutln(" RLA             ; and then into A (ultimately into AC)");
					asmOutln(" PUSH HL         ; Save HL");
					asmOutln(" LD H,A          ; Use HL for 16-bit operations");
					asmOutln(" LD L,C");
					asmOutln(" JR C,Div_HL_DE_overflow");
					asmOutln(" CALL CPHLDE     ; Check if we can subtract the divisor");
					asmOutln(" JR C,Div_HL_DE_skip  ; Carry means DE > AC");
					asmOutln("Div_HL_DE_overflow:");
					asmOutln(" OR A            ; Reset carry for the SBC");
					asmOutln(" SBC HL,DE       ; Subtract from the dividend");
					asmOutln(" LD A,H          ; Recycle new value back into AC");
					asmOutln(" LD C,L");
					asmOutln(" POP HL          ; Set bit 0 of quotient");
					asmOutln(" INC L");
					asmOutln(" PUSH HL");
					asmOutln("Div_HL_DE_skip:");
					asmOutln(" POP HL          ; Restore the value of HL");
					asmOutln(" DJNZ Div_HL_DE_loop");
					asmOutln(" LD B,A          ; Convert AC to BC");
					asmOutln(" RET");
				}

				for(int i=0; i<literals.length; i++)
					asmOutln("\nADSMBLG_Lit_"+i+":\n .DB "+literals[i]);

				for(int i=0; i<globals.length; i++) {
					if(inits[i] == null) continue;
					asmOut("\n"+globals[i].address+':');
					comment(globals[i].toString());
					asmOutln(" .DB "+inits[i]);
				}

				int size = 0;
				for(int i=0; i < memory.length && memory[i].length > 0; i++) {
					for(int j=0; j<memory[i].length; j++) {
						if(typeOf(null, memory[i][j])[0] != -1) {
							size = i+1; break;
						}
					}
				}
				if(size > 0) {
					asmOutln("\nlocal_vars:");
					for(int i=0; i<size; i++) {
						asmOut(" .DB ");
						for(int j=0; j<25 && i+j<size; j++) {
							if(j!=0) asmOut(","); asmOut("0");
						} if(i+25 >= size) break; asmOutln();
					}
				}

				asmOutln("\n.END\n.END");
				sourceFiles[current] = source;
				if(asmSave.isSelected()) {
					compileAsm = asmComp.isSelected();
					statusBar.setText("Saving assmbly code for the "+asmNames[current]+"...");
					java.io.File prevDir = chooser.getCurrentDirectory();
					javax.swing.filechooser.FileFilter prevFilter = chooser.getFileFilter();
					saveAs(tpane.indexOfTab(asmNames[current]));
					if(prevDir.exists()) {
						chooser.setCurrentDirectory(prevDir);
						chooser.setFileFilter(prevFilter);
					}
				}
			} catch(CompileError c) {
				asmOutput[current].setText(saveText);
				statusBar.setText("Errors were found in source code for the "+asmNames[current]+'.');
				String stop = "Stop compilating";
				if(
					JOptionPane.OK_OPTION == JOptionPane.showOptionDialog(
						this,
						"Compilation for the "+asmNames[current] +
						" has been\nstopped because errors were found", VERSION,
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
						SQUIRREL_ICON, new String[]{stop, "Next platform"}, stop
					)
				) throw new StopCompiling();
			}
		}

		in = null;
		if(none) statusBar.setText("There is nothing to compile. Did you forget to #define a platform?");
		else statusBar.setText("Compilation process completed.");
	}

	public static void main(String[] args) {
		new SquirrelBox().init(args);
	}
}