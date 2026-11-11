package org.apache.poi.ss.formula.eval;

import org.apache.poi.ss.formula.CacheAreaEval;
import org.apache.poi.ss.formula.functions.ArrayFunction;
import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.util.NumberComparer;

public abstract class RelationalOperationEval extends Fixed2ArgFunction implements ArrayFunction {
   public static final Function EqualEval = new RelationalOperationEval() {
      @Override
      protected boolean convertComparisonResult(int cmpResult) {
         return cmpResult == 0;
      }
   };
   public static final Function GreaterEqualEval = new RelationalOperationEval() {
      @Override
      protected boolean convertComparisonResult(int cmpResult) {
         return cmpResult >= 0;
      }
   };
   public static final Function GreaterThanEval = new RelationalOperationEval() {
      @Override
      protected boolean convertComparisonResult(int cmpResult) {
         return cmpResult > 0;
      }
   };
   public static final Function LessEqualEval = new RelationalOperationEval() {
      @Override
      protected boolean convertComparisonResult(int cmpResult) {
         return cmpResult <= 0;
      }
   };
   public static final Function LessThanEval = new RelationalOperationEval() {
      @Override
      protected boolean convertComparisonResult(int cmpResult) {
         return cmpResult < 0;
      }
   };
   public static final Function NotEqualEval = new RelationalOperationEval() {
      @Override
      protected boolean convertComparisonResult(int cmpResult) {
         return cmpResult != 0;
      }
   };

   protected abstract boolean convertComparisonResult(int var1);

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      ValueEval vA;
      ValueEval vB;
      try {
         vA = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
         vB = OperandResolver.getSingleValue(arg1, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      int cmpResult = doCompare(vA, vB);
      boolean result = this.convertComparisonResult(cmpResult);
      return BoolEval.valueOf(result);
   }

   public ValueEval evaluateArray(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      ValueEval arg0 = args[0];
      ValueEval arg1 = args[1];
      int a1FirstCol = 0;
      int a1FirstRow = 0;
      int w1;
      int h1;
      if (arg0 instanceof AreaEval) {
         AreaEval ae = (AreaEval)arg0;
         w1 = ae.getWidth();
         h1 = ae.getHeight();
         a1FirstCol = ae.getFirstColumn();
         a1FirstRow = ae.getFirstRow();
      } else if (arg0 instanceof RefEval) {
         RefEval ref = (RefEval)arg0;
         w1 = 1;
         h1 = 1;
         a1FirstCol = ref.getColumn();
         a1FirstRow = ref.getRow();
      } else {
         w1 = 1;
         h1 = 1;
      }

      int a2FirstCol = 0;
      int a2FirstRow = 0;
      int w2;
      int h2;
      if (arg1 instanceof AreaEval) {
         AreaEval ae = (AreaEval)arg1;
         w2 = ae.getWidth();
         h2 = ae.getHeight();
         a2FirstCol = ae.getFirstColumn();
         a2FirstRow = ae.getFirstRow();
      } else if (arg1 instanceof RefEval) {
         RefEval ref = (RefEval)arg1;
         w2 = 1;
         h2 = 1;
         a2FirstCol = ref.getColumn();
         a2FirstRow = ref.getRow();
      } else {
         w2 = 1;
         h2 = 1;
      }

      int width = Math.max(w1, w2);
      int height = Math.max(h1, h2);
      ValueEval[] vals = new ValueEval[height * width];
      int idx = 0;

      for (int i = 0; i < height; i++) {
         for (int j = 0; j < width; j++) {
            ValueEval vA;
            try {
               vA = OperandResolver.getSingleValue(arg0, a1FirstRow + i, a1FirstCol + j);
            } catch (EvaluationException e) {
               vA = e.getErrorEval();
            }

            ValueEval vB;
            try {
               vB = OperandResolver.getSingleValue(arg1, a2FirstRow + i, a2FirstCol + j);
            } catch (EvaluationException e) {
               vB = e.getErrorEval();
            }

            if (vA instanceof ErrorEval) {
               vals[idx++] = vA;
            } else if (vB instanceof ErrorEval) {
               vals[idx++] = vB;
            } else {
               int cmpResult = doCompare(vA, vB);
               boolean result = this.convertComparisonResult(cmpResult);
               vals[idx++] = BoolEval.valueOf(result);
            }
         }
      }

      return (ValueEval)(vals.length == 1
         ? vals[0]
         : new CacheAreaEval(srcRowIndex, srcColumnIndex, srcRowIndex + height - 1, srcColumnIndex + width - 1, vals));
   }

   private static int doCompare(ValueEval va, ValueEval vb) {
      if (va == BlankEval.instance) {
         return compareBlank(vb);
      }

      if (vb == BlankEval.instance) {
         return -compareBlank(va);
      }

      if (va instanceof BoolEval) {
         if (vb instanceof BoolEval) {
            BoolEval bA = (BoolEval)va;
            BoolEval bB = (BoolEval)vb;
            if (bA.getBooleanValue() == bB.getBooleanValue()) {
               return 0;
            } else {
               return bA.getBooleanValue() ? 1 : -1;
            }
         } else {
            return 1;
         }
      } else {
         if (vb instanceof BoolEval) {
            return -1;
         }

         if (va instanceof StringEval) {
            if (vb instanceof StringEval) {
               StringEval sA = (StringEval)va;
               StringEval sB = (StringEval)vb;
               return sA.getStringValue().compareToIgnoreCase(sB.getStringValue());
            } else {
               return 1;
            }
         } else if (vb instanceof StringEval) {
            return -1;
         } else if (va instanceof NumberEval && vb instanceof NumberEval) {
            NumberEval nA = (NumberEval)va;
            NumberEval nB = (NumberEval)vb;
            return NumberComparer.compare(nA.getNumberValue(), nB.getNumberValue());
         } else {
            throw new IllegalArgumentException("Bad operand types (" + va.getClass().getName() + "), (" + vb.getClass().getName() + ")");
         }
      }
   }

   private static int compareBlank(ValueEval v) {
      if (v == BlankEval.instance) {
         return 0;
      } else if (v instanceof BoolEval) {
         BoolEval boolEval = (BoolEval)v;
         return boolEval.getBooleanValue() ? -1 : 0;
      } else if (v instanceof NumberEval) {
         NumberEval ne = (NumberEval)v;
         return NumberComparer.compare(0.0, ne.getNumberValue());
      } else if (v instanceof StringEval) {
         StringEval se = (StringEval)v;
         return se.getStringValue().length() < 1 ? 0 : -1;
      } else {
         throw new IllegalArgumentException("bad value class (" + v.getClass().getName() + ")");
      }
   }
}
