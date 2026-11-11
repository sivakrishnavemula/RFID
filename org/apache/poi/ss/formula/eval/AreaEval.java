package org.apache.poi.ss.formula.eval;

import org.apache.poi.ss.formula.ThreeDEval;
import org.apache.poi.ss.formula.TwoDEval;

public interface AreaEval extends TwoDEval, ThreeDEval {
   int getFirstRow();

   int getLastRow();

   int getFirstColumn();

   int getLastColumn();

   ValueEval getAbsoluteValue(int var1, int var2);

   boolean contains(int var1, int var2);

   boolean containsColumn(int var1);

   boolean containsRow(int var1);

   int getWidth();

   int getHeight();

   ValueEval getRelativeValue(int var1, int var2);

   AreaEval offset(int var1, int var2, int var3, int var4);
}
