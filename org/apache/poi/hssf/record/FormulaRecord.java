package org.apache.poi.hssf.record;

import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

public final class FormulaRecord extends CellRecord implements Cloneable {
   public static final short sid = 6;
   private static int FIXED_SIZE = 14;
   private static final BitField alwaysCalc = BitFieldFactory.getInstance(1);
   private static final BitField calcOnLoad = BitFieldFactory.getInstance(2);
   private static final BitField sharedFormula = BitFieldFactory.getInstance(8);
   private double field_4_value;
   private short field_5_options;
   private int field_6_zero;
   private Formula field_8_parsed_expr;
   private FormulaRecord.SpecialCachedValue specialCachedValue;

   public FormulaRecord() {
      this.field_8_parsed_expr = Formula.create(Ptg.EMPTY_PTG_ARRAY);
   }

   public FormulaRecord(RecordInputStream ris) {
      super(ris);
      long valueLongBits = ris.readLong();
      this.field_5_options = ris.readShort();
      this.specialCachedValue = FormulaRecord.SpecialCachedValue.create(valueLongBits);
      if (this.specialCachedValue == null) {
         this.field_4_value = Double.longBitsToDouble(valueLongBits);
      }

      this.field_6_zero = ris.readInt();
      int field_7_expression_len = ris.readShort();
      int nBytesAvailable = ris.available();
      this.field_8_parsed_expr = Formula.read(field_7_expression_len, ris, nBytesAvailable);
   }

   public void setValue(double value) {
      this.field_4_value = value;
      this.specialCachedValue = null;
   }

   public void setCachedResultTypeEmptyString() {
      this.specialCachedValue = FormulaRecord.SpecialCachedValue.createCachedEmptyValue();
   }

   public void setCachedResultTypeString() {
      this.specialCachedValue = FormulaRecord.SpecialCachedValue.createForString();
   }

   public void setCachedResultErrorCode(int errorCode) {
      this.specialCachedValue = FormulaRecord.SpecialCachedValue.createCachedErrorCode(errorCode);
   }

   public void setCachedResultBoolean(boolean value) {
      this.specialCachedValue = FormulaRecord.SpecialCachedValue.createCachedBoolean(value);
   }

   public boolean hasCachedResultString() {
      return this.specialCachedValue != null && this.specialCachedValue.getTypeCode() == 0;
   }

   public int getCachedResultType() {
      return this.specialCachedValue == null ? CellType.NUMERIC.getCode() : this.specialCachedValue.getValueType();
   }

   public boolean getCachedBooleanValue() {
      return this.specialCachedValue.getBooleanValue();
   }

   public int getCachedErrorValue() {
      return this.specialCachedValue.getErrorValue();
   }

   public void setOptions(short options) {
      this.field_5_options = options;
   }

   public double getValue() {
      return this.field_4_value;
   }

   public short getOptions() {
      return this.field_5_options;
   }

   public boolean isSharedFormula() {
      return sharedFormula.isSet(this.field_5_options);
   }

   public void setSharedFormula(boolean flag) {
      this.field_5_options = sharedFormula.setShortBoolean(this.field_5_options, flag);
   }

   public boolean isAlwaysCalc() {
      return alwaysCalc.isSet(this.field_5_options);
   }

   public void setAlwaysCalc(boolean flag) {
      this.field_5_options = alwaysCalc.setShortBoolean(this.field_5_options, flag);
   }

   public boolean isCalcOnLoad() {
      return calcOnLoad.isSet(this.field_5_options);
   }

   public void setCalcOnLoad(boolean flag) {
      this.field_5_options = calcOnLoad.setShortBoolean(this.field_5_options, flag);
   }

   public Ptg[] getParsedExpression() {
      return this.field_8_parsed_expr.getTokens();
   }

   public Formula getFormula() {
      return this.field_8_parsed_expr;
   }

   public void setParsedExpression(Ptg[] ptgs) {
      this.field_8_parsed_expr = Formula.create(ptgs);
   }

   public short getSid() {
      return 6;
   }

   protected int getValueDataSize() {
      return FIXED_SIZE + this.field_8_parsed_expr.getEncodedSize();
   }

   protected void serializeValue(LittleEndianOutput out) {
      if (this.specialCachedValue == null) {
         out.writeDouble(this.field_4_value);
      } else {
         this.specialCachedValue.serialize(out);
      }

      out.writeShort(this.getOptions());
      out.writeInt(this.field_6_zero);
      this.field_8_parsed_expr.serialize(out);
   }

   protected String getRecordName() {
      return "FORMULA";
   }

   protected void appendValueText(StringBuilder sb) {
      sb.append("  .value\t = ");
      if (this.specialCachedValue == null) {
         sb.append(this.field_4_value).append("\n");
      } else {
         sb.append(this.specialCachedValue.formatDebugString()).append("\n");
      }

      sb.append("  .options   = ").append(HexDump.shortToHex(this.getOptions())).append("\n");
      sb.append("    .alwaysCalc= ").append(this.isAlwaysCalc()).append("\n");
      sb.append("    .calcOnLoad= ").append(this.isCalcOnLoad()).append("\n");
      sb.append("    .shared    = ").append(this.isSharedFormula()).append("\n");
      sb.append("  .zero      = ").append(HexDump.intToHex(this.field_6_zero)).append("\n");
      Ptg[] ptgs = this.field_8_parsed_expr.getTokens();

      for (int k = 0; k < ptgs.length; k++) {
         if (k > 0) {
            sb.append("\n");
         }

         sb.append("    Ptg[").append(k).append("]=");
         Ptg ptg = ptgs[k];
         sb.append(ptg).append(ptg.getRVAType());
      }
   }

   public FormulaRecord clone() {
      FormulaRecord rec = new FormulaRecord();
      this.copyBaseFields(rec);
      rec.field_4_value = this.field_4_value;
      rec.field_5_options = this.field_5_options;
      rec.field_6_zero = this.field_6_zero;
      rec.field_8_parsed_expr = this.field_8_parsed_expr;
      rec.specialCachedValue = this.specialCachedValue;
      return rec;
   }

   static final class SpecialCachedValue {
      private static final long BIT_MARKER = -281474976710656L;
      private static final int VARIABLE_DATA_LENGTH = 6;
      private static final int DATA_INDEX = 2;
      public static final int STRING = 0;
      public static final int BOOLEAN = 1;
      public static final int ERROR_CODE = 2;
      public static final int EMPTY = 3;
      private final byte[] _variableData;

      private SpecialCachedValue(byte[] data) {
         this._variableData = data;
      }

      public int getTypeCode() {
         return this._variableData[0];
      }

      public static FormulaRecord.SpecialCachedValue create(long valueLongBits) {
         if ((-281474976710656L & valueLongBits) != -281474976710656L) {
            return null;
         }

         byte[] result = new byte[6];
         long x = valueLongBits;

         for (int i = 0; i < 6; i++) {
            result[i] = (byte)x;
            x >>= 8;
         }

         switch (result[0]) {
            case 0:
            case 1:
            case 2:
            case 3:
               return new FormulaRecord.SpecialCachedValue(result);
            default:
               throw new RecordFormatException("Bad special value code (" + result[0] + ")");
         }
      }

      public void serialize(LittleEndianOutput out) {
         out.write(this._variableData);
         out.writeShort(65535);
      }

      public String formatDebugString() {
         return this.formatValue() + ' ' + HexDump.toHex(this._variableData);
      }

      private String formatValue() {
         int typeCode = this.getTypeCode();
         switch (typeCode) {
            case 0:
               return "<string>";
            case 1:
               return this.getDataValue() == 0 ? "FALSE" : "TRUE";
            case 2:
               return ErrorEval.getText(this.getDataValue());
            case 3:
               return "<empty>";
            default:
               return "#error(type=" + typeCode + ")#";
         }
      }

      private int getDataValue() {
         return this._variableData[2];
      }

      public static FormulaRecord.SpecialCachedValue createCachedEmptyValue() {
         return create(3, 0);
      }

      public static FormulaRecord.SpecialCachedValue createForString() {
         return create(0, 0);
      }

      public static FormulaRecord.SpecialCachedValue createCachedBoolean(boolean b) {
         return create(1, b ? 1 : 0);
      }

      public static FormulaRecord.SpecialCachedValue createCachedErrorCode(int errorCode) {
         return create(2, errorCode);
      }

      private static FormulaRecord.SpecialCachedValue create(int code, int data) {
         byte[] vd = new byte[]{(byte)code, 0, (byte)data, 0, 0, 0};
         return new FormulaRecord.SpecialCachedValue(vd);
      }

      public String toString() {
         return this.getClass().getName() + '[' + this.formatValue() + ']';
      }

      public int getValueType() {
         int typeCode = this.getTypeCode();
         switch (typeCode) {
            case 0:
               return CellType.STRING.getCode();
            case 1:
               return CellType.BOOLEAN.getCode();
            case 2:
               return CellType.ERROR.getCode();
            case 3:
               return CellType.STRING.getCode();
            default:
               throw new IllegalStateException("Unexpected type id (" + typeCode + ")");
         }
      }

      public boolean getBooleanValue() {
         if (this.getTypeCode() != 1) {
            throw new IllegalStateException("Not a boolean cached value - " + this.formatValue());
         } else {
            return this.getDataValue() != 0;
         }
      }

      public int getErrorValue() {
         if (this.getTypeCode() != 2) {
            throw new IllegalStateException("Not an error cached value - " + this.formatValue());
         } else {
            return this.getDataValue();
         }
      }
   }
}
