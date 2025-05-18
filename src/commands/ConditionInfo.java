package commands;


public class ConditionInfo extends BaseInfo implements Cloneable{
    public static final int ERROR_CODE = 1;
    public static final int ACTIVITY_COUNT = 2;
    public static final int ALL_SUCCESS = 3;
    public static final int ERROR_COUNT = 4;
    public static final int OS_CODE = 5;
    public String op;
    public String value;
    public int conType;
    public boolean isTrue(Result result) {
        return isTrue(result.errorCode,result.activityCount,result.errorCount, result.allSuccess, result.osExitCode);
    }
    private boolean isTrue(int errCode, long activityCount, int errorCount, boolean allSuccess, int osCode) {
        switch (conType) {
            case ERROR_CODE : return compare(errCode);
            case ERROR_COUNT : return compare(errorCount);
            case ACTIVITY_COUNT : return  compare(activityCount);
            case ALL_SUCCESS : return value.isEmpty() == allSuccess;
            case OS_CODE : return compare(osCode);
            default :return  true;
        }
    }
    private boolean compare(long cmpValue) {
        long v = Long.parseLong(value);
        switch (op) {
            case ">" : return cmpValue > v;
            case "<" : return cmpValue < v;
            case "<>":
            case "!=":
                return cmpValue != v;
            case ">=" : return cmpValue >= v;
            case "<=" : return cmpValue <= v;
            case "=" : return  cmpValue == v;
            default : return  true;
        }
    }
    @Override
    public int getType() {
        return BaseInfo.CONDITION;
    }
}
