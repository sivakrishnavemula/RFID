package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ValueEval;

public interface ArrayFunction {
   ValueEval evaluateArray(ValueEval[] var1, int var2, int var3);
}
