package org.apache.poi.hssf.record.chart;

import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.LittleEndianOutput;

public class ChartTitleFormatRecord extends StandardRecord {
   public static final short sid = 4176;
   private ChartTitleFormatRecord.CTFormat[] _formats;

   public ChartTitleFormatRecord(RecordInputStream in) {
      int nRecs = in.readUShort();
      this._formats = new ChartTitleFormatRecord.CTFormat[nRecs];

      for (int i = 0; i < nRecs; i++) {
         this._formats[i] = new ChartTitleFormatRecord.CTFormat(in);
      }
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this._formats.length);

      for (int i = 0; i < this._formats.length; i++) {
         this._formats[i].serialize(out);
      }
   }

   protected int getDataSize() {
      return 2 + 4 * this._formats.length;
   }

   public short getSid() {
      return 4176;
   }

   public int getFormatCount() {
      return this._formats.length;
   }

   public void modifyFormatRun(short oldPos, short newLen) {
      int shift = 0;

      for (int i = 0; i < this._formats.length; i++) {
         ChartTitleFormatRecord.CTFormat ctf = this._formats[i];
         if (shift != 0) {
            ctf.setOffset(ctf.getOffset() + shift);
         } else if (oldPos == ctf.getOffset() && i < this._formats.length - 1) {
            ChartTitleFormatRecord.CTFormat nextCTF = this._formats[i + 1];
            shift = newLen - (nextCTF.getOffset() - ctf.getOffset());
         }
      }
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[CHARTTITLEFORMAT]\n");
      buffer.append("    .format_runs       = ").append(this._formats.length).append("\n");

      for (int i = 0; i < this._formats.length; i++) {
         ChartTitleFormatRecord.CTFormat ctf = this._formats[i];
         buffer.append("       .char_offset= ").append(ctf.getOffset());
         buffer.append(",.fontidx= ").append(ctf.getFontIndex());
         buffer.append("\n");
      }

      buffer.append("[/CHARTTITLEFORMAT]\n");
      return buffer.toString();
   }

   private static final class CTFormat {
      public static final int ENCODED_SIZE = 4;
      private int _offset;
      private int _fontIndex;

      public CTFormat(RecordInputStream in) {
         this._offset = in.readShort();
         this._fontIndex = in.readShort();
      }

      public int getOffset() {
         return this._offset;
      }

      public void setOffset(int newOff) {
         this._offset = newOff;
      }

      public int getFontIndex() {
         return this._fontIndex;
      }

      public void serialize(LittleEndianOutput out) {
         out.writeShort(this._offset);
         out.writeShort(this._fontIndex);
      }
   }
}
