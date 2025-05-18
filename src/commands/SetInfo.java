package commands;

public class SetInfo extends BaseInfo implements Cloneable{
    public static final int SET_LOG = 1;
    public static final int SET_ERROR = 2;
    public static final int SET_CONCURRENCY = 3;
    public static final int SET_RETRY = 4;
    public static final int SET_DATABASE = 5;
    public static final int SET_DRIVER = 6;
    public static final int SET_JOBID = 7;
    public static final int SET_BATCH_TABLE = 8;
    public static final int LOG_SUMMARY = 0;
    public static final int LOG_DETAIL = 1;
    public static final int DEFAULT_RETRY_INTERVAL_SECS = 10;

    public int logFormat;
    public int errorLimit;
    public int concurrency;
    public String concurrencyFile;
    public String driverFile;
    public int concurrencyInterval;
    public int retryCount;    // 0 means turn off retry
    public int retryInterval; // must be positive
    public int retryIntervalRandom; //must be positive
    public Integer jobId;
    public String batchTable = "batch_task";  //default table name

    //the session created in the for statement doesn't inherit the database session set info
    //set it in the "with" partition of the for statement
    public String dbSetSQL;
    public int operator;
    public boolean isLogDetail() {
        return logFormat == LOG_DETAIL;
    }

    @Override
    public int getType() {
        return BaseInfo.SET;
    }

}
