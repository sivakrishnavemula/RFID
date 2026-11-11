package org.apache.poi.util;

public interface LittleEndianOutput {
   void writeByte(int var1);

   void writeShort(int var1);

   void writeInt(int var1);

   void writeLong(long var1);

   void writeDouble(double var1);

   void write(byte[] var1);

   void write(byte[] var1, int var2, int var3);
}
