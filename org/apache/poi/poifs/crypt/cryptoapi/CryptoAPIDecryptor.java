package org.apache.poi.poifs.crypt.cryptoapi;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.ChunkedCipherInputStream;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.BoundedInputStream;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianInputStream;
import org.apache.poi.util.StringUtil;

public class CryptoAPIDecryptor extends Decryptor implements Cloneable {
   private long length = -1L;
   private int chunkSize = -1;

   protected CryptoAPIDecryptor() {
   }

   public boolean verifyPassword(String password) {
      EncryptionVerifier ver = this.getEncryptionInfo().getVerifier();
      SecretKey skey = generateSecretKey(password, ver);

      try {
         Cipher cipher = initCipherForBlock(null, 0, this.getEncryptionInfo(), skey, 2);
         byte[] encryptedVerifier = ver.getEncryptedVerifier();
         byte[] verifier = new byte[encryptedVerifier.length];
         cipher.update(encryptedVerifier, 0, encryptedVerifier.length, verifier);
         this.setVerifier(verifier);
         byte[] encryptedVerifierHash = ver.getEncryptedVerifierHash();
         byte[] verifierHash = cipher.doFinal(encryptedVerifierHash);
         HashAlgorithm hashAlgo = ver.getHashAlgorithm();
         MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
         byte[] calcVerifierHash = hashAlg.digest(verifier);
         if (Arrays.equals(calcVerifierHash, verifierHash)) {
            this.setSecretKey(skey);
            return true;
         } else {
            return false;
         }
      } catch (GeneralSecurityException e) {
         throw new EncryptedDocumentException(e);
      }
   }

   public Cipher initCipherForBlock(Cipher cipher, int block) throws GeneralSecurityException {
      EncryptionInfo ei = this.getEncryptionInfo();
      SecretKey sk = this.getSecretKey();
      return initCipherForBlock(cipher, block, ei, sk, 2);
   }

   protected static Cipher initCipherForBlock(Cipher cipher, int block, EncryptionInfo encryptionInfo, SecretKey skey, int encryptMode) throws GeneralSecurityException {
      EncryptionVerifier ver = encryptionInfo.getVerifier();
      HashAlgorithm hashAlgo = ver.getHashAlgorithm();
      byte[] blockKey = new byte[4];
      LittleEndian.putUInt(blockKey, 0, block);
      MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
      hashAlg.update(skey.getEncoded());
      byte[] encKey = hashAlg.digest(blockKey);
      EncryptionHeader header = encryptionInfo.getHeader();
      int keyBits = header.getKeySize();
      encKey = CryptoFunctions.getBlock0(encKey, keyBits / 8);
      if (keyBits == 40) {
         encKey = CryptoFunctions.getBlock0(encKey, 16);
      }

      SecretKey key = new SecretKeySpec(encKey, skey.getAlgorithm());
      if (cipher == null) {
         cipher = CryptoFunctions.getCipher(key, header.getCipherAlgorithm(), null, null, encryptMode);
      } else {
         cipher.init(encryptMode, key);
      }

      return cipher;
   }

   protected static SecretKey generateSecretKey(String password, EncryptionVerifier ver) {
      if (password.length() > 255) {
         password = password.substring(0, 255);
      }

      HashAlgorithm hashAlgo = ver.getHashAlgorithm();
      MessageDigest hashAlg = CryptoFunctions.getMessageDigest(hashAlgo);
      hashAlg.update(ver.getSalt());
      byte[] hash = hashAlg.digest(StringUtil.getToUnicodeLE(password));
      return new SecretKeySpec(hash, ver.getCipherAlgorithm().jceId);
   }

   public ChunkedCipherInputStream getDataStream(DirectoryNode dir) throws IOException, GeneralSecurityException {
      throw new IOException("not supported");
   }

   public ChunkedCipherInputStream getDataStream(InputStream stream, int size, int initialPos) throws IOException, GeneralSecurityException {
      return new CryptoAPIDecryptor.CryptoAPICipherInputStream(stream, size, initialPos);
   }

   public POIFSFileSystem getSummaryEntries(DirectoryNode root, String encryptedStream) throws IOException, GeneralSecurityException {
      DocumentNode es = (DocumentNode)root.getEntry(encryptedStream);
      DocumentInputStream dis = root.createDocumentInputStream(es);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      IOUtils.copy(dis, bos);
      dis.close();
      CryptoAPIDocumentInputStream sbis = new CryptoAPIDocumentInputStream(this, bos.toByteArray());
      LittleEndianInputStream leis = new LittleEndianInputStream(sbis);
      POIFSFileSystem fsOut = null;

      try {
         int streamDescriptorArrayOffset = (int)leis.readUInt();
         leis.readUInt();
         long skipN = streamDescriptorArrayOffset - 8L;
         if (sbis.skip(skipN) < skipN) {
            throw new EOFException("buffer underrun");
         }

         sbis.setBlock(0);
         int encryptedStreamDescriptorCount = (int)leis.readUInt();
         CryptoAPIDecryptor.StreamDescriptorEntry[] entries = new CryptoAPIDecryptor.StreamDescriptorEntry[encryptedStreamDescriptorCount];

         for (int i = 0; i < encryptedStreamDescriptorCount; i++) {
            CryptoAPIDecryptor.StreamDescriptorEntry entry = new CryptoAPIDecryptor.StreamDescriptorEntry();
            entries[i] = entry;
            entry.streamOffset = (int)leis.readUInt();
            entry.streamSize = (int)leis.readUInt();
            entry.block = leis.readUShort();
            int nameSize = leis.readUByte();
            entry.flags = leis.readUByte();
            entry.reserved2 = leis.readInt();
            entry.streamName = StringUtil.readUnicodeLE(leis, nameSize);
            leis.readShort();
            assert entry.streamName.length() == nameSize;
         }

         fsOut = new POIFSFileSystem();

         for (CryptoAPIDecryptor.StreamDescriptorEntry entry : entries) {
            sbis.seek(entry.streamOffset);
            sbis.setBlock(entry.block);
            InputStream is = new BoundedInputStream(sbis, entry.streamSize);
            fsOut.createDocument(is, entry.streamName);
            is.close();
         }
      } catch (Exception e) {
         IOUtils.closeQuietly(fsOut);
         if (e instanceof GeneralSecurityException) {
            throw (GeneralSecurityException)e;
         }

         if (e instanceof IOException) {
            throw (IOException)e;
         }

         throw new IOException("summary entries can't be read", e);
      } finally {
         IOUtils.closeQuietly(leis);
         IOUtils.closeQuietly(sbis);
      }

      return fsOut;
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

   public CryptoAPIDecryptor clone() throws CloneNotSupportedException {
      return (CryptoAPIDecryptor)super.clone();
   }

   private class CryptoAPICipherInputStream extends ChunkedCipherInputStream {
      protected Cipher initCipherForBlock(Cipher existing, int block) throws GeneralSecurityException {
         return CryptoAPIDecryptor.this.initCipherForBlock(existing, block);
      }

      public CryptoAPICipherInputStream(InputStream stream, long size, int initialPos) throws GeneralSecurityException {
         super(stream, size, CryptoAPIDecryptor.this.chunkSize, initialPos);
      }
   }

   static class StreamDescriptorEntry {
      static BitField flagStream = BitFieldFactory.getInstance(1);
      int streamOffset;
      int streamSize;
      int block;
      int flags;
      int reserved2;
      String streamName;
   }
}
