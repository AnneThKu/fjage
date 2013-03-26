/******************************************************************************

Copyright (c) 2013, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

/**
 * An interface to be implemented by all shells.
 */
public interface Shell {
  
  /**
   * Starts the shell using the specified script engine.
   * 
   * @param engine script engine to use.
   */
  public void start(ScriptEngine engine);

  /**
   * Display a message on the shell.
   * 
   * @param s message to display.
   */
  public void println(String s);

  /**
   * Returns a terminal object associated with the shell.
   * 
   * @return the terminal.
   */
  public Term getTerm();

}

