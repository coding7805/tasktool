grammar Tasktool;
options { caseInsensitive = true; }
@header {package generated;}

commands: (command SEMI)* EOF;

command
    : cmd_logon          #CmdLogon
    | cmd_logoff         #CmdLogoff
    | cmd_exit           #CmdExit
    | cmd_excsql  		 #CmdExcSql
    | cmd_if             #CmdIf
    | cmd_for            #CmdFor
	| cmd_set            #CmdSet
	| cmd_os             #CmdOs
	| cmd_print          #CmdPrint
	| cmd_help           #CmdHelp
	| cmd_try            #CmdTry

;

cmd_help
    : '.HELP'
    ;
cmd_logon: '.LOGON' instances
             user
             password
             ;
instances: instance (',' instance)* ;
instance:  ip=IP_ADDRESS ':' port=POSITIVE_NUMBERS('/' POSITIVE_NUMBERS)* ;
user:      USER STRING_LITERAL;
password:  PASSWORD STRING_LITERAL;

cmd_excsql
    : do_statement
	| get_statement
	| split_batch_statement
	| run_batch_statement
	;
batchId
    : POSITIVE_NUMBERS ;
columnName
    : ID;
tableName
    : (ID DOT)? ID ;
processId
    : 'PROCID' '='? STRING_LITERAL ;
groupByExpr
    : STRING_LITERAL ;
hintStatement
    : STRING_LITERAL ;
whereStatement
    : STRING_LITERAL ;

split_batch_statement
    : '.SQL' BATCH RESET? (WITH RETRY errorlist? )? ('ID' | 'ID' '=')? batchId
      processId
      ON? tableName (EACH POSITIVE_NUMBERS)?
      (BY columnName (',' columnName)? (GROUP BY groupByExpr )?)?
      ('HINT' hintStatement)?
      ('WHERE'  whereStatement)?
     ;

run_batch_statement
    : '.SQL' RUN BATCH (WITH RETRY errorlist? )? batchId processId
      ('.WITH' cmd_set*)?
      '.BEGIN'
            block_batch+
      '.END'
    ;
cmd_try
    : '.TRY'
           tryblock += block_try*
      '.EXCEPTION'
           exceptblock += block_try*
      '.END TRY'
    ;
do_statement
    : '.SQL' DO? (WITH RETRY errorlist? )? sqlStment ;

get_statement
    : '.SQL' GET (WITH RETRY errorlist? )? sqlStment;

errorlist
    : '(' number (',' number)* ')'
    ;
cmd_exit
    : ('.EXIT' | '.QUIT') number
    ;

cmd_if
    : '.IF'  condition
      '.THEN'  thenblock += block_if*
      ('.ELSE' elseblock += block_if*)?
      '.END' 'IF'
    ;
for_if
    : '.IF'  condition
      '.THEN'  thenblock += block_for*
      ('.ELSE' elseblock += block_for*)?
      '.END' 'IF'
    ;
batch_if
    : '.IF'  condition
      '.THEN'  thenblock += block_batch*
     ('.ELSE' elseblock += block_batch*)?
     '.END' 'IF'
    ;
cmd_for
    : '.FOR'   ( ('DATA' 'IN' sqlStment)
               | ('SEQ'  ID 'FROM' number 'TO' number ('STEP' number)?)
               | ('LIST' ID 'IN' listStment)
               )
      ('.WITH' cmd_set*)?
      '.DO'
           block_for*
      '.END' 'FOR'
    ;

block_if
    : block_base
    | cmd_exit
    ;
block_for
    : block_base
    | for_break
    | for_continue
    | for_if
    ;
block_base
    : cmd_logoff
    | cmd_excsql
    | cmd_if
    | cmd_for
    | cmd_set
    | cmd_os
    | cmd_print
    ;
 block_try
    : block_batch
    | split_batch_statement
    ;

 block_batch
     : cmd_logoff
     | get_statement
     | do_statement
     | batch_if
     | cmd_set
     | cmd_os
     | cmd_print
     | cmd_exit
     ;
condition
    : ERROR_CODE     op=('>'|'<'|'<>'|'!='|'>='|'<='|'=') number
    | ERROR_COUNT    op=('>'|'<'|'<>'|'!='|'>='|'<='|'=') number
    | ACTIVITY_COUNT op=('>'|'<'|'<>'|'!='|'>='|'<='|'=') number
    | OS_CODE        op=('>'|'<'|'<>'|'!='|'>='|'<='|'=') number
    | ('NOT')? ALL SUCCESS
    ;


for_break
    : '.BREAK';

for_continue
    : '.CONTINUE'
    ;


cmd_set:  '.SET' setOptions;

setOptions
    : LOG '='? (SUMMARY | DETAIL)                                #OptLog
    | DRIVER '='? STRING_LITERAL                                 #OptJdbcDriver
    | ERROR LIMIT '='? POSITIVE_NUMBERS                          #OptErrorLimit
    | CONCURRENCY '='? POSITIVE_NUMBERS (WATCH ('FILE' | 'FILE' '=')?
      STRING_LITERAL EACH POSITIVE_NUMBERS 'SECS')?                                         #OptConcurrency
    | RETRY POSITIVE_NUMBERS INTERVAL POSITIVE_NUMBERS (',' rand=POSITIVE_NUMBERS )? 'SECS' #OptRetry
    | DATABASE sqlStment                                         #OptDatabase
    | JOBID '='? POSITIVE_NUMBERS                                #OptJobId
    | BATCH TABLE '='? tableName                                 #OptBatchTable
    ;

cmd_logoff: '.LOGOFF';

cmd_os
    : '.OS' (WITH RETRY errorlist? )? osStment
    ;
cmd_print
    :  '.PRINT' prtStment
    ;
listStment
    : LBRACE  STRING_LITERAL(','STRING_LITERAL)*?  RBRACE
    ;
prtStment
    : LBRACE  (~'}}' | '}' ~'}')*? RBRACE
    ;
osStment
    : LBRACE  (~'}}' | '}' ~'}')*? RBRACE
    ;
sqlStment
    : LBRACE  (~'}}' | '}' ~'}')*? RBRACE
    ;
number
    : NUMBERS | POSITIVE_NUMBERS
    ;
ACTIVITY_COUNT:           'ACTIVITY_COUNT';
ALL:					  'ALL';
BATCH:                    'BATCH';
BREAK:                    'BREAK';
BY:                       'BY';
CONCURRENCY:              'CONCURRENCY';
DATABASE:                 'DATABASE';
DETAIL:                   'DETAIL';
DO:                       'DO';
DOT:                      '.';
DRIVER:                   'DRIVER';
EACH:                     'EACH';
ELSE:                     'ELSE';
ERROR:                    'ERROR';
ERROR_CODE:               'ERROR_CODE';
ERROR_COUNT:              'ERROR_COUNT';
EXIT:                     'EXIT';
FILE:                     'FILE';
FROM:                     'FROM';
GET:                      'GET';
GROUP:                    'GROUP';
IF:                       'IF';
INTERVAL:                 'INTERVAL';
IP_ADDRESS:                ([0-9]+ '.' [0-9.]+ | [0-9A-F]* ':' [0-9A-F]* ':' [0-9A-F:]+ );
JOBID:                    'JOBID';
LBRACE:                   '{{';
LIMIT:                    'LIMIT';
LOG:                      'LOG';
NOT:                      'NOT';
POSITIVE_NUMBERS:         [1-9][0-9]*;
NUMBERS:                  '-'?[0-9]+;
OFF:                      'OFF';
ON:                       'ON';
OS_CODE:                  'OS_CODE';
PASSWORD:                 'PASSWORD';
RBRACE:                   '}}';
RETRY:                    'RETRY';
RESET:                    'RESET';
RUN:                      'RUN';
SEMI:                     ';';
SEQ:                      'SEQ';
SUCCESS:                  'SUCCESS';
SUMMARY:                  'SUMMARY';
TABLE:                    'TABLE';
THEN:                     'THEN';
USER:                     'USER';
WATCH:                    'WATCH';
WITH:                     'WITH';
COMMENT_INPUT:            '/*' .*? '*/' -> channel(HIDDEN);
LINE_COMMENT:             (('--' [ \t]* | '#') ~[\r\n]* ('\r'? '\n' | EOF)
                          | '--' ('\r'? '\n' | EOF)
                          ) -> channel(HIDDEN);
ID:                       ID_LITERAL;
STRING_LITERAL:           DQUOTA_STRING | SQUOTA_STRING | BQUOTA_STRING;
fragment DQUOTA_STRING:   '"'  ( '\\'. | '""' | ~('"'| '\\') )* '"';
fragment SQUOTA_STRING:   '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';
fragment BQUOTA_STRING:   '`'  ( ~'`' | '``' )* '`';
fragment ID_LITERAL:      [A-Z_$0-9\u0080-\uFFFF]*?[A-Z_$\u0080-\uFFFF]+?[A-Z_$0-9\u0080-\uFFFF]*;
WS :                      [ \t\r\n]+ -> channel(HIDDEN);// skip ; // skip spaces, tabs, newlines
ERR:                      .   ; // -> channel(HIDDEN);
