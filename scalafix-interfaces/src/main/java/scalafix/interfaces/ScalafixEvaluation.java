package scalafix.interfaces;

import java.util.Optional;

public interface ScalafixEvaluation {
    /**
     * @return boolean true if scalafix ran on at least one file; there might be errors in file evaluation though
     */
    boolean isSuccessful();

    Optional<ScalafixEvaluationError> getError();

    /**
     * @deprecated  replaced by {@link #getError()}
     */
    @Deprecated ScalafixError[] getErrors();
    /**
     * @deprecated  replaced by {@link #getErrorMessage()}
     */
    @Deprecated Optional<String> getMessageError();

    Optional<String> getErrorMessage();

    /**
     * @return for each file we store the scalafix evaluation: If the evaluation is successful,
     * we store the list of patches, diagnostics and the corresponding unified diff,
     * otherwise we store the errors resulted from the evaluation
     */
    ScalafixFileEvaluation[] getFileEvaluations();

    /**
     * @return Applies all patches for all files
     */
    ScalafixError[] apply();
}
