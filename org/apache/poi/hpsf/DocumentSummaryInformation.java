package org.apache.poi.hpsf;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;

public class DocumentSummaryInformation extends PropertySet {
   public static final String DEFAULT_STREAM_NAME = "\u0005DocumentSummaryInformation";
   private static final ClassID DOC_SUMMARY_INFORMATION = new ClassID("{D5CDD502-2E9C-101B-9397-08002B2CF9AE}");
   private static final ClassID USER_DEFINED_PROPERTIES = new ClassID("{D5CDD505-2E9C-101B-9397-08002B2CF9AE}");
   public static final ClassID[] FORMAT_ID = new ClassID[]{DOC_SUMMARY_INFORMATION, USER_DEFINED_PROPERTIES};

   public PropertyIDMap getPropertySetIDMap() {
      return PropertyIDMap.getDocumentSummaryInformationProperties();
   }

   public DocumentSummaryInformation() {
      this.getFirstSection().setFormatID(DOC_SUMMARY_INFORMATION);
   }

   public DocumentSummaryInformation(PropertySet ps) throws UnexpectedPropertySetTypeException {
      super(ps);
      if (!this.isDocumentSummaryInformation()) {
         throw new UnexpectedPropertySetTypeException("Not a " + this.getClass().getName());
      }
   }

   public DocumentSummaryInformation(InputStream stream) throws NoPropertySetStreamException, MarkUnsupportedException, IOException, UnsupportedEncodingException {
      super(stream);
   }

   public String getCategory() {
      return this.getPropertyStringValue(2);
   }

   public void setCategory(String category) {
      this.getFirstSection().setProperty(2, category);
   }

   public void removeCategory() {
      this.remove1stProperty(2L);
   }

   public String getPresentationFormat() {
      return this.getPropertyStringValue(3);
   }

   public void setPresentationFormat(String presentationFormat) {
      this.getFirstSection().setProperty(3, presentationFormat);
   }

   public void removePresentationFormat() {
      this.remove1stProperty(3L);
   }

   public int getByteCount() {
      return this.getPropertyIntValue(4);
   }

   public void setByteCount(int byteCount) {
      this.set1stProperty(4L, byteCount);
   }

   public void removeByteCount() {
      this.remove1stProperty(4L);
   }

   public int getLineCount() {
      return this.getPropertyIntValue(5);
   }

   public void setLineCount(int lineCount) {
      this.set1stProperty(5L, lineCount);
   }

   public void removeLineCount() {
      this.remove1stProperty(5L);
   }

   public int getParCount() {
      return this.getPropertyIntValue(6);
   }

   public void setParCount(int parCount) {
      this.set1stProperty(6L, parCount);
   }

   public void removeParCount() {
      this.remove1stProperty(6L);
   }

   public int getSlideCount() {
      return this.getPropertyIntValue(7);
   }

   public void setSlideCount(int slideCount) {
      this.set1stProperty(7L, slideCount);
   }

   public void removeSlideCount() {
      this.remove1stProperty(7L);
   }

   public int getNoteCount() {
      return this.getPropertyIntValue(8);
   }

   public void setNoteCount(int noteCount) {
      this.set1stProperty(8L, noteCount);
   }

   public void removeNoteCount() {
      this.remove1stProperty(8L);
   }

   public int getHiddenCount() {
      return this.getPropertyIntValue(9);
   }

   public void setHiddenCount(int hiddenCount) {
      this.set1stProperty(9L, hiddenCount);
   }

   public void removeHiddenCount() {
      this.remove1stProperty(9L);
   }

   public int getMMClipCount() {
      return this.getPropertyIntValue(10);
   }

   public void setMMClipCount(int mmClipCount) {
      this.set1stProperty(10L, mmClipCount);
   }

   public void removeMMClipCount() {
      this.remove1stProperty(10L);
   }

   public boolean getScale() {
      return this.getPropertyBooleanValue(11);
   }

   public void setScale(boolean scale) {
      this.set1stProperty(11L, scale);
   }

   public void removeScale() {
      this.remove1stProperty(11L);
   }

   public byte[] getHeadingPair() {
      this.notYetImplemented("Reading byte arrays ");
      return (byte[])this.getProperty(12);
   }

   public void setHeadingPair(byte[] headingPair) {
      this.notYetImplemented("Writing byte arrays ");
   }

   public void removeHeadingPair() {
      this.remove1stProperty(12L);
   }

   public byte[] getDocparts() {
      this.notYetImplemented("Reading byte arrays");
      return (byte[])this.getProperty(13);
   }

   public void setDocparts(byte[] docparts) {
      this.notYetImplemented("Writing byte arrays");
   }

   public void removeDocparts() {
      this.remove1stProperty(13L);
   }

   public String getManager() {
      return this.getPropertyStringValue(14);
   }

   public void setManager(String manager) {
      this.set1stProperty(14L, manager);
   }

   public void removeManager() {
      this.remove1stProperty(14L);
   }

   public String getCompany() {
      return this.getPropertyStringValue(15);
   }

   public void setCompany(String company) {
      this.set1stProperty(15L, company);
   }

   public void removeCompany() {
      this.remove1stProperty(15L);
   }

   public boolean getLinksDirty() {
      return this.getPropertyBooleanValue(16);
   }

   public void setLinksDirty(boolean linksDirty) {
      this.set1stProperty(16L, linksDirty);
   }

   public void removeLinksDirty() {
      this.remove1stProperty(16L);
   }

   public int getCharCountWithSpaces() {
      return this.getPropertyIntValue(17);
   }

   public void setCharCountWithSpaces(int count) {
      this.set1stProperty(17L, count);
   }

   public void removeCharCountWithSpaces() {
      this.remove1stProperty(17L);
   }

   public boolean getHyperlinksChanged() {
      return this.getPropertyBooleanValue(22);
   }

   public void setHyperlinksChanged(boolean changed) {
      this.set1stProperty(22L, changed);
   }

   public void removeHyperlinksChanged() {
      this.remove1stProperty(22L);
   }

   public int getApplicationVersion() {
      return this.getPropertyIntValue(23);
   }

   public void setApplicationVersion(int version) {
      this.set1stProperty(23L, version);
   }

   public void removeApplicationVersion() {
      this.remove1stProperty(23L);
   }

   public byte[] getVBADigitalSignature() {
      Object value = this.getProperty(24);
      return value != null && value instanceof byte[] ? (byte[])value : null;
   }

   public void setVBADigitalSignature(byte[] signature) {
      this.set1stProperty(24L, signature);
   }

   public void removeVBADigitalSignature() {
      this.remove1stProperty(24L);
   }

   public String getContentType() {
      return this.getPropertyStringValue(26);
   }

   public void setContentType(String type) {
      this.set1stProperty(26L, type);
   }

   public void removeContentType() {
      this.remove1stProperty(26L);
   }

   public String getContentStatus() {
      return this.getPropertyStringValue(27);
   }

   public void setContentStatus(String status) {
      this.set1stProperty(27L, status);
   }

   public void removeContentStatus() {
      this.remove1stProperty(27L);
   }

   public String getLanguage() {
      return this.getPropertyStringValue(28);
   }

   public void setLanguage(String language) {
      this.set1stProperty(28L, language);
   }

   public void removeLanguage() {
      this.remove1stProperty(28L);
   }

   public String getDocumentVersion() {
      return this.getPropertyStringValue(29);
   }

   public void setDocumentVersion(String version) {
      this.set1stProperty(29L, version);
   }

   public void removeDocumentVersion() {
      this.remove1stProperty(29L);
   }

   public CustomProperties getCustomProperties() {
      CustomProperties cps = null;
      if (this.getSectionCount() >= 2) {
         cps = new CustomProperties();
         Section section = (Section)this.getSections().get(1);
         Map<Long, String> dictionary = section.getDictionary();
         Property[] properties = section.getProperties();
         int propertyCount = 0;

         for (Property p : properties) {
            long id = p.getID();
            if (id == 1L) {
               cps.setCodepage((Integer)p.getValue());
            } else if (id > 1L) {
               propertyCount++;
               CustomProperty cp = new CustomProperty(p, (String)dictionary.get(id));
               cps.put(cp.getName(), cp);
            }
         }

         if (cps.size() != propertyCount) {
            cps.setPure(false);
         }
      }

      return cps;
   }

   public void setCustomProperties(CustomProperties customProperties) {
      this.ensureSection2();
      Section section = (Section)this.getSections().get(1);
      Map<Long, String> dictionary = customProperties.getDictionary();
      int cpCodepage = customProperties.getCodepage();
      if (cpCodepage < 0) {
         cpCodepage = section.getCodepage();
      }

      if (cpCodepage < 0) {
         cpCodepage = 1252;
      }

      customProperties.setCodepage(cpCodepage);
      section.setCodepage(cpCodepage);
      section.setDictionary(dictionary);

      for (CustomProperty p : customProperties.properties()) {
         section.setProperty(p);
      }
   }

   private void ensureSection2() {
      if (this.getSectionCount() < 2) {
         Section s2 = new Section();
         s2.setFormatID(USER_DEFINED_PROPERTIES);
         this.addSection(s2);
      }
   }

   public void removeCustomProperties() {
      if (this.getSectionCount() < 2) {
         throw new HPSFRuntimeException("Illegal internal format of Document SummaryInformation stream: second section is missing.");
      }

      List<Section> l = new LinkedList(this.getSections());
      this.clearSections();
      int idx = 0;

      for (Section s : l) {
         if (idx++ != 1) {
            this.addSection(s);
         }
      }
   }

   private void notYetImplemented(String msg) {
      throw new UnsupportedOperationException(msg + " is not yet implemented.");
   }
}
