package org.apache.poi.hssf.usermodel;

import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FontFormatting;

public final class HSSFFontFormatting implements FontFormatting {
   public static final byte U_NONE = 0;
   public static final byte U_SINGLE = 1;
   public static final byte U_DOUBLE = 2;
   public static final byte U_SINGLE_ACCOUNTING = 33;
   public static final byte U_DOUBLE_ACCOUNTING = 34;
   private final org.apache.poi.hssf.record.cf.FontFormatting fontFormatting;
   private final HSSFWorkbook workbook;

   protected HSSFFontFormatting(CFRuleBase cfRuleRecord, HSSFWorkbook workbook) {
      this.fontFormatting = cfRuleRecord.getFontFormatting();
      this.workbook = workbook;
   }

   protected org.apache.poi.hssf.record.cf.FontFormatting getFontFormattingBlock() {
      return this.fontFormatting;
   }

   public short getEscapementType() {
      return this.fontFormatting.getEscapementType();
   }

   public short getFontColorIndex() {
      return this.fontFormatting.getFontColorIndex();
   }

   public HSSFColor getFontColor() {
      return this.workbook.getCustomPalette().getColor(this.getFontColorIndex());
   }

   public void setFontColor(Color color) {
      HSSFColor hcolor = HSSFColor.toHSSFColor(color);
      if (hcolor == null) {
         this.fontFormatting.setFontColorIndex((short)0);
      } else {
         this.fontFormatting.setFontColorIndex(hcolor.getIndex());
      }
   }

   public int getFontHeight() {
      return this.fontFormatting.getFontHeight();
   }

   public short getFontWeight() {
      return this.fontFormatting.getFontWeight();
   }

   protected byte[] getRawRecord() {
      return this.fontFormatting.getRawRecord();
   }

   public short getUnderlineType() {
      return this.fontFormatting.getUnderlineType();
   }

   public boolean isBold() {
      return this.fontFormatting.isFontWeightModified() && this.fontFormatting.isBold();
   }

   public boolean isEscapementTypeModified() {
      return this.fontFormatting.isEscapementTypeModified();
   }

   public boolean isFontCancellationModified() {
      return this.fontFormatting.isFontCancellationModified();
   }

   public boolean isFontOutlineModified() {
      return this.fontFormatting.isFontOutlineModified();
   }

   public boolean isFontShadowModified() {
      return this.fontFormatting.isFontShadowModified();
   }

   public boolean isFontStyleModified() {
      return this.fontFormatting.isFontStyleModified();
   }

   public boolean isItalic() {
      return this.fontFormatting.isFontStyleModified() && this.fontFormatting.isItalic();
   }

   public boolean isOutlineOn() {
      return this.fontFormatting.isFontOutlineModified() && this.fontFormatting.isOutlineOn();
   }

   public boolean isShadowOn() {
      return this.fontFormatting.isFontOutlineModified() && this.fontFormatting.isShadowOn();
   }

   public boolean isStruckout() {
      return this.fontFormatting.isFontCancellationModified() && this.fontFormatting.isStruckout();
   }

   public boolean isUnderlineTypeModified() {
      return this.fontFormatting.isUnderlineTypeModified();
   }

   public boolean isFontWeightModified() {
      return this.fontFormatting.isFontWeightModified();
   }

   public void setFontStyle(boolean italic, boolean bold) {
      boolean modified = italic || bold;
      this.fontFormatting.setItalic(italic);
      this.fontFormatting.setBold(bold);
      this.fontFormatting.setFontStyleModified(modified);
      this.fontFormatting.setFontWieghtModified(modified);
   }

   public void resetFontStyle() {
      this.setFontStyle(false, false);
   }

   public void setEscapementType(short escapementType) {
      switch (escapementType) {
         case 0:
            this.fontFormatting.setEscapementType(escapementType);
            this.fontFormatting.setEscapementTypeModified(false);
            break;
         case 1:
         case 2:
            this.fontFormatting.setEscapementType(escapementType);
            this.fontFormatting.setEscapementTypeModified(true);
      }
   }

   public void setEscapementTypeModified(boolean modified) {
      this.fontFormatting.setEscapementTypeModified(modified);
   }

   public void setFontCancellationModified(boolean modified) {
      this.fontFormatting.setFontCancellationModified(modified);
   }

   public void setFontColorIndex(short fci) {
      this.fontFormatting.setFontColorIndex(fci);
   }

   public void setFontHeight(int height) {
      this.fontFormatting.setFontHeight(height);
   }

   public void setFontOutlineModified(boolean modified) {
      this.fontFormatting.setFontOutlineModified(modified);
   }

   public void setFontShadowModified(boolean modified) {
      this.fontFormatting.setFontShadowModified(modified);
   }

   public void setFontStyleModified(boolean modified) {
      this.fontFormatting.setFontStyleModified(modified);
   }

   public void setOutline(boolean on) {
      this.fontFormatting.setOutline(on);
      this.fontFormatting.setFontOutlineModified(on);
   }

   public void setShadow(boolean on) {
      this.fontFormatting.setShadow(on);
      this.fontFormatting.setFontShadowModified(on);
   }

   public void setStrikeout(boolean strike) {
      this.fontFormatting.setStrikeout(strike);
      this.fontFormatting.setFontCancellationModified(strike);
   }

   public void setUnderlineType(short underlineType) {
      switch (underlineType) {
         case 0:
            this.fontFormatting.setUnderlineType(underlineType);
            this.setUnderlineTypeModified(false);
            break;
         case 1:
         case 2:
         case 33:
         case 34:
            this.fontFormatting.setUnderlineType(underlineType);
            this.setUnderlineTypeModified(true);
      }
   }

   public void setUnderlineTypeModified(boolean modified) {
      this.fontFormatting.setUnderlineTypeModified(modified);
   }
}
