package org.apache.poi.sl.draw.geom;

public interface Formula {
   double OOXML_DEGREE = 60000.0;

   double evaluate(Context var1);
}
