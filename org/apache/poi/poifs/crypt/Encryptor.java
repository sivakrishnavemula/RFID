package org.apache.poi.poifs.crypt;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public abstract class Encryptor implements Cloneable {
   protected static final String DEFAULT_POIFS_ENTRY = "EncryptedPackage";
   private EncryptionInfo encryptionInfo;
   private SecretKey secretKey;

   public abstract OutputStream getDataStream(DirectoryNode var1) throws IOException, GeneralSecurityException;

   public abstract void confirmPassword(String var1, byte[] var2, byte[] var3, byte[] var4, byte[] var5, byte[] var6);

   public abstract void confirmPassword(String var1);

   public static Encryptor getInstance(EncryptionInfo info) {
      return info.getEncryptor();
   }

   public OutputStream getDataStream(POIFSFileSystem fs) throws IOException, GeneralSecurityException {
      return this.getDataStream(fs.getRoot());
   }

   public ChunkedCipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
      throw new EncryptedDocumentException("this decryptor doesn't support writing directly to a stream");
   }

   public SecretKey getSecretKey() {
      return this.secretKey;
   }

   public void setSecretKey(SecretKey secretKey) {
      this.secretKey = secretKey;
   }

   public EncryptionInfo getEncryptionInfo() {
      return this.encryptionInfo;
   }

   public void setEncryptionInfo(EncryptionInfo encryptionInfo) {
      this.encryptionInfo = encryptionInfo;
   }

   public void setChunkSize(int chunkSize) {
      throw new EncryptedDocumentException("this decryptor doesn't support changing the chunk size");
   }

   public Encryptor clone() throws CloneNotSupportedException {
      Encryptor other = (Encryptor)super.clone();
      other.secretKey = new SecretKeySpec(this.secretKey.getEncoded(), this.secretKey.getAlgorithm());
      return other;
   }
}
