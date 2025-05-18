package commands;

public abstract class BaseInfo implements Cloneable {
    public static int LOGON = 1;
    public static int LOGOFF = 2;
    public static int EXIT = 3;
    public static int SET = 4;
    public static int EXC_SQL = 5;
    public static int IF_STATEMENT = 6;
    public static int CONDITION = 7;
    public static int FOR_STATEMENT = 8;
    public static int FOR_BREAK = 9;
    public static int FOR_CONTINUE = 10;
    public static int OS_CMD = 11;
    public static int PRINT = 12;
    public static int HELP = 13;
    public static int RUN_BATCH_STATEMENT = 14;
    public static int TRY_STATMENT = 15;
    public abstract int getType();
    @Override
    public BaseInfo clone() {
        try {
            return (BaseInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
