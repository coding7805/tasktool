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
    private int exceptCode; // only for try statement
    private String exceptMsg; //only for try statement
    public void setException(int code, String msg) {
        exceptCode = code;
        exceptMsg = msg;
    }
    public boolean hasException() { return exceptCode != 0;}
    public int getExceptCode() { return exceptCode; };
    public String getExceptMsg() { return exceptMsg;}
    public boolean hasError() {
        return errorCode != 0;
    }
    public void setSqlErrorMsg(int code, String msg) {
        sqlCode = code;
        sqlErrorMsg = msg;
        errorCode = code;
        errorMessage = msg;
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
    public boolean needContinue() {
        return needContinue;
    }
    public boolean needBreak() {
        return needBreak;
    }

    public void setContinue(boolean blContinue) {
        needContinue = blContinue;
    }
    public void setBreak(boolean blBreak) {
        needBreak = blBreak;
    }

    public void reset() {
        setValue(0,0, "", 0,true,0,null);
    }

    @Override
    public Result clone() {
        try {
            return (Result) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
