package org.apache.poi.ss.usermodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.OldFileFormatException;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Removal;

public class WorkbookFactory {
   public static Workbook create(POIFSFileSystem fs) throws IOException {
      return create(fs, null);
   }

   private static Workbook create(POIFSFileSystem fs, String password) throws IOException {
      return create(fs.getRoot(), password);
   }

   public static Workbook create(DirectoryNode root) throws IOException {
      return create(root, null);
   }

   public static Workbook create(DirectoryNode root, String password) throws IOException {
      if (root.hasEntry("EncryptedPackage")) {
         InputStream stream = null;

         try {
            stream = DocumentFactoryHelper.getDecryptedStream(root, password);
            return createXSSFWorkbook(stream);
         } finally {
            IOUtils.closeQuietly(stream);
            root.getFileSystem().close();
         }
      } else {
         boolean passwordSet = false;
         if (password != null) {
            Biff8EncryptionKey.setCurrentUserPassword(password);
            passwordSet = true;
         }

         try {
            return createHSSFWorkbook(root);
         } finally {
            if (passwordSet) {
               Biff8EncryptionKey.setCurrentUserPassword(null);
            }
         }
      }
   }

   @Deprecated
   @Removal(version = "4.2.0")
   public static Workbook create(Object pkg) throws IOException {
      return createXSSFWorkbook(pkg);
   }

   public static Workbook create(InputStream inp) throws IOException, EncryptedDocumentException {
      return create(inp, null);
   }

   public static Workbook create(InputStream inp, String password) throws IOException, EncryptedDocumentException {
      InputStream is = FileMagic.prepareToCheckMagic(inp);
      FileMagic fm = FileMagic.valueOf(is);
      switch (fm) {
         case OLE2:
            POIFSFileSystem fs = new POIFSFileSystem(is);
            return create(fs, password);
         case OOXML:
            return createXSSFWorkbook(is);
         default:
            throw new IOException("Your InputStream was neither an OLE2 stream, nor an OOXML stream");
      }
   }

   public static Workbook create(File file) throws IOException, EncryptedDocumentException {
      return create(file, null);
   }

   public static Workbook create(File file, String password) throws IOException, EncryptedDocumentException {
      return create(file, password, false);
   }

   public static Workbook create(File file, String password, boolean readOnly) throws IOException, EncryptedDocumentException {
      if (!file.exists()) {
         throw new FileNotFoundException(file.toString());
      }

      POIFSFileSystem fs = null;

      try {
         fs = new POIFSFileSystem(file, readOnly);
         return create(fs, password);
      } catch (OfficeXmlFileException e) {
         IOUtils.closeQuietly(fs);
         return createXSSFWorkbook(file, readOnly);
      } catch (RuntimeException e) {
         IOUtils.closeQuietly(fs);
         throw e;
      }
   }

   private static Workbook createHSSFWorkbook(Object... args) throws IOException, EncryptedDocumentException {
      return createWorkbook("org.apache.poi.hssf.usermodel.HSSFWorkbookFactory", args);
   }

   private static Workbook createXSSFWorkbook(Object... args) throws IOException, EncryptedDocumentException {
      return createWorkbook("org.apache.poi.xssf.usermodel.XSSFWorkbookFactory", args);
   }

   private static Workbook createWorkbook(String factoryClass, Object[] args) throws IOException, EncryptedDocumentException {
      try {
         Class<?> clazz = WorkbookFactory.class.getClassLoader().loadClass(factoryClass);
         Class<?>[] argsClz = new Class[args.length];
         int i = 0;

         for (Object o : args) {
            Class<?> c = o.getClass();
            if (Boolean.class.isAssignableFrom(c)) {
               c = boolean.class;
            } else if (InputStream.class.isAssignableFrom(c)) {
               c = InputStream.class;
            }

            argsClz[i++] = c;
         }

         Method m = clazz.getMethod("createWorkbook", argsClz);
         return (Workbook)m.invoke(null, args);
      } catch (InvocationTargetException e) {
         Throwable t = e.getCause();
         if (t instanceof IOException) {
            throw (IOException)t;
         } else if (t instanceof EncryptedDocumentException) {
            throw (EncryptedDocumentException)t;
         } else if (t instanceof OldFileFormatException) {
            throw (OldFileFormatException)t;
         } else if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
         } else {
            throw new IOException(t.getMessage(), t);
         }
      } catch (Exception e) {
         throw new IOException(e);
      }
   }
}
