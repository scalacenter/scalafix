package scalafix.interfaces;

import java.nio.file.Path;
import java.util.Optional;

public interface ScalafixResult {
    /**
     *
     * @return Boolean true if there are no error on any file scalafix run on it.
     */
    Boolean isSuccessful();

    /**
     * If {@link ScalafixArguments#runAndReturnResult()} is not successful, the error is stored in this method.
     * @returnOptional<ScalafixError>
     */
    ScalafixError[] getError();

    /**
     * @return Optional<String> if the run is not successful and an error message is provided.
     */
    Optional<String> getMessageError();

    /**
     *
     * @return ScalafixOutput[] for each file we store diagnostics and patches
     * */
    ScalafixOutput[] getScalafixOutputs();

    /**
     *
     * @return if no error, write result to file configured in {@link ScalafixArguments()}
     */
    ScalafixError[] writeResult();
}
