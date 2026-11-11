package org.apache.poi.hssf.record;

public abstract class RecordBase {
   public abstract int serialize(int var1, byte[] var2);

   public abstract int getRecordSize();
}
