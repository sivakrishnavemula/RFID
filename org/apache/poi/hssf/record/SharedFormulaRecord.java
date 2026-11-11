package org.apache.poi.hssf.record;

import org.apache.poi.hssf.util.CellRangeAddress8Bit;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.SharedFormula;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

public final class SharedFormulaRecord extends SharedValueRecordBase {
   public static final short sid = 1212;
   private int field_5_reserved;
   private Formula field_7_parsed_expr;

   public SharedFormulaRecord() {
      this(new CellRangeAddress8Bit(0, 0, 0, 0));
   }

   private SharedFormulaRecord(CellRangeAddress8Bit range) {
      super(range);
      this.field_7_parsed_expr = Formula.create(Ptg.EMPTY_PTG_ARRAY);
   }

   public SharedFormulaRecord(RecordInputStream in) {
      super(in);
      this.field_5_reserved = in.readShort();
      int field_6_expression_len = in.readShort();
      int nAvailableBytes = in.available();
      this.field_7_parsed_expr = Formula.read(field_6_expression_len, in, nAvailableBytes);
   }

   protected void serializeExtraData(LittleEndianOutput out) {
      out.writeShort(this.field_5_reserved);
      this.field_7_parsed_expr.serialize(out);
   }

   protected int getExtraDataSize() {
      return 2 + this.field_7_parsed_expr.getEncodedSize();
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[SHARED FORMULA (").append(HexDump.intToHex(1212)).append("]\n");
      buffer.append("    .range      = ").append(this.getRange()).append("\n");
      buffer.append("    .reserved    = ").append(HexDump.shortToHex(this.field_5_reserved)).append("\n");
      Ptg[] ptgs = this.field_7_parsed_expr.getTokens();

      for (int k = 0; k < ptgs.length; k++) {
         buffer.append("Formula[").append(k).append("]");
         Ptg ptg = ptgs[k];
         buffer.append(ptg).append(ptg.getRVAType()).append("\n");
      }

      buffer.append("[/SHARED FORMULA]\n");
      return buffer.toString();
   }

   public short getSid() {
      return 1212;
   }

   public Ptg[] getFormulaTokens(FormulaRecord formula) {
      int formulaRow = formula.getRow();
      int formulaColumn = formula.getColumn();
      if (!this.isInRange(formulaRow, formulaColumn)) {
         throw new RuntimeException("Shared Formula Conversion: Coding Error");
      }

      SharedFormula sf = new SharedFormula(SpreadsheetVersion.EXCEL97);
      return sf.convertSharedFormulas(this.field_7_parsed_expr.getTokens(), formulaRow, formulaColumn);
   }

   public Object clone() {
      SharedFormulaRecord result = new SharedFormulaRecord(this.getRange());
      result.field_5_reserved = this.field_5_reserved;
      result.field_7_parsed_expr = this.field_7_parsed_expr.copy();
      return result;
   }

   public boolean isFormulaSame(SharedFormulaRecord other) {
      return this.field_7_parsed_expr.isSame(other.field_7_parsed_expr);
   }
}
