package commands;

import java.util.ArrayList;
import java.util.List;

public class IfStatementInfo extends BaseInfo implements Cloneable{
    public ConditionInfo condition;
    public List<BaseInfo> thenBlocks;
    public List<BaseInfo> elseBlocks;
    public IfStatementInfo() {
        thenBlocks = new ArrayList<>();
        elseBlocks = new ArrayList<>();
    }

    @Override
    public int getType() {
        return BaseInfo.IF_STATEMENT;
    }

    @Override
    public IfStatementInfo clone() {
        IfStatementInfo c =  (IfStatementInfo) super.clone();
        c.thenBlocks = new ArrayList<>();
        c.elseBlocks = new ArrayList<>();
        for(BaseInfo b: thenBlocks) c.thenBlocks.add(b.clone());
        for(BaseInfo b: elseBlocks) c.elseBlocks.add(b.clone());
        return c;
    }
}
