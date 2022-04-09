package gitlet;




//import java.io.File;
import java.io.Serializable;
//import java.util.Date; //You'll likely use this in this class
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//import static gitlet.Utils.sha1;

/** Represents a gitlet commit object.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Xiaowen Liu
 */
public class Commit implements Serializable {
    /**
     * add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit.
     * The timestamp
     */
    private String message;
    private String timestamp;
    private String parent;
    private String parent2;
    private HashMap<String, String> blobMap;


    /** Makes a commit */
    // The parent is a hash_id
    public Commit(String message, String parent, String date, HashMap<String, String> maps) {
        this.message = message;
        this.parent = parent;
        this.timestamp = date; // verify it is the epoc date
        this.blobMap = maps;
        this.parent2 = null;
    }


    public Commit(String message, String parent, String parent2, String date,
                  HashMap<String, String> maps) {
        this.message = message;
        this.parent = parent;
        this.timestamp = date; // very it is the epoc date
        this.blobMap = maps;
        this.parent2 = parent2;
    }

    public  Set<String> bothParents() {
        Set<String> parents = new HashSet<String>();
        if(this.getParent() != null) {
            parents.add(this.getParent());
        }
        if(this.getParent2() != null) {
            parents.add(this.getParent2());
        }

        return parents;
    }

    public static Boolean isInitial(Commit head) {
        return head.bothParents() == null;
    }



    public String getMessage() {
        return this.message;
    }
    public String getTimestamp() {
        return this.timestamp;
    }

    public void changeMessage(String mess) {
        this.message = mess;
    }
    public void changeTime(String time) {
        this.timestamp = time;
    }

    public HashMap<String, String> getfile() {
        return this.blobMap;
    }
    public String getParent() {
        return this.parent;
    }
    public String getParent2() {
        return this.parent2;
    }

    public static String getBlob(Commit obj, String fileName) {
        return obj.blobMap.get(fileName);
    }



}
