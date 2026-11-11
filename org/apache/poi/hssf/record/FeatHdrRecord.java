package org.apache.poi.hssf.record;

import org.apache.poi.hssf.record.common.FtrHeader;
import org.apache.poi.util.LittleEndianOutput;

public final class FeatHdrRecord extends StandardRecord implements Cloneable {
   public static final int SHAREDFEATURES_ISFPROTECTION = 2;
   public static final int SHAREDFEATURES_ISFFEC2 = 3;
   public static final int SHAREDFEATURES_ISFFACTOID = 4;
   public static final int SHAREDFEATURES_ISFLIST = 5;
   public static final short sid = 2151;
   private FtrHeader futureHeader;
   private int isf_sharedFeatureType;
   private byte reserved;
   private long cbHdrData;
   private byte[] rgbHdrData;

   public FeatHdrRecord() {
      this.futureHeader = new FtrHeader();
      this.futureHeader.setRecordType((short)2151);
   }

   public short getSid() {
      return 2151;
   }

   public FeatHdrRecord(RecordInputStream in) {
      this.futureHeader = new FtrHeader(in);
      this.isf_sharedFeatureType = in.readShort();
      this.reserved = in.readByte();
      this.cbHdrData = in.readInt();
      this.rgbHdrData = in.readRemainder();
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[FEATURE HEADER]\n");
      buffer.append("[/FEATURE HEADER]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      this.futureHeader.serialize(out);
      out.writeShort(this.isf_sharedFeatureType);
      out.writeByte(this.reserved);
      out.writeInt((int)this.cbHdrData);
      out.write(this.rgbHdrData);
   }

   protected int getDataSize() {
      return 19 + this.rgbHdrData.length;
   }

   public FeatHdrRecord clone() {
      return (FeatHdrRecord)this.cloneViaReserialise();
   }
}
