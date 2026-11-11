package org.apache.poi.util;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

public class DefaultTempFileCreationStrategy implements TempFileCreationStrategy {
   static final String POIFILES = "poifiles";
   public static final String KEEP_FILES = "poi.keep.tmp.files";
   private static final SecureRandom random = new SecureRandom();
   private File dir;

   public DefaultTempFileCreationStrategy() {
      this(null);
   }

   public DefaultTempFileCreationStrategy(File dir) {
      this.dir = dir;
   }

   private void createPOIFilesDirectory() throws IOException {
      if (this.dir == null) {
         String tmpDir = System.getProperty("java.io.tmpdir");
         if (tmpDir == null) {
            throw new IOException("Systems temporary directory not defined - set the -Djava.io.tmpdir jvm property!");
         }

         this.dir = new File(tmpDir, "poifiles");
      }

      this.createTempDirectory(this.dir);
   }

   private synchronized void createTempDirectory(File directory) throws IOException {
      boolean dirExists = directory.exists() || directory.mkdirs();
      if (!dirExists) {
         throw new IOException("Could not create temporary directory '" + directory + "'");
      }

      if (!directory.isDirectory()) {
         throw new IOException("Could not create temporary directory. '" + directory + "' exists but is not a directory.");
      }
   }

   public File createTempFile(String prefix, String suffix) throws IOException {
      this.createPOIFilesDirectory();
      File newFile = File.createTempFile(prefix, suffix, this.dir);
      if (System.getProperty("poi.keep.tmp.files") == null) {
         newFile.deleteOnExit();
      }

      return newFile;
   }

   public File createTempDirectory(String prefix) throws IOException {
      this.createPOIFilesDirectory();
      long n = random.nextLong();
      File newDirectory = new File(this.dir, prefix + Long.toString(n));
      this.createTempDirectory(newDirectory);
      if (System.getProperty("poi.keep.tmp.files") == null) {
         newDirectory.deleteOnExit();
      }

      return newDirectory;
   }
}
