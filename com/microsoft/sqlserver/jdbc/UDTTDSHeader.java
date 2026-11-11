package com.microsoft.sqlserver.jdbc;

final class UDTTDSHeader {
   private final int maxLen;
   private final String databaseName;
   private final String schemaName;
   private final String typeName;
   private final String assemblyQualifiedName;

   UDTTDSHeader(TDSReader var1) throws SQLServerException {
      this.maxLen = var1.readUnsignedShort();
      this.databaseName = var1.readUnicodeString(var1.readUnsignedByte());
      this.schemaName = var1.readUnicodeString(var1.readUnsignedByte());
      this.typeName = var1.readUnicodeString(var1.readUnsignedByte());
      this.assemblyQualifiedName = var1.readUnicodeString(var1.readUnsignedShort());
   }

   int getMaxLen() {
      return this.maxLen;
   }

   String getTypeName() {
      return this.typeName;
   }
}
