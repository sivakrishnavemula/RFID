package com.microsoft.sqlserver.jdbc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

class KeyStoreProviderCommon {
   static final String rsaEncryptionAlgorithmWithOAEP = "RSA_OAEP";
   static byte[] version = new byte[]{1};

   static void validateEncryptionAlgorithm(String var0, boolean var1) throws SQLServerException {
      String var2 = var1 ? "R_NullKeyEncryptionAlgorithm" : "R_NullKeyEncryptionAlgorithmInternal";
      if (null == var0) {
         throw new SQLServerException(null, SQLServerException.getErrString(var2), null, 0, false);
      }

      var2 = var1 ? "R_InvalidKeyEncryptionAlgorithm" : "R_InvalidKeyEncryptionAlgorithmInternal";
      if (!"RSA_OAEP".equalsIgnoreCase(var0.trim())) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString(var2));
         Object[] var4 = new Object[]{var0, "RSA_OAEP"};
         throw new SQLServerException(var3.format(var4), null);
      }
   }

   static void validateNonEmptyMasterKeyPath(String var0) throws SQLServerException {
      if (null == var0 || var0.trim().length() == 0) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_InvalidMasterKeyDetails"), null, 0, false);
      }
   }

   static byte[] decryptColumnEncryptionKey(String var0, String var1, byte[] var2, CertificateDetails var3) throws SQLServerException {
      if (null == var2) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_NullEncryptedColumnEncryptionKey"), null, 0, false);
      } else if (0 == var2.length) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_EmptyEncryptedColumnEncryptionKey"), null, 0, false);
      } else {
         validateEncryptionAlgorithm(var1, false);
         int var4 = version.length;
         short var5 = convertTwoBytesToShort(var2, var4);
         var4 += 2;
         short var6 = convertTwoBytesToShort(var2, var4);
         var4 += 2;
         var4 += var5;
         int var7 = var2.length - var4 - var6;
         byte[] var8 = new byte[var6];
         System.arraycopy(var2, var4, var8, 0, var6);
         var4 += var6;
         byte[] var9 = new byte[var7];
         System.arraycopy(var2, var4, var9, 0, var7);
         byte[] var10 = new byte[var2.length - var9.length];
         System.arraycopy(var2, 0, var10, 0, var2.length - var9.length);
         if (!verifyRSASignature(var10, var9, var3.certificate, var0)) {
            MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_InvalidCertificateSignature"));
            Object[] var12 = new Object[]{var0};
            throw new SQLServerException(var11.format(var12), null);
         } else {
            return decryptRSAOAEP(var8, var3);
         }
      }
   }

   private static byte[] decryptRSAOAEP(byte[] var0, CertificateDetails var1) throws SQLServerException {
      Object var2 = null;

      try {
         Cipher var3 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
         var3.init(2, var1.privateKey);
         var3.update(var0);
         return var3.doFinal();
      } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException var6) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_CEKDecryptionFailed"));
         Object[] var5 = new Object[]{var6.getMessage()};
         throw new SQLServerException(var4.format(var5), null);
      }
   }

   private static boolean verifyRSASignature(byte[] var0, byte[] var1, X509Certificate var2, String var3) throws SQLServerException {
      boolean var5 = false;

      try {
         Signature var4 = Signature.getInstance("SHA256withRSA");
         var4.initVerify(var2.getPublicKey());
         var4.update(var0);
         return var4.verify(var1);
      } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException var9) {
         MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_InvalidCertificateSignature"));
         Object[] var8 = new Object[]{var3};
         throw new SQLServerException(var7.format(var8), null);
      }
   }

   private static short convertTwoBytesToShort(byte[] var0, int var1) throws SQLServerException {
      byte var2 = -1;
      if (var1 + 1 >= var0.length) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_ByteToShortConversion"), null, 0, false);
      }

      ByteBuffer var3 = ByteBuffer.allocate(2);
      var3.order(ByteOrder.LITTLE_ENDIAN);
      var3.put(var0[var1]);
      var3.put(var0[var1 + 1]);
      return var3.getShort(0);
   }
}
