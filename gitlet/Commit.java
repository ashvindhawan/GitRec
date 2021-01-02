package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/** Commit class that saves the information of a
 * single commit and its relation to other commits.
 * @author Ashvin Dhawan
 * */
public class Commit implements Serializable {

    /** list of filename to blob mappings of a Commit.*/
    private HashMap<String, Blob> _blobList;

    /** sha1 of parent commit. */
    private ArrayList<String> _parent;

    /** commit message. */
    private String _message;

    /** Datetime that commit was made. */
    private Date _date;

    /** branch associated with this commit.*/
    private String _branch;

    /** Constructor for Commit class, with MESSAGE,
     * PARENT, BLOBLIST, DATE, BRANCH all being assigned. */
    public Commit(String message, ArrayList<String> parent,
                  HashMap<String, Blob> blobList, Date date, String branch) {
        _message = message;
        _parent = parent;
        _blobList = blobList;
        _date = date;
        _branch = branch;
    }

    /** Accessor method, returns Commit's list of BLOBS. */
    public HashMap<String, Blob> bL() {
        return _blobList;
    }

    /** Accessor method, returns raw _DATE object. */
    public Date date() {
        return _date;
    }

    /** Returns DATE TIME of Commit. */
    public String toDate() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        String a = sdf.format(date());
        return a;
    }

    /** Accessor method, returns string _PARENT. */
    public String firstParent() {
        if (_parent != null && _parent.size() > 0) {
            return _parent.get(0);
        } else {
            return null;
        }
    }

    /** Accessor method which returns string of the second parent. */
    public String secondParent() {
        if (_parent != null && _parent.size() > 1) {
            return _parent.get(1);
        } else {
            return null;
        }
    }

    /** Accessor method, returns ArrayList parent object as a whole. */
    public ArrayList<String> parent() {
        return _parent;
    }

    /** returns sha1 String of commit. */
    public String sha() {
        return Utils.sha1(Utils.serialize(this));
    }

    /** Accessor method, returns _MESSAGE. */
    public String message() {
        return _message;
    }
}
