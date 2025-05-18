package commands;
import java.util.ArrayList;
import java.util.List;
public class ForStatementInfo extends BaseInfo implements Cloneable{
    public static int FOR_DATA = 1;
    public static int FOR_SEQ = 2;
    public static int FOR_LIST = 3;
    public int forKind;

    /* used for sequence mode */
    public String forName;
    public String dataSql;
    public int from;
    public int to;
    public int step;

    /* used for list mode */
    public List<String> stringList;
    public List<SetInfo> withBlocks;
    public List<BaseInfo> doBlocks;
    public ForStatementInfo() {
        withBlocks = new ArrayList<>();
        doBlocks = new ArrayList<>();
    }

    @Override
    public int getType() {
        return BaseInfo.FOR_STATEMENT;
    }

    @Override
    public ForStatementInfo clone() {
        ForStatementInfo c =  (ForStatementInfo) super.clone();
        c.withBlocks = new ArrayList<>();
        c.doBlocks = new ArrayList<>();
        for(SetInfo b: withBlocks) c.withBlocks.add((SetInfo)b.clone());
        for(BaseInfo b: doBlocks) c.doBlocks.add(b.clone());
        return c;
    }
}
