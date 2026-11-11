package org.apache.poi.poifs.crypt.cryptoapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor.StreamDescriptorEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;

public class CryptoAPIEncryptor extends Encryptor implements Cloneable {
   private int chunkSize = 512;

   CryptoAPIEncryptor() {
   }

   public void confirmPassword(String password) {
      Random r = new SecureRandom();
      byte[] salt = new byte[16];
      byte[] verifier = new byte[16];
      r.nextBytes(salt);
      r.nextBytes(verifier);
      this.confirmPassword(password, null, null, verifier, salt, null);
   }

   public void confirmPassword(String password, byte[] keySpec, byte[] keySalt, byte[] verifier, byte[] verifierSalt, byte[] integritySalt) {
      assert verifier != null && verifierSalt != null;
      CryptoAPIEncryptionVerifier ver = (CryptoAPIEncryptionVerifier)this.getEncryptionInfo().getVerifier();
      ver.setSalt(verifierSalt);
      SecretKey skey = CryptoAPIDecryptor.generateSecretKey(password, ver);
      this.setSecretKey(skey);

      try {
         Cipher cipher = this.initCipherForBlock(null, 0);
         byte[] encryptedVerifier = new byte[verifier.length];
         cipher.update(verifier, 0, verifier.length, encryptedVerifier);
         ver.setEncryptedVerifier(encryptedVerifier);
         HashAlgorithm hashAlgo = ver.getHashAlgorithm();
         MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
         byte[] calcVerifierHash = hashAlg.digest(verifier);
         byte[] encryptedVerifierHash = cipher.doFinal(calcVerifierHash);
         ver.setEncryptedVerifierHash(encryptedVerifierHash);
      } catch (GeneralSecurityException e) {
         throw new EncryptedDocumentException("Password confirmation failed", e);
      }
   }

   public Cipher initCipherForBlock(Cipher cipher, int block) throws GeneralSecurityException {
      return CryptoAPIDecryptor.initCipherForBlock(cipher, block, this.getEncryptionInfo(), this.getSecretKey(), 1);
   }

   public ChunkedCipherOutputStream getDataStream(DirectoryNode dir) throws IOException {
      throw new IOException("not supported");
   }

   public CryptoAPIEncryptor.CryptoAPICipherOutputStream getDataStream(OutputStream stream, int initialOffset) throws IOException, GeneralSecurityException {
      return new CryptoAPIEncryptor.CryptoAPICipherOutputStream(stream);
   }

   public void setSummaryEntries(DirectoryNode dir, String encryptedStream, POIFSFileSystem entries) throws IOException, GeneralSecurityException {
      CryptoAPIDocumentOutputStream bos = new CryptoAPIDocumentOutputStream(this);
      byte[] buf = new byte[8];
      bos.write(buf, 0, 8);
      List<StreamDescriptorEntry> descList = new ArrayList();
      int block = 0;

      for (Entry entry : entries.getRoot()) {
         if (!entry.isDirectoryEntry()) {
            StreamDescriptorEntry descEntry = new StreamDescriptorEntry();
            descEntry.block = block;
            descEntry.streamOffset = bos.size();
            descEntry.streamName = entry.getName();
            descEntry.flags = StreamDescriptorEntry.flagStream.setValue(0, 1);
            descEntry.reserved2 = 0;
            bos.setBlock(block);
            DocumentInputStream dis = dir.createDocumentInputStream(entry);
            IOUtils.copy(dis, bos);
            dis.close();
            descEntry.streamSize = bos.size() - descEntry.streamOffset;
            descList.add(descEntry);
            block++;
         }
      }

      int streamDescriptorArrayOffset = bos.size();
      bos.setBlock(0);
      LittleEndian.putUInt(buf, 0, descList.size());
      bos.write(buf, 0, 4);

      for (StreamDescriptorEntry sde : descList) {
         LittleEndian.putUInt(buf, 0, sde.streamOffset);
         bos.write(buf, 0, 4);
         LittleEndian.putUInt(buf, 0, sde.streamSize);
         bos.write(buf, 0, 4);
         LittleEndian.putUShort(buf, 0, sde.block);
         bos.write(buf, 0, 2);
         LittleEndian.putUByte(buf, 0, (short)sde.streamName.length());
         bos.write(buf, 0, 1);
         LittleEndian.putUByte(buf, 0, (short)sde.flags);
         bos.write(buf, 0, 1);
         LittleEndian.putUInt(buf, 0, sde.reserved2);
         bos.write(buf, 0, 4);
         byte[] nameBytes = StringUtil.getToUnicodeLE(sde.streamName);
         bos.write(nameBytes, 0, nameBytes.length);
         LittleEndian.putShort(buf, 0, (short)0);
         bos.write(buf, 0, 2);
      }

      int savedSize = bos.size();
      int streamDescriptorArraySize = savedSize - streamDescriptorArrayOffset;
      LittleEndian.putUInt(buf, 0, streamDescriptorArrayOffset);
      LittleEndian.putUInt(buf, 4, streamDescriptorArraySize);
      bos.reset();
      bos.setBlock(0);
      bos.write(buf, 0, 8);
      bos.setSize(savedSize);
      dir.createDocument(encryptedStream, new ByteArrayInputStream(bos.getBuf(), 0, savedSize));
   }

   public void setChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
   }

   public CryptoAPIEncryptor clone() throws CloneNotSupportedException {
      return (CryptoAPIEncryptor)super.clone();
   }

   protected class CryptoAPICipherOutputStream extends ChunkedCipherOutputStream {
      protected Cipher initCipherForBlock(Cipher cipher, int block, boolean lastChunk) throws IOException, GeneralSecurityException {
         this.flush();
         EncryptionInfo ei = CryptoAPIEncryptor.this.getEncryptionInfo();
         SecretKey sk = CryptoAPIEncryptor.this.getSecretKey();
         return CryptoAPIDecryptor.initCipherForBlock(cipher, block, ei, sk, 1);
      }

      protected void calculateChecksum(File file, int i) {
      }

      protected void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile) {
         throw new EncryptedDocumentException("createEncryptionInfoEntry not supported");
      }

      CryptoAPICipherOutputStream(OutputStream stream) throws IOException, GeneralSecurityException {
         super(stream, CryptoAPIEncryptor.this.chunkSize);
      }

      public void flush() throws IOException {
         this.writeChunk(false);
         super.flush();
      }
   }
}
