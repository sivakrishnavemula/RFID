package org.apache.poi.poifs.crypt.xor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.BitSet;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.util.LittleEndian;

public class XOREncryptor extends Encryptor implements Cloneable {
   protected XOREncryptor() {
   }

   public void confirmPassword(String password) {
      int keyComp = CryptoFunctions.createXorKey1(password);
      int verifierComp = CryptoFunctions.createXorVerifier1(password);
      byte[] xorArray = CryptoFunctions.createXorArray1(password);
      byte[] shortBuf = new byte[2];
      XOREncryptionVerifier ver = (XOREncryptionVerifier)this.getEncryptionInfo().getVerifier();
      LittleEndian.putUShort(shortBuf, 0, keyComp);
      ver.setEncryptedKey(shortBuf);
      LittleEndian.putUShort(shortBuf, 0, verifierComp);
      ver.setEncryptedVerifier(shortBuf);
      this.setSecretKey(new SecretKeySpec(xorArray, "XOR"));
   }

   public void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt) {
      this.confirmPassword(password);
   }

   public OutputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
      return new XOREncryptor.XORCipherOutputStream(dir);
   }

   public XOREncryptor.XORCipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
      return new XOREncryptor.XORCipherOutputStream(stream, initialOffset);
   }

   protected int getKeySizeInBytes() {
      return -1;
   }

   public void setChunkSize(int chunkSize) {
   }

   public XOREncryptor clone() throws CloneNotSupportedException {
      return (XOREncryptor)super.clone();
   }

   private class XORCipherOutputStream extends ChunkedCipherOutputStream {
      private int recordStart;
      private int recordEnd;

      public XORCipherOutputStream(OutputStream stream, int initialPos) throws IOException, GeneralSecurityException {
         super(stream, -1);
      }

      public XORCipherOutputStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
         super(dir, -1);
      }

      protected Cipher initCipherForBlock(Cipher cipher, int block, boolean lastChunk) throws GeneralSecurityException {
         return XORDecryptor.initCipherForBlock(cipher, block, XOREncryptor.this.getEncryptionInfo(), XOREncryptor.this.getSecretKey(), 1);
      }

      protected void calculateChecksum(File file, int i) {
      }

      protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) {
         throw new EncryptedDocumentException("createEncryptionInfoEntry not supported");
      }

      public void setNextRecordSize(int recordSize, boolean isPlain) {
         if (this.recordEnd > 0 && !isPlain) {
            this.invokeCipher((int)this.getPos(), true);
         }

         this.recordStart = (int)this.getTotalPos() + 4;
         this.recordEnd = this.recordStart + recordSize;
      }

      public void flush() throws IOException {
         this.setNextRecordSize(0, true);
         super.flush();
      }

      protected int invokeCipher(int posInChunk, boolean doFinal) {
         if (posInChunk == 0) {
            return 0;
         }

         int start = Math.max(posInChunk - (this.recordEnd - this.recordStart), 0);
         BitSet plainBytes = this.getPlainByteFlags();
         byte[] xorArray = XOREncryptor.this.getEncryptionInfo().getEncryptor().getSecretKey().getEncoded();
         byte[] chunk = this.getChunk();
         byte[] plain = plainBytes.isEmpty() ? null : (byte[])chunk.clone();
         int xorArrayIndex = this.recordEnd + (start - this.recordStart);

         for (int i = start; i < posInChunk; i++) {
            byte value = chunk[i];
            value = (byte)(value ^ xorArray[xorArrayIndex++ & 15]);
            value = this.rotateLeft(value, 5);
            chunk[i] = value;
         }

         if (plain != null) {
            for (int i = plainBytes.nextSetBit(start); i >= 0 && i < posInChunk; i = plainBytes.nextSetBit(i + 1)) {
               chunk[i] = plain[i];
            }
         }

         return posInChunk;
      }

      private byte rotateLeft(byte bits, int shift) {
         return (byte)((bits & 255) << shift | (bits & 255) >>> 8 - shift);
      }
   }
}
