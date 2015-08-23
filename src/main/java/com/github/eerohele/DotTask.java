package com.github.eerohele;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.Main;
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
  private static final String TASK_NAME = "dita-ot";

  private Boolean failOnError = true;
  private Boolean inheritAll = false;
  private Boolean verbose = false;

  private String home;
  private String transtype;
  private String workdir = System.getProperty("java.io.tmpdir");

  private ArrayList<FileSet> filesets = new ArrayList<FileSet>();
  private Vector<Parameter> params = new Vector<Parameter>();

  public void setFailOnError(Boolean f) {
      failOnError = f;
  }

  public void setInheritAll(Boolean i) {
      inheritAll = i;
  }

  public void setHome(String h) {
      home = h;
  }

  public void setTranstype(String t) {
      transtype = t;
  }

  public void setVerbose(boolean v) {
      verbose = v;
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
      addSystemProperty(setParameters(task), Constants.TRANSTYPE, transtype);

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

  private Java setInheritedParameters(Java task) {
    if (inheritAll) {
      Hashtable<?, ?> props = getProject().getUserProperties();
      Enumeration<?> e = props.keys();

      while (e.hasMoreElements()) {
        String key = e.nextElement().toString();
        String value = props.get(key).toString();
        addSystemProperty(task, key, value);
      }
    }

    return task;
  }

  private Java setParameters(Java task) {
    for (Iterator it = params.iterator(); it.hasNext();) {
        Parameter param = (Parameter) it.next();
        addSystemProperty(task, param.getName(), param.getValue());
    }

    return setInheritedParameters(task);
  }

  private void runJavaTaskOnFiles(Java task, List<File> files) {
      for (File file : files) {
          String baseName = getBaseName(file);

          String tempDir = Paths.get(workdir,
                                     Constants.TEMP,
                                     baseName).toString();

          String outputDir = Paths.get(workdir,
                                       Constants.OUT,
                                       baseName).toString();

          addSystemProperty(task, Parameters.ARGS_INPUT, file.getPath());
          addSystemProperty(task, Parameters.TEMP_DIR, tempDir);
          addSystemProperty(task, Parameters.OUTPUT_DIR, outputDir);

          if (task.executeJava() != 0 && failOnError) {
              throw new RuntimeException("There was an error processing " + file.getPath() + ", aborting build.");
          }
      }
  }

  private String getBaseName(File file) {
    String fileName = file.getName();

    int pos = fileName.lastIndexOf(Constants.PERIOD);

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
    Path classPath = new Path(p, System.getProperty("java.class.path"));
    FileSet fs = new FileSet();
    fs.setDir(new File(home));
    fs.appendIncludes(Constants.DITA_CLASSPATH_INCLUDES);
    classPath.addFileset(fs);
    return classPath;
  }

  private Java setVerbosity(Java task) {
    if (verbose) {
        Argument arg = task.createArg();
        arg.setLine(Constants.ANTFLAGS_VERBOSE);
    }

    return task;
  }

  private Java setBuildFile(Java task) {
    Argument arg = task.createArg();

    arg.setLine(Constants.ANTFLAGS_BUILDFILE +
                Constants.SPACE +
                new File(home, Main.DEFAULT_BUILD_FILENAME).getPath());

    return task;
  }

  private Java initializeJavaTask(Project p) {
    Java task = new Java();
    task.setClassname(Launcher.class.getName());
    task.setFork(true);
    task.setProject(p);
    task.setClasspath(makeClassPath(p));
    return setVerbosity(setBuildFile(task));
  }
}
