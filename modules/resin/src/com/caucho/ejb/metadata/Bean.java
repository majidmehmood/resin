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

package com.caucho.ejb.metadata;

import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.logging.Logger;

import javax.sql.DataSource;

import javax.transaction.UserTransaction;

import javax.ejb.Stateless;
import javax.ejb.Stateful;
import javax.ejb.Entity;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.MethodPermissions;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.Inject;

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;
import com.caucho.bytecode.JClassWrapper;
import com.caucho.bytecode.JMethod;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.config.ConfigException;

import com.caucho.config.types.InitProgram;

import com.caucho.ejb.EjbServerManager;

import com.caucho.ejb.cfg.EjbConfig;
import com.caucho.ejb.cfg.EjbBean;
import com.caucho.ejb.cfg.EjbSessionBean;
import com.caucho.ejb.cfg.MethodSignature;
import com.caucho.ejb.cfg.EjbMethod;
import com.caucho.ejb.cfg.EjbMethodPattern;

import com.caucho.ejb.entity2.EntityIntrospector;

/**
 * Configuration for a new bean based on metadata.
 */
public class Bean {
  private static final L10N L = new L10N(Bean.class);
  private static final Logger log = Log.open(Bean.class);

  private ClassLoader _loader;
  
  private EjbServerManager _ejbManager;
  private EntityIntrospector _introspector;
  
  private JClass _type;
  private String _name;

  private ArrayList<InitProgram> _initList = new ArrayList<InitProgram>();

  public Bean(EjbServerManager ejbManager, EntityIntrospector introspector)
  {
    _loader = Thread.currentThread().getContextClassLoader();
    
    _ejbManager = ejbManager;
    _introspector = introspector;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the type.
   */
  public void setType(String typeName)
    throws ConfigException, ClassNotFoundException
  {
    _type = _ejbManager.getJClassLoader().forName(typeName);

    if (_type == null) {
      throw new ConfigException(L.l("'{0}' is an unknown type",
				    typeName));
    }
    else if (_type.getAnnotation(Stateless.class) != null) {
    }
    else if (_type.getAnnotation(Stateful.class) != null) {
    }
    else if (_type.getAnnotation(Entity.class) != null) {
    }
    else
      throw new ConfigException(L.l("{0} is an unknown bean type.",
				    _type.getName()));
  }

  /**
   * Adds an init.
   */
  public void addInit(InitProgram init)
  {
    _initList.add(init);
  }

  /**
   * Initializes the bean.
   */
  public void init()
    throws ConfigException
  {
    if (_type == null)
      throw new ConfigException(L.l("type is a require attribute of ejb-server"));
    JAnnotation stateless = _type.getAnnotation(Stateless.class);
    JAnnotation stateful = _type.getAnnotation(Stateful.class);

    try {
      if (stateless != null)
	configureStateless(_type);
      else if (stateful != null)
	configureStateful(_type);
      else if (_type.getAnnotation(Entity.class) != null) {
	_introspector.introspect(_type);
      }
      else
	throw new ConfigException(L.l("only stateless beans are currently supported."));
    } catch (RuntimeException e) {
      throw e;
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  private void configureStateless(JClass type)
    throws ConfigException
  {
    String className = type.getName();
    
    JAnnotation stateless = type.getAnnotation(Stateless.class);

    EjbSessionBean bean = new EjbSessionBean(_ejbManager.getConfig());
    bean.setAllowPOJO(true);

    bean.setSessionType("Stateless");
    bean.setTransactionType("Container");

    configureBean(bean, type, stateless.getString("name"));
  }

  private void configureStateful(JClass type)
    throws ConfigException
  {
    String className = type.getName();
    
    JAnnotation stateful = type.getAnnotation(Stateful.class);

    EjbSessionBean bean = new EjbSessionBean(_ejbManager.getConfig());
    bean.setAllowPOJO(true);

    bean.setSessionType("Stateful");
    bean.setTransactionType("Container");

    configureBean(bean, type, stateful.getString("name"));
  }

  private void configureBean(EjbBean bean, JClass type, String defaultName)
    throws ConfigException
  {
    try {
      bean.setEJBClassWrapper(type);

      String name = _name;

      if (name == null || name.equals(""))
	name = defaultName;

      if (name == null || name.equals("")) {
	String className = type.getName();
      
	int p = className.lastIndexOf('.');

	if (p > 0)
	  name = className.substring(p + 1);
	else
	  name = className;
      }

      bean.setEJBName(name);

      JClass []ifs = type.getInterfaces();

      for (int i = 0; i < ifs.length; i++) {
	JAnnotation local = ifs[i].getAnnotation(Local.class);

	if (local != null) {
	  bean.setLocalWrapper(ifs[i]);
	  continue;
	}
      
	JAnnotation remote = ifs[i].getAnnotation(Remote.class);

	if (remote != null || ifs[i].isAssignableTo(java.rmi.Remote.class)) {
	  bean.setRemoteWrapper(ifs[i]);
	  continue;
	}
      }

      if (bean.getLocal() != null || bean.getRemote() != null) {
      }
      else if (ifs.length == 0)
	throw new ConfigException(L.l("'{0}' has no interfaces.  Can't currently generate.",
				      type.getName()));
      else if (ifs.length != 1)
	throw new ConfigException(L.l("'{0}' has multiple interfaces, but none are marked as @Local or @Remote.",
				      type.getName()));
      else {
	bean.setLocalWrapper(ifs[0]);
      }

      JAnnotation xa = type.getAnnotation(TransactionAttribute.class);
      if (xa != null) {
	MethodSignature sig = new MethodSignature();
	sig.setMethodName("*");

	EjbMethodPattern pattern = bean.createMethod(sig);

	setPatternTransaction(pattern, xa);
      }

      JAnnotation perms = type.getAnnotation(MethodPermissions.class);
      if (perms != null) {
	MethodSignature sig = new MethodSignature();
	sig.setMethodName("*");

	EjbMethodPattern pattern = bean.createMethod(sig);

	// XXX: pattern.setRoles(perms.value());
      }
    
      configureMethods(bean, type);

      for (int i = 0; i < _initList.size(); i++)
	bean.addInitProgram(_initList.get(i).getBuilderProgram());

      bean.init();

      _ejbManager.getConfig().setBeanConfig(bean.getEJBName(), bean);
    } catch (ConfigException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  private void configureMethods(EjbBean bean, JClass type)
    throws ConfigException
  {
    JMethod []methods = type.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      JMethod method = methods[i];
      
      JAnnotation xa = method.getAnnotation(TransactionAttribute.class);

      if (xa != null) {
	EjbMethodPattern pattern = bean.createMethod(getSignature(method));

	setPatternTransaction(pattern, xa);
      }
      
      JAnnotation perms = method.getAnnotation(MethodPermissions.class);
      if (perms != null) {
	EjbMethodPattern pattern = bean.createMethod(getSignature(method));

	// XXX: pattern.setRoles(perms.value());
      }
    }
  }

  private void setPatternTransaction(EjbMethodPattern pattern,
				     JAnnotation xa)
    throws ConfigException
  {
    TransactionAttributeType xaType;
    xaType = (TransactionAttributeType) xa.get("value");
    
    switch (xaType) {
    case REQUIRED:
      pattern.setTransaction(EjbMethod.TRANS_REQUIRED);
      break;
	  
    case REQUIRESNEW:
      pattern.setTransaction(EjbMethod.TRANS_REQUIRES_NEW);
      break;
	  
    case MANDATORY:
      pattern.setTransaction(EjbMethod.TRANS_MANDATORY);
      break;
	  
    case SUPPORTS:
      pattern.setTransaction(EjbMethod.TRANS_SUPPORTS);
      break;
	  
    case NOTSUPPORTED:
      pattern.setTransaction(EjbMethod.TRANS_NOT_SUPPORTED);
      break;
	  
    case NEVER:
      pattern.setTransaction(EjbMethod.TRANS_NEVER);
      break;
      
    default:
      throw new IllegalStateException();
    }
  }

  private MethodSignature getSignature(JMethod method)
    throws ConfigException
  {
    MethodSignature sig = new MethodSignature();

    sig.setMethodName(method.getName());

    JClass []paramTypes = method.getParameterTypes();

    for (int i = 0; i < paramTypes.length; i++) {
      sig.addParam(paramTypes[i].getName());
    }

    return sig;
  }

  /*
  private void configureInject(EjbBean bean,
			       JMethod method,
			       Inject inject)
    throws ConfigException
  {
    JClass []paramTypes = method.getParameterTypes();

    if (paramTypes.length != 1)
      throw new ConfigException(L.l("method '{0}' must have a single value for injection.",
				    method.getName()));

    JClass paramType = paramTypes[0];

    String jndiName = inject.jndiName();
    String prefix = "";

    if (DataSource.class.isAssignableFrom(paramType)) {
      prefix = "jdbc/";
    }
    
    if (jndiName != null && ! jndiName.equals("")) {
    }
    else if (UserTransaction.class.equals(paramType)) {
      jndiName = "java:comp/UserTransaction";
    }
    else {
      jndiName = method.getName();

      if (jndiName.startsWith("set") && jndiName.length() > 3) {
	jndiName = jndiName.substring(3);

	char ch = jndiName.charAt(0);
	
	if (Character.isUpperCase(ch) &&
	    (jndiName.length() == 4 ||
	     Character.isLowerCase(jndiName.charAt(1)))) {
	  jndiName = Character.toLowerCase(ch) + jndiName.substring(1);
	}
      }
    }

    int colon = jndiName.indexOf(':');
    int slash = jndiName.indexOf('/');
    
    if (colon < 0 || slash > 0 && slash < colon)
      jndiName = "java:comp/env/" + prefix + jndiName;

    JndiInjectProgram program = new JndiInjectProgram(jndiName, method);

    bean.addInitProgram(program);
  }
  */
}

