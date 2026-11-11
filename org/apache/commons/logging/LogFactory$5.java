package org.apache.commons.logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivilegedAction;
import java.util.Properties;

final class LogFactory$5 implements PrivilegedAction {
   LogFactory$5(URL var1) {
      this.val$url = var1;
   }

   public Object run() {
      InputStream stream = null;

      try {
         URLConnection connection = this.val$url.openConnection();
         connection.setUseCaches(false);
         stream = connection.getInputStream();
         if (stream != null) {
            Properties props = new Properties();
            props.load(stream);
            stream.close();
            stream = null;
            return props;
         }
      } catch (IOException e) {
         if (LogFactory.isDiagnosticsEnabled()) {
            LogFactory.access$000("Unable to read URL " + this.val$url);
         }
      } finally {
         if (stream != null) {
            try {
               stream.close();
            } catch (IOException e) {
               if (LogFactory.isDiagnosticsEnabled()) {
                  LogFactory.access$000("Unable to close stream for URL " + this.val$url);
               }
            }
         }
      }

      return null;
   }
}
