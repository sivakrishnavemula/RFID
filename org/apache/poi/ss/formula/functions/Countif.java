package org.apache.poi.ss.formula.functions;

import java.util.regex.Pattern;
import org.apache.poi.ss.formula.ThreeDEval;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.CountUtils.I_MatchPredicate;
import org.apache.poi.ss.usermodel.FormulaError;

public final class Countif extends Fixed2ArgFunction {
   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      I_MatchPredicate mp = createCriteriaPredicate(arg1, srcRowIndex, srcColumnIndex);
      if (mp == null) {
         return NumberEval.ZERO;
      }

      double result = this.countMatchingCellsInArea(arg0, mp);
      return new NumberEval(result);
   }

   private double countMatchingCellsInArea(ValueEval rangeArg, I_MatchPredicate criteriaPredicate) {
      if (rangeArg instanceof RefEval) {
         return CountUtils.countMatchingCellsInRef((RefEval)rangeArg, criteriaPredicate);
      } else if (rangeArg instanceof ThreeDEval) {
         return CountUtils.countMatchingCellsInArea((ThreeDEval)rangeArg, criteriaPredicate);
      } else {
         throw new IllegalArgumentException("Bad range arg type (" + rangeArg.getClass().getName() + ")");
      }
   }

   static I_MatchPredicate createCriteriaPredicate(ValueEval arg, int srcRowIndex, int srcColumnIndex) {
      ValueEval evaluatedCriteriaArg = evaluateCriteriaArg(arg, srcRowIndex, srcColumnIndex);
      if (evaluatedCriteriaArg instanceof NumberEval) {
         return new Countif.NumberMatcher(((NumberEval)evaluatedCriteriaArg).getNumberValue(), Countif.CmpOp.OP_NONE);
      } else if (evaluatedCriteriaArg instanceof BoolEval) {
         return new Countif.BooleanMatcher(((BoolEval)evaluatedCriteriaArg).getBooleanValue(), Countif.CmpOp.OP_NONE);
      } else if (evaluatedCriteriaArg instanceof StringEval) {
         return createGeneralMatchPredicate((StringEval)evaluatedCriteriaArg);
      } else if (evaluatedCriteriaArg instanceof ErrorEval) {
         return new Countif.ErrorMatcher(((ErrorEval)evaluatedCriteriaArg).getErrorCode(), Countif.CmpOp.OP_NONE);
      } else if (evaluatedCriteriaArg == BlankEval.instance) {
         return null;
      } else {
         throw new RuntimeException("Unexpected type for criteria (" + evaluatedCriteriaArg.getClass().getName() + ")");
      }
   }

   private static ValueEval evaluateCriteriaArg(ValueEval arg, int srcRowIndex, int srcColumnIndex) {
      try {
         return OperandResolver.getSingleValue(arg, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }
   }

   private static I_MatchPredicate createGeneralMatchPredicate(StringEval stringEval) {
      String value = stringEval.getStringValue();
      Countif.CmpOp operator = Countif.CmpOp.getOperator(value);
      value = value.substring(operator.getLength());
      Boolean booleanVal = parseBoolean(value);
      if (booleanVal != null) {
         return new Countif.BooleanMatcher(booleanVal, operator);
      }

      Double doubleVal = OperandResolver.parseDouble(value);
      if (doubleVal != null) {
         return new Countif.NumberMatcher(doubleVal, operator);
      }

      ErrorEval ee = parseError(value);
      return ee != null ? new Countif.ErrorMatcher(ee.getErrorCode(), operator) : new Countif.StringMatcher(value, operator);
   }

   private static ErrorEval parseError(String value) {
      if (value.length() < 4 || value.charAt(0) != '#') {
         return null;
      } else if (value.equals("#NULL!")) {
         return ErrorEval.NULL_INTERSECTION;
      } else if (value.equals("#DIV/0!")) {
         return ErrorEval.DIV_ZERO;
      } else if (value.equals("#VALUE!")) {
         return ErrorEval.VALUE_INVALID;
      } else if (value.equals("#REF!")) {
         return ErrorEval.REF_INVALID;
      } else if (value.equals("#NAME?")) {
         return ErrorEval.NAME_INVALID;
      } else if (value.equals("#NUM!")) {
         return ErrorEval.NUM_ERROR;
      } else {
         return value.equals("#N/A") ? ErrorEval.NA : null;
      }
   }

   static Boolean parseBoolean(String strRep) {
      if (strRep.length() < 1) {
         return null;
      }

      switch (strRep.charAt(0)) {
         case 'F':
         case 'f':
            if ("FALSE".equalsIgnoreCase(strRep)) {
               return Boolean.FALSE;
            }
            break;
         case 'T':
         case 't':
            if ("TRUE".equalsIgnoreCase(strRep)) {
               return Boolean.TRUE;
            }
      }

      return null;
   }

   private static final class BooleanMatcher extends Countif.MatcherBase {
      private final int _value;

      public BooleanMatcher(boolean value, Countif.CmpOp operator) {
         super(operator);
         this._value = boolToInt(value);
      }

      @Override
      protected String getValueText() {
         return this._value == 1 ? "TRUE" : "FALSE";
      }

      private static int boolToInt(boolean value) {
         return value ? 1 : 0;
      }

      public boolean matches(ValueEval x) {
         if (x instanceof StringEval) {
            return false;
         }

         if (x instanceof BoolEval) {
            BoolEval be = (BoolEval)x;
            int testValue = boolToInt(be.getBooleanValue());
            return this.evaluate(testValue - this._value);
         }

         if (x instanceof BlankEval) {
            switch (this.getCode()) {
               case 2:
                  return true;
               default:
                  return false;
            }
         } else if (x instanceof NumberEval) {
            switch (this.getCode()) {
               case 2:
                  return true;
               default:
                  return false;
            }
         } else {
            return false;
         }
      }
   }

   private static final class CmpOp {
      public static final int NONE = 0;
      public static final int EQ = 1;
      public static final int NE = 2;
      public static final int LE = 3;
      public static final int LT = 4;
      public static final int GT = 5;
      public static final int GE = 6;
      public static final Countif.CmpOp OP_NONE = op("", 0);
      public static final Countif.CmpOp OP_EQ = op("=", 1);
      public static final Countif.CmpOp OP_NE = op("<>", 2);
      public static final Countif.CmpOp OP_LE = op("<=", 3);
      public static final Countif.CmpOp OP_LT = op("<", 4);
      public static final Countif.CmpOp OP_GT = op(">", 5);
      public static final Countif.CmpOp OP_GE = op(">=", 6);
      private final String _representation;
      private final int _code;

      private static Countif.CmpOp op(String rep, int code) {
         return new Countif.CmpOp(rep, code);
      }

      private CmpOp(String representation, int code) {
         this._representation = representation;
         this._code = code;
      }

      public int getLength() {
         return this._representation.length();
      }

      public int getCode() {
         return this._code;
      }

      public static Countif.CmpOp getOperator(String value) {
         int len = value.length();
         if (len < 1) {
            return OP_NONE;
         }

         char firstChar = value.charAt(0);
         switch (firstChar) {
            case '<':
               if (len > 1) {
                  switch (value.charAt(1)) {
                     case '=':
                        return OP_LE;
                     case '>':
                        return OP_NE;
                  }
               }

               return OP_LT;
            case '=':
               return OP_EQ;
            case '>':
               if (len > 1) {
                  switch (value.charAt(1)) {
                     case '=':
                        return OP_GE;
                  }
               }

               return OP_GT;
            default:
               return OP_NONE;
         }
      }

      public boolean evaluate(boolean cmpResult) {
         switch (this._code) {
            case 0:
            case 1:
               return cmpResult;
            case 2:
               return !cmpResult;
            default:
               throw new RuntimeException("Cannot call boolean evaluate on non-equality operator '" + this._representation + "'");
         }
      }

      public boolean evaluate(int cmpResult) {
         switch (this._code) {
            case 0:
            case 1:
               return cmpResult == 0;
            case 2:
               return cmpResult != 0;
            case 3:
               return cmpResult <= 0;
            case 4:
               return cmpResult < 0;
            case 5:
               return cmpResult > 0;
            case 6:
               return cmpResult >= 0;
            default:
               throw new RuntimeException("Cannot call boolean evaluate on non-equality operator '" + this._representation + "'");
         }
      }

      public String toString() {
         StringBuffer sb = new StringBuffer(64);
         sb.append(this.getClass().getName());
         sb.append(" [").append(this._representation).append("]");
         return sb.toString();
      }

      public String getRepresentation() {
         return this._representation;
      }
   }

   public static final class ErrorMatcher extends Countif.MatcherBase {
      private final int _value;

      public ErrorMatcher(int errorCode, Countif.CmpOp operator) {
         super(operator);
         this._value = errorCode;
      }

      @Override
      protected String getValueText() {
         return FormulaError.forInt(this._value).getString();
      }

      public boolean matches(ValueEval x) {
         if (x instanceof ErrorEval) {
            int testValue = ((ErrorEval)x).getErrorCode();
            return this.evaluate(testValue - this._value);
         } else {
            return false;
         }
      }

      public int getValue() {
         return this._value;
      }
   }

   private abstract static class MatcherBase implements I_MatchPredicate {
      private final Countif.CmpOp _operator;

      MatcherBase(Countif.CmpOp operator) {
         this._operator = operator;
      }

      protected final int getCode() {
         return this._operator.getCode();
      }

      protected final boolean evaluate(int cmpResult) {
         return this._operator.evaluate(cmpResult);
      }

      protected final boolean evaluate(boolean cmpResult) {
         return this._operator.evaluate(cmpResult);
      }

      public final String toString() {
         StringBuffer sb = new StringBuffer(64);
         sb.append(this.getClass().getName()).append(" [");
         sb.append(this._operator.getRepresentation());
         sb.append(this.getValueText());
         sb.append("]");
         return sb.toString();
      }

      protected abstract String getValueText();
   }

   private static final class NumberMatcher extends Countif.MatcherBase {
      private final double _value;

      public NumberMatcher(double value, Countif.CmpOp operator) {
         super(operator);
         this._value = value;
      }

      @Override
      protected String getValueText() {
         return String.valueOf(this._value);
      }

      public boolean matches(ValueEval x) {
         if (x instanceof StringEval) {
            switch (this.getCode()) {
               case 0:
               case 1:
                  StringEval se = (StringEval)x;
                  Double val = OperandResolver.parseDouble(se.getStringValue());
                  if (val == null) {
                     return false;
                  }

                  return this._value == val;
               case 2:
                  return true;
               default:
                  return false;
            }
         } else {
            if (x instanceof NumberEval) {
               NumberEval ne = (NumberEval)x;
               double testValue = ne.getNumberValue();
               return this.evaluate(Double.compare(testValue, this._value));
            }

            if (x instanceof BlankEval) {
               switch (this.getCode()) {
                  case 2:
                     return true;
                  default:
                     return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   public static final class StringMatcher extends Countif.MatcherBase {
      private final String _value;
      private final Pattern _pattern;

      public StringMatcher(String value, Countif.CmpOp operator) {
         super(operator);
         this._value = value;
         switch (operator.getCode()) {
            case 0:
            case 1:
            case 2:
               this._pattern = getWildCardPattern(value);
               break;
            default:
               this._pattern = null;
         }
      }

      @Override
      protected String getValueText() {
         return this._pattern == null ? this._value : this._pattern.pattern();
      }

      public boolean matches(ValueEval x) {
         if (x instanceof BlankEval) {
            switch (this.getCode()) {
               case 0:
               case 1:
                  return this._value.length() == 0;
               case 2:
                  return this._value.length() != 0;
               default:
                  return false;
            }
         } else {
            if (!(x instanceof StringEval)) {
               return false;
            }

            String testedValue = ((StringEval)x).getStringValue();
            if (testedValue.length() < 1 && this._value.length() < 1) {
               switch (this.getCode()) {
                  case 0:
                     return true;
                  case 1:
                     return false;
                  case 2:
                     return true;
                  default:
                     return false;
               }
            } else {
               return this._pattern != null
                  ? this.evaluate(this._pattern.matcher(testedValue).matches())
                  : this.evaluate(testedValue.compareToIgnoreCase(this._value));
            }
         }
      }

      public static Pattern getWildCardPattern(String value) {
         int len = value.length();
         StringBuffer sb = new StringBuffer(len);
         boolean hasWildCard = false;

         for (int i = 0; i < len; i++) {
            char ch = value.charAt(i);
            switch (ch) {
               case '$':
               case '(':
               case ')':
               case '.':
               case '[':
               case ']':
               case '^':
                  sb.append("\\").append(ch);
                  break;
               case '*':
                  hasWildCard = true;
                  sb.append(".*");
                  break;
               case '?':
                  hasWildCard = true;
                  sb.append('.');
                  break;
               case '~':
                  if (i + 1 < len) {
                     ch = value.charAt(i + 1);
                     switch (ch) {
                        case '*':
                        case '?':
                           hasWildCard = true;
                           sb.append('[').append(ch).append(']');
                           i++;
                           continue;
                     }
                  }

                  sb.append('~');
                  break;
               default:
                  sb.append(ch);
            }
         }

         return hasWildCard ? Pattern.compile(sb.toString(), 2) : null;
      }
   }
}
