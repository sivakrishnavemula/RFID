package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ValueEval;

public interface Function3Arg extends Function {
   ValueEval evaluate(int var1, int var2, ValueEval var3, ValueEval var4, ValueEval var5);
}
