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

import java.io.*;

class Type {
	public int bytes; // size in bytes
	public int dim; // array dimensions

	public Type(SquirrelBoxGUI gui, String type, int d) {
		if(type.equals("byte")) bytes = 1;
		else if(type.equals("word")) bytes = 2;
		else if(type.equals("void")) bytes = 0;
		else gui.ErrorMessage(
			"Invalid datatype", type
		); dim = d;
	}

	public Type(int b, int d) {
		bytes = b; dim = d;
	}

	public int size() {
		if(bytes == 0) return 0;
		if(dim == 0) return bytes;
		return dim;
	}

	public boolean equals(Type type) { // DYNAMIC DOES NOT MATTER!
		return (type.dim == dim) && (type.bytes == bytes);
	}

	public String abrv() { // DYNAMIC DOES NOT MATTER!
		if(bytes == 0) return "v";
		String string = "";
		if(dim > 0) string = Integer.toString(dim);
		if(bytes == 1) return "b"+string;
		return "w"+string;
	}

	public String toString() {
		if(bytes == 0) return "void";
		String string = (bytes == 1)? "byte" : "word";
		if(dim == 0) return string;
		if(dim < 0) return string+"[]";
		return string+"["+dim+"]";
	}

	public static boolean valid(String type, boolean ret) {
		return type.equals("byte") || type.equals("word") || (type.equals("void") && ret);
	}
}

class variable {
	public Type type;
	public String name;
	public String address;

	public variable(Type t, String n, String a) {
		type = t; name = n; address = a;
	}

	public String toString() {
		return type.toString()+' '+name;
	}

	public int getAddress() {
		if(address.indexOf("local_vars+") == 0) {
			try { return Integer.parseInt(address.substring(11)); }
			catch(NumberFormatException nfe){}
		} return -1;
	}
}

class function {
	public variable ret;       // return type/address and name
	public variable[] args;    // arguments
	public boolean[] addrs;    // force addressing (&)
	public String code;        // function-body code
	public String[] labels;    // for use with gotos
	public String[] newLabels; // for use with gotos
	public String[] used;      // variables and functions used
	public int[] times;        // how many times each is used

	public function(variable r, variable[] a, boolean[] d) {
		ret = r; code = "";
		args = a; addrs = d;
		labels = new String[0];
		newLabels = new String[0];
		used = new String[args.length];
		times = new int[args.length];
		for(int i=0; i<args.length; i++) {
			used[i] = args[i].name;
			times[i] = 1;
		}
	}

	public void use(String item) {
		int size = used.length;
		String[] u = new String[size+1];
		int[] t = new int[size+1];
		for(int i=0; i<size; i++) {
			if(used[i].equals(item)) {
				times[i]++;
				return;
			}
			u[i] = used[i];
			t[i] = times[i];
		}
		used = u;
		times = t;
		used[size] = item;
		times[size] = 1;
	}

	public void unUse(Context context, String item) {
		int index = utils.find(used, item);
		if(index < 0) return;
		times[index]--;
		if( times[index] > 0 ||
			context.cont >= 0 || context.brk >= 0
		) return;
		int size = used.length-1;
		String[] u = new String[size];
		int[] t = new int[size];
		for(int i=0; i<index; i++) {
			u[i] = used[i];
			t[i] = times[i];
		}
		for(int i=index; i<size; i++) {
			u[i] = used[i+1];
			t[i] = times[i+1];
		} used = u; times = t;
		if(item.indexOf('.') >= 0)
			item = item.substring(item.lastIndexOf('.')+1);
		index = context.find(item);
		if(index >= 0) context.remove(index);

	}

	public void clearUnused(Context context) {
		if(context != null) {
			if(context.cont >= 0 || context.brk >= 0) return;
		} int offset = 0;
		for(int i=0; i+offset<used.length; i++) {
			if(times[i] <= 0) {
				if(i+(++offset) < used.length) {
					used[i] = used[i + offset];
					times[i] = times[i + offset];
				}
			}
		} if(offset < 1) return;
		String[] u = new String[used.length-offset];
		int[] t = new int[u.length];
		for(int i=0; i<u.length; i++) {
			u[i] = used[i];
			t[i] = times[i];
		} used = u; times = t;
	}

	public String toString() {
		String string = ret.toString()+'(';
		for(int i=0; i<args.length; i++) {
			if(i>0) string += ", ";
			string += args[i].type.toString();
		}
		return string+')';
	}
}

class CompileError extends Error {}
class StopCompiling extends Exception {}

class Parser {
	private int[] marks;
	public String token;
	public String stack;
	public String output;
	public SquirrelBoxGUI gui;

	public Parser(String data, SquirrelBoxGUI g) throws StopCompiling {
		gui = g;
		stack = data+' ';
		clear(); skipWhite();
	}

	public boolean eof() {
		return stack.length() < 1;
	}

	public void reset() {
		marks[0] = output.length();
	}

	public void clear() {
		output = ""; marks = new int[0];
	}

	public void mark() {
		int[] temp = new int[marks.length+1];
		for(int i=0; i<marks.length; i++) temp[i+1] = marks[i];
		marks = temp; reset();
	}

	public int unMark() {
		int m = marks[0];
		int[] temp = new int[marks.length-1];
		for(int i=0; i<temp.length; i++) temp[i] = marks[i+1];
		marks = temp; return m;
	}

	public void pushBack() {
		int place = unMark();
		String grab = output.substring(place);
		output = output.substring(0, place);
		stack = grab + stack;
	}

	public char next() throws StopCompiling {
		if(gui.stopCompiling) throw new StopCompiling();
		if(eof()) gui.ErrorMessage("Unexpected end of file found");
		return stack.charAt(0);
	}

	public boolean frontChar(char ch) {
		return Character.isLetter(ch) || ch=='_';
	}

	public boolean wordChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch=='_';
	}

	public char get() throws StopCompiling {
		char ch = next();
		stack = stack.substring(1);
		output += ch;
		return ch;
	}

	public boolean getIf(char ch) throws StopCompiling {
		if(next() == ch) {
			get(); return true;
		} return false;
	}

	public void skipWhite() throws StopCompiling {
		if(eof()) return;
		while(Character.isWhitespace(next())) {
			get(); if(eof()) return;
		}
	}

	public char getChar() throws StopCompiling {
		char ch = get();
		token = Character.toString(ch);
		skipWhite();
		return ch;
	}

	public boolean getCharIf(char ch) throws StopCompiling {
		if(next() == ch) {
			getChar(); return true;
		} return false;
	}

	public String getIdent() throws StopCompiling {
		if(!frontChar(next()))
			gui.ErrorMessage("Identifier expected (next char is \'"+next()+"\')");
		token = "";
		while(wordChar(next())) token += get();
		skipWhite();
		return token;
	}

	private boolean isHex(char ch) {
		ch = Character.toUpperCase(ch);
		return (ch >= 'A') && (ch <= 'F');
	}

	private int digit() throws StopCompiling {
		char ch = get();
		if(ch >= '0' && ch <= '9') return ch-'0';
		return Character.toUpperCase(ch)+10-'A';
	}

	public boolean numeric() throws StopCompiling {
		return Character.isDigit(next()) || (next() == '\'');
	}

	private int numFactor() throws StopCompiling {
		boolean plus  = false, neg = false, not = false;
		for(boolean loop = true; loop; loop = !loop) {
			if(getCharIf('+')) { // Just because! Though who needs to go "X = +5";
				if(next() == '+') gui.ErrorMessage("Sorry, there is no '++' operator :-(");
				if(neg) gui.ErrorMessage("Illegal use of '-' and '+' operators on the same term");
				if(plus) gui.ErrorMessage("Illegal second use of '+' operator on the same term");

				if(!gui.makeFun.isSelected() && gui.loading) {
					String status = gui.statusBar.getText();
					gui.statusBar.setText("Insulting the programmer's coding...");
					gui.revealMakeFunOption();
					gui.madeFunOf = true;

					gui.WarningMessage(
						"\nActually, I just wanted to make fun of you"+(gui.madeFunOf?" AGAIN":"")+" for using\n"+
						"the '+' operator JUST to indicate a positive value.\n\n"+
						"Hey, perhaps there ARE people who would confuse\n"+
						"42 as being a negative number without the '+' sign!\n\n"+
						"And those people would definitely be reading code!\n\n"+
						"...but you can use it if you REALLY want to. Weirdo."
					);

					gui.statusBar.setText(status);
				}

				loop = false;
				plus = true;
			}

			if(getCharIf('-')) {
				if(next() == '-') gui.ErrorMessage("Sorry, there is no '--' operator :-(");
				if(plus) gui.ErrorMessage("Illegal use of '+' and '-' operators on the same term");
				if(neg) gui.ErrorMessage("Illegal second use of '-' operator on the same term");
				if(not) gui.ErrorMessage("Illegal combination of '~' and '-' (-~ is okay, but not ~-)");
				loop = false;
				neg = true;
			}

			if(getCharIf('~')) {
				if(not) gui.ErrorMessage("Illegal second use of '~' operator on the same term");
				loop = false;
				not = true;
			}
		}

		if(!numeric()) gui.ErrorMessage(
			"Number expected (next char is \'"+next()+"\')"
		);

		int value;

		if(getIf('\'')) {
			value = get();
			if(value == '\\') {
				value = get();
				switch(value) {
					case '\\': case '\'': case '\"': break;
					case 't': value = '\t'; break;
					case 'n': value = '\n'; break;
					case '0': value = '\0'; break;
					default: gui.ErrorMessage(
						"Invalid escape character",
						"\'\\"+(char)value+'\''
					);
				}
			}
			MatchChar('\'');
		} else {
			if(!Character.isDigit(next())) gui.ErrorMessage(
				"Numeric value expected (next char is \'"+next()+"\')"
			);
			value = 0;
			int base = 10;
			if(getIf('0')) {
				if(next()=='b') base = 2;
				else if(next()=='x') base = 16;
				if(base != 10) get();
			}
			while(getIf('0'));
			while(
				((base==16) && isHex(next())) ||
				(Character.isDigit(next()) && ((base!=2) || next()<='1'))
			) value = value*base + digit();
			while(value < 0) value += 65536;
			value %= 65536;
		}
		skipWhite();
		token = Integer.toString(value);
		return (neg)? -value : (not)? ~value : value;
	}

	public int getSingleNum() throws StopCompiling {
		char op;
		int num = numFactor();
		mark();
		while(!eof()) {
			reset();
			op = getChar();
			if(eof()) { pushBack(); return num; }
			if(!numeric()) { pushBack(); return num; }
			switch(op) {
				case '&':
					if(next() == '&') {
						pushBack(); return num;
					} skipWhite();
					num &= numFactor(); break;
				case '|':
					if(next() == '|') {
						pushBack(); return num;
					} skipWhite();
					num |= numFactor(); break;
				case '*': num *= numFactor(); break;
				case '/': num /= numFactor(); break;
				case '%': num %= numFactor(); break;
				case '^': num ^= numFactor(); break;
				default: pushBack(); return num;
			}
		}
		unMark();
		while(num < 0) num += 65536;
		return num%65536;
	}

	public int getWholeNum() throws StopCompiling {
		char op;
		int num = 0;
		try {
			num = getSingleNum()%65536;
			mark();
			while(!eof()) {
				reset();
				op = getChar();
				if(eof()) {
					pushBack();
					token = Integer.toString(num);
					return num;
				}
				if(!numeric()) {
					pushBack();
					token = Integer.toString(num);
					return num;
				}
				switch(op) {
					case '+': num += getSingleNum(); break;
					case '-': num -= getSingleNum(); break;
					default: pushBack(); return num;
				}
				while(num < 65536) num += 65536;
				num %= 65536;
			}
			unMark();
		} catch(ArithmeticException a) {
			gui.ErrorMessage("Arithmetic exception: "+a.getMessage());
		}
		token = Integer.toString(num);
		return num;
	}

	private String append() throws StopCompiling {
		skipWhite();
		if(!eof()) {
			if(getCharIf('+')) {
				if(next() == '\"') return getString(false)+append();
				if(numeric()) return ""+getWholeNum()+append();
				gui.ErrorMessage(
					"Illegal use of '+' after a string. Only numerical valuesbe\n" +
					"appended to other strings (next char is \'"+next()+"\')"
				);
			} getCharIf('$'); // use to prevent accidental concatenation
		} return "";
	}

	public String getString(boolean withQuotes) throws StopCompiling {
		MatchNext('\"');
		token = "";
		char ch = get();
		while(ch != '\"') {
			if(ch == '\\') {
				ch = get();
				switch(ch) {
					case 't': token += '\t'; break;
					case 'n': token += '\n'; break;
					case '0': token += '\0'; break;
					case '\\': token += '\\'; break;
					case '\'': token += '\''; break;
					case '\"': token += '\"'; ch = 0; break;
					default: gui.ErrorMessage(
						"Invalid escape character", "\'\\"+ch+'\''
					);
				}
			} else token += ch;
			ch = get();
		}
		token += append();
		skipWhite();
		if(withQuotes) token = "\""+token+'\"';
		return token;
	}

	public String getToken(boolean with) throws StopCompiling {
		if(frontChar(next())) return getIdent();
		if(next()=='\"') return getString(with);
		if(Character.isDigit(next()) || next() == '\'') {
			numFactor(); return token;
		} getChar(); return token;
	}

	public Type getType(boolean start, boolean needSize) throws StopCompiling {
		if(start) getIdent();
		String type = token;
		if(!Type.valid(token,true))
			gui.ErrorMessage("Invalid datatype",type);
		int dim = 0;
		if(getCharIf('[')) {
			if(getCharIf(']')) {
				if(needSize)
					gui.ErrorMessage(
						"You must specify the size of an array. The only exception\n" +
						"is when a global array is initialized with a set of values.",
						type+"[]"
					);
				dim = -1;
			} else {
				if(type.equals("void"))
					gui.ErrorMessage("Void arrays are not allowed","void[");
				dim = getWholeNum();
				if(dim < 1) gui.ErrorMessage(
					"Array sizes must be greater than zero", type+'['+dim
				); if(type.equals("word") && (dim < 2 || dim%2 == 1)) gui.ErrorMessage(
					"The size of word arrays must be a multiple of 2", "word["+dim
				); MatchChar(']');
			}
		} skipWhite();
		return new Type(gui, type, dim);
	}

	public Type getType(boolean start) throws StopCompiling {
		return getType(start, true);
	}

	public boolean Matches(String item) {
		return token.equals(item);
	}

	public void MatchIdent(String item) throws StopCompiling {
		if(!getIdent().equals(item))
			gui.ErrorMessage("\'"+item+"\' expected", token);
	}

	public void MatchNum(String item) throws StopCompiling {
		getWholeNum();
		if(!token.equals(item))
			gui.ErrorMessage("\'"+item+"\' expected", token);
	}

	public void MatchChar(char ch) throws StopCompiling {
		if(getChar() != ch)
			gui.ErrorMessage("\'"+ch+"\' expected", token);
	}

	public void MatchNext(char ch) throws StopCompiling {
		if(get() != ch)
			gui.ErrorMessage("\'"+ch+"\' expected", token);
	}

	public boolean checkForAsm(SquirrelBox s, Context context) throws StopCompiling {
		if(eof()) return false;
		if(next() != '#') return false;
		if(!frontChar(stack.charAt(1))) return false;
		String save = token; mark(); get();
		boolean isAsm = getIdent().equals("asm");
		if(isAsm) {
			if(s == null) {
				pushBack(); token = save; return true;
			} unMark();
			Result result;
			do {
				if(next() == '\"') gui.asmOut(getString(false));
				else {
					result = s.parseExpression(
						context, 0, Result.freeList(), true, false
					); if(result.register()) gui.ErrorMessage(
						"#asm arguments must be numbers or variables/references"
					); gui.asmOut(result.value);
				}
			} while(getCharIf(','));
			gui.asmOutln();
			checkForAsm(s, context);
		} else pushBack();
		token = save;
		return isAsm;
	}
}

class Preprocessor extends Parser {
	public String copy;
	public String code;
	public File source;
	private int platform;
	private boolean valid;
	private boolean notEof;
	public String[] define;
	public String[] include;
	public String[][][] replace;

	public Preprocessor(File src, String data, SquirrelBoxGUI gui)
	throws StopCompiling {
		super(data, gui);
		copy = stack;
		source = src;
		valid = true;
		platform = -1;
		refresh();
	}

	private Preprocessor(
		File src, int plat, String data, SquirrelBoxGUI gui,
		String[] def, String[] inc, String[][][] rep, boolean val
	) throws StopCompiling {
		super(data, gui);
		valid = val;
		source = src;
		define = def;
		include = inc;
		replace = rep;
		platform = plat;
	}

	private String append() throws StopCompiling {
		skipWhite();
		if(!eof()) {
			if(next() == '+') {
				getChar();
				if(next() == '\"') return getString(false)+append();
				gui.ErrorMessage(
					"You can only add other string values to\n" +
					"a string value (next char is \'"+next()+"\')"
				);
			} if(next() == '$') getChar(); // use to prevent accidental appends
		} return "";
	}

	public String getString(boolean with) throws StopCompiling { //DONT FORMAT STRINGS!
		MatchNext('\"');
		token = "";
		char ch = get();
		while(ch != '\"') {
			token += ch;
			if(ch == '\\')
				token += get();
			ch = get();
		}
		token += append();
		if(with) token = "\""+token+'\"';
		skipWhite();
		return token;
	}

	private void refresh() {
		stack = copy;
		notEof = true;
		define = new String[0];
		include = new String[0];
		replace = new String[0][][];
	}

	private void out(boolean val, String str) {
		if(val) code += str+' ';
	}

	private boolean match(String str) throws StopCompiling {
		skipWhite(); replace();
		for(int i=0; i<str.length(); i++) {
			while(Character.isWhitespace(str.charAt(i))) {
				skipWhite();
				str = str.substring(1);
				if(i >= str.length()) return true;
			} replace();
			if(eof() && (i < str.length()-1)) return false;
			if(str.charAt(i) != next()) return false;
			get();
		} return true;
	}

	private void replace() throws StopCompiling {
		if(eof()) return;
		if(next() != '.') return;
		getChar();
		replace();
		char nextch;
		int which = -1;
		String save = stack;
		String save2, ident;
		String[] idents = null;
		boolean comma, end, good, stop = false;
		for(int i=0; (which == -1) && (i < replace.length); i++) {
			if(!match(replace[i][0][0])) stack = save;
			else if(replace[i][0].length == 1) which = i;
			else {
				idents = new String[0];
				for(int j=1; (which == -1) && (j<replace[i][0].length); j++) {
					if(replace[i][0][j].charAt(0) == '.') {
						if(!match(replace[i][0][j].substring(1))) break;
					}
					else {
						end = (j==replace[i][0].length-1);
						if(end) comma = false;
						else comma = (replace[i][0][j+1].charAt(0) != '.');
						ident = "";
						good = true;
						do {
							replace();
							if(eof()) good = false;
							else {
								if(getIf(';')) {
									replace();
									good = end;
									stop = true;
								} else if(getIf(',')) {
									replace();
									good = comma;
									stop = true;
								} else {
									replace();
									save2 = stack;
									stop = match(replace[i][0][j+1].substring(1));
									if(stop) { skipWhite(); j++; }
									else {
										replace();
										stack = save2;
										if(next() == '\"')
											ident += getString(true);
										else ident += get();
									}
								}
							}
						} while(!stop && good);
						if(!good) break;
						idents = utils.add(idents, ident);
						if(j == replace[i][0].length-1) which = i;
					}
				} if(which == -1) stack = save;
			}
		} replace();
		if(which == -1) stack = '.'+save;
		else {
			if(!token.equals(";"))
			for(int i=replace[which][1].length-1; i>=0; i--) {
				if(replace[which][1][i].charAt(0) == '.')
					stack = replace[which][1][i].substring(1)+stack;
				else
					stack = idents[(int)replace[which][1][i].charAt(0)]+stack;
			} replace();
		}
	}

	public char Get() throws StopCompiling {
		replace(); return get();
	}

	public char GetChar() throws StopCompiling {
		replace(); return getChar();
	}

	public String GetIdent() throws StopCompiling {
		replace(); return getIdent();
	}

	public String GetToken(boolean with) throws StopCompiling {
		replace(); return getToken(with);
	}

	public String GetString(boolean with) throws StopCompiling {
		replace(); return super.getString(with);
	}

	private int getPlatform() throws StopCompiling {
		if(GetIdent().equals("TI82")) return SquirrelBoxGUI.TI82;
		if(Matches("TI83")) return SquirrelBoxGUI.TI83;
		if(Matches("TI83_PLUS")) return SquirrelBoxGUI.TI83_PLUS;
		if(Matches("TI83_PLUS_SE")) return SquirrelBoxGUI.TI83_PLUS_SE;
		if(Matches("TI84_PLUS")) return SquirrelBoxGUI.TI84_PLUS;
		if(Matches("TI84_PLUS_SE")) return SquirrelBoxGUI.TI84_PLUS_SE;
		if(Matches("TI85")) return SquirrelBoxGUI.TI85;
		if(Matches("TI86")) return SquirrelBoxGUI.TI86;
		if(Matches("GENERAL_Z80")) return SquirrelBoxGUI.GENERAL_Z80;
		return -1;
	}

	private boolean same() throws StopCompiling {
		int plat = getPlatform();
		if(plat == platform) return true;
		return (platform > plat) &&
			(plat >= SquirrelBoxGUI.TI83_PLUS) &&
			(platform <= SquirrelBoxGUI.TI84_PLUS_SE) && (
				(plat != SquirrelBoxGUI.TI83_PLUS_SE) ||
				(platform != SquirrelBoxGUI.TI84_PLUS)
			);
	}

	private boolean getParam(boolean was, String[][] rep) throws StopCompiling {
		boolean address = false;
		boolean is = next() == '@';
		if(is) {
			GetChar();
			address = next() == '&';
			if(address) GetChar();
		}
		if(next() == '\"') {
			GetString(false);
			if(is) {
				if(was) token = ", \""+token+"\"$$"; // glitch removes 1st $
				else token = "#asm \""+token+"\"$$";
				if(address) gui.ErrorMessage(
					"You cannot take the address of a string in a macro"
				);
			} rep[1] = utils.add(rep[1], ". "+token);
			return is;
		}
		GetIdent();
		char count = 0;
		boolean bad = true;
		for(int i=1; i<rep[0].length && bad; i++) {
			if(rep[0][i].charAt(0) != '.') {
				bad = !rep[0][i].equals(token);
				if(!bad) {
					if(is) {
						String insert = ". ";
						if(was) insert += ',';
						else insert += "#asm ";
						if(address) insert += '&';
						rep[1] = utils.add(rep[1], insert);
					} rep[1] = utils.add(rep[1], Character.toString(count));
				}
				if(count++ > 10) gui.ErrorMessage(
					"More than 10 inputs in one macro!? Heck no!"
				);
			}
		}
		if(bad) gui.ErrorMessage(
			"non-matching identifier found\n" +
			"on right side of macro definition", token
		); return is;
	}

	private boolean preprocess(int delimit, boolean val, String fileChain) throws StopCompiling {
		notEof = true;
		boolean wasHeader = false;
		boolean noHeader = false;
		while(notEof && !eof()) {
			switch(next()) {
			  case '/':
				Get();
				if(eof()) break;
				if(next() == '/') {
					do { get(); } while(
						next() != '\n' && next() != '\r'
					); get();
				} else if(next() == '*') {
					char last;
					do { last = next(); get(); }
					while(next() != '/' || last != '*');
					get();
				} else out(val, "/");
				break;
			  case '#':
				Get(); GetIdent();
				if(Matches("eof")) {
					notEof = false;
				} else {
					if(Matches("else")) {
						if(delimit < 1) gui.ErrorMessage(
							"Mismatched #else in found somewhere in "+source
						); return true;
					}
					if(Matches("endif")) {
						if(delimit < 1) gui.ErrorMessage(
							"Mismatched #endif in found somewhere in "+source
						); return false;
					}
					if(Matches("asm")) out(val, "#asm");
					else if(Matches("error")) out(val, "#error");
					else if(Matches("header")) out(val, "#header");
					else if(Matches("endheader")) out(val, "#endheader");
					else if(Matches("include")) {
						GetString(false);
	                    if(val) {
	                        File file = new File(source.getParent(), token);
	                        if(!file.exists()) file = new File(gui.incDir, token);
	                        if(!file.exists()) file = new File(token);
	                        if(utils.find(include, file.getAbsolutePath()) >= 0) {
	                            gui.WarningMessage(
	                            	"The following file was re-included in "+
	                            	source.getName()+":\n"+file.getAbsolutePath()
	                            );
	                        } include = utils.add(include, file.getAbsolutePath());
	                        Preprocessor inner = null;
	                        try {
	                            javax.swing.JTextArea area = new javax.swing.JTextArea();
	                            area.read(new BufferedReader(new FileReader(file)), null);
	                            inner = new Preprocessor(
	                            	file, platform, area.getText(),
	                            	gui, define, include, replace, val
	                            );
	                        } catch(FileNotFoundException fne) {
	                            gui.ErrorMessage(
	                            	"Cannot find \""+token+"\".\n"+"Try using the full path name of the file.",
	                            	"#include \""+token+"\" (in "+source.getName()+')'
	                            );
	                        } catch(IOException ioe) {
	                            gui.ErrorMessage(
	                            	"Error reading from file:\n"+file.getAbsolutePath()+"\n",
	                            	"#include \""+token+"\" (in "+source.getName()+')'
	                            );
	                        } inner.preprocess(fileChain);
	                        if(val) {
	                            define = inner.define;
	                            include = inner.include;
	                            replace = inner.replace;
	                        } out(val, gui.code[platform]);
	                    }
					} else if(Matches("define")) {
						if(next()=='\"') {
							String[][] rep = new String[][]{
								new String[]{GetString(false)}, new String[0]
							}; while(next() == ',') {
								GetChar();
								if(next() == '\"') token = "."+GetString(false);
								else {
									GetIdent();
									for(int i=1; i<rep[0].length; i++) {
										if(rep[0][i].equals(token)) gui.ErrorMessage(
											"identifier was used more than once\n" +
											"on the left side of a macro definition",
											token
										);
									}
								} rep[0] = utils.add(rep[0], token);
							}

							if(!GetIdent().equals("as")) gui.ErrorMessage(
								"\"as\" expected after #define", token
							);

							boolean last = getParam(false, rep);
							while(next() == ',') {
								GetChar();
								last = getParam(last, rep);
								if(eof()) break;
							} replace = utils.add(replace, rep);
						} else {
							do {
								int plat = getPlatform();
								if(val) {
									if(plat >= 0) gui.asmUsed[plat] = true;
									else {
										if(utils.find(define, token) >= 0)
											gui.ErrorMessage(token+" was already defined", "#define "+token);
										define = utils.add(define, token);
									}
								}
							} while(getCharIf(','));
						}
					} else if(Matches("ifdef") || Matches("ifndef")) {
						boolean def = token.charAt(2)!='n';
						boolean pass = same() || (utils.find(define, token)>=0);
						while(next()==',') {
							GetChar();
							pass = pass || same() || utils.find(define, token)>=0;
						} pass = (def == pass);
						String[] _def = define;
						String[] _inc = include;
						String[][][] _rep = replace;
						def = preprocess(delimit+1, val && pass, fileChain);
						if(!(val && pass)) {
							define = _def;
							include = _inc;
							replace = _rep;
						} if(def) {
							_def = define;
							_inc = include;
							_rep = replace;
							if(preprocess(delimit+1, val && !pass, fileChain))
								gui.ErrorMessage("#else where #endif expected in "+source);
							if(!val || pass) {
								define = _def;
								include = _inc;
								replace = _rep;
							}
						}
					} else gui.ErrorMessage("Not a valid preprocessor command", "#"+token);
				} break;
			default:
				GetToken(true);
				if(!wordChar(token.charAt(0)) && token.length()==1) {
					if(val) code += token;
				} else out(val, token);
			}
		} if(delimit > 0) gui.ErrorMessage(
			"Missing "+delimit+" #endif(s) somewhere in "+source
		); return false;
	}

	public void preprocess(String fileChain) throws StopCompiling {
		if(platform >= 0) {
			if(fileChain == null) fileChain = source.getName();
			else fileChain += " --> "+source.getName();
			gui.statusBar.setText(
				"Preprocessor: searching for "+gui.asmNames[platform]+" code:  "+fileChain+"..."
			); code = "";
			if(preprocess(0, valid, fileChain)) gui.ErrorMessage(
				"Mismatched #else in found somewhere in "+source
			); if(!eof() && notEof) gui.ErrorMessage(
				"Mismatched #endif found somewhere in "+source
			); gui.code[platform] = code;
		} else {
			for(platform=0; platform<gui.asmUsed.length; platform++) {
				refresh(); preprocess(fileChain);
			} for(platform=0; platform<gui.asmUsed.length; platform++) {
				if(!gui.asmUsed[platform]) gui.code[platform] = null;
			}
		}
	}
}

class utils {
	public static int[] add(int[] list, int thing) {
		int[] temp = new int[list.length+1];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static File[] add(File[] list, File thing) {
		File[] temp = new File[list.length+1];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static String[] add(String[] list, String thing) {
		String[] temp = new String[list.length+1];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static String[][] add(String[][] list, String[] thing) {
		String[][] temp = new String[list.length+1][];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static String[][][] add(String[][][] list, String[][] thing) {
		String[][][] temp = new String[list.length+1][][];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static function[] add(function[] list, function thing) {
		function[] temp = new function[list.length+1];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static variable[] add(variable[] list, variable thing) {
		variable[] temp = new variable[list.length+1];
		for(int i=0; i<list.length; i++)
			temp[i] = list[i];
		temp[list.length] = thing;
		return temp;
	}

	public static int find(String[] list, String thing) {
		for(int i=0; i<list.length; i++)
			if(list[i].equals(thing)) return i;
		return -1;
	}

	public static int find(variable[] list, String thing) {
		for(int i=0; i<list.length; i++)
			if(list[i].name.equals(thing)) return i;
		return -1;
	}

	public static int find(function[] list, String thing) {
		for(int i=0; i<list.length; i++)
			if(list[i].ret.name.equals(thing)) return i;
		return -1;
	}
}