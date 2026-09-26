# Spinning donut

A tiny Java terminal renderer for a rotating 3D torus. It uses Komp-Geom's JVM package for 3D points, normals, vector operations, and composed affine rotations.

From this directory, run:

```bash
./gradlew run
```

The animation runs until interrupted with `Ctrl-C`.

To create and run the standard Gradle application distribution:

```bash
./gradlew installDist
./build/install/spinning-donut/bin/spinning-donut
```

This creates a launcher script, the application JAR, and a `lib/` directory containing Komp-Geom and the other runtime dependencies. The JAR in `build/libs/` is a regular library JAR and is not intended to be started with `java -jar`; use the generated launcher or the classpath command above.
