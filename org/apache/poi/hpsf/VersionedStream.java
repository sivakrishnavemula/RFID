package org.apache.poi.hpsf;

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

@Internal
public class VersionedStream {
   private final GUID _versionGuid = new GUID();
   private final IndirectPropertyName _streamName = new IndirectPropertyName();

   public void read(LittleEndianByteArrayInputStream lei) {
      this._versionGuid.read(lei);
      this._streamName.read(lei);
   }
}
