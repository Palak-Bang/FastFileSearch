import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileSearchApp extends Application {

    private final ArrayList<String> excludedFolders = new ArrayList<>();
    private final List<CheckBox> presetOptions = new ArrayList<>();
    private Stage primaryStage;
    private FileIndexer index;

    private boolean isFirstRun() {
        File flagFile=new File(System.getProperty("user.home"),".fileSearchIndexed");
        return !flagFile.exists();
    }

    private void markIndexingComplete() {
        try {
            File flagFile=new File(System.getProperty("user.home"),".fileSearchIndexed");
            flagFile.createNewFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Smart File Search");
        if (isFirstRun()) {
            showExclusionScreen();
        }
        else {
            showMainSearchScreen();
        }
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Platform.runLater(() -> {
                primaryStage.hide();
            });
        });
    }

    private void showExclusionScreen() {
        VBox exclusionLayout = new VBox(10);
        exclusionLayout.setPadding(new Insets(20));
        exclusionLayout.getChildren().add(new Label("Select folders to exclude from indexing:"));

        String userHome = System.getProperty("user.home");

        String[] defaultPaths = {
            userHome + "\\AppData",
            "C:\\Program Files",
            "C:\\Program Files (x86)",
            "C:\\Windows",
            "C:\\$Recycle.Bin",
            "C:\\System Volume Information",
            "C:\\Temp"
        };

        for (String path : defaultPaths) {
            CheckBox cb = new CheckBox(path);
            cb.setSelected(true);
            presetOptions.add(cb);
            exclusionLayout.getChildren().add(cb);
        }

        Button moreButton = new Button("More...");
        moreButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select a folder to exclude");
            File folder = chooser.showDialog(primaryStage);
            if (folder != null) {
                CheckBox cb = new CheckBox(folder.getAbsolutePath());
                cb.setSelected(true);
                presetOptions.add(cb);
                exclusionLayout.getChildren().add(cb);
            }
        });

        Button continueBtn = new Button("Continue");
        continueBtn.setOnAction(e -> {
            for (CheckBox cb : presetOptions) {
                if (cb.isSelected()) {
                    excludedFolders.add(cb.getText());
                }
            }

            index=new FileIndexer();
            index.removeExcludedPaths(excludedFolders);
            index.addToExcludedPaths(presetOptions);
            index.start();

            showIndexingScreen();
        });

        exclusionLayout.getChildren().addAll(moreButton, continueBtn);
        Scene exclusionScene = new Scene(exclusionLayout, 500, 400);
        primaryStage.setScene(exclusionScene);
        primaryStage.show();
    }

    private void showIndexingScreen() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        ProgressIndicator progress = new ProgressIndicator();
        Label statusLabel = new Label("Indexing files");

        layout.getChildren().addAll(progress, statusLabel);
        Scene indexingScene = new Scene(layout, 400, 300);
        primaryStage.setScene(indexingScene);

        // Simulate indexing delay
        new Thread(() -> {
            try {
                Thread.sleep(3000); // simulate delay
            } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                layout.getChildren().remove(progress);
                statusLabel.setText("Indexing Complete");
                markIndexingComplete();

                Button proceed = new Button("Go to Search");
                proceed.setOnAction(ev -> showMainSearchScreen());
                layout.getChildren().add(proceed);
            });
        }).start();
    }

    private void showMainSearchScreen() {
        BorderPane mainLayout = new BorderPane();

        // Left menu
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(10));
        Button excludeBtn = new Button("Folders to Exclude");
        excludeBtn.setOnAction(e -> showExclusionScreen());
        Button reindexBtn = new Button("ReIndex");
        reindexBtn.setOnAction(e -> showIndexingScreen());
        menu.getChildren().addAll(excludeBtn, reindexBtn);

        // Search and Results
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(20));
        TextField searchBar = new TextField();
        searchBar.setPromptText("Search");
        ListView<String> results = new ListView<>();

        searchBar.setOnAction(e -> {
            String query=searchBar.getText().trim();
            if (!query.isEmpty() && index!=null) {
                List<String> matches=index.search(query);
                results.getItems().clear();
                results.getItems().addAll(matches.isEmpty() ? List.of("No matching files found") : matches);
            }
        });

        results.setOnMouseClicked(click -> {
            String selected=results.getSelectionModel().getSelectedItem();
            if (selected!=null && !selected.equals("No matching files found")) {
                try {
                    java.awt.Desktop.getDesktop().open(new File(selected));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        centerBox.getChildren().addAll(searchBar, results);

        mainLayout.setLeft(menu);
        mainLayout.setCenter(centerBox);

        Scene scene = new Scene(mainLayout, 700, 500);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}