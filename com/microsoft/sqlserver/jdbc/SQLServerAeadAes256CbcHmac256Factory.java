package com.microsoft.sqlserver.jdbc;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.DatatypeConverter;

class SQLServerAeadAes256CbcHmac256Factory extends SQLServerEncryptionAlgorithmFactory {
   private byte algorithmVersion = 1;
   private ConcurrentHashMap<String, SQLServerAeadAes256CbcHmac256Algorithm> encryptionAlgorithms = new ConcurrentHashMap();

   SQLServerEncryptionAlgorithm create(SQLServerSymmetricKey var1, SQLServerEncryptionType var2, String var3) throws SQLServerException {
      assert var1 != null;
      if (var2 != SQLServerEncryptionType.Deterministic && var2 != SQLServerEncryptionType.Randomized) {
         MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_InvalidEncryptionType"));
         Object[] var11 = new Object[]{var2, var3, "'" + SQLServerEncryptionType.Deterministic + "," + SQLServerEncryptionType.Randomized + "'"};
         throw new SQLServerException(this, var10.format(var11), null, 0, false);
      }

      String var4 = "";

      try {
         StringBuffer var5 = new StringBuffer();
         var5.append(DatatypeConverter.printBase64Binary(new String(var1.getRootKey(), "UTF-8").getBytes()));
         var5.append(":");
         var5.append(var2);
         var5.append(":");
         var5.append(this.algorithmVersion);
         var4 = var5.toString();
         if (!this.encryptionAlgorithms.containsKey(var4)) {
            SQLServerAeadAes256CbcHmac256EncryptionKey var13 = new SQLServerAeadAes256CbcHmac256EncryptionKey(var1.getRootKey(), "AEAD_AES_256_CBC_HMAC_SHA256");
            SQLServerAeadAes256CbcHmac256Algorithm var12 = new SQLServerAeadAes256CbcHmac256Algorithm(var13, var2, this.algorithmVersion);
            this.encryptionAlgorithms.putIfAbsent(var4, var12);
         }
      } catch (UnsupportedEncodingException var8) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         Object[] var7 = new Object[]{"UTF-8"};
         throw new SQLServerException(this, var6.format(var7), null, 0, false);
      }

      return (SQLServerEncryptionAlgorithm)this.encryptionAlgorithms.get(var4);
   }
}
