package org.apache.poi.ss.formula.ptg;

import org.apache.poi.ss.formula.SheetNameFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.util.LittleEndianOutput;

public final class Deleted3DPxg extends OperandPtg implements Pxg {
   private int externalWorkbookNumber = -1;
   private String sheetName;

   public Deleted3DPxg(int externalWorkbookNumber, String sheetName) {
      this.externalWorkbookNumber = externalWorkbookNumber;
      this.sheetName = sheetName;
   }

   public Deleted3DPxg(String sheetName) {
      this(-1, sheetName);
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(this.getClass().getName());
      sb.append(" [");
      if (this.externalWorkbookNumber >= 0) {
         sb.append(" [");
         sb.append("workbook=").append(this.getExternalWorkbookNumber());
         sb.append("] ");
      }

      sb.append("sheet=").append(this.getSheetName());
      sb.append(" ! ");
      sb.append(FormulaError.REF.getString());
      sb.append("]");
      return sb.toString();
   }

   public int getExternalWorkbookNumber() {
      return this.externalWorkbookNumber;
   }

   public String getSheetName() {
      return this.sheetName;
   }

   public void setSheetName(String sheetName) {
      this.sheetName = sheetName;
   }

   public String toFormulaString() {
      StringBuilder sb = new StringBuilder(64);
      if (this.externalWorkbookNumber >= 0) {
         sb.append('[');
         sb.append(this.externalWorkbookNumber);
         sb.append(']');
      }

      if (this.sheetName != null) {
         SheetNameFormatter.appendFormat(sb, this.sheetName);
      }

      sb.append('!');
      sb.append(FormulaError.REF.getString());
      return sb.toString();
   }

   public byte getDefaultOperandClass() {
      return 32;
   }

   public int getSize() {
      return 1;
   }

   public void write(LittleEndianOutput out) {
      throw new IllegalStateException("XSSF-only Ptg, should not be serialised");
   }
}
