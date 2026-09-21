package projectLabo;

import projectLabo.parser.*;
import projectLabo.parser.ast.Prog;
import java.io.StringReader;
import java.io.BufferedReader;

// Build with the other Java sources as described in README.md.
// Run after building: java -cp build/classes projectLabo.TestParser

public class TestParser {
    public static void main(String[] args) throws Exception {
        String input = "print [1]@[2]@[3]";
        try (ParserInterface parser = new Parser(new Tokenizer(new BufferedReader(new StringReader(input))))) {
            Prog prog = parser.parseProg();
            System.out.println(prog);
        }
    }
}
