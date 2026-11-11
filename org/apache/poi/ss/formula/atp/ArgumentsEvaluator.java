package org.apache.poi.ss.formula.atp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.poi.ss.formula.eval.AreaEvalBase;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.DateUtil;

final class ArgumentsEvaluator {
   public static final ArgumentsEvaluator instance = new ArgumentsEvaluator();

   private ArgumentsEvaluator() {
   }

   public double evaluateDateArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, (short)srcCellCol);
      if (ve instanceof StringEval) {
         String strVal = ((StringEval)ve).getStringValue();
         Double dVal = OperandResolver.parseDouble(strVal);
         if (dVal != null) {
            return dVal;
         }

         Calendar date = DateParser.parseDate(strVal);
         return DateUtil.getExcelDate(date, false);
      } else {
         return OperandResolver.coerceValueToDouble(ve);
      }
   }

   public double[] evaluateDatesArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
      if (arg == null) {
         return new double[0];
      }

      if (arg instanceof StringEval) {
         return new double[]{this.evaluateDateArg(arg, srcCellRow, srcCellCol)};
      }

      if (!(arg instanceof AreaEvalBase)) {
         return new double[]{OperandResolver.coerceValueToDouble(arg)};
      }

      List<Double> valuesList = new ArrayList();
      AreaEvalBase area = (AreaEvalBase)arg;

      for (int i = area.getFirstRow(); i <= area.getLastRow(); i++) {
         for (int j = area.getFirstColumn(); j <= area.getLastColumn(); j++) {
            valuesList.add(this.evaluateDateArg(area.getAbsoluteValue(i, j), i, j));
         }
      }

      double[] values = new double[valuesList.size()];

      for (int i = 0; i < valuesList.size(); i++) {
         values[i] = (Double)valuesList.get(i);
      }

      return values;
   }

   public double evaluateNumberArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
      return arg == null ? 0.0 : OperandResolver.coerceValueToDouble(arg);
   }
}
