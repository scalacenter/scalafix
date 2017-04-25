addSbtPlugin("com.eed3si9n"    % "sbt-doge"            % "0.1.5")
addSbtPlugin("com.eed3si9n"    % "sbt-buildinfo"       % "0.6.1")
addSbtPlugin("com.lihaoyi"     % "scalatex-sbt-plugin" % "0.3.7")
addSbtPlugin("io.get-coursier" % "sbt-coursier"        % "1.0.0-M15")
addSbtPlugin("org.wartremover" % "sbt-wartremover"     % "1.2.1")
addSbtPlugin("com.jsuereth"    % "sbt-pgp"             % "1.0.0")
addSbtPlugin("org.xerial.sbt"  % "sbt-sonatype"        % "1.1")
addSbtPlugin("me.lessis"       % "bintray-sbt"         % "0.3.0")
addSbtPlugin("com.dwijnand"    % "sbt-dynver"          % "1.2.0")

// TODO(olafur) re-enable scoverage.
// scoverage is disabled because it messes up with scalafix-nsc
//addSbtPlugin("org.scoverage"    % "sbt-scoverage"       % "1.0.1")
libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
