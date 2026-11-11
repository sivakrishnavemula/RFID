package com.microsoft.sqlserver.jdbc;

final class StreamRetValue extends StreamPacket {
   private String paramName;
   private int ordinalOrLength;
   private int status;

   final int getOrdinalOrLength() {
      return this.ordinalOrLength;
   }

   StreamRetValue() {
      super(172);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (172 != var1.readUnsignedByte()) {
         assert false;
      }

      this.ordinalOrLength = var1.readUnsignedShort();
      this.paramName = var1.readUnicodeString(var1.readUnsignedByte());
      this.status = var1.readUnsignedByte();
   }

   CryptoMetadata getCryptoMetadata(TDSReader var1) throws SQLServerException {
      return new StreamColumns().readCryptoMetadata(var1);
   }
}
