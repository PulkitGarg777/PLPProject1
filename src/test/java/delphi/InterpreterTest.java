package delphi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

class InterpreterTest {
    @Test
    void executesClassAndMethods() throws IOException {
        String output = run("test1.pas", "");
        assertEquals("6\n", output);
    }

    @Test
    void executesInheritanceAndInterface() throws IOException {
        String output = run("test2.pas", "");
        assertEquals("1\n2\n", output);
    }

    @Test
    void executesReadln() throws IOException {
        String output = run("test3.pas", "41\n");
        assertEquals("42\n", output);
    }

    @Test
    void preventsPrivateFieldAccess() throws IOException {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> run("test4.pas", ""));
        assertEquals("Field not accessible: secret", ex.getMessage());
    }

    private String run(String fileName, String input) throws IOException {
        Path filePath = Paths.get("tests", fileName);
        DelphiLexer lexer = new DelphiLexer(CharStreams.fromPath(filePath));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DelphiParser parser = new DelphiParser(tokens);
        Interpreter interpreter = new Interpreter();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        interpreter.execute(parser.program(), new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))),
                new PrintStream(outputStream));
        return outputStream.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
