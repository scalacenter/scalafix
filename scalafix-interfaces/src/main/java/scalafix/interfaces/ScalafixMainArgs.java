package scalafix.interfaces;

import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * Wrapper around arguments for invoking the Scalafix command-line interface main method.
 * <p>
 * To obtain an instance of MainArgs, use {@link scalafix.interfaces.Scalafix#newMainArgs()}.
 * Instances of MainArgs are immutable and thread safe. It is safe to re-use the same
 * MainArgs instance for multiple Scalafix invocations.
 *
 * @implNote This interface is not intended for extension, the only implementation of this interface
 * should live in the Scalafix repository.
 */
public interface ScalafixMainArgs {

    /**
     * @param rules The rules passed via the --rules flag matching the syntax provided in
     *              <code>rules = [ "... " ]</code> in .scalafix.conf files.
     */
    ScalafixMainArgs withRules(List<String> rules);

    /**
     * @param toolClasspath Custom classpath for classloading and compiling external rules.
     *                      Must be a URLClassLoader (not regular ClassLoader) to support
     *                      compiling sources.
     */
    ScalafixMainArgs withToolClasspath(URLClassLoader toolClasspath);

    /**
     * @param paths Files and directories to run Scalafix on. Directories are recursively expanded
     *              for files matching the patterns <code>*.scala</code> and <code>*.sbt</code>.
     */
    ScalafixMainArgs withPaths(List<Path> paths);

    /**
     * @param path The working directory of where to invoke the command-line interface.
     *             Primarily used to absolutize relative directories passed via
     *             {@link ScalafixMainArgs#withPaths(List) } and also to auto-detect the
     *             location of <code>.scalafix.conf</code>.
     */
    ScalafixMainArgs withWorkingDirectory(Path path);

    /**
     * @param path Optional path to a <code>.scalafix.conf</code>. If not provided, Scalafix
     *             will infer such a file from the working directory or fallback to the default
     *             configuration.
     */
    ScalafixMainArgs withConfig(Path path);

    /**
     * @param mode The mode to run via --test or --stdout or --auto-suppress-linter-errors
     */
    ScalafixMainArgs withMode(ScalafixMainMode mode);

    /**
     * @param args Unparsed command-line arguments that are fed directly to <code>main(Array[String])</code>
     */
    ScalafixMainArgs withArgs(List<String> args);

    /**
     * @param out The output stream to use for reporting diagnostics while running Scalafix.
     *            Defaults to System.out.
     */
    ScalafixMainArgs withPrintStream(PrintStream out);

    /**
     * @param classpath Full Java classpath of the module being fixed. Required for running
     *                  semantic rewrites such as ExpliticResultTypes. Source files that
     *                  are to be fixed must be compiled with the semanticdb-scalac compiler
     *                  plugin and must have corresponding <code>META-INF/semanticdb/../*.semanticdb</code>
     *                  payloads. The dependency classpath must be included as well but dependency
     *                  sources do not have to be compiled with semanticdb-scalac.
     */
    ScalafixMainArgs withClasspath(List<Path> classpath);

    /**
     * @param path The SemanticDB sources path passed via --sourceroot. Must match <code>path</code>
     *             in <code>-Xplugin:semanticdb:sourceroot:{path}</path></code> if used. Defaults
     *             to the current working directory.
     */
    ScalafixMainArgs withSourceroot(Path path);

    /**
     * @param callback Handler for reported linter messages. If not provided, defaults to printing
     *                 linter messages to the Stdout.
     */
    ScalafixMainArgs withMainCallback(ScalafixMainCallback callback);


    /**
     * @param charset Charset for reading source files from disk. Defaults to UTF-8.
     */
    ScalafixMainArgs withCharset(Charset charset);
}
