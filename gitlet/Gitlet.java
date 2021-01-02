package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.Arrays;

/** Class that maintains the state of a single repository.
 *  @author Ashvin Dhawan
 */
public class Gitlet implements Serializable {

    /** Hashmap to store all commits. */
    private HashMap<String, Commit> commitMap;
    /** Hashmap to store files in staging area. */
    private HashMap<String, Blob> stageMap;
    /** String to indicate current branch. */
    private String _curbranch;
    /** Hashmap to store all branches and their relevant current commits. */
    private HashMap<String, Commit> branchMap;
    /** Hashmap to store files marked for removal. */
    private HashMap<String, Blob> removeMap;
    /** Hashmap to store String IDs of hashmaps. */
    private HashMap<HashMap, String> fileDirectory;
    /** Stores branchName to be added as a parent in the case of a merge. */
    private String _mergedBranch;
    /** list of all possible commands. */
    private List<String> _listArgs;
    /** length of a SHA-1 code. */
    static final int SHALENGTH = 40;


    /** Contructor for Gitlet class. */
    public Gitlet() {
        commitMap = new HashMap<String, Commit>();
        stageMap = new HashMap<String, Blob>();
        branchMap = new HashMap<String, Commit>();
        removeMap = new HashMap<String, Blob>();
        _listArgs = Arrays.asList("init", "add",
                "commit", "checkout", "log", "global-log", "rm",
                "rm-branch", "find", "status", "branch", "reset", "merge");
        fileDirectory = new HashMap<HashMap, String>();
        _curbranch = "master";
    }


    /** checks for incorrect operands based on INT
     * SIZE and ARRAYLIST ARGUMENTS. */
    public void checkIncorrectOperands(int size, ArrayList<String> arguments) {
        if (arguments.size() != size) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Reads arguments from user in OPERATOR and ARGUMENTS
     * and executes the correct gitlet command. */
    public void execute(String operator,
                        ArrayList<String> arguments) throws IOException {
        if (!_listArgs.contains(operator)) {
            System.out.println("No command with that name exists.");
            System.exit(0);
        } else {
            if (operator.equals("init")) {
                checkIncorrectOperands(0, arguments); this.init();
            } else if (!new File(".gitlet").exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            } else {
                if (operator.equals("add")) {
                    checkIncorrectOperands(1, arguments);
                    this.add(arguments.get(0));
                } else if (operator.equals("commit")) {
                    checkIncorrectOperands(1, arguments);
                    this.commit(arguments.get(0), false);
                } else if (operator.equals("checkout")) {
                    if (arguments.size() == 2
                            && arguments.get(0).equals("--")) {
                        this.checkout(arguments.get(1));
                    } else if (arguments.size() == 3
                            && arguments.get(1).equals("--")) {
                        this.checkout(arguments.get(0), arguments.get(2));
                    } else if (arguments.size() == 1) {
                        this.checkoutBranch(arguments.get(0));
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                } else if (operator.equals("log")) {
                    checkIncorrectOperands(0, arguments); this.log();
                } else if (operator.equals("global-log")) {
                    checkIncorrectOperands(0, arguments); this.globalLog();
                } else if (operator.equals("rm")) {
                    checkIncorrectOperands(1, arguments);
                    this.rm(arguments.get(0));
                } else if (operator.equals("rm-branch")) {
                    checkIncorrectOperands(1, arguments);
                    this.rmBranch(arguments.get(0));
                } else if (operator.equals("find")) {
                    checkIncorrectOperands(1, arguments);
                    this.find(arguments.get(0));
                } else if (operator.equals("status")) {
                    checkIncorrectOperands(0, arguments); this.status();
                } else if (operator.equals("branch")) {
                    checkIncorrectOperands(1, arguments);
                    this.branch(arguments.get(0));
                } else if (operator.equals("reset")) {
                    checkIncorrectOperands(1, arguments);
                    this.reset(arguments.get(0));
                } else if (operator.equals("merge")) {
                    checkIncorrectOperands(1, arguments);
                    this.merge(arguments.get(0));
                } else {
                    System.out.println("No command with that name exists.");
                    System.exit(0);
                }
            }
        }
    }

    /** initializes a gitlet repository in the
     * working directory and specifies a default initial commit. */
    public void init() throws IOException {
        Path path = Paths.get(".gitlet");
        if (Files.exists(path)) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            System.exit(0);
        } else {
            Files.createDirectory(path);
            Commit emptyCommit = new Commit("initial commit", null,
                    new HashMap<String, Blob>(), new Date(0), _curbranch);
            byte[] initial = Utils.serialize(emptyCommit);
            commitMap.put(Utils.sha1(initial), emptyCommit);
            commitMap.put("Current commit", emptyCommit);
            branchMap.put("master", emptyCommit);
            pushHashMap(commitMap, "commitMap");
            pushHashMap(stageMap, "stageMap");
            pushHashMap(branchMap, "branchMap");
            pushHashMap(removeMap, "removeMap");
            pushCurrBranch(_curbranch);
        }
    }

    /** Persistence serialization for current branch with string CUR. */
    public void pushCurrBranch(String cur) {
        String branchPath = System.getProperty("user.dir")
                + "/.gitlet/" + "branch";
        File file = new File(branchPath);
        Utils.writeContents(file, Utils.serialize(cur));
    }

    /** Persistence accessor for current branch; return string. */
    public String pullCurrBranch() {
        String branchPath = System.getProperty("user.dir")
                + "/.gitlet/" + "branch";
        File file = new File(branchPath);
        return Utils.readObject(file, String.class);
    }

    /** Reserializes and writes a hashmap MAP identified by a unique
     * FILENAME to save changes for the next run. */
    public void pushHashMap(HashMap map, String fileName) {
        String mapPath = System.getProperty("user.dir")
                + "/.gitlet/" + fileName;
        File file = new File(mapPath);
        Utils.writeContents(file, Utils.serialize(map));
    }

    /** Unserializes and returns a hashmap identified by a
     *  unique FILENAME to be modified. */
    @SuppressWarnings("unchecked")
    public HashMap<String, Blob> pullBlobHashMap(String fileName) {
        String mapPath = System.getProperty("user.dir")
                + "/.gitlet/" + fileName;
        File file = new File(mapPath);
        return Utils.readObject(file, HashMap.class);
    }

    /** Unserializes and returns a hashmap identified by a
     *  unique FILENAME to be modified. */
    @SuppressWarnings("unchecked")
    public HashMap<String, Commit> pullCommitHashMap(String fileName) {
        String mapPath = System.getProperty("user.dir")
                + "/.gitlet/" + fileName;
        File file = new File(mapPath);
        return Utils.readObject(file, HashMap.class);
    }

    /** Adds file FILENAME to the staging area. */
    public void add(String fileName) {
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        String filePath = System.getProperty("user.dir") + "/" + fileName;
        File file = new File(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Blob toAdd = new Blob(fileName);
        byte [] serializedBlob = Utils.serialize(toAdd);
        String byteSha = Utils.sha1(serializedBlob);

        HashMap tempRemoveMap = pullBlobHashMap("removeMap");
        if (tempRemoveMap.get(fileName) != null) {
            tempRemoveMap.remove(fileName);
        }
        pushHashMap(tempRemoveMap, "removeMap");

        HashMap tempCommitMap = pullCommitHashMap("commitMap");
        Commit currentCommit = (Commit) tempCommitMap.get("Current commit");
        if (currentCommit.bL().get(fileName) != null) {
            if (currentCommit.bL().get(fileName)
                    .caS().equals
                            (Utils.readContentsAsString(file))) {
                tempStageMap.remove(fileName);
                return;
            }
        }
        tempStageMap.put(fileName, toAdd);
        pushHashMap(tempStageMap, "stageMap");
    }

    /** Creates a commit, or a snapshot of all changed files in the
     * working directory, with a corresponding MESSAGE and boolean ISMERGE. */
    public void commit(String message, boolean isMerge) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        HashMap<String, Blob> newBlobList = new HashMap<String, Blob>();
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        HashMap<String, Commit> tempBranchMap = pullCommitHashMap("branchMap");
        String tempCurrBranch = pullCurrBranch();
        Commit compareCommit = tempCommitMap.get("Current commit");
        String compareCommitId = compareCommit.sha();

        HashMap<String, Blob> parentBlobList = compareCommit.bL();
        for (Map.Entry<String,
                Blob> entry : parentBlobList.entrySet()) {
            newBlobList.put(entry.getKey(), entry.getValue());
        }
        boolean isChanged = false;
        HashMap<String, Blob> tempRemoveMap = pullBlobHashMap("removeMap");
        for (Map.Entry<String, Blob> entry : tempRemoveMap.entrySet()) {
            newBlobList.remove(entry.getKey());
            isChanged = true;
        }
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        for (Map.Entry<String, Blob> entry : tempStageMap.entrySet()) {
            newBlobList.put(entry.getKey(), entry.getValue());
            isChanged = true;
        }
        if (!isChanged) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        ArrayList<String> parents = new ArrayList<String>();
        parents.add(compareCommitId);
        if (isMerge) {
            parents.add(tempBranchMap.get(_mergedBranch).sha());
        }
        Commit newCommit = new Commit(message, parents,
                newBlobList, new Date(), tempCurrBranch);
        tempCommitMap.put(newCommit.sha(), newCommit);
        tempCommitMap.put("Current commit", newCommit);
        tempBranchMap.put(tempCurrBranch, newCommit);
        pushHashMap(tempCommitMap, "commitMap");
        tempRemoveMap.clear();
        pushHashMap(tempRemoveMap, "removeMap");
        pushHashMap(tempBranchMap, "branchMap");
        tempStageMap.clear();
        pushHashMap(tempStageMap, "stageMap");
    }

    /** Restores version of file FILENAME from the most
     * recent commit in the working directory. */
    public void checkout(String fileName) throws IOException {
        HashMap<String, Commit>  tempCommitMap = pullCommitHashMap("commitMap");
        Commit curCommit = tempCommitMap.get("Current commit");
        if (!curCommit.bL().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob thisBlob = curCommit.bL().get(fileName);
        List<String> allFiles =
                Utils.plainFilenamesIn(System.getProperty("user.dir"));
        if (allFiles.contains(fileName)) {
            Path path = Paths.get(thisBlob.name());
            Utils.writeContents(path.toFile(), thisBlob.conts());
        } else {
            Path path = Paths.get(thisBlob.name());
            Utils.writeContents(path.toFile(), thisBlob.conts());
        }
        pushHashMap(tempCommitMap, "commitMap");
    }

    /** Restores version of file FILENAME from commit
     * with id COMMITSHA in the working directory. */
    public void checkout(String commitSha, String fileName) throws IOException {
        HashMap<String, Commit>  tempCommitMap = pullCommitHashMap("commitMap");
        if (commitSha.length() < SHALENGTH) {
            for (Map.Entry<String, Commit> entry : tempCommitMap.entrySet()) {
                if (entry.getKey().contains(commitSha)) {
                    commitSha = entry.getKey();
                }
            }
        }
        if (!tempCommitMap.containsKey(commitSha)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit prevCommit = tempCommitMap.get(commitSha);
        if (!prevCommit.bL().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob thisBlob = prevCommit.bL().get(fileName);
        Path path = Paths.get(thisBlob.name());
        Utils.writeContents(path.toFile(), thisBlob.conts());
        pushHashMap(tempCommitMap, "commitMap");
    }

    /** checkouts entire BRANCH to working directory. */
    public void checkoutBranch(String branch) throws IOException {
        HashMap<String, Commit> tempBranchMap = pullCommitHashMap("branchMap");
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        String tempCurrBranch = pullCurrBranch();
        HashMap<String, Blob> untracked = getCurrentUntracked();
        List<String> allFiles =
                Utils.plainFilenamesIn(System.getProperty("user.dir"));
        if (tempBranchMap.get(branch) == null) {
            System.out.println("No such branch exists.");
        } else if (tempCurrBranch.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
        } else {
            Commit target = tempBranchMap.get(branch);
            Commit cur = tempCommitMap.get("Current commit");
            for (Map.Entry<String, Blob> entry : untracked.entrySet()) {
                if (target.bL().containsKey(entry.getKey())) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it or add it first.");
                    System.exit(0);
                }
            }
            for (Map.Entry<String, Blob> entry : cur.bL().entrySet()) {
                if (!target.bL().containsKey(entry.getKey())) {
                    Utils.restrictedDelete(entry.getKey());
                }
            }
            for (Map.Entry<String, Blob> entry : target.bL().entrySet()) {
                if (allFiles.contains(entry.getKey())) {
                    Path path = Paths.get(entry.getKey());
                    Utils.writeContents(path.toFile(),
                            entry.getValue().conts());
                } else {
                    Path path = Paths.get(entry.getKey());
                    Utils.writeContents(path.toFile(),
                            entry.getValue().conts());
                }
            }
            tempCurrBranch = branch;
            tempCommitMap.put("Current commit", tempBranchMap.get(branch));
            pushHashMap(tempCommitMap, "commitMap");
            pushHashMap(tempBranchMap, "branchMap");
            HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
            tempStageMap.clear();
            pushHashMap(tempStageMap, "stageMap");
            pushCurrBranch(tempCurrBranch);
        }
    }
    /** resets working directory and head branch
     * to a given commit with id COMMITID. */
    public void reset(String commitId) throws IOException {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        Commit cur = tempCommitMap.get("Current commit");
        Commit target = tempCommitMap.get(commitId);
        if (commitId.length() < SHALENGTH) {
            for (Map.Entry<String, Commit> entry : tempCommitMap.entrySet()) {
                if (entry.getKey().contains(commitId)) {
                    commitId = entry.getKey();
                }
            }
        }
        if (!tempCommitMap.containsKey(commitId)) {
            System.out.println("No commit with that id exists.");
        } else {
            HashMap<String, Blob> untracked = getCurrentUntracked();
            List<String> allFiles =
                    Utils.plainFilenamesIn(System.getProperty("user.dir"));
            boolean isThere = true;
            for (String i : allFiles) {
                if (!cur.bL().containsKey(i) && target.bL().containsKey(i)) {
                    isThere = false;
                }
            }
            for (Map.Entry<String, Blob> entry : untracked.entrySet()) {
                if (target.bL().containsKey(entry) || !isThere) {
                    System.out.println("There is an untracked file in "
                            + "the way; delete it or add it first.");
                    return;
                }
            }
            for (Map.Entry<String, Blob> entry : cur.bL().entrySet()) {
                if (!target.bL().containsKey(entry.getKey())) {
                    rm(entry.getKey());
                }
            }
            for (Map.Entry<String, Blob> entry : target.bL().entrySet()) {
                checkout(commitId, entry.getKey());
            }
            tempCommitMap.put("Current commit", tempCommitMap.get(commitId));
            pushHashMap(tempCommitMap, "commitMap");
        }
    }

    /** creates new BRANCH and assigns its head to the current branch. */
    public void branch(String branch) {
        HashMap<String, Commit> tempBranchMap = pullCommitHashMap("branchMap");
        if (tempBranchMap.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
        } else {
            HashMap<String, Commit> tempCommitMap =
                    pullCommitHashMap("commitMap");
            tempBranchMap.put(branch, tempCommitMap.get("Current commit"));
            pushHashMap(tempCommitMap, "commitMap");
            pushHashMap(tempBranchMap, "branchMap");
        }
    }
    /** Prints out commit id, date, and commit message
     * of current commit and all ancestor commits. */
    public void log() {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        Commit curCommit = tempCommitMap.get("Current commit");
        while (curCommit != null) {
            if (curCommit.parent() != null && curCommit.parent().size() == 2) {
                System.out.println("===");
                String idLine = "commit " + curCommit.sha();
                System.out.println(idLine);
                String mergeLine = "Merge: "
                        + curCommit.parent().get(0).substring(0, 7) + " "
                        + curCommit.parent().get(1).substring(0, 7);
                System.out.println(mergeLine);
                String dateLine = "Date: " + curCommit.toDate();
                System.out.println(dateLine);
                System.out.println(curCommit.message());
                System.out.println();
                curCommit = tempCommitMap.get(curCommit.firstParent());
            } else {
                System.out.println("===");
                String idLine = "commit " + curCommit.sha();
                System.out.println(idLine);
                String dateLine = "Date: " + curCommit.toDate();
                System.out.println(dateLine);
                System.out.println(curCommit.message());
                System.out.println();
                curCommit = tempCommitMap.get(curCommit.firstParent());
            }
        }
    }

    /** Log function for every commit ever made. */
    public void globalLog() {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        for (Map.Entry<String, Commit> entry : tempCommitMap.entrySet()) {
            if (!entry.getKey().equals("Current commit")) {
                if (entry.getValue().parent() != null
                        && entry.getValue().parent().size() == 2) {
                    System.out.println("===");
                    String idLine = "commit " + entry.getKey();
                    System.out.println(idLine);
                    String mergeLine = "Merge: "
                            + entry.getValue().parent().get(0).substring(0, 7)
                            + " "
                            + entry.getValue().parent().get(1).substring(0, 7);
                    System.out.println(mergeLine);
                    String dateLine = "Date: " + entry.getValue().toDate();
                    System.out.println(dateLine);
                    System.out.println(entry.getValue().message());
                    System.out.println();
                } else {
                    System.out.println("===");
                    String idLine = "commit " + entry.getKey();
                    System.out.println(idLine);
                    String dateLine = "Date: " + entry.getValue().toDate();
                    System.out.println(dateLine);
                    System.out.println(entry.getValue().message());
                    System.out.println();
                }
            }
        }
        pushHashMap(tempCommitMap, "commitMap");
    }

    /** removes branch with name BRANCHNAME if applicable. */
    public void rmBranch(String branchName) {
        HashMap<String, Commit> tempBranchMap = pullCommitHashMap("branchMap");
        String tempCurBranch = pullCurrBranch();
        if (!tempBranchMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (tempCurBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            tempBranchMap.remove(branchName);
        }
        pushHashMap(tempBranchMap, "branchMap");
    }

    /** Prints branches, removed files, and staged files. */
    public void status() {
        ArrayList<String> sorter = new ArrayList<String>();
        HashMap<String, Commit> tempBranchMap = pullCommitHashMap("branchMap");
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        HashMap<String, Blob> tempRemoveMap = pullBlobHashMap("removeMap");
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        String tempCurrBranch = pullCurrBranch();
        System.out.println("=== Branches ===");
        for (Map.Entry<String, Commit> entry : tempBranchMap.entrySet()) {
            sorter.add(entry.getKey());
        }
        sorter.sort(String::compareToIgnoreCase);
        for (String i : sorter) {
            if (i.equals(tempCurrBranch)) {
                System.out.println("*" + i);
            } else {
                System.out.println(i);
            }
        }
        sorter.clear(); System.out.println();
        System.out.println("=== Staged Files ===");
        for (Map.Entry<String, Blob> entry : tempStageMap.entrySet()) {
            sorter.add(entry.getKey());
        }
        sorter.sort(String::compareToIgnoreCase);
        for (String i : sorter) {
            System.out.println(i);
        }
        sorter.clear(); System.out.println();
        System.out.println("=== Removed Files ===");
        for (Map.Entry<String, Blob> entry : tempRemoveMap.entrySet()) {
            sorter.add(entry.getKey());
        }
        sorter.sort(String::compareToIgnoreCase);
        for (String i : sorter) {
            System.out.println(i);
        }
        sorter.clear(); System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
        pushHashMap(tempBranchMap, "branchMap");
        pushHashMap(tempStageMap, "stageMap");
        pushHashMap(tempRemoveMap, "removeMap");
        pushHashMap(tempCommitMap, "commitMap");
    }


    /** returns HashMap of currently untracked files. */
    public HashMap<String, Blob> getCurrentUntracked() {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        Commit cur = tempCommitMap.get("Current commit");
        List<String> allFiles =
                Utils.plainFilenamesIn(System.getProperty("user.dir"));
        HashMap<String, Blob> untracked = new HashMap<String, Blob>();
        for (String i : allFiles) {
            if (!cur.bL().containsKey(i)
                    && !tempStageMap.containsKey(i)) {
                untracked.put(i, new Blob(i));
            }
        }
        return untracked;
    }
    /** prints IDs of all commits with the given Commit MESSAGE. */
    public void find(String message) {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        boolean isCommit = false;
        for (Map.Entry<String, Commit> entry : tempCommitMap.entrySet()) {
            if (!entry.getKey().equals("Current commit")) {
                if (entry.getValue().message().equals(message)) {
                    isCommit = true;
                    System.out.println(entry.getKey());
                }
            }
        }
        if (!isCommit) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** removes file FILENAME from git tracking. */
    public void rm(String fileName) {
        boolean isReason = false;
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        HashMap<String, Blob> tempRemoveMap = pullBlobHashMap("removeMap");
        if (tempStageMap.containsKey(fileName)) {
            isReason = true;
            tempStageMap.remove(fileName);
            pushHashMap(tempStageMap, "stageMap");
            System.exit(0);
        }
        List<String> allFiles =
                Utils.plainFilenamesIn(System.getProperty("user.dir"));
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        Commit curCommit = tempCommitMap.get("Current commit");
        if (curCommit.bL().containsKey(fileName)) {
            isReason = true;
            if (allFiles.contains(fileName)) {
                tempRemoveMap.put(fileName, new Blob(fileName));
            } else {
                tempRemoveMap.put(fileName, null);
            }
            File file = new File(fileName);
            Utils.restrictedDelete(fileName);
        }
        pushHashMap(tempRemoveMap, "removeMap");
        if (!isReason) {
            System.out.println("No reason to remove the file.");
        }
    }

    /** merges two branches, the current branch and the branch BRANCHNAME. */
    public void merge(String branchName) throws IOException {
        HashMap<String, Blob> tempStageMap = pullBlobHashMap("stageMap");
        HashMap<String, Blob> tempRemoveMap = pullBlobHashMap("removeMap");
        HashMap<String, Commit> tempBranchMap = pullCommitHashMap("branchMap");
        String tempCurrBranch = pullCurrBranch();
        checkMergeFail(tempRemoveMap, tempStageMap,
                tempBranchMap, tempCurrBranch, branchName);
        Commit cur = tempBranchMap.get(tempCurrBranch);
        Commit given = tempBranchMap.get(branchName);
        Commit splitPoint = findSplitPoint(cur, given);
        checkAncestors(cur, given, splitPoint, tempBranchMap, tempCurrBranch);
        checkTrackFail(branchName, splitPoint, cur, given);
        HashMap<String, List> md = new HashMap<String, List>();
        for (Map.Entry<String, Blob> entry : splitPoint.bL().entrySet()) {
            String key = entry.getKey(); Blob value = entry.getValue();
            byte[] vC = value.conts();
            HashMap<String, Blob> gL = given.bL();
            HashMap<String, Blob> cL = cur.bL();
            if (gL.containsKey(key) && cL.containsKey(key)) {
                if (!gL.get(key).equals(vC) && cL.get(key).equals(vC)) {
                    checkout(given.sha(), key); add(key);
                }
                if (!gL.get(key).conts().equals(vC)
                        && !cL.get(key).conts().equals(vC)) {
                    if (!gL.get(key).conts().equals(cL.get(key).conts())) {
                        md.put(key, Arrays.asList(cL.get(key).caS(),
                                gL.get(key).caS()));
                    }
                }
            }
            if (!gL.containsKey(key) && cL.containsKey(key)) {
                if (Arrays.equals(cL.get(key).conts(), vC)) {
                    rm(key);
                } else {
                    md.put(key, Arrays.asList(cL.get(key).caS(), ""));
                }
            }
            if (gL.containsKey(key) && !cL.containsKey(key)) {
                if (!Arrays.equals(gL.get(key).conts(), vC)) {
                    md.put(key, Arrays.asList("", gL.get(key).caS()));
                }
            }
        }
        for (Map.Entry<String, Blob> entry : given.bL().entrySet()) {
            if (!cur.bL().containsKey(entry.getKey())
                    && !splitPoint.bL().containsKey(entry.getKey())) {
                checkout(given.sha(), entry.getKey()); add(entry.getKey());
            }
            if (!splitPoint.bL().containsKey(entry.getKey())
                    && cur.bL().containsKey(entry.getKey())) {
                if (!entry.getValue().conts().equals
                        (cur.bL().get(entry.getKey()).conts())) {
                    md.put(entry.getKey(), Arrays.asList(cur.bL().
                            get(entry.getKey()).caS(), given.bL().
                            get(entry.getKey()).caS()));
                }
            }
        }
        cleanup(md, tempStageMap, tempBranchMap, branchName, tempCurrBranch);
    }

    /** shortener method, takes in MODIFIEDDIFFERENT, TEMPSTAGEMAP,
     * TEMPBRANCHMAP, BRANCHNAME, TEMPCURRBRANCH. */
    public void cleanup(HashMap<String, List> modifiedDifferent,
                        HashMap<String, Blob> tempStageMap,
                        HashMap<String, Commit> tempBranchMap,
                        String branchName, String tempCurrBranch) {
        for (Map.Entry<String, List> entry : modifiedDifferent.entrySet()) {
            Path path = Paths.get(entry.getKey());
            String conflict = "<<<<<<< HEAD\n"
                    + entry.getValue().get(0) + "=======\n"
                    + entry.getValue().get(1) + ">>>>>>>";
            Utils.writeContents(path.toFile(), conflict);
            add(entry.getKey());
        }
        pushHashMap(tempStageMap, "stageMap");
        pushHashMap(tempBranchMap, "branchMap");
        _mergedBranch = branchName;
        commit("Merged " + branchName + " into " + tempCurrBranch + ".", true);
        if (modifiedDifferent.size() > 0) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Returns latest common ancestor COMMIT of commits CUR and GIVEN. */
    public Commit findSplitPoint(Commit cur, Commit given) {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        ArrayList<String> curList = new ArrayList<String>();
        ArrayList<String> givenList = new ArrayList<String>();
        String curSha = Utils.sha1(Utils.serialize(cur));
        String givenSha = Utils.sha1(Utils.serialize(given));
        if (curSha.equals(givenSha)) {
            return cur;
        }
        while (cur.firstParent() != null && given.firstParent() != null) {
            if (cur.firstParent().equals(given.firstParent())) {
                return tempCommitMap.get(cur.firstParent());
            } else if (given.parent().contains(cur.firstParent())) {
                return tempCommitMap.get(cur.firstParent());
            } else if (cur.parent().contains(given.firstParent())) {
                return tempCommitMap.get(given.firstParent());
            } else {
                if (cur.parent().size() > 1) {
                    for (String i : cur.parent()) {
                        curList.add(i);
                    }
                } else {
                    curList.add(cur.firstParent());
                }
                if (given.parent().size() > 1) {
                    for (String i : given.parent()) {
                        givenList.add(i);
                    }
                } else {
                    givenList.add(given.firstParent());
                }
                if (curList.contains(Utils.sha1(Utils.serialize(given)))) {
                    return (Commit) tempCommitMap.get
                            (Utils.sha1(Utils.serialize(given)));
                } else if (givenList.contains
                        (Utils.sha1(Utils.serialize(cur)))) {
                    return (Commit) tempCommitMap.get
                            (Utils.sha1(Utils.serialize(cur)));
                }
                cur = (Commit) tempCommitMap.get(cur.firstParent());
                given = (Commit) tempCommitMap.get(given.firstParent());
            }
        }
        if (cur.firstParent() == null) {
            return cur;
        } else if (given.firstParent() == null) {
            return given;
        }
        return null;
    }

    /** Checks for untracked failure cases in merge,
     *  takes in BRANCHNAME, SPLITPOINT, CUR, and GIVEN. */
    public void checkTrackFail(String branchName,
                               Commit splitPoint, Commit cur, Commit given) {
        HashMap<String, Blob> untracked = getCurrentUntracked();
        List<String> allFiles =
                Utils.plainFilenamesIn(System.getProperty("user.dir"));

        for (Map.Entry<String, Blob> entry : untracked.entrySet()) {
            String key = entry.getKey();
            Blob value = entry.getValue();
            if (given.bL().containsKey(key)
                    && splitPoint.bL().containsKey(key)
                    && !given.bL().get(key).equals
                    (splitPoint.bL().get(key))) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it or add it first.");
                System.exit(0);
            }
            if (!splitPoint.bL().containsKey(key)
                    && given.bL().containsKey(key)) {
                System.out.println("There is an untracked file "
                       + "in the way; delete it or add it first.");
                System.exit(0);
            }
        }
    }

    /** Checks failures in merge, takes in TEMPREMOVEMAP, TEMPSTAGEMAP,
     * TEMPBRANCHMAP, TEMPCURRBRANCH, AND BRANCHNAME. */
    public void checkMergeFail(HashMap tempRemoveMap,
                               HashMap tempStageMap, HashMap tempBranchMap,
                               String tempCurrBranch, String branchName) {
        if (!tempRemoveMap.isEmpty() || !tempStageMap.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        } else if (!tempBranchMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (tempCurrBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    /** Takes in CUR, GIVEN, and SPLITPOINT, TEMPBRANCHMAP,
     * AND TEMPCURRBRANCH and checks ancestors. */
    public void checkAncestors(Commit cur, Commit given,
                               Commit splitPoint,
                               HashMap<String, Commit> tempBranchMap,
                               String tempCurrBranch) {
        HashMap<String, Commit> tempCommitMap = pullCommitHashMap("commitMap");
        ArrayList<String> ancestorCur = new ArrayList<String>();
        ArrayList<String> ancestorGiven = new ArrayList<String>();
        Commit tempCur = cur;
        Commit tempGiven = given;
        while (tempCur.firstParent() != null) {
            ancestorCur.add(tempCur.firstParent());
            tempCur = tempCommitMap.get(tempCur.firstParent());
        }
        while (tempGiven.firstParent() != null) {
            ancestorGiven.add(tempGiven.firstParent());
            tempGiven = tempCommitMap.get(tempGiven.firstParent());
        }
        if (ancestorCur.contains(given.sha())) {
            splitPoint = given;
        }
        if (ancestorGiven.contains(cur.sha())) {
            splitPoint = cur;
        }
        if (splitPoint.sha().equals(given.sha())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            System.exit(0);
        } else if (splitPoint.sha().equals(cur.sha())) {
            tempBranchMap.put(tempCurrBranch, given);
            for (Map.Entry<String, Blob> entry : splitPoint.bL().entrySet()) {
                if (!given.bL().containsKey(entry.getKey())) {
                    rm(entry.getKey());
                }
            }
            System.out.println("Current branch fast-forwarded.");
            pushHashMap(tempBranchMap, "branchMap");
            System.exit(0);
        }
    }
}

