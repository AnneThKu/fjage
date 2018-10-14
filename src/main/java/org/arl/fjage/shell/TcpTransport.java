/******************************************************************************

Copyright (c) 2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.shell;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * TCP server implementation.
 */
public class TcpTransport extends Thread implements Transport {

  protected int port;
  protected boolean charmode;
  protected ServerSocket sock = null;
  protected OutputThread outThread = null;
  protected List<ClientThread> clientThreads = Collections.synchronizedList(new ArrayList<ClientThread>());
  protected Logger log = Logger.getLogger(getClass().getName());
  protected PseudoInputStream pin = new PseudoInputStream();
  protected PseudoOutputStream pout = new PseudoOutputStream();

  /**
   * Creates a TCP server running on a specified port.
   *
   * @param port TCP port number.
   */
  public TcpTransport(int port, boolean charmode) {
    this.port = port;
    this.charmode = charmode;
    setName(getClass().getSimpleName());
    setDaemon(true);
    start();
  }

  /**
   * Shutdown the TCP server.
   */
  public void shutdown() {
    synchronized(clientThreads) {
      for (ClientThread t: clientThreads)
        t.close();
    }
    clientThreads.clear();
    try {
      ServerSocket s = sock;
      sock = null;
      s.close();
    } catch (Exception ex) {
      // do nothing
    }
  }

  @Override
  public void run() {
    outThread = new OutputThread();
    outThread.start();
    try {
      log.info("Listening on port "+port);
      sock = new ServerSocket(port);
      while (sock != null) {
        try {
          new ClientThread(sock.accept()).start();
        } catch (IOException ex) {
          // do nothing
        }
      }
    } catch (IOException ex) {
      // do nothing
    }
    log.info("Stopped listening");
    outThread.close();
    outThread = null;
  }

  @Override
  public InputStream getInputStream() {
    return pin;
  }

  @Override
  public OutputStream getOutputStream() {
    return pout;
  }

  // thread to monitor incoming data on output stream and write to TCP clients

  private class OutputThread extends Thread {

    OutputThread() {
      setName(getClass().getSimpleName());
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        int c = pout.read();
        if (c < 0) break;
        synchronized(clientThreads) {
          for (ClientThread t: clientThreads)
            t.write(c);
        }
      }
    }

    void close() {
      if (pout != null) pout.close();
    }

  }

  // threads to monitor incoming data from TCP clients and write to input stream

  private class ClientThread extends Thread {

    Socket client;
    OutputStream out = null;

    ClientThread(Socket client) {
      setName(getClass().getSimpleName());
      setDaemon(true);
      this.client = client;
    }

    @Override
    public void run() {
      clientThreads.add(this);
      String cname = "(unknown)";
      InputStream in = null;
      try {
        cname = client.getInetAddress().toString();
        log.info("New connection from "+cname);
        in = client.getInputStream();
        out = client.getOutputStream();
        if (charmode) {
          int[] charmodeBytes = new int[] { 255, 251, 1, 255, 251, 3, 255, 252, 34 };
          for (int b: charmodeBytes)
            out.write(b);
          out.flush();
        }
        // ignore initial handshake data
        try {
          sleep(100);
        } catch (InterruptedException ex) {
          // do nothing
        }
        while (in.available() > 0)
          in.read();
        // process incoming data
        while (true) {
          int c = in.read();
          if (c < 0 || c == 4) break;
          if (c > 0) pin.write(c);
        }
      } catch (Exception ex) {
        // do nothing
      }
      log.info("Connection from "+cname+" closed");
      close(in);
      close(out);
      close(client);
      clientThreads.remove(this);
      client = null;
      out = null;
    }

    void write(int c) {
      try {
        if (out != null) {
          out.write(c);
          out.flush();
        }
      } catch (IOException ex) {
        // do nothing
      }
    }

    void close() {
      close(client);
    }

    void close(Closeable x) {
      try {
        if (x != null) x.close();
      } catch (IOException ex) {
        // do nothing
      }
    }

  }

}
