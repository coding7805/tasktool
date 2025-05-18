
public class Main {
    public static void main(String[] args) {
        TaskMgr mgr = new TaskMgr();
        //String[] args = new String[1];
        //args[0] = "/Users/pony/IdeaProjects/tasktool/a.cmd";
        if (args.length == 0) {
            mgr.runInteractive();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-c")) {
            String file = args[1];
            if (mgr.compileFile(file)) {
                System.out.println("syntax of file " + file + " is ok! ");
            } else {
                System.exit(-1);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-e")) {
            String pass = args[1];
            System.out.println("encoded password:" + Utils.encode(pass));
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("-f")) {
            String file = args[1];
            if (mgr.compileFile(file)) {
                mgr.run(args);
            }
        }else {
            System.out.println("usage:");
            System.out.println("    1. java -jar " + System.getProperty("java.class.path") + " -f filename args...");
            System.out.println("    2. java -jar " + System.getProperty("java.class.path") + " -c filename");
            System.out.println("    3. java -jar " + System.getProperty("java.class.path") + " -e password");
            System.out.println("    4. java -jar " + System.getProperty("java.class.path"));
        }
    }
}
