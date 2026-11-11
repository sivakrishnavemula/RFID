package org.apache.poi.poifs.dev;

import java.util.Iterator;

public interface POIFSViewable {
   Object[] getViewableArray();

   Iterator<Object> getViewableIterator();

   boolean preferArray();

   String getShortDescription();
}
