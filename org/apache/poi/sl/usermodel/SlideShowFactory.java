package org.apache.poi.sl.usermodel;

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

public class SlideShowFactory {
   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(POIFSFileSystem fs) throws IOException {
      return create(fs, null);
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(POIFSFileSystem fs, String password) throws IOException {
      return create(fs.getRoot(), password);
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(DirectoryNode root) throws IOException {
      return create(root, null);
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(DirectoryNode root, String password) throws IOException {
      if (root.hasEntry("EncryptedPackage")) {
         InputStream stream = null;

         try {
            stream = DocumentFactoryHelper.getDecryptedStream(root, password);
            return createXSLFSlideShow(stream);
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
            return createHSLFSlideShow(root);
         } finally {
            if (passwordSet) {
               Biff8EncryptionKey.setCurrentUserPassword(null);
            }
         }
      }
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(InputStream inp) throws IOException, EncryptedDocumentException {
      return create(inp, null);
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(InputStream inp, String password) throws IOException, EncryptedDocumentException {
      InputStream is = FileMagic.prepareToCheckMagic(inp);
      FileMagic fm = FileMagic.valueOf(is);
      switch (fm) {
         case OLE2:
            POIFSFileSystem fs = new POIFSFileSystem(is);
            return create(fs, password);
         case OOXML:
            return createXSLFSlideShow(is);
         default:
            throw new IOException("Your InputStream was neither an OLE2 stream, nor an OOXML stream");
      }
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(File file) throws IOException, EncryptedDocumentException {
      return create(file, null);
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(File file, String password) throws IOException, EncryptedDocumentException {
      return create(file, password, false);
   }

   public static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> create(File file, String password, boolean readOnly) throws IOException, EncryptedDocumentException {
      if (!file.exists()) {
         throw new FileNotFoundException(file.toString());
      }

      POIFSFileSystem fs = null;

      try {
         fs = new POIFSFileSystem(file, readOnly);
         return create(fs, password);
      } catch (OfficeXmlFileException e) {
         IOUtils.closeQuietly(fs);
         return createXSLFSlideShow(file, readOnly);
      } catch (RuntimeException e) {
         IOUtils.closeQuietly(fs);
         throw e;
      }
   }

   private static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> createHSLFSlideShow(Object... args) throws IOException, EncryptedDocumentException {
      return createSlideShow("org.apache.poi.hslf.usermodel.HSLFSlideShowFactory", args);
   }

   private static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> createXSLFSlideShow(Object... args) throws IOException, EncryptedDocumentException {
      return createSlideShow("org.apache.poi.xslf.usermodel.XSLFSlideShowFactory", args);
   }

   private static <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> SlideShow<S, P> createSlideShow(String factoryClass, Object[] args) throws IOException, EncryptedDocumentException {
      try {
         Class<?> clazz = SlideShowFactory.class.getClassLoader().loadClass(factoryClass);
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

         Method m = clazz.getMethod("createSlideShow", argsClz);
         return (SlideShow<S, P>)m.invoke(null, args);
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
            throw new IOException(t);
         }
      } catch (Exception e) {
         throw new IOException(e);
      }
   }
}
