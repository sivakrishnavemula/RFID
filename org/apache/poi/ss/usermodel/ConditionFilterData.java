package org.apache.poi.ss.usermodel;

public interface ConditionFilterData {
   boolean getAboveAverage();

   boolean getBottom();

   boolean getEqualAverage();

   boolean getPercent();

   long getRank();

   int getStdDev();
}
