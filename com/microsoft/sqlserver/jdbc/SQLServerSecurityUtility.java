package com.microsoft.sqlserver.jdbc;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class SQLServerSecurityUtility {
   static byte[] getHMACWithSHA256(byte[] var0, byte[] var1, int var2) throws NoSuchAlgorithmException, InvalidKeyException {
      byte[] var4 = new byte[var2];
      Mac var5 = Mac.getInstance("HmacSHA256");
      SecretKeySpec var6 = new SecretKeySpec(var1, "HmacSHA256");
      var5.init(var6);
      byte[] var3 = var5.doFinal(var0);
      System.arraycopy(var3, 0, var4, 0, var4.length);
      return var4;
   }

   static boolean compareBytes(byte[] var0, byte[] var1, int var2, int var3) {
      if (null != var0 && null != var1) {
         if (var1.length - var2 < var3) {
            return false;
         }

         for (int var4 = 0; var4 < var0.length && var4 < var3; var4++) {
            if (var0[var4] != var1[var2 + var4]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static byte[] encryptWithKey(byte[] var0, CryptoMetadata var1, SQLServerConnection var2) throws SQLServerException {
      String var3 = var2.getTrustedServerNameAE();
      assert var3 != null : "Server name should npt be null in EncryptWithKey";
      if (!var1.IsAlgorithmInitialized()) {
         decryptSymmetricKey(var1, var2);
      }

      assert var1.IsAlgorithmInitialized();
      byte[] var4 = var1.cipherAlgorithm.encryptData(var0);
      if (null != var4 && 0 != var4.length) {
         return var4;
      } else {
         throw new SQLServerException(null, SQLServerException.getErrString("R_NullCipherTextAE"), null, 0, false);
      }
   }

   private static String ValidateAndGetEncryptionAlgorithmName(byte var0, String var1) throws SQLServerException {
      if (2 != var0) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_CustomCipherAlgorithmNotSupportedAE"), null, 0, false);
      } else {
         return "AEAD_AES_256_CBC_HMAC_SHA256";
      }
   }

   static void decryptSymmetricKey(CryptoMetadata var0, SQLServerConnection var1) throws SQLServerException {
      assert null != var0 : "md should not be null in DecryptSymmetricKey.";
      assert null != var0.cekTableEntry : "md.EncryptionInfo should not be null in DecryptSymmetricKey.";
      assert null != var0.cekTableEntry.columnEncryptionKeyValues : "md.EncryptionInfo.ColumnEncryptionKeyValues should not be null in DecryptSymmetricKey.";
      SQLServerSymmetricKey var2 = null;
      EncryptionKeyInfo var3 = null;
      SQLServerSymmetricKeyCache var4 = SQLServerSymmetricKeyCache.getInstance();
      Iterator var5 = var0.cekTableEntry.columnEncryptionKeyValues.iterator();
      SQLServerException var6 = null;

      while (var5.hasNext()) {
         EncryptionKeyInfo var7 = (EncryptionKeyInfo)var5.next();

         try {
            var2 = var4.getKey(var7, var1);
            if (null != var2) {
               var3 = var7;
               break;
            }
         } catch (SQLServerException var9) {
            var6 = var9;
         }
      }

      if (null == var2) {
         assert null != var6 : "CEK decryption failed without raising exceptions";
         throw var6;
      }

      var0.cipherAlgorithm = null;
      Object var10 = null;
      String var8 = ValidateAndGetEncryptionAlgorithmName(var0.cipherAlgorithmId, var0.cipherAlgorithmName);
      var10 = SQLServerEncryptionAlgorithmFactoryList.getInstance().getAlgorithm(var2, var0.encryptionType, var8);
      assert null != var10 : "Cipher algorithm cannot be null in DecryptSymmetricKey";
      var0.cipherAlgorithm = (SQLServerEncryptionAlgorithm)var10;
      var0.encryptionKeyInfo = var3;
   }

   static byte[] decryptWithKey(byte[] var0, CryptoMetadata var1, SQLServerConnection var2) throws SQLServerException {
      String var3 = var2.getTrustedServerNameAE();
      assert null != var3 : "serverName should not be null in DecryptWithKey.";
      if (!var1.IsAlgorithmInitialized()) {
         decryptSymmetricKey(var1, var2);
      }

      assert var1.IsAlgorithmInitialized() : "Decryption Algorithm is not initialized";
      byte[] var4 = var1.cipherAlgorithm.decryptData(var0);
      if (null == var4) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_PlainTextNullAE"), null, 0, false);
      } else {
         return var4;
      }
   }
}
