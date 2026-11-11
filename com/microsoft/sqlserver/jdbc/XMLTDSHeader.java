package com.microsoft.sqlserver.jdbc;

final class XMLTDSHeader {
   private final String databaseName;
   private final String owningSchema;
   private final String xmlSchemaCollection;

   XMLTDSHeader(TDSReader var1) throws SQLServerException {
      if (0 != var1.readUnsignedByte()) {
         this.databaseName = var1.readUnicodeString(var1.readUnsignedByte());
         this.owningSchema = var1.readUnicodeString(var1.readUnsignedByte());
         this.xmlSchemaCollection = var1.readUnicodeString(var1.readUnsignedShort());
      } else {
         this.xmlSchemaCollection = null;
         this.owningSchema = null;
         this.databaseName = null;
      }
   }
}
