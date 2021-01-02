package gitlet;

import java.io.File;
import java.io.Serializable;

/** BLob class that stores a blobs _NAME and serialized _CONTENTS.
 * @author Ashvin Dhawan */
public class Blob implements Serializable {

    /** FileName of Blob object. */
    private String _name;
    /** Serialized contents of blob object. */
    private byte[] _contents;

    /** String version of file contents. */
    private String _contentsAsString;

    /** constructor for blob class, takes in NAME for fileName. */
    public Blob(String name) {
        _name = name;
        String blobPath = System.getProperty("user.dir") + "/" + name;
        File test = new File(blobPath);
        _contents = Utils.readContents(test);
        _contentsAsString = Utils.readContentsAsString(test);
    }

    /** Accessor method, returns NAME. */
    public String name() {
        return _name;
    }

    /** Accessor method, returns CONTENTS. */
    public byte[] conts() {
        return _contents;
    }

    /** returns contents of the blob as a string. */
    public String caS() {
        return _contentsAsString;
    }
}
