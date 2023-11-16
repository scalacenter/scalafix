package scalafix.interfaces;

public interface ScalafixPatch {
    /**
     *
     * @return This patch as an array of text edits.
     */
    default ScalafixTextEdit[] textEdits() {
        throw new UnsupportedOperationException("textEdits() is not implemented");
    }

    /**
     *
     * @return True if this patch is atomic, else false.
     */
    default boolean isAtomic() {
        throw new UnsupportedOperationException("isAtomic() is not implemented");
    }
}
