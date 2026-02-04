package scalafix.internal.reflect.bridge;

import java.io.Serializable;

// Changes in this file must be mirrored in IsolatedScala213Compiler which uses reflection
public final class CompilerMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String severity;
    public final int start;
    public final int end;
    public final String text;

    public CompilerMessage(String severity, int start, int end, String text) {
        this.severity = severity;
        this.start = start;
        this.end = end;
        this.text = text;
    }

    @Override
    public String toString() {
        return severity + " [" + start + "-" + end + "]: " + text;
    }
}
