package org.apache.poi.hssf.usermodel;

import org.apache.poi.hssf.record.CFRuleBase;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.BorderFormatting;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Color;

public final class HSSFBorderFormatting implements BorderFormatting {
   private final HSSFWorkbook workbook;
   private final CFRuleBase cfRuleRecord;
   private final org.apache.poi.hssf.record.cf.BorderFormatting borderFormatting;

   protected HSSFBorderFormatting(CFRuleBase cfRuleRecord, HSSFWorkbook workbook) {
      this.workbook = workbook;
      this.cfRuleRecord = cfRuleRecord;
      this.borderFormatting = cfRuleRecord.getBorderFormatting();
   }

   protected org.apache.poi.hssf.record.cf.BorderFormatting getBorderFormattingBlock() {
      return this.borderFormatting;
   }

   public BorderStyle getBorderBottom() {
      return BorderStyle.valueOf((short)this.borderFormatting.getBorderBottom());
   }

   public BorderStyle getBorderDiagonal() {
      return BorderStyle.valueOf((short)this.borderFormatting.getBorderDiagonal());
   }

   public BorderStyle getBorderLeft() {
      return BorderStyle.valueOf((short)this.borderFormatting.getBorderLeft());
   }

   public BorderStyle getBorderRight() {
      return BorderStyle.valueOf((short)this.borderFormatting.getBorderRight());
   }

   public BorderStyle getBorderTop() {
      return BorderStyle.valueOf((short)this.borderFormatting.getBorderTop());
   }

   public short getBottomBorderColor() {
      return (short)this.borderFormatting.getBottomBorderColor();
   }

   public HSSFColor getBottomBorderColorColor() {
      return this.workbook.getCustomPalette().getColor(this.borderFormatting.getBottomBorderColor());
   }

   public short getDiagonalBorderColor() {
      return (short)this.borderFormatting.getDiagonalBorderColor();
   }

   public HSSFColor getDiagonalBorderColorColor() {
      return this.workbook.getCustomPalette().getColor(this.borderFormatting.getDiagonalBorderColor());
   }

   public short getLeftBorderColor() {
      return (short)this.borderFormatting.getLeftBorderColor();
   }

   public HSSFColor getLeftBorderColorColor() {
      return this.workbook.getCustomPalette().getColor(this.borderFormatting.getLeftBorderColor());
   }

   public short getRightBorderColor() {
      return (short)this.borderFormatting.getRightBorderColor();
   }

   public HSSFColor getRightBorderColorColor() {
      return this.workbook.getCustomPalette().getColor(this.borderFormatting.getRightBorderColor());
   }

   public short getTopBorderColor() {
      return (short)this.borderFormatting.getTopBorderColor();
   }

   public HSSFColor getTopBorderColorColor() {
      return this.workbook.getCustomPalette().getColor(this.borderFormatting.getTopBorderColor());
   }

   public boolean isBackwardDiagonalOn() {
      return this.borderFormatting.isBackwardDiagonalOn();
   }

   public boolean isForwardDiagonalOn() {
      return this.borderFormatting.isForwardDiagonalOn();
   }

   public void setBackwardDiagonalOn(boolean on) {
      this.borderFormatting.setBackwardDiagonalOn(on);
      if (on) {
         this.cfRuleRecord.setTopLeftBottomRightBorderModified(on);
      }
   }

   public void setForwardDiagonalOn(boolean on) {
      this.borderFormatting.setForwardDiagonalOn(on);
      if (on) {
         this.cfRuleRecord.setBottomLeftTopRightBorderModified(on);
      }
   }

   public void setBorderBottom(BorderStyle border) {
      short code = border.getCode();
      this.borderFormatting.setBorderBottom(code);
      if (code != 0) {
         this.cfRuleRecord.setBottomBorderModified(true);
      } else {
         this.cfRuleRecord.setBottomBorderModified(false);
      }
   }

   public void setBorderDiagonal(BorderStyle border) {
      short code = border.getCode();
      this.borderFormatting.setBorderDiagonal(code);
      if (code != 0) {
         this.cfRuleRecord.setBottomLeftTopRightBorderModified(true);
         this.cfRuleRecord.setTopLeftBottomRightBorderModified(true);
      } else {
         this.cfRuleRecord.setBottomLeftTopRightBorderModified(false);
         this.cfRuleRecord.setTopLeftBottomRightBorderModified(false);
      }
   }

   public void setBorderLeft(BorderStyle border) {
      short code = border.getCode();
      this.borderFormatting.setBorderLeft(code);
      if (code != 0) {
         this.cfRuleRecord.setLeftBorderModified(true);
      } else {
         this.cfRuleRecord.setLeftBorderModified(false);
      }
   }

   public void setBorderRight(BorderStyle border) {
      short code = border.getCode();
      this.borderFormatting.setBorderRight(code);
      if (code != 0) {
         this.cfRuleRecord.setRightBorderModified(true);
      } else {
         this.cfRuleRecord.setRightBorderModified(false);
      }
   }

   public void setBorderTop(BorderStyle border) {
      short code = border.getCode();
      this.borderFormatting.setBorderTop(code);
      if (code != 0) {
         this.cfRuleRecord.setTopBorderModified(true);
      } else {
         this.cfRuleRecord.setTopBorderModified(false);
      }
   }

   public void setBottomBorderColor(short color) {
      this.borderFormatting.setBottomBorderColor(color);
      if (color != 0) {
         this.cfRuleRecord.setBottomBorderModified(true);
      } else {
         this.cfRuleRecord.setBottomBorderModified(false);
      }
   }

   public void setBottomBorderColor(Color color) {
      HSSFColor hcolor = HSSFColor.toHSSFColor(color);
      if (hcolor == null) {
         this.setBottomBorderColor((short)0);
      } else {
         this.setBottomBorderColor(hcolor.getIndex());
      }
   }

   public void setDiagonalBorderColor(short color) {
      this.borderFormatting.setDiagonalBorderColor(color);
      if (color != 0) {
         this.cfRuleRecord.setBottomLeftTopRightBorderModified(true);
         this.cfRuleRecord.setTopLeftBottomRightBorderModified(true);
      } else {
         this.cfRuleRecord.setBottomLeftTopRightBorderModified(false);
         this.cfRuleRecord.setTopLeftBottomRightBorderModified(false);
      }
   }

   public void setDiagonalBorderColor(Color color) {
      HSSFColor hcolor = HSSFColor.toHSSFColor(color);
      if (hcolor == null) {
         this.setDiagonalBorderColor((short)0);
      } else {
         this.setDiagonalBorderColor(hcolor.getIndex());
      }
   }

   public void setLeftBorderColor(short color) {
      this.borderFormatting.setLeftBorderColor(color);
      if (color != 0) {
         this.cfRuleRecord.setLeftBorderModified(true);
      } else {
         this.cfRuleRecord.setLeftBorderModified(false);
      }
   }

   public void setLeftBorderColor(Color color) {
      HSSFColor hcolor = HSSFColor.toHSSFColor(color);
      if (hcolor == null) {
         this.setLeftBorderColor((short)0);
      } else {
         this.setLeftBorderColor(hcolor.getIndex());
      }
   }

   public void setRightBorderColor(short color) {
      this.borderFormatting.setRightBorderColor(color);
      if (color != 0) {
         this.cfRuleRecord.setRightBorderModified(true);
      } else {
         this.cfRuleRecord.setRightBorderModified(false);
      }
   }

   public void setRightBorderColor(Color color) {
      HSSFColor hcolor = HSSFColor.toHSSFColor(color);
      if (hcolor == null) {
         this.setRightBorderColor((short)0);
      } else {
         this.setRightBorderColor(hcolor.getIndex());
      }
   }

   public void setTopBorderColor(short color) {
      this.borderFormatting.setTopBorderColor(color);
      if (color != 0) {
         this.cfRuleRecord.setTopBorderModified(true);
      } else {
         this.cfRuleRecord.setTopBorderModified(false);
      }
   }

   public void setTopBorderColor(Color color) {
      HSSFColor hcolor = HSSFColor.toHSSFColor(color);
      if (hcolor == null) {
         this.setTopBorderColor((short)0);
      } else {
         this.setTopBorderColor(hcolor.getIndex());
      }
   }

   public BorderStyle getBorderVertical() {
      return BorderStyle.NONE;
   }

   public BorderStyle getBorderHorizontal() {
      return BorderStyle.NONE;
   }

   public BorderStyle getBorderBottomEnum() {
      return this.getBorderBottom();
   }

   public BorderStyle getBorderDiagonalEnum() {
      return this.getBorderDiagonal();
   }

   public BorderStyle getBorderLeftEnum() {
      return this.getBorderLeft();
   }

   public BorderStyle getBorderRightEnum() {
      return this.getBorderRight();
   }

   public BorderStyle getBorderTopEnum() {
      return this.getBorderTop();
   }

   @Deprecated
   public BorderStyle getBorderVerticalEnum() {
      return this.getBorderVertical();
   }

   @Deprecated
   public BorderStyle getBorderHorizontalEnum() {
      return this.getBorderHorizontal();
   }

   public short getVerticalBorderColor() {
      return HSSFColorPredefined.AUTOMATIC.getIndex();
   }

   public Color getVerticalBorderColorColor() {
      return HSSFColorPredefined.AUTOMATIC.getColor();
   }

   public short getHorizontalBorderColor() {
      return HSSFColorPredefined.AUTOMATIC.getIndex();
   }

   public Color getHorizontalBorderColorColor() {
      return HSSFColorPredefined.AUTOMATIC.getColor();
   }

   public void setBorderHorizontal(BorderStyle border) {
   }

   public void setBorderVertical(BorderStyle border) {
   }

   public void setHorizontalBorderColor(short color) {
   }

   public void setHorizontalBorderColor(Color color) {
   }

   public void setVerticalBorderColor(short color) {
   }

   public void setVerticalBorderColor(Color color) {
   }
}
