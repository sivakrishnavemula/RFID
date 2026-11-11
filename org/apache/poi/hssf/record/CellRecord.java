package org.apache.poi.hssf.record;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

public abstract class CellRecord extends StandardRecord implements CellValueRecordInterface {
   private int _rowIndex;
   private int _columnIndex;
   private int _formatIndex;

   protected CellRecord() {
   }

   protected CellRecord(RecordInputStream in) {
      this._rowIndex = in.readUShort();
      this._columnIndex = in.readUShort();
      this._formatIndex = in.readUShort();
   }

   public final void setRow(int row) {
      this._rowIndex = row;
   }

   public final void setColumn(short col) {
      this._columnIndex = col;
   }

   public final void setXFIndex(short xf) {
      this._formatIndex = xf;
   }

   public final int getRow() {
      return this._rowIndex;
   }

   public final short getColumn() {
      return (short)this._columnIndex;
   }

   public final short getXFIndex() {
      return (short)this._formatIndex;
   }

   public final String toString() {
      StringBuilder sb = new StringBuilder();
      String recordName = this.getRecordName();
      sb.append("[").append(recordName).append("]\n");
      sb.append("    .row    = ").append(HexDump.shortToHex(this.getRow())).append("\n");
      sb.append("    .col    = ").append(HexDump.shortToHex(this.getColumn())).append("\n");
      sb.append("    .xfindex= ").append(HexDump.shortToHex(this.getXFIndex())).append("\n");
      this.appendValueText(sb);
      sb.append("\n");
      sb.append("[/").append(recordName).append("]\n");
      return sb.toString();
   }

   protected abstract void appendValueText(StringBuilder var1);

   protected abstract String getRecordName();

   protected abstract void serializeValue(LittleEndianOutput var1);

   protected abstract int getValueDataSize();

   public final void serialize(LittleEndianOutput out) {
      out.writeShort(this.getRow());
      out.writeShort(this.getColumn());
      out.writeShort(this.getXFIndex());
      this.serializeValue(out);
   }

   protected final int getDataSize() {
      return 6 + this.getValueDataSize();
   }

   protected final void copyBaseFields(CellRecord rec) {
      rec._rowIndex = this._rowIndex;
      rec._columnIndex = this._columnIndex;
      rec._formatIndex = this._formatIndex;
   }
}
