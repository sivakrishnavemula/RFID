package com.microsoft.sqlserver.jdbc;

final class StreamRetStatus extends StreamPacket {
   private int status;

   final int getStatus() {
      return this.status;
   }

   StreamRetStatus() {
      super(121);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (121 != var1.readUnsignedByte()) {
         assert false;
      }

      this.status = var1.readInt();
   }
}
