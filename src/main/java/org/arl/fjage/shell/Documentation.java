/******************************************************************************

Copyright (c) 2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

import java.util.*;
import java.util.regex.*;

public class Documentation {

  protected List<String> doc = new ArrayList<String>();
  protected Map<String,Integer> ndx = new HashMap<String,Integer>();
  protected Pattern heading = Pattern.compile("^#+ +([^ ]+) +\\-.*$");
  protected Pattern section = Pattern.compile("^#+ .*$");

  /**
   * Add markdown documentation.
   *
   * @param s multiline string.
   */
  public void add(String s) {
    String lines[] = s.split("\\r?\\n");
    for (String line: lines) {
      Matcher m = heading.matcher(line);
      if (m.matches()) ndx.put(m.group(1), doc.size());
      doc.add(line);
    }
  }

  /**
   * Get documentation index.
   */
  public String get() {
    StringBuffer sb = new StringBuffer();
    for (Integer v: ndx.values()) {
      String s = doc.get(v);
      if (s.startsWith("# ")) {
        sb.append(s.substring(2));
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  /**
   * Get documentation by keyword.
   *
   * @param keyword keyword.
   */
  public String get(String keyword) {
    Integer pos = ndx.get(keyword);
    if (pos == null) return null;
    String s = doc.get(pos);
    int level = s.indexOf(' ');
    if (level <= 0) return null;
    int endlevel = level;
    StringBuffer sb = new StringBuffer();
    sb.append(s.replaceAll("^#+ +", ""));
    sb.append('\n');
    int skip = 0;
    for (int i = pos+1; i < doc.size(); i++) {
      s = doc.get(i);
      Matcher m = section.matcher(s);
      if (m.matches()) {
        m = heading.matcher(s);
        level = s.indexOf(' ');
        if (level <= endlevel) break;
        boolean nl = false;
        if (skip == 0 || level <= skip) {
          if (skip > 0) nl = true;
          skip = 0;
          s = s.replaceAll("^#+ +", "");
        }
        if (m.matches()) {
          skip = level;
          sb.append("- ");
          sb.append(s);
          sb.append('\n');
        } else if (nl) {
          sb.append('\n');
        }
      }
      if (skip == 0) {
        sb.append(s);
        sb.append('\n');
      }
    }
    return sb.toString().replaceAll("\\n+$", "\n");
  }

}
