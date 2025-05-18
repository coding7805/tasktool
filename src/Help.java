public class Help {
    static public void printHelp() {
        String help =
                "List of all tasktool commands:\n" +
                "Note: 1. command must start with '.' \n" +
                "Note: 2. command must end with ';' except embedded in .for/.if command\n" +
                ".help          Display this help              \n" +
                ".logon         Logon to database              \n" +
                ".logoff        Logoff from database           \n" +
                ".os            Execute a system shell command \n" +
                ".print         Print some message             \n" +
                ".sql get       Execute and display query      \n" +
                ".sql           Execute query                  \n" +
                ".set           Set some control info          \n" +
                ".if            Condition expression           \n" +
                ".for           For statement                  \n" +
                ".continue      Continue, in For statement     \n" +
                ".break         Break out the for statement    \n" +
                ".exit          Exit tasktool. Same as .quit   \n"
                ;
        System.out.println(help);
    }
}
