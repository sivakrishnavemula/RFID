package org.apache.poi.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.extractor.EventBasedExcelExtractor;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public final class OLE2ExtractorFactory {
   private static final POILogger LOGGER = POILogFactory.getLogger(OLE2ExtractorFactory.class);
   private static final ThreadLocal<Boolean> threadPreferEventExtractors = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static Boolean allPreferEventExtractors;

   private OLE2ExtractorFactory() {
   }

   public static boolean getThreadPrefersEventExtractors() {
      return (Boolean)threadPreferEventExtractors.get();
   }

   public static Boolean getAllThreadsPreferEventExtractors() {
      return allPreferEventExtractors;
   }

   public static void setThreadPrefersEventExtractors(boolean preferEventExtractors) {
      threadPreferEventExtractors.set(preferEventExtractors);
   }

   public static void setAllThreadsPreferEventExtractors(Boolean preferEventExtractors) {
      allPreferEventExtractors = preferEventExtractors;
   }

   public static boolean getPreferEventExtractor() {
      return allPreferEventExtractors != null ? allPreferEventExtractors : (Boolean)threadPreferEventExtractors.get();
   }

   public static <T extends POITextExtractor> T createExtractor(POIFSFileSystem fs) throws IOException {
      return (T)createExtractor(fs.getRoot());
   }

   public static <T extends POITextExtractor> T createExtractor(InputStream input) throws IOException {
      Class<?> cls = getOOXMLClass();
      if (cls != null) {
         try {
            Method m = cls.getDeclaredMethod("createExtractor", new Class[]{InputStream.class});
            return (T)m.invoke(null, new Object[]{input});
         } catch (IllegalArgumentException iae) {
            throw iae;
         } catch (Exception e) {
            throw new IllegalArgumentException("Error creating Extractor for InputStream", e);
         }
      } else {
         return createExtractor(new POIFSFileSystem(input));
      }
   }

   private static Class<?> getOOXMLClass() {
      try {
         return OLE2ExtractorFactory.class.getClassLoader().loadClass("org.apache.poi.extractor.ExtractorFactory");
      } catch (ClassNotFoundException e) {
         LOGGER.log(5, new Object[]{"POI OOXML jar missing"});
         return null;
      }
   }

   private static Class<?> getScratchpadClass() {
      try {
         return OLE2ExtractorFactory.class.getClassLoader().loadClass("org.apache.poi.extractor.ole2.OLE2ScratchpadExtractorFactory");
      } catch (ClassNotFoundException e) {
         LOGGER.log(7, new Object[]{"POI Scratchpad jar missing"});
         throw new IllegalStateException("POI Scratchpad jar missing, required for ExtractorFactory");
      }
   }

   public static POITextExtractor createExtractor(DirectoryNode poifsDir) throws IOException {
      for (String workbookName : InternalWorkbook.WORKBOOK_DIR_ENTRY_NAMES) {
         if (poifsDir.hasEntry(workbookName)) {
            if (getPreferEventExtractor()) {
               return new EventBasedExcelExtractor(poifsDir);
            }

            return new ExcelExtractor(poifsDir);
         }
      }

      if (poifsDir.hasEntry("Book")) {
         throw new OldExcelFormatException("Old Excel Spreadsheet format (1-95) found. Please call OldExcelExtractor directly for basic text extraction");
      }

      Class<?> cls = getScratchpadClass();

      try {
         Method m = cls.getDeclaredMethod("createExtractor", new Class[]{DirectoryNode.class});
         POITextExtractor ext = (POITextExtractor)m.invoke(null, new Object[]{poifsDir});
         if (ext != null) {
            return ext;
         }
      } catch (IllegalArgumentException iae) {
         throw iae;
      } catch (Exception e) {
         throw new IllegalArgumentException("Error creating Scratchpad Extractor", e);
      }

      throw new IllegalArgumentException("No supported documents found in the OLE2 stream");
   }

   public static POITextExtractor[] getEmbededDocsTextExtractors(POIOLE2TextExtractor ext) throws IOException {
      List<Entry> dirs = new ArrayList();
      List<InputStream> nonPOIFS = new ArrayList();
      DirectoryEntry root = ext.getRoot();
      if (root == null) {
         throw new IllegalStateException("The extractor didn't know which POIFS it came from!");
      }

      if (ext instanceof ExcelExtractor) {
         Iterator<Entry> it = root.getEntries();

         while (it.hasNext()) {
            Entry entry = (Entry)it.next();
            if (entry.getName().startsWith("MBD")) {
               dirs.add(entry);
            }
         }
      } else {
         Class<?> cls = getScratchpadClass();

         try {
            Method m = cls.getDeclaredMethod("identifyEmbeddedResources", new Class[]{POIOLE2TextExtractor.class, List.class, List.class});
            m.invoke(null, new Object[]{ext, dirs, nonPOIFS});
         } catch (Exception e) {
            throw new IllegalArgumentException("Error checking for Scratchpad embedded resources", e);
         }
      }

      if (dirs.size() == 0 && nonPOIFS.size() == 0) {
         return new POITextExtractor[0];
      }

      ArrayList<POITextExtractor> e = new ArrayList();

      for (Entry dir : dirs) {
         e.add(createExtractor((DirectoryNode)dir));
      }

      for (InputStream nonPOIF : nonPOIFS) {
         try {
            e.add(createExtractor(nonPOIF));
         } catch (Exception xe) {
            LOGGER.log(5, new Object[]{xe});
         }
      }

      return (POITextExtractor[])e.toArray(new POITextExtractor[0]);
   }
}
