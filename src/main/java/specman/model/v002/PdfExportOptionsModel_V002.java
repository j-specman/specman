package specman.model.v002;

import specman.model.v001.PDFExportOptionsModel_V001;

public class PdfExportOptionsModel_V002 {
    public final String filename;
    public final String modelFilename;
    public final String pageSize;
    public final boolean portrait;
    public final boolean paging;

    @Deprecated public PdfExportOptionsModel_V002() { // For Jackson only
        filename = modelFilename = pageSize = null;
        portrait = paging = false;
    }

    public PdfExportOptionsModel_V002(String filename, String modelFilename, String pageSize, boolean portrait, boolean paging) {
        this.filename = filename;
        this.modelFilename = modelFilename;
        this.pageSize = pageSize;
        this.portrait = portrait;
        this.paging = paging;
    }

    public static PdfExportOptionsModel_V002 from(PDFExportOptionsModel_V001 v1) {
        if (v1 == null) return null;
        return new PdfExportOptionsModel_V002(v1.filename, v1.modelFilename, v1.pageSize, v1.portrait, v1.paging);
    }
}
