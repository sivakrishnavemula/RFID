package com.microsoft.sqlserver.jdbc;

class CekTable {
   CekTableEntry[] keyList;

   CekTable(int var1) {
      this.keyList = new CekTableEntry[var1];
   }

   int getSize() {
      return this.keyList.length;
   }

   CekTableEntry getCekTableEntry(int var1) {
      return this.keyList[var1];
   }

   void setCekTableEntry(int var1, CekTableEntry var2) {
      this.keyList[var1] = var2;
   }
}
