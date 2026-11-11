package com.microsoft.sqlserver.jdbc;

final class StreamLoginAck extends StreamPacket {
   String sSQLServerVersion;
   int tdsVersion;

   StreamLoginAck() {
      super(173);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (173 != var1.readUnsignedByte()) {
         assert false;
      }

      var1.readUnsignedShort();
      var1.readUnsignedByte();
      this.tdsVersion = var1.readIntBigEndian();
      var1.readUnicodeString(var1.readUnsignedByte());
      int var2 = var1.readUnsignedByte();
      int var3 = var1.readUnsignedByte();
      int var4 = var1.readUnsignedByte() << 8 | var1.readUnsignedByte();
      this.sSQLServerVersion = var2 + "." + (var3 <= 9 ? "0" : "") + var3 + "." + var4;
   }
}
