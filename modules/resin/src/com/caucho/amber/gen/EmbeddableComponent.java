/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.gen;

import com.caucho.amber.field.*;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.Type;
import com.caucho.bytecode.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Generates the Java code for the wrapped object.
 */
public class EmbeddableComponent extends ClassComponent {
  private static final L10N L = new L10N(EmbeddableComponent.class);

  private String _baseClassName;
  private String _extClassName;

  private EmbeddableType _embeddableType;

  public EmbeddableComponent()
  {
  }

  /**
   * Sets the bean info for the generator
   */
  public void setEmbeddableType(EmbeddableType embeddableType)
  {
    _embeddableType = embeddableType;
  }

  /**
   * Sets the base class name
   */
  public void setBaseClassName(String baseClassName)
  {
    _baseClassName = baseClassName;
  }

  /**
   * Gets the base class name
   */
  public String getBaseClassName()
  {
    return _baseClassName;
  }

  /**
   * Sets the ext class name
   */
  public void setExtClassName(String extClassName)
  {
    _extClassName = extClassName;
  }

  /**
   * Sets the ext class name
   */
  public String getClassName()
  {
    return _extClassName;
  }

  /**
   * Get bean class name.
   */
  public String getBeanClassName()
  {
    return _baseClassName;
  }

  /**
   * Starts generation of the Java code
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    try {
      generateHeader(out);

      generateFields(out);

      generateLoad(out);

    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * Generates the class header for the generated code.
   */
  private void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println("/*");
    out.println(" * Generated by Resin Amber");
    out.println(" * " + com.caucho.Version.VERSION);
    out.println(" */");
    out.print("private static final java.util.logging.Logger __caucho_log = ");
    out.println("java.util.logging.Logger.getLogger(\"" + getBeanClassName() + "\");");
  }

  /**
   * Generates the fields.
   */
  private void generateFields(JavaWriter out)
    throws IOException
  {
    ArrayList<AmberField> fields = _embeddableType.getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField prop = fields.get(i);

      prop.generateSuperGetter(out);
      prop.generateGetProperty(out);

      prop.generateSuperSetter(out);
      prop.generateSetProperty(out);
    }
  }

  /**
   * Generates the load.
   */
  private void generateLoad(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public int __caucho_load(com.caucho.amber.manager.AmberConnection aConn,");
    out.println("                         java.sql.ResultSet rs,");
    out.println("                         int index)");
    out.println("  throws java.sql.SQLException");
    out.println("{");
    out.pushDepth();

    _embeddableType.generateLoad(out, "rs", "", 1, -1);

    out.println("return index;");

    out.popDepth();
    out.println("}");
  }
}
