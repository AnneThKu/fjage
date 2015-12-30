/******************************************************************************

Copyright (c) 2015, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.json;

import org.arl.fjage.*;
import com.google.gson.*;

/**
 * Class representing a JSON request/response message.
 */
class JsonMessage {

  String id;
  Action action;
  Action inResponseTo;
  AgentID agentID;
  String service;
  Object response;
  Message message;
  Boolean relay;

  private static Gson gson = new GsonBuilder()
                                  .registerTypeHierarchyAdapter(Message.class, new MessageAdapter())
                                  .registerTypeHierarchyAdapter(AgentID.class, new AgentIDAdapter())
                                  .create();

  static JsonMessage fromJson(String s) {
    return gson.fromJson(s, JsonMessage.class);
  }

  String toJson() {
    return gson.toJson(this);
  }

}
