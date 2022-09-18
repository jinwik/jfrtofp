# jfrtofp
[![Maven Package](https://jitpack.io/v/parttimenerd/jfrtofp.svg)](https://jitpack.io/#parttimenerd/jfrtofp)

JFR to [Firefox Profiler](https://profiler.firefox.com) converter for JDK 11+.

It works best with this custom [Firefox Profiler fork](https://github.com/parttimenerd/firefox-profiler/tree/merged)
which includes many of our own PRs which are not yet upstream (and might be less stable).

## Basic Usage

We recommend using the [jfrtofp-server](https://github.com/parttimenerd/jfrtofp-server) which includes a
custom Firefox Profiler distribution with the converter and a webserver which serves both.

Download the latest `jfrtofp-all.jar` release and simply pass the JFR file as its first argument:

```sh
  java -jar jfrtofp-all.jar samples/small_profile.jfr
```

This will produce a `samples/small_profile.json.gz` file, you can customize the output file
by passing the `--output <file>` option.

There is the possibility to produce [Speedscope](https://www.speedscope.app/) files as well, by passing the "--mode speedscope" option:

```sh
  java -jar jfrtofp-all.jar samples/small_profile.jfr --mode speedscope
```

This will produce a `samples/small_profile.json.gz` file. 
But this is considered experimental and not the main focus of this project.

## Run from Source

```sh
  git clone https://github.com/parttimenerd/jfrtofp.git
  cd jfrtofp
  ./gradlew run --args="samples/small_profile.jfr"
```

## Usage as a Library
```groovy
dependencies {
    implementation 'com.github.parttimenerd:jfrtofp-server:main-SNAPSHOT'
}

repositories {
    maven {
        url = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

## License
MIT

## Ideas from Andreas
- [ ] use dates ("12:00:34.2") instead of just seconds ("34.2")
- [ ] skip methods with less than x % of time in the method tables and flamegraphs (e.g. focus on the main culprits in a method),
  map these to "other"

## Ideas from Oliver
- [ ] which JVM version, params
- [ ] how long does the GC take (wall clock time), mark long GC times with a different color
- [ ] maybe import GC History / GC History viewer

## Other Ideas
- [ ] use the whole descriptor in the method name and use the resource only for actual files (?)
- [ ] package converter as a separate JAR and repository, then package the server with the converter
- [ ] use relative times instead of absolute times
- [ ] fork the FirefoxProfiler (friendly)
- [ ] combine several markers into one (collect all Environment variables)