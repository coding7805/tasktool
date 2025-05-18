import commands.BaseInfo;
import commands.DataSet;
import commands.Result;
import commands.SetInfo;

import java.util.HashMap;
import java.util.List;

public interface IForStatment {
    Result doForStatment(DataSet data, SetInfo par,
                         List<SetInfo> initBlocks,
                         List<BaseInfo> doBlocks,
                         TaskLocks locks,
                         HashMap<String,String> cursorCtx);
}
