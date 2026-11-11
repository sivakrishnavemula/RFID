package org.apache.poi.poifs.property;

public interface Child {
   Child getNextChild();

   Child getPreviousChild();

   void setNextChild(Child var1);

   void setPreviousChild(Child var1);
}
