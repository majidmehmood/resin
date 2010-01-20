/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.connection;

import java.net.InetAddress;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Represents a protocol-independent connection.  Protocol servers and
 * their associated Requests use Connection to retrieve the read and
 * write streams and to get information about the connection.
 *
 * <p>TcpConnection is the most common implementation.  The test harness
 * provides a string based Connection.
 */
public abstract class AbstractTransportConnection
  implements TransportConnection
{
  private final ReadStream _readStream;
  private final WriteStream _writeStream;

  public AbstractTransportConnection()
  {
    _readStream = new ReadStream();
    _readStream.setReuseBuffer(true);
    _writeStream = new WriteStream();
    _writeStream.setReuseBuffer(true);
  }

  /**
   * Returns the connection id.  Primarily for debugging.
   */
  abstract public int getId();

  /**
   * Returns the connection's buffered read stream.  If the ReadStream
   * needs to block, it will automatically flush the corresponding
   * WriteStream.
   */
  public final ReadStream getReadStream()
  {
    return _readStream;
  }

  /**
   * Returns the connection's buffered write stream.  If the ReadStream
   * needs to block, it will automatically flush the corresponding
   * WriteStream.
   */
  public final WriteStream getWriteStream()
  {
    return _writeStream;
  }

  /**
   * Returns true if secure (ssl)
   */
  public boolean isSecure()
  {
    return false;
  }
  /**
   * Returns the static virtual host
   */
  public String getVirtualHost()
  {
    return null;
  }
  /**
   * Returns the local address of the connection
   */
  public abstract InetAddress getLocalAddress();

  /**
   * Returns the local port of the connection
   */
  public abstract int getLocalPort();

  /**
   * Returns the remote address of the connection
   */
  public abstract InetAddress getRemoteAddress();

  /**
   * Returns the remote client's inet address.
   */
  public String getRemoteHost()
  {
    return getRemoteAddress().getHostAddress();
  }

  /**
   * Returns the remote address of the connection
   */
  public int getRemoteAddress(byte []buffer, int offset, int length)
  {
    InetAddress remote = getRemoteAddress();
    String name = remote.getHostAddress();
    int len = name.length();

    for (int i = 0; i < len; i++)
      buffer[offset + i] = (byte) name.charAt(i);

    return len;
  }

  /**
   * Returns the remove port of the connection
   */
  public abstract int getRemotePort();

  /**
   * Sends a broadcast request.
   */
  /*
  public void sendBroadcast(BroadcastTask task)
  {
  }
  */

  public boolean isKeepaliveAllocated()
  {
    return false;
  }

  /*
  public boolean toKeepalive()
  {
    return false;
  }
  */

  public void killKeepalive()
  {
  }

  public ConnectionState getState()
  {
    return ConnectionState.REQUEST_ACTIVE_KA;
  }

  /**
   * Returns true for a comet connection
   */
  public boolean isCometActive()
  {
    return false;
  }

  /**
   * Returns true for a comet connection
   */
  public boolean isCometSuspend()
  {
    return false;
  }
  
  public boolean isCometComplete()
  {
    return false;
  }

  /**
   * Returns true for a duplex connection
   */
  public boolean isDuplex()
  {
    return false;
  }

  public boolean wake()
  {
    return false;
  }

  /**
   * Starts a comet request
   */
  public AsyncController toComet(CometHandler cometHandler)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Close the controller
   */
  /*
  public void closeController(ConnectionCometController controller)
  {
  }

  public void toCometComplete()
  {
  }
  */

  /*
  public void toSuspend()
  {
  }
  */

  /**
   * Wakes the connection
   */
  /*
  protected boolean wake()
  {
    return false;
  }
  */

  /*
  static {
    try {
      Class<?> asyncComet = Class.forName("com.caucho.server.connection.AsyncConnectionCometController");

      _cometConstructor = asyncComet.getConstructor(new Class[] {
          TransportConnection.class,
          boolean.class,
          ServletRequest.class,
          ServletResponse.class
        });
    } catch (Throwable e) {
    }
  }
  */
}