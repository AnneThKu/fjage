/******************************************************************************

Copyright (c) 2013, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

/**
 * Terminal ANSI sequences for pretty display.
 */
public class Term {

  ///////// constants

  private static final String ESC = "\033[";
  private static final String RESET = ESC+"0m";
  private static final String BLACK = ESC+"30m";
  private static final String RED = ESC+"31m";
  private static final String GREEN = ESC+"32m";
  private static final String YELLOW = ESC+"33m";
  private static final String BLUE = ESC+"34m";
  private static final String MAGENTA = ESC+"35m";
  private static final String CYAN = ESC+"36m";
  private static final String WHITE = ESC+"37m";
  private static final String HOME = ESC+"0G";
  private static final String CLREOL = ESC+"0K";

  ///////// private state

  private static boolean defaultState = true;
  private boolean enabled;

  ///////// public interface

  public Term() {
    enabled = defaultState;
  }

  public Term(boolean enabled) {
    this.enabled = enabled;
  }

  public static void setDefaultState(boolean enabled) {
    defaultState = enabled;
  }

  public void enable() {
    enabled = true;
  }

  public void disable() {
    enabled = false;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String error(String msg) {
    if (!enabled) return msg;
    return HOME+CLREOL+RED+msg+RESET;
  }

  public String response(String msg) {
    if (!enabled) return msg;
    return HOME+CLREOL+GREEN+msg+RESET;
  }

  public String notification(String msg) {
    if (!enabled) return msg;
    return HOME+CLREOL+BLUE+msg+RESET;
  }

  public String prompt(String msg) {
    if (!enabled) return msg;
    return HOME+CLREOL+CYAN+msg+RESET;
  }

}

