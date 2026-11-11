package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.functions.XYNumericFunction.Accumulator;

public final class Sumxmy2 extends XYNumericFunction {
   private static final Accumulator XMinusYSquaredAccumulator = new Accumulator() {
      public double accumulate(double x, double y) {
         double xmy = x - y;
         return xmy * xmy;
      }
   };

   protected Accumulator createAccumulator() {
      return XMinusYSquaredAccumulator;
   }
}
