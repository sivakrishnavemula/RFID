package com.microsoft.sqlserver.jdbc;

final class StreamTabName extends StreamPacket {
   private TDSReader tdsReader;
   private TDSReaderMark tableNamesMark;

   StreamTabName() {
      super(164);
   }

   void setFromTDS(TDSReader var1) throws SQLServerException {
      if (164 != var1.readUnsignedByte()) {
         assert false : "Not a TABNAME token";
      }

      this.tdsReader = var1;
      int var2 = var1.readUnsignedShort();
      this.tableNamesMark = var1.mark();
      var1.skip(var2);
   }

   void applyTo(Column[] var1, int var2) throws SQLServerException {
      TDSReaderMark var3 = this.tdsReader.mark();
      this.tdsReader.reset(this.tableNamesMark);
      SQLIdentifier[] var4 = new SQLIdentifier[var2];

      for (int var5 = 0; var5 < var2; var5++) {
         var4[var5] = this.tdsReader.readSQLIdentifier();
      }

      for (int var7 = 0; var7 < var1.length; var7++) {
         Column var6 = var1[var7];
         if (var6.getTableNum() > 0) {
            var6.setTableName(var4[var6.getTableNum() - 1]);
         }
      }

      this.tdsReader.reset(var3);
   }
}
