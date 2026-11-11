package org.apache.poi.ss.formula.functions;

public abstract class MinaMaxa extends MultiOperandNumericFunction {
   public static final Function MAXA = new MinaMaxa() {
      protected double evaluate(double[] values) {
         return values.length > 0 ? MathX.max(values) : 0.0;
      }
   };
   public static final Function MINA = new MinaMaxa() {
      protected double evaluate(double[] values) {
         return values.length > 0 ? MathX.min(values) : 0.0;
      }
   };

   protected MinaMaxa() {
      super(true, true);
   }
}
