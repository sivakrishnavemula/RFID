package org.apache.poi.ss.formula;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.formula.eval.ConcatEval;
import org.apache.poi.ss.formula.eval.FunctionEval;
import org.apache.poi.ss.formula.eval.IntersectionEval;
import org.apache.poi.ss.formula.eval.PercentEval;
import org.apache.poi.ss.formula.eval.RangeEval;
import org.apache.poi.ss.formula.eval.RelationalOperationEval;
import org.apache.poi.ss.formula.eval.TwoOperandNumericOperation;
import org.apache.poi.ss.formula.eval.UnaryMinusEval;
import org.apache.poi.ss.formula.eval.UnaryPlusEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.ArrayFunction;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.formula.functions.Indirect;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AddPtg;
import org.apache.poi.ss.formula.ptg.ConcatPtg;
import org.apache.poi.ss.formula.ptg.DividePtg;
import org.apache.poi.ss.formula.ptg.EqualPtg;
import org.apache.poi.ss.formula.ptg.GreaterEqualPtg;
import org.apache.poi.ss.formula.ptg.GreaterThanPtg;
import org.apache.poi.ss.formula.ptg.IntersectionPtg;
import org.apache.poi.ss.formula.ptg.LessEqualPtg;
import org.apache.poi.ss.formula.ptg.LessThanPtg;
import org.apache.poi.ss.formula.ptg.MultiplyPtg;
import org.apache.poi.ss.formula.ptg.NotEqualPtg;
import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.PercentPtg;
import org.apache.poi.ss.formula.ptg.PowerPtg;
import org.apache.poi.ss.formula.ptg.RangePtg;
import org.apache.poi.ss.formula.ptg.SubtractPtg;
import org.apache.poi.ss.formula.ptg.UnaryMinusPtg;
import org.apache.poi.ss.formula.ptg.UnaryPlusPtg;

final class OperationEvaluatorFactory {
   private static final Map<OperationPtg, Function> _instancesByPtgClass = initialiseInstancesMap();

   private OperationEvaluatorFactory() {
   }

   private static Map<OperationPtg, Function> initialiseInstancesMap() {
      Map<OperationPtg, Function> m = new HashMap(32);
      put(m, EqualPtg.instance, RelationalOperationEval.EqualEval);
      put(m, GreaterEqualPtg.instance, RelationalOperationEval.GreaterEqualEval);
      put(m, GreaterThanPtg.instance, RelationalOperationEval.GreaterThanEval);
      put(m, LessEqualPtg.instance, RelationalOperationEval.LessEqualEval);
      put(m, LessThanPtg.instance, RelationalOperationEval.LessThanEval);
      put(m, NotEqualPtg.instance, RelationalOperationEval.NotEqualEval);
      put(m, ConcatPtg.instance, ConcatEval.instance);
      put(m, AddPtg.instance, TwoOperandNumericOperation.AddEval);
      put(m, DividePtg.instance, TwoOperandNumericOperation.DivideEval);
      put(m, MultiplyPtg.instance, TwoOperandNumericOperation.MultiplyEval);
      put(m, PercentPtg.instance, PercentEval.instance);
      put(m, PowerPtg.instance, TwoOperandNumericOperation.PowerEval);
      put(m, SubtractPtg.instance, TwoOperandNumericOperation.SubtractEval);
      put(m, UnaryMinusPtg.instance, UnaryMinusEval.instance);
      put(m, UnaryPlusPtg.instance, UnaryPlusEval.instance);
      put(m, RangePtg.instance, RangeEval.instance);
      put(m, IntersectionPtg.instance, IntersectionEval.instance);
      return m;
   }

   private static void put(Map<OperationPtg, Function> m, OperationPtg ptgKey, Function instance) {
      Constructor<?>[] cc = ptgKey.getClass().getDeclaredConstructors();
      if (cc.length <= 1 && Modifier.isPrivate(cc[0].getModifiers())) {
         m.put(ptgKey, instance);
      } else {
         throw new RuntimeException("Failed to verify instance (" + ptgKey.getClass().getName() + ") is a singleton.");
      }
   }

   public static ValueEval evaluate(OperationPtg ptg, ValueEval[] args, OperationEvaluationContext ec) {
      if (ptg == null) {
         throw new IllegalArgumentException("ptg must not be null");
      }

      Function result = (Function)_instancesByPtgClass.get(ptg);
      FreeRefFunction udfFunc = null;
      if (result == null && ptg instanceof AbstractFunctionPtg) {
         AbstractFunctionPtg fptg = (AbstractFunctionPtg)ptg;
         int functionIndex = fptg.getFunctionIndex();
         switch (functionIndex) {
            case 148:
               udfFunc = Indirect.instance;
               break;
            case 255:
               udfFunc = UserDefinedFunction.instance;
               break;
            default:
               result = FunctionEval.getBasicFunction(functionIndex);
         }
      }

      if (result == null) {
         if (udfFunc != null) {
            return udfFunc.evaluate(args, ec);
         } else {
            throw new RuntimeException("Unexpected operation ptg class (" + ptg.getClass().getName() + ")");
         }
      } else {
         EvaluationSheet evalSheet = ec.getWorkbook().getSheet(ec.getSheetIndex());
         EvaluationCell evalCell = evalSheet.getCell(ec.getRowIndex(), ec.getColumnIndex());
         return evalCell != null && (evalCell.isPartOfArrayFormulaGroup() || ec.isArraymode()) && result instanceof ArrayFunction
            ? ((ArrayFunction)result).evaluateArray(args, ec.getRowIndex(), ec.getColumnIndex())
            : result.evaluate(args, ec.getRowIndex(), ec.getColumnIndex());
      }
   }
}
