import commands.BaseInfo;
import commands.DataSet;
import commands.Result;
import commands.SetInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class TaskThreads {
    private final static int MIN_INTERVAL = 20; //watch file change at least every 20 seconds
    private final DataSet rows;
    private final SetInfo params;
    private final List<SetInfo> init_blocks;
    private final List<BaseInfo> do_blocks;
    private final IForStatment handler;
    private final TaskLocks locks;
    private final HashMap<String,String> cursor;
    private final LinkedBlockingQueue<Thread> threads;
    public Result run() {
        if(params.concurrency <= 0) {
            params.concurrency = 1;
        } else if(params.concurrency > rows.getRowCount()) {
            params.concurrency = rows.getRowCount();
        }
        for(int i = 1; i <= params.concurrency; i++) {
            newTaskThread();
        }

        Thread watchThread = watchFile();
        if(watchThread != null) {
            watchThread.start();
        }

        while (locks.getThreadCount() > 0) {
            try {
                Thread t = threads.take();
                locks.decrementThread();
                t.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        if(watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        Result ret = new Result();
        ret.errorCount = locks.getErrors();
        ret.allSuccess = ret.errorCount == 0;
        if(!ret.allSuccess) {
            ret.errorCode = -999;
            ret.errorMessage = "total task " + rows.getRowCount() + ", executed: " + locks.getExecuted() + ", failed " + locks.getErrors();
            System.out.println(ret.errorMessage);
        } else {
            ret.errorMessage = "total task " + rows.getRowCount() + ", executed: " + locks.getExecuted() + ", failed 0";
            System.out.println(ret.errorMessage);
        }
        return ret;
    }
    private Thread watchFile() {
        Thread thd = null;
        SetInfo par = (SetInfo)params.clone();
        if(par.concurrencyFile != null && !par.concurrencyFile.isEmpty()) {
            if(par.concurrencyInterval < MIN_INTERVAL)  par.concurrencyInterval = MIN_INTERVAL;
            thd =  new Thread(() -> {
                while(true) {
                    try {
                        sleep(par.concurrencyInterval * 1000L);
                    } catch (InterruptedException e) {
                        if(locks.getThreadCount() == 0) break;
                    }
                    if(locks.getCurrentDataRow() > rows.getRowCount() || locks.getErrors() > par.errorLimit || locks.needBreak()) {
                        break;
                    }
                    File file = new File(par.concurrencyFile);
                    try(
                            BufferedReader br = new BufferedReader(new FileReader(file))
                        )
                    {
                        String line = br.readLine();
                        int new_threads = Integer.parseInt(line);
                        if(new_threads == par.concurrency) continue;

                        if(new_threads > par.concurrency){
                            if(locks.needDecreaseThread(false)) continue; //still decreasing threads
                            for(int i = new_threads - par.concurrency; i > 0 ; i--) {
                                newTaskThread();
                            }
                            par.concurrency = new_threads;
                        } else if(new_threads > 0){
                            if(locks.needDecreaseThread(false)) continue; //still decreasing threads
                            locks.setDecreaseThread(par.concurrency - new_threads);
                            par.concurrency = new_threads;
                        } else {
                            System.err.println("invalid threads number to be set [" + new_threads + "]");
                        }
                    } catch (Exception ie) {
                        System.err.println(ie.getMessage());
                    }
                }
            });
            // thd.start();
        }
        return  thd;
    }

    private void newTaskThread() {
        Thread t = new Thread(() -> {
            handler.doForStatment(rows,(SetInfo) params.clone(), init_blocks, do_blocks, locks,cursor);
            try {
                threads.put(Thread.currentThread());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        locks.incrementThread();
        t.start();
    }

    public TaskThreads(DataSet ds, SetInfo par, List<SetInfo> withBlock, List<BaseInfo> doBlock,IForStatment handler, HashMap<String,String> cursor ) {
        rows = ds;
        params = par;
        init_blocks = withBlock;
        do_blocks = doBlock;
        this.handler = handler;
        threads = new LinkedBlockingQueue<>();
        locks = new TaskLocks(ds.getInitialRow(),ds.getStep());
        this.cursor = cursor;
    }

}
