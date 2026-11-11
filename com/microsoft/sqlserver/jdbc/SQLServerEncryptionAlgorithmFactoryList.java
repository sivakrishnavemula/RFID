package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;

final class SQLServerEncryptionAlgorithmFactoryList {
   private ConcurrentHashMap<String, SQLServerEncryptionAlgorithmFactory> encryptionAlgoFactoryMap = new ConcurrentHashMap();
   private static final SQLServerEncryptionAlgorithmFactoryList instance = new SQLServerEncryptionAlgorithmFactoryList();

   private SQLServerEncryptionAlgorithmFactoryList() {
      this.encryptionAlgoFactoryMap.putIfAbsent("AEAD_AES_256_CBC_HMAC_SHA256", new SQLServerAeadAes256CbcHmac256Factory());
   }

   static SQLServerEncryptionAlgorithmFactoryList getInstance() {
      return instance;
   }

   String getRegisteredCipherAlgorithmNames() {
      StringBuffer var1 = new StringBuffer();
      boolean var2 = true;

      for (String var4 : this.encryptionAlgoFactoryMap.keySet()) {
         if (var2) {
            var1.append("'");
            var2 = false;
         } else {
            var1.append(", '");
         }

         var1.append(var4);
         var1.append("'");
      }

      return var1.toString();
   }

   SQLServerEncryptionAlgorithm getAlgorithm(SQLServerSymmetricKey var1, SQLServerEncryptionType var2, String var3) throws SQLServerException {
      Object var4 = null;
      SQLServerEncryptionAlgorithmFactory var5 = null;
      if (!this.encryptionAlgoFactoryMap.containsKey(var3)) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_UnknownColumnEncryptionAlgorithm"));
         Object[] var7 = new Object[]{var3, getInstance().getRegisteredCipherAlgorithmNames()};
         throw new SQLServerException(this, var6.format(var7), null, 0, false);
      } else {
         var5 = (SQLServerEncryptionAlgorithmFactory)this.encryptionAlgoFactoryMap.get(var3);
         assert null != var5 : "Null Algorithm Factory class detected";
         return var5.create(var1, var2, var3);
      }
   }
}
