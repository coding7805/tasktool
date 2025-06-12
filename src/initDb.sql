create table if not exists batch_task(
         task_id bigint auto_random   comment '任务编号'
        ,job_id int                   comment '作业编号: .set jobid = n '
        ,batch_id int                 comment '拆批编号:一个作业中可能有多次拆批'
        ,process_id varchar(50)       comment '任务对应的处理标识，譬如 业务日期'
        ,batch_table varchar(100)     comment '拆批源表'
        ,batch_column varchar(200)    comment '拆批字段'
        ,batch_rows int               comment '任务包含的记录数'
        ,batch_time datetime default CURRENT_TIMESTAMP   comment '拆批时间'
        ,task_status int                                 comment '任务状态 0: init 1:running 2:failed 3:success'
        ,start_key varchar(200)                          comment '记录的起始值{start_key}'
        ,end_key varchar(200)                            comment '记录的终止值{end_key}'
        ,start_time datetime(3)                             comment '任务开始时间'
        ,end_time datetime(3)                               comment '任务结束时间'
        ,task_errcode int                                comment '任务执行错误码'
        ,task_errmessage varchar(512)                    comment '任务执行错误描述'
        ,primary key(task_id) clustered
        ,key idx_job_batch(job_id,batch_id,process_id,task_status)
    );