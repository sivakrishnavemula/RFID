package org.apache.poi.hssf.record;

import java.util.Arrays;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.LittleEndianOutput;

public final class CFRuleRecord extends CFRuleBase implements Cloneable {
   public static final short sid = 433;

   private CFRuleRecord(byte conditionType, byte comparisonOperation) {
      super(conditionType, comparisonOperation);
      this.setDefaults();
   }

   private CFRuleRecord(byte conditionType, byte comparisonOperation, Ptg[] formula1, Ptg[] formula2) {
      super(conditionType, comparisonOperation, formula1, formula2);
      this.setDefaults();
   }

   private void setDefaults() {
      this.formatting_options = modificationBits.setValue(this.formatting_options, -1);
      this.formatting_options = fmtBlockBits.setValue(this.formatting_options, 0);
      this.formatting_options = undocumented.clear(this.formatting_options);
      this.formatting_not_used = -32766;
      this._fontFormatting = null;
      this._borderFormatting = null;
      this._patternFormatting = null;
   }

   public static CFRuleRecord create(HSSFSheet sheet, String formulaText) {
      Ptg[] formula1 = parseFormula(formulaText, sheet);
      return new CFRuleRecord((byte)2, (byte)0, formula1, null);
   }

   public static CFRuleRecord create(HSSFSheet sheet, byte comparisonOperation, String formulaText1, String formulaText2) {
      Ptg[] formula1 = parseFormula(formulaText1, sheet);
      Ptg[] formula2 = parseFormula(formulaText2, sheet);
      return new CFRuleRecord((byte)1, comparisonOperation, formula1, formula2);
   }

   public CFRuleRecord(RecordInputStream in) {
      this.setConditionType(in.readByte());
      this.setComparisonOperation(in.readByte());
      int field_3_formula1_len = in.readUShort();
      int field_4_formula2_len = in.readUShort();
      this.readFormatOptions(in);
      this.setFormula1(Formula.read(field_3_formula1_len, in));
      this.setFormula2(Formula.read(field_4_formula2_len, in));
   }

   public short getSid() {
      return 433;
   }

   public void serialize(LittleEndianOutput out) {
      int formula1Len = getFormulaSize(this.getFormula1());
      int formula2Len = getFormulaSize(this.getFormula2());
      out.writeByte(this.getConditionType());
      out.writeByte(this.getComparisonOperation());
      out.writeShort(formula1Len);
      out.writeShort(formula2Len);
      this.serializeFormattingBlock(out);
      this.getFormula1().serializeTokens(out);
      this.getFormula2().serializeTokens(out);
   }

   protected int getDataSize() {
      return 6 + this.getFormattingBlockSize() + getFormulaSize(this.getFormula1()) + getFormulaSize(this.getFormula2());
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[CFRULE]\n");
      buffer.append("    .condition_type   =").append(this.getConditionType()).append("\n");
      buffer.append("    OPTION FLAGS=0x").append(Integer.toHexString(this.getOptions())).append("\n");
      if (this.containsFontFormattingBlock()) {
         buffer.append(this._fontFormatting).append("\n");
      }

      if (this.containsBorderFormattingBlock()) {
         buffer.append(this._borderFormatting).append("\n");
      }

      if (this.containsPatternFormattingBlock()) {
         buffer.append(this._patternFormatting).append("\n");
      }

      buffer.append("    Formula 1 =").append(Arrays.toString(this.getFormula1().getTokens())).append("\n");
      buffer.append("    Formula 2 =").append(Arrays.toString(this.getFormula2().getTokens())).append("\n");
      buffer.append("[/CFRULE]\n");
      return buffer.toString();
   }

   public CFRuleRecord clone() {
      CFRuleRecord rec = new CFRuleRecord(this.getConditionType(), this.getComparisonOperation());
      super.copyTo(rec);
      return rec;
   }
}
