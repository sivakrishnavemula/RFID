package com.microsoft.sqlserver.jdbc;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

class CekTableEntry {
   private static final Logger aeLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.AE");
   Vector<EncryptionKeyInfo> columnEncryptionKeyValues;
   int ordinal;
   int databaseId;
   int cekId;
   int cekVersion;
   byte[] cekMdVersion;

   Vector<EncryptionKeyInfo> getColumnEncryptionKeyValues() {
      return this.columnEncryptionKeyValues;
   }

   int getOrdinal() {
      return this.ordinal;
   }

   int getDatabaseId() {
      return this.databaseId;
   }

   int getCekId() {
      return this.cekId;
   }

   int getCekVersion() {
      return this.cekVersion;
   }

   byte[] getCekMdVersion() {
      return this.cekMdVersion;
   }

   CekTableEntry(int var1) {
      this.ordinal = var1;
      this.databaseId = 0;
      this.cekId = 0;
      this.cekVersion = 0;
      this.cekMdVersion = null;
      this.columnEncryptionKeyValues = new Vector();
   }

   int getSize() {
      return this.columnEncryptionKeyValues.size();
   }

   void add(byte[] var1, int var2, int var3, int var4, byte[] var5, String var6, String var7, String var8) {
      assert null != this.columnEncryptionKeyValues : "columnEncryptionKeyValues should already be initialized.";
      if (aeLogger.isLoggable(Level.FINE)) {
         aeLogger.fine("Retrieving CEK values");
      }

      EncryptionKeyInfo var9 = new EncryptionKeyInfo(var1, var2, var3, var4, var5, var6, var7, var8);
      this.columnEncryptionKeyValues.add(var9);
      if (0 == this.databaseId) {
         this.databaseId = var2;
         this.cekId = var3;
         this.cekVersion = var4;
         this.cekMdVersion = var5;
      } else {
         assert this.databaseId == var2;
         assert this.cekId == var3;
         assert this.cekVersion == var4;
         assert null != this.cekMdVersion && null != var5 && this.cekMdVersion.length == var5.length;
      }
   }
}
