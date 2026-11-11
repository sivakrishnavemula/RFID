package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.Locale;

enum AuthenticationScheme {
   nativeAuthentication,
   javaKerberos;

   static AuthenticationScheme valueOfString(String var0) throws SQLServerException {
      AuthenticationScheme var1;
      if (var0.toLowerCase(Locale.US).equalsIgnoreCase(javaKerberos.toString())) {
         var1 = javaKerberos;
      } else {
         if (!var0.toLowerCase(Locale.US).equalsIgnoreCase(nativeAuthentication.toString())) {
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidAuthenticationScheme"));
            Object[] var3 = new Object[]{var0};
            throw new SQLServerException(null, var2.format(var3), null, 0, false);
         }

         var1 = nativeAuthentication;
      }

      return var1;
   }
}
