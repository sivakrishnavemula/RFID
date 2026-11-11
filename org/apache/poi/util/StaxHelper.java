package org.apache.poi.util;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

public final class StaxHelper {
   private static final POILogger logger = POILogFactory.getLogger(StaxHelper.class);

   private StaxHelper() {
   }

   public static XMLInputFactory newXMLInputFactory() {
      XMLInputFactory factory = XMLInputFactory.newFactory();
      trySetProperty(factory, "javax.xml.stream.isNamespaceAware", true);
      trySetProperty(factory, "javax.xml.stream.isValidating", false);
      trySetProperty(factory, "javax.xml.stream.supportDTD", false);
      trySetProperty(factory, "javax.xml.stream.isSupportingExternalEntities", false);
      return factory;
   }

   public static XMLOutputFactory newXMLOutputFactory() {
      XMLOutputFactory factory = XMLOutputFactory.newFactory();
      trySetProperty(factory, "javax.xml.stream.isRepairingNamespaces", true);
      return factory;
   }

   public static XMLEventFactory newXMLEventFactory() {
      return XMLEventFactory.newFactory();
   }

   private static void trySetProperty(XMLInputFactory factory, String feature, boolean flag) {
      try {
         factory.setProperty(feature, flag);
      } catch (Exception e) {
         logger.log(5, new Object[]{"StAX Property unsupported", feature, e});
      } catch (AbstractMethodError ame) {
         logger.log(5, new Object[]{"Cannot set StAX property because outdated StAX parser in classpath", feature, ame});
      }
   }

   private static void trySetProperty(XMLOutputFactory factory, String feature, boolean flag) {
      try {
         factory.setProperty(feature, flag);
      } catch (Exception e) {
         logger.log(5, new Object[]{"StAX Property unsupported", feature, e});
      } catch (AbstractMethodError ame) {
         logger.log(5, new Object[]{"Cannot set StAX property because outdated StAX parser in classpath", feature, ame});
      }
   }
}
