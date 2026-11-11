package com.microsoft.sqlserver.jdbc;

class EncryptionKeyInfo {
   byte[] encryptedKey;
   int databaseId;
   int cekId;
   int cekVersion;
   byte[] cekMdVersion;
   String keyPath;
   String keyStoreName;
   String algorithmName;
   byte normalizationRuleVersion;

   EncryptionKeyInfo(byte[] var1, int var2, int var3, int var4, byte[] var5, String var6, String var7, String var8) {
      this.encryptedKey = var1;
      this.databaseId = var2;
      this.cekId = var3;
      this.cekVersion = var4;
      this.cekMdVersion = var5;
      this.keyPath = var6;
      this.keyStoreName = var7;
      this.algorithmName = var8;
   }
}
