package com.example.secviz.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelsRepository {

    private static List<Level> LEVELS;

    public static List<Level> getLevels() {
        if (LEVELS == null) {
            LEVELS = buildLevels();
        }
        return LEVELS;
    }

    private static List<Level> buildLevels() {
        List<Level> levels = new ArrayList<>();

        // ── LEVEL 1: The Crash ──────────────────────────────────────────────
        List<CodeLine> code1 = new ArrayList<>();
        code1.add(new CodeLine("#include <stdio.h>"));
        code1.add(new CodeLine("#include <unistd.h>"));
        code1.add(new CodeLine("void vuln() {"));
        code1.add(new CodeLine("    char buff[16];",
                "0000000000400466 <vuln>:",
                "  400466:       push   rbp",
                "  400467:       mov    rbp,rsp",
                "  40046a:       sub    rsp,0x10"));
        code1.add(new CodeLine("    read(1, buff, 100); // User inputs > 24 bytes to crash",
                "  40046e:       lea    rax,[rbp-0x10]",
                "  400472:       mov    edx,0x64",
                "  400477:       mov    rsi,rax",
                "  40047a:       mov    edi,0x1",
                "  40047f:       call   400370 <read@plt>"));
        code1.add(new CodeLine("}",
                "  400484:       nop",
                "  400485:       leave",
                "  400486:       ret"));
        code1.add(new CodeLine("int main() {",
                "0000000000400487 <main>:",
                "  400487:       push   rbp",
                "  400488:       mov    rbp,rsp"));
        code1.add(new CodeLine("    vuln();",
                "  40048b:       call   400466 <vuln>"));
        code1.add(new CodeLine("}",
                "  400490:       mov    eax,0x0",
                "  400495:       pop    rbp",
                "  400496:       ret"));

        List<StackBlock> stack1 = new ArrayList<>();
        stack1.add(new StackBlock("0x7fff4", "main Frame Data", "0x...", 8, StackBlock.TYPE_MAIN_FRAME));
        stack1.add(new StackBlock("0x7ffec", "main Return Addr", "0x7fa2b", 8, StackBlock.TYPE_MAIN_FRAME));
        stack1.add(new StackBlock("0x7ffe4", "main Saved EBP", "0x7fff0", 8, StackBlock.TYPE_MAIN_FRAME));
        stack1.add(new StackBlock("0x7ffdc", "vuln Return Addr", "0x00000", 8, StackBlock.TYPE_SAFE));
        stack1.add(new StackBlock("0x7ffd4", "vuln Saved EBP", "0x00000", 8, StackBlock.TYPE_SAFE));
        stack1.add(new StackBlock("0x7ffcc", "buff[8..15]", "0x0", 8, StackBlock.TYPE_NEUTRAL));
        stack1.add(new StackBlock("0x7ffc4", "buff[0..7]", "0x0", 8, StackBlock.TYPE_NEUTRAL));

        List<String[]> presets1 = new ArrayList<>();
        presets1.add(new String[]{"Safe", "Hello!"});
        presets1.add(new String[]{"Crash", "AAAAAAAAAAAAAAAA++++++++BBBBBBBB"});

        levels.add(new Level(
                "1", "Level 1: The Crash", "Can you trigger a Segmentation Fault?",
                "CRASH",
                "Crash Successful!",
                "You successfully overflowed the buffer and corrupted the Return Address, causing the program to jump to invalid memory and crash.",
                6, code1, stack1, presets1,
                new DefensePatch(
                        "    read(1, buff, 100); // User inputs > 24 bytes to crash",
                        "    read(1, buff, 16); // Safely read only 16 bytes max!")));

        // ── LEVEL 2A: Leaking Secrets ────────────────────────────────────────
        List<CodeLine> code2a = new ArrayList<>();
        code2a.add(new CodeLine("#include <stdio.h>"));
        code2a.add(new CodeLine("#include <unistd.h>"));
        code2a.add(new CodeLine("void vuln() {"));
        code2a.add(new CodeLine("    // Compiler places secret right above buff in the stack"));
        code2a.add(new CodeLine("    char secret_key[16] = \"SUPER_SECRET_KEY\";",
                "0000000000400486 <vuln>:",
                "  400486:       push   rbp",
                "  400487:       mov    rbp,rsp",
                "  40048a:       sub    rsp,0x20",
                "  40048e:       movabs rax,0x45535f5245505553",
                "  400498:       movabs rdx,0x59454b5f54455243",
                "  4004a2:       mov    QWORD PTR [rbp-0x10],rax",
                "  4004a6:       mov    QWORD PTR [rbp-0x8],rdx"));
        code2a.add(new CodeLine("    char buff[16];"));
        code2a.add(new CodeLine("    puts(\"Enter your name:\");",
                "  4004aa:       mov    edi,0x4011e8",
                "  4004af:       call   400370 <puts@plt>"));
        code2a.add(new CodeLine("    read(1, buff, 16); // Safe length, but no null-terminator appended!",
                "  4004b4:       lea    rax,[rbp-0x20]",
                "  4004b8:       mov    edx,0x10",
                "  4004bd:       mov    rsi,rax",
                "  4004c0:       mov    edi,0x1",
                "  4004c5:       call   400390 <read@plt>"));
        code2a.add(new CodeLine("    printf(\"Hello %s\\n\", buff); // Will print the name AND the secret key",
                "  4004ca:       lea    rax,[rbp-0x20]",
                "  4004ce:       mov    rsi,rax",
                "  4004d1:       mov    edi,0x4011f9",
                "  4004d6:       mov    eax,0x0",
                "  4004db:       call   400380 <printf@plt>"));
        code2a.add(new CodeLine("}",
                "  4004e0:       nop",
                "  4004e1:       leave",
                "  4004e2:       ret"));
        code2a.add(new CodeLine("int main() {",
                "00000000004004e3 <main>:",
                "  4004e3:       push   rbp",
                "  4004e4:       mov    rbp,rsp"));
        code2a.add(new CodeLine("    vuln();",
                "  4004e7:       call   400486 <vuln>"));
        code2a.add(new CodeLine("}",
                "  4004ec:       mov    eax,0x0",
                "  4004f1:       pop    rbp",
                "  4004f2:       ret"));

        List<StackBlock> stack2a = new ArrayList<>();
        stack2a.add(new StackBlock("0x7fff4", "main Saved EBP", "0x7fff0", 8, StackBlock.TYPE_MAIN_FRAME));
        stack2a.add(new StackBlock("0x7ffec", "vuln Return Addr", "0x00000", 8, StackBlock.TYPE_SAFE));
        stack2a.add(new StackBlock("0x7ffe4", "vuln Saved EBP", "0x00000", 8, StackBlock.TYPE_SAFE));
        stack2a.add(new StackBlock("0x7ffdc", "secret_key[8..15]", "0x0", 8, StackBlock.TYPE_WARN));
        stack2a.add(new StackBlock("0x7ffd4", "secret_key[0..7]", "0x0", 8, StackBlock.TYPE_WARN));
        stack2a.add(new StackBlock("0x7ffcc", "buff[8..15]", "0x0", 8, StackBlock.TYPE_NEUTRAL));
        stack2a.add(new StackBlock("0x7ffc4", "buff[0..7]", "0x0", 8, StackBlock.TYPE_NEUTRAL));

        List<String[]> presets2a = new ArrayList<>();
        presets2a.add(new String[]{"Safe", "Hi!"});
        presets2a.add(new String[]{"Leak Secret", "AAAAAAAAAAAAAAAA"});

        levels.add(new Level(
                "2a", "Level 2A: Leaking Secrets", "Leak the secret by manipulating null-terminators!",
                "LEAK",
                "Secret Leaked!",
                "Since read() does not append a null byte, providing exactly 16 bytes bridged buff directly into secret_key. printf kept reading memory until it hit the null byte at the end of the secret, leaking it!",
                10, code2a, stack2a, presets2a,
                new DefensePatch(
                        "    read(1, buff, 16); // Safe length, but no null-terminator appended!",
                        "    read(1, buff, 15); buff[15] = '\\0'; // Null-terminator secures the secret!")));

        return levels;
    }
}
