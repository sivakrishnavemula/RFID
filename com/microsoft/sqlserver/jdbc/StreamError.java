package com.microsoft.sqlserver.jdbc;

final class StreamError extends StreamPacket {
   String errorMessage = "";
   int errorNumber;
   int errorState;
   int errorSeverity;
   String serverName;
   String procName;
   long lineNumber;

   final String getMessage() {
      return this.errorMessage;
   }

   final int getErrorNumber() {
      return this.errorNumber;
   }

   final int getErrorState() {
      return this.errorState;
   }

   final int getErrorSeverity() {
      return this.errorSeverity;
   }

   StreamError() {
      super(170);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (170 != var1.readUnsignedByte()) {
         assert false;
      }

      this.setContentsFromTDS(var1);
   }

   void setContentsFromTDS(TDSReader var1) throws SQLServerException {
      var1.readUnsignedShort();
      this.errorNumber = var1.readInt();
      this.errorState = var1.readUnsignedByte();
      this.errorSeverity = var1.readUnsignedByte();
      this.errorMessage = var1.readUnicodeString(var1.readUnsignedShort());
      this.serverName = var1.readUnicodeString(var1.readUnsignedByte());
      this.procName = var1.readUnicodeString(var1.readUnsignedByte());
      this.lineNumber = var1.readUnsignedInt();
   }
}
