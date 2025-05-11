package scalafix.loader.internal;

import com.typesafe.config.*;

import coursierapi.Repository;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import scalafix.interfaces.Scalafix;
import scalafix.interfaces.ScalafixArguments;
import scalafix.interfaces.ScalafixError;
import scalafix.interfaces.ScalafixEvaluation;
import scalafix.interfaces.ScalafixException;
import scalafix.interfaces.ScalafixMainCallback;
import scalafix.interfaces.ScalafixMainMode;
import scalafix.interfaces.ScalafixRule;
import scalafix.interfaces.ScalafixVersions;
import scalafix.internal.interfaces.ScalafixCoursier;
import scalafix.internal.interfaces.ScalafixInterfacesClassloader;
import scalafix.internal.interfaces.ScalafixProperties;

@Value
@With
@AllArgsConstructor
public class ScalafixArgumentsImpl implements ScalafixArguments {

    // Fetch arguments
    @NonNull Optional<Path> config;
    @NonNull List<Repository> repositories;
    @NonNull String scalaVersion;
    @NonNull Path workingDirectory;

    // Load arguments
    List<String> toolDependencyCoordinates;
    List<URL> toolDependencyURLs;

    // Run arguments
    Charset charset;
    List<Path> classpath;
    List<PathMatcher> excludedPaths;
    ScalafixMainCallback mainCallback;
    ScalafixMainMode mode;
    List<String> parsedArguments;
    List<Path> paths;
    PrintStream printStream;
    List<String> rules;
    List<String> scalacOptions;
    List<Path> semanticdbTargetroots;
    Path sourceroot;

    public ScalafixArgumentsImpl() {
        // Fetch arguments
        this.config = Optional.empty();
        this.repositories = Repository.defaults();
        this.scalaVersion = "3";
        this.workingDirectory = Paths.get(System.getProperty("user.dir"));

        // Load arguments
        this.toolDependencyCoordinates = null;
        this.toolDependencyURLs = null;

        // Run arguments
        this.charset = null;
        this.classpath = null;
        this.excludedPaths = null;
        this.mainCallback = null;
        this.mode = null;
        this.parsedArguments = null;
        this.paths = null;
        this.printStream = null;
        this.rules = null;
        this.scalacOptions = null;
        this.semanticdbTargetroots = null;
        this.sourceroot = null;
    }

    @Override
    public List<ScalafixRule> availableRules() throws ScalafixException {
        return load().availableRules();
    }

    @Override
    public ScalafixEvaluation evaluate() throws ScalafixException {
        return load().evaluate();
    }

    @Override
    public List<ScalafixRule> rulesThatWillRun() throws ScalafixException {
        return load().rulesThatWillRun();
    }

    @Override
    public ScalafixError[] run() throws ScalafixException {
        return load().run();
    }

    @Override
    public Optional<ScalafixException> validate() throws ScalafixException {
        return load().validate();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ScalafixArguments withToolClasspath(
            List<URL> customURLs) {
        return withToolDependencyURLs(customURLs);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ScalafixArguments withToolClasspath(
            List<URL> customURLs,
            List<String> customDependenciesCoordinates) {
        return withToolDependencyURLs(customURLs)
                .withToolDependencyCoordinates(customDependenciesCoordinates);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ScalafixArguments withToolClasspath(
            List<URL> customURLs,
            List<String> customDependenciesCoordinates,
            List<Repository> repositories) {
        return withToolDependencyURLs(customURLs)
                .withToolDependencyCoordinates(customDependenciesCoordinates)
                .withRepositories(repositories);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ScalafixArguments withToolClasspath(
            URLClassLoader toolClasspath) {
        throw new UnsupportedOperationException(
                "Unsupported method 'withToolClasspath', use withToolDependency*() instead");
    }

    private ScalafixArguments load() throws ScalafixException {
        ScalafixVersions scalafixVersions = versionsForConfigOrBuiltinScalafixVersion();

        List<URL> cliJars = ScalafixCoursier.scalafixCliJars(
                repositories,
                scalafixVersions.scalafixVersion(),
                scalafixVersions.cliScalaVersion(scalaVersion));
        ClassLoader parent = new ScalafixInterfacesClassloader(ScalafixArgumentsImpl.class.getClassLoader());
        ClassLoader classLoader = new URLClassLoader(cliJars.stream().toArray(URL[]::new), parent);

        // TODO: memoize
        ScalafixArguments arguments = classLoadScalafixArguments(classLoader)
                .withConfig(config)
                .withRepositories(repositories)
                .withScalaVersion(scalaVersion)
                .withWorkingDirectory(workingDirectory);

        if (toolDependencyCoordinates != null) {
            arguments = arguments.withToolDependencyCoordinates(toolDependencyCoordinates);
        }

        if (toolDependencyURLs != null) {
            arguments = arguments.withToolDependencyURLs(toolDependencyURLs);
        }

        if (charset != null) {
            arguments = arguments.withCharset(charset);
        }

        if (classpath != null) {
            arguments = arguments.withClasspath(classpath);
        }

        if (excludedPaths != null) {
            arguments = arguments.withExcludedPaths(excludedPaths);
        }

        if (mainCallback != null) {
            arguments = arguments.withMainCallback(mainCallback);
        }

        if (mode != null) {
            arguments = arguments.withMode(mode);
        }

        if (paths != null) {
            arguments = arguments.withPaths(paths);
        }

        if (printStream != null) {
            arguments = arguments.withPrintStream(printStream);
        }

        if (rules != null) {
            arguments = arguments.withRules(rules);
        }
        if (scalacOptions != null) {
            arguments = arguments.withScalacOptions(scalacOptions);
        }

        if (semanticdbTargetroots != null) {
            arguments = arguments.withSemanticdbTargetroots(semanticdbTargetroots);
        }

        if (sourceroot != null) {
            arguments = arguments.withSourceroot(sourceroot);
        }

        // Apply that one last to allow dynamic/user override of build-tool setup
        if (parsedArguments != null) {
            arguments = arguments.withParsedArguments(parsedArguments);
        }

        return arguments;
    }

    private ScalafixVersions versionsForConfigOrBuiltinScalafixVersion() throws ScalafixException {
        // default to the built-in scalafix version
        ScalafixVersions scalafixVersions = ScalafixVersions.get();

        final String key = "version";
        File configFile = config.orElse(workingDirectory.resolve(".scalafix.conf")).toFile();
        try {
            Config typesafeConfig = ConfigFactory.parseFile(configFile);
            if (typesafeConfig.hasPath(key)) {
                String requestedVersion = typesafeConfig.getString(key);
                // TODO: some validation on requestedVersion
                if (!scalafixVersions.scalafixVersion().equals(requestedVersion)) {
                    List<URL> versionsJars = ScalafixCoursier.scalafixVersionsJars(
                            repositories,
                            requestedVersion);
                    scalafixVersions = ScalafixVersions.get(versionsJars);
                }
            }
        } catch (ConfigException e) {
        }

        return scalafixVersions;
    }

    private ScalafixArguments classLoadScalafixArguments(ClassLoader classLoader) throws ScalafixException {
        ServiceLoader<ScalafixArguments> loader = ServiceLoader.load(ScalafixArguments.class);
        Iterator<ScalafixArguments> iterator = loader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new ScalafixException("No implementation found");
        }
    }
}
