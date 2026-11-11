package org.apache.poi;

import java.io.File;
import java.io.OutputStream;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public abstract class POIReadOnlyDocument extends POIDocument {
   protected POIReadOnlyDocument(DirectoryNode dir) {
      super(dir);
   }

   protected POIReadOnlyDocument(POIFSFileSystem fs) {
      super(fs);
   }

   public void write() {
      throw new IllegalStateException("Writing is not yet implemented for this Document Format");
   }

   public void write(File file) {
      throw new IllegalStateException("Writing is not yet implemented for this Document Format");
   }

   public void write(OutputStream out) {
      throw new IllegalStateException("Writing is not yet implemented for this Document Format");
   }
}
