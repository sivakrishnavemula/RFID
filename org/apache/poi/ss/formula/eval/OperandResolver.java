package org.apache.poi.ss.formula.eval;

import java.util.regex.Pattern;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.util.CellRangeAddress;

public final class OperandResolver {
   private static final String Digits = "(\\p{Digit}+)";
   private static final String Exp = "[eE][+-]?(\\p{Digit}+)";
   private static final String fpRegex = "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?))))[\\x00-\\x20]*";

   private OperandResolver() {
   }

   public static ValueEval getSingleValue(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval result;
      if (arg instanceof RefEval) {
         result = chooseSingleElementFromRef((RefEval)arg);
      } else if (arg instanceof AreaEval) {
         result = chooseSingleElementFromArea((AreaEval)arg, srcCellRow, srcCellCol);
      } else {
         result = arg;
      }

      if (result instanceof ErrorEval) {
         throw new EvaluationException((ErrorEval)result);
      } else {
         return result;
      }
   }

   public static ValueEval getElementFromArray(AreaEval ae, EvaluationCell cell) {
      CellRangeAddress range = cell.getArrayFormulaRange();
      int relativeRowIndex = cell.getRowIndex() - range.getFirstRow();
      int relativeColIndex = cell.getColumnIndex() - range.getFirstColumn();
      if (ae.isColumn()) {
         if (ae.isRow()) {
            return ae.getRelativeValue(0, 0);
         }

         if (relativeRowIndex < ae.getHeight()) {
            return ae.getRelativeValue(relativeRowIndex, 0);
         }
      } else {
         if (!ae.isRow() && relativeRowIndex < ae.getHeight() && relativeColIndex < ae.getWidth()) {
            return ae.getRelativeValue(relativeRowIndex, relativeColIndex);
         }

         if (ae.isRow() && relativeColIndex < ae.getWidth()) {
            return ae.getRelativeValue(0, relativeColIndex);
         }
      }

      return ErrorEval.NA;
   }

   public static ValueEval chooseSingleElementFromArea(AreaEval ae, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval result = chooseSingleElementFromAreaInternal(ae, srcCellRow, srcCellCol);
      if (result instanceof ErrorEval) {
         throw new EvaluationException((ErrorEval)result);
      } else {
         return result;
      }
   }

   private static ValueEval chooseSingleElementFromAreaInternal(AreaEval ae, int srcCellRow, int srcCellCol) throws EvaluationException {
      if (ae.isColumn()) {
         if (ae.isRow()) {
            return ae.getRelativeValue(0, 0);
         } else if (!ae.containsRow(srcCellRow)) {
            throw EvaluationException.invalidValue();
         } else {
            return ae.getAbsoluteValue(srcCellRow, ae.getFirstColumn());
         }
      } else if (!ae.isRow()) {
         if (ae.containsRow(srcCellRow) && ae.containsColumn(srcCellCol)) {
            return ae.getAbsoluteValue(srcCellRow, srcCellCol);
         } else {
            throw EvaluationException.invalidValue();
         }
      } else if (!ae.containsColumn(srcCellCol)) {
         throw EvaluationException.invalidValue();
      } else {
         return ae.getAbsoluteValue(ae.getFirstRow(), srcCellCol);
      }
   }

   private static ValueEval chooseSingleElementFromRef(RefEval ref) {
      return ref.getInnerValueEval(ref.getFirstSheetIndex());
   }

   public static int coerceValueToInt(ValueEval ev) throws EvaluationException {
      if (ev == BlankEval.instance) {
         return 0;
      }

      double d = coerceValueToDouble(ev);
      return (int)Math.floor(d);
   }

   public static double coerceValueToDouble(ValueEval ev) throws EvaluationException {
      if (ev == BlankEval.instance) {
         return 0.0;
      }

      if (ev instanceof NumericValueEval) {
         return ((NumericValueEval)ev).getNumberValue();
      }

      if (ev instanceof StringEval) {
         Double dd = parseDouble(((StringEval)ev).getStringValue());
         if (dd == null) {
            throw EvaluationException.invalidValue();
         } else {
            return dd;
         }
      } else {
         throw new RuntimeException("Unexpected arg eval type (" + ev.getClass().getName() + ")");
      }
   }

   public static Double parseDouble(String pText) {
      if (Pattern.matches(
         "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?))))[\\x00-\\x20]*",
         pText
      )) {
         try {
            return Double.parseDouble(pText);
         } catch (NumberFormatException e) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static String coerceValueToString(ValueEval ve) {
      if (ve instanceof StringValueEval) {
         StringValueEval sve = (StringValueEval)ve;
         return sve.getStringValue();
      } else if (ve == BlankEval.instance) {
         return "";
      } else {
         throw new IllegalArgumentException("Unexpected eval class (" + ve.getClass().getName() + ")");
      }
   }

   public static Boolean coerceValueToBoolean(ValueEval ve, boolean stringsAreBlanks) throws EvaluationException {
      if (ve == null || ve == BlankEval.instance) {
         return null;
      }

      if (ve instanceof BoolEval) {
         return ((BoolEval)ve).getBooleanValue();
      }

      if (ve == BlankEval.instance) {
         return null;
      }

      if (ve instanceof StringEval) {
         if (stringsAreBlanks) {
            return null;
         } else {
            String str = ((StringEval)ve).getStringValue();
            if (str.equalsIgnoreCase("true")) {
               return Boolean.TRUE;
            } else if (str.equalsIgnoreCase("false")) {
               return Boolean.FALSE;
            } else {
               throw new EvaluationException(ErrorEval.VALUE_INVALID);
            }
         }
      } else if (ve instanceof NumericValueEval) {
         NumericValueEval ne = (NumericValueEval)ve;
         double d = ne.getNumberValue();
         if (Double.isNaN(d)) {
            throw new EvaluationException(ErrorEval.VALUE_INVALID);
         } else {
            return d != 0.0;
         }
      } else if (ve instanceof ErrorEval) {
         throw new EvaluationException((ErrorEval)ve);
      } else {
         throw new RuntimeException("Unexpected eval (" + ve.getClass().getName() + ")");
      }
   }
}
