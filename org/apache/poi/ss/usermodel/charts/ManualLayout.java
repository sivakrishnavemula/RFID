package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ManualLayout {
   void setTarget(LayoutTarget var1);

   LayoutTarget getTarget();

   void setXMode(LayoutMode var1);

   LayoutMode getXMode();

   void setYMode(LayoutMode var1);

   LayoutMode getYMode();

   double getX();

   void setX(double var1);

   double getY();

   void setY(double var1);

   void setWidthMode(LayoutMode var1);

   LayoutMode getWidthMode();

   void setHeightMode(LayoutMode var1);

   LayoutMode getHeightMode();

   void setWidthRatio(double var1);

   double getWidthRatio();

   void setHeightRatio(double var1);

   double getHeightRatio();
}
