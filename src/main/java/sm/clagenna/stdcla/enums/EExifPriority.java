package sm.clagenna.stdcla.enums;

public enum EExifPriority {

  ExifFileDir("Exif File Dir"), //
  FileDirExif("File Dir Exif"), //
  DirFileExif("Dir File Exif");

  private String desc;

  private EExifPriority(String sz) {
    desc = sz;
  }

  public String desc() {
    return desc;
  }

}
