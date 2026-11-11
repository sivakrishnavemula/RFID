package org.apache.poi.poifs.macros;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.macros.Module.ModuleType;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.RLEDecompressingInputStream;
import org.apache.poi.util.StringUtil;

public class VBAMacroReader implements Closeable {
   protected static final String VBA_PROJECT_OOXML = "vbaProject.bin";
   protected static final String VBA_PROJECT_POIFS = "VBA";
   private POIFSFileSystem fs;
   private static final int STREAMNAME_RESERVED = 50;
   private static final int PROJECT_CONSTANTS_RESERVED = 60;
   private static final int HELP_FILE_PATH_RESERVED = 61;
   private static final int REFERENCE_NAME_RESERVED = 62;
   private static final int DOC_STRING_RESERVED = 64;
   private static final int MODULE_DOCSTRING_RESERVED = 72;

   public VBAMacroReader(InputStream rstream) throws IOException {
      InputStream is = FileMagic.prepareToCheckMagic(rstream);
      FileMagic fm = FileMagic.valueOf(is);
      if (fm == FileMagic.OLE2) {
         this.fs = new POIFSFileSystem(is);
      } else {
         this.openOOXML(is);
      }
   }

   public VBAMacroReader(File file) throws IOException {
      try {
         this.fs = new POIFSFileSystem(file);
      } catch (OfficeXmlFileException e) {
         this.openOOXML(new FileInputStream(file));
      }
   }

   public VBAMacroReader(POIFSFileSystem fs) {
      this.fs = fs;
   }

   private void openOOXML(InputStream zipFile) throws IOException {
      ZipInputStream zis = new ZipInputStream(zipFile);
      Throwable var3 = null;

      try {
         ZipEntry zipEntry;
         while ((zipEntry = zis.getNextEntry()) != null) {
            if (StringUtil.endsWithIgnoreCase(zipEntry.getName(), "vbaProject.bin")) {
               try {
                  this.fs = new POIFSFileSystem(zis);
                  return;
               } catch (IOException e) {
                  zis.close();
                  throw e;
               }
            }
         }

         throw new IllegalArgumentException("No VBA project found");
      } catch (Throwable var16) {
         var3 = var16;
         throw var16;
      } finally {
         if (zis != null) {
            if (var3 != null) {
               try {
                  zis.close();
               } catch (Throwable var14) {
                  var3.addSuppressed(var14);
               }
            } else {
               zis.close();
            }
         }
      }
   }

   public void close() throws IOException {
      this.fs.close();
      this.fs = null;
   }

   public Map<String, Module> readMacroModules() throws IOException {
      VBAMacroReader.ModuleMap modules = new VBAMacroReader.ModuleMap();
      this.findMacros(this.fs.getRoot(), modules);
      this.findProjectProperties(this.fs.getRoot(), modules);
      Map<String, Module> moduleSources = new HashMap();

      for (Entry<String, VBAMacroReader.ModuleImpl> entry : modules.entrySet()) {
         VBAMacroReader.ModuleImpl module = (VBAMacroReader.ModuleImpl)entry.getValue();
         module.charset = modules.charset;
         moduleSources.put(entry.getKey(), module);
      }

      return moduleSources;
   }

   public Map<String, String> readMacros() throws IOException {
      Map<String, Module> modules = this.readMacroModules();
      Map<String, String> moduleSources = new HashMap();

      for (Entry<String, Module> entry : modules.entrySet()) {
         moduleSources.put(entry.getKey(), ((Module)entry.getValue()).getContent());
      }

      return moduleSources;
   }

   protected void findMacros(DirectoryNode dir, VBAMacroReader.ModuleMap modules) throws IOException {
      if ("VBA".equalsIgnoreCase(dir.getName())) {
         this.readMacros(dir, modules);
      } else {
         for (org.apache.poi.poifs.filesystem.Entry child : dir) {
            if (child instanceof DirectoryNode) {
               this.findMacros((DirectoryNode)child, modules);
            }
         }
      }
   }

   private static void readModuleMetadataFromDirStream(RLEDecompressingInputStream in, String streamName, VBAMacroReader.ModuleMap modules) throws IOException {
      int moduleOffset = in.readInt();
      VBAMacroReader.ModuleImpl module = (VBAMacroReader.ModuleImpl)modules.get(streamName);
      if (module == null) {
         module = new VBAMacroReader.ModuleImpl();
         module.offset = moduleOffset;
         modules.put(streamName, module);
      } else {
         InputStream stream = new RLEDecompressingInputStream(new ByteArrayInputStream(module.buf, moduleOffset, module.buf.length - moduleOffset));
         module.read(stream);
         stream.close();
      }
   }

   private static void readModuleFromDocumentStream(DocumentNode documentNode, String name, VBAMacroReader.ModuleMap modules) throws IOException {
      VBAMacroReader.ModuleImpl module = (VBAMacroReader.ModuleImpl)modules.get(name);
      if (module == null) {
         module = new VBAMacroReader.ModuleImpl();
         modules.put(name, module);
         InputStream dis = new DocumentInputStream(documentNode);
         Throwable compressed = null;

         try {
            module.read(dis);
         } catch (Throwable var34) {
            compressed = var34;
            throw var34;
         } finally {
            if (dis != null) {
               if (compressed != null) {
                  try {
                     dis.close();
                  } catch (Throwable var32) {
                     compressed.addSuppressed(var32);
                  }
               } else {
                  dis.close();
               }
            }
         }
      } else if (module.buf == null) {
         if (module.offset == null) {
            throw new IOException("Module offset for '" + name + "' was never read.");
         }

         InputStream decompressed = null;
         InputStream compressed = new DocumentInputStream(documentNode);

         try {
            long skippedBytes = compressed.skip(module.offset.intValue());
            if (skippedBytes != module.offset.intValue()) {
               throw new IOException("tried to skip " + module.offset + " bytes, but actually skipped " + skippedBytes + " bytes");
            }

            decompressed = new RLEDecompressingInputStream(compressed);
            module.read(decompressed);
            return;
         } catch (IllegalArgumentException | IllegalStateException var36) {
         } finally {
            IOUtils.closeQuietly(compressed);
            IOUtils.closeQuietly(decompressed);
         }

         InputStream var41 = new DocumentInputStream(documentNode);

         byte[] decompressedBytes;
         try {
            decompressedBytes = findCompressedStreamWBruteForce(var41);
         } finally {
            IOUtils.closeQuietly(var41);
         }

         if (decompressedBytes != null) {
            module.read(new ByteArrayInputStream(decompressedBytes));
         }

         return;
      }
   }

   private static void trySkip(InputStream in, long n) throws IOException {
      long skippedBytes = IOUtils.skipFully(in, n);
      if (skippedBytes != n) {
         if (skippedBytes < 0L) {
            throw new IOException("Tried skipping " + n + " bytes, but no bytes were skipped. The end of the stream has been reached or the stream is closed.");
         } else {
            throw new IOException(
               "Tried skipping " + n + " bytes, but only " + skippedBytes + " bytes were skipped. This should never happen with a non-corrupt file."
            );
         }
      }
   }

   protected void readMacros(DirectoryNode macroDir, VBAMacroReader.ModuleMap modules) throws IOException {
      for (String entryName : macroDir.getEntryNames()) {
         if ("dir".equalsIgnoreCase(entryName)) {
            this.processDirStream(macroDir.getEntry(entryName), modules);
            break;
         }
      }

      for (org.apache.poi.poifs.filesystem.Entry entry : macroDir) {
         if (entry instanceof DocumentNode) {
            String name = entry.getName();
            DocumentNode document = (DocumentNode)entry;
            if (!"dir".equalsIgnoreCase(name) && !StringUtil.startsWithIgnoreCase(name, "__SRP") && !StringUtil.startsWithIgnoreCase(name, "_VBA_PROJECT")) {
               readModuleFromDocumentStream(document, name, modules);
            }
         }
      }
   }

   protected void findProjectProperties(DirectoryNode node, VBAMacroReader.ModuleMap modules) throws IOException {
      for (org.apache.poi.poifs.filesystem.Entry entry : node) {
         if ("project".equalsIgnoreCase(entry.getName())) {
            DocumentNode document = (DocumentNode)entry;
            DocumentInputStream dis = new DocumentInputStream(document);
            this.readProjectProperties(dis, modules);
         } else {
            for (org.apache.poi.poifs.filesystem.Entry child : node) {
               if (child instanceof DirectoryNode) {
                  this.findProjectProperties((DirectoryNode)child, modules);
               }
            }
         }
      }
   }

   private void processDirStream(org.apache.poi.poifs.filesystem.Entry dir, VBAMacroReader.ModuleMap modules) throws IOException {
      DocumentNode dirDocumentNode = (DocumentNode)dir;
      VBAMacroReader.DIR_STATE dirState = VBAMacroReader.DIR_STATE.INFORMATION_RECORD;
      DocumentInputStream dis = new DocumentInputStream(dirDocumentNode);
      Throwable var6 = null;

      try {
         String streamName = null;
         int recordId = 0;
         boolean inReferenceTwiddled = false;

         try {
            RLEDecompressingInputStream in = new RLEDecompressingInputStream(dis);
            Throwable var11 = null;

            try {
               while (true) {
                  recordId = in.readShort();
                  if (recordId == -1) {
                     break;
                  }

                  VBAMacroReader.RecordType type = VBAMacroReader.RecordType.lookup(recordId);
                  if (type.equals(VBAMacroReader.RecordType.EOF) || type.equals(VBAMacroReader.RecordType.DIR_STREAM_TERMINATOR)) {
                     break;
                  }

                  switch (type) {
                     case PROJECT_VERSION:
                        trySkip(in, VBAMacroReader.RecordType.PROJECT_VERSION.getConstantLength());
                        break;
                     case PROJECT_CODEPAGE:
                        in.readInt();
                        int codepage = in.readShort();
                        modules.charset = Charset.forName(CodePageUtil.codepageToEncoding(codepage, true));
                        break;
                     case MODULE_STREAM_NAME:
                        VBAMacroReader.ASCIIUnicodeStringPair pair = this.readStringPair(in, modules.charset, 50);
                        streamName = pair.getAscii();
                        break;
                     case PROJECT_DOC_STRING:
                        this.readStringPair(in, modules.charset, 64);
                        break;
                     case PROJECT_HELP_FILE_PATH:
                        this.readStringPair(in, modules.charset, 61);
                        break;
                     case PROJECT_CONSTANTS:
                        this.readStringPair(in, modules.charset, 60);
                        break;
                     case REFERENCE_NAME:
                        if (dirState.equals(VBAMacroReader.DIR_STATE.INFORMATION_RECORD)) {
                           dirState = VBAMacroReader.DIR_STATE.REFERENCES_RECORD;
                        }

                        this.readStringPair(in, modules.charset, 62);
                        break;
                     case MODULE_DOC_STRING:
                        int modDocStringLength = in.readInt();
                        readString(in, modDocStringLength, modules.charset);
                        int modDocStringReserved = in.readShort();
                        if (modDocStringReserved != 72) {
                           throw new IOException(
                              "Expected x003C after stream name before Unicode stream name, but found: " + Integer.toHexString(modDocStringReserved)
                           );
                        }

                        int unicodeModDocStringLength = in.readInt();
                        this.readUnicodeString(in, unicodeModDocStringLength);
                        break;
                     case MODULE_OFFSET:
                        int modOffsetSz = in.readInt();
                        readModuleMetadataFromDirStream(in, streamName, modules);
                        break;
                     case PROJECT_MODULES:
                        dirState = VBAMacroReader.DIR_STATE.MODULES_RECORD;
                        in.readInt();
                        in.readShort();
                        break;
                     case REFERENCE_CONTROL_A:
                        int szTwiddled = in.readInt();
                        trySkip(in, szTwiddled);
                        int nextRecord = in.readShort();
                        if (nextRecord == VBAMacroReader.RecordType.REFERENCE_NAME.id) {
                           this.readStringPair(in, modules.charset, 62);
                           nextRecord = in.readShort();
                        }

                        if (nextRecord != 48) {
                           throw new IOException("Expected 0x30 as Reserved3 in a ReferenceControl record");
                        }

                        int szExtended = in.readInt();
                        trySkip(in, szExtended);
                        break;
                     case MODULE_TERMINATOR:
                        int endOfModulesReserved = in.readInt();
                        break;
                     default:
                        if (type.getConstantLength() > -1) {
                           trySkip(in, type.getConstantLength());
                        } else {
                           int recordLength = in.readInt();
                           trySkip(in, recordLength);
                        }
                  }
               }
            } catch (Throwable var47) {
               var11 = var47;
               throw var47;
            } finally {
               if (in != null) {
                  if (var11 != null) {
                     try {
                        in.close();
                     } catch (Throwable var46) {
                        var11.addSuppressed(var46);
                     }
                  } else {
                     in.close();
                  }
               }
            }
         } catch (IOException e) {
            throw new IOException("Error occurred while reading macros at section id " + recordId + " (" + HexDump.shortToHex(recordId) + ")", e);
         }
      } catch (Throwable var50) {
         var6 = var50;
         throw var50;
      } finally {
         if (dis != null) {
            if (var6 != null) {
               try {
                  dis.close();
               } catch (Throwable var45) {
                  var6.addSuppressed(var45);
               }
            } else {
               dis.close();
            }
         }
      }
   }

   private VBAMacroReader.ASCIIUnicodeStringPair readStringPair(RLEDecompressingInputStream in, Charset charset, int reservedByte) throws IOException {
      int nameLength = in.readInt();
      String ascii = readString(in, nameLength, charset);
      int reserved = in.readShort();
      if (reserved != reservedByte) {
         throw new IOException("Expected " + Integer.toHexString(reservedByte) + "after name before Unicode name, but found: " + Integer.toHexString(reserved));
      }

      int unicodeNameRecordLength = in.readInt();
      String unicode = this.readUnicodeString(in, unicodeNameRecordLength);
      return new VBAMacroReader.ASCIIUnicodeStringPair(ascii, unicode);
   }

   private static String readString(InputStream stream, int length, Charset charset) throws IOException {
      byte[] buffer = IOUtils.safelyAllocate(length, 20000);
      int bytesRead = IOUtils.readFully(stream, buffer);
      if (bytesRead != length) {
         throw new IOException("Tried to read: " + length + ", but could only read: " + bytesRead);
      } else {
         return new String(buffer, 0, length, charset);
      }
   }

   protected void readProjectProperties(DocumentInputStream dis, VBAMacroReader.ModuleMap modules) throws IOException {
      InputStreamReader reader = new InputStreamReader(dis, modules.charset);
      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[512];

      int read;
      while ((read = reader.read(buffer)) >= 0) {
         builder.append(buffer, 0, read);
      }

      String properties = builder.toString();

      for (String line : properties.split("\r\n|\n\r")) {
         if (!line.startsWith("[")) {
            String[] tokens = line.split("=");
            if (tokens.length > 1 && tokens[1].length() > 1 && tokens[1].startsWith("\"") && tokens[1].endsWith("\"")) {
               tokens[1] = tokens[1].substring(1, tokens[1].length() - 1);
            }

            if ("Document".equals(tokens[0])) {
               String mn = tokens[1].substring(0, tokens[1].indexOf("/&H"));
               VBAMacroReader.ModuleImpl module = (VBAMacroReader.ModuleImpl)modules.get(mn);
               module.moduleType = ModuleType.Document;
            } else if ("Module".equals(tokens[0])) {
               VBAMacroReader.ModuleImpl module = (VBAMacroReader.ModuleImpl)modules.get(tokens[1]);
               module.moduleType = ModuleType.Module;
            } else if ("Class".equals(tokens[0])) {
               VBAMacroReader.ModuleImpl module = (VBAMacroReader.ModuleImpl)modules.get(tokens[1]);
               module.moduleType = ModuleType.Class;
            }
         }
      }
   }

   private String readUnicodeString(RLEDecompressingInputStream in, int unicodeNameRecordLength) throws IOException {
      byte[] buffer = IOUtils.safelyAllocate(unicodeNameRecordLength, 20000);
      int bytesRead = IOUtils.readFully(in, buffer);
      if (bytesRead != unicodeNameRecordLength) {
      }

      return new String(buffer, StringUtil.UTF16LE);
   }

   private static byte[] findCompressedStreamWBruteForce(InputStream is) throws IOException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      IOUtils.copy(is, bos);
      byte[] compressed = bos.toByteArray();
      byte[] decompressed = null;

      for (int i = 0; i < compressed.length; i++) {
         if (compressed[i] == 1 && i < compressed.length - 1) {
            int w = LittleEndian.getUShort(compressed, i + 1);
            if (w > 0 && (w & 28672) == 12288) {
               decompressed = tryToDecompress(new ByteArrayInputStream(compressed, i, compressed.length - i));
               if (decompressed != null && decompressed.length > 9) {
                  int firstX = Math.min(20, decompressed.length);
                  String start = new String(decompressed, 0, firstX, StringUtil.WIN_1252);
                  if (start.contains("Attribute")) {
                     return decompressed;
                  }
               }
            }
         }
      }

      return decompressed;
   }

   private static byte[] tryToDecompress(InputStream is) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      try {
         IOUtils.copy(new RLEDecompressingInputStream(is), bos);
      } catch (IllegalArgumentException | IOException | IllegalStateException e) {
         return null;
      }

      return bos.toByteArray();
   }

   private static class ASCIIUnicodeStringPair {
      private final String ascii;
      private final String unicode;

      ASCIIUnicodeStringPair(String ascii, String unicode) {
         this.ascii = ascii;
         this.unicode = unicode;
      }

      private String getAscii() {
         return this.ascii;
      }

      private String getUnicode() {
         return this.unicode;
      }
   }

   private enum DIR_STATE {
      INFORMATION_RECORD,
      REFERENCES_RECORD,
      MODULES_RECORD;
   }

   protected static class ModuleImpl implements Module {
      Integer offset;
      byte[] buf;
      ModuleType moduleType;
      Charset charset;

      void read(InputStream in) throws IOException {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         IOUtils.copy(in, out);
         out.close();
         this.buf = out.toByteArray();
      }

      public String getContent() {
         return new String(this.buf, this.charset);
      }

      public ModuleType geModuleType() {
         return this.moduleType;
      }
   }

   protected static class ModuleMap extends HashMap<String, VBAMacroReader.ModuleImpl> {
      Charset charset = StringUtil.WIN_1252;
   }

   private enum RecordType {
      MODULE_OFFSET(49),
      PROJECT_SYS_KIND(1),
      PROJECT_LCID(2),
      PROJECT_LCID_INVOKE(20),
      PROJECT_CODEPAGE(3),
      PROJECT_NAME(4),
      PROJECT_DOC_STRING(5),
      PROJECT_HELP_FILE_PATH(6),
      PROJECT_HELP_CONTEXT(7, 8),
      PROJECT_LIB_FLAGS(8),
      PROJECT_VERSION(9, 10),
      PROJECT_CONSTANTS(12),
      PROJECT_MODULES(15),
      DIR_STREAM_TERMINATOR(16),
      PROJECT_COOKIE(19),
      MODULE_NAME(25),
      MODULE_NAME_UNICODE(71),
      MODULE_STREAM_NAME(26),
      MODULE_DOC_STRING(28),
      MODULE_HELP_CONTEXT(30),
      MODULE_COOKIE(44),
      MODULE_TYPE_PROCEDURAL(33, 4),
      MODULE_TYPE_OTHER(34, 4),
      MODULE_PRIVATE(40, 4),
      REFERENCE_NAME(22),
      REFERENCE_REGISTERED(13),
      REFERENCE_PROJECT(14),
      REFERENCE_CONTROL_A(47),
      REFERENCE_CONTROL_B(51),
      MODULE_TERMINATOR(43),
      EOF(-1),
      UNKNOWN(-2);

      private final int VARIABLE_LENGTH = -1;
      private final int id;
      private final int constantLength;

      RecordType(int id) {
         this.id = id;
         this.constantLength = -1;
      }

      RecordType(int id, int constantLength) {
         this.id = id;
         this.constantLength = constantLength;
      }

      int getConstantLength() {
         return this.constantLength;
      }

      static VBAMacroReader.RecordType lookup(int id) {
         for (VBAMacroReader.RecordType type : values()) {
            if (type.id == id) {
               return type;
            }
         }

         return UNKNOWN;
      }
   }
}
