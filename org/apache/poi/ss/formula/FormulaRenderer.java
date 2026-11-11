package org.apache.poi.ss.formula;

import java.util.Stack;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.MemAreaPtg;
import org.apache.poi.ss.formula.ptg.MemErrPtg;
import org.apache.poi.ss.formula.ptg.MemFuncPtg;
import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.ParenthesisPtg;
import org.apache.poi.ss.formula.ptg.Ptg;

public class FormulaRenderer {
   public static String toFormulaString(FormulaRenderingWorkbook book, Ptg[] ptgs) {
      if (ptgs != null && ptgs.length != 0) {
         Stack<String> stack = new Stack();

         for (Ptg ptg : ptgs) {
            if (!(ptg instanceof MemAreaPtg) && !(ptg instanceof MemFuncPtg) && !(ptg instanceof MemErrPtg)) {
               if (ptg instanceof ParenthesisPtg) {
                  String contents = (String)stack.pop();
                  stack.push("(" + contents + ")");
               } else if (ptg instanceof AttrPtg) {
                  AttrPtg attrPtg = (AttrPtg)ptg;
                  if (!attrPtg.isOptimizedIf() && !attrPtg.isOptimizedChoose() && !attrPtg.isSkip() && !attrPtg.isSpace() && !attrPtg.isSemiVolatile()) {
                     if (!attrPtg.isSum()) {
                        throw new RuntimeException("Unexpected tAttr: " + attrPtg);
                     }

                     String[] operands = getOperands(stack, attrPtg.getNumberOfOperands());
                     stack.push(attrPtg.toFormulaString(operands));
                  }
               } else if (ptg instanceof WorkbookDependentFormula) {
                  WorkbookDependentFormula optg = (WorkbookDependentFormula)ptg;
                  stack.push(optg.toFormulaString(book));
               } else if (!(ptg instanceof OperationPtg)) {
                  stack.push(ptg.toFormulaString());
               } else {
                  OperationPtg o = (OperationPtg)ptg;
                  String[] operands = getOperands(stack, o.getNumberOfOperands());
                  stack.push(o.toFormulaString(operands));
               }
            }
         }

         if (stack.isEmpty()) {
            throw new IllegalStateException("Stack underflow");
         } else {
            String result = (String)stack.pop();
            if (!stack.isEmpty()) {
               throw new IllegalStateException("too much stuff left on the stack");
            } else {
               return result;
            }
         }
      } else {
         throw new IllegalArgumentException("ptgs must not be null");
      }
   }

   private static String[] getOperands(Stack<String> stack, int nOperands) {
      String[] operands = new String[nOperands];

      for (int j = nOperands - 1; j >= 0; j--) {
         if (stack.isEmpty()) {
            String msg = "Too few arguments supplied to operation. Expected (" + nOperands + ") operands but got (" + (nOperands - j - 1) + ")";
            throw new IllegalStateException(msg);
         }

         operands[j] = (String)stack.pop();
      }

      return operands;
   }
}
