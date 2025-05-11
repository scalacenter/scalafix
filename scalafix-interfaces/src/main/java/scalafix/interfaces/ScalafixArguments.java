package scalafix.interfaces;

import coursierapi.Repository;

import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper around arguments for invoking the Scalafix command-line interface
 * main method.
 * <p>
 * To obtain an instance of ScalafixArguments, use
 * {@link scalafix.interfaces.Scalafix#newArguments()}.
 * Instances of ScalafixArguments are immutable and thread safe.
 *
 * @implNote This interface is not intended for extension, the only
 *           implementation of this interface should live in the Scalafix
 *           repository.
 */
public interface ScalafixArguments {

    /**
     * The rules that are valid arguments for {@link #withRules(List) }.
     * <p>
     * Takes into account built-in rules as well as the tool classpath provided via
     * {@link #withToolClasspath(URLClassLoader) }.
     * 
     * @throws ScalafixException In case of an error loading the runner.
     */
    List<ScalafixRule> availableRules() throws ScalafixException;

    /**
     * Similar to {@link #run()}, but without any side effects on the source files.
     * Via the returned {@link ScalafixEvaluation}, for each file, diagnostics can
     * be inspected, and patches can be previewed and applied.
     * <p>
     * Incompatible with {@link #withMainCallback} and {@link #withMode}.
     * 
     * @throws ScalafixException In case of an error loading the runner.
     */
    ScalafixEvaluation evaluate() throws ScalafixException;

    /**
     * The rules that would run when calling {@link #run() }
     * <p>
     * Takes into account rules that are configured in .scalafix.conf.
     *
     * @throws ScalafixException In case of an error loading the runner.
     */
    List<ScalafixRule> rulesThatWillRun() throws ScalafixException;

    /**
     * Run the Scalafix commmand-line interface <code>main</code> function.
     *
     * @throws ScalafixException In case of an error loading the runner.
     */
    ScalafixError[] run() throws ScalafixException;

    /**
     * Validates that the passed arguments are valid.
     * <p>
     * Takes into account provided rules, .scalafix.conf configuration, scala
     * version, scalac options and other potential problems. The primary purpose
     * of this method is to validate the arguments before starting compilation
     * to populate {@link #withClasspath(List)}.
     *
     * @return Optional.empty in case the arguments are valid. Optional.of
     *         an exception in case of an error loading the arguments.
     * 
     * @throws ScalafixException In case of an error loading the runner.
     */
    Optional<ScalafixException> validate() throws ScalafixException;

    /**
     * @param charset Charset for reading source files from disk. Defaults to UTF-8.
     */
    ScalafixArguments withCharset(Charset charset);

    /**
     * @param classpath Full Java classpath of the module being fixed. Required for
     *                  running semantic rewrites such as ExpliticResultTypes.
     *                  Source files that are to be fixed must be compiled with the
     *                  semanticdb-scalac compiler plugin and must have
     *                  corresponding
     *                  <code>META-INF/semanticdb/../*.semanticdb</code>
     *                  payloads. The dependency classpath must be included as well
     *                  but dependency sources do not have to be compiled with
     *                  semanticdb-scalac.
     */
    ScalafixArguments withClasspath(List<Path> classpath);

    /**
     * @param config Optional path to a <code>.scalafix.conf</code>. If empty,
     *               Scalafix will infer such a file from the working directory or
     *               fallback to the default configuration.
     */
    ScalafixArguments withConfig(Optional<Path> config);

    /**
     * @param matchers Optional list of path matchers to exclude files when
     *                 expanding directories in {@link #withPaths(List)}.
     */
    ScalafixArguments withExcludedPaths(List<PathMatcher> matchers);

    /**
     * @param callback Handler for reported linter messages. If not provided,
     *                 defaults to printing linter messages to System.out.
     */
    ScalafixArguments withMainCallback(ScalafixMainCallback callback);

    /**
     * @param mode The mode to run via --check or --stdout or
     *             --auto-suppress-linter-errors or --triggered
     */
    ScalafixArguments withMode(ScalafixMainMode mode);

    /**
     * @param args Unparsed command-line arguments that are fed directly to
     *             <code>main(Array[String])</code>
     */
    ScalafixArguments withParsedArguments(List<String> args);

    /**
     * @param paths Files and directories to run Scalafix on. The ability to pass in
     *              directories is primarily supported to make it ergonomic to
     *              invoke the command-line interface. It's recommended to only pass
     *              in files with this API. Directories are recursively expanded for
     *              files matching the patterns <code>*.scala</code> and
     *              <code>*.sbt</code> and files that do not match the path matchers
     *              provided in {@link #withExcludedPaths(List)}.
     */
    ScalafixArguments withPaths(List<Path> paths);

    /**
     * @param out The output stream to use for reporting diagnostics while running
     *            Scalafix. Defaults to System.out.
     */
    ScalafixArguments withPrintStream(PrintStream out);

    /**
     * @param repositories Maven/Ivy repositories to fetch artifacts from.
     */
    ScalafixArguments withRepositories(List<Repository> repositories);

    /**
     * @param rules The rules passed via the --rules flag matching the syntax
     *              provided in <code>rules = [ "... " ]</code> in .scalafix.conf
     *              files.
     */
    ScalafixArguments withRules(List<String> rules);

    /**
     * @param options The Scala compiler flags used to compile this classpath.
     *                For example List(-Ywarn-unused-import).
     */
    ScalafixArguments withScalacOptions(List<String> options);

    /**
     * @param version The major or binary Scala version that the provided files are
     *                targeting, for the full version that was used to compile them
     *                when a classpath is provided. For example "2.12.8" or "2.12"
     *                or "2". To be able to run advanced semantic rules using the
     *                Scala Presentation Compiler (such as ExplicitResultTypes), the
     *                fullSscala version must match the binary version available in
     *                the classloader of this instance, as requested/provided in the
     *                static factory methods of {@link Scalafix}.
     * @throws ScalafixException In case of an error parsing the scalaVersion.
     */
    ScalafixArguments withScalaVersion(String version);

    /**
     * @param path The SemanticDB targetroot paths passed via
     *             --semanticdb-targetroots. Must match <code>path</code> in
     *             <code>-Xplugin:semanticdb:targetroot:{path}</path></code> if
     *             used.
     */
    ScalafixArguments withSemanticdbTargetroots(List<Path> path);

    /**
     * @param path The SemanticDB sources path passed via --sourceroot. Must match
     *             <code>path</code> in
     *             <code>-Xplugin:semanticdb:sourceroot:{path}</path></code> if
     *             used. Defaults to the current working directory.
     */
    ScalafixArguments withSourceroot(Path path);

    /**
     * @deprecated Use {@link #withToolDependencyURLs()} instead.
     */
    @Deprecated
    ScalafixArguments withToolClasspath(List<URL> customURLs);

    /**
     * @deprecated Use {@link #withToolDependencyURLs()} &
     *             {@link #withToolDependencyCoordinates()} instead.
     */
    @Deprecated
    ScalafixArguments withToolClasspath(
            List<URL> customURLs,
            List<String> customDependenciesCoordinates);

    /**
     * @deprecated Use {@link #withToolDependencyURLs()},
     *             {@link #withToolDependencyCoordinates()} &
     *             {@link #withRepositories()} instead.
     */
    @Deprecated
    ScalafixArguments withToolClasspath(
            List<URL> customURLs,
            List<String> customDependenciesCoordinates,
            List<Repository> repositories);

    /**
     * @deprecated Not supported when {@link Scalafix#get()} is used. Use
     *             higher-level {@link #withToolDependencyURLs()},
     *             {@link #withToolDependencyCoordinates()} &
     *             {@link #withRepositories()} instead.
     */
    @Deprecated
    ScalafixArguments withToolClasspath(URLClassLoader toolClasspath);

    /**
     * @param dependencyCoordinates Extra dependencies for classloading and
     *                              compiling external rules. For example
     *                              "com.nequissimus::sort-imports:0.5.2".
     *                              Artifacts will be resolved against the
     *                              Scala version in the classloader of the parent
     *                              {@link Scalafix} instance and fetched using
     *                              Coursier.
     */
    ScalafixArguments withToolDependencyCoordinates(List<String> dependencyCoordinates);

    /**
     * @param dependencyURLs Extra URLs for classloading and compiling external
     *                       rules.
     */
    ScalafixArguments withToolDependencyURLs(List<URL> dependencyURLs);

    /**
     * @param path The working directory of where to invoke the command-line
     *             interface. Primarily used to absolutize relative directories
     *             passed via {@link ScalafixArguments#withPaths(List) } and also to
     *             auto-detect the location of <code>.scalafix.conf</code>.
     */
    ScalafixArguments withWorkingDirectory(Path path);
}
