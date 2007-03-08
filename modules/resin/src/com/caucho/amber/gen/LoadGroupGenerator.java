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
 * @author Scott Ferguson
 */

package com.caucho.amber.gen;

import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JMethod;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates the Java code for the wrapped object.
 */
public class LoadGroupGenerator extends ClassComponent {
  private static final L10N L = new L10N(LoadGroupGenerator.class);

  private String _extClassName;
  private RelatedType _relatedType;
  private int _index;

  public LoadGroupGenerator(String extClassName,
                            RelatedType relatedType,
                            int index)
  {
    _extClassName = extClassName;
    _relatedType = relatedType;
    _index = index;
  }

  /**
   * Generates the load group.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("protected void __caucho_load_" + _index +  "(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.pushDepth();

    int group = _index / 64;
    long mask = (1L << (_index % 64));

    // jpa/0ge2: MappedSuperclassType
    if (_relatedType.getTable() != null) {
      generateTransactionChecks(out, group, mask);

      int min = 0;

      if (_relatedType.getParentType() == null)
        min = _index;

      // XXX: need to do another check for a long hierarchy and/or many-to-one
      // if ((_relatedType.getParentType() != null) &&
      //     (_index = _relatedType.getParentType().getLoadGroupIndex() + 1)) {
      //   min = _relatedType.getParentType().getLoadGroupIndex();
      // }

      int max = _index;

      for (int i = min; i <= max; i++) {
        out.println("__caucho_load_select_" + i + "(aConn);");
      }

      if (min <= max) {
        // jpa/0g0k: only makes transactional if exists.
        out.println();
        out.println("if ((__caucho_loadMask_0 & 1L) != 0) {");
        out.println("  aConn.makeTransactional(this);");
        out.println("}");
      }

      // needs to be after load to prevent loop if toString() expects data
      out.println();
      out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINE))");
      out.println("  __caucho_log.fine(\"amber loaded-" + _index + " \" + this);");

      out.println();
      out.println("aConn.postLoad(this);");

      generateCallbacks(out, _relatedType.getPostLoadCallbacks());
    }

    out.popDepth();
    out.println("}");

    if (_index == 0 && _relatedType.getHasLoadCallback()) {
      out.println();
      out.println("protected void __caucho_load_callback() {}");
    }

    generateLoadSelect(out, group, mask);
  }

  private void generateTransactionChecks(JavaWriter out, int group, long mask)
    throws IOException
  {
    // non-read-only entities must be reread in a transaction
    if (! _relatedType.isReadOnly()) {
      out.println("if (aConn.isInTransaction()) {");

      // deleted objects are not reloaded
      out.println("  if (com.caucho.amber.entity.EntityState.P_DELETING.ordinal() <= __caucho_state.ordinal()) {");
      out.println("    return;");
      out.println("  }");

      // from non-transactional to transactional
      out.println("  else if (__caucho_state.ordinal() <= com.caucho.amber.entity.EntityState.P_NON_TRANSACTIONAL.ordinal()) {");
      out.println("    com.caucho.amber.entity.EntityState state = __caucho_state;");

      out.println("    __caucho_state = com.caucho.amber.entity.EntityState.P_TRANSACTIONAL;");

      // XXX: ejb/0d01 (create issue?)
      // jpa/0g0k: see __caucho_load_select
      // out.println("    aConn.makeTransactional(this);");
      // out.println("    if ((state > 0) && ((__caucho_loadMask_" + group + " & " + mask + "L) != 0))");
      // out.println("      return;");
      out.println();

      int loadCount = _relatedType.getLoadGroupIndex();
      for (int i = 0; i <= loadCount / 64; i++) {
        out.println("    __caucho_loadMask_" + i + " = 0;");
      }
      int dirtyCount = _relatedType.getDirtyIndex();
      for (int i = 0; i <= dirtyCount / 64; i++) {
        out.println("    __caucho_dirtyMask_" + i + " = 0;");
      }

      out.println("  }");
      // ejb/0d01 - already loaded in the transaction
      out.println("  else if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0)");
      out.println("    return;");
      out.println("}");
      out.print("else ");
    }

    out.println("if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0)");
    out.println("  return;");

    // XXX: the load doesn't cover other load groups
    out.println("else if (__caucho_item != null) {");
    out.pushDepth();
    out.println(_extClassName + " item = (" + _extClassName + ") __caucho_item.getEntity();");

    out.println("item.__caucho_load_" + _index + "(aConn);");

    _relatedType.generateCopyLoadObject(out, "super", "item", _index);

    // out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");
    out.println("__caucho_loadMask_" + group + " |= item.__caucho_loadMask_" + group + ";"); // mask + "L;");

    out.println();
    out.println("return;");

    out.popDepth();
    out.println("}");

    out.println();
  }

  private void generateLoadSelect(JavaWriter out, int group, long mask)
    throws IOException
  {
    out.println();
    out.println("protected void __caucho_load_select_" + _index +  "(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.pushDepth();

    if (_relatedType.getTable() == null) {
      out.popDepth();
      out.println("}");

      return;
    }

    Table table = _relatedType.getTable();

    String from = null;
    String select = null;
    String where = null;

    String subSelect = _relatedType.generateLoadSelect(table, "o", _index);
    Table mainTable = null;
    String tableName = null;

    if (subSelect != null) {
      select = subSelect;

      from = table.getName() + " o";
      where = _relatedType.getId().generateMatchArgWhere("o");

      mainTable = table;
      tableName = "o";
    }

    ArrayList<Table> subTables = _relatedType.getSecondaryTables();

    for (int i = 0; i < subTables.size(); i++) {
      Table subTable = subTables.get(i);

      subSelect = _relatedType.generateLoadSelect(subTable, "o" + i, _index);

      if (subSelect == null)
        continue;

      if (select != null)
        select = select + ", " + subSelect;
      else
        select = subSelect;

      if (from != null)
        from = from + ", " + subTable.getName() + " o" + i;
      else
        from = subTable.getName() + " o" + i;

      if (where != null) {
        LinkColumns link = subTable.getDependentIdLink();

        where = where + " and " + link.generateJoin("o" + i, "o");
      }
      else
        throw new IllegalStateException();
    }

    if (select == null) {
      if (_index > 0) {
        // XXX: jpa/0o00

        out.println("return;");

        out.popDepth();
        out.println("}");

        return;
      }

      select = "1";
    }

    if (where == null) {
      from = table.getName() + " o";

      where = _relatedType.getId().generateMatchArgWhere("o");
    }

    String sql = "select " + select + " from " + from + " where " + where;

    out.println("java.sql.ResultSet rs = null;");
    out.println();

    out.println("try {");
    out.pushDepth();

    out.println("String sql = \"" + sql + "\";");

    out.println();
    out.println("java.sql.PreparedStatement pstmt = aConn.prepareStatement(sql);");

    out.println("int index = 1;");
    _relatedType.getId().generateSet(out, "pstmt", "index", "super");

    out.println();
    out.println("rs = pstmt.executeQuery();");

    out.println("if (rs.next()) {");
    out.pushDepth();

    _relatedType.generateLoad(out, "rs", "", 1, _index, null);
    out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");

    // jpa/0o01, jpa/0o04
    // XXX: Should be handled in AmberEntityHome.find()?
    // XXX: Will this ever add the cache entity to the context?
    if (_relatedType instanceof EntityType) {
      // XXX: add isJPA test?
      out.println("aConn.addEntity(this);");
    }

    _relatedType.generateLoadEager(out, "rs", "", 1, _index);

    // commented out: jpa/0r01
    // ArrayList<JMethod> postLoadCallbacks = _relatedType.getPostLoadCallbacks();
    // if (postLoadCallbacks.size() > 0 && _index == 0) {
    //   out.println("if (__caucho_state == com.caucho.amber.entity.EntityState.P_TRANSACTIONAL) {");
    //   out.pushDepth();
    //   generateCallbacks(out, postLoadCallbacks);
    //   out.popDepth();
    //   out.println("}");
    // }

    if (_relatedType.getHasLoadCallback())
      out.println("__caucho_load_callback();");

    out.popDepth();
    out.println("}");
    out.println("else {");
    out.println("  rs.close();");

    String errorString = ("(\"amber load: no matching object " +
                          _relatedType.getName() + "[\" + __caucho_getPrimaryKey() + \"]\")");

    out.println("  throw new com.caucho.amber.AmberObjectNotFoundException(" + errorString + ");");
    out.println("}");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("} finally {");
    out.println("  try {");
    out.println("    if (rs != null)");
    out.println("      rs.close();");
    out.println("  } catch (RuntimeException e) {");
    out.println("    throw e;");
    out.println("  } catch (Exception e) {");
    out.println("    throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("  }");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  private void generateCallbacks(JavaWriter out, ArrayList<JMethod> callbacks)
    throws IOException
  {
    if (callbacks.size() == 0)
      return;

    out.println();
    for (JMethod method : callbacks) {
      out.println(method.getName() + "();");
    }
  }
}
