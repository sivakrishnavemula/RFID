package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;

enum SQLServerEncryptionType {
   Deterministic((byte)1),
   Randomized((byte)2),
   PlainText((byte)0);

   byte value;

   SQLServerEncryptionType(byte var3) {
      this.value = var3;
   }

   byte getValue() {
      return this.value;
   }

   static SQLServerEncryptionType of(byte var0) throws SQLServerException {
      for (SQLServerEncryptionType var4 : values()) {
         if (var0 == var4.value) {
            return var4;
         }
      }

      MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_unknownColumnEncryptionType"));
      Object[] var6 = new Object[]{var0};
      SQLServerException.makeFromDriverError(null, null, var5.format(var6), null, true);
      return null;
   }
}
