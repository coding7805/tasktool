import commands.*;
import generated.TasktoolBaseVisitor;
import generated.TasktoolParser;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVisitor extends TasktoolBaseVisitor<Object> {
    public LogonInfo visitCmdLogon(TasktoolParser.CmdLogonContext ctx) {
       LogonInfo logon = new LogonInfo();
       logon.hosts = ctx.cmd_logon().instances().getText();
       List<String> lst = new ArrayList<>();
       TasktoolParser.InstancesContext instances = ctx.cmd_logon().instances();
       int count = instances.instance().size();
       int[] hosts = new int[count];
       int i = 0;
       while(count > 0) {
           int n = i % hosts.length;
           if(hosts[n] < instances.instance(n).POSITIVE_NUMBERS().size()) {
               lst.add(instances.instance(n).ip.getText() + ":" + instances.instance(n).POSITIVE_NUMBERS(hosts[n]).getText());
               hosts[n] ++;
           }else if(hosts[n] == instances.instance(n).POSITIVE_NUMBERS().size()){
               count--;
           }
           i++;
       }
       logon.hosts = String.join(",", lst);
       logon.user = ctx.cmd_logon().user().STRING_LITERAL().getText();
       logon.password = ctx.cmd_logon().password().STRING_LITERAL().getText();
       logon.user = Utils.unQuotaString(logon.user);
       logon.needDecode = logon.password.startsWith("`");
       logon.password = Utils.unQuotaString(logon.password);
       return logon;
    }
    public HelpInfo visitCmdHelp(TasktoolParser.Cmd_helpContext ctx) {
        return new HelpInfo();
    }
    public PrintInfo visitCmdPrint(TasktoolParser.Cmd_printContext ctx) {
        PrintInfo info = new PrintInfo();
        Interval interval;
        int a, b;
        a = ctx.prtStment().start.getStartIndex();
        b = ctx.prtStment().stop.getStopIndex();
        interval = new Interval(a,b);
        info.prtString = ctx.prtStment().start.getInputStream().getText(interval);
        info.prtString = info.prtString.substring(2,info.prtString.length()-2); //reduce the '{{' and  '}}'
        return info;
    }
    public OsCmdInfo visitCmdOs(TasktoolParser.Cmd_osContext ctx) {
        OsCmdInfo info = new OsCmdInfo();
        TasktoolParser.ErrorlistContext errlist;
        Interval interval;
        int a, b;
        a = ctx.osStment().start.getStartIndex();
        b = ctx.osStment().stop.getStopIndex();
        interval = new Interval(a,b);
        info.cmdString = ctx.osStment().start.getInputStream().getText(interval);
        info.cmdString = info.cmdString.substring(2,info.cmdString.length()-2); //reduce the '{{' and  '}}'
        info.retry =  ctx.WITH() != null;
        errlist = ctx.errorlist();
        if(errlist != null) {
            info.errorList = new HashMap<>();
            errlist.number().forEach(x -> info.errorList.put(Integer.parseInt(x.getText()),""));
        }
        return info;
    }

    public SqlInfo visitGet_statement(TasktoolParser.Get_statementContext ctx) {
        SqlInfo info = new SqlInfo();
        Interval interval;
        int a, b;
        TasktoolParser.ErrorlistContext errlist = null;
        info.type = SqlInfo.SELECT;
        info.retry =  ctx.WITH() != null;
        errlist = ctx.errorlist();
        a = ctx.sqlStment().start.getStartIndex();
        b = ctx.sqlStment().stop.getStopIndex();
        interval = new Interval(a,b);
        info.sql = ctx.start.getInputStream().getText(interval);
        info.sql = info.sql.substring(2,info.sql.length()-2); //reduce the '{{' and  '}}'
        if(errlist != null) {
            info.errorList = new HashMap<>();
            errlist.number().forEach(x -> info.errorList.put(Integer.parseInt(x.getText()),""));
        }
        return info;
    }

    public SqlInfo visitDo_statement(TasktoolParser.Do_statementContext ctx) {
        SqlInfo info = new SqlInfo();
        Interval interval;
        int a, b;
        TasktoolParser.ErrorlistContext errlist = null;
        info.type = SqlInfo.NonSELECT;
        info.retry =  ctx.WITH() != null;
        errlist = ctx.errorlist();
        a = ctx.sqlStment().start.getStartIndex();
        b = ctx.sqlStment().stop.getStopIndex();
        interval = new Interval(a,b);
        info.sql = ctx.start.getInputStream().getText(interval);
        info.sql = info.sql.substring(2,info.sql.length()-2); //reduce the '{{' and  '}}'
        if(errlist != null) {
            info.errorList = new HashMap<>();
            errlist.number().forEach(x -> info.errorList.put(Integer.parseInt(x.getText()),""));
        }
        return info;
    }
    public SqlInfo visitSplit_batch_statement(TasktoolParser.Split_batch_statementContext ctx) {
        SqlInfo info = new SqlInfo();
        Interval interval;
        int a, b;
        TasktoolParser.ErrorlistContext errlist = null;
        info.type = SqlInfo.Batch;
        BatchInfo batch = new BatchInfo();
        info.batch = batch;
        batch.reset = ctx.RESET() != null;
        batch.batchId = Integer.parseInt(ctx.batchId().getText());
        batch.processId = Utils.unQuotaString(ctx.processId().STRING_LITERAL().getText());
        if(ctx.tableName() != null) batch.tableName = ctx.tableName().getText();
        if(ctx.POSITIVE_NUMBERS() != null) batch.batchRows = Integer.parseInt(ctx.POSITIVE_NUMBERS().getText());
        if(ctx.columnName() != null) batch.columnName = ctx.columnName().stream().map(RuleContext::getText).collect(Collectors.joining(","));
        if(batch.columnName != null && batch.columnName.isEmpty()) batch.columnName = null;
        if(ctx.groupByExpr() != null) batch.groupBy = Utils.unQuotaString(ctx.groupByExpr().getText());
        if(ctx.whereStatement() != null) batch.where = Utils.unQuotaString(ctx.whereStatement().getText());
        if(ctx.hintStatement() != null) batch.hint = Utils.unQuotaString(ctx.hintStatement().getText());
        errlist = ctx.errorlist();
        if(errlist != null) {
            info.errorList = new HashMap<>();
            errlist.number().forEach(x -> info.errorList.put(Integer.parseInt(x.getText()),""));
        }
        return info;
    }

    public SqlInfo visitRun_batch_statement(TasktoolParser.Run_batch_statementContext ctx) {
        SqlInfo info = new SqlInfo();
        Interval interval;
        int a, b;
        TasktoolParser.ErrorlistContext errlist = null;
        info.type = SqlInfo.RunBatch;
        BatchInfo batch = new BatchInfo();
        info.batch = batch;
        batch.batchId = Integer.parseInt(ctx.batchId().getText());
        batch.processId = Utils.unQuotaString(ctx.processId().STRING_LITERAL().getText());
        errlist = ctx.errorlist();
        if(errlist != null) {
            info.errorList = new HashMap<>();
            errlist.number().forEach(x -> info.errorList.put(Integer.parseInt(x.getText()),""));
        }
        ForStatementInfo stmt = new ForStatementInfo();
        stmt.forKind = ForStatementInfo.FOR_DATA;
        if(ctx.cmd_set() != null) {
            for(TasktoolParser.Cmd_setContext x: ctx.cmd_set()){
                stmt.withBlocks.add(visitCmdSet(x));
            }
        }

        if(ctx.block_batch() != null) {
            List<BaseInfo> blocks = stmt.doBlocks;
            ctx.block_batch().forEach(blk -> {
                visitBlockBatch(blocks, blk);
            });
        }
        info.forStmt = stmt;
        return info;
    }
    public SqlInfo visitCmdExcSql(TasktoolParser.Cmd_excsqlContext ctx) {
        if(ctx.get_statement() != null) {
            return visitGet_statement(ctx.get_statement());
        } else if(ctx.do_statement() != null) {
            return visitDo_statement(ctx.do_statement());
        } else if(ctx.split_batch_statement() != null) {
            return visitSplit_batch_statement(ctx.split_batch_statement());
        } else if(ctx.run_batch_statement() != null) {
            return visitRun_batch_statement(ctx.run_batch_statement());
        }
        return null;
    }
    public SetInfo visitCmdSet(TasktoolParser.Cmd_setContext ctx) {
        SetInfo setinfo = new SetInfo();
        TasktoolParser.SetOptionsContext option = ctx.setOptions();
        if(option instanceof TasktoolParser.OptLogContext) {
            setinfo.operator = SetInfo.SET_LOG;
            TasktoolParser.OptLogContext opt = (TasktoolParser.OptLogContext) option;
            setinfo.logFormat =  opt.SUMMARY() != null ? SetInfo.LOG_SUMMARY : SetInfo.LOG_DETAIL;
        } else if(option instanceof TasktoolParser.OptErrorLimitContext) {
            setinfo.operator = SetInfo.SET_ERROR;
            setinfo.errorLimit = Integer.parseInt(((TasktoolParser.OptErrorLimitContext) option).POSITIVE_NUMBERS().getText());
        } else if(option instanceof TasktoolParser.OptConcurrencyContext) {
            setinfo.operator = SetInfo.SET_CONCURRENCY;
            if(((TasktoolParser.OptConcurrencyContext) option).EACH() != null) {
                setinfo.concurrency = Integer.parseInt(((TasktoolParser.OptConcurrencyContext) option).POSITIVE_NUMBERS(0).getText());
                setinfo.concurrencyInterval = Integer.parseInt(((TasktoolParser.OptConcurrencyContext) option).POSITIVE_NUMBERS(1).getText());
                setinfo.concurrencyFile = Utils.unQuotaString(((TasktoolParser.OptConcurrencyContext) option).STRING_LITERAL().getText());
            } else {
                setinfo.concurrency = Integer.parseInt(((TasktoolParser.OptConcurrencyContext) option).POSITIVE_NUMBERS(0).getText());
            }
        } else if(option instanceof TasktoolParser.OptJdbcDriverContext) {
            setinfo.operator = SetInfo.SET_DRIVER;
            setinfo.driverFile = Utils.unQuotaString(((TasktoolParser.OptJdbcDriverContext) option).STRING_LITERAL().getText());
        } else if(option instanceof TasktoolParser.OptRetryContext) {
            setinfo.operator = SetInfo.SET_RETRY;
            setinfo.retryCount = Integer.parseInt(((TasktoolParser.OptRetryContext) option).POSITIVE_NUMBERS(0).getText());
            setinfo.retryInterval = Integer.parseInt(((TasktoolParser.OptRetryContext) option).POSITIVE_NUMBERS(1).getText());
            setinfo.retryIntervalRandom = Integer.parseInt(((TasktoolParser.OptRetryContext) option).rand.getText());
        } else if(option instanceof TasktoolParser.OptDatabaseContext) {
            setinfo.operator = SetInfo.SET_DATABASE;
            int a = ((TasktoolParser.OptDatabaseContext) option).sqlStment().start.getStartIndex();
            int b = ((TasktoolParser.OptDatabaseContext) option).sqlStment().stop.getStopIndex();
            Interval interval = new Interval(a,b);
            setinfo.dbSetSQL = option.start.getInputStream().getText(interval);
            setinfo.dbSetSQL = setinfo.dbSetSQL.substring(2,setinfo.dbSetSQL.length()-2);
        } else if(option instanceof TasktoolParser.OptJobIdContext) {
            setinfo.operator = SetInfo.SET_JOBID;
            setinfo.jobId = Integer.parseInt(((TasktoolParser.OptJobIdContext) option).POSITIVE_NUMBERS().getText());
        } else if(option instanceof TasktoolParser.OptBatchTableContext) {
            setinfo.operator = SetInfo.SET_BATCH_TABLE;
            setinfo.batchTable = ((TasktoolParser.OptBatchTableContext) option).tableName().getText();
        }

        return setinfo;
    }
    public ConditionInfo visitCondition(TasktoolParser.ConditionContext ctx) {
        ConditionInfo condition = new ConditionInfo();
        if(ctx.ERROR_CODE() != null) {
            condition.conType = ConditionInfo.ERROR_CODE;
            condition.op = ctx.op.getText();
            condition.value = ctx.number().getText();
        } else if(ctx.ACTIVITY_COUNT() != null) {
            condition.conType = ConditionInfo.ACTIVITY_COUNT;
            condition.op = ctx.op.getText();
            condition.value = ctx.number().getText();
        } else if(ctx.ERROR_COUNT() != null) {
            condition.conType = ConditionInfo.ERROR_COUNT;
            condition.op = ctx.op.getText();
            condition.value = ctx.number().getText();
        } else if(ctx.SUCCESS() != null) {
            condition.conType = ConditionInfo.ALL_SUCCESS;
            condition.value = (ctx.NOT() != null) ? "NOT" : "";
        } else if(ctx.OS_CODE() != null) {
            condition.conType = ConditionInfo.OS_CODE;
            condition.op = ctx.op.getText();
            condition.value = ctx.number().getText();
        }
        return condition;
    }
    public LogoffInfo visitCmd_logoff(TasktoolParser.Cmd_logoffContext ctx) {
        return new LogoffInfo();
    }
    public ExitInfo visitCmd_exit(TasktoolParser.Cmd_exitContext ctx) {
        ExitInfo info = new ExitInfo();
        info.exitCode = Integer.parseInt(ctx.number().getText());
        return info;
    }
    public TryInfo visitCmd_try(TasktoolParser.Cmd_tryContext ctx) {
        TryInfo t = new TryInfo();
        if(ctx.tryblock != null) {
            ctx.tryblock.forEach( x -> {
                if(x.block_batch() != null) {
                    visitBlockBatch(t.tryBlocks, x.block_batch());
                }else if(x.split_batch_statement() != null) {
                    t.tryBlocks.add(visitSplit_batch_statement(x.split_batch_statement()));
                }
            });
        }
        if(ctx.exceptblock != null) {
            ctx.exceptblock.forEach(x -> {
                if(x.block_batch() != null) {
                    visitBlockBatch(t.exceptBlocks, x.block_batch());
                }else if(x.split_batch_statement() != null) {
                    t.exceptBlocks.add(visitSplit_batch_statement(x.split_batch_statement()));
                }
            });
        }
        return t;
    }
    public IfStatementInfo visitCmdIf(TasktoolParser.Cmd_ifContext ctx) {
        IfStatementInfo ifs = new IfStatementInfo();
        ifs.condition = visitCondition(ctx.condition());
        if(ctx.thenblock != null) {
            ctx.thenblock.forEach(x -> {
                if(x.block_base() != null) {
                    visitBlock(ifs.thenBlocks, x.block_base());
                } else if (x.cmd_exit() != null) {
                    ifs.thenBlocks.add(visitCmd_exit(x.cmd_exit()));
                }
            });
        }
        if(ctx.elseblock != null) {
            ctx.elseblock.forEach(x -> {
                if(x.block_base() != null) {
                    visitBlock(ifs.elseBlocks, x.block_base());
                } else if(x.cmd_exit() != null) {
                    ifs.elseBlocks.add(visitCmd_exit(x.cmd_exit()));
            }
            });
        }
        return ifs;
    }
    public IfStatementInfo visitCmdIf(TasktoolParser.CmdIfContext ctx) {
        return visitCmdIf(ctx.cmd_if());
    }
    public IfStatementInfo visitBatch_if(TasktoolParser.Batch_ifContext ctx) {
        IfStatementInfo ifs = new IfStatementInfo();
        ifs.condition = visitCondition(ctx.condition());
        if(ctx.thenblock != null) {
            ctx.thenblock.forEach(x -> {
                visitBlockBatch(ifs.thenBlocks, x);
            });
        }
        if(ctx.elseblock != null) {
            ctx.elseblock.forEach(x -> {
                visitBlockBatch(ifs.elseBlocks, x);
            });
        }
        return ifs;
    }
    private void visitBlock(List<BaseInfo> blocks, TasktoolParser.Block_baseContext blk) {
        if(blk.cmd_set() != null) {
            blocks.add(visitCmdSet(blk.cmd_set()));
        } else if(blk.cmd_excsql() != null) {
            blocks.add(visitCmdExcSql(blk.cmd_excsql()));
        } else if(blk.cmd_if() != null) {
            blocks.add(visitCmdIf(blk.cmd_if()));
        } else if(blk.cmd_for() != null) {
            blocks.add(visitCmdFor(blk.cmd_for()));
        } else if(blk.cmd_logoff() != null) {
            blocks.add(visitCmd_logoff(blk.cmd_logoff()));
        } else if(blk.cmd_os() != null) {
            blocks.add(visitCmdOs(blk.cmd_os()));
        } else if(blk.cmd_print() != null) {
            blocks.add(visitCmdPrint(blk.cmd_print()));
        }
    }
    private void visitBlockBatch(List<BaseInfo> blocks, TasktoolParser.Block_batchContext blk) {
        if (blk.cmd_set() != null) {
            blocks.add(visitCmdSet(blk.cmd_set()));
        } else if(blk.get_statement() != null) {
            blocks.add(visitGet_statement(blk.get_statement()));
        } else if(blk.do_statement() != null) {
            blocks.add(visitDo_statement(blk.do_statement()));
        } else if(blk.cmd_logoff() != null) {
            blocks.add(visitCmd_logoff(blk.cmd_logoff()));
        } else if(blk.cmd_os() != null) {
            blocks.add(visitCmdOs(blk.cmd_os()));
        } else if(blk.cmd_print() != null) {
            blocks.add(visitCmdPrint(blk.cmd_print()));
        } else if(blk.cmd_exit() != null) {
            blocks.add(visitCmd_exit(blk.cmd_exit()));
        } else if(blk.batch_if() != null) {
            blocks.add(visitBatch_if(blk.batch_if()));
        }
    }
    public ForStatementInfo visitCmdFor(TasktoolParser.CmdForContext ctx) {
        return visitCmdFor(ctx.cmd_for());
    }
    public ForStatementInfo visitCmdFor(TasktoolParser.Cmd_forContext ctx) {
        ForStatementInfo forstmt = new ForStatementInfo();
        if(ctx.sqlStment() != null) {
            forstmt.forKind = ForStatementInfo.FOR_DATA;
            int a = ctx.sqlStment().start.getStartIndex();
            int b = ctx.sqlStment().stop.getStopIndex();
            Interval interval = new Interval(a, b);
            forstmt.dataSql = ctx.sqlStment().start.getInputStream().getText(interval);
            forstmt.dataSql = forstmt.dataSql.substring(2, forstmt.dataSql.length() - 2);
        } else if(ctx.SEQ() != null) {
            forstmt.forKind = ForStatementInfo.FOR_SEQ;
            forstmt.forName = ctx.ID().getText();
            forstmt.from = Integer.parseInt(ctx.number(0).getText());
            forstmt.to = Integer.parseInt(ctx.number(1).getText());
            forstmt.step = ctx.number().size() == 3 ? Integer.parseInt(ctx.number(2).getText()) :
                    (forstmt.from <= forstmt.to ? 1 : -1 );
        } else {
            forstmt.forKind = ForStatementInfo.FOR_LIST;
            forstmt.forName = ctx.ID().getText();
            List<String>  list = new ArrayList<>();
            ctx.listStment().STRING_LITERAL().forEach(x -> {
                String src = x.getText().substring(0,1);
                list.add(Utils.unSlashString(Utils.unQuotaString(x.getText()), src+src, src));
            });
            forstmt.stringList = list;
        }
        if(ctx.cmd_set() != null) {
            for(TasktoolParser.Cmd_setContext x: ctx.cmd_set()){
                forstmt.withBlocks.add(visitCmdSet(x));
            }
        }

        if(ctx.block_for() != null) {
            List<BaseInfo> blocks = forstmt.doBlocks;
            ctx.block_for().forEach(blk -> {
                visitBlockFor(blocks, blk);
            });
        }
        return forstmt;
    }
    public IfStatementInfo visitFor_if(TasktoolParser.For_ifContext ctx) {
        IfStatementInfo ifs = new IfStatementInfo();
        ifs.condition = visitCondition(ctx.condition());

        if(ctx.thenblock != null) {
            ctx.thenblock.forEach(x -> visitBlockFor(ifs.thenBlocks, x));
        }
        if(ctx.elseblock != null) {
            ctx.elseblock.forEach(x -> visitBlockFor(ifs.elseBlocks, x));
        }
        return ifs;
    }
    private void visitBlockFor(List<BaseInfo> blocks, TasktoolParser.Block_forContext blk) {
        if (blk.block_base() != null) {
            visitBlock(blocks, blk.block_base());
        } else if (blk.for_break() != null) {
            blocks.add(new BreakInfo());
        } else if (blk.for_continue() != null) {
            blocks.add(new ContinueInfo());
        } else if(blk.for_if() != null) {
            blocks.add(visitFor_if(blk.for_if()));
        }
    }
}
