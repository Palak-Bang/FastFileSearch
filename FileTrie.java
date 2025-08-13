import java.io.File;
import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children=new HashMap<>();
    List<String> filePaths=new ArrayList<>();
    boolean isEndOfWord=false;
}

public class FileTrie {
    private final TrieNode root=new TrieNode();
    private final Set<String> allFilePaths=new HashSet<>();

    public void insert(String fileName, String fullPath) {
        synchronized(this) {
            TrieNode node=root;
            fileName=fileName.toLowerCase();

            allFilePaths.add(fullPath);

            for (char c : fileName.toCharArray()) {
                node=node.children.computeIfAbsent(c, k->new TrieNode());
                node.filePaths.add(fullPath);
            }
            node.isEndOfWord=true;
        }
    }

    public void remove(String fileName, String fullPath) {
        allFilePaths.remove(fullPath);
    }

    public void clear() {
        synchronized(this) {
            root.children.clear();
            root.filePaths.clear();
            allFilePaths.clear();
        }
    }

    public List<String> search(String query) {
        TrieNode node=root;
        query=query.toLowerCase();
        boolean prefixMatch=true;

        for (char c : query.toCharArray()) {
            if (node.children.containsKey(c)) {
                node=node.children.get(c);
            }
            else {
                prefixMatch=false;
                break;
            }
        }
        if (prefixMatch && node!=null && node.filePaths!=null && !node.filePaths.isEmpty()) {
            return node.filePaths;
        }
        else {
            List<String> results=new ArrayList<>();
            for (String path : allFilePaths) {
                String fname=new File(path).getName().toLowerCase();
                if (fname.contains(query)) {
                    results.add(path);
                }
        }
        return results;
        }
    }
}