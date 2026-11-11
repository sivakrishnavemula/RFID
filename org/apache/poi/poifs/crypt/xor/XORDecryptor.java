package org.apache.poi.poifs.crypt.xor;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherInputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.util.LittleEndian;

public class XORDecryptor extends Decryptor implements Cloneable {
   private long length = -1L;
   private int chunkSize = 512;

   protected XORDecryptor() {
   }

   public boolean verifyPassword(String password) {
      XOREncryptionVerifier ver = (XOREncryptionVerifier)this.getEncryptionInfo().getVerifier();
      int keyVer = LittleEndian.getUShort(ver.getEncryptedKey());
      int verifierVer = LittleEndian.getUShort(ver.getEncryptedVerifier());
      int keyComp = CryptoFunctions.createXorKey1(password);
      int verifierComp = CryptoFunctions.createXorVerifier1(password);
      if (keyVer == keyComp && verifierVer == verifierComp) {
         byte[] xorArray = CryptoFunctions.createXorArray1(password);
         this.setSecretKey(new SecretKeySpec(xorArray, "XOR"));
         return true;
      } else {
         return false;
      }
   }

   public Cipher initCipherForBlock(Cipher cipher, int block) throws GeneralSecurityException {
      return null;
   }

   protected static Cipher initCipherForBlock(Cipher cipher, int block, EncryptionInfo encryptionInfo, SecretKey skey, int encryptMode) throws GeneralSecurityException {
      return null;
   }

   public ChunkedCipherInputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
      throw new EncryptedDocumentException("not supported");
   }

   public InputStream getDataStream(InputStream stream, int size, int initialPos) throws IOException, GeneralSecurityException {
      return new XORDecryptor.XORCipherInputStream(stream, initialPos);
   }

   public long getLength() {
      if (this.length == -1L) {
         throw new IllegalStateException("Decryptor.getDataStream() was not called");
      } else {
         return this.length;
      }
   }

   public void setChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
   }

   public XORDecryptor clone() throws CloneNotSupportedException {
      return (XORDecryptor)super.clone();
   }

   private class XORCipherInputStream extends ChunkedCipherInputStream {
      private final int initialOffset;
      private int recordStart;
      private int recordEnd;

      public XORCipherInputStream(InputStream stream, int initialPos) throws GeneralSecurityException {
         super(stream, 2147483647L, XORDecryptor.this.chunkSize);
         this.initialOffset = initialPos;
      }

      protected Cipher initCipherForBlock(Cipher existing, int block) throws GeneralSecurityException {
         return XORDecryptor.this.initCipherForBlock(existing, block);
      }

      protected int invokeCipher(int totalBytes, boolean doFinal) {
         int pos = (int)this.getPos();
         byte[] xorArray = XORDecryptor.this.getEncryptionInfo().getDecryptor().getSecretKey().getEncoded();
         byte[] chunk = this.getChunk();
         byte[] plain = this.getPlain();
         int posInChunk = pos & this.getChunkMask();
         int xorArrayIndex = this.initialOffset + this.recordEnd + (pos - this.recordStart);

         for (int i = 0; pos + i < this.recordEnd && i < totalBytes; i++) {
            byte value = plain[posInChunk + i];
            value = this.rotateLeft(value, 3);
            value = (byte)(value ^ xorArray[xorArrayIndex + i & 15]);
            chunk[posInChunk + i] = value;
         }

         return totalBytes;
      }

      private byte rotateLeft(byte bits, int shift) {
         return (byte)((bits & 255) << shift | (bits & 255) >>> 8 - shift);
      }

      public void setNextRecordSize(int recordSize) {
         int pos = (int)this.getPos();
         byte[] chunk = this.getChunk();
         int chunkMask = this.getChunkMask();
         this.recordStart = pos;
         this.recordEnd = this.recordStart + recordSize;
         int nextBytes = Math.min(recordSize, chunk.length - (pos & chunkMask));
         this.invokeCipher(nextBytes, true);
      }
   }
}
