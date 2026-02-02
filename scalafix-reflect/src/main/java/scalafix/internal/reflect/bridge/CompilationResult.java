package scalafix.internal.reflect.bridge;

import java.io.Serializable;
import java.util.List;

// Changes in this file must be mirrored in IsolatedScala213Compiler which uses reflection
public final class CompilationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public final boolean success;
    public final List<CompilerMessage> messages;

    public CompilationResult(boolean success, List<CompilerMessage> messages) {
        this.success = success;
        this.messages = messages;
    }

    @Override
    public String toString() {
        if (success) {
            return "CompilationResult(success=true)";
        } else {
            return "CompilationResult(success=false, messages=" + messages.size() + ")";
        }
    }
}
