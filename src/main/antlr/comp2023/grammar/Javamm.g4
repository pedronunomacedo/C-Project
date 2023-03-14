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
    : '!' expression                                                #UnaryOp
    | expression op=('*' | '/' | '%') expression                    #BinaryOp
    | expression op=('+' | '-') expression                          #BinaryOp
    | expression op=('<'|'<='|'>'|'>=') expression                  #BinaryOp
    | expression op='&&' expression                                 #BinaryOp
    | expression op='||' expression                                 #BinaryOp
    | expression '[' expression ']'                                 #Array
    | expression '.' 'length'                                       #Lenght
    | expression '.' id=ID '(' (expression (',' expression)*)? ')'  #Method
    | 'new' 'int' '[' expression ']'                                #NewObject
    | 'new' ID '(' ')'                                              #NewObject
    | '(' expression ')'                                            #Expr
    | INT                                                           #Integer
    | 'true'                                                        #Bool
    | 'false'                                                       #Bool
    | ID                                                            #Identifier
    | 'this'                                                        #SelfCall
    ;
