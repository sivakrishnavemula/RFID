package com.microsoft.sqlserver.jdbc;

final class StreamColumns extends StreamPacket {
   private Column[] columns;
   private CekTable cekTable = null;
   private boolean shouldHonorAEForRead = false;

   CekTable getCekTable() {
      return this.cekTable;
   }

   StreamColumns() {
      super(129);
   }

   StreamColumns(boolean var1) {
      super(129);
      this.shouldHonorAEForRead = var1;
   }

   CekTableEntry readCEKTableEntry(TDSReader var1) throws SQLServerException {
      int var2 = var1.readInt();
      int var3 = var1.readInt();
      int var4 = var1.readInt();
      byte[] var5 = new byte[8];
      var1.readBytes(var5, 0, 8);
      int var6 = var1.readUnsignedByte();
      CekTableEntry var7 = new CekTableEntry(var6);

      for (int var8 = 0; var8 < var6; var8++) {
         short var9 = var1.readShort();
         byte[] var10 = new byte[var9];
         var1.readBytes(var10, 0, var9);
         int var11 = var1.readUnsignedByte();
         String var12 = var1.readUnicodeString(var11);
         short var13 = var1.readShort();
         String var14 = var1.readUnicodeString(var13);
         int var15 = var1.readUnsignedByte();
         String var16 = var1.readUnicodeString(var15);
         var7.add(var10, var2, var3, var4, var5, var14, var12, var16);
      }

      return var7;
   }

   void readCEKTable(TDSReader var1) throws SQLServerException {
      short var2 = var1.readShort();
      if (0 != var2) {
         this.cekTable = new CekTable(var2);

         for (int var3 = 0; var3 < var2; var3++) {
            this.cekTable.setCekTableEntry(var3, this.readCEKTableEntry(var1));
         }
      }
   }

   CryptoMetadata readCryptoMetadata(TDSReader var1) throws SQLServerException {
      short var2 = 0;
      if (null != this.cekTable) {
         var2 = var1.readShort();
      }

      TypeInfo var3 = TypeInfo.getInstance(var1, false);
      byte var4 = (byte)var1.readUnsignedByte();
      String var5 = null;
      if (0 == var4) {
         int var6 = var1.readUnsignedByte();
         var5 = var1.readUnicodeString(var6);
      }

      byte var9 = (byte)var1.readUnsignedByte();
      byte var7 = (byte)var1.readUnsignedByte();
      CryptoMetadata var8 = new CryptoMetadata(this.cekTable == null ? null : this.cekTable.getCekTableEntry(var2), var2, var4, var5, var9, var7);
      var8.setBaseTypeInfo(var3);
      return var8;
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (129 != var1.readUnsignedByte()) {
         assert false;
      }

      int var2 = var1.readUnsignedShort();
      if (65535 != var2) {
         if (var1.getServerSupportsColumnEncryption()) {
            this.readCEKTable(var1);
         }

         this.columns = new Column[var2];

         for (int var3 = 0; var3 < var2; var3++) {
            TypeInfo var4 = TypeInfo.getInstance(var1, true);
            SQLIdentifier var5 = new SQLIdentifier();
            if (SSType.TEXT == var4.getSSType() || SSType.NTEXT == var4.getSSType() || SSType.IMAGE == var4.getSSType()) {
               var5 = var1.readSQLIdentifier();
            }

            CryptoMetadata var6 = null;
            if (var1.getServerSupportsColumnEncryption() && var4.isEncrypted()) {
               var6 = this.readCryptoMetadata(var1);
               var6.baseTypeInfo.setFlags(var4.getFlagsAsShort());
               var4.setSQLCollation(var6.baseTypeInfo.getSQLCollation());
            }

            String var7 = var1.readUnicodeString(var1.readUnsignedByte());
            if (this.shouldHonorAEForRead) {
               this.columns[var3] = new Column(var4, var7, var5, var6);
            } else {
               this.columns[var3] = new Column(var4, var7, var5, null);
            }
         }
      }
   }

   Column[] buildColumns(StreamColInfo var1, StreamTabName var2) throws SQLServerException {
      if (null != var1 && null != var2) {
         var2.applyTo(this.columns, var1.applyTo(this.columns));
      }

      return this.columns;
   }
}
