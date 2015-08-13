# DITA-OT Ant Task

A simple [Ant](https://ant.apache.org/) task for publishing things with
[DITA Open Toolkit](http://www.dita-ot.org).

## Features

- No need to set classpath.
- You can use [Ant filesets](https://ant.apache.org/manual/Types/fileset.html) to publish many files at once.
- Platform-independent.

## Installation

**NOTE**: Java 7+ required.

```bash
git clone https://github.com/eerohele/dottask.git
cd dottask
ant
cp target/jar/dotTask-0.1.0.jar /your/dita/ant/project/path/lib
```

## Sample Ant buildfile

```xml
<project name="docs" default="build" basedir=".">
  <!-- Let Ant know where your DITA-OT installation is located. -->
  <property name="dita.home" location="/path/to/dita-ot"/>

  <!-- Let Ant know where your DITA XML files are. -->
  <property name="input.dir" location="${basedir}/input"

  <!-- Let Ant know where to find the DITA-OT Ant Task. -->
  <taskdef name="dita-ot"
           classname="com.github.eerohele.DotTask"
           classpath="${basedir}/lib/dotTask-0.1.0.jar"/>

  <target name="build">
    <!--
    Publish every file with a .ditamap extension in the input directory into
    HTML5.
    -->
    <dita-ot home="${dita.home}" transtype="html5">
      <fileset dir="${input.dir}" includes="*.ditamap"/>

      <!--
      Set DITA-OT parameters.

      For information on DITA-OT parameters, see:

      http://www.dita-ot.org/dev/parameters/ant-parameters_intro.html
      -->
      <parameter name="args.css" value="${basedir}/resources/css/default.css"/>
      <parameter name="args.copycss" value="yes"/>
      <parameter name="processing-mode" value="strict"/>
    </dita-ot>
  </target>
</project>
```

## Optional attributes

You can set these optional attributes on the `<dita-ot>` element.

- `workdir`

    The directory where to store temporary and output files. The default is `${java.io.tmpdir}/dita-ot`.

- `fork`

    Create a new Java Virtual Machine for running DITA-OT. The default is `false`.

## Caveats

- Only tested on Java 7+, DITA-OT 2.1, and default DITA-OT plugins at the moment.
- I resemble a Java programmer about as closely as I resemble George Clooney.

## TODO

- Support inheriting properties from the parent project
- Support Java property files
- Publish to Maven Central (if there's interest)


