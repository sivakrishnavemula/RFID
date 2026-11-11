package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.Locale;

enum ApplicationIntent {
   READ_WRITE("readwrite"),
   READ_ONLY("readonly");

   private String value;

   ApplicationIntent(String var3) {
      this.value = var3;
   }

   public String toString() {
      return this.value;
   }

   static ApplicationIntent valueOfString(String var0) throws SQLServerException {
      ApplicationIntent var1 = READ_WRITE;
      assert var0 != null;
      var0 = var0.toUpperCase(Locale.US).toLowerCase(Locale.US);
      if (var0.equalsIgnoreCase(READ_ONLY.toString())) {
         var1 = READ_ONLY;
      } else {
         if (!var0.equalsIgnoreCase(READ_WRITE.toString())) {
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidapplicationIntent"));
            Object[] var3 = new Object[]{new String(var0)};
            throw new SQLServerException(null, var2.format(var3), null, 0, false);
         }

         var1 = READ_WRITE;
      }

      return var1;
   }
}
