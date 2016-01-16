/******************************************************************************

Copyright (c) 2016, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import groovy.lang.*;
import groovy.transform.ThreadInterrupt;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.*;
import org.arl.fjage.Message;

/**
 * Groovy scripting engine.
 */
public class GroovyScriptEngine extends Thread implements ScriptEngine {
  
  ////// private attributes

  private GroovyShell groovy;
  private Binding binding;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private Future<?> last = null;
  private Logger log = Logger.getLogger(getClass().getName());
  
  ////// constructor

  public GroovyScriptEngine() {
    binding = new ConcurrentBinding();
    init();
  }

  public GroovyScriptEngine(boolean protect) {
    binding = protect ? new ProtectedBinding() : new Binding();
    init();
  }
  
  private void init() {
    CompilerConfiguration compiler = new CompilerConfiguration();
    compiler.setScriptBaseClass(BaseGroovyScript.class.getName());
    ImportCustomizer imports = new ImportCustomizer();
    binding.setVariable("imports", imports);
    binding.setVariable("rsp", null);
    binding.setVariable("ntf", null);
    compiler.addCompilationCustomizers(imports);
    compiler.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
    GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader());
    groovy = new GroovyShell(gcl, binding, compiler);
    binding.setVariable("groovy", groovy);
    groovy.evaluate("_init_()");
  }
  
  ////// script engine methods
  
  @Override
  public boolean exec(final String cmd1, final Shell out) {
    if (isBusy()) return false;
    last = executor.submit(new Callable<Object>() {
      @Override
      public Object call() {
        String cmd = cmd1.trim();
        if (cmd.startsWith("help ")) cmd = "help '"+cmd.substring(5)+"'";
        else if (cmd.startsWith("import ")) cmd = "shellImport '"+cmd.substring(7)+"'";
        else if (cmd.startsWith("<")) {
          if (cmd.contains(" ")) cmd = "run('"+cmd.substring(1).replaceFirst(" ","',")+");";
          else cmd = "run('"+cmd.substring(1)+"');";
        }
        log.info("EVAL: "+cmd);
        Object rv = null;
        try {
          if (binding.hasVariable(cmd)) {
            rv = binding.getVariable(cmd);
            if (rv instanceof Closure) {
              Closure<?> cl = (Closure<?>)rv;
              try {
                rv = cl.call();
              } catch (MissingMethodException ex) {
                // do nothing, as it's probably a closure that needs at least one argument
              }
            }
          } else {
            binding.setVariable("out", out);
            binding.setVariable("script", null);
            binding.setVariable("args", null);
            rv = groovy.evaluate(cmd);
            if (rv instanceof Class) {
              // if a script is also on the classpath, it can return the class loaded by a wrong classloader
              // in that case, we try to find the original file and execute it
              String name = ((Class)rv).getName();
              if (name.equals(cmd)) {
                String folder = binding.hasVariable("scripts") ? (String)binding.getVariable("scripts") : null;
                File f = new File(folder, name+".groovy");
                if (f.exists() && f.isFile()) {
                  execFromFile(f, new ArrayList<String>(), out);
                  rv = null;
                }
              }
            }
          }
        } catch (Exception ex) {
          error(out, ex);
        } finally {
          binding.setVariable("out", null);
          binding.setVariable("ans", rv);
        }
        if (rv != null && !cmd.endsWith(";"))
          println(out, (rv instanceof Message) ? rv : groovy.evaluate("ans.toString()"));
        return rv;
      }
    });
    return true;
  }

  @Override
  public boolean exec(final File script, final Shell out) {
    return exec(script, new ArrayList<String>(), out);
  }

  @Override
  public boolean exec(final File script, final List<String> args, final Shell out) {
    if (isBusy()) return false;
    execFromFile(script, args, out);
    return true;
  }

  @Override
  public boolean exec(final Class<?> script, final Shell out) {
    return exec(script, new ArrayList<String>(), out);
  }

  @Override
  public boolean exec(final Class<?> script, final List<String> args, final Shell out) {
    if (isBusy()) return false;
    last = executor.submit(new Callable<Object>() {
      @Override
      public Object call() {
        log.info("RUN: "+script.getName());
        try {
          binding.setVariable("out", out);
          binding.setVariable("script", script.getName());
          binding.setVariable("args", args);
          Script gs = (Script)script.newInstance();
          gs.setBinding(binding);
          gs.run();
        } catch (Exception ex) {
          error(out, ex);
        } finally {
          binding.setVariable("out", null);
          binding.setVariable("script", null);
        }
        return null;
      }
    });
    return true;
  }

  @Override
  public boolean exec(final Reader reader, final String name, final Shell out) {
    return exec(reader, name, new ArrayList<String>(), out);
  }

  @Override
  public boolean exec(final Reader reader, final String name, final List<String> args, final Shell out) {
    if (isBusy()) return false;
    last = executor.submit(new Callable<Object>() {
      @Override
      public Object call() {
        log.info("RUN: "+name);
        try {
          binding.setVariable("out", out);
          binding.setVariable("script", name);
          binding.setVariable("args", null);
          groovy.getClassLoader().clearCache();
          groovy.run(reader, name, args);
        } catch (Exception ex) {
          error(out, ex);
        } finally {
          binding.setVariable("out", null);
          binding.setVariable("script", null);
        }
        return null;
      }
    });
    return true;
  }

  @Override
  public boolean isBusy() {
    if (last == null) return false;
    if (last.isCancelled() || last.isDone()) return false;
    return true;
  }

  @Override
  public void abort() {
    if (isBusy()) last.cancel(true);
  }

  @Override
  public void waitUntilCompletion() {
    try {
      if (last != null) last.get();
      last = null;
    } catch (Exception ex) {
      // do nothing
    }
  }

  @Override
  public Object getResult() {
    if (isBusy()) return null;
    return getVariable("ans");
  }

  @Override
  public void setVariable(String name, Object value) {
    binding.setVariable(name, value);
  }
  
  @Override
  public Object getVariable(String name) {
    return binding.getVariable(name);
  }

  @Override
  public void shutdown() {
    executor.shutdown();
    abort();
  }

  ////// private methods

  private void execFromFile(final File script, final List<String> args, final Shell out) {
    last = executor.submit(new Callable<Object>() {
      @Override
      public Object call() {
        log.info("RUN: "+script.getAbsolutePath());
        try {
          binding.setVariable("out", out);
          binding.setVariable("script", script.getAbsoluteFile());
          binding.setVariable("args", null);
          groovy.getClassLoader().clearCache();
          groovy.run(script, args);
        } catch (Throwable ex) {
          error(out, ex);
        } finally {
          binding.setVariable("out", null);
          binding.setVariable("script", null);
        }
        return null;
      }
    });
  }
  
  private void println(Shell out, Object s) {
    String str = s.toString();
    log.info("RESULT: "+str);
    // Mostly log the String version, but for messages, log it so that GUI can display details
    // Be careful not to ever log AgentIDs otherwise the toString() extensions can get called too often by GUI!
    if (out != null) out.println((s instanceof Message)?s:str, OutputType.OUTPUT);
  }

  private void error(Shell out, Throwable ex) {
    log.log(Level.WARNING, "Exception in Groovy script", ex);
    if (out != null) out.println(ex, OutputType.ERROR);
  }

}
