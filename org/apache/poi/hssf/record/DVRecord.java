package org.apache.poi.hssf.record;

import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.BitField;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;

public final class DVRecord extends StandardRecord implements Cloneable {
   public static final short sid = 446;
   private static final UnicodeString NULL_TEXT_STRING = new UnicodeString("\u0000");
   private int _option_flags;
   private UnicodeString _promptTitle;
   private UnicodeString _errorTitle;
   private UnicodeString _promptText;
   private UnicodeString _errorText;
   private short _not_used_1 = 16352;
   private Formula _formula1;
   private short _not_used_2 = 0;
   private Formula _formula2;
   private CellRangeAddressList _regions;
   private static final BitField opt_data_type = new BitField(15);
   private static final BitField opt_error_style = new BitField(112);
   private static final BitField opt_string_list_formula = new BitField(128);
   private static final BitField opt_empty_cell_allowed = new BitField(256);
   private static final BitField opt_suppress_dropdown_arrow = new BitField(512);
   private static final BitField opt_show_prompt_on_cell_selected = new BitField(262144);
   private static final BitField opt_show_error_on_invalid_value = new BitField(524288);
   private static final BitField opt_condition_operator = new BitField(7340032);

   public DVRecord(
      int validationType,
      int operator,
      int errorStyle,
      boolean emptyCellAllowed,
      boolean suppressDropDownArrow,
      boolean isExplicitList,
      boolean showPromptBox,
      String promptTitle,
      String promptText,
      boolean showErrorBox,
      String errorTitle,
      String errorText,
      Ptg[] formula1,
      Ptg[] formula2,
      CellRangeAddressList regions
   ) {
      if (promptTitle != null && promptTitle.length() > 32) {
         throw new IllegalStateException("Prompt-title cannot be longer than 32 characters, but had: " + promptTitle);
      }

      if (promptText != null && promptText.length() > 255) {
         throw new IllegalStateException("Prompt-text cannot be longer than 255 characters, but had: " + promptText);
      }

      if (errorTitle != null && errorTitle.length() > 32) {
         throw new IllegalStateException("Error-title cannot be longer than 32 characters, but had: " + errorTitle);
      }

      if (errorText != null && errorText.length() > 255) {
         throw new IllegalStateException("Error-text cannot be longer than 255 characters, but had: " + errorText);
      }

      int flags = 0;
      flags = opt_data_type.setValue(flags, validationType);
      flags = opt_condition_operator.setValue(flags, operator);
      flags = opt_error_style.setValue(flags, errorStyle);
      flags = opt_empty_cell_allowed.setBoolean(flags, emptyCellAllowed);
      flags = opt_suppress_dropdown_arrow.setBoolean(flags, suppressDropDownArrow);
      flags = opt_string_list_formula.setBoolean(flags, isExplicitList);
      flags = opt_show_prompt_on_cell_selected.setBoolean(flags, showPromptBox);
      flags = opt_show_error_on_invalid_value.setBoolean(flags, showErrorBox);
      this._option_flags = flags;
      this._promptTitle = resolveTitleText(promptTitle);
      this._promptText = resolveTitleText(promptText);
      this._errorTitle = resolveTitleText(errorTitle);
      this._errorText = resolveTitleText(errorText);
      this._formula1 = Formula.create(formula1);
      this._formula2 = Formula.create(formula2);
      this._regions = regions;
   }

   public DVRecord(RecordInputStream in) {
      this._option_flags = in.readInt();
      this._promptTitle = readUnicodeString(in);
      this._errorTitle = readUnicodeString(in);
      this._promptText = readUnicodeString(in);
      this._errorText = readUnicodeString(in);
      int field_size_first_formula = in.readUShort();
      this._not_used_1 = in.readShort();
      this._formula1 = Formula.read(field_size_first_formula, in);
      int field_size_sec_formula = in.readUShort();
      this._not_used_2 = in.readShort();
      this._formula2 = Formula.read(field_size_sec_formula, in);
      this._regions = new CellRangeAddressList(in);
   }

   public int getDataType() {
      return opt_data_type.getValue(this._option_flags);
   }

   public int getErrorStyle() {
      return opt_error_style.getValue(this._option_flags);
   }

   public boolean getListExplicitFormula() {
      return opt_string_list_formula.isSet(this._option_flags);
   }

   public boolean getEmptyCellAllowed() {
      return opt_empty_cell_allowed.isSet(this._option_flags);
   }

   public boolean getSuppressDropdownArrow() {
      return opt_suppress_dropdown_arrow.isSet(this._option_flags);
   }

   public boolean getShowPromptOnCellSelected() {
      return opt_show_prompt_on_cell_selected.isSet(this._option_flags);
   }

   public boolean getShowErrorOnInvalidValue() {
      return opt_show_error_on_invalid_value.isSet(this._option_flags);
   }

   public int getConditionOperator() {
      return opt_condition_operator.getValue(this._option_flags);
   }

   public String getPromptTitle() {
      return resolveTitleString(this._promptTitle);
   }

   public String getErrorTitle() {
      return resolveTitleString(this._errorTitle);
   }

   public String getPromptText() {
      return resolveTitleString(this._promptText);
   }

   public String getErrorText() {
      return resolveTitleString(this._errorText);
   }

   public Ptg[] getFormula1() {
      return Formula.getTokens(this._formula1);
   }

   public Ptg[] getFormula2() {
      return Formula.getTokens(this._formula2);
   }

   public CellRangeAddressList getCellRangeAddress() {
      return this._regions;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[DV]\n");
      sb.append(" options=").append(Integer.toHexString(this._option_flags));
      sb.append(" title-prompt=").append(formatTextTitle(this._promptTitle));
      sb.append(" title-error=").append(formatTextTitle(this._errorTitle));
      sb.append(" text-prompt=").append(formatTextTitle(this._promptText));
      sb.append(" text-error=").append(formatTextTitle(this._errorText));
      sb.append("\n");
      appendFormula(sb, "Formula 1:", this._formula1);
      appendFormula(sb, "Formula 2:", this._formula2);
      sb.append("Regions: ");
      int nRegions = this._regions.countRanges();

      for (int i = 0; i < nRegions; i++) {
         if (i > 0) {
            sb.append(", ");
         }

         CellRangeAddress addr = this._regions.getCellRangeAddress(i);
         sb.append('(').append(addr.getFirstRow()).append(',').append(addr.getLastRow());
         sb.append(',').append(addr.getFirstColumn()).append(',').append(addr.getLastColumn()).append(')');
      }

      sb.append("\n");
      sb.append("[/DV]");
      return sb.toString();
   }

   private static String formatTextTitle(UnicodeString us) {
      String str = us.getString();
      return str.length() == 1 && str.charAt(0) == 0 ? "'\\0'" : str;
   }

   private static void appendFormula(StringBuffer sb, String label, Formula f) {
      sb.append(label);
      if (f == null) {
         sb.append("<empty>\n");
      } else {
         Ptg[] ptgs = f.getTokens();
         sb.append('\n');

         for (Ptg ptg : ptgs) {
            sb.append('\t').append(ptg).append('\n');
         }
      }
   }

   public void serialize(LittleEndianOutput out) {
      out.writeInt(this._option_flags);
      serializeUnicodeString(this._promptTitle, out);
      serializeUnicodeString(this._errorTitle, out);
      serializeUnicodeString(this._promptText, out);
      serializeUnicodeString(this._errorText, out);
      out.writeShort(this._formula1.getEncodedTokenSize());
      out.writeShort(this._not_used_1);
      this._formula1.serializeTokens(out);
      out.writeShort(this._formula2.getEncodedTokenSize());
      out.writeShort(this._not_used_2);
      this._formula2.serializeTokens(out);
      this._regions.serialize(out);
   }

   private static UnicodeString resolveTitleText(String str) {
      return str != null && str.length() >= 1 ? new UnicodeString(str) : NULL_TEXT_STRING;
   }

   private static String resolveTitleString(UnicodeString us) {
      return us != null && !us.equals(NULL_TEXT_STRING) ? us.getString() : null;
   }

   private static UnicodeString readUnicodeString(RecordInputStream in) {
      return new UnicodeString(in);
   }

   private static void serializeUnicodeString(UnicodeString us, LittleEndianOutput out) {
      StringUtil.writeUnicodeString(out, us.getString());
   }

   private static int getUnicodeStringSize(UnicodeString us) {
      String str = us.getString();
      return 3 + str.length() * (StringUtil.hasMultibyte(str) ? 2 : 1);
   }

   protected int getDataSize() {
      int size = 12;
      size += getUnicodeStringSize(this._promptTitle);
      size += getUnicodeStringSize(this._errorTitle);
      size += getUnicodeStringSize(this._promptText);
      size += getUnicodeStringSize(this._errorText);
      size += this._formula1.getEncodedTokenSize();
      size += this._formula2.getEncodedTokenSize();
      return size + this._regions.getSize();
   }

   public short getSid() {
      return 446;
   }

   public DVRecord clone() {
      return (DVRecord)this.cloneViaReserialise();
   }
}
