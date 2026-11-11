package org.apache.poi.ss.usermodel;

import java.awt.Dimension;

public interface Picture extends Shape {
   void resize();

   void resize(double var1);

   void resize(double var1, double var3);

   ClientAnchor getPreferredSize();

   ClientAnchor getPreferredSize(double var1, double var3);

   Dimension getImageDimension();

   PictureData getPictureData();

   ClientAnchor getClientAnchor();

   Sheet getSheet();
}
