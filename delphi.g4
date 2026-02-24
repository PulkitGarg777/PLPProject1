grammar Delphi;

program
    : PROGRAM IDENT SEMI block DOT EOF
    ;

block
    : typeSection? varSection? compoundStatement
    ;

typeSection
    : TYPE typeDecl+
    ;

typeDecl
    : IDENT EQ typeSpec SEMI
    ;

typeSpec
    : classType
    | interfaceType
    | typeId
    ;

typeId
    : IDENT
    | INTEGER
    ;

classType
    : CLASS classInheritance? classBody END
    ;

classInheritance
    : LPAREN typeIdList RPAREN
    ;

typeIdList
    : typeId (COMMA typeId)*
    ;

classBody
    : visibilitySection*
    ;

visibilitySection
    : (visibilitySpec COLON?)? classMemberDecl+
    ;

visibilitySpec
    : PUBLIC
    | PRIVATE
    | PROTECTED
    ;

classMemberDecl
    : fieldDecl
    | constructorDecl
    | destructorDecl
    | methodDecl
    ;

fieldDecl
    : identList COLON typeId SEMI
    ;

constructorDecl
    : CONSTRUCTOR IDENT formalParams? SEMI block SEMI
    ;

destructorDecl
    : DESTRUCTOR IDENT formalParams? SEMI block SEMI
    ;

methodDecl
    : PROCEDURE IDENT formalParams? SEMI block SEMI
    ;

interfaceType
    : INTERFACE interfaceBody END
    ;

interfaceBody
    : (methodSignature SEMI)*
    ;

methodSignature
    : PROCEDURE IDENT formalParams?
    ;

varSection
    : VAR varDecl+
    ;

varDecl
    : identList COLON typeId SEMI
    ;

compoundStatement
    : BEGIN statementList? END
    ;

statementList
    : statement (SEMI statement)* SEMI?
    ;

statement
    : assignment
    | procedureCall
    | compoundStatement
    | ifStatement
    | whileStatement
    | emptyStatement
    ;

emptyStatement
    :
    ;

assignment
    : variable ASSIGN expr
    ;

procedureCall
    : callExpr
    ;

ifStatement
    : IF expr THEN statement (ELSE statement)?
    ;

whileStatement
    : WHILE expr DO statement
    ;

expr
    : equalityExpr
    ;

equalityExpr
    : relationalExpr ((EQ | NEQ) relationalExpr)*
    ;

relationalExpr
    : addExpr ((LT | LE | GT | GE) addExpr)*
    ;

addExpr
    : mulExpr ((PLUS | MINUS) mulExpr)*
    ;

mulExpr
    : unaryExpr ((STAR | SLASH) unaryExpr)*
    ;

unaryExpr
    : (PLUS | MINUS)? primary
    ;

primary
    : callExpr
    | variable
    | INT
    | LPAREN expr RPAREN
    ;

callExpr
    : variable LPAREN argList? RPAREN
    ;

argList
    : expr (COMMA expr)*
    ;

variable
    : IDENT (DOT IDENT)*
    ;

identList
    : IDENT (COMMA IDENT)*
    ;

formalParams
    : LPAREN paramDecl (SEMI paramDecl)* RPAREN
    | LPAREN RPAREN
    ;

paramDecl
    : identList COLON typeId
    ;

PROGRAM: [pP][rR][oO][gG][rR][aA][mM];
TYPE: [tT][yY][pP][eE];
CLASS: [cC][lL][aA][sS][sS];
INTERFACE: [iI][nN][tT][eE][rR][fF][aA][cC][eE];
CONSTRUCTOR: [cC][oO][nN][sS][tT][rR][uU][cC][tT][oO][rR];
DESTRUCTOR: [dD][eE][sS][tT][rR][uU][cC][tT][oO][rR];
PUBLIC: [pP][uU][bB][lL][iI][cC];
PRIVATE: [pP][rR][iI][vV][aA][tT][eE];
PROTECTED: [pP][rR][oO][tT][eE][cC][tT][eE][dD];
PROCEDURE: [pP][rR][oO][cC][eE][dD][uU][rR][eE];
BEGIN: [bB][eE][gG][iI][nN];
END: [eE][nN][dD];
VAR: [vV][aA][rR];
INTEGER: [iI][nN][tT][eE][gG][eE][rR];
IF: [iI][fF];
THEN: [tT][hH][eE][nN];
ELSE: [eE][lL][sS][eE];
WHILE: [wW][hH][iI][lL][eE];
DO: [dD][oO];

ASSIGN: ':=';
EQ: '=';
NEQ: '<>';
LT: '<';
LE: '<=';
GT: '>';
GE: '>=';
PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';
DOT: '.';
COMMA: ',';
SEMI: ';';
COLON: ':';
LPAREN: '(';
RPAREN: ')';

INT: [0-9]+;
IDENT: [a-zA-Z_][a-zA-Z0-9_]*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
BRACE_COMMENT: '{' .*? '}' -> skip;
BLOCK_COMMENT: '(*' .*? '*)' -> skip;

WS: [ \t\r\n]+ -> skip;
