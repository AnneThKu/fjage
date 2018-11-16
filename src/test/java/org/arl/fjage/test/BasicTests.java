/******************************************************************************

Copyright (c) 2013, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.test;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.*;
import org.arl.fjage.*;
import org.arl.fjage.remote.*;
import org.arl.fjage.shell.*;
import org.junit.*;

public class BasicTests {

  private static final int TICKS = 100;
  private static final int DELAY = 1100;

  private Random rnd = new Random();
  private Logger log = Logger.getLogger(getClass().getName());

  @Before
  public void beforeTesting() {
    LogFormatter.install(null);
  }

  @Test
  public void testRT() {
    log.info("testRT");
    Platform platform = new RealTimePlatform();
    Container container = new Container(platform);
    ClientAgent client = new ClientAgent();
    ServerAgent server = new ServerAgent();
    container.add("C", client);
    container.add("S", server);
    platform.start();
    platform.delay(DELAY);
    platform.shutdown();
    assertTrue(client.bad == 0);
    assertTrue(client.good == client.requests);
    assertTrue(client.requests == server.requests);
    assertTrue(client.nuisance == server.nuisance);
  }

  @Test
  public void testSim() {
    log.info("testSim");
    Platform platform = new DiscreteEventSimulator();
    Container container = new Container(platform);
    ClientAgent client = new ClientAgent();
    ServerAgent server = new ServerAgent();
    container.add("C", client);
    container.add("S", server);
    platform.start();
    platform.delay(DELAY);
    platform.shutdown();
    assertTrue(client.bad == 0);
    assertTrue(client.good == client.requests);
    assertTrue(client.requests == server.requests);
    assertTrue(client.nuisance == server.nuisance);
  }

  @Test
  public void testRemote1() throws IOException {
    log.info("testRemote1");
    Platform platform = new RealTimePlatform();
    MasterContainer master = new MasterContainer(platform);
    Container slave = new SlaveContainer(platform, "localhost", master.getPort());
    ClientAgent client = new ClientAgent();
    ServerAgent server = new ServerAgent();
    slave.add("C", client);
    master.add("S", server);
    platform.start();
    AgentID c = new AgentID("C");
    AgentID s = new AgentID("S");
    assertTrue(master.containsAgent(s));
    assertTrue(!master.containsAgent(c));
    assertTrue(master.canLocateAgent(c));
    assertTrue(slave.containsAgent(c));
    assertTrue(!slave.containsAgent(s));
    assertTrue(slave.canLocateAgent(s));
    platform.delay(DELAY);
    platform.shutdown();
    assertTrue(client.bad == 0);
    assertTrue(client.good == client.requests);
    assertTrue(client.requests == server.requests);
    assertTrue(client.nuisance == server.nuisance);
  }

  @Test
  public void testRemote2() throws IOException {
    log.info("testRemote2");
    Platform platform = new RealTimePlatform();
    MasterContainer master = new MasterContainer(platform);
    Container slave = new SlaveContainer(platform, "localhost", master.getPort());
    ClientAgent client = new ClientAgent();
    ServerAgent server = new ServerAgent();
    master.add("C", client);
    slave.add("S", server);
    platform.start();
    platform.delay(DELAY);
    platform.shutdown();
    assertTrue(client.bad == 0);
    assertTrue(client.good == client.requests);
    assertTrue(client.requests == server.requests);
    assertTrue(client.nuisance == server.nuisance);
  }

  @Test
  public void testRemote3() throws IOException {
    log.info("testRemote3");
    Platform platform = new RealTimePlatform();
    MasterContainer master = new MasterContainer(platform);
    Container slave = new SlaveContainer(platform, "localhost", master.getPort());
    ClientAgent client = new ClientAgent();
    ServerAgent server = new ServerAgent();
    slave.add("C", client);
    slave.add("S", server);
    platform.start();
    platform.delay(DELAY);
    platform.shutdown();
    assertTrue(client.bad == 0);
    assertTrue(client.good == client.requests);
    assertTrue(client.requests == server.requests);
    assertTrue(client.nuisance == server.nuisance);
  }

  @Test
  public void testGateway() throws IOException {
    log.info("testGateway");
    Platform platform = new RealTimePlatform();
    MasterContainer master = new MasterContainer(platform);
    ServerAgent server = new ServerAgent();
    master.add("S", server);
    platform.start();
    Gateway gw = new Gateway("localhost", master.getPort());
    Message rsp = gw.receive(100);
    assertTrue(rsp == null);
    AgentID s = gw.agentForService("server");
    assertTrue(server.getAgentID().equals(s));
    Message req = new RequestMessage(s);
    gw.send(req);
    rsp = gw.receive(100);
    assertTrue(rsp != null);
    assertTrue(rsp.getClass() == ResponseMessage.class);
    req = new RequestMessage(server.getAgentID());
    rsp = gw.request(req, 100);
    assertTrue(rsp != null);
    assertTrue(rsp.getClass() == ResponseMessage.class);
    req = new NuisanceMessage(server.getAgentID());
    rsp = gw.request(req, 100);
    assertTrue(rsp == null);
    gw.shutdown();
    platform.shutdown();
  }

  @Test
  public void testFSM() {
    log.info("testFSM");
    Platform platform = new RealTimePlatform();
    Container container = new Container(platform);
    Agent agent = new Agent();
    container.add(agent);
    FSMBehavior fsm = new FSMBehavior();
    fsm.add(new FSMBehavior.State("tick") {
      @Override
      public void onEnter() {
        block(100);
      }
      @Override
      public void action() {
        setNextState("tock");
      }
    });
    fsm.add(new FSMBehavior.State("tock") {
      int n = 0;
      @Override
      public void onEnter() {
        block(50);
      }
      @Override
      public void action() {
        n++;
        if (n > 5) terminate();
        else setNextState("tick");
      }
    });
    agent.add(fsm);
    platform.start();
    platform.delay(DELAY);
    assertTrue(fsm.done());
    platform.shutdown();
  }

  @Test
  public void testTickers() {
    log.info("testTickers");
    final int nAgents = 10;
    final int tickDelay = 100;
    final int ticks = 6000;
    Platform platform = new DiscreteEventSimulator();
    Container container = new Container(platform);
    TickerBehavior[] tb = new TickerBehavior[nAgents];
    for (int i = 0; i < nAgents; i++) {
      Agent agent = new Agent();
      container.add(agent);
      tb[i] = new TickerBehavior(tickDelay) {
        long last = -1;
        @Override
        public void onTick() {
          if (getTickCount() >= ticks) stop();
          if (last >= 0 && agent.currentTimeMillis() != last+tickDelay) {
            log.warning("broken / ticks = "+getTickCount()+", expected = "+(last+tickDelay)+", now = "+agent.currentTimeMillis());
            stop();
          }
          last = agent.currentTimeMillis();
        }
      };
      agent.add(tb[i]);
    }
    platform.start();
    platform.delay(tickDelay*ticks+1000);
    platform.shutdown();
    for (int i = 0; i < nAgents; i++)
      assertTrue("ticks = "+tb[i].getTickCount()+", expected "+ticks, tb[i].getTickCount() == ticks);
  }

  @Test
  public void testSerialCloner() {
    log.info("testSerialCloner");
    Platform platform = new DiscreteEventSimulator();
    Container container = new Container(platform);
    container.setCloner(Container.SERIAL_CLONER);
    RequestMessage s1 = new RequestMessage(null);
    s1.x = 77;
    RequestMessage s2 = container.clone(s1);
    assertTrue(s1 != s2);
    assertTrue(s1.x == s2.x);
  }

  @Test
  public void testFastCloner() {
    log.info("testFastCloner");
    Platform platform = new DiscreteEventSimulator();
    Container container = new Container(platform);
    container.setCloner(Container.FAST_CLONER);
    RequestMessage s1 = new RequestMessage(null);
    s1.x = 77;
    RequestMessage s2 = container.clone(s1);
    assertTrue(s1 != s2);
    assertTrue(s1.x == s2.x);
  }

  @Test
  public void testShell() {
    log.info("testShell");
    Platform platform = new RealTimePlatform();
    Container container = new Container(platform);
    container.add("shell", new ShellAgent(new EchoScriptEngine()));
    ShellTestAgent agent = new ShellTestAgent();
    container.add("test", agent);
    platform.start();
    while (!agent.done)
      platform.delay(1000);
    platform.shutdown();
    assertTrue(agent.exec);
    assertTrue(agent.put);
    assertTrue(agent.get);
    assertTrue(agent.dir);
    assertTrue(agent.del);
  }

  private static class RequestMessage extends Message {
    private static final long serialVersionUID = 1L;
    public int x;
    public RequestMessage(AgentID recipient) {
      super(recipient, Performative.REQUEST);
    }
  }

  private static class ResponseMessage extends Message {
    private static final long serialVersionUID = 1L;
    public int x, y;
    public ResponseMessage(Message request) {
      super(request, Performative.INFORM);
    }
  }

  private static class NuisanceMessage extends Message {
    private static final long serialVersionUID = 1L;
    public NuisanceMessage(AgentID recipient) {
      super(recipient, Performative.INFORM);
    }
  }

  private class ServerAgent extends Agent {
    public int requests = 0, nuisance = 0;
    @Override
    public void init() {
      register("server");
      subscribe(topic("noise"));
      add(new MessageBehavior(RequestMessage.class) {
        @Override
        public void onReceive(Message msg) {
          requests++;
          RequestMessage req = (RequestMessage)msg;
          ResponseMessage rsp = new ResponseMessage(req);
          rsp.x = req.x;
          rsp.y = 2*req.x + 1;
          agent.send(rsp);
        }
      });
      add(new MessageBehavior(NuisanceMessage.class) {
        @Override
        public void onReceive(Message msg) {
          nuisance++;
        }
      });
    }
  }

  private class ClientAgent extends Agent {
    public int requests = 0, nuisance = 0, good = 0, bad = 0;
    @Override
    public void init() {
      add(new TickerBehavior(10) {
        @Override
        public void onTick() {
          if (getTickCount() > TICKS) {
            stop();
            return;
          }
          if (rnd.nextBoolean()) {
            requests++;
            AgentID server = agent.agentForService("server");
            RequestMessage req = new RequestMessage(server);
            req.x = requests;
            agent.send(req);
          } else {
            nuisance++;
            AgentID server = topic("noise");
            NuisanceMessage n = new NuisanceMessage(server);
            agent.send(n);
          }
        }
      });
      add(new MessageBehavior() {
        @Override
        public void onReceive(Message msg) {
          if (msg instanceof ResponseMessage) {
            ResponseMessage rsp = (ResponseMessage)msg;
            if (2*rsp.x + 1 == rsp.y) good++;
            else bad++;
          } else bad++;
        }
      });
    }
  }

  private class ShellTestAgent extends Agent {
    private final String DIRNAME = "/tmp";
    private final String FILENAME = "fjage-test.txt";
    public boolean exec = false, put = false, get = false, del = false, dir = false, done = false;
    @Override
    public void init() {
      add(new OneShotBehavior() {
        @Override
        public void action() {
          AgentID shell = new AgentID("shell");
          Message req = new ShellExecReq(shell, "boo");
          Message rsp = request(req);
          if (rsp != null && rsp.getPerformative() == Performative.AGREE) exec = true;
          byte[] bytes = "this is a test".getBytes();
          req = new ShellPutFileReq(shell, DIRNAME+File.separator+FILENAME, bytes);
          rsp = request(req);
          log.info("put rsp: "+rsp);
          if (rsp != null && rsp.getPerformative() == Performative.AGREE) {
            File f = new File(DIRNAME+File.separator+FILENAME);
            if (f.exists()) put = true;
          }
          req = new ShellGetFileReq(shell, DIRNAME+File.separator+FILENAME);
          rsp = request(req);
          log.info("get rsp: "+rsp);
          if (rsp != null && rsp instanceof ShellGetFileRsp) {
            byte[] contents = ((ShellGetFileRsp)rsp).getContents();
            log.info("get data len: "+contents.length);
            if (contents.length == bytes.length) {
              log.info("get data: "+new String(contents));
              get = true;
              for (int i = 0; i < contents.length; i++)
                if (contents[i] != bytes[i]) get = false;
            }
          }
          req = new ShellGetFileReq(shell, DIRNAME);
          rsp = request(req);
          log.info("get dir rsp: "+rsp);
          if (rsp != null && rsp instanceof ShellGetFileRsp) {
            String contents = new String(((ShellGetFileRsp)rsp).getContents());
            String[] lines = contents.split("\\r?\\n");
            for (String s: lines) {
              log.info("DIR: "+s);
              if (s.startsWith(FILENAME+"\t")) dir = true;
            }
          }
          req = new ShellPutFileReq(shell, DIRNAME+File.separator+FILENAME, null);
          rsp = request(req);
          log.info("del rsp: "+rsp);
          if (rsp != null && rsp.getPerformative() == Performative.AGREE) {
            File f = new File(DIRNAME+File.separator+FILENAME);
            if (!f.exists()) del = true;
          }
          log.info("exec="+exec+", put="+put+", get="+get+", del="+del+", dir="+dir);
          done = true;
        }
      });
    }
  }

}
