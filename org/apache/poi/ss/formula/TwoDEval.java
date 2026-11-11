package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.eval.ValueEval;

public interface TwoDEval extends ValueEval {
   ValueEval getValue(int var1, int var2);

   int getWidth();

   int getHeight();

   boolean isRow();

   boolean isColumn();

   TwoDEval getRow(int var1);

   TwoDEval getColumn(int var1);

   boolean isSubTotal(int var1, int var2);
}
