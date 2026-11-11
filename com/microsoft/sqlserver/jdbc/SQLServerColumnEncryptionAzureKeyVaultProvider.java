package com.microsoft.sqlserver.jdbc;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.KeyVaultClientImpl;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.apache.http.impl.client.HttpClientBuilder;

public class SQLServerColumnEncryptionAzureKeyVaultProvider extends SQLServerColumnEncryptionKeyStoreProvider {
   String name = "AZURE_KEY_VAULT";
   private final String azureKeyVaultDomainName = "vault.azure.net";
   private final String rsaEncryptionAlgorithmWithOAEPForAKV = "RSA-OAEP";
   private final byte[] firstVersion = new byte[]{1};
   private KeyVaultClient keyVaultClient;
   private KeyVaultCredential credential;

   public void setName(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public SQLServerColumnEncryptionAzureKeyVaultProvider(SQLServerKeyVaultAuthenticationCallback var1, ExecutorService var2) throws SQLServerException {
      if (null == var1) {
         MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_NullValue"));
         Object[] var4 = new Object[]{"SQLServerKeyVaultAuthenticationCallback"};
         throw new SQLServerException(var5.format(var4), null);
      }

      this.credential = new KeyVaultCredential(var1);
      HttpClientBuilder var3 = HttpClientBuilder.create();
      this.keyVaultClient = new KeyVaultClientImpl(var3, var2, this.credential);
   }

   public byte[] decryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException {
      this.ValidateNonEmptyAKVPath(var1);
      if (null == var3) {
         throw new SQLServerException(SQLServerException.getErrString("R_NullEncryptedColumnEncryptionKey"), null);
      }

      if (0 == var3.length) {
         throw new SQLServerException(SQLServerException.getErrString("R_EmptyEncryptedColumnEncryptionKey"), null);
      }

      var2 = this.validateEncryptionAlgorithm(var2);
      int var4 = this.getAKVKeySize(var1);
      if (var3[0] != this.firstVersion[0]) {
         MessageFormat var22 = new MessageFormat(SQLServerException.getErrString("R_InvalidEcryptionAlgorithmVersion"));
         Object[] var23 = new Object[]{String.format("%02X ", new Object[]{var3[0]}), String.format("%02X ", new Object[]{this.firstVersion[0]})};
         throw new SQLServerException(this, var22.format(var23), null, 0, false);
      }

      int var5 = this.firstVersion.length;
      short var6 = this.convertTwoBytesToShort(var3, var5);
      var5 += 2;
      short var7 = this.convertTwoBytesToShort(var3, var5);
      var5 += 2;
      var5 += var6;
      if (var7 != var4) {
         MessageFormat var24 = new MessageFormat(SQLServerException.getErrString("R_AKVKeyLengthError"));
         Object[] var26 = new Object[]{var7, var4, var1};
         throw new SQLServerException(this, var24.format(var26), null, 0, false);
      }

      int var8 = var3.length - var5 - var7;
      if (var8 != var4) {
         MessageFormat var25 = new MessageFormat(SQLServerException.getErrString("R_AKVSignatureLengthError"));
         Object[] var27 = new Object[]{var8, var4, var1};
         throw new SQLServerException(this, var25.format(var27), null, 0, false);
      }

      byte[] var9 = new byte[var7];
      System.arraycopy(var3, var5, var9, 0, var7);
      var5 += var7;
      byte[] var10 = new byte[var8];
      System.arraycopy(var3, var5, var10, 0, var8);
      byte[] var11 = new byte[var3.length - var10.length];
      System.arraycopy(var3, 0, var11, 0, var3.length - var10.length);
      MessageDigest var12 = null;

      try {
         var12 = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException var16) {
         throw new SQLServerException(SQLServerException.getErrString("R_NoSHA256Algorithm"), null);
      }

      var12.update(var11);
      byte[] var13 = var12.digest();
      if (null == var13) {
         throw new SQLServerException(SQLServerException.getErrString("R_HashNull"), null);
      } else if (!this.AzureKeyVaultVerifySignature(var13, var10, var1)) {
         MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_CEKSignatureNotMatchCMK"));
         Object[] var15 = new Object[]{var1};
         throw new SQLServerException(this, var14.format(var15), null, 0, false);
      } else {
         return this.AzureKeyVaultUnWrap(var1, var2, var9);
      }
   }

   private short convertTwoBytesToShort(byte[] var1, int var2) throws SQLServerException {
      byte var3 = -1;
      if (var2 + 1 >= var1.length) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_ByteToShortConversion"), null, 0, false);
      }

      ByteBuffer var4 = ByteBuffer.allocate(2);
      var4.order(ByteOrder.LITTLE_ENDIAN);
      var4.put(var1[var2]);
      var4.put(var1[var2 + 1]);
      return var4.getShort(0);
   }

   public byte[] encryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException {
      this.ValidateNonEmptyAKVPath(var1);
      if (null == var3) {
         throw new SQLServerException(SQLServerException.getErrString("R_NullColumnEncryptionKey"), null);
      }

      if (0 == var3.length) {
         throw new SQLServerException(SQLServerException.getErrString("R_EmptyCEK"), null);
      }

      var2 = this.validateEncryptionAlgorithm(var2);
      int var4 = this.getAKVKeySize(var1);
      byte[] var5 = new byte[]{this.firstVersion[0]};
      Object var6 = null;

      try {
         var6 = var1.toLowerCase().getBytes("UTF-16LE");
      } catch (UnsupportedEncodingException var19) {
         MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         Object[] var9 = new Object[]{"UTF-16LE"};
         throw new SQLServerException(this, var8.format(var9), null, 0, false);
      }

      byte[] var7 = new byte[]{(byte)((short)((Object[])var6).length & 0xFF), (byte)((short)((Object[])var6).length >> 8 & 0xFF)};
      byte[] var22 = this.AzureKeyVaultWrap(var1, var2, var3);
      byte[] var23 = new byte[]{(byte)((short)var22.length & 0xFF), (byte)((short)var22.length >> 8 & 0xFF)};
      if (var22.length != var4) {
         throw new SQLServerException(SQLServerException.getErrString("R_CipherTextLengthNotMatchRSASize"), null);
      }

      byte[] var10 = new byte[var5.length + var7.length + var23.length + ((Object[])var6).length + var22.length];
      int var11 = var5.length;
      System.arraycopy(var5, 0, var10, 0, var5.length);
      System.arraycopy(var7, 0, var10, var11, var7.length);
      var11 += var7.length;
      System.arraycopy(var23, 0, var10, var11, var23.length);
      var11 += var23.length;
      System.arraycopy(var6, 0, var10, var11, ((Object[])var6).length);
      var11 += ((Object[])var6).length;
      System.arraycopy(var22, 0, var10, var11, var22.length);
      MessageDigest var12 = null;

      try {
         var12 = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException var18) {
         throw new SQLServerException(SQLServerException.getErrString("R_NoSHA256Algorithm"), null);
      }

      var12.update(var10);
      byte[] var13 = var12.digest();
      byte[] var14 = null;
      var14 = this.AzureKeyVaultSignHashedData(var13, var1);
      if (var14.length != var4) {
         throw new SQLServerException(SQLServerException.getErrString("R_SignedHashLengthError"), null);
      }

      if (!this.AzureKeyVaultVerifySignature(var13, var14, var1)) {
         throw new SQLServerException(SQLServerException.getErrString("R_InvalidSignatureComputed"), null);
      }

      int var15 = var5.length + var23.length + var7.length + var22.length + ((Object[])var6).length + var14.length;
      byte[] var16 = new byte[var15];
      int var17 = 0;
      System.arraycopy(var5, 0, var16, var17, var5.length);
      var17 += var5.length;
      System.arraycopy(var7, 0, var16, var17, var7.length);
      var17 += var7.length;
      System.arraycopy(var23, 0, var16, var17, var23.length);
      var17 += var23.length;
      System.arraycopy(var6, 0, var16, var17, ((Object[])var6).length);
      var17 += ((Object[])var6).length;
      System.arraycopy(var22, 0, var16, var17, var22.length);
      var17 += var22.length;
      System.arraycopy(var14, 0, var16, var17, var14.length);
      return var16;
   }

   private String validateEncryptionAlgorithm(String var1) throws SQLServerException {
      if (null == var1) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_NullKeyEncryptionAlgorithm"), null, 0, false);
      }

      if (var1.equalsIgnoreCase("RSA_OAEP")) {
         var1 = "RSA-OAEP";
      }

      if (!"RSA-OAEP".equalsIgnoreCase(var1.trim())) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_InvalidKeyEncryptionAlgorithm"));
         Object[] var3 = new Object[]{var1, "RSA-OAEP"};
         throw new SQLServerException(this, var2.format(var3), null, 0, false);
      } else {
         return var1;
      }
   }

   private void ValidateNonEmptyAKVPath(String var1) throws SQLServerException {
      if (null != var1 && !var1.trim().isEmpty()) {
         URI var7 = null;

         try {
            var7 = new URI(var1);
         } catch (URISyntaxException var6) {
            MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_AKVURLInvalid"));
            Object[] var5 = new Object[]{var1};
            throw new SQLServerException(null, var4.format(var5), null, 0, false);
         }

         if (!var7.getHost().toLowerCase().endsWith("vault.azure.net")) {
            MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_AKVMasterKeyPathInvalid"));
            Object[] var10 = new Object[]{var1};
            throw new SQLServerException(null, var9.format(var10), null, 0, false);
         }
      } else {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_AKVPathNull"));
         Object[] var3 = new Object[]{var1};
         throw new SQLServerException(null, var2.format(var3), null, 0, false);
      }
   }

   private byte[] AzureKeyVaultWrap(String var1, String var2, byte[] var3) throws SQLServerException {
      if (null == var3) {
         throw new SQLServerException(SQLServerException.getErrString("R_CEKNull"), null);
      }

      KeyOperationResult var4 = null;

      try {
         var4 = (KeyOperationResult)this.keyVaultClient.wrapKeyAsync(var1, var2, var3).get();
      } catch (InterruptedException | ExecutionException var6) {
         throw new SQLServerException(SQLServerException.getErrString("R_EncryptCEKError"), null);
      }

      return var4.getResult();
   }

   private byte[] AzureKeyVaultUnWrap(String var1, String var2, byte[] var3) throws SQLServerException {
      if (null == var3) {
         throw new SQLServerException(SQLServerException.getErrString("R_EncryptedCEKNull"), null);
      }

      if (0 == var3.length) {
         throw new SQLServerException(SQLServerException.getErrString("R_EmptyEncryptedCEK"), null);
      }

      KeyOperationResult var4;
      try {
         var4 = (KeyOperationResult)this.keyVaultClient.unwrapKeyAsync(var1, var2, var3).get();
      } catch (InterruptedException | ExecutionException var6) {
         throw new SQLServerException(SQLServerException.getErrString("R_DecryptCEKError"), null);
      }

      return var4.getResult();
   }

   private byte[] AzureKeyVaultSignHashedData(byte[] var1, String var2) throws SQLServerException {
      assert null != var1 && 0 != var1.length;
      KeyOperationResult var3 = null;

      try {
         var3 = (KeyOperationResult)this.keyVaultClient.signAsync(var2, "RS256", var1).get();
      } catch (InterruptedException | ExecutionException var5) {
         throw new SQLServerException(SQLServerException.getErrString("R_GenerateSignature"), null);
      }

      return var3.getResult();
   }

   private boolean AzureKeyVaultVerifySignature(byte[] var1, byte[] var2, String var3) throws SQLServerException {
      assert null != var1 && 0 != var1.length;
      assert null != var2 && 0 != var2.length;
      boolean var4 = false;

      try {
         return (Boolean)this.keyVaultClient.verifyAsync(var3, "RS256", var1, var2).get();
      } catch (InterruptedException | ExecutionException var6) {
         throw new SQLServerException(SQLServerException.getErrString("R_VerifySignature"), null);
      }
   }

   private int getAKVKeySize(String var1) throws SQLServerException {
      KeyBundle var2 = null;

      try {
         var2 = (KeyBundle)this.keyVaultClient.getKeyAsync(var1).get();
      } catch (InterruptedException | ExecutionException var5) {
         throw new SQLServerException(SQLServerException.getErrString("R_GetAKVKeySize"), null);
      }

      if (!var2.getKey().getKty().equalsIgnoreCase("RSA") && !var2.getKey().getKty().equalsIgnoreCase("RSA-HSM")) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_NonRSAKey"));
         Object[] var4 = new Object[]{var2.getKey().getKty()};
         throw new SQLServerException(null, var3.format(var4), null, 0, false);
      } else {
         return var2.getKey().getN().length;
      }
   }
}
