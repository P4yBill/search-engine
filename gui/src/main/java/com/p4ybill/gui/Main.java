package com.p4ybill.gui;
import com.p4ybill.engine.demo.Engine;
import com.p4ybill.engine.queryparser.QueryResult;

import java.io.*;
import java.util.Scanner;

public class Main{

    public static void main(String[] args) throws IOException {
        // TODO: Create a JavaFx gui app
        startGui();


//        Application.launch(args);
    }

    /**
     * Starts a terminal gui.
     * TODO: Move gui to another class
     *
     * @throws IOException
     */
    public static void startGui() throws IOException {
        Scanner scanner = new Scanner(System.in);  // Create a Scanner object
        String filePath = "";
        Engine engine;
        while(true){
            try{
                filePath = getCorrectFilePathFromUser(scanner);
                engine = new Engine(filePath);
                break;
            }catch (IllegalStateException ise){
                System.out.println(ise.getMessage());
            }
        }

        if(filePath != null && !filePath.equals("")){
            getEngineReady(engine, scanner);
            queryHandler(engine, scanner);
        }
    }

    /**
     * Asks user to type and submit a query and shows results.
     * This done until user types "/exit/" to quit the app
     *
     * @param engine the engine
     * @param scanner
     */
    private static void queryHandler(Engine engine, Scanner scanner){
        String query = "";
        String exitString = "/exit/";
        while(query.isEmpty() || query.isBlank() || !query.equals(exitString)){
            System.out.println("[+] Type your query: (type "+ exitString +" (the slashes are important) to quit app)");
            query = scanner.nextLine();
            if(query.equals(exitString)){
                break;
            }
            if(!query.isEmpty() && !query.isBlank() && !query.equals(exitString)) {
                try {
                    System.out.println(query);
                    QueryResult res = engine.query(query);
                    printResultDocs(res);
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Prints the results docs to the screen
     * @param res
     */
    private static void printResultDocs(QueryResult res){
//        System.out.printf("### Total Results: %d ###\n", res.getScoreDocuments().size());
//        for (ScoreDocument doc : res.getScoreDocuments()) {
//            System.out.printf("## DOC ID: %d ##\n", doc.getDocId());
//            System.out.println("Filename: " + res.getFileNames().get(doc.getDocId()));
//        }
//
        // We show the docs in reverse order because its more readable in the terminal.
        for(int i = res.getScoreDocuments().size() - 1; i >= 0 ; i--){
            int docId = res.getScoreDocuments().get(i).getDocId();
            System.out.println("Filename: " + res.getFileNames().get(docId));
            System.out.printf("## %d. DOC ID: %d ##\n", i + 1, docId);
        }
        System.out.printf("### Total Results: %d ###\n", res.getScoreDocuments().size());
    }

    /**
     * Checks if the folder provided is already indexed.
     * If not, index the folder.
     * If its indexed, asks the user if he want to re-index or to use the old indexed files.
     *
     * @param engine
     * @param scanner
     * @throws IOException
     */
    private static void getEngineReady(Engine engine, Scanner scanner) throws IOException {
        if(engine.isAlreadyIndexed()){
            boolean wantToLoad = wantToLoad(scanner);

            if(wantToLoad){
                engine.load();
            }else{
                engineIndexDocs(engine);
            }
        }else{
            engineIndexDocs(engine);
        }
    }

    /**
     * Indexes the docs that are in the provided folder
     *
     * @param engine
     * @throws IOException
     */
    private static void engineIndexDocs(Engine engine) throws IOException {
        System.out.println("Indexing...");
        engine.indexDocs();
        System.out.println("Done indexing");
    }

    /**
     * Asks the user if he wants to re-index or to load the old files related to the index.
     *
     * @param scanner
     * @return
     */
    private static boolean wantToLoad(Scanner scanner){
        String loadAnswer = "";
        System.out.println("Looks like the given folder is already indexed");
        while(!loadAnswer.equals("l") && !loadAnswer.equals("ri")){
            System.out.println("Do you want to load the files or re-index? (type l for load or ri for re-index)");

            loadAnswer = scanner.nextLine();
        }

        return loadAnswer.equals("l");
    }

    /**
     * Gets a valid folder path from user input.
     *
     * @param scanner
     * @return
     */
    private static String getCorrectFilePathFromUser(Scanner scanner){
        String filePath = "";
        while(filePath.equals("")){
            System.out.println("Please type the folder you want to index: ");
            filePath = scanner.nextLine();
        }

        return filePath;
    }

//    @Override
//    public void start(Stage primaryStage) {
//        primaryStage.setTitle("JavaFX App");
//
//        DirectoryChooser directoryChooser = new DirectoryChooser();
//        directoryChooser.setInitialDirectory(new File("src"));
//
//        Button button = new Button("Select Directory");
//        button.setOnAction(e -> {
//            File selectedDirectory = directoryChooser.showDialog(primaryStage);
//            try{
//                System.out.println(selectedDirectory.getAbsolutePath());
//            }catch (NullPointerException npe){
//                System.out.println("No Dir selected");
//            }
//        });
//
//
//        VBox vBox = new VBox(button);
//        //HBox hBox = new HBox(button1, button2);
//        Scene scene = new Scene(vBox, 960, 600);
//
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }
}