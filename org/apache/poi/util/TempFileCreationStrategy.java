package org.apache.poi.util;

import java.io.File;
import java.io.IOException;

public interface TempFileCreationStrategy {
   File createTempFile(String var1, String var2) throws IOException;

   File createTempDirectory(String var1) throws IOException;
}
