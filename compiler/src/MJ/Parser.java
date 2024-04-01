/*  MicroJava Parser (HM 06-12-28)
    ================
*/
package MJ;

import java.util.*;
import MJ.SymTab.*;
//import MJ.CodeGen.*;



import javax.xml.stream.FactoryConfigurationError;

public class Parser {
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		assign    = 15,
		semicolon = 16,
		comma     = 17,
		period    = 18,
		lpar      = 19,
		rpar      = 20,
		lbrack    = 21,
		rbrack    = 22,
		lbrace    = 23,
		rbrace    = 24,
		class_    = 25,
		else_     = 26,
		final_    = 27,
		if_       = 28,
		new_      = 29,
		print_    = 30,
		program_  = 31,
		read_     = 32,
		return_   = 33,
		void_     = 34,
		while_    = 35,
		eof       = 36;
	private static final String[] name = { // token names for error messages
		"none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
		"==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
		"[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while", "eof"
		};

	private static Token t;			// current token (recently recognized)
	private static Token la;		// lookahead token
	private static int sym;			// always contains la.kind
	public  static int errors;  // error counter
	private static int errDist;	// no. of correctly recognized tokens since last error

	private static BitSet exprStart, statStart, statSync, statSeqFollow, declStart, declFollow;

	private static Obj curMethod;

	//------------------- auxiliary methods ----------------------
	private static void scan() {
		t = la;
		la = Scanner.next();
		sym = la.kind;
		errDist++;
/*
		System.out.print("line " + la.line + ", col " + la.col + ": " + name[sym]);
		if (sym == ident) System.out.print(" (" + la.val + ")");
		if (sym == number || sym == charCon) System.out.print(" (" + la.numVal + ")");
		System.out.println();*/
	}

	private static void check(int expected) {
		if (sym == expected) scan();
		else error(name[expected] + " expected");
	}

	public static void error(String msg) { // syntactic error at token la
		if (errDist >= 3) {
			System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
			errors++;
		}
		errDist = 0;
	}

	//-------------- parsing methods (in alphabetical order) -----------------

	// Program = "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
	private static void Program() {
		check(program_);
		check(ident);
		Tab.openScope();
		while(sym == final_ || sym == class_ || sym == ident){
			if (sym == final_) ConsDecl();
			else if (sym == class_) ClassDecl();
			else if (sym == ident) VarDecl();
			else error("invalid program");
		}
		check(lbrace);
		while (sym == ident || sym == void_){
			MethodDecl();
		}
		check(rbrace);
		Tab.dumpScope(Tab.curScope.locals);
		Tab.closeScope();
	}

	//ConsDecl() = "final" Type ident "=" (number | charConst) ";"
	private static void ConsDecl(){
		check(final_);
		Struct type = Type();
		check(ident);
		Obj obj = Tab.insert(Obj.Con, t.val, type);
		check(assign);
		if (sym == number) {
			scan();
			obj.val = t.numVal;
			if (type != Tab.intType) error("cannot initialize with a number");
		} else if (sym == charCon) {
			scan();
			obj.val = t.numVal;
			if (type != Tab.charType) error("cannot initialize with a char constant");
		} else error("number or charCon expected");
		check(semicolon);
	}

	//ClassDecl = "class" ident "{" {VarDecl} "}".
	private static void ClassDecl(){
		Struct type = new Struct(Struct.Class);
		check(class_);
		check(ident);
		Tab.insert(Obj.Type, t.val, type);
		Tab.openScope();
		check(lbrace);
		while(sym == ident) VarDecl();
		check(rbrace);
		type.fields = Tab.curScope.locals;
		type.nFields = Tab.curScope.nVars;
		Tab.closeScope();
	}

	//VarDecl() = Type ident {"," ident } ";"
	private static void VarDecl(){
		Struct type = Type();
		check(ident);
		Tab.insert(Obj.Var, t.val, type);
		while(sym == comma){
			scan();
			check(ident);
			Tab.insert(Obj.Var, t.val, type);
		}
		check(semicolon);
	}

	//MethodDecl() = (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block
	private static void MethodDecl(){
		Struct type = Tab.noType;
		if(sym == ident) {
			type = Type();
		}
		else if (sym == void_) scan();
		else error("incompatible type in assigment");
		check(ident);

		curMethod = Tab.insert(Obj.Meth, t.val, type);
		Tab.openScope();

		check(lpar);
		if(sym == ident) FormPars();
		check(rpar);
		while(sym == ident) VarDecl();
		Block();
		curMethod.locals = Tab.curScope.locals;
		Tab.closeScope();
	}

	//Type() = ident ["[" "]"]
	private static Struct Type(){
		check(ident);
		Obj obj = Tab.find(t.val);
		Struct type = obj.type;
		if(sym == lbrack){
			scan();
			check(rbrack);
			type = new Struct(Struct.Arr,type);
		}
		return type;
	}

	//FormPars() = Type ident {"," Type ident}
	private static void FormPars(){
		Struct type = Type();
		check(ident);
		Tab.insert(Obj.Var, t.val, type);
		while(sym == comma){
			scan();
			type = Type();
			check(ident);
			Tab.insert(Obj.Var, t.val, type);
		}
	}

	//Block() =  "{" {Statement} "}".
	private static void Block(){
		check(lbrace);
		while(sym != rbrace) Statement();
		check(rbrace);
	}

	// Statement() = Designator ("=" Expr | ActPars) ";"
	//				| "if" "(" Condition ")" Statement ["else" Statement]
	//				| "while" "(" Condition ")" Statement
	//				| "return" [Expr] ";"
	//				| "read" "(" Designator ")" ";"
	//				| "print" "(" Expr ["," number] ")" ";"
	//				| Block
	//				| ";"

	private static void Statement(){
		//System.out.println("Stat beg --");
		if (!statStart.get(sym)) {
			error("illegal start of statement");
			do {
				scan();
			} while (!statSync.get(sym));
			//System.out.println("recovered with " + sym);
			errDist = 0;
		}

		// Designator ("=" Expr | ActPars) ";"
		if (sym == ident) {
			Designator();
			//System.out.println("sym = " + sym);
			if (sym == assign) {
				scan();
				Expr();
			}
			else if (sym == lpar) ActPars();
			else error("invalid assignment or call");
			check(semicolon);
		}

		//	| "if" "(" Condition ")" Statement ["else" Statement]
		else if (sym == if_) {
			scan();
			check(lpar);
			Condition();
			check(rpar);
			Statement();
			if (sym == else_){
				scan();
				Statement();
			}
		}
		//  | "while" "(" Condition ")" Statement
		else if (sym == while_){
			scan();
			check(lpar);
			Condition();
			check(rpar);
			Statement();
		}

		//  | "return" [Expr] ";"
		else if (sym == return_){
			scan();
			if (exprStart.get(sym)) Expr();
			check(semicolon);
		}

		//   | "read" "(" Designator ")" ";"
		/*else if (sym == read_){
			scan();
			check(lpar);
			Designator();
			check(rpar);
			check(semicolon);
		}*/
		else if (sym == read_){
			scan();
			check(lpar);
			if (sym != ident) {
				error("invalid expression");
			}
			else {
				scan();
			}
			check(rpar);
			check(semicolon);
		}

		//	| "print" "(" Expr ["," number] ")" ";"
		else if (sym == print_){
			scan();
			check(lpar);
			Expr();
			if (sym == comma) {
				scan();
				check(number);
			}
			check(rpar);
			check(semicolon);
		}

		// | Block
		else if (sym == lbrace) Block();

		// | ";"
		else if (sym == semicolon) scan();

		//System.out.println("-- end Stat");
		else error ("invalid expression");
	}

	/*private static Struct Type(){
		check(ident);
		Obj obj = Tab.find(t.val);
		Struct type = obj.type;
		if(sym == lbrack){
			scan();
			check(rbrack);
			type = new Struct(Struct.Arr,type);
		}
		return type;
	}*/

	// Designator() = ident {"." ident | "[" Expr "]"}
	private static void Designator(){
		check(ident);
		Obj obj = Tab.find(t.val);
		while (true){
			if (sym == period) {
				scan();
				check(ident);
			}
			else if (sym == lbrack) {
				scan();
				Expr();
				check(rbrack);
			}
			else break;
		}
	}

	// Expr() =  ["-"] Term {Addop Term}
	// Addop = "+" | "-".
	// Term = Factor {Mulop Factor}
	private static void Expr(){
		if (sym == minus) scan();
		Term();
		while (sym == plus || sym == minus){
			Addop();
			Term();
		}
	}

	// ActPars = "(" [ Expr {"," Expr} ] ")"
	private static void ActPars(){
		check(lpar);
		if (exprStart.get(sym)){
			while (sym == comma) {
				if (exprStart.get(sym)) Expr();
			}
		}
		else error("invalid input for ActPars");
		check(rpar);
	}

	// Condition() = Expr Relop Expr.
	private static void Condition(){
		Expr();
		Relop();
		Expr();
	}

	// Term() = Factor {Mulop Factor}
	// Mulop = "*" | "/" | "%".

	private static void Term(){
		Factor();
		while (sym == times || sym == slash || sym == rem){
			scan();
			Mulop();
			Factor();
		}
	}

	// Addop() = "+" | "-"
	private static void Addop(){
		if (sym == plus) scan();
		else if (sym == minus) scan();
	}

	// Relop() = "==" | "!=" | ">" | ">=" | "<" | "<=".
	private static void Relop(){
		if (sym == eql) scan();
		else if (sym == neq) scan();
		else if (sym == gtr) scan();
		else if (sym == geq) scan();
		else if (sym == lss) scan();
		else if (sym == leq) scan();
	}

	// Factor() = Designator [ActPars]
	//			| number
	//			| charConst
	//			| "new" ident ["[" Expr "]"]
	//			| "(" Expr ")"

	private static void Factor(){
		if (sym == ident) {
			Designator();
			if (sym == lpar) ActPars();
		}
		else if (sym == number) scan();
		else if (sym == charCon) scan();
		else if (sym == new_){
			scan();
			check(ident);
			Obj obj = Tab.find(t.val);
			if (sym == lbrack){
				scan();
				Expr();
				check(rbrack);
			}
		}
		else if (sym == lpar) {
			scan();
			Expr();
			check(rpar);
		}
		else error ("invalid factor");
	}

	// Mulop() = "*" | "/" | "%".
	private static void Mulop(){
		if (sym == times) scan();
		else if (sym == slash) scan();
		else if (sym == rem) scan();
	}


	//Designator = ident {"." ident | "[" Expr "]"}.
	public static void parse() {
		// initialize symbol sets
		BitSet s;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		statSync = (BitSet) statStart.clone();
		statSync.clear(ident);
		statSync.set(rbrace);
		statSync.set(eof);

		s = new BitSet(64); statSeqFollow = s;
		s.set(rbrace); s.set(eof);

		s = new BitSet(64); declStart = s;
		s.set(final_); s.set(ident); s.set(class_);

		s = new BitSet(64); declFollow = s;
		s.set(lbrace); s.set(void_); s.set(eof);

		// start parsing
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}
}

