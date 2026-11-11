package org.apache.poi.util;

public interface LittleEndianInput {
   int available();

   byte readByte();

   int readUByte();

   short readShort();

   int readUShort();

   int readInt();

   long readLong();

   double readDouble();

   void readFully(byte[] var1);

   void readFully(byte[] var1, int var2, int var3);

   void readPlain(byte[] var1, int var2, int var3);
}
