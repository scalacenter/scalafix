package scalafix.internal.interfaces;

import coursierapi.Module;
import coursierapi.*;
import coursierapi.error.CoursierError;
import scalafix.interfaces.ScalafixException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScalafixCoursier {

    private static List<URL> fetch(
            List<Repository> repositories,
            List<Dependency> dependencies,
            ResolutionParams resolutionParams
    ) throws ScalafixException {
        List<URL> jars = new ArrayList<>();
        try {
            List<File> files = Fetch.create()
                    .withRepositories(repositories.stream().toArray(Repository[]::new))
                    .withDependencies(dependencies.stream().toArray(Dependency[]::new))
                    .withResolutionParams(resolutionParams)
                    .fetch();
            for (File file : files) {
                URL url = file.toURI().toURL();
                jars.add(url);
            }
        } catch (CoursierError | MalformedURLException e) {
            throw new ScalafixException("Failed to fetch " + dependencies + "from " + repositories, e);
        }
        return jars;
    }

    public static List<URL> scalafixCliJars(
            List<Repository> repositories,
            String scalafixVersion,
            String scalaVersion
    ) throws ScalafixException {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        dependencies.add(
            Dependency.parse(
                "ch.epfl.scala:::scalafix-cli:" + scalafixVersion,
                ScalaVersion.of(scalaVersion)
            ).withConfiguration("runtime")
        );
        // Coursier does not seem to fetch runtime dependencies transitively, despite what Maven dictates
        // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope
        // so to be able to retrieve the runtime dependencies of scalafix-core, we need an explicit reference
        dependencies.add(
            Dependency.parse(
                "ch.epfl.scala::scalafix-core:" + scalafixVersion,
                ScalaVersion.of(scalaVersion)
            ).withConfiguration("runtime")
        );
        return fetch(repositories, dependencies, ResolutionParams.create());
    }

    public static List<URL> toolClasspath(
            List<Repository> repositories,
            List<String> extraDependenciesCoordinates,
            String scalaVersion
    ) throws ScalafixException {
        // External rules are built against `scalafix-core` to expose `scalafix.v1.Rule` implementations. The
        // classloader loading `scalafix-cli` already contains  `scalafix-core` to be able to discover them (which
        // is why it must be the parent of the one loading the tool classpath), so effectively, the version/instance
        // in the tool classpath will not be used. This is OK, as `scalafix-core` should not break binary
        // compatibility, as discussed in https://github.com/scalacenter/scalafix/issues/1108#issuecomment-639853899.
        Module scalafixCore = Module.parse("ch.epfl.scala::scalafix-core", ScalaVersion.of(scalaVersion));
        ResolutionParams excludeDepsInParentClassloader = ResolutionParams.create()
                .addExclusion(scalafixCore.getOrganization(), scalafixCore.getName())
                .addExclusion("org.scala-lang", "scala-library");

        List<Dependency> dependencies = extraDependenciesCoordinates
                .stream()
                .map(coordinates -> Dependency.parse(coordinates, ScalaVersion.of(scalaVersion)))
                .collect(Collectors.toList());
        return fetch(repositories, dependencies, excludeDepsInParentClassloader);
    }
}
