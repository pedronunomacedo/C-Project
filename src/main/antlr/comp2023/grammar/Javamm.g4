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
    : keyType=type value=ID ((',' keyType=type value=ID)*)?          #ClassParametersDeclaration
    ;



methodDeclaration
    : ('public')? type methodName=ID  '(' classParameters? ')' '{'
            (varDeclaration)*
            (statement)*

            'return' expression ';'
      '}'                                                               #MethodDeclarationOther
    | ('public')? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{'
            (varDeclaration)*
            (statement)*
      '}'                                                               #MethodDeclarationMain
    ;

varDeclaration
    : type varName=ID ';'                    #VariableDeclaration
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
    | ID '[' expression ']' '=' expression ';'      #Assignment
    | ID '=' expression ';'                         #Assignment
    | type expression ';'                           #ExprStmt
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
    | expression '.' 'length'                                       #Method
    | expression '.' ID '(' (expression (',' expression)*)? ')'     #Method
    | 'new' 'int' '[' expression ']'                                #NewObject
    | 'new' ID '(' ')'                                              #NewObject
    | '(' expression ')'                                            #Expr
    | INT                                                           #Integer
    | 'true'                                                        #Bool
    | 'false'                                                       #Bool
    | ID                                                            #Identifier
    | 'this'                                                        #SelfCall
    ;
