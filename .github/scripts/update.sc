#!/usr/bin/env -S scala-cli shebang

// Adapted from https://github.com/alexarchambault/sonatype-stats
//
// /!\ Run it from the repository root directory!

//> using scala "2.12.17"
//> using lib "com.softwaremill.sttp::core:1.5.10"
//> using lib "com.lihaoyi::upickle:2.0.0"
//> using lib "com.github.tototoshi::scala-csv:1.3.5"
//> using lib "com.twitter::algebird-core:0.13.0"
//> using lib "org.plotly-scala::plotly-render:0.5.2"
//> using files "sonatype-stats.scala", "plot.scala"

stats.SonatypeStats.collect()
plot.Plot.writePlots()
