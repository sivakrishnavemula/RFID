package org.apache.poi.poifs.crypt;

import java.io.IOException;
import org.apache.poi.util.LittleEndianInput;

public interface EncryptionInfoBuilder {
   void initialize(EncryptionInfo var1, LittleEndianInput var2) throws IOException;

   void initialize(EncryptionInfo var1, CipherAlgorithm var2, HashAlgorithm var3, int var4, int var5, ChainingMode var6);
}
