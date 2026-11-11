package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ValueEval;

public interface IDStarAlgorithm {
   boolean processMatch(ValueEval var1);

   ValueEval getResult();
}
