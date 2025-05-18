package commands;

public class ExitInfo extends BaseInfo implements Cloneable{
    public int exitCode;

    @Override
    public int getType() {
        return BaseInfo.EXIT;
    }
}
