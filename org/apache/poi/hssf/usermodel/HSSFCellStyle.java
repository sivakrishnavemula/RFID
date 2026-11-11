package org.apache.poi.hssf.usermodel;

import java.util.List;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.record.StyleRecord;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

public final class HSSFCellStyle implements CellStyle {
   private final ExtendedFormatRecord _format;
   private final short _index;
   private final InternalWorkbook _workbook;
   private static final ThreadLocal<Short> lastDateFormat = new ThreadLocal<Short>() {
      protected Short initialValue() {
         return (short)-32768;
      }
   };
   private static final ThreadLocal<List<FormatRecord>> lastFormats = new ThreadLocal();
   private static final ThreadLocal<String> getDataFormatStringCache = new ThreadLocal();

   protected HSSFCellStyle(short index, ExtendedFormatRecord rec, HSSFWorkbook workbook) {
      this(index, rec, workbook.getWorkbook());
   }

   protected HSSFCellStyle(short index, ExtendedFormatRecord rec, InternalWorkbook workbook) {
      this._workbook = workbook;
      this._index = index;
      this._format = rec;
   }

   public short getIndex() {
      return this._index;
   }

   public HSSFCellStyle getParentStyle() {
      short parentIndex = this._format.getParentIndex();
      return parentIndex != 0 && parentIndex != 4095 ? new HSSFCellStyle(parentIndex, this._workbook.getExFormatAt(parentIndex), this._workbook) : null;
   }

   public void setDataFormat(short fmt) {
      this._format.setFormatIndex(fmt);
   }

   public short getDataFormat() {
      return this._format.getFormatIndex();
   }

   public String getDataFormatString() {
      if (getDataFormatStringCache.get() != null
         && (Short)lastDateFormat.get() == this.getDataFormat()
         && this._workbook.getFormats().equals(lastFormats.get())) {
         return (String)getDataFormatStringCache.get();
      }

      lastFormats.set(this._workbook.getFormats());
      lastDateFormat.set(this.getDataFormat());
      getDataFormatStringCache.set(this.getDataFormatString(this._workbook));
      return (String)getDataFormatStringCache.get();
   }

   public String getDataFormatString(Workbook workbook) {
      HSSFDataFormat format = new HSSFDataFormat(((HSSFWorkbook)workbook).getWorkbook());
      int idx = this.getDataFormat();
      return idx == -1 ? "General" : format.getFormat(this.getDataFormat());
   }

   public String getDataFormatString(InternalWorkbook workbook) {
      HSSFDataFormat format = new HSSFDataFormat(workbook);
      return format.getFormat(this.getDataFormat());
   }

   public void setFont(Font font) {
      this.setFont((HSSFFont)font);
   }

   public void setFont(HSSFFont font) {
      this._format.setIndentNotParentFont(true);
      short fontindex = font.getIndex();
      this._format.setFontIndex(fontindex);
   }

   @Deprecated
   public short getFontIndex() {
      return this._format.getFontIndex();
   }

   public int getFontIndexAsInt() {
      return this._format.getFontIndex();
   }

   public HSSFFont getFont(Workbook parentWorkbook) {
      return ((HSSFWorkbook)parentWorkbook).getFontAt(this.getFontIndexAsInt());
   }

   public void setHidden(boolean hidden) {
      this._format.setIndentNotParentCellOptions(true);
      this._format.setHidden(hidden);
   }

   public boolean getHidden() {
      return this._format.isHidden();
   }

   public void setLocked(boolean locked) {
      this._format.setIndentNotParentCellOptions(true);
      this._format.setLocked(locked);
   }

   public boolean getLocked() {
      return this._format.isLocked();
   }

   public void setQuotePrefixed(boolean quotePrefix) {
      this._format.set123Prefix(quotePrefix);
   }

   public boolean getQuotePrefixed() {
      return this._format.get123Prefix();
   }

   public void setAlignment(HorizontalAlignment align) {
      this._format.setIndentNotParentAlignment(true);
      this._format.setAlignment(align.getCode());
   }

   public HorizontalAlignment getAlignment() {
      return HorizontalAlignment.forInt(this._format.getAlignment());
   }

   public HorizontalAlignment getAlignmentEnum() {
      return this.getAlignment();
   }

   public void setWrapText(boolean wrapped) {
      this._format.setIndentNotParentAlignment(true);
      this._format.setWrapText(wrapped);
   }

   public boolean getWrapText() {
      return this._format.getWrapText();
   }

   public void setVerticalAlignment(VerticalAlignment align) {
      this._format.setVerticalAlignment(align.getCode());
   }

   public VerticalAlignment getVerticalAlignment() {
      return VerticalAlignment.forInt(this._format.getVerticalAlignment());
   }

   public VerticalAlignment getVerticalAlignmentEnum() {
      return this.getVerticalAlignment();
   }

   public void setRotation(short rotation) {
      if (rotation != 255) {
         if (rotation < 0 && rotation >= -90) {
            rotation = (short)(90 - rotation);
         } else if ((rotation <= 90 || rotation > 180) && (rotation < -90 || rotation > 90)) {
            throw new IllegalArgumentException("The rotation must be between -90 and 90 degrees, or 0xff");
         }
      }

      this._format.setRotation(rotation);
   }

   public short getRotation() {
      short rotation = this._format.getRotation();
      if (rotation == 255) {
         return rotation;
      }

      if (rotation > 90) {
         rotation = (short)(90 - rotation);
      }

      return rotation;
   }

   public void setIndention(short indent) {
      this._format.setIndent(indent);
   }

   public short getIndention() {
      return this._format.getIndent();
   }

   public void setBorderLeft(BorderStyle border) {
      this._format.setIndentNotParentBorder(true);
      this._format.setBorderLeft(border.getCode());
   }

   public BorderStyle getBorderLeft() {
      return BorderStyle.valueOf(this._format.getBorderLeft());
   }

   public BorderStyle getBorderLeftEnum() {
      return this.getBorderLeft();
   }

   public void setBorderRight(BorderStyle border) {
      this._format.setIndentNotParentBorder(true);
      this._format.setBorderRight(border.getCode());
   }

   public BorderStyle getBorderRight() {
      return BorderStyle.valueOf(this._format.getBorderRight());
   }

   public BorderStyle getBorderRightEnum() {
      return this.getBorderRight();
   }

   public void setBorderTop(BorderStyle border) {
      this._format.setIndentNotParentBorder(true);
      this._format.setBorderTop(border.getCode());
   }

   public BorderStyle getBorderTop() {
      return BorderStyle.valueOf(this._format.getBorderTop());
   }

   public BorderStyle getBorderTopEnum() {
      return this.getBorderTop();
   }

   public void setBorderBottom(BorderStyle border) {
      this._format.setIndentNotParentBorder(true);
      this._format.setBorderBottom(border.getCode());
   }

   public BorderStyle getBorderBottom() {
      return BorderStyle.valueOf(this._format.getBorderBottom());
   }

   public BorderStyle getBorderBottomEnum() {
      return this.getBorderBottom();
   }

   public void setLeftBorderColor(short color) {
      this._format.setLeftBorderPaletteIdx(color);
   }

   public short getLeftBorderColor() {
      return this._format.getLeftBorderPaletteIdx();
   }

   public void setRightBorderColor(short color) {
      this._format.setRightBorderPaletteIdx(color);
   }

   public short getRightBorderColor() {
      return this._format.getRightBorderPaletteIdx();
   }

   public void setTopBorderColor(short color) {
      this._format.setTopBorderPaletteIdx(color);
   }

   public short getTopBorderColor() {
      return this._format.getTopBorderPaletteIdx();
   }

   public void setBottomBorderColor(short color) {
      this._format.setBottomBorderPaletteIdx(color);
   }

   public short getBottomBorderColor() {
      return this._format.getBottomBorderPaletteIdx();
   }

   public void setFillPattern(FillPatternType fp) {
      this._format.setAdtlFillPattern(fp.getCode());
   }

   public FillPatternType getFillPattern() {
      return FillPatternType.forInt(this._format.getAdtlFillPattern());
   }

   public FillPatternType getFillPatternEnum() {
      return this.getFillPattern();
   }

   private void checkDefaultBackgroundFills() {
      short autoIdx = HSSFColorPredefined.AUTOMATIC.getIndex();
      if (this._format.getFillForeground() == autoIdx) {
         if (this._format.getFillBackground() != autoIdx + 1) {
            this.setFillBackgroundColor((short)(autoIdx + 1));
         }
      } else if (this._format.getFillBackground() == autoIdx + 1 && this._format.getFillForeground() != autoIdx) {
         this.setFillBackgroundColor(autoIdx);
      }
   }

   public void setFillBackgroundColor(short bg) {
      this._format.setFillBackground(bg);
      this.checkDefaultBackgroundFills();
   }

   public short getFillBackgroundColor() {
      short autoIndex = HSSFColorPredefined.AUTOMATIC.getIndex();
      short result = this._format.getFillBackground();
      return result == autoIndex + 1 ? autoIndex : result;
   }

   public HSSFColor getFillBackgroundColorColor() {
      HSSFPalette pallette = new HSSFPalette(this._workbook.getCustomPalette());
      return pallette.getColor(this.getFillBackgroundColor());
   }

   public void setFillForegroundColor(short bg) {
      this._format.setFillForeground(bg);
      this.checkDefaultBackgroundFills();
   }

   public short getFillForegroundColor() {
      return this._format.getFillForeground();
   }

   public HSSFColor getFillForegroundColorColor() {
      HSSFPalette pallette = new HSSFPalette(this._workbook.getCustomPalette());
      return pallette.getColor(this.getFillForegroundColor());
   }

   public String getUserStyleName() {
      StyleRecord sr = this._workbook.getStyleRecord(this._index);
      if (sr == null) {
         return null;
      } else {
         return sr.isBuiltin() ? null : sr.getName();
      }
   }

   public void setUserStyleName(String styleName) {
      StyleRecord sr = this._workbook.getStyleRecord(this._index);
      if (sr == null) {
         sr = this._workbook.createStyleRecord(this._index);
      }

      if (sr.isBuiltin() && this._index <= 20) {
         throw new IllegalArgumentException("Unable to set user specified style names for built in styles!");
      }

      sr.setName(styleName);
   }

   public void setShrinkToFit(boolean shrinkToFit) {
      this._format.setShrinkToFit(shrinkToFit);
   }

   public boolean getShrinkToFit() {
      return this._format.getShrinkToFit();
   }

   public short getReadingOrder() {
      return this._format.getReadingOrder();
   }

   public void setReadingOrder(short order) {
      this._format.setReadingOrder(order);
   }

   public void verifyBelongsToWorkbook(HSSFWorkbook wb) {
      if (wb.getWorkbook() != this._workbook) {
         throw new IllegalArgumentException(
            "This Style does not belong to the supplied Workbook. Are you trying to assign a style from one workbook to the cell of a differnt workbook?"
         );
      }
   }

   public void cloneStyleFrom(CellStyle source) {
      if (source instanceof HSSFCellStyle) {
         this.cloneStyleFrom((HSSFCellStyle)source);
      } else {
         throw new IllegalArgumentException("Can only clone from one HSSFCellStyle to another, not between HSSFCellStyle and XSSFCellStyle");
      }
   }

   public void cloneStyleFrom(HSSFCellStyle source) {
      this._format.cloneStyleFrom(source._format);
      if (this._workbook != source._workbook) {
         lastDateFormat.set((short)-32768);
         lastFormats.set(null);
         getDataFormatStringCache.set(null);
         short fmt = (short)this._workbook.createFormat(source.getDataFormatString());
         this.setDataFormat(fmt);
         FontRecord fr = this._workbook.createNewFont();
         fr.cloneStyleFrom(source._workbook.getFontRecordAt(source.getFontIndexAsInt()));
         HSSFFont font = new HSSFFont((short)this._workbook.getFontIndex(fr), fr);
         this.setFont(font);
      }
   }

   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this._format == null ? 0 : this._format.hashCode());
      return 31 * result + this._index;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (obj instanceof HSSFCellStyle) {
         HSSFCellStyle other = (HSSFCellStyle)obj;
         if (this._format == null) {
            if (other._format != null) {
               return false;
            }
         } else if (!this._format.equals(other._format)) {
            return false;
         }

         return this._index == other._index;
      } else {
         return false;
      }
   }
}
