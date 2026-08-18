package specman.model.v002;

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
}
