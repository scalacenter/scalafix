package scalafix.interfaces;

import java.util.Optional;

public interface ScalafixEvaluation {
    /**
     *
     * @return boolean true if there are no error on any file scalafix run on it.
     */
    boolean isSuccessful();

    /**
     * If not successful, the error is stored in this method.
     * @returnOptional<ScalafixError>
     */
    ScalafixError[] getError();

    /**
     * @return Optional<String> if the run is not successful and an error message is provided.
     */
    Optional<String> getMessageError();

    /**
     *
     * @return ScalafixOutput[] for each file we store the scalafix evaluation: If the evaluation is successful,
     * we store the list of patches, diagnostics, the corresponding unified diff, otherwise we store the error resulted from the evaluation
     * */
    ScalafixOutput[] getScalafixOutputs();

    /**
     *
     * @return if no error, write result to file configured in {@link ScalafixArguments()}
     */
    ScalafixError[] writeResult();
}
