package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.Locale;

enum KeyStoreAuthentication {
   JavaKeyStorePassword;

   static KeyStoreAuthentication valueOfString(String var0) throws SQLServerException {
      Object var1 = null;
      if (var0.toLowerCase(Locale.US).equalsIgnoreCase(JavaKeyStorePassword.toString())) {
         return JavaKeyStorePassword;
      }

      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_InvalidConnectionSetting"));
      Object[] var3 = new Object[]{"keyStoreAuthentication", var0};
      throw new SQLServerException(var2.format(var3), null);
   }
}
