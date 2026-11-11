package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class SaveRecalcRecord extends StandardRecord {
   public static final short sid = 95;
   private short field_1_recalc;

   public SaveRecalcRecord() {
   }

   public SaveRecalcRecord(RecordInputStream in) {
      this.field_1_recalc = in.readShort();
   }

   public void setRecalc(boolean recalc) {
      this.field_1_recalc = (short)(recalc ? 1 : 0);
   }

   public boolean getRecalc() {
      return this.field_1_recalc == 1;
   }

   public String toString() {
      return "[SAVERECALC]\n    .recalc         = " + this.getRecalc() + "\n[/SAVERECALC]\n";
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.field_1_recalc);
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 95;
   }

   public Object clone() {
      SaveRecalcRecord rec = new SaveRecalcRecord();
      rec.field_1_recalc = this.field_1_recalc;
      return rec;
   }
}
