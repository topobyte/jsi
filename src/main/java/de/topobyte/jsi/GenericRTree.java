//  This file is part of JSI.
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package de.topobyte.jsi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;
import com.slimjars.dist.gnu.trove.map.TIntObjectMap;
import com.slimjars.dist.gnu.trove.map.TObjectIntMap;
import com.slimjars.dist.gnu.trove.map.hash.TIntObjectHashMap;
import com.slimjars.dist.gnu.trove.map.hash.TObjectIntHashMap;
import com.slimjars.dist.gnu.trove.procedure.TIntProcedure;
import com.slimjars.dist.gnu.trove.procedure.TObjectProcedure;

/**
 * An implementation of the GenericSpatialIndex that internally uses the
 * com.infomatiq.jsi.rtree.RTree.
 * 
 * @author Sebastian Kuerten (sebastian@topobyte.de)
 * 
 * @param <T>
 *            the type of elements stored in this RTree.
 */
public class GenericRTree<T> implements GenericSpatialIndex<T>, Externalizable
{

  RTree rtree;
  int indexer = 0;
  TIntObjectMap<T> idToThing;
  TObjectIntMap<T> thingToId;
  Map<T, Rectangle> thingToRect;

  /**
   * This constructor creates a GenericRTree. The specified minimal and
   * maximal number of childs per node is internally used for the implementing
   * RTree.
   * 
   * @param minNodes
   *            the minimum number of childs per node.
   * @param maxNodes
   *            the maximum number of childs per node.
   */
  public GenericRTree(int minNodes, int maxNodes)
  {
    rtree = new RTree(minNodes, maxNodes);
    idToThing = new TIntObjectHashMap<>();
    thingToId = new TObjectIntHashMap<>();
    thingToRect = new HashMap<>();
  }

  /**
   * default constructor that initializes the underlying tree to have at least
   * 1 and at most 10 childs per node.
   */
  public GenericRTree()
  {
    this(1, 10);
  }

  @Override
  public void add(Rectangle r, T thing)
  {
    int index = indexer++;
    add(r, thing, index);
  }

  private void add(Rectangle r, T thing, int index)
  {
    rtree.add(r, index);
    idToThing.put(index, thing);
    thingToId.put(thing, index);
    thingToRect.put(thing, r);
  }

  @Override
  public boolean delete(Rectangle r, T thing)
  {
    int id = thingToId.get(thing);
    boolean success = rtree.delete(r, id);
    if (success) {
      thingToId.remove(thing);
      idToThing.remove(id);
      thingToRect.remove(thing);
    }
    return success;
  }

  @Override
  public void contains(Rectangle r, final TObjectProcedure<T> procedure)
  {
    rtree.contains(r, new TIntProcedure() {

      @Override
      public boolean execute(int id)
      {
        T thing = idToThing.get(id);
        boolean ret = procedure.execute(thing);
        return ret;
      }
    });
  }

  @Override
  public Set<T> contains(Rectangle r)
  {
    final Set<T> results = new HashSet<>();
    contains(r, new TObjectProcedure<T>() {

      @Override
      public boolean execute(T thing)
      {
        results.add(thing);
        return true;
      }
    });
    return results;
  }

  @Override
  public void intersects(Rectangle r, final TObjectProcedure<T> procedure)
  {
    rtree.intersects(r, new TIntProcedure() {

      @Override
      public boolean execute(int id)
      {
        T thing = idToThing.get(id);
        boolean ret = procedure.execute(thing);
        return ret;
      }
    });
  }

  @Override
  public Set<T> intersects(Rectangle r)
  {
    final Set<T> results = new HashSet<>();
    intersects(r, new TObjectProcedure<T>() {

      @Override
      public boolean execute(T thing)
      {
        results.add(thing);
        return true;
      }
    });
    return results;
  }

  @Override
  public List<T> intersectionsAsList(Rectangle r)
  {
    final List<T> results = new ArrayList<>();
    intersects(r, new TObjectProcedure<T>() {

      @Override
      public boolean execute(T thing)
      {
        results.add(thing);
        return true;
      }
    });
    return results;
  }

  @Override
  public void nearest(Point p, final TObjectProcedure<T> procedure,
      float distance)
  {
    rtree.nearest(p, new TIntProcedure() {

      @Override
      public boolean execute(int id)
      {
        T thing = idToThing.get(id);
        boolean ret = procedure.execute(thing);
        return ret;
      }
    }, distance);
  }

  @Override
  public Set<T> nearest(Point p, float distance)
  {
    final Set<T> results = new HashSet<>();
    nearest(p, new TObjectProcedure<T>() {

      @Override
      public boolean execute(T thing)
      {
        results.add(thing);
        return true;
      }
    }, distance);
    return results;
  }

  @Override
  public int size()
  {
    return rtree.size();
  }

  /**
   * @return the bounds of the contained elements.
   */
  public Rectangle getBounds()
  {
    return rtree.getBounds();
  }

  @Override
  public void writeExternal(ObjectOutput oo) throws IOException
  {
    oo.writeInt(indexer);
    oo.writeInt(idToThing.size());
    int[] keys = idToThing.keys();
    for (int i : keys) {
      T thing = idToThing.get(i);
      Rectangle rect = thingToRect.get(thing);
      oo.writeInt(i);
      oo.writeObject(thing);
      oo.writeFloat(rect.minX);
      oo.writeFloat(rect.maxX);
      oo.writeFloat(rect.minY);
      oo.writeFloat(rect.maxY);
    }
  }

  @Override
  public void readExternal(ObjectInput oi) throws IOException,
      ClassNotFoundException
  {
    indexer = oi.readInt();
    int size = oi.readInt();
    for (int i = 0; i < size; i++) {
      int id = oi.readInt();
      // unchecked cast not avoidable
      T thing = (T) oi.readObject();
      float minX = oi.readFloat();
      float maxX = oi.readFloat();
      float minY = oi.readFloat();
      float maxY = oi.readFloat();
      Rectangle r = new Rectangle(minX, minY, maxX, maxY);
      add(r, thing, id);
    }
  }

}
