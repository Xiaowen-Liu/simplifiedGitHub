package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import static gitlet.Repository.GITLET_DIR;
import static gitlet.Utils.*;

public class StagingArea implements Serializable {
    public static final File STAGE_ADD = join(GITLET_DIR, "StagedAdd");
    public static final File STAGE_REMOVE = join(GITLET_DIR, "StagedRemove");

    public static void saveAdd(HashMap map) {
        writeObject(STAGE_ADD, map);
    }
    public static void saveRemove(HashMap map) {
        writeObject(STAGE_REMOVE, map);
    }

    public static void clearStage() {
        clearAdd();
        clearRemove();
    }


    public static void clearAdd() {
        HashMap<String, String> mp = new HashMap<String, String>();
        saveAdd(mp);
    }

    public static void clearRemove() {
        HashMap<String, String> mp = new HashMap<String, String>();
        saveRemove(mp);
    }
}

