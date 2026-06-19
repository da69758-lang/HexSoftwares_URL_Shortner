package url;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class urlshortner extends Application {

    private static final int PORT = 8080;
    private static final String BASE62 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();
    private static final int CODE_LENGTH = 6;

    private final Map<String, String> urlStore = new ConcurrentHashMap<>();
    private HttpServer server;

    // ---- Inline style strings (no external CSS file) ----
    private static final String ROOT_STYLE =
            "-fx-background-color: #12112a;";

    private static final String CARD_STYLE =
            "-fx-background-color: linear-gradient(to bottom, #221f40, #1c1a36);" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: #3a5cff;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(58,92,255,0.65), 45, 0.45, 0, 0);";

    private static final String TITLE_STYLE =
            "-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold;";

    private static final String SUBTITLE_STYLE =
            "-fx-text-fill: #b3b3da; -fx-font-size: 16px; -fx-font-weight: bold;";

    private static final String FIELD_STYLE =
            "-fx-background-color: #34315c;" +
            "-fx-background-radius: 10;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #8a87ad;" +
            "-fx-padding: 12 16;" +
            "-fx-font-size: 14px;" +
            "-fx-highlight-fill: #3a5cff;" +
            "-fx-highlight-text-fill: white;";

    private static final String FIELD_FOCUSED_STYLE =
            FIELD_STYLE + "-fx-background-color: #3c3968;" +
            "-fx-effect: dropshadow(gaussian, rgba(58,92,255,0.5), 10, 0.3, 0, 0);";

    private static final String BUTTON_STYLE =
            "-fx-background-color: #a6f51b;" +
            "-fx-background-radius: 10;" +
            "-fx-text-fill: #14121f;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;" +
            "-fx-padding: 12 36;" +
            "-fx-cursor: hand;";

    private static final String BUTTON_HOVER_STYLE =
            BUTTON_STYLE.replace("#a6f51b", "#b9ff3a");

    private static final String RESULT_BOX_STYLE =
            "-fx-background-color: #20274a;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 16;" +
            "-fx-border-color: #3a5cff;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;";

    private static final String RESULT_LINK_STYLE =
            "-fx-text-fill: #7fffb0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-underline: true;";

    private static final String RESULT_HINT_STYLE =
            "-fx-text-fill: #8a87ad; -fx-font-size: 11px;";

    private static final String ERROR_STYLE =
            "-fx-text-fill: #ff6b6b; -fx-font-size: 12px;";

    @Override
    public void start(Stage stage) {
        startRedirectServer();

        // ---- Title & subtitle ----
        Label title = new Label("URL Shortener");
        title.setStyle(TITLE_STYLE);

        Label subtitle = new Label("Enter Long URL:");
        subtitle.setStyle(SUBTITLE_STYLE);

        // ---- Input field ----
        TextField urlField = new TextField();
        urlField.setPromptText("https://example.com/very/long/path...");
        urlField.setPrefWidth(460);
        urlField.setPrefHeight(46);
        urlField.setStyle(FIELD_STYLE);
        urlField.focusedProperty().addListener((obs, wasFocused, isFocused) ->
                urlField.setStyle(isFocused ? FIELD_FOCUSED_STYLE : FIELD_STYLE));

        // ---- Shorten button ----
        Button shortenBtn = new Button("Shorten");
        shortenBtn.setStyle(BUTTON_STYLE);
        shortenBtn.setOnMouseEntered(e -> shortenBtn.setStyle(BUTTON_HOVER_STYLE));
        shortenBtn.setOnMouseExited(e -> shortenBtn.setStyle(BUTTON_STYLE));

        // ---- Error label (hidden until needed) ----
        Label errorLabel = new Label();
        errorLabel.setStyle(ERROR_STYLE);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // ---- Result area (hidden until a URL has been shortened) ----
        Label resultLink = new Label();
        resultLink.setStyle(RESULT_LINK_STYLE);
        resultLink.setCursor(Cursor.HAND);
        resultLink.setWrapText(true);

        Label resultHint = new Label("Click to open \u2014 it will redirect to your original URL");
        resultHint.setStyle(RESULT_HINT_STYLE);

        VBox resultBox = new VBox(4, resultLink, resultHint);
        resultBox.setStyle(RESULT_BOX_STYLE);
        resultBox.setAlignment(Pos.CENTER_LEFT);
        resultBox.setMaxWidth(460);
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        shortenBtn.setOnAction(e -> {
            String rawInput = urlField.getText() == null ? "" : urlField.getText().trim();

            if (rawInput.isEmpty()) {
                errorLabel.setText("Please enter a URL to shorten.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                resultBox.setVisible(false);
                resultBox.setManaged(false);
                return;
            }

            String normalized = normalizeUrl(rawInput);
            String code = generateUniqueCode();
            urlStore.put(code, normalized);

            String shortUrl = "http://localhost:" + PORT + "/" + code;
            resultLink.setText(shortUrl);
            resultBox.setVisible(true);
            resultBox.setManaged(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            resultLink.setOnMouseClicked(ev -> openInBrowser(shortUrl));
        });

        // Allow pressing Enter in the text field to trigger shortening too
        urlField.setOnAction(e -> shortenBtn.fire());

        // ---- Card ----
        VBox card = new VBox(18, title, subtitle, urlField, errorLabel, shortenBtn, resultBox);
        card.setStyle(CARD_STYLE);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(560);
        card.setPadding(new Insets(45, 50, 45, 50));

        // ---- Root ----
        StackPane root = new StackPane(card);
        root.setStyle(ROOT_STYLE);
        root.setPadding(new Insets(60));

        Scene scene = new Scene(root, 900, 560);

        stage.setTitle("URL Shortener - JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    // ---------------------------------------------------------------
    // Embedded HTTP server: handles the actual "shorten and redirect"
    // ---------------------------------------------------------------

    private void startRedirectServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", this::handleRedirect);
            server.setExecutor(null);
            server.start();
        } catch (IOException ex) {
            Platform.runLater(() ->
                    System.err.println("Could not start local redirect server on port "
                            + PORT + ": " + ex.getMessage()));
        }
    }

    private void handleRedirect(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String code = path.length() > 1 ? path.substring(1) : "";
        String target = urlStore.get(code);

        if (target != null) {
            exchange.getResponseHeaders().add("Location", target);
            exchange.sendResponseHeaders(302, -1);
        } else {
            byte[] body = "404 - Short URL not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
        exchange.close();
    }

    private void stopRedirectServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
            }
            code = sb.toString();
        } while (urlStore.containsKey(code));
        return code;
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            System.err.println("Could not open browser: " + ex.getMessage());
        }
    }

    @Override
    public void stop() {
        stopRedirectServer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}