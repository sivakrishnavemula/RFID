package org.apache.poi.hssf.record.cf;

import org.apache.poi.hssf.record.common.ExtendedColor;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public final class DataBarFormatting implements Cloneable {
   private static POILogger log = POILogFactory.getLogger(DataBarFormatting.class);
   private byte options;
   private byte percentMin;
   private byte percentMax;
   private ExtendedColor color;
   private DataBarThreshold thresholdMin;
   private DataBarThreshold thresholdMax;
   private static BitField iconOnly = BitFieldFactory.getInstance(1);
   private static BitField reversed = BitFieldFactory.getInstance(4);

   public DataBarFormatting() {
      this.options = 2;
   }

   public DataBarFormatting(LittleEndianInput in) {
      in.readShort();
      in.readByte();
      this.options = in.readByte();
      this.percentMin = in.readByte();
      this.percentMax = in.readByte();
      if (this.percentMin < 0 || this.percentMin > 100) {
         log.log(5, new Object[]{"Inconsistent Minimum Percentage found " + this.percentMin});
      }

      if (this.percentMax < 0 || this.percentMax > 100) {
         log.log(5, new Object[]{"Inconsistent Minimum Percentage found " + this.percentMin});
      }

      this.color = new ExtendedColor(in);
      this.thresholdMin = new DataBarThreshold(in);
      this.thresholdMax = new DataBarThreshold(in);
   }

   public boolean isIconOnly() {
      return this.getOptionFlag(iconOnly);
   }

   public void setIconOnly(boolean only) {
      this.setOptionFlag(only, iconOnly);
   }

   public boolean isReversed() {
      return this.getOptionFlag(reversed);
   }

   public void setReversed(boolean rev) {
      this.setOptionFlag(rev, reversed);
   }

   private boolean getOptionFlag(BitField field) {
      int value = field.getValue(this.options);
      return value != 0;
   }

   private void setOptionFlag(boolean option, BitField field) {
      this.options = field.setByteBoolean(this.options, option);
   }

   public byte getPercentMin() {
      return this.percentMin;
   }

   public void setPercentMin(byte percentMin) {
      this.percentMin = percentMin;
   }

   public byte getPercentMax() {
      return this.percentMax;
   }

   public void setPercentMax(byte percentMax) {
      this.percentMax = percentMax;
   }

   public ExtendedColor getColor() {
      return this.color;
   }

   public void setColor(ExtendedColor color) {
      this.color = color;
   }

   public DataBarThreshold getThresholdMin() {
      return this.thresholdMin;
   }

   public void setThresholdMin(DataBarThreshold thresholdMin) {
      this.thresholdMin = thresholdMin;
   }

   public DataBarThreshold getThresholdMax() {
      return this.thresholdMax;
   }

   public void setThresholdMax(DataBarThreshold thresholdMax) {
      this.thresholdMax = thresholdMax;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("    [Data Bar Formatting]\n");
      buffer.append("          .icon_only= ").append(this.isIconOnly()).append("\n");
      buffer.append("          .reversed = ").append(this.isReversed()).append("\n");
      buffer.append(this.color);
      buffer.append(this.thresholdMin);
      buffer.append(this.thresholdMax);
      buffer.append("    [/Data Bar Formatting]\n");
      return buffer.toString();
   }

   public Object clone() {
      DataBarFormatting rec = new DataBarFormatting();
      rec.options = this.options;
      rec.percentMin = this.percentMin;
      rec.percentMax = this.percentMax;
      rec.color = this.color.clone();
      rec.thresholdMin = this.thresholdMin.clone();
      rec.thresholdMax = this.thresholdMax.clone();
      return rec;
   }

   public int getDataLength() {
      return 6 + this.color.getDataLength() + this.thresholdMin.getDataLength() + this.thresholdMax.getDataLength();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(0);
      out.writeByte(0);
      out.writeByte(this.options);
      out.writeByte(this.percentMin);
      out.writeByte(this.percentMax);
      this.color.serialize(out);
      this.thresholdMin.serialize(out);
      this.thresholdMax.serialize(out);
   }
}
