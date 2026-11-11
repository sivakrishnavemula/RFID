package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

final class SQLServerClobWriter extends Writer {
   private SQLServerClobBase parentClob = null;
   private long streamPos;

   SQLServerClobWriter(SQLServerClobBase var1, long var2) {
      this.parentClob = var1;
      this.streamPos = var2;
   }

   public void write(char[] var1) throws IOException {
      if (null != var1) {
         this.write(new String(var1));
      }
   }

   public void write(char[] var1, int var2, int var3) throws IOException {
      if (null != var1) {
         this.write(new String(var1, var2, var3));
      }
   }

   public void write(int var1) throws IOException {
      char[] var2 = new char[]{(char)var1};
      this.write(new String(var2));
   }

   public void write(String var1, int var2, int var3) throws IOException {
      this.checkClosed();

      try {
         int var4 = this.parentClob.setString(this.streamPos, var1, var2, var3);
         this.streamPos += var4;
      } catch (SQLException var5) {
         throw new IOException(var5.getMessage());
      }
   }

   public void write(String var1) throws IOException {
      if (null != var1) {
         this.write(var1, 0, var1.length());
      }
   }

   public void flush() throws IOException {
      this.checkClosed();
   }

   public void close() throws IOException {
      this.checkClosed();
      this.parentClob = null;
   }

   private void checkClosed() throws IOException {
      if (null == this.parentClob) {
         throw new IOException(SQLServerException.getErrString("R_streamIsClosed"));
      }
   }
}
