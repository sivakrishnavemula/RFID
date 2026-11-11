package com.microsoft.sqlserver.jdbc;

abstract class StreamPacket {
   int packetType;

   final int getTokenType() {
      return this.packetType;
   }

   StreamPacket() {
      this.packetType = 0;
   }

   StreamPacket(int var1) {
      this.packetType = var1;
   }

   abstract void setFromTDS(TDSReader var1) throws SQLServerException;
}
