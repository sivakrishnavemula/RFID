package org.apache.poi.poifs.eventfilesystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.poi.poifs.filesystem.DocumentDescriptor;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;

class POIFSReaderRegistry {
   private Set<POIFSReaderListener> omnivorousListeners = new HashSet();
   private Map<POIFSReaderListener, Set<DocumentDescriptor>> selectiveListeners = new HashMap();
   private Map<DocumentDescriptor, Set<POIFSReaderListener>> chosenDocumentDescriptors = new HashMap();

   void registerListener(POIFSReaderListener listener, POIFSDocumentPath path, String documentName) {
      if (!this.omnivorousListeners.contains(listener)) {
         Set<DocumentDescriptor> descriptors = (Set<DocumentDescriptor>)this.selectiveListeners.computeIfAbsent(listener, k -> new HashSet());
         DocumentDescriptor descriptor = new DocumentDescriptor(path, documentName);
         if (descriptors.add(descriptor)) {
            Set<POIFSReaderListener> listeners = (Set<POIFSReaderListener>)this.chosenDocumentDescriptors.computeIfAbsent(descriptor, k -> new HashSet());
            listeners.add(listener);
         }
      }
   }

   void registerListener(POIFSReaderListener listener) {
      if (!this.omnivorousListeners.contains(listener)) {
         this.removeSelectiveListener(listener);
         this.omnivorousListeners.add(listener);
      }
   }

   Iterable<POIFSReaderListener> getListeners(POIFSDocumentPath path, String name) {
      Set<POIFSReaderListener> rval = new HashSet(this.omnivorousListeners);
      Set<POIFSReaderListener> selectiveListenersInner = (Set<POIFSReaderListener>)this.chosenDocumentDescriptors.get(new DocumentDescriptor(path, name));
      if (selectiveListenersInner != null) {
         rval.addAll(selectiveListenersInner);
      }

      return rval;
   }

   private void removeSelectiveListener(POIFSReaderListener listener) {
      Set<DocumentDescriptor> selectedDescriptors = (Set<DocumentDescriptor>)this.selectiveListeners.remove(listener);
      if (selectedDescriptors != null) {
         for (DocumentDescriptor selectedDescriptor : selectedDescriptors) {
            this.dropDocument(listener, selectedDescriptor);
         }
      }
   }

   private void dropDocument(POIFSReaderListener listener, DocumentDescriptor descriptor) {
      Set<POIFSReaderListener> listeners = (Set<POIFSReaderListener>)this.chosenDocumentDescriptors.get(descriptor);
      listeners.remove(listener);
      if (listeners.size() == 0) {
         this.chosenDocumentDescriptors.remove(descriptor);
      }
   }
}
