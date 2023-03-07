grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]*;

WS : [ \t\n\r\f]+ -> skip;

COMMENT : '//' ~[\n]* -> skip;
LINE_COMMENT: '/*' .*? '*/' -> skip;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' first=ID ( '.' second+=ID)* ';'
    ;

classDeclaration
    : 'class' ID ('extends' ID)? '{'
            (varDeclaration)*
            (methodDeclaration)*
      '}'
    ;

methodDeclaration
    : ('public')? type ID  '(' (type ID (',' type ID)*)? ')' '{'
            (varDeclaration)* (statement)*
            'return' expression ';'
      '}'
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{'
            (varDeclaration)* (statement)*
      '}'
    ;

varDeclaration
    : type ID ';'
    ;

type
    : 'int' '[' ']'
    | 'boolean'
    | 'int'
    | 'String'
    | ID
    ;

statement
    : '{'
            (statement)*
      '}'
    | 'if' '(' expression ')'
            ((type)? statement)*
      'else'
            ((type)? statement)*
    | 'while' '(' expression ')'
            (statement)*
    | ID '[' expression ']' '=' expression ';'
    | ID '=' expression ';'
    | type expression ';'
    | expression ';'
    ;



expression
    : '!' expression #UnaryOP
    | expression op=('*' | '/' | '%') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<'|'<='|'>'|'>=') expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | expression op='||' expression #BinaryOp
    | expression '[' expression ']' #Array
    | expression '.' 'length' #Method
    | expression '.' ID '(' (expression (',' expression)*)? ')' #Method
    | 'new' 'int' '[' expression ']' #NewObject
    | 'new' ID '(' ')' #NewObject
    | '(' expression ')' #Expr
    | INT #Integer
    | 'true' #Bool
    | 'false' #Bool
    | ID #Identifier
    | 'this' #SelfCall
    ;
