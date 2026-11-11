package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.eval.ValueEval;

public interface ThreeDEval extends TwoDEval, SheetRange {
   ValueEval getValue(int var1, int var2, int var3);
}
