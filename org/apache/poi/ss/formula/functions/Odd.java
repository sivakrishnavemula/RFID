package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.functions.NumericFunction.OneArg;

public final class Odd extends OneArg {
   private static final long PARITY_MASK = -2L;

   protected double evaluate(double d) {
      if (d == 0.0) {
         return 1.0;
      } else {
         return d > 0.0 ? calcOdd(d) : -calcOdd(-d);
      }
   }

   private static long calcOdd(double d) {
      double dpm1 = d + 1.0;
      long x = (long)dpm1 & -2L;
      return Double.compare(x, dpm1) == 0 ? x - 1L : x + 1L;
   }
}
