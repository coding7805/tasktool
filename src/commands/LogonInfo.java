package commands;

public class LogonInfo extends BaseInfo implements Cloneable{
    public String hosts;
    public String user;
    public String password;
    public boolean needDecode;
    @Override
    public int getType() {
        return BaseInfo.LOGON;
    }
}
