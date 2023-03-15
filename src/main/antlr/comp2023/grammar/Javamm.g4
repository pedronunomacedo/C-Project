grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0] | [1-9][0-9]* ;
// ID : [_]?[a-zA-Z][a-zA-Z0-9_]*;
ID: ('a'..'z' | 'A'..'Z'|'_')(('a'..'z' | 'A'..'Z'|'0'..'9'|'_'))*;

WS : [ \t\n\r\f]+ -> skip;

COMMENT : '//' ~[\n]* -> skip;
LINE_COMMENT: '/*' .*? '*/' -> skip;

program
    : (importDeclaration)* (classDeclaration) EOF
    ;

subImportDeclaration
    : '.' id=ID                                       #SubImport
    ;

importDeclaration
    : 'import' id=ID (subImportDeclaration)* ';'      #Import
    ;

extendsClassDeclaration
    : 'extends' extendedClassName=ID                                 #ExtendedClass
    ;

classDeclaration
    : 'class' className=ID (extendsClassDeclaration)? '{'
            (varDeclaration)*
            (methodDeclaration)*
      '}'                                              #Class
    ;

classParameters
    : type value=ID
    ;



methodDeclaration
    : ('public')? returnType methodName=ID  '(' (classParameters ( ',' classParameters)*)?  ')' '{'
            (localVariables)*
            (statement)*

            'return' returnObj ';'
      '}'                                                               #MethodDeclarationOther
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{'
            (localVariables)*
            (statement)*
      '}'                                                               #MethodDeclarationMain
    ;

localVariables
    : type varName=ID ';'
    | varName=ID ('=' (ID | INT)) ';'
    ;

varType
    : type
    ;

returnType
    : type
    ;

returnObj
    : expression
    ;

varDeclaration
    : type varName=ID ';'                     #VariableDeclaration
    ;

type
    : typeName='int' '[' ']'                  #IntegerArrayType
    | typeName='int'                          #IntegerType
    | typeName='boolean'                      #BooleanType
    | typeName='String'                       #StringType
    | typeName=ID                             #IdType
    ;

statement
    : '{'
            (statement)*
      '}'                                           #ExprStmt
    | 'if' '(' expression ')'
            ((type)? statement)*
      'else'
            ((type)? statement)*                    #Conditional
    | 'while' '(' expression ')'
            (statement)*                            #Loop
    | varName=ID '[' expression ']' '=' expression ';'      #Assignment
    | varName=ID '=' expression ';'                         #Assignment
    | varDeclaration ';'                            #VarDeclare
    | expression ';'                                #ExprStmt
    ;



expression
    : '(' expression ')'                                            #ExprParentheses
    | expression '[' expression ']'                                 #Array
    | expression '.' 'length'                                       #Lenght
    | expression '.' id=ID '(' (expression (',' expression)*)? ')'  #MemberAccess
    | '!' expression                                                #UnaryOp
    | expression op=('*' | '/' | '%') expression                    #BinaryOp
    | expression op=('+' | '-') expression                          #BinaryOp
    | expression op=('<'|'<='|'>'|'>=') expression                  #BinaryOp
    | expression op='&&' expression                                 #BinaryOp
    | expression op='||' expression                                 #BinaryOp
    | 'new' 'int' '[' expression ']'                                #NewObject
    | 'new' ID '(' ')'                                              #NewObject
    | INT                                                           #Integer
    | 'true'                                                        #Bool
    | 'false'                                                       #Bool
    | ID                                                            #Identifier
    | 'this'                                                        #SelfCall
    ;
