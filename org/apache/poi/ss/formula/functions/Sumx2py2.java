package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.functions.XYNumericFunction.Accumulator;

public final class Sumx2py2 extends XYNumericFunction {
   private static final Accumulator XSquaredPlusYSquaredAccumulator = new Accumulator() {
      public double accumulate(double x, double y) {
         return x * x + y * y;
      }
   };

   protected Accumulator createAccumulator() {
      return XSquaredPlusYSquaredAccumulator;
   }
}
