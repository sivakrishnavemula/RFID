package com.microsoft.sqlserver.jdbc;

final class ParameterUtils {
   static byte[] HexToBin(String var0) throws SQLServerException {
      int var1 = var0.length();
      char[] var2 = var0.toCharArray();
      if (var1 % 2 != 0) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_stringNotInHex"), null, false);
      }

      byte[] var3 = new byte[var1 / 2];

      for (int var4 = 0; var4 < var1 / 2; var4++) {
         var3[var4] = (byte)((CharToHex(var2[2 * var4]) << 4) + CharToHex(var2[2 * var4 + 1]));
      }

      return var3;
   }

   static byte CharToHex(char var0) throws SQLServerException {
      byte var1 = 0;
      if (var0 >= 'A' && var0 <= 'F') {
         var1 = (byte)(var0 - 'A' + 10);
      } else if (var0 >= 'a' && var0 <= 'f') {
         var1 = (byte)(var0 - 'a' + 10);
      } else if (var0 >= '0' && var0 <= '9') {
         var1 = (byte)(var0 - '0');
      } else {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_stringNotInHex"), null, false);
      }

      return var1;
   }

   static int scanSQLForChar(char var0, String var1, int var2) {
      int var5 = var1.length();

      label71:
      while (var2 < var5) {
         char var4;
         switch (var4 = var1.charAt(var2++)) {
            case '/':
               if (var2 == var5) {
                  break;
               }

               if (var1.charAt(var2) == '*') {
                  while (++var2 < var5) {
                     if (var1.charAt(var2) == '*' && var2 + 1 < var5 && var1.charAt(var2 + 1) == '/') {
                        var2 += 2;
                        break;
                     }
                  }
                  break;
               } else if (var1.charAt(var2) == '-') {
                  break;
               }
            case '-':
               if (var1.charAt(var2) == '-') {
                  do {
                     if (++var2 >= var5) {
                        continue label71;
                     }
                  } while (var1.charAt(var2) != '\n' && var1.charAt(var2) != '\r');

                  var2++;
                  break;
               }
            default:
               if (var0 == var4) {
                  return var2 - 1;
               }
               break;
            case '[':
               var4 = ']';
            case '"':
            case '\'':
               char var3 = var4;

               while (var2 < var5) {
                  if (var1.charAt(var2++) == var3) {
                     if (var5 == var2 || var1.charAt(var2) != var3) {
                        break;
                     }

                     var2++;
                  }
               }
         }
      }

      return var5;
   }
}
