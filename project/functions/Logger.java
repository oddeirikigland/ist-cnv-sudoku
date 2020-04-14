package functions;

import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path; 
import java.nio.file.Paths;

public class Logger {
    public static synchronized void logToFile(String line) {
        try {
            PrintWriter log_writer = new PrintWriter(new BufferedWriter(new FileWriter(".log", true)));
            log_writer.println(line);
            log_writer.close();
        } catch (IOException e) {
            System.out.println("exception with log-writer instantiation");
        }
    }
}