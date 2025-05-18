package commands;

import java.sql.ResultSet;

public class Result implements Cloneable{
    public int errorCode;
    public long  activityCount;
    public String errorMessage;
    public int errorCount;
    public int osExitCode;
    public boolean allSuccess;
    private boolean needContinue;  // continue statement in if-statement embedded in for-statement
    private boolean needBreak;  // break statement in if-statement embedded in for-statement
    public DataSet rows;
    public int sqlCode;
    public String sqlErrorMsg;
    public int exceptionCode;   // only for try exception statement
    public String exceptionMessage; // only for try exception statement
    public void setException() {
        exceptionCode = sqlCode;
        exceptionMessage = sqlErrorMsg.replaceAll("'","''");
    }
    public void resetException() {
        exceptionCode = 0;
        exceptionMessage = "";
    }
    public void setSqlErrorMsg(int code, String msg) {
        sqlCode = code;
        sqlErrorMsg = msg;
    }
    public void resetSqlError() {
        sqlCode = 0;
        sqlErrorMsg = "";
    }
    public void setValue(int code, long count, String msg,int errCount,boolean success, int osCode, ResultSet set) {
        errorCode = code;
        activityCount = count;
        errorMessage = msg;
        errorCount = errCount;
        osExitCode = osCode;
        allSuccess = success;
        rows = null;
        if(set != null) {
            rows = new DataSet(set);
        }
    }
    public boolean isNeedContinue() {
        return needContinue;
    }
    public boolean isNeedBreak() {
        return needBreak;
    }

    public void setContinue(boolean blContinue) {
        needContinue = blContinue;
    }
    public void setBreak(boolean blBreak) {
        needBreak = blBreak;
    }
    public void setValue(int code, long count, String msg,int errCount,boolean success, int osCode, ResultSet set, boolean blContinue,boolean blBreak) {
        setValue(code,count,msg,errCount,success,osCode,set);
        needContinue = blContinue;
        needBreak = blBreak;
    }
    public void reset() {
        setValue(0,0, "", 0,true,0,null);
    }
}
