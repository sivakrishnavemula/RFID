package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class DeltaRecord extends StandardRecord implements Cloneable {
   public static final short sid = 16;
   public static final double DEFAULT_VALUE = 0.001;
   private double field_1_max_change;

   public DeltaRecord(double maxChange) {
      this.field_1_max_change = maxChange;
   }

   public DeltaRecord(RecordInputStream in) {
      this.field_1_max_change = in.readDouble();
   }

   public double getMaxChange() {
      return this.field_1_max_change;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[DELTA]\n");
      buffer.append("    .maxchange = ").append(this.getMaxChange()).append("\n");
      buffer.append("[/DELTA]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeDouble(this.getMaxChange());
   }

   protected int getDataSize() {
      return 8;
   }

   public short getSid() {
      return 16;
   }

   public DeltaRecord clone() {
      return this;
   }
}
