# DITA-OT Ant Task

A simple [Ant][ant] task for publishing things with
[DITA Open Toolkit][dot].

## Features

- No need to set classpath.
- You can use [Ant filesets][ant-fileset] to publish many files at once.
- Platform-independent.

## Installation

**NOTE**: Java 7+ and [Ant][ant] required.

```bash
$ git clone https://github.com/eerohele/dottask.git
$ cd dottask
$ ant
$ cp target/jar/dotTask-0.1.0.jar /your/dita/ant/project/path/lib
```

## Sample Ant buildfile

See `samples/build.xml`.

To run it:

```bash
$ cd samples
$ ant -Ddita.home=/path/to/your/dita-ot/installation
```

## Optional attributes

You can set these optional attributes on the `<dita-ot>` element.

- `workdir`

    The directory where to store temporary and output files. The default is `${java.io.tmpdir}`.

- `inheritAll`

    Inherit (almost) all properties from the parent project. The
    [`args.input`][args-input], [`output.dir`][output-dir], and
    [`dita.temp.dir`][dita-temp-dir] parameters aren't inherited because that
    wouldn't make sense. The default is `false`.

- `verbose`

    Run Ant in verbose mode. The default is `false`.

## Caveats

- Only very cursorily tested on Java 7+, DITA-OT 1.8.x, DITA-OT 2.x, and default DITA-OT plugins at the moment. Help appreciated.
- I resemble a Java programmer about as closely as I resemble George Clooney.

## TODO

- Publish to Maven Central (if there's interest)

[ant-fileset]: https://ant.apache.org/manual/Types/fileset.html
[ant]: https://ant.apache.org/
[args-input]: http://www.dita-ot.org/dev/parameters/ant-parameters-base-transformation.html#ant-parameters-base-transformation__args.input
[dita-temp-dir]: http://www.dita-ot.org/dev/parameters/ant-parameters-base-transformation.html#ant-parameters-base-transformation__dita.temp.dir
[dot]: http://www.dita-ot.org
[output-dir]: http://www.dita-ot.org/dev/parameters/ant-parameters-base-transformation.html#ant-parameters-base-transformation__output.dir
