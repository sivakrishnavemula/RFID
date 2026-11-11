package org.apache.poi.hssf.usermodel;

import java.util.Iterator;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.hssf.record.common.UnicodeString.FormatRun;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;

public final class HSSFRichTextString implements Comparable<HSSFRichTextString>, RichTextString {
   public static final short NO_FONT = 0;
   private UnicodeString _string;
   private InternalWorkbook _book;
   private LabelSSTRecord _record;

   public HSSFRichTextString() {
      this("");
   }

   public HSSFRichTextString(String string) {
      if (string == null) {
         this._string = new UnicodeString("");
      } else {
         this._string = new UnicodeString(string);
      }
   }

   HSSFRichTextString(InternalWorkbook book, LabelSSTRecord record) {
      this.setWorkbookReferences(book, record);
      this._string = book.getSSTString(record.getSSTIndex());
   }

   void setWorkbookReferences(InternalWorkbook book, LabelSSTRecord record) {
      this._book = book;
      this._record = record;
   }

   private UnicodeString cloneStringIfRequired() {
      return this._book == null ? this._string : (UnicodeString)this._string.clone();
   }

   private void addToSSTIfRequired() {
      if (this._book != null) {
         int index = this._book.addSSTString(this._string);
         this._record.setSSTIndex(index);
         this._string = this._book.getSSTString(index);
      }
   }

   public void applyFont(int startIndex, int endIndex, short fontIndex) {
      if (startIndex > endIndex) {
         throw new IllegalArgumentException("Start index must be less than end index.");
      }

      if (startIndex < 0 || endIndex > this.length()) {
         throw new IllegalArgumentException("Start and end index not in range.");
      }

      if (startIndex != endIndex) {
         short currentFont = 0;
         if (endIndex != this.length()) {
            currentFont = this.getFontAtIndex(endIndex);
         }

         this._string = this.cloneStringIfRequired();
         Iterator<FormatRun> formatting = this._string.formatIterator();
         if (formatting != null) {
            while (formatting.hasNext()) {
               FormatRun r = (FormatRun)formatting.next();
               if (r.getCharacterPos() >= startIndex && r.getCharacterPos() < endIndex) {
                  formatting.remove();
               }
            }
         }

         this._string.addFormatRun(new FormatRun((short)startIndex, fontIndex));
         if (endIndex != this.length()) {
            this._string.addFormatRun(new FormatRun((short)endIndex, currentFont));
         }

         this.addToSSTIfRequired();
      }
   }

   public void applyFont(int startIndex, int endIndex, Font font) {
      this.applyFont(startIndex, endIndex, font.getIndex());
   }

   public void applyFont(Font font) {
      this.applyFont(0, this._string.getCharCount(), font);
   }

   public void clearFormatting() {
      this._string = this.cloneStringIfRequired();
      this._string.clearFormatting();
      this.addToSSTIfRequired();
   }

   public String getString() {
      return this._string.getString();
   }

   UnicodeString getUnicodeString() {
      return this.cloneStringIfRequired();
   }

   UnicodeString getRawUnicodeString() {
      return this._string;
   }

   void setUnicodeString(UnicodeString str) {
      this._string = str;
   }

   public int length() {
      return this._string.getCharCount();
   }

   public short getFontAtIndex(int index) {
      int size = this._string.getFormatRunCount();
      FormatRun currentRun = null;

      for (int i = 0; i < size; i++) {
         FormatRun r = this._string.getFormatRun(i);
         if (r.getCharacterPos() > index) {
            break;
         }

         currentRun = r;
      }

      return currentRun == null ? 0 : currentRun.getFontIndex();
   }

   public int numFormattingRuns() {
      return this._string.getFormatRunCount();
   }

   public int getIndexOfFormattingRun(int index) {
      FormatRun r = this._string.getFormatRun(index);
      return r.getCharacterPos();
   }

   public short getFontOfFormattingRun(int index) {
      FormatRun r = this._string.getFormatRun(index);
      return r.getFontIndex();
   }

   public int compareTo(HSSFRichTextString r) {
      return this._string.compareTo(r._string);
   }

   public boolean equals(Object o) {
      return o instanceof HSSFRichTextString ? this._string.equals(((HSSFRichTextString)o)._string) : false;
   }

   public int hashCode() {
      assert false : "hashCode not designed";
      return 42;
   }

   public String toString() {
      return this._string.toString();
   }

   public void applyFont(short fontIndex) {
      this.applyFont(0, this._string.getCharCount(), fontIndex);
   }
}
