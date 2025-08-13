import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

import javafx.scene.control.CheckBox;

import java.nio.file.*;
import java.io.File;
import java.io.IOException;

public class FileIndexer implements Runnable{
    private final HashSet<Path> userExcludedPaths=new HashSet<>();
    private volatile FileTrie activeTrie=new FileTrie();
    private final ScheduledExecutorService scheduler=Executors.newScheduledThreadPool(1);
    private final ExecutorService watchExecutor=Executors.newSingleThreadExecutor();

    public FileIndexer() {
        loadExcludedDefaults();
    }

    public void loadExcludedDefaults() {
        String userHome=System.getProperty("user.home");

        userExcludedPaths.add(Paths.get(userHome, "AppData").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get("C:", "Windows").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get("C:", "Program Files").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get("C:", "Program Files (x86)").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get("C:", "System Volume Information").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get("C:", "Temp").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get(userHome,".gradle").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get(userHome,".m2").toAbsolutePath().normalize());
        userExcludedPaths.add(Paths.get("C:","$Recycle.Bin").toAbsolutePath().normalize());
    }

    public void removeExcludedPaths(List<String> deselectedPaths) {
        for (String path : deselectedPaths) {
            userExcludedPaths.remove(Paths.get(path).toAbsolutePath().normalize());
        }
    }

    public void addToExcludedPaths(List<CheckBox> pathsToAdd) {
        for (CheckBox path : pathsToAdd) {
            userExcludedPaths.add(Paths.get(path.getText()).toAbsolutePath().normalize());
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void run() {
        indexAllDrivesSync ();

        scheduler.scheduleAtFixedRate(() -> {
            indexAllDrivesSync();
        }, 10, 10, TimeUnit.MINUTES);

        watchExecutor.submit(() -> {
            try {
                watchLiveFolders();
            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void indexAllDrivesSync() {
        FileTrie newTrie=new FileTrie();
            File[] roots=File.listRoots();
            for (File root : roots) {
                indexDirectory(root, newTrie);
            }
            activeTrie=newTrie;
            System.out.println("Indexing complete. Trie swapped at "+System.currentTimeMillis());
    }

    public void indexDirectory(File dir, FileTrie trie) {
        if (dir==null || !dir.exists() || !dir.canRead()) {
            return;
        }
        try {
            Path dirPath=dir.toPath().toAbsolutePath().normalize();
            for (Path excluded  : userExcludedPaths) {
                if (dirPath.equals(excluded) || dirPath.startsWith(excluded)) {
                    return;
                }
            }

            File[] files=dir.listFiles();
            if (files==null) return;

            for (File file : files) {
                if (file.isDirectory()) {
                    trie.insert(file.getName(), file.getAbsolutePath());
                    indexDirectory(file, trie);
                }
                else {
                    trie.insert(file.getName(), file.getAbsolutePath());
                }
            }
        }
        catch (Exception e) {
            System.err.println("Failed to index : "+dir.getAbsolutePath());
        }
    }

    private void watchLiveFolders() throws IOException, InterruptedException {
        WatchService watchService=FileSystems.getDefault().newWatchService();
        String userHome=System.getProperty("user.home");

        Path downloadsPath=Paths.get(userHome, "Downloads");
        Path desktopPath=Paths.get(userHome,"Desktop");

        registerFolder(watchService, downloadsPath);
        registerFolder(watchService, desktopPath);

        while (true) {
            WatchKey key=watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind=event.kind();
                Path dir=(Path) key.watchable();
                Path fullPath=dir.resolve((Path) event.context());

                File file=fullPath.toFile();
                String name=file.getName();

                if (kind==StandardWatchEventKinds.ENTRY_CREATE && file.isFile()) {
                    activeTrie.insert(name, file.getAbsolutePath());
                }
                else if (kind==StandardWatchEventKinds.ENTRY_DELETE && file.isFile()) {
                    activeTrie.remove(name, file.getAbsolutePath());
                }
            }
            key.reset();
        }
    }

    public void registerFolder(WatchService watchService, Path path) throws IOException {
        if (Files.exists(path)) {
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
        }
    }

    public List<String> search(String query) {
        return activeTrie.search(query);
    }
}