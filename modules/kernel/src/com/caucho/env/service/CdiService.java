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

package com.caucho.env.service;

import java.io.IOException;

import com.caucho.config.inject.InjectManager;
import com.caucho.java.WorkDir;
import com.caucho.util.L10N;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;

/**
 * Root service for the root and data directories.
 *
 */
public class CdiService extends AbstractResinService 
{
  private static final L10N L = new L10N(CdiService.class);
  public static final int START_PRIORITY = 1;
  
  private InjectManager _cdiManager;
  
  /**
   * Creates a new servlet server.
   */
  public CdiService(ResinSystem system)
  {
    _cdiManager = InjectManager.create(system.getClassLoader());
  }

  /**
   * Returns the current active directory service.
   */
  public static CdiService getCurrent()
  {
    return ResinSystem.getCurrentService(CdiService.class);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  public void start()
  {
    _cdiManager.start();
  }
}