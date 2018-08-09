package scalafix.interfaces;

import java.util.Optional;

/**
 * A diagnostic such as a linter message or general error reported by Scalafix.
 */
public interface ScalafixDiagnostic {


    /**
     * @return The short message of this diagnostic.
     */
    String message();

    /**
     * @return An optional detailed explanation for this diagnostic. May be empty.
     */
    String explanation();

    /**
     * @return The severity of this message: error, warning or info.
     */
    ScalafixSeverity severity();

    /**
     * @return The source position where this diagnostic points to, if any.
     */
    Optional<ScalafixPosition> position();

    /**
     * @return The unique identifier for the category of this linter message, if any.
     */
    Optional<ScalafixLintID> lintID();

}
