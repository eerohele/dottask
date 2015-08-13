package com.github.eerohele;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.launch.Launcher;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;

import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

public class DotTask extends Task {
  String TASK_NAME = "dita-ot";

  String OUT = "out";
  String TEMP = "temp";

  String PARAM_ARGS_INPUT = "args.input";
  String PARAM_OUTPUT_DIR = "output.dir";
  String PARAM_TEMP_DIR = "dita.temp.dir";

  String BUILD_XML = "build.xml";
  String TRANSTYPE = "transtype";

  String PERIOD = ".";
  String SPACE = " ";

  String PROPS_JAVA_CLASS_PATH = "java.class.path";
  String FLAGS_BUILDFILE = "-buildfile";

  String[] DITA_CLASSPATH_INCLUDES = { "lib/*.jar", "resources" };

  Boolean fork = false;

  String home;
  String transtype;
  String workdir = System.getProperty("java.io.tmpdir");

  private ArrayList<FileSet> filesets = new ArrayList<FileSet>();
  private Vector<Parameter> params = new Vector<Parameter>();

  public void setFork(Boolean f) {
      fork = f;
  }

  public void setHome(String h) {
      home = h;
  }

  public void setTranstype(String t) {
      transtype = t;
  }

  public void setWorkdir(String w) {
      workdir = w;
  }

  public void addFileset(FileSet f) {
      filesets.add(f);
  }

  protected void validate() {
      if (filesets.size() < 1) {
          throw new BuildException("No fileset given.");
      }

      if (home.length() < 1) {
          throw new BuildException("DITA-OT home directory not set.");
      }
  }

  public void execute() {
      validate();

      getProject().setName(TASK_NAME);

      Java task = initializeJavaTask(getProject());
      task.setTaskName(TASK_NAME);
      addSystemProperty(setParameters(task), TRANSTYPE, transtype);

      ArrayList<File> files = new ArrayList<File>();

      for (FileSet fs : filesets) {
          DirectoryScanner ds = fs.getDirectoryScanner(getProject());

          for (String path : ds.getIncludedFiles()) {
            files.add(new File(fs.getDir(), path));
          }

          runJavaTaskOnFiles(task, files);
      }

      task.clearArgs();
  }

  public Parameter createParameter() {
      Parameter param = new Parameter();
      params.add(param);
      return param;
  }

  public class Parameter {
      public Parameter() {}

      String name;
      String value;

      public void setName(String name) { this.name = name; }
      public String getName() { return name; }
      public void setValue(String value) { this.value = value; }
      public String getValue() { return value; }
  }

  private Java setParameters(Java task) {
    for (Iterator it = params.iterator(); it.hasNext();) {
        Parameter param = (Parameter) it.next();
        addSystemProperty(task, param.getName(), param.getValue());
    }

    return task;
  }

  private void runJavaTaskOnFiles(Java task, List<File> files) {
      for (File file : files) {
          String baseName = getBaseName(file);

          String tempDir = Paths.get(workdir, TASK_NAME, TEMP, baseName)
                                .toString();

          String outputDir = Paths.get(workdir, TASK_NAME, OUT, baseName)
                                  .toString();

          addSystemProperty(task, PARAM_ARGS_INPUT, file.getPath());
          addSystemProperty(task, PARAM_TEMP_DIR, tempDir);
          addSystemProperty(task, PARAM_OUTPUT_DIR, outputDir);

          task.executeJava();
      }
  }

  private String getBaseName(File file) {
    String fileName = file.getName();

    int pos = fileName.lastIndexOf(PERIOD);

    if (pos > 0) {
        return fileName.substring(0, pos);
    } else {
        return fileName;
    }
  }

  private Java addSystemProperty(Java task, String key, String value) {
    Variable var = new Variable();
    var.setKey(key);
    var.setValue(value);
    task.addSysproperty(var);
    return task;
  }

  private Path makeClassPath(Project p) {
    Path classPath = new Path(p, System.getProperty(PROPS_JAVA_CLASS_PATH));
    FileSet fs = new FileSet();
    fs.setDir(new File(home));
    fs.appendIncludes(DITA_CLASSPATH_INCLUDES);
    classPath.addFileset(fs);
    return classPath;
  }

  private Java setBuildFile(Java task) {
    Argument arg = task.createArg();
    arg.setLine(FLAGS_BUILDFILE + SPACE + new File(home, BUILD_XML).getPath());
    return task;
  }

  private Java initializeJavaTask(Project p) {
    Java task = new Java();
    task.setClassname(Launcher.class.getName());
    task.setFork(fork);
    task.setProject(p);
    task.setClasspath(makeClassPath(p));
    return setBuildFile(task);
  }
}