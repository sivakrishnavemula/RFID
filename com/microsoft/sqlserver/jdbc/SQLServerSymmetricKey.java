package com.microsoft.sqlserver.jdbc;

class SQLServerSymmetricKey {
   private byte[] rootKey;

   SQLServerSymmetricKey(byte[] var1) throws SQLServerException {
      if (null == var1) {
         throw new SQLServerException(this, SQLServerException.getErrString("R_NullColumnEncryptionKey"), null, 0, false);
      }

      if (0 == var1.length) {
         throw new SQLServerException(this, SQLServerException.getErrString("R_EmptyColumnEncryptionKey"), null, 0, false);
      }

      this.rootKey = var1;
   }

   byte[] getRootKey() {
      return this.rootKey;
   }

   int length() {
      return this.rootKey.length;
   }

   void zeroOutKey() {
      for (int var1 = 0; var1 < this.rootKey.length; var1++) {
         this.rootKey[var1] = 0;
      }
   }
}
