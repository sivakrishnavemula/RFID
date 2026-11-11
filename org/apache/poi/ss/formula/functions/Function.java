package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ValueEval;

public interface Function {
   ValueEval evaluate(ValueEval[] var1, int var2, int var3);
}
