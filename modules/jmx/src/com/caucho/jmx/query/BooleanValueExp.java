/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jmx.query;

/**
 * Represents a value expression
 */
public class BooleanValueExp extends AbstractValueExp {
  private static BooleanValueExp TRUE = new BooleanValueExp(true);
  private static BooleanValueExp FALSE = new BooleanValueExp(false);
  
  private boolean _value;
  
  /**
   * Creates a null string value expression with a value.
   */
  private BooleanValueExp(boolean value)
  {
    _value = value;
  }

  /**
   * Creates the boolean value.
   */
  public static BooleanValueExp create(boolean value)
  {
    return value ? TRUE : FALSE;
  }

  /**
   * Returns the underlying value as a string.
   */
  public String getString()
  {
    return String.valueOf(_value);
  }

  /**
   * Returns the underlying value as a boolean.
   */
  public boolean getBoolean()
  {
    return _value;
  }

  /**
   * Returns the underlying value as a long.
   */
  public long getLong()
  {
    return _value ? 1 : 0;
  }

  /**
   * Returns the underlying value as a double.
   */
  public double getDouble()
  {
    return _value ? 1 : 0;
  }

  /**
   * Returns the underlying string.
   */
  public String toString()
  {
    return String.valueOf(_value);
  }
}
