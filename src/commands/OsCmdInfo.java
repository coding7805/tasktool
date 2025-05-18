package commands;

import java.util.HashMap;

public class OsCmdInfo extends BaseInfo implements Cloneable{
    public String cmdString;
    public boolean retry;
    public HashMap<Integer,String> errorList;

    @Override
    public int getType() {
        return BaseInfo.OS_CMD;
    }

}
