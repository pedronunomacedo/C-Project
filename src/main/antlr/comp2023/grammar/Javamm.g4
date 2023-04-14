grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0] | [1-9][0-9]* ;
ID : [a-zA-Z$_][a-zA-Z0-9_]*;

WS : [ \t\n\r\f]+ -> skip;

COMMENT : '//' ~[\n]* -> skip;
LINE_COMMENT: '/*' .*? '*/' -> skip;

// STRING: '"' .*? '"';

program
    : (importDeclaration)* (classDeclaration) EOF
    ;

subImportDeclaration
    : '.' id=ID                                       #SubImport
    ;

importDeclaration
    : 'import' id=ID (subImportDeclaration)* ('.' '*')? ';'      #Import
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
    : ('public' | 'private' | 'static')? returnType methodName=ID  '(' (classParameters ( ',' classParameters)*)?  ')' '{'
            (localVariables)*
            (statement)*

            ('return' returnObj ';')?
      '}'                                                               #MethodDeclarationOther
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{'
            (localVariables)*
            (statement)*
      '}'                                                               #MethodDeclarationMain
    ;

localVariables
    : type varName=ID ('=' val2=(ID | INT))? ';'
    | varName=ID ('=' val=(ID | INT)) ';'
    ;
/*
localVariables
    : type varName=ID ';'
    | varName=ID ('=' returnObj) ';'
    ;
*/

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
    | typeName='void'                         #VoidType
    | typeName=ID                             #IdType
    ;

statement
    : '{'
            (statement)*
      '}'                                                   #Brackets
    | 'if' '(' expression ')'
            ((type)? statement)*
      'else'
            ((type)? statement)*                            #Conditional
    | 'while' '(' expression ')'
            (statement)*                                    #Loop
    | varName=ID '[' expression ']' '=' expression ';'      #ArrayAssignment
    | varName=ID '=' expression ';'                         #Assignment
    | varDeclaration                                        #VarDeclar
    | expression ';'                                        #Expr
    ;



expression
    : '(' expression ')'                                            #ExprParentheses
    | expression '[' expression ']'                                 #Array
    | expression '.' 'length'                                       #Lenght
    | expression '.' id=ID '(' ( expression (',' expression)*)? ')' #MemberAccess
    | '!' expression                                                #UnaryOp
    | expression op=('*' | '/' | '%') expression                    #BinaryOp
    | expression op=('+' | '-') expression                          #BinaryOp
    | expression op=('<'|'<='|'>'|'>=') expression                  #BinaryOp
    | expression op='&&' expression                                 #BinaryOp
    | expression op='||' expression                                 #BinaryOp
    | 'new' 'int' '[' expression ']'                                #NewArrayObject
    | 'new' val=ID '(' ')'                                          #NewObject
    | val=INT                                                       #Integer
    | val='true'                                                    #Bool
    | val='false'                                                   #Bool
    | val='this'                                                    #SelfCall
    | val=ID                                                        #Identifier
    ;
