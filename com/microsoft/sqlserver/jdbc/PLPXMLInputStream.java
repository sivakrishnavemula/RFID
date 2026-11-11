package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;

final class PLPXMLInputStream extends PLPInputStream {
   private static final byte[] xmlBOM = new byte[]{-1, -2};
   private final ByteArrayInputStream bomStream = new ByteArrayInputStream(xmlBOM);

   static final PLPXMLInputStream makeXMLStream(TDSReader var0, InputStreamGetterArgs var1, ServerDTVImpl var2) throws SQLServerException {
      long var3 = var0.readLong();
      if (-1L == var3) {
         return null;
      }

      PLPXMLInputStream var5 = new PLPXMLInputStream(var0, var3, var1, var2);
      if (null != var5) {
         var5.setLoggingInfo(var1.logContext);
      }

      return var5;
   }

   PLPXMLInputStream(TDSReader var1, long var2, InputStreamGetterArgs var4, ServerDTVImpl var5) throws SQLServerException {
      super(var1, var2, var4.isAdaptive, var4.isStreaming, var5);
   }

   public void close() throws IOException {
      super.close();
   }

   int readBytes(byte[] var1, int var2, int var3) throws IOException {
      assert var2 >= 0;
      assert var3 >= 0;
      if (0 == var3) {
         return 0;
      }

      int var4 = 0;
      int var5 = 0;
      if (null == var1) {
         int var6 = 0;

         while (var4 < var3 && 0 != (var6 = (int)this.bomStream.skip(var3 - var4))) {
            var4 += var6;
         }
      } else {
         int var8 = 0;

         while (var4 < var3 && -1 != (var8 = this.bomStream.read(var1, var2 + var4, var3 - var4))) {
            var4 += var8;
         }
      }

      while (var4 < var3 && -1 != (var5 = super.readBytes(var1, var2 + var4, var3 - var4))) {
         var4 += var5;
      }

      if (var4 > 0) {
         return var4;
      }

      assert -1 == var5;
      return -1;
   }

   public void mark(int var1) {
      this.bomStream.mark(xmlBOM.length);
      super.mark(var1);
   }

   public void reset() throws IOException {
      this.bomStream.reset();
      super.reset();
   }

   byte[] getBytes() throws SQLServerException {
      byte[] var1 = new byte[2];

      try {
         int var2 = this.bomStream.read(var1);
         byte[] var3 = super.getBytes();
         if (var2 > 0) {
            assert 2 == var2;
            byte[] var4 = new byte[var3.length + var2];
            System.arraycopy(var1, 0, var4, 0, var2);
            System.arraycopy(var3, 0, var4, var2, var3.length);
            return var4;
         } else {
            return var3;
         }
      } catch (IOException var5) {
         SQLServerException.makeFromDriverError(null, null, var5.getMessage(), null, true);
         return null;
      }
   }
}
