package org.apache.poi.sl.usermodel;

import org.apache.poi.util.Internal;

@Internal
public interface Line<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends AutoShape<S, P> {
}
