package org.apache.poi.hssf.record;

import java.util.Iterator;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.hssf.record.cont.ContinuableRecord;
import org.apache.poi.hssf.record.cont.ContinuableRecordOutput;
import org.apache.poi.util.IntMapper;

public final class SSTRecord extends ContinuableRecord {
   public static final short sid = 252;
   private static final UnicodeString EMPTY_STRING = new UnicodeString("");
   static final int STD_RECORD_OVERHEAD = 4;
   static final int SST_RECORD_OVERHEAD = 12;
   static final int MAX_DATA_SPACE = 8216;
   private int field_1_num_strings;
   private int field_2_num_unique_strings;
   private IntMapper<UnicodeString> field_3_strings;
   private SSTDeserializer deserializer;
   int[] bucketAbsoluteOffsets;
   int[] bucketRelativeOffsets;

   public SSTRecord() {
      this.field_1_num_strings = 0;
      this.field_2_num_unique_strings = 0;
      this.field_3_strings = new IntMapper();
      this.deserializer = new SSTDeserializer(this.field_3_strings);
   }

   public int addString(UnicodeString string) {
      this.field_1_num_strings++;
      UnicodeString ucs = string == null ? EMPTY_STRING : string;
      int index = this.field_3_strings.getIndex(ucs);
      int rval;
      if (index != -1) {
         rval = index;
      } else {
         rval = this.field_3_strings.size();
         this.field_2_num_unique_strings++;
         SSTDeserializer.addToStringTable(this.field_3_strings, ucs);
      }

      return rval;
   }

   public int getNumStrings() {
      return this.field_1_num_strings;
   }

   public int getNumUniqueStrings() {
      return this.field_2_num_unique_strings;
   }

   public UnicodeString getString(int id) {
      return (UnicodeString)this.field_3_strings.get(id);
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[SST]\n");
      buffer.append("    .numstrings     = ").append(Integer.toHexString(this.getNumStrings())).append("\n");
      buffer.append("    .uniquestrings  = ").append(Integer.toHexString(this.getNumUniqueStrings())).append("\n");

      for (int k = 0; k < this.field_3_strings.size(); k++) {
         UnicodeString s = (UnicodeString)this.field_3_strings.get(k);
         buffer.append("    .string_" + k + "      = ").append(s.getDebugInfo()).append("\n");
      }

      buffer.append("[/SST]\n");
      return buffer.toString();
   }

   public short getSid() {
      return 252;
   }

   public SSTRecord(RecordInputStream in) {
      this.field_1_num_strings = in.readInt();
      this.field_2_num_unique_strings = in.readInt();
      this.field_3_strings = new IntMapper();
      this.deserializer = new SSTDeserializer(this.field_3_strings);
      if (this.field_1_num_strings == 0) {
         this.field_2_num_unique_strings = 0;
      } else {
         this.deserializer.manufactureStrings(this.field_2_num_unique_strings, in);
      }
   }

   Iterator<UnicodeString> getStrings() {
      return this.field_3_strings.iterator();
   }

   int countStrings() {
      return this.field_3_strings.size();
   }

   protected void serialize(ContinuableRecordOutput out) {
      SSTSerializer serializer = new SSTSerializer(this.field_3_strings, this.getNumStrings(), this.getNumUniqueStrings());
      serializer.serialize(out);
      this.bucketAbsoluteOffsets = serializer.getBucketAbsoluteOffsets();
      this.bucketRelativeOffsets = serializer.getBucketRelativeOffsets();
   }

   SSTDeserializer getDeserializer() {
      return this.deserializer;
   }

   public ExtSSTRecord createExtSSTRecord(int sstOffset) {
      if (this.bucketAbsoluteOffsets != null && this.bucketRelativeOffsets != null) {
         ExtSSTRecord extSST = new ExtSSTRecord();
         extSST.setNumStringsPerBucket((short)8);
         int[] absoluteOffsets = (int[])this.bucketAbsoluteOffsets.clone();
         int[] relativeOffsets = (int[])this.bucketRelativeOffsets.clone();

         for (int i = 0; i < absoluteOffsets.length; i++) {
            absoluteOffsets[i] += sstOffset;
         }

         extSST.setBucketOffsets(absoluteOffsets, relativeOffsets);
         return extSST;
      } else {
         throw new IllegalStateException("SST record has not yet been serialized.");
      }
   }

   public int calcExtSSTRecordSize() {
      return ExtSSTRecord.getRecordSizeForStrings(this.field_3_strings.size());
   }
}
