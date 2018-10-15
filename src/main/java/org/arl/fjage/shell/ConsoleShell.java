/******************************************************************************

Copyright (c) 2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

import java.io.*;
import java.util.logging.*;
import org.jline.reader.*;
import org.jline.terminal.*;
import org.jline.utils.*;
import org.arl.fjage.connectors.Connector;

/**
 * Shell input/output driver for console devices.
 */
public class ConsoleShell implements Shell {

  private Terminal term = null;
  private LineReader console = null;
  private ScriptEngine scriptEngine = null;
  private AttributedStyle outputStyle = null;
  private AttributedStyle notifyStyle = null;
  private AttributedStyle errorStyle = null;
  private Logger log = Logger.getLogger(getClass().getName());

  /**
   * Create a console shell attached to the system terminal.
   */
  public ConsoleShell() {
    try {
      term = TerminalBuilder.terminal();
      setupStyles();
    } catch (IOException ex) {
      log.warning("Unable to open terminal: "+ex.toString());
    }
  }

  /**
   * Create a console shell attached to a specified input and output stream.
   *
   * @param in input stream.
   * @param out output stream.
   */
  public ConsoleShell(InputStream in, OutputStream out) {
    try {
      term = TerminalBuilder.builder().streams(in, out).build();
      setupStyles();
    } catch (IOException ex) {
      log.warning("Unable to open terminal: "+ex.toString());
    }
  }

  /**
   * Create a console shell attached to a specified input and output stream.
   *
   * @param in input stream.
   * @param out output stream.
   * @param dump true to force a dumb terminal, false otherwise.
   */
  public ConsoleShell(InputStream in, OutputStream out, boolean dumb) {
    try {
      if (dumb) term = new org.jline.terminal.impl.DumbTerminal(in, out);
      else {
        term = TerminalBuilder.builder().streams(in, out).dumb(dumb).build();
        setupStyles();
      }
    } catch (IOException ex) {
      log.warning("Unable to open terminal: "+ex.toString());
    }
  }

  /**
   * Create a console shell attached to a specified connector.
   *
   * @param connector input/output streams.
   */
  public ConsoleShell(Connector connector) {
    try {
      InputStream in = connector.getInputStream();
      OutputStream out = connector.getOutputStream();
      term = TerminalBuilder.builder().streams(in, out).build();
      setupStyles();
    } catch (IOException ex) {
      log.warning("Unable to open terminal: "+ex.toString());
    }
  }

  /**
   * Create a console shell attached to a specified connector.
   *
   * @param connector input/output streams.
   * @param dumb true to force a dumb terminal, false otherwise.
   */
  public ConsoleShell(Connector connector, boolean dumb) {
    try {
      InputStream in = connector.getInputStream();
      OutputStream out = connector.getOutputStream();
      if (dumb) term = new org.jline.terminal.impl.DumbTerminal(in, out);
      else {
        term = TerminalBuilder.builder().streams(in, out).dumb(dumb).build();
        setupStyles();
      }
    } catch (IOException ex) {
      log.warning("Unable to open terminal: "+ex.toString());
    }
  }

  private void setupStyles() {
    AttributedStyle style = new AttributedStyle();
    outputStyle = style.foreground(AttributedStyle.GREEN);
    notifyStyle = style.foreground(AttributedStyle.BLUE);
    errorStyle = style.foreground(AttributedStyle.RED);
  }

  @Override
  public void init(ScriptEngine engine) {
    if (term == null) return;
    scriptEngine = engine;
    if (scriptEngine == null) console = LineReaderBuilder.builder().terminal(term).build();
    else {
      Parser parser = new Parser() {
        @Override
        public ParsedLine parse(String s, int cursor) {
          if (!scriptEngine.isComplete(s)) throw new EOFError(0, cursor, "Incomplete sentence");
          return null;
        }
        @Override
        public ParsedLine parse(String s, int cursor, Parser.ParseContext context) {
          return parse(s, cursor);
        }
      };
      console = LineReaderBuilder.builder().parser(parser).terminal(term).build();
    }
  }

  @Override
  public void println(Object obj) {
    if (obj == null || console == null) return;
    console.printAbove(new AttributedString(obj.toString(), outputStyle));
  }

  @Override
  public void notify(Object obj) {
    if (obj == null || console == null) return;
    console.printAbove(new AttributedString(obj.toString(), notifyStyle));
  }

  @Override
  public void error(Object obj) {
    if (obj == null || console == null) return;
    console.printAbove(new AttributedString(obj.toString(), errorStyle));
  }

  @Override
  public String readLine(String prompt1, String prompt2, String line) {
    if (console == null) return null;
    try {
      console.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, prompt2);
      return console.readLine(prompt1, null, (Character)null, line);
    } catch (UserInterruptException ex) {
      return ABORT;
    } catch (Throwable ex) {
      Thread.interrupted();
      return null;
    }
  }

  @Override
  public void shutdown() {
    console = null;
  }

}
