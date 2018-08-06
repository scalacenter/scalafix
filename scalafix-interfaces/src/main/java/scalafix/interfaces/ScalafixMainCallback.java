package scalafix.interfaces;

/**
 * Callback handler for events that happen in the command-line interface.
 */
public interface ScalafixMainCallback {
    /**
     * Handle a diagnostic event reported by the Scalafix reporter.
     *
     * @param diagnostic the reported diagnostic.
     */
    void reportDiagnostic(ScalafixDiagnostic diagnostic);
}
