package com.microsoft.sqlserver.jdbc;

final class TDSPacket {
   final byte[] header = new byte[8];
   final byte[] payload;
   int payloadLength;
   volatile TDSPacket next;

   public final String toString() {
      return "TDSPacket(SPID:" + Util.readUnsignedShortBigEndian(this.header, 4) + " Seq:" + this.header[6] + ")";
   }

   TDSPacket(int var1) {
      this.payload = new byte[var1];
      this.payloadLength = 0;
      this.next = null;
   }

   final boolean isEOM() {
      return 1 == (this.header[1] & 1);
   }
}
