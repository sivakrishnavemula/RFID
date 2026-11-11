package com.microsoft.sqlserver.jdbc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class SQLServerColumnEncryptionJavaKeyStoreProvider extends SQLServerColumnEncryptionKeyStoreProvider {
   String name = "MSSQL_JAVA_KEYSTORE";
   String keyStorePath = null;
   char[] keyStorePwd = null;
   private static final Logger javaKeyStoreLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionJavaKeyStoreProvider");

   public void setName(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public SQLServerColumnEncryptionJavaKeyStoreProvider(String var1, char[] var2) throws SQLServerException {
      javaKeyStoreLogger.entering(SQLServerColumnEncryptionJavaKeyStoreProvider.class.getName(), "SQLServerColumnEncryptionJavaKeyStoreProvider");
      if (null != var1 && 0 != var1.length()) {
         this.keyStorePath = var1;
         if (javaKeyStoreLogger.isLoggable(Level.FINE)) {
            javaKeyStoreLogger.fine("Path of key store provider is set.");
         }

         if (null == var2) {
            var2 = "".toCharArray();
         }

         this.keyStorePwd = new char[var2.length];
         System.arraycopy(var2, 0, this.keyStorePwd, 0, var2.length);
         if (javaKeyStoreLogger.isLoggable(Level.FINE)) {
            javaKeyStoreLogger.fine("Password for key store provider is set.");
         }

         javaKeyStoreLogger.exiting(SQLServerColumnEncryptionJavaKeyStoreProvider.class.getName(), "SQLServerColumnEncryptionJavaKeyStoreProvider");
      } else {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_InvalidConnectionSetting"));
         Object[] var4 = new Object[]{"keyStoreLocation", var1};
         throw new SQLServerException(var3.format(var4), null);
      }
   }

   public byte[] decryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException {
      javaKeyStoreLogger.entering(
         SQLServerColumnEncryptionJavaKeyStoreProvider.class.getName(), "decryptColumnEncryptionKey", "Decrypting Column Encryption Key."
      );
      KeyStoreProviderCommon.validateNonEmptyMasterKeyPath(var1);
      CertificateDetails var4 = this.getCertificateDetails(var1);
      byte[] var5 = KeyStoreProviderCommon.decryptColumnEncryptionKey(var1, var2, var3, var4);
      javaKeyStoreLogger.exiting(
         SQLServerColumnEncryptionJavaKeyStoreProvider.class.getName(), "decryptColumnEncryptionKey", "Finished decrypting Column Encryption Key."
      );
      return var5;
   }

   private CertificateDetails getCertificateDetails(String var1) throws SQLServerException {
      FileInputStream var2 = null;
      KeyStore var3 = null;
      CertificateDetails var4 = null;

      try {
         if (null == var1 || 0 == var1.length()) {
            throw new SQLServerException(null, SQLServerException.getErrString("R_InvalidMasterKeyDetails"), null, 0, false);
         }

         try {
            var3 = KeyStore.getInstance("JKS");
            var2 = new FileInputStream(this.keyStorePath);
            var3.load(var2, this.keyStorePwd);
         } catch (IOException var17) {
            if (null != var2) {
               var2.close();
            }

            var3 = KeyStore.getInstance("PKCS12");
            var2 = new FileInputStream(this.keyStorePath);
            var3.load(var2, this.keyStorePwd);
         }

         var4 = this.getCertificateDetailsByAlias(var3, var1);
      } catch (FileNotFoundException var18) {
         throw new SQLServerException(this, SQLServerException.getErrString("R_KeyStoreNotFound"), null, 0, false);
      } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException var19) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_invalidKeyStoreFile"));
         Object[] var7 = new Object[]{this.keyStorePath};
         throw new SQLServerException(var6.format(var7), var19);
      } finally {
         try {
            if (null != var2) {
               var2.close();
            }
         } catch (IOException var16) {
         }
      }

      return var4;
   }

   private CertificateDetails getCertificateDetailsByAlias(KeyStore var1, String var2) throws SQLServerException {
      try {
         X509Certificate var3 = (X509Certificate)var1.getCertificate(var2);
         Key var10 = var1.getKey(var2, this.keyStorePwd);
         if (null == var3) {
            MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_CertificateNotFoundForAlias"));
            Object[] var6 = new Object[]{var2, "MSSQL_JAVA_KEYSTORE"};
            throw new SQLServerException(this, var12.format(var6), null, 0, false);
         } else if (null == var10) {
            throw new UnrecoverableKeyException();
         } else {
            return new CertificateDetails(var3, var10);
         }
      } catch (UnrecoverableKeyException var7) {
         MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_UnrecoverableKeyAE"));
         Object[] var11 = new Object[]{var2};
         throw new SQLServerException(this, var9.format(var11), null, 0, false);
      } catch (NoSuchAlgorithmException | KeyStoreException var8) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_CertificateError"));
         Object[] var5 = new Object[]{var2, this.name};
         throw new SQLServerException(var4.format(var5), var8);
      }
   }

   public byte[] encryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException {
      javaKeyStoreLogger.entering(
         SQLServerColumnEncryptionJavaKeyStoreProvider.class.getName(),
         Thread.currentThread().getStackTrace()[1].getMethodName(),
         "Encrypting Column Encryption Key."
      );
      byte[] var4 = KeyStoreProviderCommon.version;
      KeyStoreProviderCommon.validateNonEmptyMasterKeyPath(var1);
      if (null == var3) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_NullColumnEncryptionKey"), null, 0, false);
      }

      if (0 == var3.length) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_EmptyColumnEncryptionKey"), null, 0, false);
      }

      KeyStoreProviderCommon.validateEncryptionAlgorithm(var2, true);
      CertificateDetails var5 = this.getCertificateDetails(var1);
      byte[] var6 = this.encryptRSAOAEP(var3, var5);
      byte[] var7 = this.getLittleEndianBytesFromShort((short)var6.length);

      byte[] var8;
      try {
         var8 = var1.toLowerCase().getBytes("UTF-16LE");
      } catch (UnsupportedEncodingException var16) {
         MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         throw new SQLServerException(var10.format(new Object[]{"UTF-16LE"}), null, 0, null);
      }

      byte[] var9 = this.getLittleEndianBytesFromShort((short)var8.length);
      byte[] var17 = new byte[var4.length + var9.length + var7.length + var8.length + var6.length];
      int var11 = var4.length;
      System.arraycopy(var4, 0, var17, 0, var4.length);
      System.arraycopy(var9, 0, var17, var11, var9.length);
      var11 += var9.length;
      System.arraycopy(var7, 0, var17, var11, var7.length);
      var11 += var7.length;
      System.arraycopy(var8, 0, var17, var11, var8.length);
      var11 += var8.length;
      System.arraycopy(var6, 0, var17, var11, var6.length);
      byte[] var12 = this.rsaSignHashedData(var17, var5);
      int var13 = var4.length + var7.length + var9.length + var6.length + var8.length + var12.length;
      byte[] var14 = new byte[var13];
      int var15 = 0;
      System.arraycopy(var4, 0, var14, var15, var4.length);
      var15 += var4.length;
      System.arraycopy(var9, 0, var14, var15, var9.length);
      var15 += var9.length;
      System.arraycopy(var7, 0, var14, var15, var7.length);
      var15 += var7.length;
      System.arraycopy(var8, 0, var14, var15, var8.length);
      var15 += var8.length;
      System.arraycopy(var6, 0, var14, var15, var6.length);
      var15 += var6.length;
      System.arraycopy(var12, 0, var14, var15, var12.length);
      javaKeyStoreLogger.exiting(
         SQLServerColumnEncryptionJavaKeyStoreProvider.class.getName(),
         Thread.currentThread().getStackTrace()[1].getMethodName(),
         "Finished encrypting Column Encryption Key."
      );
      return var14;
   }

   private byte[] encryptRSAOAEP(byte[] var1, CertificateDetails var2) throws SQLServerException {
      Object var3 = null;

      try {
         Cipher var4 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
         var4.init(1, var2.certificate.getPublicKey());
         var4.update(var1);
         return var4.doFinal();
      } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException var7) {
         MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_EncryptionFailed"));
         Object[] var6 = new Object[]{var7.getMessage()};
         throw new SQLServerException(this, var5.format(var6), null, 0, false);
      }
   }

   private byte[] rsaSignHashedData(byte[] var1, CertificateDetails var2) throws SQLServerException {
      Object var4 = null;

      try {
         Signature var3 = Signature.getInstance("SHA256withRSA");
         var3.initSign((PrivateKey)var2.privateKey);
         var3.update(var1);
         return var3.sign();
      } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException var8) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_EncryptionFailed"));
         Object[] var7 = new Object[]{var8.getMessage()};
         throw new SQLServerException(this, var6.format(var7), null, 0, false);
      }
   }

   private byte[] getLittleEndianBytesFromShort(short var1) {
      ByteBuffer var2 = ByteBuffer.allocate(2);
      var2.order(ByteOrder.LITTLE_ENDIAN);
      return var2.putShort(var1).array();
   }
}
