package scalafix.interfaces;

import java.nio.file.Path;
import java.util.Optional;

public interface ScalafixOutput {

    /**
     *
     * @return original path of the file to be fixed
     */
    Path getPath();

    /**
     *
     * @return corresponding list of rules that has been applied
     */
    ScalafixRule[] getRules();

    /**
     *
     * @return Boolean true if there are no error when scalafix fixed the file
     */
    Boolean isSuccessful();
    /**
     *
     * @return is not successful, the error is stored in this method.
     */
    ScalafixError[] getError();


    /**
     *
     * @return list of diagnostics
     */
    ScalafixDiagnostic[] getDiagnostics();

    /**
     *
     * @return if the fixed file is different from the input file, this method return non empty unified diff
     */
    Optional<String> getUnifiedDiff();

    /**
     *
     * @return the output file after running scalafix if no error
     */
    Optional<String> getOutputFileFixed();

    /**
     *
     * @return scalafixPatches
     */
    ScalafixPatch[] getPatches();

    /**
     *
     * @return apply all patches and write the result to file.
     */
    ScalafixError[] applyPatches();

    /**
     *
     * @return Optional<String> containes the new file if no error.
     */
    Optional<String> getOutputFixedWithSelectivePatches(ScalafixPatch[] patches);

    /**
     *
     * @return apply selected patches to file
     */
    ScalafixError[] applySelectivePatches(ScalafixPatch[] patches);
}
