package org.apache.poi.poifs.property;

import java.io.IOException;
import java.util.Iterator;

public interface Parent extends Child, Iterable<Property> {
   Iterator<Property> getChildren();

   void addChild(Property var1) throws IOException;

   void setPreviousChild(Child var1);

   void setNextChild(Child var1);
}
