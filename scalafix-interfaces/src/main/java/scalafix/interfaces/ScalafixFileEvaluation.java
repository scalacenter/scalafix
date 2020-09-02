package scalafix.interfaces;

import java.nio.file.Path;
import java.util.Optional;

public interface ScalafixFileEvaluation {

    /**
     * @return original path of the file scalafix has evaluated on
     */
    Path getEvaluatedFile();

    /**
     *
     * @return corresponding list of rules that has been evaluated
     */
    ScalafixRule[] getEvaluatedRules();

    /**
     *
     * @return boolean true if there is no error when scalafix has been evaluated on the file
     */
    boolean isSuccessful();

    ScalafixError[] getErrors();

    ScalafixDiagnostic[] getDiagnostics();

    /**
     *
     * @return if the fixed file is different from the input file and the evaluation
     * is successful this method return non empty unified diff. The original file will stay unchanged.
     */
    Optional<String> previewPatchesAsUnifiedDiff();

    ScalafixPatch[] getPatches();

    /**
     *
     * @return It returns the new content of the file after scalafix has been evaluated on it if no error.
     * The original file will stay unchanged.
     */
    Optional<String> previewPatches();

    /**
     *
     * @return apply all patches and write the result to file.
     */
    ScalafixError[] applyPatches();

    /**
     * @return It returns the new content file after applying the desired patches if no error.
     * The original file will stay unchanged.
     */
    Optional<String> previewPatches(ScalafixPatch[] patches);

    /**
     *
     * @param patches the patches should belong to this scalafixFileEvaluation's patches.
     *                If you apply patches that result from another ScalafixFileEvaluation,
     *                no patch will be applied to the file.
     */
    ScalafixError[] applyPatches(ScalafixPatch[] patches);
}
