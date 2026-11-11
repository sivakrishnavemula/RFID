package org.apache.poi.ss.usermodel;

import java.io.IOException;
import org.apache.poi.poifs.filesystem.DirectoryEntry;

public interface ObjectData extends SimpleShape {
   byte[] getObjectData() throws IOException;

   boolean hasDirectoryEntry();

   DirectoryEntry getDirectory() throws IOException;

   String getOLE2ClassName();

   String getFileName();

   PictureData getPictureData();

   default String getContentType() {
      return "binary/octet-stream";
   }
}
