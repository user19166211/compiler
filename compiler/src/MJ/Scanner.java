/* MicroJava Scanner (HM 06-12-28)
  =================
*/
package MJ;
import java.io.*;

public class Scanner {
	private static final char eofCh = '\u0080';
	private static final char eol = '\n';
	private static final int  // token codes
			none = 0,
			ident = 1,
			number = 2,
			charCon = 3,
			plus = 4,
			minus = 5,
			times = 6,
			slash = 7,
			rem = 8,
			eql = 9,
			neq = 10,
			lss = 11,
			leq = 12,
			gtr = 13,
			geq = 14,
			assign = 15,
			semicolon = 16,
			comma = 17,
			period = 18,
			lpar = 19,
			rpar = 20,
			lbrack = 21,
			rbrack = 22,
			lbrace = 23,
			rbrace = 24,
			class_ = 25,
			else_ = 26,
			final_ = 27,
			if_ = 28,
			new_ = 29,
			print_ = 30,
			program_ = 31,
			read_ = 32,
			return_ = 33,
			void_ = 34,
			while_ = 35,
			eof = 36;
	private static final String key[] = { // sorted list of keywords
			"class", "else", "final", "if", "new", "print",
			"program", "read", "return", "void", "while"
	};
	private static final int keyVal[] = {
			class_, else_, final_, if_, new_, print_,
			program_, read_, return_, void_, while_
	};

	private static char ch;          // lookahead character
	public static int col;          // current column
	public static int line;      // current line
	private static int pos;          // current position from start of source file
	private static Reader in;      // source file reader
	private static char[] lex; // current lexeme (token string)

	//----- ch = next input character
	private static void nextCh() {
		try {
			ch = (char) in.read();
			col++;
			pos++;
			if (ch == eol) {
				line++;
				col = 0;
			} else if (ch == '\uffff') ch = eofCh;
		} catch (IOException e) {
			ch = eofCh;
		}
	}

	//--------- Initialize scanner
	public static void init(Reader r) {
		in = new BufferedReader(r);
		lex = new char[64];
		line = 1;
		col = 0;
		nextCh();
	}

	//---------- Return next input token
	public static Token next() {
		while (ch <= ' ') nextCh(); // skip blanks, tabs, eols
		Token t = new Token();
		t.line = line;
		t.col = col;
		switch (ch) {
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'h':
			case 'i':
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'n':
			case 'o':
			case 'p':
			case 'q':
			case 'r':
			case 's':
			case 't':
			case 'u':
			case 'v':
			case 'w':
			case 'x':
			case 'y':
			case 'z':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
				readName(t);
				break;

			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				readNumber(t);
				break;

			case ';':
				nextCh();
				t.kind = semicolon;
				break;
			case '.':
				nextCh();
				t.kind = period;
				break;
			case ',':
				nextCh();
				t.kind = comma;
				break;
			case '+':
				nextCh();
				t.kind = plus;
				break;
			case '-':
				nextCh();
				t.kind = minus;
				break;
			case '*':
				nextCh();
				t.kind = times;
				break;
			case '%':
				nextCh();
				t.kind = rem;
				break;
			case '(':
				nextCh();
				t.kind = lpar;
				break;
			case ')':
				nextCh();
				t.kind = rpar;
				break;
			case '[':
				nextCh();
				t.kind = lbrack;
				break;
			case ']':
				nextCh();
				t.kind = rbrack;
				break;
			case '{':
				nextCh();
				t.kind = lbrace;
				break;
			case '}':
				nextCh();
				t.kind = rbrace;
				break;
			case '\'':
				nextCh();
				readCharCon(t);
				break;

			case eofCh:
				t.kind = eof;
				break; // no nextCh() any more

			case '=':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = eql;
				} else t.kind = assign;
				break;
			case '<':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = leq;
				} else t.kind = lss;
				break;
			case '>':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = geq;
				} else t.kind = gtr;
				break;
			case '!':
				nextCh();
				if (ch == '=') {
					nextCh();
					t.kind = neq;
				} else System.err.println("illegal character constants at " + t.line + ", column " + t.col);
				break;
			case '/':
				nextCh();
				if (ch == '/') {
					do nextCh(); while (ch != '\n' && ch != eofCh);
					t = next(); // call scanner recursively
				} else t.kind = slash;
				break;
			default:
				System.err.println("Unknown token at line " + t.line + ", column " + t.col);
				nextCh(); // Move to the next character
				t.kind = none; // Set the token kind to none
				break;
		}
		return t;
	}

	private static void readName(Token t) {
		t.val = "";
		while (Character.isLetter(ch) || Character.isDigit(ch) || ch == '_') {
			t.val += ch;
			nextCh();
		}
		for (int i = 0; i < key.length; i++) {
			if (t.val.equals(key[i])) {
				t.kind = keyVal[i];
				return;
			}
		}
		t.kind = ident;
	}

	private static void readNumber(Token t) {
		t.val = "";
		while (Character.isDigit(ch)) {
			t.val += ch;
			nextCh();
		}
		try {
			t.numVal = Integer.parseInt(t.val);
			t.kind = number;
		} catch (NumberFormatException e) {
			System.err.println("illegal number at line " + t.line + ", column " + t.col );
			t.kind = none;
		}
	}

	private static void readCharCon(Token t) {
		t.val = "";
		t.kind = charCon;
		t.numVal = 0;
		//nextCh();
		while (ch != '\'' && ch != eofCh && ch != '\n') {
			t.val += ch;
			nextCh();
		}

		//System.out.println(" length" + t.val.length());
		if (ch == eofCh || ch == '\n') {
			System.err.println("illegal character constants at line " + t.line + ", column " + t.col);
		} else if (t.val.length() == 1) {
			t.numVal = t.val.charAt(0);
		}else if (t.val.length() == 2 && t.val.charAt(0) == '\\') {
			if (t.val.charAt(1) == 'r') t.numVal = '\r';
			else if (t.val.charAt(1) == 'n') t.numVal = '\n';
			else if (t.val.charAt(1) == 't') t.numVal = '\t';
			else System.err.println("invalid: unknown escape sequence at line " + t.line + ", column " + t.col);
		}else if (t.val.isEmpty()) {
			System.err.println("invalid: empty character constant at line " + t.line + ", column " + t.col);
		} else System.err.println("invalid: multiple characters in character constant at line " + t.line + ", column " + t.col);
		nextCh();
		return;
	}

}

