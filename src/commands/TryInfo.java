package commands;

import java.util.ArrayList;
import java.util.List;

public class TryInfo extends BaseInfo implements Cloneable{
    public List<BaseInfo> tryBlocks;
    public List<BaseInfo> exceptBlocks;
    public TryInfo() {
        tryBlocks = new ArrayList<>();
        exceptBlocks = new ArrayList<>();
    }

    @Override
    public int getType() {
        return BaseInfo.TRY_STATMENT;
    }

    @Override
    public TryInfo clone() {
        TryInfo c =  (TryInfo) super.clone();
        c.tryBlocks = new ArrayList<>();
        c.exceptBlocks = new ArrayList<>();
        for(BaseInfo b: tryBlocks) c.tryBlocks.add(b.clone());
        for(BaseInfo b: exceptBlocks) c.exceptBlocks.add(b.clone());
        return c;
    }
}
