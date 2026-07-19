package search;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {
    private static final String LOG_FILE = "engine.log";

    public static synchronized void clear() {
        try (FileWriter fw = new FileWriter(LOG_FILE, false)) {
            fw.write(""); // Overwrites to empty
        } catch (IOException e) {
            // Ignore logging errors
        }
    }

    public static synchronized void log(String msg) {
        // Output to UCI GUI console
        System.out.println(msg);
        System.out.flush();

        // Write to log file
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(msg);
        } catch (IOException e) {
            // Ignore logging errors
        }
    }
}
