import commands.*;
import generated.TasktoolParser;
import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class TaskMgr implements IForStatment {
    private final MainParser parser;
    private final DbCfg cfg;
    private TasktoolParser.CommandsContext tree;
    private final MainVisitor visit;
    private final SqlExecutor session;
    private final SetInfo params;
    private final Map<String, String> env;
    private final Context ctx;
    private static final Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
    private String[] commandArgs;
    public TaskMgr() {
        parser = new MainParser();
        visit = new MainVisitor();
        cfg = new DbCfg();
        session = new SqlExecutor(cfg,new Result());
        params = new SetInfo();
        env = System.getenv();
        ctx = new Context();
    }
    public boolean compileFile(String filename) {
        File f = new File(filename);
        if(! f.exists()) {
            Utils.log("can not find file: " + filename);
            return false;
        }
        if(! f.canRead()) {
            Utils.log("can not read file: " + filename);
            return false;
        }
        tree = parser.parseFile(filename);
        if(!parser.getErrorList().isEmpty()) {
            parser.getErrorList().forEach(System.out::println);
            return false;
        }
        return validCheckAllStatement();
    }
    private boolean validCheckAllStatement() {
        List<BaseInfo> commands = generateCommands();
        for (BaseInfo obj : commands) {
            if (obj instanceof ForStatementInfo) {
                if (!checkForStatment((ForStatementInfo) obj)) {
                    Utils.log("step value [" + ((ForStatementInfo) obj).step + "] in  the for statement is invalid!");
                    return false;
                }
            }
        }
        return true;
    }

    public boolean compile(String str) {
        tree = parser.parse(str);
        if(!parser.getErrorList().isEmpty()) {
           parser.getErrorList().forEach(System.out::println);
            return false;
        }
        return validCheckAllStatement();
    }
    private Result runOneCommand(BaseInfo cmd, SetInfo setCfg, SqlExecutor sess, HashMap<String,String> cursor,Context context) {
        Result ret = null;
        int cmdType = cmd.getType();
        if(cmdType == BaseInfo.EXC_SQL) {
            SqlInfo sql = (SqlInfo) cmd;
            if(sql.type == SqlInfo.Batch) {
                doBatch(sql, sess, cursor, setCfg);
                ret = sess.result;
            } else if(sql.type == SqlInfo.RunBatch) {
                ret = doRunBatch(sql, sess, cursor, setCfg,context);
            } else {
                doSql(sql, sess, cursor, setCfg);
                if (sess.result.rows != null) sess.result.rows.displayDataSet();
                ret = sess.result;
            }
        } else if(cmdType == BaseInfo.LOGON){
            doLogon((LogonInfo) cmd, sess);
            ret = sess.result;
        } else if(cmdType == BaseInfo.LOGOFF){
            sess.logoff();
            ret = sess.result;
        } else if(cmdType == BaseInfo.EXIT){
            doExit((ExitInfo) cmd);
            return null;
        } else if(cmdType == BaseInfo.SET) {
            doSet((SetInfo) cmd, setCfg, sess, cursor);
            ret = sess.result;
        } else if(cmdType == BaseInfo.IF_STATEMENT) {
            IfStatementInfo ifs = (IfStatementInfo) cmd;
            List<BaseInfo> steps;
            if(ifs.condition.isTrue(sess.result)){
                steps = ifs.thenBlocks;
            } else {
                steps = ifs.elseBlocks;
            }
            ret = sess.result;
            if(steps.isEmpty()) ret.reset();
            for (BaseInfo s : steps) {
                if (s.getType() == BaseInfo.FOR_CONTINUE) {
                    ret.reset();
                    ret.setContinue(true);
                    break;
                } else if (s.getType() == BaseInfo.FOR_BREAK) {
                    ret.reset();
                    ret.setBreak(true);
                    break;
                } else {
                    ret = runOneCommand(s, setCfg, sess, cursor,context);
                    if(ret.hasError() && context.isTryContext()) {
                        break;
                    }
                }
            }

        } else if(cmdType == BaseInfo.FOR_STATEMENT) {
            DataSet ds;
            ForStatementInfo forStmt = (ForStatementInfo) cmd;
            if(forStmt.forKind == ForStatementInfo.FOR_DATA) {
                doSql(new SqlInfo(SqlInfo.SELECT, forStmt.dataSql), sess, cursor, setCfg);
                if (sess.result.hasError() || sess.result.rows == null || sess.result.rows.isEmpty()) {
                    sess.result.allSuccess = (!sess.result.hasError()) && (sess.result.rows != null);
                    return sess.result;
                }
                ds = sess.result.rows;
            } else if(forStmt.forKind == ForStatementInfo.FOR_SEQ) {
                ds = new DataSet(forStmt.from, forStmt.to, forStmt.step, forStmt.forName);
            } else {
                ds = new DataSet(forStmt.stringList, forStmt.forName);
            }
            SetInfo par = (SetInfo)setCfg.clone();
            forStmt.withBlocks.stream().filter(s->s.operator != SetInfo.SET_DATABASE).forEach(x->doSet(x,par,null, cursor));
            TaskThreads task = new TaskThreads(ds,par,forStmt.withBlocks,forStmt.doBlocks,this,cursor,context);
            ret = task.run();
        } else if(cmdType == BaseInfo.OS_CMD) {
            doOsCmd((OsCmdInfo) cmd, cursor, sess.result, setCfg);
            ret = sess.result;
        } else if(cmdType == BaseInfo.PRINT) {
            doPrint((PrintInfo) cmd, cursor, sess.result, setCfg);
            ret = sess.result;
            ret.reset();
        } else if(cmdType == BaseInfo.HELP) {
            Help.printHelp();
            ret = sess.result;
        } else if(cmdType == BaseInfo.TRY_STATMENT) {
            ret = doTryStatment((TryInfo) cmd, cursor, sess, setCfg,context);
        }
        return ret;
    }

    private Result doTryStatment(TryInfo cmd, HashMap<String, String> cursor, SqlExecutor exe, SetInfo setCfg, Context ctx) {
        Result ret = null;
        int exceptCode = 0;
        String exceptMsg = "";
        Context context = new Context();
        context.setTryContext(true);
        for(BaseInfo base: cmd.tryBlocks) {
            ret = runOneCommand(base, setCfg, exe, cursor,context);
            if(ret.hasError()) break;
        }
        if(ret != null && ret.hasError()) {
            if(cmd.internal_use_for_run_batch) {
                exceptCode = ret.errorCode;
                exceptMsg = ret.errorMessage;
                exe.result.setException(exceptCode, exceptMsg);
            }

            if(cmd.exceptBlocks.isEmpty()) return new Result(); //eat the exception

            for(BaseInfo base: cmd.exceptBlocks) {
                ret = runOneCommand(base, setCfg, exe, cursor,ctx);
                if(ret.hasError() && ctx.isTryContext()) break;
            }
        }
        if(cmd.internal_use_for_run_batch){
            if(ret.hasError()) return ret;
            if(exceptCode != 0) {
                ret.errorCode = exceptCode;
                ret.errorMessage = exceptMsg;
                return ret;
            }
        }
        return ret == null ? new Result() : ret;
    }

    private void doPrint(PrintInfo cmd, HashMap<String, String> cursor, Result result, SetInfo setCfg) {
        String[] r = mappingFieldValue(cursor, cmd.prtString,result);
        Utils.log(r[0]);
    }

    private void executeCommand(String cmd, Result result) {
        try {
            result.reset();
            List<String> args = new ArrayList<>();
            args.add(System.getProperty("os.name").startsWith("Windows") ? "cmd.exe" : "sh");
            args.add("-c");
            args.add(cmd);
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.redirectErrorStream(true); // Redirect stderr to stdout
            Process process = processBuilder.start();
            process.getOutputStream().close();
            // Read the combined output of the process in a separate thread
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Utils.log(line);
                    }
                } catch (IOException e) {
                    Utils.log(e.getMessage());
                }
            });

            outputThread.start();
            result.osExitCode = process.waitFor();
            outputThread.join(); // Wait for the output thread to finish
            process.destroy();
            result.errorCode = result.osExitCode; //if exitcode != 0 ,means error happened
        }catch(Exception e) {
            result.errorCode = -1;
            result.errorMessage = e.getMessage();
            Utils.log(e.getMessage());
        }
    }
    private void doOsCmd(OsCmdInfo info, HashMap<String, String> cursor, Result result,SetInfo param) {
        result.reset();
        String[] r = mappingFieldValue(cursor, info.cmdString,result);
        String command = r[0];
        StringBuilder sb = new StringBuilder();
        if ((r[1] != null) && !r[1].isEmpty()) {
            if(param.isLogDetail()) {
                Utils.log(Utils.formatCmd(command));
            }
        } else {
            Utils.log(Utils.formatCmd(command));
        }
        for(int retry = 0; retry <= param.retryCount; retry++) {
            sb.setLength(0);
            Instant instant1 = Instant.now();
            executeCommand(command, result);
            Instant instant2 = Instant.now();
            if (r[1] != null && !r[1].isEmpty()) {
                sb.append(Utils.getElapsedTime(instant1, instant2)).append(", mapping:").append(r[1]);
            } else {
                sb.append(Utils.getElapsedTime(instant1, instant2));
            }
            if (result.hasError()) {
                sb.append(", error code: ").append(result.errorCode)
                  .append(" error message: ").append(result.errorMessage);
            }
            if((result.hasError() && info.retry) && (info.errorList == null
                    || info.errorList.isEmpty() || info.errorList.containsKey(result.errorCode))) {
                long sleepTime = param.retryInterval <= 0 ? SetInfo.DEFAULT_RETRY_INTERVAL_SECS : param.retryInterval;
                if (param.retryIntervalRandom > 0) {
                    sleepTime += (new Random()).nextInt(param.retryIntervalRandom);
                }
                if (retry < param.retryCount)
                    sb.append("\n").append("will sleep ").append(sleepTime).append(" seconds and retry again...");
                Utils.logWithTime(sb.toString());
                try {
                    sleep(sleepTime * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Utils.logWithTime(sb.toString());
                break;
            }
        }
    }
    public void runInteractive() {
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Completer commandCompleter = new StringsCompleter(
                ".if",".then",".else",".end",".for","data","list","from","seq","in","step","to",
                ".with",".do",".break",".continue","error_code","activity_count","error_count",
                "os_code","not","all","success",
                ".logon","password","user",".logoff",".exit",".quit",
                ".print",".help","retry","file","secs",
                "concurrency","each","interval","drive","limit","log","watch","summary",
                ".sql","get",".os",".set",".try",".exception","batch","procid","run"
        );

        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(commandCompleter)
                .build();

        String prompt = "tasktool> ";
        Utils.log("========================================================================");
        Utils.log("Welcome to the tasktool.          Commands end with ;                   ");
        Utils.log("tasktool version 1.1.0            Mailto: shponyxie@163.com             ");
        Utils.log("use .help; to get help                                                  ");
        StringBuilder sb = new StringBuilder();
        List<BaseInfo> commands;
        while(true) {
            try {
                String str = lineReader.readLine(prompt);
                sb.append(str);
                if (str.trim().endsWith(";")) {
                    try {
                        if (compile(sb.toString())) {
                            commands = generateCommands();
                            commands.forEach(cmd -> session.result = runOneCommand(cmd, params, session, new HashMap<>(),ctx));
                        }
                    }finally {
                        sb.setLength(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
    private List<BaseInfo> generateCommands() {
        List<BaseInfo> commands = new ArrayList<>();
        tree.command().forEach(x-> {
            if( x instanceof TasktoolParser.CmdLogonContext) {
                commands.add(visit.visitCmdLogon((TasktoolParser.CmdLogonContext) x));
            } else if( x instanceof TasktoolParser.CmdLogoffContext) {
                commands.add(visit.visitCmd_logoff(((TasktoolParser.CmdLogoffContext) x).cmd_logoff() ));
            } else if( x instanceof TasktoolParser.CmdExitContext) {
                commands.add(visit.visitCmd_exit(((TasktoolParser.CmdExitContext) x).cmd_exit()));
            } else if( x instanceof TasktoolParser.CmdExcSqlContext) {
                commands.add(visit.visitCmdExcSql(((TasktoolParser.CmdExcSqlContext) x).cmd_excsql()));
            } else if( x instanceof TasktoolParser.CmdSetContext) {
                commands.add(visit.visitCmdSet(((TasktoolParser.CmdSetContext) x).cmd_set()));
            } else if( x instanceof TasktoolParser.CmdIfContext) {
                commands.add(visit.visitCmdIf((TasktoolParser.CmdIfContext) x));
            } else if( x instanceof TasktoolParser.CmdForContext) {
                commands.add(visit.visitCmdFor((TasktoolParser.CmdForContext)x));
            } else if( x instanceof TasktoolParser.CmdOsContext) {
                commands.add(visit.visitCmdOs(((TasktoolParser.CmdOsContext)x).cmd_os()));
            } else if( x instanceof TasktoolParser.CmdPrintContext) {
                commands.add(visit.visitCmdPrint(((TasktoolParser.CmdPrintContext)x).cmd_print()));
            } else if( x instanceof TasktoolParser.CmdHelpContext) {
                commands.add(visit.visitCmdHelp(((TasktoolParser.CmdHelpContext)x).cmd_help()));
            } else if( x instanceof TasktoolParser.CmdTryContext) {
                commands.add(visit.visitCmd_try(((TasktoolParser.CmdTryContext)x).cmd_try()));
            }
        });
        return commands;
    }
    private boolean checkForStatment(ForStatementInfo forstmt) {
        return (forstmt.forKind == ForStatementInfo.FOR_DATA
                || forstmt.forKind == ForStatementInfo.FOR_LIST)
                || (forstmt.step != 0);
    }

    public void run(String[] args) {
        commandArgs = args;
        List<BaseInfo> commands = generateCommands();
        commands.forEach(cmd -> session.result = runOneCommand(cmd, params, session, new HashMap<>(),ctx));
    }
    private void doSet(SetInfo info, SetInfo tgt, SqlExecutor exe, HashMap<String,String> cursor) {
        switch (info.operator) {
            case SetInfo.SET_LOG:  tgt.logFormat = info.logFormat; break;
            case SetInfo.SET_ERROR: tgt.errorLimit = info.errorLimit; break;
            case SetInfo.SET_CONCURRENCY:
                tgt.concurrency = info.concurrency;
                tgt.concurrencyFile = info.concurrencyFile;
                tgt.concurrencyInterval = info.concurrencyInterval;
                break;
            case SetInfo.SET_RETRY:
                tgt.retryCount = info.retryCount;
                tgt.retryInterval = info.retryInterval;
                tgt.retryIntervalRandom = info.retryIntervalRandom;
                if(tgt.retryCount < 0) tgt.retryCount = 0;
                break;
            case SetInfo.SET_DATABASE :
                doSql(new SqlInfo(SqlInfo.NonSELECT, info.dbSetSQL),exe,cursor,tgt);
                break;
            case SetInfo.SET_DRIVER:
                tgt.driverFile = info.driverFile;
                cfg.setDriver(info.driverFile);
                break;
            case SetInfo.SET_JOBID:
                tgt.jobId = info.jobId;
                break;
            case SetInfo.SET_BATCH_TABLE:
                tgt.batchTable = info.batchTable;
                break;
        }
    }

    private void doExit(ExitInfo info) {
        session.logoff();
        Utils.log("Program finished with exit code: " + info.exitCode);
        System.exit(info.exitCode);
    }
    private void doLogon(LogonInfo logon, SqlExecutor sess) {
        sess.dbcfg.setHost(logon.hosts);
        sess.dbcfg.setUser(logon.user);
        if(logon.needDecode) {
            sess.dbcfg.setPassword(Utils.decode(logon.password));
        } else {
            sess.dbcfg.setPassword(mappingFieldValue(new HashMap<>(), logon.password,sess.result)[0]);
        }
        sess.logon();
    }
    private boolean needSetJobId(SqlExecutor exe, SetInfo param) {
        if(param.jobId == null) {
            exe.result.errorCode = -1;
            exe.result.errorMessage = "error, please set a valid Jobid first! ";
            Utils.log(" " + exe.result.errorMessage);
            return true;
        }
        return false;
    }
    private Result doRunBatch(SqlInfo sqlinfo, SqlExecutor exe, HashMap<String,String> cursor, SetInfo param,Context ctx) {
       if(needSetJobId(exe, param)) {
           Result ret = new Result();
           ret.errorCode = -1;
           ret.errorMessage = "error, please set a valid jobid.";
           ret.allSuccess = false;
           return ret;
       }
       SqlInfo info = sqlinfo.clone();
       String[] r = mappingFieldValue(cursor, info.batch.processId,exe.result);
       info.batch.processId = r[0];
       info.forStmt.dataSql = info.batch.getBatchTaskSQL(param);
       TryInfo tryinfo = new TryInfo();
       tryinfo.internal_use_for_run_batch = true;
       tryinfo.tryBlocks.add(new SqlInfo(SqlInfo.NonSELECT,info.batch.getStartTransSQL(param)));
       tryinfo.tryBlocks.add(new SqlInfo(SqlInfo.NonSELECT,"begin;"));
       tryinfo.tryBlocks.addAll(info.forStmt.doBlocks);
       tryinfo.tryBlocks.add(new SqlInfo(SqlInfo.NonSELECT,info.batch.getSuccessTransSQL(param)));
       tryinfo.tryBlocks.add(new SqlInfo(SqlInfo.NonSELECT,"commit;"));

       tryinfo.exceptBlocks.add(new SqlInfo(SqlInfo.NonSELECT,"rollback;"));
       tryinfo.exceptBlocks.add(new SqlInfo(SqlInfo.NonSELECT,info.batch.getFailedTransSQL(param)));
       info.forStmt.doBlocks.clear();
       info.forStmt.doBlocks.add(tryinfo);
       return runOneCommand(info.forStmt,param,exe,cursor,ctx);
    }
    private void doBatch(SqlInfo sqlinfo, SqlExecutor exe, HashMap<String,String> cursor, SetInfo param) {
        if(needSetJobId(exe, param)) return;
        SqlInfo info = sqlinfo.clone();
        String[] r = mappingFieldValue(cursor, info.batch.processId,exe.result);
        info.batch.processId = r[0];
        if(info.batch.where != null) {
            r = mappingFieldValue(cursor, info.batch.where, exe.result);
            info.batch.where = r[0];
        }
        Instant instant1 = Instant.now();
        exe.splitBatch(info.batch,param);
        Instant instant2 = Instant.now();
        StringBuilder sb = new StringBuilder();
        sb.append(Utils.getElapsedTime(instant1, instant2));
        if (exe.result.hasError()) {
            sb.append(", error code: ").append(exe.result.errorCode)
                    .append(" error message: ").append(exe.result.errorMessage);
        } else {
            sb.append(", affected rows: ").append(exe.result.activityCount);
        }
        Utils.log(sb.toString());
    }
    private void doSql(SqlInfo info, SqlExecutor exe, HashMap<String,String> cursor, SetInfo param) {
        String[] r = mappingFieldValue(cursor, info.sql,exe.result);
        StringBuilder sb = new StringBuilder();
        for(int retry = 0; retry <= param.retryCount; retry++) {
            Instant instant1 = Instant.now();
            exe.execute(info.type, r[0]);
            Instant instant2 = Instant.now();
            sb.setLength(0);
            if (r[1] != null && !r[1].isEmpty()) {
                if (param.isLogDetail()) {
                    Utils.log(Utils.formatSQL(r[0]));
                }
                sb.append(Utils.getElapsedTime(instant1, instant2));
                sb.append(", mapping:").append(r[1]);
            } else {
                Utils.log(Utils.formatSQL(r[0]));
                sb.append(Utils.getElapsedTime(instant1, instant2));
            }
            if (exe.result.hasError()) {
                sb.append(", error code: ").append(exe.result.errorCode)
                        .append(" error message: ").append(exe.result.errorMessage);
            } else {
                sb.append(", affected rows: ").append(exe.result.activityCount);
            }

            if((exe.result.hasError() && info.retry) && (info.errorList == null
                    || info.errorList.isEmpty() || info.errorList.containsKey(exe.result.errorCode))) {
                    long sleepTime = param.retryInterval <= 0 ? SetInfo.DEFAULT_RETRY_INTERVAL_SECS : param.retryInterval;
                    if (param.retryIntervalRandom > 0) {
                        sleepTime += (new Random()).nextInt(param.retryIntervalRandom);
                    }
                    if (retry < param.retryCount)
                        sb.append("\n").append("will sleep ").append(sleepTime).append(" seconds and retry again...");
                    Utils.logWithTime(sb.toString());
                    try {
                        sleep(sleepTime * 1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
            } else {
                Utils.logWithTime(sb.toString());
                break;
            }
        }
    }

    /**
     * mapping env variable and column value, env variables is case-sensitive
     * @param map current context variables and values
     * @param src the string needing to be parsed and replaced
     * @return String, the replacement of src string
     */
    private String[] mappingFieldValue(Map<String, String> map, String src, Result lastError) {
        String s = src;
        HashMap<String, String> cols = new HashMap<>();
        Matcher matcher = pattern.matcher(s);
        while(true) {
            if (matcher.find()) {
                String colName;
                String colValue;
                if(matcher.group(1).toLowerCase().startsWith("env:")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = env.get(matcher.group(1).substring(4));
                } else if(matcher.group(1).equalsIgnoreCase("sqlerr:code")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.sqlCode + "";
                } else if(matcher.group(1).equalsIgnoreCase("sqlerr:message")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.sqlErrorMsg;
                }else if(matcher.group(1).equalsIgnoreCase("sqlerr:message_sq")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.sqlErrorMsg.replaceAll("'","''");
                } else if(matcher.group(1).equalsIgnoreCase("error:code")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.errorCode + "";
                } else if(matcher.group(1).equalsIgnoreCase("error:message")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.errorMessage;
                }else if(matcher.group(1).equalsIgnoreCase("error:message_sq")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.errorMessage.replaceAll("'","''");
                } else if(matcher.group(1).equalsIgnoreCase("except:code")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.getExceptCode() + "";
                } else if(matcher.group(1).equalsIgnoreCase("except:message")) {
                    colName =  "\\{" + matcher.group(1) + "\\}";
                    colValue = lastError.getExceptMsg().replaceAll("'","''");;
                }else if(matcher.group(1).toLowerCase().startsWith("args:")) {
                    String args = matcher.group(1).substring(5).trim();
                    colName  = "\\{" + matcher.group(1) + "\\}";
                    try {
                        int argno = Integer.parseInt(args);
                        colValue = argno <= 0 ? null : commandArgs[argno + 1];
                    } catch (Exception e) {
                        colValue = null;
                    }
                } else {
                    colName =  "\\{" + matcher.group(1).toLowerCase() + "\\}";
                    colValue = map.get(colName);
                }

                if (colValue != null) {
                    cols.put(colName, colValue);
                }
            } else {
                break;
            }
        }
        String[] r = new String[2];
        for(Map.Entry<String, String> e: cols.entrySet()) {
            s = s.replaceAll("(?i)" + e.getKey() ,Matcher.quoteReplacement(e.getValue()));
        }
        r[0] = Utils.unSlashString(s,"\\}", "}");
        r[1] = cols.entrySet().stream().map(e->e.getKey() + "->" + e.getValue())
                   .collect(Collectors.joining(", ")).replaceAll("\\\\","");
        return r;
    }
    @Override
    public Result doForStatment(DataSet data, SetInfo par, List<SetInfo> initBlocks, List<BaseInfo> doBlocks, TaskLocks locks, HashMap<String,String> cursorCtx, Context ctx) {
        Result result = new Result();
        SqlExecutor exe = new SqlExecutor(cfg, result);
        exe.logon();

        initBlocks.stream().filter(s -> s.operator == SetInfo.SET_DATABASE).forEach(x -> doSet(x, par, exe, cursorCtx));
        int row;
        HashMap<String, String> mergeCursor = new HashMap<>(cursorCtx);
        while(true) {
            if (locks.needDecreaseThread(true)) {
                exe.logoff();
                return result;
            }

            row = locks.getAndIncreaseDataRow();

            if(!data.isValidRow(row) || locks.getErrors() > par.errorLimit || locks.needBreak()) {
                exe.logoff();
                return result;
            }

            mergeCursor.putAll(data.mapCursorValue(row));
            locks.incExecuted();

            for (BaseInfo c : doBlocks) {
                Result ret = null;
                if(c.getType() != BaseInfo.FOR_BREAK && c.getType() != BaseInfo.FOR_CONTINUE) {
                    ret = this.runOneCommand(c, par, exe, mergeCursor,ctx);
                    if(ret.hasError()) locks.incErrors();
                }
                if((ret != null && ret.needContinue()) || c.getType() == BaseInfo.FOR_CONTINUE) {
                    break;
                }
                if((ret != null && ret.needBreak()) || c.getType() == BaseInfo.FOR_BREAK) {
                    locks.setBreak();
                    return result;
                }
            }
        }
    }

}
