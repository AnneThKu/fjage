/******************************************************************************

Copyright (c) 2015-2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.remote;

import java.io.*;
import java.net.*;
import java.util.*;
import org.arl.fjage.*;
import org.arl.fjage.connectors.*;

/**
 * Master container supporting multiple remote slave containers. Agents in linked
 * master and slave containers function as if they were in the same container,
 * i.e., are able to communicate with each other through messaging, topics and
 * directory services.
 *
 * @author Mandar Chitre
 */
public class MasterContainer extends RemoteContainer implements ConnectionListener {

  ////////////// Private attributes

  private static final long TIMEOUT = 1000;

  private TcpServer listener = null;
  private List<ConnectionHandler> slaves = new ArrayList<ConnectionHandler>();
  private boolean needsCleanup = false;

  ////////////// Constructors

  /**
   * Creates a master container, runs its TCP server on an automatically selected port.
   *
   * @param platform platform on which the container runs.
   */
  public MasterContainer(Platform platform) throws IOException {
    super(platform);
    openTcpServer(0);
  }

  /**
   * Creates a master container, runs its TCP server on a specified port.
   *
   * @param platform platform on which the container runs.
   * @param port port on which the container's TCP server runs.
   */
  public MasterContainer(Platform platform, int port) throws IOException {
    super(platform);
    openTcpServer(port);
  }

  /**
   * Creates a named master container, runs its TCP server on an automatically selected port.
   *
   * @param platform platform on which the container runs.
   * @param name name of the container.
   */
  public MasterContainer(Platform platform, String name) throws IOException {
    super(platform, name);
    openTcpServer(0);
  }

  /**
   * Creates a named master container, runs its TCP server on a specified port.
   *
   * @param platform platform on which the container runs.
   * @param name of the container.
   * @param port port on which the container's TCP server runs.
   */
  public MasterContainer(Platform platform, String name, int port) throws IOException {
    super(platform, name);
    openTcpServer(port);
  }

  /**
   * Creates a master container running a RS232 server.
   *
   * @param platform platform on which the container runs.
   * @param devname device name of the RS232 port.
   * @param baud baud rate for the RS232 port.
   * @param settings RS232 settings (null for defaults, or "N81" for no parity, 8 bits, 1 stop bit).
   */
  public MasterContainer(Platform platform, String devname, int baud, String settings) {
    super(platform);
    openSerialPort(devname, baud, settings);
  }

  /**
   * Creates a named master container running a RS232 server.
   *
   * @param platform platform on which the container runs.
   * @param name of the container.
   * @param devname device name of the RS232 port.
   * @param baud baud rate for the RS232 port.
   * @param settings RS232 settings (null for defaults, or "N81" for no parity, 8 bits, 1 stop bit).
   */
  public MasterContainer(Platform platform, String name, String devname, int baud, String settings) {
    super(platform);
    openSerialPort(devname, baud, settings);
  }

  /**
   * Creates a master container running a RS232 server and a TCP server.
   *
   * @param platform platform on which the container runs.
   * @param port port on which the container's TCP server runs (0 to select port automatically).
   * @param devname device name of the RS232 port.
   * @param baud baud rate for the RS232 port.
   * @param settings RS232 settings (null for defaults, or "N81" for no parity, 8 bits, 1 stop bit).
   */
  public MasterContainer(Platform platform, int port, String devname, int baud, String settings) throws IOException {
    super(platform);
    openTcpServer(port);
    openSerialPort(devname, baud, settings);
  }

  /**
   * Creates a named master container running a RS232 server and a TCP server.
   *
   * @param platform platform on which the container runs.
   * @param name of the container.
   * @param port port on which the container's TCP server runs (0 to select port automatically).
   * @param devname device name of the RS232 port.
   * @param baud baud rate for the RS232 port.
   * @param settings RS232 settings (null for defaults, or "N81" for no parity, 8 bits, 1 stop bit).
   */
  public MasterContainer(Platform platform, String name, int port, String devname, int baud, String settings) throws IOException {
    super(platform);
    openTcpServer(port);
    openSerialPort(devname, baud, settings);
  }

  /**
   * Gets the TCP port on which the master container listens for connections.
   *
   * @return port on which the container's TCP server runs, -1 if none.
   */
  public int getPort() {
    if (listener == null) return -1;
    return listener.getPort();
  }

  /////////////// Container interface methods to override

  @Override
  protected boolean isDuplicate(AgentID aid) {
    if (super.isDuplicate(aid)) return true;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.CONTAINS_AGENT;
    rq.agentID = aid;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    if (needsCleanup) cleanupSlaves();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves) {
        JsonMessage rsp = slave.printlnAndGetResponse(json, rq.id, TIMEOUT);
        if (rsp != null && rsp.answer) return true;
      }
    }
    return false;
  }

  @Override
  public boolean send(Message m) {
    return send(m, true);
  }

  @Override
  public boolean send(Message m, boolean relay) {
    if (!running) return false;
    AgentID aid = m.getRecipient();
    if (aid == null) return false;
    if (super.send(m, false) && !aid.isTopic()) return true;
    if (!relay) return false;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.SEND;
    rq.message = m;
    rq.relay = false;
    String json = rq.toJson();
    if (needsCleanup) cleanupSlaves();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves)
        slave.println(json);
    }
    return true;
  }

  @Override
  public AgentID[] getAgents() {
    List<AgentID> rv = new ArrayList<AgentID>();
    AgentID[] aids = super.getAgents();
    for (int i = 0; i < aids.length; i++)
      rv.add(aids[i]);
    JsonMessage rq = new JsonMessage();
    rq.action = Action.AGENTS;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    if (needsCleanup) cleanupSlaves();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves) {
        JsonMessage rsp = slave.printlnAndGetResponse(json, rq.id, TIMEOUT);
        if (rsp != null && rsp.agentIDs != null) {
          for (int i = 0; i < rsp.agentIDs.length; i++)
            rv.add(rsp.agentIDs[i]);
        }
      }
    }
    return rv.toArray(new AgentID[0]);
  }

  @Override
  public String[] getServices() {
    Set<String> rv = new HashSet<String>();
    String[] svc = super.getServices();
    for (int i = 0; i < svc.length; i++)
      rv.add(svc[i]);
    JsonMessage rq = new JsonMessage();
    rq.action = Action.SERVICES;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    if (needsCleanup) cleanupSlaves();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves) {
        JsonMessage rsp = slave.printlnAndGetResponse(json, rq.id, TIMEOUT);
        if (rsp != null && rsp.services != null) {
          for (int i = 0; i < rsp.services.length; i++)
            rv.add(rsp.services[i]);
        }
      }
    }
    return rv.toArray(new String[0]);
  }

  @Override
  public AgentID agentForService(String service) {
    AgentID aid = super.agentForService(service);
    if (aid != null) return aid;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.AGENT_FOR_SERVICE;
    rq.service = service;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    if (needsCleanup) cleanupSlaves();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves) {
        JsonMessage rsp = slave.printlnAndGetResponse(json, rq.id, TIMEOUT);
        if (rsp != null && rsp.agentID != null) return rsp.agentID;
      }
    }
    return null;
  }

  @Override
  public AgentID[] agentsForService(String service) {
    List<AgentID> rv = new ArrayList<AgentID>();
    AgentID[] aids = super.agentsForService(service);
    if (aids != null)
      for (int i = 0; i < aids.length; i++)
        rv.add(aids[i]);
    JsonMessage rq = new JsonMessage();
    rq.action = Action.AGENTS_FOR_SERVICE;
    rq.service = service;
    rq.id = UUID.randomUUID().toString();
    String json = rq.toJson();
    if (needsCleanup) cleanupSlaves();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves) {
        JsonMessage rsp = slave.printlnAndGetResponse(json, rq.id, TIMEOUT);
        if (rsp != null && rsp.agentIDs != null) {
          for (int i = 0; i < rsp.agentIDs.length; i++)
            rv.add(rsp.agentIDs[i]);
        }
      }
    }
    return rv.toArray(new AgentID[0]);
  }

  @Override
  AgentID[] getLocalAgents() {
    return getAgents();
  }

  @Override
  String[] getLocalServices() {
    return getServices();
  }

  @Override
  AgentID localAgentForService(String service) {
    return agentForService(service);
  }

  @Override
  AgentID[] localAgentsForService(String service) {
    return agentsForService(service);
  }

  @Override
  public void shutdown() {
    if (!running) return;
    JsonMessage rq = new JsonMessage();
    rq.action = Action.SHUTDOWN;
    String json = rq.toJson();
    synchronized(slaves) {
      for (ConnectionHandler slave: slaves) {
        slave.println(json);
        slave.close();
      }
      slaves.clear();
      needsCleanup = false;
    }
    if (listener != null) {
      listener.close();
      listener = null;
    }
    super.shutdown();
  }

  @Override
  public String toString() {
    String s = getClass().getName()+"@"+name;
    s += "/master/"+platform;
    return s;
  }

  @Override
  public void connectionClosed(ConnectionHandler handler) {
    log.info("Connection "+handler.getName()+" closed");
    needsCleanup = true;
  }

  /////////////// ConnectionListener interface method

  @Override
  public void connected(TcpConnector conn) {
    log.info("Incoming connection "+conn.toString());
    ConnectionHandler t = new ConnectionHandler(conn, MasterContainer.this);
    synchronized(slaves) {
      slaves.add(t);
    }
    t.start();
  }

  /////////////// Private stuff

  private void openTcpServer(int port) throws IOException {
    listener = new TcpServer(port, this);
    log.info("Listening on port "+listener.getPort());
  }

  private void openSerialPort(String devname, int baud, String settings) {
    try {
      SerialPortConnector conn = new SerialPortConnector(devname, baud, settings);
      log.info("Listening on "+devname+"@"+baud);
      ConnectionHandler t = new ConnectionHandler(conn, MasterContainer.this);
      synchronized(slaves) {
        slaves.add(t);
      }
      t.start();
    } catch (IOException ex) {
      throw new FjageError(ex.getMessage());
    }
  }

  private void cleanupSlaves() {
    synchronized(slaves) {
      Iterator<ConnectionHandler> it = slaves.iterator();
      while (it.hasNext()) {
        ConnectionHandler slave = it.next();
        if (slave.isClosed()) it.remove();
      }
    }
    needsCleanup = false;
  }

}
