package com.microsoft.sqlserver.jdbc;

class CryptoMetadata {
   TypeInfo baseTypeInfo;
   CekTableEntry cekTableEntry;
   byte cipherAlgorithmId;
   String cipherAlgorithmName;
   SQLServerEncryptionType encryptionType;
   byte normalizationRuleVersion;
   SQLServerEncryptionAlgorithm cipherAlgorithm = null;
   EncryptionKeyInfo encryptionKeyInfo;
   short ordinal;

   CekTableEntry getCekTableEntry() {
      return this.cekTableEntry;
   }

   void setCekTableEntry(CekTableEntry var1) {
      this.cekTableEntry = var1;
   }

   TypeInfo getBaseTypeInfo() {
      return this.baseTypeInfo;
   }

   void setBaseTypeInfo(TypeInfo var1) {
      this.baseTypeInfo = var1;
   }

   SQLServerEncryptionAlgorithm getEncryptionAlgorithm() {
      return this.cipherAlgorithm;
   }

   void setEncryptionAlgorithm(SQLServerEncryptionAlgorithm var1) {
      this.cipherAlgorithm = var1;
   }

   EncryptionKeyInfo getEncryptionKeyInfo() {
      return this.encryptionKeyInfo;
   }

   void setEncryptionKeyInfo(EncryptionKeyInfo var1) {
      this.encryptionKeyInfo = var1;
   }

   byte getEncryptionAlgorithmId() {
      return this.cipherAlgorithmId;
   }

   String getEncryptionAlgorithmName() {
      return this.cipherAlgorithmName;
   }

   SQLServerEncryptionType getEncryptionType() {
      return this.encryptionType;
   }

   byte getNormalizationRuleVersion() {
      return this.normalizationRuleVersion;
   }

   short getOrdinal() {
      return this.ordinal;
   }

   CryptoMetadata(CekTableEntry var1, short var2, byte var3, String var4, byte var5, byte var6) throws SQLServerException {
      this.cekTableEntry = var1;
      this.ordinal = var2;
      this.cipherAlgorithmId = var3;
      this.cipherAlgorithmName = var4;
      this.encryptionType = SQLServerEncryptionType.of(var5);
      this.normalizationRuleVersion = var6;
      this.encryptionKeyInfo = null;
   }

   boolean IsAlgorithmInitialized() {
      return null != this.cipherAlgorithm;
   }
}
