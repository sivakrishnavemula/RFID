package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;

final class AsciiFilteredUnicodeInputStream extends InputStream {
   private final Reader containedReader;
   private final Charset asciiCharSet;
   private final byte[] bSingleByte = new byte[1];

   static AsciiFilteredUnicodeInputStream MakeAsciiFilteredUnicodeInputStream(BaseInputStream var0, Reader var1) throws SQLServerException {
      if (BaseInputStream.logger.isLoggable(Level.FINER)) {
         BaseInputStream.logger.finer(var0.toString() + " wrapping in AsciiFilteredInputStream");
      }

      return new AsciiFilteredUnicodeInputStream(var1);
   }

   private AsciiFilteredUnicodeInputStream(Reader var1) throws SQLServerException {
      this.containedReader = var1;
      this.asciiCharSet = Charset.forName("US-ASCII");
   }

   public void close() throws IOException {
      this.containedReader.close();
   }

   public long skip(long var1) throws IOException {
      return this.containedReader.skip(var1);
   }

   public int available() throws IOException {
      return 0;
   }

   public int read() throws IOException {
      int var1 = this.read(this.bSingleByte);
      return -1 == var1 ? -1 : this.bSingleByte[0] & 0xFF;
   }

   public int read(byte[] var1) throws IOException {
      return this.read(var1, 0, var1.length);
   }

   public int read(byte[] var1, int var2, int var3) throws IOException {
      char[] var4 = new char[var3];
      int var5 = this.containedReader.read(var4);
      if (var5 > 0) {
         if (var5 < var3) {
            var3 = var5;
         }

         ByteBuffer var6 = this.asciiCharSet.encode(CharBuffer.wrap(var4));
         var6.get(var1, var2, var3);
      }

      return var5;
   }

   public boolean markSupported() {
      return this.containedReader.markSupported();
   }

   public void mark(int var1) {
      try {
         this.containedReader.mark(var1);
      } catch (IOException var3) {
      }
   }

   public void reset() throws IOException {
      this.containedReader.reset();
   }
}
