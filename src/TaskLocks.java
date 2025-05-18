import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaskLocks {
    private final Lock lock = new ReentrantLock();
    /* taskInfo[0]: the threads should be decreased cause of max threads decreased
       taskInfo[1]: the row id of the data that has been processed
       taskInfo[2]: the total errors
       taskInfo[3]: break status > 0 means should break the for statement
       taskInfo[4]: total threads
       taskInfo[5]: the total success
    * */
    private final int[] taskInfo;
    private final int step;
    public void incrementThread() {
        lock.lock();
        taskInfo[4] ++;
        lock.unlock();
    }
    public void decrementThread() {
        lock.lock();
        taskInfo[4] --;
        lock.unlock();
    }
    public int getThreadCount() {
        lock.lock();
        int count = taskInfo[4];
        lock.unlock();
        return count;
    }
    public int getCurrentDataRow() {
        lock.lock();
        int ret = taskInfo[1];
        lock.unlock();
        return ret;
    }
    public int getAndIncreaseDataRow() {
        lock.lock();
        taskInfo[1] += step;
        int ret = taskInfo[1];
        lock.unlock();
        return ret;
    }
    public void incErrors() {
        lock.lock();
        taskInfo[2]++;
        lock.unlock();
    }
    public int getErrors() {
        lock.lock();
        int err = taskInfo[2];
        lock.unlock();
        return err;
    }
    public void incExecuted() {
        lock.lock();
        taskInfo[5]++;
        lock.unlock();
    }
    public int getExecuted() {
        lock.lock();
        int err = taskInfo[5];
        lock.unlock();
        return err;
    }
    public void setBreak() {
        lock.lock();
        taskInfo[3] ++;
        lock.unlock();
    }
    public boolean needBreak() {
        lock.lock();
        boolean ret = taskInfo[3] > 0;
        lock.unlock();
        return ret;
    }
    public void setDecreaseThread(int count) {
        lock.lock();
        taskInfo[0] = count;
        lock.unlock();
    }
    public boolean needDecreaseThread(boolean decrease) {
        lock.lock();
        boolean ret = taskInfo[0] > 0;
        if(ret && decrease) taskInfo[0] --;
        lock.unlock();
        return ret;
    }
    public void lock() {
        lock.lock();
    }
    public void unlock() {
        lock.unlock();
    }
    public TaskLocks(int initialRow, int seqStep) {
        taskInfo = new int[6];
        step = seqStep;
        taskInfo[1] = initialRow;

    }
}
