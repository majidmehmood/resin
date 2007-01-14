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

package com.caucho.jsf.taglib;

import java.io.*;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * The f:selectItem
 */
public class FacesSelectItemTag extends UIComponentELTag
{
  private ValueExpression _itemDisabled;
  private ValueExpression _itemLabel;
  private ValueExpression _itemValue;
  private String _id;

  public String getComponentType()
  {
    return "javax.faces.SelectItem";
  }

  public String getRendererType()
  {
    return null;
  }

  public String getId()
  {
    return _id;
  }

  public void setId(String id)
  {
    _id = id;
  }
  
  public void setItemLabel(ValueExpression expr)
  {
    _itemLabel = expr;
  }
  
  public void setItemValue(ValueExpression expr)
  {
    _itemValue = expr;
  }
  
  public void setItemDisabled(ValueExpression expr)
  {
    _itemDisabled = expr;
  }
  
  /**
   * Sets the overridden properties of the tag
   */
  @Override
  protected void setProperties(UIComponent component)
  {
    super.setProperties(component);

    if (_itemDisabled != null)
      component.setValueExpression("itemDisabled", _itemDisabled);

    if (_itemLabel != null)
      component.setValueExpression("itemLabel", _itemLabel);

    if (_itemValue != null)
      component.setValueExpression("itemValue", _itemValue);
  }
}
