package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.Locale;

enum SqlAuthentication {
   NotSpecified,
   SqlPassword,
   ActiveDirectoryPassword,
   ActiveDirectoryIntegrated;

   static SqlAuthentication valueOfString(String var0) throws SQLServerException {
      SqlAuthentication var1 = null;
      if (var0.toLowerCase(Locale.US).equalsIgnoreCase(NotSpecified.toString())) {
         var1 = NotSpecified;
      } else if (var0.toLowerCase(Locale.US).equalsIgnoreCase(SqlPassword.toString())) {
         var1 = SqlPassword;
      } else if (var0.toLowerCase(Locale.US).equalsIgnoreCase(ActiveDirectoryPassword.toString())) {
         var1 = ActiveDirectoryPassword;
      } else {
         if (!var0.toLowerCase(Locale.US).equalsIgnoreCase(ActiveDirectoryIntegrated.toString())) {
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_InvalidConnectionSetting"));
            Object[] var3 = new Object[]{"authentication", var0};
            throw new SQLServerException(null, var2.format(var3), null, 0, false);
         }

         var1 = ActiveDirectoryIntegrated;
      }

      return var1;
   }
}
