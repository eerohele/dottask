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
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;

import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;

public class DotTask extends Task {
  private static final String TASK_NAME = "dita-ot";

  private Boolean failOnError = true;
  private Boolean inheritAll = false;
  private Boolean verbose = false;

  private String home;
  private String transtype;
  private String outputdir = System.getProperty("java.io.tmpdir");

  protected Mapper mapperElement = null;

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

  public void setOutputDir(String w) {
      outputdir = w;
  }

  public void addFileset(FileSet f) {
      filesets.add(f);
  }

  private FileNameMapper getMapper(File file) {
      FileNameMapper mapper = null;

      if (mapperElement != null) {
          mapper = mapperElement.getImplementation();
      } else {
          mapper = new GlobPatternMapper();
          mapper.setFrom(file.getParent() + "/*");
          mapper.setTo(outputdir + "/*");
      }

      return mapper;
  }

  public Mapper createMapper() throws BuildException {
      if (mapperElement != null) {
          throw new BuildException("Cannot define more than one mapper",
                                   getLocation());
      }

      mapperElement = new Mapper(getProject());
      return mapperElement;
  }

  public void add(final FileNameMapper fileNameMapper) {
      createMapper().add(fileNameMapper);
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
          String fileName = file.getPath();
          String mappedFileName = getMapper(file).mapFileName(fileName)[0];
          String mappedOutputDir = stripExtension(mappedFileName);

          addSystemProperty(task, Parameters.ARGS_INPUT, file.getPath());
          addSystemProperty(task, Parameters.OUTPUT_DIR, mappedOutputDir);

          if (task.executeJava() != 0 && failOnError) {
              throw new RuntimeException(
                  String.format(
                    "There was an error processing %s, aborting build.",
                    fileName));
          }
      }
  }

  private String stripExtension(String fileName) {
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
