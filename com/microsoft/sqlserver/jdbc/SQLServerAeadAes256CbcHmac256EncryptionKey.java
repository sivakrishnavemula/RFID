package com.microsoft.sqlserver.jdbc;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

class SQLServerAeadAes256CbcHmac256EncryptionKey extends SQLServerSymmetricKey {
   static final int keySize = 256;
   private final String algorithmName;
   private String encryptionKeySaltFormat;
   private String macKeySaltFormat;
   private String ivKeySaltFormat;
   private SQLServerSymmetricKey encryptionKey;
   private SQLServerSymmetricKey macKey;
   private SQLServerSymmetricKey ivKey;

   SQLServerAeadAes256CbcHmac256EncryptionKey(byte[] var1, String var2) throws SQLServerException {
      super(var1);
      this.algorithmName = var2;
      this.encryptionKeySaltFormat = "Microsoft SQL Server cell encryption key with encryption algorithm:" + this.algorithmName + " and key length:" + 256;
      this.macKeySaltFormat = "Microsoft SQL Server cell MAC key with encryption algorithm:" + this.algorithmName + " and key length:" + 256;
      this.ivKeySaltFormat = "Microsoft SQL Server cell IV key with encryption algorithm:" + this.algorithmName + " and key length:" + 256;
      byte var3 = 32;
      if (var1.length != var3) {
         MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_InvalidKeySize"));
         Object[] var13 = new Object[]{var1.length, Integer.valueOf(var3), this.algorithmName};
         throw new SQLServerException(this, var11.format(var13), null, 0, false);
      }

      byte[] var4 = new byte[var3];

      try {
         var4 = SQLServerSecurityUtility.getHMACWithSHA256(this.encryptionKeySaltFormat.getBytes("UTF-16LE"), var1, var4.length);
         this.encryptionKey = new SQLServerSymmetricKey(var4);
         byte[] var5 = new byte[var3];
         var5 = SQLServerSecurityUtility.getHMACWithSHA256(this.macKeySaltFormat.getBytes("UTF-16LE"), var1, var5.length);
         this.macKey = new SQLServerSymmetricKey(var5);
         byte[] var15 = new byte[var3];
         var15 = SQLServerSecurityUtility.getHMACWithSHA256(this.ivKeySaltFormat.getBytes("UTF-16LE"), var1, var15.length);
         this.ivKey = new SQLServerSymmetricKey(var15);
      } catch (UnsupportedEncodingException var8) {
         MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         Object[] var17 = new Object[]{"UTF-16LE"};
         throw new SQLServerException(this, var14.format(var17), null, 0, false);
      } catch (InvalidKeyException | NoSuchAlgorithmException var9) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_KeyExtractionFailed"));
         Object[] var7 = new Object[]{var9.getMessage()};
         throw new SQLServerException(this, var6.format(var7), null, 0, false);
      }
   }

   byte[] getEncryptionKey() {
      return this.encryptionKey.getRootKey();
   }

   byte[] getMacKey() {
      return this.macKey.getRootKey();
   }

   byte[] getIVKey() {
      return this.ivKey.getRootKey();
   }
}
