create table if not exists batch_task(
         task_id bigint auto_random
        ,job_id int
        ,batch_id int
        ,process_id varchar(50)
        ,batch_table varchar(100)
        ,batch_column varchar(200)
        ,batch_rows int
        ,batch_time datetime default CURRENT_TIMESTAMP
        ,task_status int -- 0: init 1:running 2:failed 3:success
        ,start_key varchar(200)
        ,end_key varchar(200)
        ,start_time datetime
        ,end_time datetime
        ,task_errcode int
        ,task_errmessage varchar(256)
        ,primary key(task_id) clustered
        ,key idx_job_batch(job_id,batch_id,process_id,task_status)
    )
