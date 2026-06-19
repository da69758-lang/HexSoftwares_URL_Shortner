URL Shortener — Pure JavaFX (single file)

Everything — the window, the layout, and the styling — is plain JavaFX
code in one Java file (URLShortener.java). No external CSS file,
no extra classes, no packages.

What it does


You paste a long URL and click Shorten.
The app generates a 6-character short code and shows you a short link
like http://localhost:8080/aB3xQ9.
That short link is real — a tiny embedded HTTP server
(com.sun.net.httpserver.HttpServer, part of the JDK, not an extra
library) listens on port 8080 and sends a genuine HTTP 302 redirect
back to your original long URL. Click the short link in the app and
it opens your browser, which gets redirected to the original page.
All mappings live in memory for as long as the app is running.


Project structure

url-shortener-javafx/
├── pom.xml                              # only used for "Option A" below
└── src/main/java/URLShortener.java      # the entire program — one file

How to run

Option A — Maven (easiest, downloads JavaFX for you)

Requires JDK 17+ and Maven 3.6+.

bashcd url-shortener-javafx
mvn clean javafx:run

Option B — Plain javac/java (no Maven, just the JavaFX SDK)


Download the JavaFX SDK for your OS from https://gluonhq.com/products/javafx/
and unzip it somewhere, e.g. ~/javafx-sdk-21.
Compile and run:


bashcd url-shortener-javafx/src/main/java

javac --module-path ~/javafx-sdk-21/lib --add-modules javafx.controls URLShortener.java

java --module-path ~/javafx-sdk-21/lib --add-modules javafx.controls URLShortener

That's it — no build tool required at all.

Notes / things you can extend


Port 8080 is hardcoded (the PORT constant) — change it if that
port is already in use on your machine.
The short URL → long URL map is in-memory only (ConcurrentHashMap).
Restarting the app clears it.
Each click on "Shorten" always creates a new code, even for a URL
you've already shortened. Add a reverse lookup if you want duplicate
long URLs to reuse the same short code.
