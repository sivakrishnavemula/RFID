package org.apache.poi.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LittleEndianInputStream extends FilterInputStream implements LittleEndianInput {
   private static final int EOF = -1;

   public LittleEndianInputStream(InputStream is) {
      super(is);
   }

   @SuppressForbidden("just delegating")
   public int available() {
      try {
         return super.available();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public byte readByte() {
      return (byte)this.readUByte();
   }

   public int readUByte() {
      byte[] buf = new byte[1];

      try {
         checkEOF(this.read(buf), 1);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return LittleEndian.getUByte(buf);
   }

   public double readDouble() {
      return Double.longBitsToDouble(this.readLong());
   }

   public int readInt() {
      byte[] buf = new byte[4];

      try {
         checkEOF(this.read(buf), buf.length);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return LittleEndian.getInt(buf);
   }

   public long readUInt() {
      long retNum = this.readInt();
      return retNum & 4294967295L;
   }

   public long readLong() {
      byte[] buf = new byte[8];

      try {
         checkEOF(this.read(buf), 8);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return LittleEndian.getLong(buf);
   }

   public short readShort() {
      return (short)this.readUShort();
   }

   public int readUShort() {
      byte[] buf = new byte[2];

      try {
         checkEOF(this.read(buf), 2);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return LittleEndian.getUShort(buf);
   }

   private static void checkEOF(int actualBytes, int expectedBytes) {
      if (expectedBytes != 0 && (actualBytes == -1 || actualBytes != expectedBytes)) {
         throw new RuntimeException("Unexpected end-of-file");
      }
   }

   public void readFully(byte[] buf) {
      this.readFully(buf, 0, buf.length);
   }

   public void readFully(byte[] buf, int off, int len) {
      try {
         checkEOF(this._read(buf, off, len), len);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private int _read(byte[] buffer, int offset, int length) throws IOException {
      int remaining = length;

      while (remaining > 0) {
         int location = length - remaining;
         int count = this.read(buffer, offset + location, remaining);
         if (-1 == count) {
            break;
         }

         remaining -= count;
      }

      return length - remaining;
   }

   public void readPlain(byte[] buf, int off, int len) {
      this.readFully(buf, off, len);
   }
}
