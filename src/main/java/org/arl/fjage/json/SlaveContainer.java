/******************************************************************************

Copyright (c) 2015, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.json;

import java.io.*;
import java.net.*;
import java.util.*;
import org.arl.fjage.*;

/**
 * Slave container attached to a master container. Agents in linked
 * master and slave containers function as if they were in the same container,
 * i.e., are able to communicate with each other through messaging, topics and
 * directory services.
 *
 * @author Mandar Chitre
 */
public class SlaveContainer extends Container implements ConnectionClosedListener {

  ////////////// Private attributes

  private static final long TIMEOUT = 1000;

  private ConnectionHandler master;
  private String hostname;
  private int port;
  private Map<String,Object> pending = Collections.synchronizedMap(new HashMap<String,Object>());

  ////////////// Constructors

  /**
   * Creates a slave container.
   * 
   * @param platform platform on which the container runs.
   * @param hostname hostname of the master container.
   * @param port port on which the master container's TCP server runs.
   */
  public SlaveContainer(Platform platform, String hostname, int port) {
    super(platform);
    this.hostname = hostname;
    this.port = port;
    connectToMaster();
  }
  
  /**
   * Creates a named slave container.
   * 
   * @param platform platform on which the container runs.
   * @param name name of the container.
   * @param hostname hostname of the master container.
   * @param port port on which the master container's TCP server runs.
   */
  public SlaveContainer(Platform platform, String name, String hostname, int port) {
    super(platform, name);
    this.hostname = hostname;
    this.port = port;
    connectToMaster();
  }

  /////////////// Container interface methods to override

  @Override
  protected boolean isDuplicate(AgentID aid) {
    if (super.isDuplicate(aid)) return true;
    if (master == null) return false;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.CONTAINS_AGENT;
    rq.agentID = aid;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    master.println(json);
    JsonMessage rsp = master.getResponse(rq.id, TIMEOUT);
    if (rsp != null && rsp.answer) return true;
    return false;
  }

  @Override
  public boolean send(Message m) {
    return send(m, true);
  }

  @Override
  public boolean send(Message m, boolean relay) {
    if (!running) return false;
    if (master == null) return false;
    AgentID aid = m.getRecipient();
    if (aid == null) return false;
    if (aid.isTopic()) {
      if (relay) {
        JsonMessage rq = new JsonMessage();
        rq.action = Action.SEND;
        rq.message = m;
        rq.relay = true;
        String json = rq.toJson();
        master.println(json);
      }
      super.send(m, false);
      return true;
    } else {
      if (super.send(m, false)) return true;
      if (!relay) return false;
      JsonMessage rq = new JsonMessage();
      rq.action = Action.SEND;
      rq.message = m;
      rq.relay = true;
      String json = rq.toJson();
      master.println(json);
      return true;
    }
  }

  @Override
  public boolean register(AgentID aid, String service) {
    if (master == null) return false;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.REGISTER;
    rq.agentID = aid;
    rq.service = service;
    String json = rq.toJson();
    master.println(json);
    return true;
  }

  @Override
  public AgentID agentForService(String service) {
    if (master == null) return null;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.AGENT_FOR_SERVICE;
    rq.service = service;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    master.println(json);
    JsonMessage rsp = master.getResponse(rq.id, TIMEOUT);
    if (rsp == null) return null;
    return rsp.agentID;
  }

  @Override
  public AgentID[] agentsForService(String service) {
    if (master == null) return null;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.AGENTS_FOR_SERVICE;
    rq.service = service;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    master.println(json);
    JsonMessage rsp = master.getResponse(rq.id, TIMEOUT);
    if (rsp == null) return null;
    return rsp.agentIDs;
  }

  @Override
  public boolean deregister(AgentID aid, String service) {
    if (master == null) return false;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.DEREGISTER;
    rq.agentID = aid;
    rq.service = service;
    String json = rq.toJson();
    master.println(json);
    return true;
  }

  @Override
  public void deregister(AgentID aid) {
    if (master == null) return;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.DEREGISTER;
    rq.agentID = aid;
    String json = rq.toJson();
    master.println(json);
  }

  @Override
  public String getState() {
    if (!running) return "Not running";
    if (master == null) return "Running, connecting to "+hostname+":"+port+"...";
    return "Running, connected to "+hostname+":"+port;
  }

  @Override
  public String toString() {
    String s = getClass().getName()+"@"+name;
    s += "/slave/"+platform;
    return s;
  }

  @Override
  public void connectionClosed(ConnectionHandler handler) {
    log.info("Connection to "+handler.getName()+" lost, retrying");
    master = null;
    connectToMaster();
  }

  /////////////// Private stuff

  private void connectToMaster() {
    while (true) {
      try {
        log.info("Connecting to "+hostname+":"+port);
        Socket conn = new Socket(hostname, port);
        master = new ConnectionHandler(conn, this);
        master.start();
        return;
      } catch (IOException ex) {
        log.warning("Connection failed: "+ex.toString());
      }
    }
  }

}
