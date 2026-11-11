package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.Locale;

enum ColumnEncryptionSetting {
   Enabled,
   Disabled;

   static ColumnEncryptionSetting valueOfString(String var0) throws SQLServerException {
      ColumnEncryptionSetting var1 = null;
      if (var0.toLowerCase(Locale.US).equalsIgnoreCase(Enabled.toString())) {
         var1 = Enabled;
      } else {
         if (!var0.toLowerCase(Locale.US).equalsIgnoreCase(Disabled.toString())) {
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_InvalidConnectionSetting"));
            Object[] var3 = new Object[]{"columnEncryptionSetting", var0};
            throw new SQLServerException(var2.format(var3), null);
         }

         var1 = Disabled;
      }

      return var1;
   }
}
