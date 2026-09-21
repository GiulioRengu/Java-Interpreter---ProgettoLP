package projectLabo.parser;

public enum TokenType {
	// used internally by the tokenizer, should never be accessed by the parser
	KEYWORD, SKIP, SYMBOL,
	// non singleton categories
	IDENT, NUM,
	// end-of-file
	EOF,
	// symbols
	ASSIGN, CLOSE_BLOCK, CLOSE_PAR, EQ, MINUS, OPEN_BLOCK, OPEN_PAR, PAIR_OP, PLUS, STMT_SEP, TIMES, AND, NOT,
	// keywords
	BOOL, ELSE, FST, IF, PRINT, SND, VAR, ASSERT,

	//simboli progetto
	CAT, ZIP, FLATTEN, OPEN_VECT, CLOSE_VECT, AT,
	//keywords progetto
	FOR, IN, REV,
}
