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

    private static FetchResult fetch(
            List<Repository> repositories,
            List<Dependency> dependencies,
            ResolutionParams resolutionParams
    ) throws ScalafixException {
        try {
            return Fetch.create()
                    .withRepositories(repositories.stream().toArray(Repository[]::new))
                    .withDependencies(dependencies.stream().toArray(Dependency[]::new))
                    .withResolutionParams(resolutionParams)
                    .fetchResult();
        } catch (CoursierError e) {
            throw new ScalafixException("Failed to fetch " + dependencies + "from " + repositories, e);
        }
    }

    private static List<URL> toURLs(FetchResult result) throws ScalafixException {
        List<URL> urls = new ArrayList<>();
        for (File file : result.getFiles()) {
            try {
                URL url = file.toURI().toURL();
                urls.add(url);
            } catch (MalformedURLException e) {
                throw new ScalafixException("Failed to load dependency " + file, e);
            }
        }
        return urls;
    }

    public static List<URL> scalafixCliJars(
            List<Repository> repositories,
            String scalafixVersion,
            String scalaVersion
    ) throws ScalafixException {
        Dependency scalafixCli = Dependency
				.parse("ch.epfl.scala:::scalafix-cli:" + scalafixVersion, ScalaVersion.of(scalaVersion))
				.withConfiguration("runtime");
        return toURLs(fetch(repositories, Collections.singletonList(scalafixCli), ResolutionParams.create()));
    }

    public static FetchResult toolClasspath(
            List<Repository> repositories,
            List<String> extraDependenciesCoordinates,
            String scalaVersion
    ) throws ScalafixException {
        ResolutionParams excludeDepsInParentClassloader = ResolutionParams.create()
                .addExclusion("org.scala-lang", "scala-library");

        List<Dependency> dependencies = extraDependenciesCoordinates
                .stream()
                .map(coordinates -> Dependency.parse(coordinates, ScalaVersion.of(scalaVersion)))
                .collect(Collectors.toList());
        return fetch(repositories, dependencies, excludeDepsInParentClassloader);
    }
}
