package com.microsoft.sqlserver.jdbc;

enum DescribeParameterEncryptionResultSet1 {
   KeyOrdinal,
   DbId,
   KeyId,
   KeyVersion,
   KeyMdVersion,
   EncryptedKey,
   ProviderName,
   KeyPath,
   KeyEncryptionAlgorithm;

   private int value;

   int value() {
      return this.value;
   }

   static {
      for (int var0 = 0; var0 < values().length; var0++) {
         values()[var0].value = var0 + 1;
      }
   }
}
