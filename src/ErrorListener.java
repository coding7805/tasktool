import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;
public class ErrorListener extends BaseErrorListener {
        final private List<String> errors;
        public void clear() {
            errors.clear();
        }
        public List<String> getErrorList() {
            return errors;
        }
        public ErrorListener() {
            errors = new ArrayList<>();
        }
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            System.out.println("object:" + offendingSymbol.getClass());
            String errorMessage = String.format("error at line %d:%d - %s", line, charPositionInLine, msg);
            errors.add(errorMessage);
        }

}
