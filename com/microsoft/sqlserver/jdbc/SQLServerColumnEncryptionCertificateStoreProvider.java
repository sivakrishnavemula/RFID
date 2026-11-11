package com.microsoft.sqlserver.jdbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

public final class SQLServerColumnEncryptionCertificateStoreProvider extends SQLServerColumnEncryptionKeyStoreProvider {
   private static final Logger windowsCertificateStoreLogger = Logger.getLogger(
      "com.microsoft.sqlserver.jdbc.SQLServerColumnEncryptionCertificateStoreProvider"
   );
   static boolean isWindows;
   String name = "MSSQL_CERTIFICATE_STORE";
   static final String localMachineDirectory = "LocalMachine";
   static final String currentUserDirectory = "CurrentUser";
   static final String myCertificateStore = "My";
   private Path keyStoreDirectoryPath = null;

   public SQLServerColumnEncryptionCertificateStoreProvider() {
      windowsCertificateStoreLogger.entering(
         SQLServerColumnEncryptionCertificateStoreProvider.class.getName(), "SQLServerColumnEncryptionCertificateStoreProvider"
      );
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public byte[] encryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException {
      throw new SQLServerException(null, SQLServerException.getErrString("R_InvalidWindowsCertificateStoreEncryption"), null, 0, false);
   }

   private byte[] decryptColumnEncryptionKeyWindows(String var1, String var2, byte[] var3) throws SQLServerException {
      try {
         return AuthenticationJNI.DecryptColumnEncryptionKey(var1, var2, var3);
      } catch (DLLException var5) {
         DLLException.buildException(var5.GetErrCode(), var5.GetParam1(), var5.GetParam2(), var5.GetParam3());
         return null;
      }
   }

   private CertificateDetails getCertificateDetails(String var1) throws SQLServerException {
      String var2 = null;
      String[] var3 = var1.split("/");
      if (var3.length > 3) {
         MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_AECertpathBad"));
         Object[] var12 = new Object[]{var1};
         throw new SQLServerException(var9.format(var12), null);
      }

      if (var3.length > 2) {
         if (var3[0].equalsIgnoreCase("LocalMachine")) {
            var2 = "LocalMachine";
         } else {
            if (!var3[0].equalsIgnoreCase("CurrentUser")) {
               MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_AECertLocBad"));
               Object[] var11 = new Object[]{var3[0], var1};
               throw new SQLServerException(var8.format(var11), null);
            }

            var2 = "CurrentUser";
         }
      }

      if (var3.length > 1 && !var3[var3.length - 2].equalsIgnoreCase("My")) {
         MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_AECertStoreBad"));
         Object[] var10 = new Object[]{var3[var3.length - 2], var1};
         throw new SQLServerException(var7.format(var10), null);
      }

      String var4 = var3[var3.length - 1];
      if (null != var4 && 0 != var4.length()) {
         return this.getCertificateByThumbprint(var2, var4, var1);
      }

      MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_AECertHashEmpty"));
      Object[] var6 = new Object[]{var1};
      throw new SQLServerException(var5.format(var6), null);
   }

   private String getThumbPrint(X509Certificate var1) throws NoSuchAlgorithmException, CertificateEncodingException {
      MessageDigest var2 = MessageDigest.getInstance("SHA-1");
      byte[] var3 = var1.getEncoded();
      var2.update(var3);
      byte[] var4 = var2.digest();
      return DatatypeConverter.printHexBinary(var4);
   }

   private CertificateDetails getCertificateByThumbprint(String var1, String var2, String var3) throws SQLServerException {
      InputStream var4 = null;
      if (null == this.keyStoreDirectoryPath) {
         MessageFormat var26 = new MessageFormat(SQLServerException.getErrString("R_AEKeyPathEmptyOrReserved"));
         Object[] var28 = new Object[]{this.keyStoreDirectoryPath};
         throw new SQLServerException(var26.format(var28), null);
      }

      Path var5 = this.keyStoreDirectoryPath.resolve(var1);
      KeyStore var6 = null;

      try {
         var6 = KeyStore.getInstance("PKCS12");
      } catch (KeyStoreException var21) {
         MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_CertificateError"));
         Object[] var9 = new Object[]{var3, this.name};
         throw new SQLServerException(this, var8.format(var9), null, 0, false);
      }

      File var7 = var5.toFile();
      File[] var29 = var7.listFiles();
      if (null != var29 && (null == var29 || 0 != var29.length)) {
         for (File var12 : var29) {
            if (!var12.isDirectory()) {
               char[] var13 = "".toCharArray();

               try {
                  var4 = new FileInputStream(var12);
                  var6.load(var4, var13);
               } catch (IOException | CertificateException | NoSuchAlgorithmException var24) {
                  continue;
               }

               try {
                  Enumeration var14 = var6.aliases();

                  while (var14.hasMoreElements()) {
                     String var31 = (String)var14.nextElement();
                     X509Certificate var32 = (X509Certificate)var6.getCertificate(var31);
                     if (var2.matches(this.getThumbPrint(var32))) {
                        Key var17 = null;

                        try {
                           var17 = var6.getKey(var31, "".toCharArray());
                           if (null == var17) {
                              MessageFormat var18 = new MessageFormat(SQLServerException.getErrString("R_UnrecoverableKeyAE"));
                              Object[] var34 = new Object[]{var3};
                              throw new SQLServerException(this, var18.format(var34), null, 0, false);
                           }
                        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException var22) {
                           MessageFormat var19 = new MessageFormat(SQLServerException.getErrString("R_UnrecoverableKeyAE"));
                           Object[] var20 = new Object[]{var3};
                           throw new SQLServerException(this, var19.format(var20), null, 0, false);
                        }

                        return new CertificateDetails(var32, var17);
                     }
                  }
               } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException var23) {
                  MessageFormat var15 = new MessageFormat(SQLServerException.getErrString("R_CertificateError"));
                  Object[] var16 = new Object[]{var3, this.name};
                  throw new SQLServerException(var15.format(var16), var23);
               }
            }
         }

         throw new SQLServerException(SQLServerException.getErrString("R_KeyStoreNotFound"), null);
      } else {
         throw new SQLServerException(SQLServerException.getErrString("R_KeyStoreNotFound"), null);
      }
   }

   private byte[] decryptColumnEncryptionKeyLinux(String var1, String var2, byte[] var3) throws SQLServerException {
      KeyStoreProviderCommon.validateNonEmptyMasterKeyPath(var1);
      CertificateDetails var4 = this.getCertificateDetails(var1);
      return KeyStoreProviderCommon.decryptColumnEncryptionKey(var1, var2, var3, var4);
   }

   public byte[] decryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException {
      windowsCertificateStoreLogger.entering(
         SQLServerColumnEncryptionCertificateStoreProvider.class.getName(), "decryptColumnEncryptionKey", "Decrypting Column Encryption Key."
      );
      B var4 = null;
      if (isWindows) {
         var4 = this.decryptColumnEncryptionKeyWindows(var1, var2, var3);
         windowsCertificateStoreLogger.exiting(
            SQLServerColumnEncryptionCertificateStoreProvider.class.getName(), "decryptColumnEncryptionKey", "Finished decrypting Column Encryption Key."
         );
         return (byte[])var4;
      } else {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), null);
      }
   }

   static {
      if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")) {
         isWindows = true;
      } else {
         isWindows = false;
      }
   }
}
