package org.apache.poi.ss.usermodel;

public interface Shape {
   String getShapeName();

   Shape getParent();

   ChildAnchor getAnchor();

   boolean isNoFill();

   void setNoFill(boolean var1);

   void setFillColor(int var1, int var2, int var3);

   void setLineStyleColor(int var1, int var2, int var3);
}
