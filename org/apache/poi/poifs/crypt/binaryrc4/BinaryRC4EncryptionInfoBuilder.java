package org.apache.poi.poifs.crypt.binaryrc4;

import java.io.IOException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionInfoBuilder;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.util.LittleEndianInput;

public class BinaryRC4EncryptionInfoBuilder implements EncryptionInfoBuilder {
   public void initialize(EncryptionInfo info, LittleEndianInput dis) throws IOException {
      int vMajor = info.getVersionMajor();
      int vMinor = info.getVersionMinor();
      assert vMajor == 1 && vMinor == 1;
      info.setHeader(new BinaryRC4EncryptionHeader());
      info.setVerifier(new BinaryRC4EncryptionVerifier(dis));
      Decryptor dec = new BinaryRC4Decryptor();
      dec.setEncryptionInfo(info);
      info.setDecryptor(dec);
      Encryptor enc = new BinaryRC4Encryptor();
      enc.setEncryptionInfo(info);
      info.setEncryptor(enc);
   }

   public void initialize(
      EncryptionInfo info, CipherAlgorithm cipherAlgorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode
   ) {
      info.setHeader(new BinaryRC4EncryptionHeader());
      info.setVerifier(new BinaryRC4EncryptionVerifier());
      Decryptor dec = new BinaryRC4Decryptor();
      dec.setEncryptionInfo(info);
      info.setDecryptor(dec);
      Encryptor enc = new BinaryRC4Encryptor();
      enc.setEncryptionInfo(info);
      info.setEncryptor(enc);
   }
}
