package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;



/** Represents a gitlet repository.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Xiaowen Liu
 */
public class Repository extends PriorityQueue implements Serializable {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File STAGE_ADD = join(GITLET_DIR, "StagedAdd");
    public static final File STAGE_REMOVE = join(GITLET_DIR, "StagedRemove");
    public static final File BLOBS_DIR = join(GITLET_DIR, "Blobs");
    public static final File COMMITS_DIR = join(GITLET_DIR, "Commits");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "Branches");

    public static final File CURR_BRANCH = join(GITLET_DIR, "Branch");
    public static final File HEAD_COMMIT = join(GITLET_DIR, "HEAD COMMIT");
    // public static final File master = join(BRANCHES_DIR, "master");
    //public static HashMap<String, String> stage;
    //public static HashMap<String, String> remove;


    private static void setupPersistence() {
        HashMap<String, String> stage = new HashMap<String, String>();
        StagingArea.saveAdd(stage);
        StagingArea.saveRemove(stage);
    }
    private static void checkInit() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /**
     * make the initial commit
     * create the gitlet.dict
     * create all the rest of things in the .gitlet that we need
     * if .gitlet has already exists, then print out the error message
     */
    public static void init() {
        String err = "A Gitlet version-control system already exists in the current directory.";
        if (GITLET_DIR.exists()) {
            System.out.println(err);
        } else {
            /**Create a gitlet dictionary*/
            GITLET_DIR.mkdir();
            /**Create an blob*/
            BLOBS_DIR.mkdir();
            /**Create a commit_dict*/
            COMMITS_DIR.mkdir();
            BRANCHES_DIR.mkdir();
            writeContents(CURR_BRANCH, "master");
            setupPersistence();


            /**this gitlet dictionary contains:1)staging area   2)commits
             * when init,
             * commits contains commit 0
             * commit 0: 1)contains metadata 2)with null data / not tracking any files
            */
            String message = "initial commit";
            String myDate = createDate(new Date(0));
            HashMap<String, String> initial = new HashMap<String, String>();
            Commit initialCommit = new Commit(message, null, myDate, initial);
            String id = sha1(serialize(initialCommit));
            File initialCommitFile = join(COMMITS_DIR, id);
            writeObject(initialCommitFile, initialCommit);
            writeContents(HEAD_COMMIT, id);
            writeContents(currentBranch(), id);


        }
    }
    private static File currentBranch() {
        String branchName = readContentsAsString(CURR_BRANCH);
        return join(BRANCHES_DIR, branchName);
    }


    //After creating a commit object后，save the object as a file
    // save the file into COMMITS_DIR
    // and return a file path
    //Currently not used
    private File fileConverter(Commit obj, String name) {
        File commitFile = join(COMMITS_DIR, name);
        writeObject(commitFile, obj);
        return commitFile;
    }

    private static Commit headCommit() {
        String id = readContentsAsString(HEAD_COMMIT);
        File add = join(COMMITS_DIR, id);
        return readObject(add, Commit.class);
    }


    public static void add(String fileName) {
        /** 1.read the blob of the file --> refer as blob 0
         *  2.staged for addition
         *
         *  比如add Hello.txt的时候，因为Hello.txt的内容和the version we've currently tracked is the same
         *  所以不需要add again
         * */
        checkInit();
        //find the address of the added file in the cwd
        File fileAdd = join(CWD, fileName);
        if (!fileAdd.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        //read the file and collect the content
        String infile = readContentsAsString(fileAdd);

        //Create a blob with the desired content
        //First step: find the blobId of the content
        String blobId = sha1(infile);

        //if the content/sha1 of added file is the same as the version
        //As commit couldn't be sha1-code() directly,
        // we should first convert object into string，then sha1-code
        String id = readContentsAsString(HEAD_COMMIT);
        File add = join(COMMITS_DIR, id);
        Commit head = readObject(add, Commit.class);
        if (head.getfile() != null && blobId.equals(Commit.getBlob(head, fileName))) {
            //1.不要stage for addition
            //2.假如在staging area里面有这个file的话，就remove走；
            //本来这里要有个remove这个command，但现在先不加
            HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
            stage.remove(fileName);
            StagingArea.saveAdd(stage);
            HashMap<String, String> remove = readObject(STAGE_REMOVE, HashMap.class);
            remove.remove(fileName);
            StagingArea.saveRemove(remove);
            return;
        } else {
            //Save the content into a file named as blobId
            //每个Blob file名字是content string的sha1 id; 比如：exhshi264sbv1pw
            //每个Blob file内容是content string;比如 "This is me."
            File blob = join(BLOBS_DIR, blobId);
            Utils.writeContents(blob, infile);

            //Stage_add
            //每个stage file的名字是file name；比如："Hello World"
            //每个stage file的内容是blob的sha1 id;比如：exhshi264sbv1pw
            /**delete
             * File add = join(STAGE_ADD, fileName);
            Utils.writeContents(add, blobId); */
            HashMap<String, String> stage = new HashMap<String, String>();
            if (STAGE_ADD.exists()) {
                stage = readObject(STAGE_ADD, HashMap.class);
            }
            stage.put(fileName, blobId);
            //Store the map as a file
            StagingArea.saveAdd(stage);
        }
    }
    //blob is just the contents of the file
    /**private static String createBlob(File fileTarget) {
    } */

    public static void commit(String message, String givenID) {
        //when no files have been staged, abort.
        // 打印the message "No changes added to the commit"
        //如果commit message是blank的话，
        // 打印error message "Please enter a commit message"
        /** 1.clone the commit0 as commit1
         * 2.change the metadata according to 1)user message 2)time the command is called
         * 3.according to staged area, commit the corresponding blob (eg.blob 0)
         *4.move the head & master
         */
        //error message
        checkInit();
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        //check if the stage is empty
        if (!STAGE_ADD.exists() || !STAGE_REMOVE.exists()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
        HashMap<String, String> remove = readObject(STAGE_REMOVE, HashMap.class);
        if (stage.size() == 0 && remove.size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        //clone the commit
        //Find the id of current commit
        String parID = readContentsAsString(HEAD_COMMIT);
        //Clone the current commit as new commit
        Commit headCommit = headCommit();

        //change the date of new commit
        /**
        newCommit.message = message;
        newCommit.timestamp = createDate(currentDate());
        newCommit.parent = parentID;
         */
        //如何iterate through staging add的hashmap，
        // 然后加到新的hashmap里面
        HashMap mp = readObject(STAGE_ADD, HashMap.class);
        Set<String> addKeys = mp.keySet();
        HashMap mp2 = readObject(STAGE_REMOVE, HashMap.class);
        Set<String> removeKeys = mp2.keySet();
        HashMap<String, String> newMap;
        if (headCommit.getfile() == null) {
            newMap = new HashMap<String, String>();
        } else {
            newMap = (HashMap<String, String>) headCommit.getfile().clone();
        }
        for (String i:addKeys) {
            newMap.put(i, stage.get(i));
        }
        for (String i: removeKeys) {
            newMap.remove(i);
        }
        Commit newCommit = new Commit(message, parID, givenID, createDate(currentDate()), newMap);

        //Store the new commit in the dir
        String shaID = sha1((Object) serialize(newCommit));
        File commitFile = join(COMMITS_DIR, shaID);
        writeObject(commitFile, newCommit);
        //assign head commit as new commit
        writeContents(HEAD_COMMIT, shaID);
        writeContents(currentBranch(), shaID);
        //clean the staging area
        //mp.clear();
        //tagingArea.saveAdd(mp);
        StagingArea.clearAdd();
        StagingArea.clearRemove();
    }
    private static Date currentDate() {
        return new Date();
    }
    private static String createDate(Date mydate) {
        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat myFormat = new SimpleDateFormat(pattern);
        return myFormat.format(mydate);
    }

    public static void rm(String fileName) {
        /**
         * Unstage the file if it is currently staged for addition.
         * If the file is tracked in the current commit,
         * stage it for removal
         * remove the file from the working directory if the user has not already done so
         * (do not remove it unless it is tracked in the current commit).*/

        /** If the file is neither staged
         * nor tracked by the head commit
         * print the error message*/
        checkInit();
        Commit head = headCommit();
        //if add has never been called or no commits have been made
        if (!STAGE_ADD.exists() || head.getfile() == null) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
        if (stage.get(fileName) == null && head.getfile().get(fileName) == null) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (stage.get(fileName) != null) {
            stage.remove(fileName);
            StagingArea.saveAdd(stage);
        }
        if (head.getfile().get(fileName) != null) {
            //The sha1-ID stored by the file in the commit
            String sha1ID = head.getfile().get(fileName);
            //Save the sha1-ID & file name in the remove hashmap
            HashMap<String, String> remove;
            if (!STAGE_REMOVE.exists()) {
                remove = new HashMap<String, String>();
            } else if (readObject(STAGE_REMOVE, HashMap.class) == null) {
                remove = new HashMap<String, String>();
            } else {
                remove = readObject(STAGE_REMOVE, HashMap.class);
            }
            remove.put(fileName, sha1ID);
            StagingArea.saveRemove(remove);
            //delete the file from the path
            File path = join(CWD, fileName);
            path.delete();
        }
    }

    private static void checkCommit() {
        Commit curr = headCommit();
        while (curr.getParent() != null) {
            File path = join(COMMITS_DIR, curr.getParent());
            curr = readObject(path, Commit.class);
        }
    }

    private static void printLog(Commit curr, String id) {
        System.out.println("===");
        System.out.println("commit " + id);
        System.out.println("Date: " + curr.getTimestamp());
        System.out.println(curr.getMessage());
        System.out.println();
    }
    public static void log() {
        checkInit();
        String id = readContentsAsString(HEAD_COMMIT);
        Commit curr = headCommit();
        while (curr != null) {
            printLog(curr, id);
            id = curr.getParent();
            if (id != null) {
                File currFile = join(COMMITS_DIR, id);
                curr = readObject(currFile, Commit.class);
            } else {
                break;
            }
        }

    }
    public static void globalLog() {
        checkInit();
        List<String> files = plainFilenamesIn(COMMITS_DIR);
        if (files != null) {
            for (String i: files) {
                File path = join(COMMITS_DIR, i);
                Commit curr = readObject(path, Commit.class);
                printLog(curr, i);
            }
        }

    }
    private static void branchExist(File add) {
        if (!add.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
    }

    private static void changeCWD(Commit head, String fileName) {
        //find the version/blob of the file
        if (head.getfile().get(fileName) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobID = head.getfile().get(fileName);
        //find the content of the version
        File blob = join(BLOBS_DIR, blobID);
        String content = readContentsAsString(blob);
        //find the file in the CWD
        File path = join(CWD, fileName);
        //Overwrite
        writeContents(path, content);

    }
    private static String findID(String id) {
        List<String> files = plainFilenamesIn(COMMITS_DIR);
        String commitID = null;
        if (files != null) {
            for (String i: files) {
                if (i.startsWith(id)) {
                    commitID = i;
                    break;
                }
            }
        }
        return commitID;
    }
    public static void checkout(String id, String operand, String fileName) {
        /**
         * the audience will give you a commit id and a file name
         * and change <file name> to version with commit id
         */

        // Takes the version of the file as it exists in the commit with the given id
        // Puts it in the working directory
        // Overwriting the version of the file that's already there
        // The new version of the file is not staged
        checkInit();
        if (!operand.equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String commitID = findID(id);
        if (commitID == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        File add = join(COMMITS_DIR, commitID);
        Commit head = readObject(add, Commit.class);
        changeCWD(head, fileName);
    }
    public static void checkout(String operand, String fileName) {
        //Takes the version of the file as it exists in the head commit
        // puts it in the working directory
        // overwriting the version of the file that’s already in the CWD if there is one.
        // The new version of the file is not staged.

        //read head commit
        checkInit();
        if (!operand.equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Commit head = headCommit();
        changeCWD(head, fileName);
    }
    public static void checkout(String branchName) {
        checkInit();
        //find the given branch
        File add = join(BRANCHES_DIR, branchName);
        branchExist(add);
        if (currentBranch().equals(add)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        //find the commit id
        String commitID = readContentsAsString(add);
        //Find the path to the commit
        File commit = join(COMMITS_DIR, commitID);
        //Find the head commit in the branch
        Commit givenHead = readObject(commit, Commit.class);
        //check if there's any file is not tracked in the current branch
        File[] cwdFiles = CWD.listFiles();
        assert cwdFiles != null;
        //to check 'tracked', find the staging area & commit
        HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
        HashMap givenMap = givenHead.getfile();
        //current branch's commit
        Commit currHead = headCommit();
        //check each file in the cwd except gitlet
        checkUntrack(currHead, givenMap);
        //put the files in the CWD
        Set<String> keys = givenMap.keySet();
        for (String i: keys) {
            String blobID = (String) givenMap.get(i);
            File blob = join(BLOBS_DIR, blobID);
            String content = readContentsAsString(blob);
            File path = join(CWD, i);
            writeContents(path, content);
        }
        //for files in current branch,check if it exists in the given branch
        for (String i: currHead.getfile().keySet()) {
            if (givenMap.get(i) == null) {
                File path =  join(CWD, i);
                path.delete();
            }
        }
        writeContents(CURR_BRANCH, branchName);
        writeContents(HEAD_COMMIT, commitID);
        StagingArea.clearStage();
    }
    private static void checkUntrack(Commit currHead, HashMap<String, String> givenMap) {
        File[] cwdFiles = CWD.listFiles();
        assert cwdFiles != null;
        for (File i: cwdFiles) {
            String fileName = i.getName();
            if (fileName.equals(".gitlet")) {
                continue;
            }
            HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
            //if it is not tracked by the current commit / added to stage
            if (stage.get(fileName) == null && currHead.getfile().get(fileName) == null) {
                //will be overwritten by the new branch
                if (givenMap.get(fileName) != null) {
                    String message = "There is an untracked file in the way; ";
                    message += "delete it, or add and commit it first.";
                    System.out.println(message);
                    System.exit(0);
                }
            }
        }
    }
    public static void find(String message) {
        checkInit();
        List<String> files = plainFilenamesIn(COMMITS_DIR);
        Boolean have = false;
        if (files != null) {
            for (String i: files) {
                File path = join(COMMITS_DIR, i);
                Commit curr = readObject(path, Commit.class);
                if (curr.getMessage().equals(message)) {
                    have = true;
                    System.out.println(i);
                }
            }
        }
        if (!have) {
            System.out.println("Found no commit with that message.");
        }
    }
    public static void branch(String branchName) {
        checkInit();
        File add = join(BRANCHES_DIR, branchName);
        if (add.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            writeContents(add, readContentsAsString(HEAD_COMMIT));
        }
    }

    public static void rmBranch(String branchName) {
        checkInit();
        File add = join(BRANCHES_DIR, branchName);
        if (!add.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (currentBranch().equals(add)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        add.delete();
    }
    private static List<String> setList(Set<String> mySet) {
        List<String> sortedList = new ArrayList<>(mySet);
        Collections.sort(sortedList);
        return sortedList;
    }
    public static void status() {
        checkInit();
        System.out.println("=== Branches ===");
        String branchName = readContentsAsString(CURR_BRANCH);
        File[] branches = BRANCHES_DIR.listFiles();
        String[] names = new String[branches.length];
        //Order the branches
        for (int i = 0; i < branches.length; i += 1) {
            names[i] = branches[i].getName();
        }
        Arrays.sort(names);
        for (String i: names) {
            if (i.equals(branchName)) {
                System.out.println("*" + i);
            } else {
                System.out.println(i);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        HashMap<String, String> add = readObject(STAGE_ADD, HashMap.class);
        for (String i: add.keySet()) {
            System.out.println(i);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        HashMap<String, String> toRemove = readObject(STAGE_REMOVE, HashMap.class);
        for (String i: toRemove.keySet()) {
            System.out.println(i);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        Set<String> unTrack = new HashSet<>();
        Set<String> deleted = new HashSet<>();
        HashMap<String, String> modifications = new HashMap<String, String>();

        File[] cwdFiles = CWD.listFiles();
        Set<String> cwdNames = new HashSet<>();
        Set<String> commitKeys = headCommit().getfile().keySet();
        Set<String> addKeys = add.keySet();
        for (File i: cwdFiles) {
            String name = i.getName();
            if (name.equals(".gitlet")) {
                break;
            }
            cwdNames.add(name);
            String sha1ID = sha1(readContentsAsString(i));
            if (add.get(name) == null && headCommit().getfile().get(name) == null) {
                unTrack.add(name);
            } else if (add.get(name) == null && headCommit().getfile().get(name) != null) {
                if (!sha1ID.equals(headCommit().getfile().get(name))) {
                    modifications.put(name, "modified");
                }
                //Staged for addition, but with different contents than in the working directory; or
            } else if (add.get(name) != null) {
                if (!add.get(name).equals(sha1ID)) {
                    modifications.put(name, "modified");
                }
            }
        }
        addKeys.removeAll(cwdNames);
        deleted.addAll(addKeys);
        commitKeys.removeAll(toRemove.keySet());
        commitKeys.removeAll(cwdNames);
        deleted.addAll(commitKeys);
        for (String i: deleted) {
            modifications.put(i, "deleted");
        }
        List<String> result = setList(modifications.keySet());
        for (String i: result) {
            System.out.println(i + " (" + modifications.get(i) + ")");
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        List<String> unTracked = setList(unTrack);
        for (String i: unTracked) {
            System.out.println(i);
        }
        System.out.println();
    }
    public static void reset(String id) {
        checkInit();
        //Checks out all the files tracked by the given commit.
        String commitID = findID(id);
        if (commitID == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit currHead = headCommit();
        File commitPath = join(COMMITS_DIR, commitID);
        Commit target = readObject(commitPath, Commit.class);
        checkUntrack(currHead, target.getfile());
        for (String i: target.getfile().keySet()) {
            checkout(id, "--", i);
        }
        //Removes tracked files that are not present in that commit.

        for (String i: currHead.getfile().keySet()) {
            if (target.getfile().get(i) == null) {
                File path =  join(CWD, i);
                path.delete();
            }
        }
        //moves the current branch’s head to that commit node.
        writeContents(HEAD_COMMIT, commitID);
        writeContents(currentBranch(), commitID);
        StagingArea.clearStage();
    }
    private static Commit findCommit(String commitID) {
        File path = join(COMMITS_DIR, commitID);
        Commit givenHead = readObject(path, Commit.class);
        return givenHead;
    }

    // CASE 3.2:
    //  given branch:  not modified
    //  current branch: modified
    //  result: current branch
    //

    // CASE 3.3:
    //  given branch:  modified same
    //  current branch: modified same
    //  result: unchanged (curr branch)
    //

    // CASE 3.4:
    //  split: x
    //  given branch:  x
    //  current branch: present
    //  result: unchanged (curr branch)
    //

    // CASE 3.5:
    //  split: x
    //  given branch:  present
    //  current branch: x
    //  result: given branch (check out & staged)
    //

    // CASE 3.6:
    //  split: present
    //  given branch:  x
    //  current branch: present same (not modified)
    //  result: remove (untracked)
    //  ！！唯一一个delete file的case

    // CASE 3.7:
    //  split: present
    //  given branch:  present same (not modified)
    //  current branch: x
    //  result: remain absent
    //

    // CASE 3.8:
    //  split: present version 1 / absent
    //  given branch:  modified version 2
    //  current branch: modified version 3
    //  result: re-write the file
    //          stage the file
    //

    //----------
    // Case 1-3.7:
    //  merge commits
    //  + log message: "Merged [given branch name] into [current branch name]."

    //Case 3.8 print(而非log) :"Encountered a merge conflict."

    private static void errorCheck(String branchName) {
        //ERROR 1
        // Have staged additions & removals
        HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
        HashMap<String, String> toRemove = readObject(STAGE_REMOVE, HashMap.class);
        if (stage.size() != 0 || toRemove.size() != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        //ERROR 2
        // branch name 不存在
        File add = join(BRANCHES_DIR, branchName);
        if (!add.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        //ERROR 3
        // 和自己merge &Print "Cannot merge a branch with itself."
        String activeBranch = readContentsAsString(CURR_BRANCH);
        if (activeBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }
    private static String findSplit(String givenID) {
        //find the latest split commit
        HashMap<String, Integer>  givenAncestors = bfs(givenID);
        String currID = readContentsAsString(HEAD_COMMIT);
        HashMap<String, Integer>  currAncestors = bfs(currID);
        Set<String> givenAns = givenAncestors.keySet();
        Set<String> currAns = currAncestors.keySet();
        //find the common ancestors
        currAns.retainAll(givenAns);

        Double val = Double.POSITIVE_INFINITY;
        String splitID = null;
        for (String i: currAns) {
            if (currAncestors.get(i) < val) {
                val = (double) currAncestors.get(i);
                splitID = i;
            }
        }
        return splitID;

    }
    private static void checkSplit(String splitID, String givenID, String branchName) {
        //-------
        //CASE 1:
        // If the split point is the same commit as the given branch,
        if (splitID.equals(givenID)) {
            System.out.print("Given branch is an ancestor of the current branch");
            System.exit(0);
        }
        //CASE 2:
        // If the split point is the current branch,
        if (splitID.equals(readContentsAsString(HEAD_COMMIT))) {
            checkout(branchName);
            System.out.print("Current branch fast-forwarded");
            System.exit(0);
        }
    }

    private static Set<String>[] divideSet(Set<String> all, HashMap<String, String> splitMap,
                                  HashMap<String, String> currMap,
                                  HashMap<String, String> givenMap) {
        Set<String>[] setArrays = new Set[4];
        Set<String> staySame = new HashSet<String>();
        Set<String> mergeConflict = new HashSet<String>();
        Set<String> toGiven = new HashSet<String>();
        Set<String> letsRemove = new HashSet<String>();

        for (String i: all) {
            String splitBlob = splitMap.get(i);
            String currBlob = currMap.get(i);
            String givenBlob = givenMap.get(i);
            if (splitBlob != null) {
                if (currBlob != null) {
                    if (givenBlob != null) {
                        if (currBlob.equals(splitBlob)) {
                            if (givenBlob.equals(splitBlob)) {
                                staySame.add(i);
                            } else {
                                toGiven.add(i);
                            }
                        } else {
                            if (givenBlob.equals(splitBlob)) {
                                staySame.add(i);
                            } else if (givenBlob.equals(currBlob)) {
                                staySame.add(i);
                            } else {
                                mergeConflict.add(i);
                            }
                        }
                    } else {
                        if (currBlob.equals(splitBlob)) {
                            letsRemove.add(i);
                        } else {
                            mergeConflict.add(i);
                        }
                    }
                } else {
                    if (givenBlob != null) {
                        if (givenBlob.equals(splitBlob)) {
                            staySame.add(i);
                        } else {
                            mergeConflict.add(i);
                        }
                    } else {
                        staySame.add(i);
                    }
                }
            } else {
                if (currBlob != null) {
                    if (givenBlob != null) {
                        if (givenBlob.equals(currBlob)) {
                            staySame.add(i);
                        } else {
                            mergeConflict.add(i);
                        }
                    } else {
                        staySame.add(i);
                    }
                } else {
                    if (givenBlob != null) {
                        toGiven.add(i);
                    } else {
                        staySame.add(i);
                    }
                }
            }
        }
        setArrays[0] = staySame;
        setArrays[1] = mergeConflict;
        setArrays[2] = toGiven;
        setArrays[3] = letsRemove;
        return setArrays;
    }


    public static void merge(String branchName) {
        errorCheck(branchName);
        HashMap<String, String> stage = readObject(STAGE_ADD, HashMap.class);
        HashMap<String, String> toRemove = readObject(STAGE_REMOVE, HashMap.class);
        File add = join(BRANCHES_DIR, branchName);
        String activeBranch = readContentsAsString(CURR_BRANCH);
        //ERROR 5
        // untracked file in the current commit would be overwritten or deleted by the merge
        Commit currHead = headCommit();
        HashMap<String, String> currMap = currHead.getfile();
        String givenID = readContentsAsString(add);
        Commit givenHead = findCommit(givenID);
        HashMap<String, String> givenMap = givenHead.getfile();
        checkUntrack(currHead, givenMap);

        String splitID = findSplit(givenID);
        Commit splitCommit = findCommit(splitID);


        checkSplit(splitID, givenID, branchName);
        // Otherwise, we continue with the steps below.
        // CASE 3.1:
        HashMap<String, String> splitMap = splitCommit.getfile();

        Set<String> all = new HashSet<>(splitMap.keySet());
        all.addAll(currMap.keySet());
        all.addAll(givenMap.keySet());

        Set<String>[] setArrays = divideSet(all, splitMap, currMap, givenMap);
        Set<String> staySame = setArrays[0];
        Set<String> mergeConflict = setArrays[1];
        Set<String> toGiven = setArrays[2];
        Set<String> letsRemove = setArrays[3];

        for (String i: toGiven) {
            checkout(givenID, "--", i);
            stage.put(i, givenMap.get(i));
        }
        for (String i: letsRemove) {
            toRemove.put(i, currMap.get(i));
            File path = join(CWD, i);
            path.delete();
        }
        for (String i: mergeConflict) {
            //find the file in the CWD
            File path = join(CWD, i);
            //Overwrite
            String curContent;
            String giveContent;
            if (currMap.get(i) != null) {
                File curr = join(BLOBS_DIR, currMap.get(i));
                curContent = readContentsAsString(curr);
            } else {
                curContent = "";
            }

            if (givenMap.get(i) != null) {
                File give = join(BLOBS_DIR, givenMap.get(i));
                giveContent = readContentsAsString(give);
            } else {
                giveContent = "";
            }

            String content = "<<<<<<< HEAD\n" + curContent
                    + "=======\n" + giveContent + ">>>>>>>\n";
            writeContents(path, content);
            String blobID = sha1(content);
            File addPath = join(BLOBS_DIR, blobID);
            writeContents(addPath, content);
            stage.put(i, blobID);
        }
        StagingArea.saveAdd(stage);
        StagingArea.saveRemove(toRemove);
        String mess = "Merged " + branchName + " into " + activeBranch + ".";

        commit(mess, givenID);
        if (!mergeConflict.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
        }
    }



    private static HashMap<String, Integer> bfs(String commitID) {
        //create a hashmap with: commitID as key, distance as value.
        boolean[] marked = new boolean[20];
        int[] edgeTo  = new int[20];
        int[] distTo  = new int[20];

        //create a queue: any vertices on the queue is not visited;
        // when we reach a new vertice, add to the queue;
        // when we visit it, remove it from the queue
        PriorityQueue<String> fringe = new PriorityQueue<String>();
        //Create a HashMap that stores the index and the corresponding commit ID
        HashMap<String, Integer> verticeString = new HashMap<String, Integer>();
        Integer s = 0;
        verticeString.put(commitID, s);

        //enqueue means: addLast
        fringe.add(commitID);
        //the sth index of marked, means the element with index s is visited
        marked[s] = true;
        distTo[s] = 0;
        //只要还有vertices没有被visited
        while (!fringe.isEmpty()) {
            //v 是queue上第一个element
            String v = fringe.remove();

            // for 这个element的每一个parent/adjacent
            Commit toVisit = findCommit(v);
            for (String i : toVisit.bothParents()) {
                if (i != null && !Commit.isInitial(toVisit)) {
                    s += 1;
                    verticeString.put(i, s);

                    //如果这个parent没有被marked
                    if (!marked[s]) {
                        //queue上加上这个parent
                        fringe.add(i);
                        //parent被visit过
                        marked[s] = true;
                        //标注它的neighbour是v
                        edgeTo[s] = verticeString.get(v);
                        distTo[s] = distTo[edgeTo[s]] + 1;
                    }
                }
            }
        }
        return verticeString;
    }
}
