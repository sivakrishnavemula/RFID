package org.apache.poi.poifs.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.poifs.dev.POIFSViewable;
import org.apache.poi.poifs.property.DirectoryProperty;
import org.apache.poi.poifs.property.DocumentProperty;
import org.apache.poi.poifs.property.Property;

public class DirectoryNode extends EntryNode implements DirectoryEntry, POIFSViewable, Iterable<Entry> {
   private final Map<String, Entry> _byname = new HashMap();
   private final ArrayList<Entry> _entries = new ArrayList();
   private final POIFSFileSystem _nfilesystem;
   private final POIFSDocumentPath _path;

   DirectoryNode(DirectoryProperty property, POIFSFileSystem nfilesystem, DirectoryNode parent) {
      super(property, parent);
      this._nfilesystem = nfilesystem;
      if (parent == null) {
         this._path = new POIFSDocumentPath();
      } else {
         this._path = new POIFSDocumentPath(parent._path, new String[]{property.getName()});
      }

      Iterator<Property> iter = property.getChildren();

      while (iter.hasNext()) {
         Property child = (Property)iter.next();
         Entry childNode;
         if (child.isDirectory()) {
            DirectoryProperty childDir = (DirectoryProperty)child;
            childNode = new DirectoryNode(childDir, this._nfilesystem, this);
         } else {
            childNode = new DocumentNode((DocumentProperty)child, this);
         }

         this._entries.add(childNode);
         this._byname.put(childNode.getName(), childNode);
      }
   }

   public POIFSDocumentPath getPath() {
      return this._path;
   }

   public POIFSFileSystem getFileSystem() {
      return this._nfilesystem;
   }

   public POIFSFileSystem getNFileSystem() {
      return this._nfilesystem;
   }

   public DocumentInputStream createDocumentInputStream(String documentName) throws IOException {
      return this.createDocumentInputStream(this.getEntry(documentName));
   }

   public DocumentInputStream createDocumentInputStream(Entry document) throws IOException {
      if (!document.isDocumentEntry()) {
         throw new IOException("Entry '" + document.getName() + "' is not a DocumentEntry");
      }

      DocumentEntry entry = (DocumentEntry)document;
      return new DocumentInputStream(entry);
   }

   DocumentEntry createDocument(POIFSDocument document) throws IOException {
      DocumentProperty property = document.getDocumentProperty();
      DocumentNode rval = new DocumentNode(property, this);
      ((DirectoryProperty)this.getProperty()).addChild(property);
      this._nfilesystem.addDocument(document);
      this._entries.add(rval);
      this._byname.put(property.getName(), rval);
      return rval;
   }

   boolean changeName(String oldName, String newName) {
      boolean rval = false;
      EntryNode child = (EntryNode)this._byname.get(oldName);
      if (child != null) {
         rval = ((DirectoryProperty)this.getProperty()).changeName(child.getProperty(), newName);
         if (rval) {
            this._byname.remove(oldName);
            this._byname.put(child.getProperty().getName(), child);
         }
      }

      return rval;
   }

   boolean deleteEntry(EntryNode entry) {
      boolean rval = ((DirectoryProperty)this.getProperty()).deleteChild(entry.getProperty());
      if (rval) {
         this._entries.remove(entry);
         this._byname.remove(entry.getName());

         try {
            this._nfilesystem.remove(entry);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      return rval;
   }

   public Iterator<Entry> getEntries() {
      return this._entries.iterator();
   }

   public Set<String> getEntryNames() {
      return this._byname.keySet();
   }

   public boolean isEmpty() {
      return this._entries.isEmpty();
   }

   public int getEntryCount() {
      return this._entries.size();
   }

   public boolean hasEntry(String name) {
      return name != null && this._byname.containsKey(name);
   }

   public Entry getEntry(String name) throws FileNotFoundException {
      Entry rval = null;
      if (name != null) {
         rval = (Entry)this._byname.get(name);
      }

      if (rval == null) {
         throw new FileNotFoundException("no such entry: \"" + name + "\", had: " + this._byname.keySet());
      } else {
         return rval;
      }
   }

   public DocumentEntry createDocument(String name, InputStream stream) throws IOException {
      return this.createDocument(new POIFSDocument(name, this._nfilesystem, stream));
   }

   public DocumentEntry createDocument(String name, int size, POIFSWriterListener writer) throws IOException {
      return this.createDocument(new POIFSDocument(name, size, this._nfilesystem, writer));
   }

   public DirectoryEntry createDirectory(String name) throws IOException {
      DirectoryProperty property = new DirectoryProperty(name);
      DirectoryNode rval = new DirectoryNode(property, this._nfilesystem, this);
      this._nfilesystem.addDirectory(property);
      ((DirectoryProperty)this.getProperty()).addChild(property);
      this._entries.add(rval);
      this._byname.put(name, rval);
      return rval;
   }

   public DocumentEntry createOrUpdateDocument(String name, InputStream stream) throws IOException {
      if (!this.hasEntry(name)) {
         return this.createDocument(name, stream);
      }

      DocumentNode existing = (DocumentNode)this.getEntry(name);
      POIFSDocument nDoc = new POIFSDocument(existing);
      nDoc.replaceContents(stream);
      return existing;
   }

   public ClassID getStorageClsid() {
      return this.getProperty().getStorageClsid();
   }

   public void setStorageClsid(ClassID clsidStorage) {
      this.getProperty().setStorageClsid(clsidStorage);
   }

   public boolean isDirectoryEntry() {
      return true;
   }

   protected boolean isDeleteOK() {
      return this.isEmpty();
   }

   public Object[] getViewableArray() {
      return new Object[0];
   }

   public Iterator<Object> getViewableIterator() {
      List<Object> components = new ArrayList();
      components.add(this.getProperty());
      components.addAll(this._entries);
      return components.iterator();
   }

   public boolean preferArray() {
      return false;
   }

   public String getShortDescription() {
      return this.getName();
   }

   public Iterator<Entry> iterator() {
      return this.getEntries();
   }
}
