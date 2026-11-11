package org.apache.poi.hssf.usermodel;

import java.io.IOException;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.Internal;

@Internal
public class HSSFWorkbookFactory extends WorkbookFactory {
   public static HSSFWorkbook createWorkbook(POIFSFileSystem fs) throws IOException {
      return new HSSFWorkbook(fs);
   }

   public static HSSFWorkbook createWorkbook(DirectoryNode root) throws IOException {
      return new HSSFWorkbook(root, true);
   }
}
