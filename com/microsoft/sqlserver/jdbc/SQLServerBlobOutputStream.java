package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

final class SQLServerBlobOutputStream extends OutputStream {
   private SQLServerBlob parentBlob = null;
   private long currentPos;

   SQLServerBlobOutputStream(SQLServerBlob var1, long var2) {
      this.parentBlob = var1;
      this.currentPos = var2;
   }

   public void write(byte[] var1) throws IOException {
      if (null != var1) {
         this.write(var1, 0, var1.length);
      }
   }

   public void write(byte[] var1, int var2, int var3) throws IOException {
      try {
         int var4 = this.parentBlob.setBytes(this.currentPos, var1, var2, var3);
         this.currentPos += var4;
      } catch (SQLException var5) {
         throw new IOException(var5.getMessage());
      }
   }

   public void write(int var1) throws IOException {
      byte[] var2 = new byte[]{(byte)(var1 & 0xFF)};
      this.write(var2, 0, var2.length);
   }
}
