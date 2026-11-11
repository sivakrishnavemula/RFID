package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartAxis {
   long getId();

   AxisPosition getPosition();

   void setPosition(AxisPosition var1);

   String getNumberFormat();

   void setNumberFormat(String var1);

   boolean isSetLogBase();

   void setLogBase(double var1);

   double getLogBase();

   boolean isSetMinimum();

   double getMinimum();

   void setMinimum(double var1);

   boolean isSetMaximum();

   double getMaximum();

   void setMaximum(double var1);

   AxisOrientation getOrientation();

   void setOrientation(AxisOrientation var1);

   void setCrosses(AxisCrosses var1);

   AxisCrosses getCrosses();

   void crossAxis(ChartAxis var1);

   boolean isVisible();

   void setVisible(boolean var1);

   AxisTickMark getMajorTickMark();

   void setMajorTickMark(AxisTickMark var1);

   AxisTickMark getMinorTickMark();

   void setMinorTickMark(AxisTickMark var1);

   boolean hasNumberFormat();
}
