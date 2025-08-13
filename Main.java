import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);

        FileIndexer indexer=new FileIndexer();

        Thread indexThread=new Thread(indexer);
        indexThread.start();

        System.out.println("Indexing started");

        try {
            indexThread.join();
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            System.out.println("Search> ");
            String query=sc.nextLine();
            if (query.equalsIgnoreCase("")) break;
            List<String> results=indexer.search(query);
            if (results.isEmpty()) {
                System.out.println("No matching files found");
            }
            else {
                for (String result : results) {
                    System.out.println(result);
                }
            }
        }
        sc.close();
    }
}