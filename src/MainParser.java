import generated.TasktoolLexer;
import generated.TasktoolParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.util.List;

public class MainParser  {
    ErrorListener errlsr;
    public List<String> getErrorList() {
        return errlsr.getErrorList();
    }
    public TasktoolParser.CommandsContext parseFile(String file) {
        errlsr.clear();
        TasktoolLexer lexer = null;
        try {
            lexer = new TasktoolLexer(CharStreams.fromFileName(file));
        } catch (IOException e) {
            return null;
        }
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TasktoolParser parser = new TasktoolParser(tokens);
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errlsr);
        parser.addErrorListener(errlsr);
        return parser.commands();
    }
    public TasktoolParser.CommandsContext parse(String cmd) {
        errlsr.clear();
        TasktoolLexer lexer = new TasktoolLexer(CharStreams.fromString(cmd));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TasktoolParser parser = new TasktoolParser(tokens);
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errlsr);
        parser.addErrorListener(errlsr);
        return parser.commands();
    }
    public MainParser() {
        errlsr = new ErrorListener();
    }
}
