package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.util.logging.Level;

final class SimpleInputStream extends BaseInputStream {
   private final int payloadLength;
   private byte[] bSingleByte;

   SimpleInputStream(TDSReader var1, int var2, InputStreamGetterArgs var3, ServerDTVImpl var4) throws SQLServerException {
      super(var1, var3.isAdaptive, var3.isStreaming, var4);
      this.setLoggingInfo(var3.logContext);
      this.payloadLength = var2;
   }

   public void close() throws IOException {
      if (null != this.tdsReader) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + "Enter Closing SimpleInputStream.");
         }

         this.skip(this.payloadLength - this.streamPos);
         this.closeHelper();
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + "Exit Closing SimpleInputStream.");
         }
      }
   }

   private final boolean isEOS() throws IOException {
      assert this.streamPos <= this.payloadLength;
      return this.streamPos == this.payloadLength;
   }

   public long skip(long var1) throws IOException {
      this.checkClosed();
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " Skipping :" + var1);
      }

      if (var1 < 0L) {
         return 0L;
      }

      if (this.isEOS()) {
         return 0L;
      }

      int var3;
      if (this.streamPos + var1 > this.payloadLength) {
         var3 = this.payloadLength - this.streamPos;
      } else {
         var3 = (int)var1;
      }

      try {
         this.tdsReader.skip(var3);
      } catch (SQLServerException var5) {
         throw new IOException(var5.getMessage());
      }

      this.streamPos += var3;
      if (this.isReadLimitSet && this.streamPos - this.markedStreamPos > this.readLimit) {
         this.clearCurrentMark();
      }

      return var3;
   }

   public int available() throws IOException {
      this.checkClosed();
      assert this.streamPos <= this.payloadLength;
      int var1 = this.payloadLength - this.streamPos;
      if (this.tdsReader.available() < var1) {
         var1 = this.tdsReader.available();
      }

      return var1;
   }

   public int read() throws IOException {
      this.checkClosed();
      if (null == this.bSingleByte) {
         this.bSingleByte = new byte[1];
      }

      if (this.isEOS()) {
         return -1;
      }

      int var1 = this.read(this.bSingleByte, 0, 1);
      return 0 == var1 ? -1 : this.bSingleByte[0] & 0xFF;
   }

   public int read(byte[] var1) throws IOException {
      this.checkClosed();
      return this.read(var1, 0, var1.length);
   }

   public int read(byte[] var1, int var2, int var3) throws IOException {
      this.checkClosed();
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " Reading " + var3 + " from stream offset " + this.streamPos + " payload length " + this.payloadLength);
      }

      if (var2 < 0 || var3 < 0 || var2 + var3 > var1.length) {
         throw new IndexOutOfBoundsException();
      }

      if (0 == var3) {
         return 0;
      }

      if (this.isEOS()) {
         return -1;
      }

      int var4 = 0;
      if (this.streamPos + var3 > this.payloadLength) {
         var4 = this.payloadLength - this.streamPos;
      } else {
         var4 = var3;
      }

      try {
         this.tdsReader.readBytes(var1, var2, var4);
      } catch (SQLServerException var6) {
         throw new IOException(var6.getMessage());
      }

      this.streamPos += var4;
      if (this.isReadLimitSet && this.streamPos - this.markedStreamPos > this.readLimit) {
         this.clearCurrentMark();
      }

      return var4;
   }

   public void mark(int var1) {
      if (null != this.tdsReader && var1 > 0) {
         this.currentMark = this.tdsReader.mark();
         this.markedStreamPos = this.streamPos;
         this.setReadLimit(var1);
      }
   }

   public void reset() throws IOException {
      this.resetHelper();
      this.streamPos = this.markedStreamPos;
   }

   final byte[] getBytes() throws SQLServerException {
      assert 0 == this.streamPos;
      byte[] var1 = new byte[this.payloadLength];

      try {
         this.read(var1);
         this.close();
      } catch (IOException var3) {
         SQLServerException.makeFromDriverError(null, null, var3.getMessage(), null, true);
      }

      return var1;
   }
}
