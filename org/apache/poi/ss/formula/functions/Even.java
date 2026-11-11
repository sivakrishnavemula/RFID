package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.functions.NumericFunction.OneArg;

public final class Even extends OneArg {
   private static final long PARITY_MASK = -2L;

   protected double evaluate(double d) {
      if (d == 0.0) {
         return 0.0;
      }

      long result;
      if (d > 0.0) {
         result = calcEven(d);
      } else {
         result = -calcEven(-d);
      }

      return result;
   }

   private static long calcEven(double d) {
      long x = (long)d & -2L;
      return x == d ? x : x + 2L;
   }
}
