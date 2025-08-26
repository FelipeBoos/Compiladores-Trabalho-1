package br.com.trabalho.compilerui.io;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class TextFileIO {
    public static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }


    public static void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}