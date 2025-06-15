package commands;

public class BatchInfo implements Cloneable {
    public static int MIN_BATCH_ROWS = 100;
    public static int DEFAULT_BATCH_ROWS = 10000;
    public boolean reset;
    public int batchId;
    public int batchRows;
    public String processId;
    public String tableName;
    public String columnName;
    public String groupBy;
    public boolean noCross;
    public String hint = "";
    public String where;
    public String getCheckBatchSQL(SetInfo cfg){
        return String.format("select 'batched' from %s where job_id = %d  and batch_id = %d and process_id = '%s' limit 1"
                , cfg.batchTable, cfg.jobId, batchId, processId);
    }
    public String getResetBatchSQL(SetInfo cfg) {
        return String.format("delete from %s where job_id = %d  and batch_id = %d and process_id = '%s' "
                , cfg.batchTable, cfg.jobId, batchId, processId);
    }

    public String getBatchTaskSQL(SetInfo cfg) {
        return String.format("select task_id,start_key,end_key, "
                + " replace(regexp_replace(start_key,\"','.+\",''),\"('\",'') start_col1,"
                + " replace(regexp_replace(start_key,\".+','\",''),\"')\",'') start_col2,"
                + " replace(regexp_replace(end_key,\"','.+\",''),\"('\",'') end_col1,"
                + " replace(regexp_replace(end_key,\".+','\",''),\"')\",'') end_col2"
                + " from %s where job_id = %d  and batch_id = %d and process_id = '%s'  "
                + " and task_status in (0,1,2) order by rand() "
                , cfg.batchTable, cfg.jobId, batchId, processId);
    }

    public String getDefaultBatchColumn(){
        String dbname = "lower(database())";
        String tbname = tableName.toLowerCase();
        String[] str = tableName.split("\\.");
        if(str.length > 1) {
            dbname = str[0];
            tbname = str[1];
        }
        return String.format("select coalesce(group_concat(column_name),'_tidb_rowid') cols, " +
                "   coalesce(group_concat(coltype),1) colstype " +
                " from(" +
                " select  b.COLUMN_NAME, " +
                "     if(b.COLUMN_NAME is null,1,if(c.data_type in('tinyint','smallint','mediumint','int','bigint','decimal') and numeric_scale = 0,1,0)) coltype" +
                " from INFORMATION_SCHEMA.tables a" +
                " left join INFORMATION_SCHEMA.TIDB_INDEXES b" +
                " on   a.TABLE_SCHEMA = b.TABLE_SCHEMA " +
                " and  a.table_name = b.TABLE_NAME " +
                " and  lower(b.CLUSTERED)  = 'yes' " +
                " left join INFORMATION_SCHEMA.columns c " +
                " on   c.TABLE_SCHEMA = b.table_schema" +
                " and  c.TABLE_NAME = b.table_name" +
                " and  c.COLUMN_NAME = b.column_name" +
                " where lower(a.TABLE_SCHEMA) = '%s' " +
                "  and lower(a.table_name) = '%s' " +
                " order by b.SEQ_IN_INDEX  limit 2 ) t",dbname, tbname);
    }

    /**
     * if configured groupBy ,the column in groupBy must exists in columnName, otherwise
     * the split result would be wrong. e.g ( by customer_no group by txn_dt, substr(customer_no,1,4) )
     */
    public String getSplitBatchSQL(SetInfo cfg, String[] defCols) {
        if(batchRows == 0) batchRows = DEFAULT_BATCH_ROWS;
        //agg_rows used only for default split
        int agg_rows = batchRows >= MIN_BATCH_ROWS ? batchRows / 10 : MIN_BATCH_ROWS / 20;
        StringBuilder sb =  new StringBuilder();
        String insertSQL = String.format("insert into %s (job_id,batch_id,process_id,batch_table,batch_column,task_status,batch_rows,start_key,end_key)", cfg.batchTable);
        String selectSQL = String.format("select %d, %d, '%s','%s',",cfg.jobId, batchId, processId, tableName);
        sb.append(insertSQL).append(selectSQL);
        if(columnName == null) {
            String[] colName = defCols[0].split(",");
            String[] colType = defCols[1].split(",");
            sb.append("'").append(defCols[0]).append("',0,");
            if(colName.length == 1) {
                if(colType[0].trim().equals("1")) {
                    sb.append(" sum(cnt) as batchSize, min(minKey) as startKey, max(maxKey) as endKey ")
                    .append(" from ( ")
                    .append("   select minKey, maxKey, cnt, floor((sum(cnt) over(order by minKey) ) / ").append(batchRows).append(") page ")
                    .append("   from ( ")
                    .append("      select /*+ agg_to_cop()  ").append(hint == null ? "" : hint).append(" */ ")
                    .append("         min(").append(colName[0]).append(") minKey, max(").append(colName[0]).append(") maxKey, count(*) cnt ")
                    .append("      from ").append(tableName)
                    .append(       where == null ? "" : " where (" + where + ")")
                    .append("      group by floor((").append(colName[0]).append(") / ").append(agg_rows).append(" )")
                    .append("    ) t1 ")
                    .append(" ) t2 ")
                    .append("group by page");
                    //System.out.println("[\n" + sb.toString() + "\n]");
                } else {
                    sb.append(" count(*) as batchSize, min(minKey) as startKey, max(minKey) as endKey ")
                            .append(" from (")
                            .append("   select ").append(hint == null ? "" : " /*+ " + hint + " */ ")
                            .append("   floor((rank() over(order by ").append(colName[0]).append(")) / ").append(batchRows).append(") page " )
                    .append("  ,").append(colName[0]).append("  minKey ")
                    .append("  from ").append(tableName)
                    .append(where == null ? "" : " where (" + where + ")")
                    .append("  ) t1 group by page");
                }
            }else{ // 2 columns
                    sb.append("batchSize , startKey, endKey from (")
                    .append("select  distinct sum(cnt) over w as batchSize,")
                    .append(" concat('(''',FIRST_VALUE(").append(colName[0]).append(") over w,''',''',FIRST_VALUE(minKey) over w,''')') as startKey, ")
                    .append(" concat('(''',LAST_VALUE(").append(colName[0]).append(") over w,''',''',LAST_VALUE(maxKey) over w,''')') as endKey ")
                    .append(" from ( ")
                    .append(" select ").append(colName[0]).append(",minKey, maxKey, cnt, ")
                    .append(" floor((sum(cnt) over(order by ").append(colName[0]).append(", minKey)) / ").append(batchRows).append(") page")
                    .append(" from ( ")
                    .append("      select /*+ agg_to_cop()  ").append(hint == null ? "" : hint).append(" */ ");
                    if(colType[1].trim().equals("1")) {
                        sb.append(colName[0]).append(", min(").append(colName[1]).append(") minKey, max(").append(colName[1]).append(") maxKey,count(*) cnt ");
                    } else {
                        sb.append(colName[0]).append(", ").append(colName[1]).append(" minKey, ").append(colName[1]).append(" maxKey, 1 cnt ");
                    }
                    sb.append(" from ").append(tableName)
                    .append( where == null ? "" : " where (" + where + ")");
                    if(colType[1].trim().equals("1")) {
                        sb.append(" group by ").append(colName[0]).append(", floor((").append(colName[1]).append(") / ").append(agg_rows).append(" )");
                    }
                    sb.append(") t1 ")
                    .append(") t2 window w as (partition by page order by ").append(colName[0]).append(",minKey rows between UNBOUNDED PRECEDING and UNBOUNDED FOLLOWING)")
                    .append(") t3");
                    //System.out.println("[\n" + sb.toString() + "\n]");
                }
        }else {
            String[] colName = columnName.split(",");
            sb.append("'").append(columnName).append("',0,");
            if(colName.length == 1) {
                sb.append(" sum(cnt) as batchSize, min(minKey) as startKey, max(maxKey) as endKey ")
                .append(" from ( ")
                .append("   select minKey, maxKey, cnt, floor((sum(cnt) over(order by minKey) ) / ").append(batchRows).append(") page ")
                .append("   from ( ")
                .append("      select /*+ agg_to_cop()  ").append(hint == null ? "" : hint).append(" */ ")
                .append("         min(").append(colName[0]).append(") minKey, max(").append(colName[0]).append(") maxKey, count(*) cnt ")
                .append("      from ").append(tableName)
                .append(       where == null ? "" : " where (" + where + ")")
                .append("      group by ").append(groupBy != null ? groupBy : colName[0])
                .append("    ) t1 ")
                .append(" ) t2 ")
                .append("group by page");
                //System.out.println("[\n" + sb.toString() + "\n]");
            }else{
                sb.append("batchSize , startKey, endKey from (")
                .append("select  distinct sum(cnt) over w as batchSize,")
                .append(" concat('(''',FIRST_VALUE(minKey1) over w,''',''',FIRST_VALUE(minKey2) over w,''')') as startKey, ")
                .append(" concat('(''',LAST_VALUE(maxKey1)  over w,''',''',LAST_VALUE(maxKey2) over w,''')') as endKey ")
                .append(" from ( ")
                .append(" select ")
                .append(  noCross ? colName[0] + "," : "")
                .append(" minKey1,maxKey1,minKey2, maxKey2, cnt, ")
                .append(" floor((sum(cnt) over(order by minKey1, minKey2) ) / ").append(batchRows).append(") page")
                .append(" from ( ")
                .append("      select /*+ agg_to_cop()  ").append(hint == null ? "" : hint).append(" */ ")
                .append(         noCross ? colName[0] + "," : "")
                .append("        min(").append(colName[0]).append(") minKey1, max(").append(colName[0]).append(") maxKey1, min(").append(colName[1]).append(") minKey2, max(").append(colName[1]).append(") maxKey2,count(*) cnt ")
                .append("      from ").append(tableName)
                .append(       where == null ? "" : " where (" + where + ")")
                .append("      group by ").append(groupBy != null ? groupBy : columnName)
                .append("     ) t1 ")
                .append("   ) t2 window w as (partition by ").append(noCross ? colName[0] + "," : "")
                .append(" page order by minKey1, minKey2 rows between UNBOUNDED PRECEDING and UNBOUNDED FOLLOWING)")
                .append(") t3");
                //System.out.println("[\n" + sb.toString() + "\n]");
            }
        }
        return sb.toString();
    }

    @Override
    public BatchInfo clone() {
        try {
            return (BatchInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String getStartTransSQL(SetInfo param) {
        return String.format("update  %s set task_status = 1, start_time = current_timestamp(3), end_time = null, task_errcode = null,task_errmessage = null  " +
                        "where task_id = {task_id} "
                , param.batchTable);
    }

    public String getSuccessTransSQL(SetInfo param) {
        return String.format("update  %s set task_status = 3, end_time = current_timestamp(3), task_errcode = 0,task_errmessage = 'success'  " +
                        "where task_id = {task_id} "
                , param.batchTable);
    }

    public String getFailedTransSQL(SetInfo param) {
        return String.format("update  %s set task_status = 2, end_time = current_timestamp(3), task_errcode = {except:code},task_errmessage = substr('{except:message}',1,512)  " +
                        "where task_id = {task_id} "
                , param.batchTable);
    }
}
