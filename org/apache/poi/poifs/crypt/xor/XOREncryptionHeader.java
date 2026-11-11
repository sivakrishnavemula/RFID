package org.apache.poi.poifs.crypt.xor;

import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;

public class XOREncryptionHeader extends EncryptionHeader implements EncryptionRecord, Cloneable {
   protected XOREncryptionHeader() {
   }

   public void write(LittleEndianByteArrayOutputStream leos) {
   }

   public XOREncryptionHeader clone() throws CloneNotSupportedException {
      return (XOREncryptionHeader)super.clone();
   }
}
