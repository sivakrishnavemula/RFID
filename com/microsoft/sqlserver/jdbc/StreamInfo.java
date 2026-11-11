package com.microsoft.sqlserver.jdbc;

final class StreamInfo extends StreamPacket {
   final StreamError msg = new StreamError();

   StreamInfo() {
      super(171);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (171 != var1.readUnsignedByte()) {
         assert false;
      }

      this.msg.setContentsFromTDS(var1);
   }
}
