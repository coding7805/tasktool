package commands;

import java.util.HashMap;

public class SqlInfo extends BaseInfo implements Cloneable{
    public final static int SELECT = 0;
    public final static int NonSELECT = 1;
    public final static int Batch = 2;
    public final static int RunBatch = 3;
    public boolean retry;
    public HashMap<Integer,String> errorList;
    public int type;
    public String sql;
    public BatchInfo batch;
    public ForStatementInfo forStmt; //use for run batch

    public SqlInfo() {}
    public SqlInfo(int type, String sql) {
        this.type = type;
        this.sql = sql;
    }
    @Override
    public int getType() {
        return BaseInfo.EXC_SQL;
    }


    @Override
    public SqlInfo clone() {
        SqlInfo c = (SqlInfo) super.clone();
        if(batch != null) {
            c.batch = batch.clone();
        }
        if(forStmt != null) {
            c.forStmt = forStmt.clone();
        }
        return c;
    }
}
