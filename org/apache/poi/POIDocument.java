package org.apache.poi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor;
import org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptor;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public abstract class POIDocument implements Closeable {
   private SummaryInformation sInf;
   private DocumentSummaryInformation dsInf;
   private DirectoryNode directory;
   private static final POILogger logger = POILogFactory.getLogger(POIDocument.class);
   private boolean initialized;

   protected POIDocument(DirectoryNode dir) {
      this.directory = dir;
   }

   protected POIDocument(POIFSFileSystem fs) {
      this(fs.getRoot());
   }

   public DocumentSummaryInformation getDocumentSummaryInformation() {
      if (!this.initialized) {
         this.readProperties();
      }

      return this.dsInf;
   }

   public SummaryInformation getSummaryInformation() {
      if (!this.initialized) {
         this.readProperties();
      }

      return this.sInf;
   }

   public void createInformationProperties() {
      if (!this.initialized) {
         this.readProperties();
      }

      if (this.sInf == null) {
         this.sInf = PropertySetFactory.newSummaryInformation();
      }

      if (this.dsInf == null) {
         this.dsInf = PropertySetFactory.newDocumentSummaryInformation();
      }
   }

   protected void readProperties() {
      if (!this.initialized) {
         DocumentSummaryInformation dsi = this.readPropertySet(DocumentSummaryInformation.class, "\u0005DocumentSummaryInformation");
         if (dsi != null) {
            this.dsInf = dsi;
         }

         SummaryInformation si = this.readPropertySet(SummaryInformation.class, "\u0005SummaryInformation");
         if (si != null) {
            this.sInf = si;
         }

         this.initialized = true;
      }
   }

   private <T> T readPropertySet(Class<T> clazz, String name) {
      String localName = clazz.getName().substring(clazz.getName().lastIndexOf(46) + 1);

      try {
         PropertySet ps = this.getPropertySet(name);
         if (clazz.isInstance(ps)) {
            return (T)ps;
         }

         if (ps != null) {
            logger.log(5, new Object[]{localName + " property set came back with wrong class - " + ps.getClass().getName()});
         } else {
            logger.log(5, new Object[]{localName + " property set came back as null"});
         }
      } catch (IOException e) {
         logger.log(7, new Object[]{"can't retrieve property set", e});
      }

      return null;
   }

   protected PropertySet getPropertySet(String setName) throws IOException {
      return this.getPropertySet(setName, this.getEncryptionInfo());
   }

   protected PropertySet getPropertySet(String setName, EncryptionInfo encryptionInfo) throws IOException {
      DirectoryNode dirNode = this.directory;
      POIFSFileSystem encPoifs = null;
      String step = "getting";

      try {
         if (encryptionInfo != null && encryptionInfo.isDocPropsEncrypted()) {
            step = "getting encrypted";
            String encryptedStream = this.getEncryptedPropertyStreamName();
            if (!dirNode.hasEntry(encryptedStream)) {
               throw new EncryptedDocumentException("can't find encrypted property stream '" + encryptedStream + "'");
            }

            CryptoAPIDecryptor dec = (CryptoAPIDecryptor)encryptionInfo.getDecryptor();
            encPoifs = dec.getSummaryEntries(dirNode, encryptedStream);
            dirNode = encPoifs.getRoot();
         }

         if (dirNode != null && dirNode.hasEntry(setName)) {
            step = "getting";
            DocumentInputStream dis = dirNode.createDocumentInputStream(dirNode.getEntry(setName));
            Throwable var34 = null;

            try {
               step = "creating";
               return PropertySetFactory.create(dis);
            } catch (Throwable var28) {
               var34 = var28;
               throw var28;
            } finally {
               if (dis != null) {
                  if (var34 != null) {
                     try {
                        dis.close();
                     } catch (Throwable var27) {
                        var34.addSuppressed(var27);
                     }
                  } else {
                     dis.close();
                  }
               }
            }
         } else {
            return null;
         }
      } catch (IOException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException("Error " + step + " property set with name " + setName, e);
      } finally {
         IOUtils.closeQuietly(encPoifs);
      }
   }

   protected void writeProperties() throws IOException {
      this.validateInPlaceWritePossible();
      this.writeProperties(this.directory.getFileSystem(), null);
   }

   protected void writeProperties(POIFSFileSystem outFS) throws IOException {
      this.writeProperties(outFS, null);
   }

   protected void writeProperties(POIFSFileSystem outFS, List<String> writtenEntries) throws IOException {
      EncryptionInfo ei = this.getEncryptionInfo();
      boolean encryptProps = ei != null && ei.isDocPropsEncrypted();
      POIFSFileSystem tmpFS = new POIFSFileSystem();
      Throwable var6 = null;

      try {
         POIFSFileSystem fs = encryptProps ? tmpFS : outFS;
         this.writePropertySet("\u0005SummaryInformation", this.getSummaryInformation(), fs, writtenEntries);
         this.writePropertySet("\u0005DocumentSummaryInformation", this.getDocumentSummaryInformation(), fs, writtenEntries);
         if (!encryptProps) {
            return;
         }

         this.writePropertySet("\u0005DocumentSummaryInformation", PropertySetFactory.newDocumentSummaryInformation(), outFS);
         if (outFS.getRoot().hasEntry("\u0005SummaryInformation")) {
            outFS.getRoot().getEntry("\u0005SummaryInformation").delete();
         }

         Encryptor encGen = ei.getEncryptor();
         if (!(encGen instanceof CryptoAPIEncryptor)) {
            throw new EncryptedDocumentException("Using " + ei.getEncryptionMode() + " encryption. Only CryptoAPI encryption supports encrypted property sets!");
         }

         CryptoAPIEncryptor enc = (CryptoAPIEncryptor)encGen;

         try {
            enc.setSummaryEntries(outFS.getRoot(), this.getEncryptedPropertyStreamName(), fs);
         } catch (GeneralSecurityException e) {
            throw new IOException(e);
         }
      } catch (Throwable var21) {
         var6 = var21;
         throw var21;
      } finally {
         if (tmpFS != null) {
            if (var6 != null) {
               try {
                  tmpFS.close();
               } catch (Throwable var19) {
                  var6.addSuppressed(var19);
               }
            } else {
               tmpFS.close();
            }
         }
      }
   }

   private void writePropertySet(String name, PropertySet ps, POIFSFileSystem outFS, List<String> writtenEntries) throws IOException {
      if (ps != null) {
         this.writePropertySet(name, ps, outFS);
         if (writtenEntries != null) {
            writtenEntries.add(name);
         }
      }
   }

   private void writePropertySet(String name, PropertySet set, POIFSFileSystem outFS) throws IOException {
      try {
         PropertySet mSet = new PropertySet(set);
         ByteArrayOutputStream bOut = new ByteArrayOutputStream();
         mSet.write(bOut);
         byte[] data = bOut.toByteArray();
         ByteArrayInputStream bIn = new ByteArrayInputStream(data);
         outFS.createOrUpdateDocument(bIn, name);
         logger.log(3, new Object[]{"Wrote property set " + name + " of size " + data.length});
      } catch (WritingNotSupportedException wnse) {
         logger.log(7, new Object[]{"Couldn't write property set with name " + name + " as not supported by HPSF yet"});
      }
   }

   protected void validateInPlaceWritePossible() throws IllegalStateException {
      if (this.directory == null) {
         throw new IllegalStateException("Newly created Document, cannot save in-place");
      } else if (this.directory.getParent() != null) {
         throw new IllegalStateException("This is not the root Document, cannot save embedded resource in-place");
      } else if (this.directory.getFileSystem() == null || !this.directory.getFileSystem().isInPlaceWriteable()) {
         throw new IllegalStateException("Opened read-only or via an InputStream, a Writeable File is required");
      }
   }

   public abstract void write() throws IOException;

   public abstract void write(File var1) throws IOException;

   public abstract void write(OutputStream var1) throws IOException;

   public void close() throws IOException {
      if (this.directory != null && this.directory.getNFileSystem() != null) {
         this.directory.getNFileSystem().close();
         this.clearDirectory();
      }
   }

   @Internal
   public DirectoryNode getDirectory() {
      return this.directory;
   }

   @Internal
   protected void clearDirectory() {
      this.directory = null;
   }

   @Internal
   protected boolean initDirectory() {
      if (this.directory == null) {
         this.directory = new POIFSFileSystem().getRoot();
         return true;
      } else {
         return false;
      }
   }

   @Internal
   protected void replaceDirectory(DirectoryNode newDirectory) {
      this.directory = newDirectory;
   }

   protected String getEncryptedPropertyStreamName() {
      return "encryption";
   }

   public EncryptionInfo getEncryptionInfo() throws IOException {
      return null;
   }
}
