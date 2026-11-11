package org.apache.poi.ss.formula.atp;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.usermodel.DateUtil;

final class WorkdayFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new WorkdayFunction(ArgumentsEvaluator.instance);
   private ArgumentsEvaluator evaluator;

   private WorkdayFunction(ArgumentsEvaluator anEvaluator) {
      this.evaluator = anEvaluator;
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      if (args.length >= 2 && args.length <= 3) {
         int srcCellRow = ec.getRowIndex();
         int srcCellCol = ec.getColumnIndex();

         try {
            double start = this.evaluator.evaluateDateArg(args[0], srcCellRow, srcCellCol);
            int days = (int)Math.floor(this.evaluator.evaluateNumberArg(args[1], srcCellRow, srcCellCol));
            ValueEval holidaysCell = args.length == 3 ? args[2] : null;
            double[] holidays = this.evaluator.evaluateDatesArg(holidaysCell, srcCellRow, srcCellCol);
            return new NumberEval(DateUtil.getExcelDate(WorkdayCalculator.instance.calculateWorkdays(start, days, holidays)));
         } catch (EvaluationException e) {
            return ErrorEval.VALUE_INVALID;
         }
      } else {
         return ErrorEval.VALUE_INVALID;
      }
   }
}
