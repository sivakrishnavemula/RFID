package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.RefEvalBase;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ptg.AreaI;
import org.apache.poi.ss.formula.ptg.AreaI.OffsetArea;
import org.apache.poi.ss.util.CellReference;

public final class LazyRefEval extends RefEvalBase {
   private final SheetRangeEvaluator _evaluator;

   public LazyRefEval(int rowIndex, int columnIndex, SheetRangeEvaluator sre) {
      super(sre, rowIndex, columnIndex);
      this._evaluator = sre;
   }

   public ValueEval getInnerValueEval(int sheetIndex) {
      return this._evaluator.getEvalForCell(sheetIndex, this.getRow(), this.getColumn());
   }

   public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx) {
      AreaI area = new OffsetArea(this.getRow(), this.getColumn(), relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);
      return new LazyAreaEval(area, this._evaluator);
   }

   public boolean isSubTotal() {
      SheetRefEvaluator sheetEvaluator = this._evaluator.getSheetEvaluator(this.getFirstSheetIndex());
      return sheetEvaluator.isSubTotal(this.getRow(), this.getColumn());
   }

   public String toString() {
      CellReference cr = new CellReference(this.getRow(), this.getColumn());
      return this.getClass().getName() + "[" + this._evaluator.getSheetNameRange() + '!' + cr.formatAsString() + "]";
   }
}
