package com.microsoft.sqlserver.jdbc;

final class StreamSSPI extends StreamPacket {
   byte[] sspiBlob;

   StreamSSPI() {
      super(237);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (237 != var1.readUnsignedByte()) {
         assert false;
      }

      int var2 = var1.readUnsignedShort();
      this.sspiBlob = new byte[var2];
      var1.readBytes(this.sspiBlob, 0, var2);
   }
}
