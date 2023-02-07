package scalafix.interfaces;

public interface ScalafixPatch {
    /**
     *
     * @return This patch as an array of text edits.
     */
    default ScalafixTextEdit[] textEdits() {
        throw new UnsupportedOperationException("textEdits() is not implemented");
    }
}
