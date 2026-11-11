package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class DMax implements IDStarAlgorithm {
   private ValueEval maximumValue;

   public boolean processMatch(ValueEval eval) {
      if (eval instanceof NumericValueEval) {
         if (this.maximumValue == null) {
            this.maximumValue = eval;
         } else {
            double currentValue = ((NumericValueEval)eval).getNumberValue();
            double oldValue = ((NumericValueEval)this.maximumValue).getNumberValue();
            if (currentValue > oldValue) {
               this.maximumValue = eval;
            }
         }
      }

      return true;
   }

   public ValueEval getResult() {
      return (ValueEval)(this.maximumValue == null ? NumberEval.ZERO : this.maximumValue);
   }
}
