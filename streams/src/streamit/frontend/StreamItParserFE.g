/*
 * StreamItParserFE.g: StreamIt parser producing front-end tree
 * $Id: StreamItParserFE.g,v 1.3 2002-08-22 22:16:53 dmaze Exp $
 */

header {
	package streamit.frontend;
	import streamit.frontend.nodes.*;
	import java.io.DataInputStream;
	import java.util.List;

	import java.util.ArrayList;
}

options {
	mangleLiteralPrefix = "TK_";
	//language="Cpp";
}

class StreamItParserFE extends Parser;
options {
	importVocab=StreamItLex;	// use vocab generated by lexer
	k = 4;
}

tokens {
  PRE_INCR; PRE_DECR; POST_INCR; POST_DECR;
}

{
	public static void main(String[] args)
	{
		try
		{
			DataInputStream dis = new DataInputStream(System.in);
			StreamItLex lexer = new StreamItLex(dis);
			StreamItParserFE parser = new StreamItParserFE(lexer);
			parser.program();
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}

	public FEContext getContext(Token t)
	{
		int line = t.getLine();
		if (line == 0) line = -1;
		int col = t.getColumn();
		if (col == 0) col = -1;
		return new FEContext(null, line, col);
	}
}

program	:	(stream_decl)*
		EOF
	;

stream_decl
	:	struct_decl
	|	(stream_type_decl TK_filter) => filter_decl
	|	struct_stream_decl
	;

filter_decl
	: 	stream_type_decl
		TK_filter^
		id:ID
		(param_decl_list)?
		LCURLY
		(stream_inside_decl | work_decl)*
		RCURLY!
	;

stream_type_decl
	:	data_type ARROW^ data_type
	;

struct_stream_decl
	:	stream_type_decl
		(TK_pipeline^ | TK_splitjoin^ | TK_feedbackloop^)
		id:ID
		(param_decl_list)?
		block
	;

inline_stream_decl
	:	inline_filter_decl
	|	inline_struct_stream_decl
	;

inline_filter_decl
	:	TK_filter^
		LCURLY!
		(stream_inside_decl | work_decl)*
		RCURLY!
	;

inline_struct_stream_decl
	:	(TK_pipeline^ | TK_splitjoin^ | TK_feedbackloop^)
		LCURLY!
		(stream_inside_decl)*
		RCURLY!
	;

stream_inside_decl
	:	init_decl
	|	(function_decl) => function_decl
	|	variable_decl SEMI!
	;

work_decl
	:	TK_work^ (rate_decl)* block
	;

rate_decl
	:	TK_push^ right_expr
	|	TK_pop^ right_expr
	|	TK_peek^ right_expr
	;

init_decl
	:	TK_init^  block
	;

push_statement
	:	TK_push^ LPAREN! right_expr RPAREN!
	;

statement
	:	streamit_statement
	;

streamit_statement
	:	minic_statement
	|	add_statement
	|	body_statement
	| 	loop_statement
	|	split_statement SEMI!
	|	join_statement SEMI!
	|	enqueue_statement SEMI!
	|	push_statement SEMI!
	|	print_statement SEMI!
	;

add_statement
	: TK_add^ stream_or_inline
	;

body_statement
	: TK_body^ stream_or_inline
	;

loop_statement
	: TK_loop^ stream_or_inline
	;

stream_or_inline
	: (stream_type_decl TK_filter) => stream_type_decl TK_filter^ block
	| (TK_pipeline) => TK_pipeline^ block
	| (TK_splitjoin) => TK_splitjoin^ block
	| (TK_feedbackloop) => TK_feedbackloop^ block
	| ID (LESS_THAN data_type MORE_THAN!)? (func_call_params)? SEMI!
	;

split_statement
	: TK_split^ splitter_or_joiner
	;

join_statement
	: TK_join^ splitter_or_joiner
	;

splitter_or_joiner
	: TK_roundrobin^
		(	func_call_params
		|	{ /* #splitter_or_joiner = #([TK_roundrobin, "roundrobin"], [LPAREN, "("]); */ }
		)
	| TK_duplicate (LPAREN! RPAREN!)?
	;

enqueue_statement
	: TK_enqueue^ right_expr
	;

print_statement
	:	TK_print^
		right_expr
	;

data_type
	:	(primitive_type LSQUARE) =>
			primitive_type LSQUARE^ right_expr RSQUARE!
				(LSQUARE! right_expr RSQUARE!)*
	|	primitive_type
	|	TK_void
	;

primitive_type
	:	TK_bit
	|	TK_int
	|	TK_float
	|	TK_double
	|	TK_complex
	|	id:ID
	;

global_declaration
	:	(function_decl) => function_decl
	|	variable_decl SEMI!
	|	struct_decl SEMI!
	;

variable_decl
	:	data_type id:ID^ (variable_init)?
	;

array_modifiers
	:	LSQUARE^ right_expr RSQUARE!  
		(LSQUARE! right_expr RSQUARE!)*
	;

variable_init
	:	ASSIGN^ right_expr
	;

variable_list
	:	LPAREN^ (variable_decl (COMMA! variable_decl)* )? RPAREN!
	;

function_decl
	:	data_type id:ID^
		variable_list
		block
	;

param_decl_list
	:	LPAREN^ (param_decl (COMMA! param_decl)* )? RPAREN!
	;

param_decl
	:	data_type id:ID^
	;

block
	:	LCURLY^ ( statement )* RCURLY!
	;

minic_statement
	:	block
	|	(variable_decl) => variable_decl SEMI!
	|	(expr_statement) => expr_statement SEMI!
	|	TK_break SEMI!
	|	TK_continue SEMI!
	|	TK_return^ (right_expr)? SEMI!
	|	if_else_statement
	|	TK_while^ LPAREN! right_expr RPAREN! statement
	|	TK_do^ statement TK_while! LPAREN! right_expr RPAREN!
	|	TK_for^ LPAREN! for_init_statement SEMI! right_expr SEMI!
		for_incr_statement RPAREN! statement
	;

for_init_statement
	:	(variable_decl) => variable_decl
	|	(expr_statement) => expr_statement
	;

for_incr_statement
	:	expr_statement
	;

if_else_statement
	:	TK_if^ LPAREN! right_expr RPAREN! statement
		((TK_else) => (TK_else! statement))?
	;

expr_statement
	:	(incOrDec) => incOrDec
	|	(assign_expr) => assign_expr
	|	streamit_value_expr
	;

assign_expr
	:	left_expr ((ASSIGN^ | PLUS_EQUALS^ | MINUS_EQUALS^) right_expr)?
	;

func_call_params returns [List l] { l = new ArrayList(); Expression x; }
	:	LPAREN
		(	x=right_expr { l.add(x); }
			(COMMA x=right_expr { l.add(x); })*
		)?
		RPAREN
	;

left_expr returns [Expression x] { x = null; }
	:	x=value
	;

right_expr returns [Expression x] { x = null; }
	:	x=ternaryExpr
	;

ternaryExpr returns [Expression x] { x = null; Expression b, c; }
	:	x=logicOrExpr
		(QUESTION b=ternaryExpr COLON c=ternaryExpr
			{ x = new ExprTernary(x.getContext(), ExprTernary.TEROP_COND,
					x, b, c); }
		)?
	;

logicOrExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=logicAndExpr
		(LOGIC_OR r=logicOrExpr
			{ x = new ExprBinary(x.getContext(), ExprBinary.BINOP_OR, x, r); }
		)?
	;

logicAndExpr returns [Expression x] { x = null; Expression r; }
	:	x=bitwiseExpr
		(LOGIC_AND r=logicAndExpr
			{ x = new ExprBinary(x.getContext(), ExprBinary.BINOP_AND, x, r); }
		)?
	;

bitwiseExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=equalExpr
		(	( BITWISE_OR  { o = ExprBinary.BINOP_BOR; }
			| BITWISE_AND { o = ExprBinary.BINOP_BAND; }
			| BITWISE_XOR { o = ExprBinary.BINOP_BXOR; }
			)
			r=bitwiseExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)?
	;

equalExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=compareExpr
		(	( EQUAL     { o = ExprBinary.BINOP_EQ; }
			| NOT_EQUAL { o = ExprBinary.BINOP_NEQ; }
			)
			r = equalExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)?
	;

compareExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=addExpr
		(	( LESS_THAN  { o = ExprBinary.BINOP_LT; }
			| LESS_EQUAL { o = ExprBinary.BINOP_LE; }
			| MORE_THAN  { o = ExprBinary.BINOP_GT; }
			| MORE_EQUAL { o = ExprBinary.BINOP_GE; }
			)
			r = compareExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)?
	;

addExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=multExpr
		(	( PLUS  { o = ExprBinary.BINOP_ADD; }
			| MINUS { o = ExprBinary.BINOP_SUB; }
			)
			r=addExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)?
	;

multExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=inc_dec_expr
		(	( STAR { o = ExprBinary.BINOP_MUL; }
			| DIV  { o = ExprBinary.BINOP_DIV; }
			| MOD  { o = ExprBinary.BINOP_MOD; }
			)
			r=multExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)?
	;

inc_dec_expr returns [Expression x] { x = null; }
	:	(incOrDec) => x=incOrDec
	|	x=value_expr
	;

incOrDec returns [Expression x] { x = null; }
	:	x=left_expr
		(	INCREMENT
			{ x = new ExprUnary(x.getContext(), ExprUnary.UNOP_POSTINC, x); }
		|	DECREMENT
			{ x = new ExprUnary(x.getContext(), ExprUnary.UNOP_POSTDEC, x); }
		)
	|	i:INCREMENT x=left_expr
			{ x = new ExprUnary(getContext(i), ExprUnary.UNOP_PREINC, x); }
	|	d:DECREMENT x=left_expr
			{ x = new ExprUnary(getContext(d), ExprUnary.UNOP_PREDEC, x); }
	;

value_expr returns [Expression x] { x = null; }
	:	x=minic_value_expr
	|	x=streamit_value_expr
	;

streamit_value_expr returns [Expression x] { x = null; }
	:	t:TK_pop LPAREN RPAREN
			{ x = new ExprPop(getContext(t)); }
	|	u:TK_peek LPAREN x=right_expr RPAREN
			{ x = new ExprPeek(getContext(u), x); }
	;

minic_value_expr returns [Expression x] { x = null; }
	:	LPAREN! x=right_expr RPAREN!
	|	x=value
	|	x=constantExpr
	;

value returns [Expression x] { x = null; Expression array; List l; }
	:	name:ID
		(	l=func_call_params
			{ x = new ExprFunCall(getContext(name), name.getText(), l); }
		|	{ x = new ExprVar(getContext(name), name.getText()); }
			(	DOT field:ID
				{ x = new ExprField(x.getContext(), x, field.getText()); }
			|	LSQUARE array=right_expr RSQUARE
				{ x = new ExprArray(x.getContext(), x, array); }
			)*
		)
	;

constantExpr returns [Expression x] { x = null; }
	:	n:NUMBER
			{ x = ExprConstant.createConstant(getContext(n), n.getText()); }
	|	c:CHAR_LITERAL
			{ x = new ExprConstChar(getContext(c), c.getText()); }
	|	s:STRING_LITERAL
			{ x = new ExprConstStr(getContext(s), s.getText()); }
	|	pi:TK_pi
			{ x = new ExprConstFloat(getContext(pi), Math.PI); }
	;

struct_decl
	:	TK_struct^ id:ID
		LCURLY!
		(variable_decl SEMI!)*
		RCURLY!
	;
