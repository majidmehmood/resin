/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.types;

import com.caucho.config.*;
import com.caucho.config.annotation.StartupType;
import com.caucho.config.inject.AnnotatedElementImpl;
import com.caucho.config.inject.BeanTypeImpl;
import com.caucho.config.inject.BeanMethodImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.*;
import com.caucho.config.type.*;
import com.caucho.util.*;
import com.caucho.xml.QName;

import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

import javax.annotation.*;
import javax.enterprise.context.ScopeType;
import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.deployment.DeploymentType;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ManagedBean;
import javax.enterprise.inject.spi.ProducerBean;
import javax.interceptor.InterceptorBindingType;

import org.w3c.dom.Node;

/**
 * Custom bean configured by namespace
 */
public class ConfigProgramArray {
  private ArrayList<ConfigProgram> _args = new ArrayList<ConfigProgram>();

  public void addBuilderProgram(ConfigProgram program)
  {
    _args.add(program);
  }

  public ArrayList<ConfigProgram> getArgs()
  {
    return _args;
  }
}