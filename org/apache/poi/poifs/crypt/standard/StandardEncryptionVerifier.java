package org.apache.poi.poifs.crypt.standard;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianInput;

public class StandardEncryptionVerifier extends EncryptionVerifier implements EncryptionRecord, Cloneable {
   private static final int SPIN_COUNT = 50000;
   private final int verifierHashSize;

   protected StandardEncryptionVerifier(LittleEndianInput is, StandardEncryptionHeader header) {
      int saltSize = is.readInt();
      if (saltSize != 16) {
         throw new RuntimeException("Salt size != 16 !?");
      }

      byte[] salt = new byte[16];
      is.readFully(salt);
      this.setSalt(salt);
      byte[] encryptedVerifier = new byte[16];
      is.readFully(encryptedVerifier);
      this.setEncryptedVerifier(encryptedVerifier);
      this.verifierHashSize = is.readInt();
      byte[] encryptedVerifierHash = new byte[header.getCipherAlgorithm().encryptedVerifierHashLength];
      is.readFully(encryptedVerifierHash);
      this.setEncryptedVerifierHash(encryptedVerifierHash);
      this.setSpinCount(50000);
      this.setCipherAlgorithm(header.getCipherAlgorithm());
      this.setChainingMode(header.getChainingMode());
      this.setEncryptedKey(null);
      this.setHashAlgorithm(header.getHashAlgorithm());
   }

   protected StandardEncryptionVerifier(CipherAlgorithm cipherAlgorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode) {
      this.setCipherAlgorithm(cipherAlgorithm);
      this.setHashAlgorithm(hashAlgorithm);
      this.setChainingMode(chainingMode);
      this.setSpinCount(50000);
      this.verifierHashSize = hashAlgorithm.hashSize;
   }

   protected void setSalt(byte[] salt) {
      if (salt != null && salt.length == 16) {
         super.setSalt(salt);
      } else {
         throw new EncryptedDocumentException("invalid verifier salt");
      }
   }

   protected void setEncryptedVerifier(byte[] encryptedVerifier) {
      super.setEncryptedVerifier(encryptedVerifier);
   }

   protected void setEncryptedVerifierHash(byte[] encryptedVerifierHash) {
      super.setEncryptedVerifierHash(encryptedVerifierHash);
   }

   public void write(LittleEndianByteArrayOutputStream bos) {
      byte[] salt = this.getSalt();
      assert salt.length == 16;
      bos.writeInt(salt.length);
      bos.write(salt);
      byte[] encryptedVerifier = this.getEncryptedVerifier();
      assert encryptedVerifier.length == 16;
      bos.write(encryptedVerifier);
      bos.writeInt(20);
      byte[] encryptedVerifierHash = this.getEncryptedVerifierHash();
      assert encryptedVerifierHash.length == this.getCipherAlgorithm().encryptedVerifierHashLength;
      bos.write(encryptedVerifierHash);
   }

   protected int getVerifierHashSize() {
      return this.verifierHashSize;
   }

   public StandardEncryptionVerifier clone() throws CloneNotSupportedException {
      return (StandardEncryptionVerifier)super.clone();
   }
}
