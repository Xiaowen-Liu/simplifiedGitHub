package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Xiaowen Liu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        //if user inputs a command with the wrong number or format of operands
        // print the message Incorrect operands.

        //if a user inputs a command that requires
        // being in an initialized Gitlet working directory
        // but is not in such a directory
        // Print error message
        //Repository.setupPersistence();
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // handle the `init` command
                Repository.init();
                break;
            case "add":
                // handle the `add [filename]` command
                Repository.add(args[1]);
                break;
            case "commit":
                //handle the commit command
                Repository.commit(args[1], null);
                break;
            case "checkout":
                //handle the commit command
                //How to deal with "checkout -- [file name]"
                if (args.length == 3) {
                    Repository.checkout(args[1], args[2]);
                } else if (args.length == 4) {
                    Repository.checkout(args[1], args[2], args[3]);
                } else {
                    Repository.checkout(args[1]);
                }
                break;
            case "log":
                Repository.log();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "rm":
                Repository.rm(args[1]);
                break;
            case "branch":
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                Repository.rmBranch(args[1]);
                break;
            case "status":
                Repository.status();
                break;
            case "reset":
                Repository.reset(args[1]);
                break;
            case "merge":
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;

            //If a user inputs a command that doesn’t exist, print the message
            // (see the bottom of page 12)
        }
    }
}
