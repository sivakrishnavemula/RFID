package org.apache.poi.hssf.usermodel;

import java.util.List;
import org.apache.poi.ss.usermodel.ShapeContainer;

public interface HSSFShapeContainer extends ShapeContainer<HSSFShape> {
   List<HSSFShape> getChildren();

   void addShape(HSSFShape var1);

   void setCoordinates(int var1, int var2, int var3, int var4);

   void clear();

   int getX1();

   int getY1();

   int getX2();

   int getY2();

   boolean removeShape(HSSFShape var1);
}
