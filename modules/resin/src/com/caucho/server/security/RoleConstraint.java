/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.security;

import java.io.*;
import java.util.*;
import java.security.*;

import javax.servlet.http.*;
import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.http.*;

public class RoleConstraint extends AbstractConstraint {
  private String []_roles;

  public void addRoleName(String role)
  {
    if (_roles == null)
      _roles = new String[] { role };
    else {
      String []newRoles = new String[_roles.length + 1];
      System.arraycopy(_roles, 0, newRoles, 0, _roles.length);
      newRoles[_roles.length] = role;
      _roles = newRoles;
    }
  }
  
  /**
   * Returns true if the constraint requires authentication.
   */
  public boolean needsAuthentication()
  {
    return _roles != null && _roles.length > 0;
  }

  /**
   * Returns true if the user is authorized for the resource.
   */
  public boolean isAuthorized(HttpServletRequest request,
                              HttpServletResponse response,
                              ServletContext application)
    throws ServletException, IOException
  {
    for (int i = 0; _roles != null && i < _roles.length; i++) {
      if (_roles[i].equals("*"))
        return true;
      if (request.isUserInRole(_roles[i])) {
        return true;
      }
    }
    
    response.sendError(HttpServletResponse.SC_FORBIDDEN, null);

    return false;
  }

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("RoleConstraint[");
    for (int i = 0; i < _roles.length; i++) {
      if (i != 0)
        cb.append(',');
      cb.append(_roles[i]);
    }
    cb.append("]");
    
    return cb.close();
  }
}
