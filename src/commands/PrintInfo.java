package commands;

public class PrintInfo extends BaseInfo implements Cloneable{
    public String prtString;

    @Override
    public int getType() {
        return BaseInfo.PRINT;
    }

}
