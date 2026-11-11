package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.formula.ConditionalFormattingEvaluator;
import org.apache.poi.ss.formula.EvaluationConditionalFormatRule;

public class ExcelNumberFormat {
   private final int idx;
   private final String format;

   public static ExcelNumberFormat from(CellStyle style) {
      return style == null ? null : new ExcelNumberFormat(style.getDataFormat(), style.getDataFormatString());
   }

   public static ExcelNumberFormat from(Cell cell, ConditionalFormattingEvaluator cfEvaluator) {
      if (cell == null) {
         return null;
      }

      ExcelNumberFormat nf = null;
      if (cfEvaluator != null) {
         for (EvaluationConditionalFormatRule rule : cfEvaluator.getConditionalFormattingForCell(cell)) {
            nf = rule.getNumberFormat();
            if (nf != null) {
               break;
            }
         }
      }

      if (nf == null) {
         CellStyle style = cell.getCellStyle();
         nf = from(style);
      }

      return nf;
   }

   public ExcelNumberFormat(int idx, String format) {
      this.idx = idx;
      this.format = format;
   }

   public int getIdx() {
      return this.idx;
   }

   public String getFormat() {
      return this.format;
   }
}
