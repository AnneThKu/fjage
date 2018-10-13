/******************************************************************************

Copyright (c) 2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

import java.io.File;
import java.io.Reader;
import java.util.List;

/**
 * Implements a simple script engine that simply echoes whatever is sent to it.
 * This is useful for testing purposes.
 */
public class EchoScriptEngine implements ScriptEngine {

  protected Shell shell = null;

  protected void println(String s) {
    if (shell != null) shell.println(s);
  }

  public String getPrompt() {
    return "# ";
  }

  public boolean isComplete(String cmd) {
    if (cmd == null || cmd.length() == 0) return false;
    return true;
  }

  public void bind(Shell shell) {
    this.shell = shell;
  }

  public boolean exec(String cmd) {
    println(cmd);
    return true;
  }

  public boolean exec(File script) {
    return false;
  }

  public boolean exec(File script, List<String> args) {
    return false;
  }

  public boolean exec(Class<?> script) {
    return false;
  }

  public boolean exec(Class<?> script, List<String> args) {
    return false;
  }

  public boolean exec(Reader reader, String name) {
    return false;
  }

  public boolean exec(Reader reader, String name, List<String> args) {
    return false;
  }

  public boolean isBusy() {
    return false;
  }

  public void abort() {
    // do nothing
  }

  public void waitUntilCompletion() {
    // do nothing
  }

  public void setVariable(String name, Object value) {
    // do nothing
  }

  public Object getVariable(String name) {
    return null;
  }

  public void shutdown() {
    // do nothing
  }

}
