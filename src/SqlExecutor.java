import commands.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlExecutor {
    DbCfg dbcfg;
    Result result;
    Connection conn;

    public void logoff(){
        result.reset();
        try {
            if(conn != null) {
                conn.close();
                System.out.println("logoff from database successfully!");
                conn = null;
            }
            result.setValue(0,0,"ok", 0,true, 0,null);
        } catch (SQLException e) {
            setSQLException(e);
        }
    }
    public void logon() {
        result.reset();
        try {
            if(conn != null) conn.close();
            conn = dbcfg.getConnection();
            result.setValue(0,0,"ok", 0,true, 0,null);
        } catch (SQLException e) {
            setSQLException(e);
        }
    }
    public void splitBatch(BatchInfo b, SetInfo param) {
        result.reset();
        result.resetSqlError();
        if(needLogon()) return;

        try(Statement stmt = conn.createStatement())
        {
            ResultSet rs = stmt.executeQuery(b.getCheckBatchSQL(param));
            boolean batched = rs.next();
            rs.close();
            if(batched) {
                if(! b.reset) {
                    Utils.log(String.format(" job: %d batchid: %d procid: %s has already batched, skip it."
                            , param.jobId, b.batchId, b.processId));
                    return;
                }else{
                    stmt.execute(b.getResetBatchSQL(param));
                }
            }

            //if(b.batchRows < BatchInfo.MIN_BATCH_ROWS) {
            //    b.batchRows = BatchInfo.MIN_BATCH_ROWS;
            //    Utils.log(String.format(" batch size < %d , set batch size to %d ",b.batchRows,b.batchRows));
            //}
            String[] defBatchCols = null;
            if(b.columnName == null) {
                defBatchCols = new String[2];
                rs = stmt.executeQuery(b.getDefaultBatchColumn());
                if(rs.next()) {
                    defBatchCols[0] = rs.getString(1);
                    defBatchCols[1] = rs.getString(2);
                }
                rs.close();
            }
            stmt.execute(b.getSplitBatchSQL(param, defBatchCols));
            result.activityCount = stmt.getLargeUpdateCount();
            result.setSqlErrorMsg(0,String.format("split batch for [job: %d batchid: %d procid: %s] successfully."
                    , param.jobId, b.batchId, b.processId));
            Utils.log(result.sqlErrorMsg);
            String cols = b.columnName == null ? defBatchCols[0] : b.columnName;
            if(cols.contains(",")) {
                Utils.log(String.format("use (%s) >= ({start_key}) and (%s) <= ({end_key}) to qualify the data.",cols,cols));
                Utils.log("or use {start_col1} {start_col2} {end_col1} {end_col2} to qualify the data.");
            }else{
                Utils.log(String.format("use %s between {start_key} and {end_key} to qualify the data.",cols));
            }
        } catch (SQLException e) {
            setSQLException(e);
        }
    }
    public void execute(int sqlType, String sql) {
        result.reset();
        result.resetSqlError();
        if(needLogon()) return;

        try(Statement stmt = conn.createStatement())
        {
            if (sqlType == SqlInfo.SELECT) {
                stmt.setFetchSize(10000);
                result.rows = new DataSet(stmt.executeQuery(sql));
                result.activityCount = result.rows.getRowCount();
            } else {
                if(stmt.execute(sql)) {
                    ResultSet rs = stmt.getResultSet();
                    int rows = 0;
                    while(rs.next()) rows++;
                    rs.close();
                    result.activityCount = rows;
                } else {
                    result.activityCount = stmt.getLargeUpdateCount();
                }
            }
        } catch (SQLException e) {
            setSQLException(e);
        }
    }
    private boolean needLogon() {
        if(conn == null) {
            result.setValue(-1,0,"please logon to database first!",0,false,0,null);
            return true;
        }
        return false;
    }
    private void setSQLException(SQLException e){
        // SQLException raised, but e.getErrorCode() == 0 (e.g: connection broken)
        int sqlcode = e.getErrorCode() == 0 ? -2 : e.getErrorCode();
        String msg = e.getMessage();
        result.setValue(sqlcode,0,msg,0,false,0,null);
        result.setSqlErrorMsg(sqlcode, msg);
    }
    public SqlExecutor(DbCfg cfg, Result result) {
        dbcfg = cfg;
        this.result = result;
    }
}
