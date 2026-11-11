package org.apache.poi.poifs.crypt.xor;

import java.io.IOException;
import org.apache.poi.poifs.crypt.ChainingMode;
import org.apache.poi.poifs.crypt.CipherAlgorithm;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionInfoBuilder;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.util.LittleEndianInput;

public class XOREncryptionInfoBuilder implements EncryptionInfoBuilder {
   public void initialize(EncryptionInfo info, LittleEndianInput dis) throws IOException {
      info.setHeader(new XOREncryptionHeader());
      info.setVerifier(new XOREncryptionVerifier(dis));
      Decryptor dec = new XORDecryptor();
      dec.setEncryptionInfo(info);
      info.setDecryptor(dec);
      Encryptor enc = new XOREncryptor();
      enc.setEncryptionInfo(info);
      info.setEncryptor(enc);
   }

   public void initialize(
      EncryptionInfo info, CipherAlgorithm cipherAlgorithm, HashAlgorithm hashAlgorithm, int keyBits, int blockSize, ChainingMode chainingMode
   ) {
      info.setHeader(new XOREncryptionHeader());
      info.setVerifier(new XOREncryptionVerifier());
      Decryptor dec = new XORDecryptor();
      dec.setEncryptionInfo(info);
      info.setDecryptor(dec);
      Encryptor enc = new XOREncryptor();
      enc.setEncryptionInfo(info);
      info.setEncryptor(enc);
   }
}
