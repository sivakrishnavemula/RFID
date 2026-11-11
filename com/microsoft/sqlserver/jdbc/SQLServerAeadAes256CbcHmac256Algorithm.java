package com.microsoft.sqlserver.jdbc;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class SQLServerAeadAes256CbcHmac256Algorithm extends SQLServerEncryptionAlgorithm {
   private static final Logger aeLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.SQLServerAeadAes256CbcHmac256Algorithm");
   static final String algorithmName = "AEAD_AES_256_CBC_HMAC_SHA256";
   private SQLServerAeadAes256CbcHmac256EncryptionKey columnEncryptionkey;
   private byte algorithmVersion;
   private boolean isDeterministic = false;
   private int blockSizeInBytes = 16;
   private int keySizeInBytes = 32;
   private byte[] version = new byte[]{1};
   private byte[] versionSize = new byte[]{1};
   private int minimumCipherTextLengthInBytesNoAuthenticationTag = 1 + this.blockSizeInBytes + this.blockSizeInBytes;
   private int minimumCipherTextLengthInBytesWithAuthenticationTag = this.minimumCipherTextLengthInBytesNoAuthenticationTag + this.keySizeInBytes;

   SQLServerAeadAes256CbcHmac256Algorithm(SQLServerAeadAes256CbcHmac256EncryptionKey var1, SQLServerEncryptionType var2, byte var3) {
      this.columnEncryptionkey = var1;
      if (var2 == SQLServerEncryptionType.Deterministic) {
         this.isDeterministic = true;
      }

      this.algorithmVersion = var3;
      this.version[0] = var3;
   }

   byte[] encryptData(byte[] var1) throws SQLServerException {
      return this.encryptData(var1, true);
   }

   protected byte[] encryptData(byte[] var1, boolean var2) throws SQLServerException {
      aeLogger.entering(SQLServerAeadAes256CbcHmac256Algorithm.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "Encrypting data.");
      assert var1 != null;
      byte[] var3 = new byte[this.blockSizeInBytes];
      SecretKeySpec var4 = new SecretKeySpec(this.columnEncryptionkey.getEncryptionKey(), "AES");
      if (this.isDeterministic) {
         try {
            var3 = SQLServerSecurityUtility.getHMACWithSHA256(var1, this.columnEncryptionkey.getIVKey(), this.blockSizeInBytes);
         } catch (InvalidKeyException | NoSuchAlgorithmException var20) {
            MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_EncryptionFailed"));
            Object[] var7 = new Object[]{var20.getMessage()};
            throw new SQLServerException(this, var6.format(var7), null, 0, false);
         }
      } else {
         SecureRandom var5 = new SecureRandom();
         var5.nextBytes(var3);
      }

      int var22 = var1.length / this.blockSizeInBytes + 1;
      byte var23 = 1;
      int var24 = var2 ? this.keySizeInBytes : 0;
      int var8 = var23 + var24;
      int var9 = var8 + this.blockSizeInBytes;
      int var10 = 1 + var24 + var3.length + var22 * this.blockSizeInBytes;
      byte[] var11 = new byte[var10];
      var11[0] = this.algorithmVersion;
      System.arraycopy(var3, 0, var11, var8, var3.length);

      try {
         IvParameterSpec var12 = new IvParameterSpec(var3);
         Cipher var25 = Cipher.getInstance("AES/CBC/PKCS5Padding");
         var25.init(1, var4, var12);
         int var26 = 0;
         int var15 = var9;
         if (var22 > 1) {
            var26 = (var22 - 1) * this.blockSizeInBytes;
            var15 += var25.update(var1, 0, var26, var11, var15);
         }

         byte[] var16 = var25.doFinal(var1, var26, var1.length - var26);
         System.arraycopy(var16, 0, var11, var15, var16.length);
         if (var2) {
            Mac var17 = Mac.getInstance("HmacSHA256");
            SecretKeySpec var18 = new SecretKeySpec(this.columnEncryptionkey.getMacKey(), "HmacSHA256");
            var17.init(var18);
            var17.update(this.version, 0, this.version.length);
            var17.update(var3, 0, var3.length);
            var17.update(var11, var9, var22 * this.blockSizeInBytes);
            var17.update(this.versionSize, 0, this.version.length);
            byte[] var19 = var17.doFinal();
            System.arraycopy(var19, 0, var11, var23, var24);
         }
      } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | ShortBufferException var21) {
         MessageFormat var13 = new MessageFormat(SQLServerException.getErrString("R_EncryptionFailed"));
         Object[] var14 = new Object[]{var21.getMessage()};
         throw new SQLServerException(this, var13.format(var14), null, 0, false);
      }

      aeLogger.exiting(SQLServerAeadAes256CbcHmac256Algorithm.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "Data encrypted.");
      return var11;
   }

   byte[] decryptData(byte[] var1) throws SQLServerException {
      return this.decryptData(var1, true);
   }

   private byte[] decryptData(byte[] var1, boolean var2) throws SQLServerException {
      assert var1 != null;
      byte[] var3 = new byte[this.blockSizeInBytes];
      int var4 = var2 ? this.minimumCipherTextLengthInBytesWithAuthenticationTag : this.minimumCipherTextLengthInBytesNoAuthenticationTag;
      if (var1.length < var4) {
         MessageFormat var16 = new MessageFormat(SQLServerException.getErrString("R_InvalidCipherTextSize"));
         Object[] var18 = new Object[]{var1.length, var4};
         throw new SQLServerException(this, var16.format(var18), null, 0, false);
      }

      int var5 = 0;
      if (var1[var5] != this.algorithmVersion) {
         MessageFormat var17 = new MessageFormat(SQLServerException.getErrString("R_InvalidAlgorithmVersion"));
         Object[] var19 = new Object[]{String.format("%02X ", new Object[]{var1[var5]}), String.format("%02X ", new Object[]{this.algorithmVersion})};
         throw new SQLServerException(this, var17.format(var19), null, 0, false);
      }

      var5++;
      int var6 = 0;
      if (var2) {
         var6 = var5;
         var5 += this.keySizeInBytes;
      }

      System.arraycopy(var1, var5, var3, 0, var3.length);
      var5 += var3.length;
      int var7 = var5;
      int var8 = var1.length - var5;
      if (var2) {
         byte[] var9;
         try {
            var9 = this.prepareAuthenticationTag(var3, var1, var7, var8);
         } catch (InvalidKeyException | NoSuchAlgorithmException var13) {
            MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_DecryptionFailed"));
            Object[] var12 = new Object[]{var13.getMessage()};
            throw new SQLServerException(this, var11.format(var12), null, 0, false);
         }

         if (!SQLServerSecurityUtility.compareBytes(var9, var1, var6, var8)) {
            throw new SQLServerException(this, SQLServerException.getErrString("R_InvalidAuthenticationTag"), null, 0, false);
         }
      }

      return this.decryptData(var3, var1, var7, var8);
   }

   private byte[] decryptData(byte[] var1, byte[] var2, int var3, int var4) throws SQLServerException {
      aeLogger.entering(SQLServerAeadAes256CbcHmac256Algorithm.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "Decrypting data.");
      assert var2 != null;
      assert var1 != null;
      B var5 = null;
      SecretKeySpec var6 = new SecretKeySpec(this.columnEncryptionkey.getEncryptionKey(), "AES");
      IvParameterSpec var7 = new IvParameterSpec(var1);

      try {
         Cipher var8 = Cipher.getInstance("AES/CBC/PKCS5Padding");
         var8.init(2, var6, var7);
         var5 = var8.doFinal(var2, var3, var4);
      } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException var12) {
         MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_DecryptionFailed"));
         Object[] var11 = new Object[]{var12.getMessage()};
         throw new SQLServerException(this, var10.format(var11), null, 0, false);
      }

      aeLogger.exiting(SQLServerAeadAes256CbcHmac256Algorithm.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "Data decrypted.");
      return (byte[])var5;
   }

   private byte[] prepareAuthenticationTag(byte[] var1, byte[] var2, int var3, int var4) throws NoSuchAlgorithmException, InvalidKeyException {
      assert var2 != null;
      byte[] var6 = new byte[this.keySizeInBytes];
      Mac var7 = Mac.getInstance("HmacSHA256");
      SecretKeySpec var8 = new SecretKeySpec(this.columnEncryptionkey.getMacKey(), "HmacSHA256");
      var7.init(var8);
      var7.update(this.version, 0, this.version.length);
      var7.update(var1, 0, var1.length);
      var7.update(var2, var3, var4);
      var7.update(this.versionSize, 0, this.version.length);
      byte[] var5 = var7.doFinal();
      System.arraycopy(var5, 0, var6, 0, var6.length);
      return var6;
   }
}
