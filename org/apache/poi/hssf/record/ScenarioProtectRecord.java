package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class ScenarioProtectRecord extends StandardRecord {
   public static final short sid = 221;
   private short field_1_protect;

   public ScenarioProtectRecord() {
   }

   public ScenarioProtectRecord(RecordInputStream in) {
      this.field_1_protect = in.readShort();
   }

   public void setProtect(boolean protect) {
      if (protect) {
         this.field_1_protect = 1;
      } else {
         this.field_1_protect = 0;
      }
   }

   public boolean getProtect() {
      return this.field_1_protect == 1;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[SCENARIOPROTECT]\n");
      buffer.append("    .protect         = ").append(this.getProtect()).append("\n");
      buffer.append("[/SCENARIOPROTECT]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.field_1_protect);
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 221;
   }

   public Object clone() {
      ScenarioProtectRecord rec = new ScenarioProtectRecord();
      rec.field_1_protect = this.field_1_protect;
      return rec;
   }
}
