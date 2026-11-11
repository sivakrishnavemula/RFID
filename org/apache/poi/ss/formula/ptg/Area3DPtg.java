package org.apache.poi.ss.formula.ptg;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.ExternSheetReferenceToken;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.WorkbookDependentFormula;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public final class Area3DPtg extends AreaPtgBase implements WorkbookDependentFormula, ExternSheetReferenceToken {
   public static final byte sid = 59;
   private static final int SIZE = 11;
   private int field_1_index_extern_sheet;

   public Area3DPtg(String arearef, int externIdx) {
      super(new AreaReference(arearef, SpreadsheetVersion.EXCEL97));
      this.setExternSheetIndex(externIdx);
   }

   public Area3DPtg(LittleEndianInput in) {
      this.field_1_index_extern_sheet = in.readShort();
      this.readCoordinates(in);
   }

   public Area3DPtg(
      int firstRow,
      int lastRow,
      int firstColumn,
      int lastColumn,
      boolean firstRowRelative,
      boolean lastRowRelative,
      boolean firstColRelative,
      boolean lastColRelative,
      int externalSheetIndex
   ) {
      super(firstRow, lastRow, firstColumn, lastColumn, firstRowRelative, lastRowRelative, firstColRelative, lastColRelative);
      this.setExternSheetIndex(externalSheetIndex);
   }

   public Area3DPtg(AreaReference arearef, int externIdx) {
      super(arearef);
      this.setExternSheetIndex(externIdx);
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(this.getClass().getName());
      sb.append(" [");
      sb.append("sheetIx=").append(this.getExternSheetIndex());
      sb.append(" ! ");
      sb.append(this.formatReferenceAsString());
      sb.append("]");
      return sb.toString();
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(59 + this.getPtgClass());
      out.writeShort(this.field_1_index_extern_sheet);
      this.writeCoordinates(out);
   }

   public int getSize() {
      return 11;
   }

   public int getExternSheetIndex() {
      return this.field_1_index_extern_sheet;
   }

   public void setExternSheetIndex(int index) {
      this.field_1_index_extern_sheet = index;
   }

   public String format2DRefAsString() {
      return this.formatReferenceAsString();
   }

   public String toFormulaString(FormulaRenderingWorkbook book) {
      return ExternSheetNameResolver.prependSheetName(book, this.field_1_index_extern_sheet, this.formatReferenceAsString());
   }

   public String toFormulaString() {
      throw new RuntimeException("3D references need a workbook to determine formula text");
   }
}
