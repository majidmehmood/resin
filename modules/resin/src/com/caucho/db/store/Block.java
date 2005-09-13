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

package com.caucho.db.store;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import com.caucho.util.L10N;
import com.caucho.util.ClockCacheItem;
import com.caucho.util.CacheListener;
import com.caucho.util.FreeList;

import com.caucho.vfs.TempBuffer;

import com.caucho.log.Log;

import com.caucho.db.table.Table;

/**
 * Represents a versioned row
 */
abstract public class Block implements ClockCacheItem, CacheListener {
  private static final Logger log = Log.open(Block.class);
  private static final L10N L = new L10N(Block.class);

  protected static final FreeList<byte[]> _freeBuffers =
    new FreeList<byte[]>(4);

  private Store _store;
  private long _blockId;

  private int _useCount;

  private boolean _isFlushDirtyOnCommit;
  private boolean _isValid;
  
  private int _dirtyMin = Store.BLOCK_SIZE;
  private int _dirtyMax;

  Block(Store store, long blockId)
  {
    if (store.getId() != (blockId & Store.BLOCK_INDEX_MASK)) {
      throw new IllegalArgumentException(L.l("block {0} must match store {1}.",
					     blockId & Store.BLOCK_INDEX_MASK,
					     store));
    }
    else if (store.getId() <= 0) {
      throw new IllegalArgumentException(L.l("invalid store {0}.", store));
    }
    
    _store = store;
    _blockId = blockId;

    _isFlushDirtyOnCommit = _store.isFlushDirtyBlocksOnCommit();

    if (log.isLoggable(Level.FINER))
      log.finer(this + " create");
  }

  /**
   * Returns true if the block should be flushed on a commit.
   */
  public boolean isFlushDirtyOnCommit()
  {
    return _isFlushDirtyOnCommit;
  }

  /**
   * True if the block should be flushed on a commit.
   */
  public void setFlushDirtyOnCommit(boolean isFlush)
  {
    _isFlushDirtyOnCommit = isFlush;
  }

  /**
   * Allocates the block for a query.
   */
  boolean allocate()
  {
    synchronized (this) {
      if (getBuffer() == null)
	return false;
      
      _useCount++;
      
      if (log.isLoggable(Level.FINEST))
	log.finest(this + " allocate");

      if (_useCount > 32 && log.isLoggable(Level.FINE)) {
	log.fine("using " + this + " " + _useCount + " times");
      }
    }

    return true;
  }

  /**
   * Returns the block's table.
   */
  Store getStore()
  {
    return _store;
  }

  /**
   * Returns the block's id.
   */
  public long getBlockId()
  {
    return _blockId;
  }
  
  /**
   * Returns the block's buffer.
   */
  abstract public byte []getBuffer();

  /**
   * Reads into the block.
   */
  public void read()
    throws IOException
  {
    synchronized (this) {
      if (! _isValid) {
	if (log.isLoggable(Level.FINER))
	  log.finer("read db-block " + this);
      
	_store.readBlock(_blockId & Store.BLOCK_MASK,
			 getBuffer(), 0, Store.BLOCK_SIZE);
	_isValid = true;

	_dirtyMin = Store.BLOCK_SIZE;
	_dirtyMax = 0;
      }
    }
  }

  /**
   * Handle any database writes necessary at commit time.  If
   * isFlushDirtyOnCommit() is true, this will write the data to
   * the backing file.
   */
  public void commit()
    throws IOException
  {
    if (! _isFlushDirtyOnCommit)
      return;
    else
      write();
  }

  /**
   * Forces a write of the data (should be private?)
   */
  public void write()
    throws IOException
  {
    synchronized (this) {
      int dirtyMin = _dirtyMin;
      _dirtyMin = Store.BLOCK_SIZE;

      int dirtyMax = _dirtyMax;
      _dirtyMax = 0;

      if (dirtyMin < dirtyMax) {
	if (log.isLoggable(Level.FINER))
	  log.finer("write db-block " + this + " [" + dirtyMin + ", " + dirtyMax + "]");

	_store.writeBlock((_blockId & Store.BLOCK_MASK) + dirtyMin,
			  getBuffer(), dirtyMin, dirtyMax - dirtyMin);
      }
      else {
	if (log.isLoggable(Level.FINER))
	  log.finer("not-dirty db-block " + this);
      }
      
      _isValid = true;
    }
  }

  /**
   * Marks the block's data as invalid.
   */
  public void invalidate()
  {
    if (_dirtyMin < _dirtyMax)
      throw new IllegalStateException();
    
    _isValid = false;
    _dirtyMin = Store.BLOCK_SIZE;
    _dirtyMax = 0;
  }

  /**
   * Marks the data as valid.
   */
  void validate()
  {
    _isValid = true;
  }

  /**
   * Marks the block's data as dirty
   */
  public void setDirty(int min, int max)
  {
    _isValid = true;

    if (min < _dirtyMin)
      _dirtyMin = min;
    
    if (_dirtyMax < max)
      _dirtyMax = max;
  }

  /**
   * Returns true if the block needs writing
   */
  public boolean isDirty()
  {
    return _dirtyMin < _dirtyMax;
  }

  /**
   * Return true if this is a free block.
   */
  public boolean isFree()
  {
    return _useCount == 0;
  }

  /**
   * Frees a block from a query.
   */
  public void free()
  {
    synchronized (this) {
      if (log.isLoggable(Level.FINEST))
	log.finest(this + " free");

      _useCount--;
      
      if (_useCount < 0) {
	_useCount = 0;
	
	log.warning("db-block illegal free " + this);
	throw new IllegalStateException();
      }
    }
  }

  /**
   * Returns true if the block is currently in use, i.e. it should not
   * be removed from the cache.
   */
  public boolean isUsed()
  {
    return _useCount > 0;
  }

  /**
   * Called by the clock cache to mark the item as currently in use.
   */
  public void setUsed()
  {
  }
  
  /**
   * Called by the clock cache to mark the item as allowed to be removed.
   */
  public void clearUsed()
  {
  }

  /**
   * Called when the block is removed from the cache.
   */
  public void removeEvent()
  {
    close();
  }
  
  /**
   * Called when the block is removed from the cache.
   */
  public void close()
  {
    synchronized (this) {
      if (_dirtyMin < _dirtyMax) {
	try {
	  write();
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }

      freeImpl();

      if (log.isLoggable(Level.FINER))
	log.finer("db-block remove " + this);
    }
  }

  /**
   * Frees any resources.
   */
  protected void freeImpl()
  {
  }

  public String toString()
  {
    return "Block[" + _store + "," + _blockId / Store.BLOCK_SIZE + "]";
  }
}
