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
     * @return corresponding list of rules that has been evaluated
     */
    ScalafixRule[] getRules();

    /**
     *
     * @return boolean true if there are no error when scalafix run on the file
     */
    boolean isSuccessful();

    ScalafixError[] getErrors();

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

    ScalafixPatch[] getPatches();

    /**
     *
     * @return the output content after evaluating scalafix if no error. The original file will stay unchanged.
     */
    Optional<String> getOutputFixed();

    /**
     *
     * @return apply all patches and write the result to file.
     */
    ScalafixError[] applyPatches();

    /**
     *
     * @return Optional<String> the output content after evaluating scalafix with desired patches if no error.
     * The original file will stay unchanged.
     */
    Optional<String> getOutputFixedWithSelectivePatches(ScalafixPatch[] patches);

    /**applySelectivePatches
     *
     * @return apply selected patches to file
     */
    ScalafixError[] applySelectivePatches(ScalafixPatch[] patches);
}
