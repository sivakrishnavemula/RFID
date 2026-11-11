package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

final class SQLServerClobAsciiOutputStream extends OutputStream {
   private SQLServerClobBase parentClob = null;
   private long streamPos;
   private byte[] bSingleByte = new byte[1];

   SQLServerClobAsciiOutputStream(SQLServerClobBase var1, long var2) {
      this.parentClob = var1;
      this.streamPos = var2;
   }

   public void write(byte[] var1) throws IOException {
      if (null != var1) {
         this.write(var1, 0, var1.length);
      }
   }

   public void write(byte[] var1, int var2, int var3) throws IOException {
      if (null != var1) {
         try {
            String var4 = new String(var1, var2, var3, "US-ASCII");
            int var5 = this.parentClob.setString(this.streamPos, var4);
            this.streamPos += var5;
         } catch (SQLException var6) {
            throw new IOException(var6.getMessage());
         }
      }
   }

   public void write(int var1) throws IOException {
      this.bSingleByte[0] = (byte)(var1 & 0xFF);
      this.write(this.bSingleByte, 0, this.bSingleByte.length);
   }
}
