package org.apache.poi.poifs.property;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DirectoryProperty extends Property implements Parent, Iterable<Property> {
   private List<Property> _children = new ArrayList();
   private Set<String> _children_names = new HashSet();

   public DirectoryProperty(String name) {
      this.setName(name);
      this.setSize(0);
      this.setPropertyType((byte)1);
      this.setStartBlock(0);
      this.setNodeColor((byte)1);
   }

   protected DirectoryProperty(int index, byte[] array, int offset) {
      super(index, array, offset);
   }

   public boolean changeName(Property property, String newName) {
      String oldName = property.getName();
      property.setName(newName);
      String cleanNewName = property.getName();
      boolean result;
      if (this._children_names.contains(cleanNewName)) {
         property.setName(oldName);
         result = false;
      } else {
         this._children_names.add(cleanNewName);
         this._children_names.remove(oldName);
         result = true;
      }

      return result;
   }

   public boolean deleteChild(Property property) {
      boolean result = this._children.remove(property);
      if (result) {
         this._children_names.remove(property.getName());
      }

      return result;
   }

   public boolean isDirectory() {
      return true;
   }

   protected void preWrite() {
      if (this._children.size() > 0) {
         Property[] children = (Property[])this._children.toArray(new Property[0]);
         Arrays.sort(children, new DirectoryProperty.PropertyComparator());
         int midpoint = children.length / 2;
         this.setChildProperty(children[midpoint].getIndex());
         children[0].setPreviousChild(null);
         children[0].setNextChild(null);

         for (int j = 1; j < midpoint; j++) {
            children[j].setPreviousChild(children[j - 1]);
            children[j].setNextChild(null);
         }

         if (midpoint != 0) {
            children[midpoint].setPreviousChild(children[midpoint - 1]);
         }

         if (midpoint != children.length - 1) {
            children[midpoint].setNextChild(children[midpoint + 1]);

            for (int j = midpoint + 1; j < children.length - 1; j++) {
               children[j].setPreviousChild(null);
               children[j].setNextChild(children[j + 1]);
            }

            children[children.length - 1].setPreviousChild(null);
            children[children.length - 1].setNextChild(null);
         } else {
            children[midpoint].setNextChild(null);
         }
      }
   }

   public Iterator<Property> getChildren() {
      return this._children.iterator();
   }

   public Iterator<Property> iterator() {
      return this.getChildren();
   }

   public void addChild(Property property) throws IOException {
      String name = property.getName();
      if (this._children_names.contains(name)) {
         throw new IOException("Duplicate name \"" + name + "\"");
      }

      this._children_names.add(name);
      this._children.add(property);
   }

   public static class PropertyComparator implements Comparator<Property>, Serializable {
      public int compare(Property o1, Property o2) {
         String VBA_PROJECT = "_VBA_PROJECT";
         String name1 = o1.getName();
         String name2 = o2.getName();
         int result = name1.length() - name2.length();
         if (result == 0) {
            if (name1.compareTo(VBA_PROJECT) == 0) {
               result = 1;
            } else if (name2.compareTo(VBA_PROJECT) == 0) {
               result = -1;
            } else if (name1.startsWith("__") && name2.startsWith("__")) {
               result = name1.compareToIgnoreCase(name2);
            } else if (name1.startsWith("__")) {
               result = 1;
            } else if (name2.startsWith("__")) {
               result = -1;
            } else {
               result = name1.compareToIgnoreCase(name2);
            }
         }

         return result;
      }
   }
}
