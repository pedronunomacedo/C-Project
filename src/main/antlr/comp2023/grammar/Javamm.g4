grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]*;

WS : [ \t\n\r\f]+ -> skip;

COMMENT : '//' ~[\n]* -> skip;
LINE_COMMENT: '/*' .*? '*/' -> skip;

program
    : importDeclaration* classDeclaration EOF
    ;

importDeclaration
    : 'import' ID ( '.' ID)* ';'
    ;

classDeclaration
    : 'class' ID ('extends' ID)? '{'
            (varDeclaration)*
            (methodDeclaration)*
      '}'
    ;

methodDeclaration
    : 'public' type ID  '(' (type ID (',' type ID)*)? ')' '{'
            (varDeclaration)* (statement)*
            'return' expression ';'
      '}'
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{'
            (varDeclaration)* (statement)*
      '}'
    ;

varDeclaration
    : type statement
    ;

type
    : 'int' '[' ']'
    | 'boolean'
    | 'int'
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

unary
    : op=('!' | '~') ID
    ;

expression
    : expression op=('++' | '--')                                                                                      // (1) postfixExpression
    | op=('+' | '-') expression                                                                                        // (2) unaryExpression
    | expression op=('*' | '/' | '%') expression                                                                       // (3) multiplicativeExpression
    | expression op=('+' | '-') expression                                                                             // (4) additiveExpression
    | unary op=('+' | '-' | '*' | '/') expression                                                                      // (5) notExpression (???????????)
    | expression op=('<<' | '>>' | '>>>') expression                                                                   // (6) shiftExpression
    | expression op=('<' | '>' | '<=' | '>=') expression                                                               // (7) relationalExpression
    | expression op=('==' | '!=') expression                                                                           // (8) equalityExpression
    | expression op='&' expression                                                                                     // (9) bitwiseAndExpression
    | expression op='^' expression                                                                                     // (10) bitwiseExclusiveOrExpression
    | expression op='|' expression                                                                                     // (11) bitwiseInclusiveOrExpression
    | expression op='&&' expression                                                                                    // (12) logicalAndExpression
    | expression op='||' expression                                                                                    // (13) logicalOrExpression
    | expression op='?' expression ':' expression                                                                      // (14) ternaryExpression
    | expression op=('=' | '+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '^=' | '|=' | '<<=' | '>>=' | '>>>=') expression  // (15) assignementExpression
    | expression op='.' expression                                                                                     // (16) arrayLengthAccess
    | expression op='[' expression ']'                                                                                 // (17) arrayAccess
    | 'new' 'int' '[' expression ']'                                                                                   // (18) arrayInstantiation
    | 'new' ID '(' ')'                                                                                                 // (19) classInstantiation
    | op='!' expression                                                                                                // (20) logicalNotExpression
    | '(' expression ')'                                                                                               // (21) parenthesisExpression
    | INTEGER                                                                                                          // (22) stringsExpressions
    | ID                                                                                                               // (23) identifierExpression
    | ID expression                                                                                                    // (24) identifierExperssion
    | 'this' op='.' expression                                                                                         // (25) thisExpression
    | 'true'                                                                                                           // (26) booleanTrueLiteral
    | 'false'                                                                                                          // (27) booleanFalseLiteral
    ;