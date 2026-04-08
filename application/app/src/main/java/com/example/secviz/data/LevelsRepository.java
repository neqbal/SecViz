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
        String objdump1 =
                "vuln:     file format elf64-x86-64\n" +
                        "\n" +
                        "\n" +
                        "Disassembly of section .init:\n" +
                        "\n" +
                        "000000000040033c <_init>:\n" +
                        "  40033c:       f3 0f 1e fa             endbr64\n" +
                        "  400340:       48 83 ec 08             sub    rsp,0x8\n" +
                        "  400344:       48 8b 05 95 2c 00 00    mov    rax,QWORD PTR [rip+0x2c95]        # 402fe0 <__gmon_start__@Base>\n" +
                        "  40034b:       48 85 c0                test   rax,rax\n" +
                        "  40034e:       74 02                   je     400352 <_init+0x16>\n" +
                        "  400350:       ff d0                   call   rax\n" +
                        "  400352:       48 83 c4 08             add    rsp,0x8\n" +
                        "  400356:       c3                      ret\n" +
                        "\n" +
                        "Disassembly of section .plt:\n" +
                        "\n" +
                        "0000000000400360 <read@plt-0x10>:\n" +
                        "  400360:       ff 35 8a 2c 00 00       push   QWORD PTR [rip+0x2c8a]        # 402ff0 <_GLOBAL_OFFSET_TABLE_+0x8>\n" +
                        "  400366:       ff 25 8c 2c 00 00       jmp    QWORD PTR [rip+0x2c8c]        # 402ff8 <_GLOBAL_OFFSET_TABLE_+0x10>\n" +
                        "  40036c:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "\n" +
                        "0000000000400370 <read@plt>:\n" +
                        "  400370:       ff 25 8a 2c 00 00       jmp    QWORD PTR [rip+0x2c8a]        # 403000 <read@GLIBC_2.2.5>\n" +
                        "  400376:       68 00 00 00 00          push   0x0\n" +
                        "  40037b:       e9 e0 ff ff ff          jmp    400360 <_init+0x24>\n" +
                        "\n" +
                        "Disassembly of section .text:\n" +
                        "\n" +
                        "0000000000400380 <_start>:\n" +
                        "  400380:       f3 0f 1e fa             endbr64\n" +
                        "  400384:       31 ed                   xor    ebp,ebp\n" +
                        "  400386:       49 89 d1                mov    r9,rdx\n" +
                        "  400389:       5e                      pop    rsi\n" +
                        "  40038a:       48 89 e2                mov    rdx,rsp\n" +
                        "  40038d:       48 83 e4 f0             and    rsp,0xfffffffffffffff0\n" +
                        "  400391:       50                      push   rax\n" +
                        "  400392:       54                      push   rsp\n" +
                        "  400393:       45 31 c0                xor    r8d,r8d\n" +
                        "  400396:       31 c9                   xor    ecx,ecx\n" +
                        "  400398:       48 c7 c7 87 04 40 00    mov    rdi,0x400487\n" +
                        "  40039f:       ff 15 33 2c 00 00       call   QWORD PTR [rip+0x2c33]        # 402fd8 <__libc_start_main@GLIBC_2.34>\n" +
                        "  4003a5:       f4                      hlt\n" +
                        "  4003a6:       66 2e 0f 1f 84 00 00    cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  4003ad:       00 00 00\n" +
                        "\n" +
                        "00000000004003b0 <_dl_relocate_static_pie>:\n" +
                        "  4003b0:       f3 0f 1e fa             endbr64\n" +
                        "  4003b4:       c3                      ret\n" +
                        "  4003b5:       66 2e 0f 1f 84 00 00    cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  4003bc:       00 00 00\n" +
                        "  4003bf:       90                      nop\n" +
                        "\n" +
                        "00000000004003c0 <deregister_tm_clones>:\n" +
                        "  4003c0:       b8 10 30 40 00          mov    eax,0x403010\n" +
                        "  4003c5:       48 3d 10 30 40 00       cmp    rax,0x403010\n" +
                        "  4003cb:       74 13                   je     4003e0 <deregister_tm_clones+0x20>\n" +
                        "  4003cd:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  4003d2:       48 85 c0                test   rax,rax\n" +
                        "  4003d5:       74 09                   je     4003e0 <deregister_tm_clones+0x20>\n" +
                        "  4003d7:       bf 10 30 40 00          mov    edi,0x403010\n" +
                        "  4003dc:       ff e0                   jmp    rax\n" +
                        "  4003de:       66 90                   xchg   ax,ax\n" +
                        "  4003e0:       c3                      ret\n" +
                        "  4003e1:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "  4003e5:       66 66 2e 0f 1f 84 00    data16 cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  4003ec:       00 00 00 00\n" +
                        "\n" +
                        "00000000004003f0 <register_tm_clones>:\n" +
                        "  4003f0:       be 10 30 40 00          mov    esi,0x403010\n" +
                        "  4003f5:       48 81 ee 10 30 40 00    sub    rsi,0x403010\n" +
                        "  4003fc:       48 89 f0                mov    rax,rsi\n" +
                        "  4003ff:       48 c1 ee 3f             shr    rsi,0x3f\n" +
                        "  400403:       48 c1 f8 03             sar    rax,0x3\n" +
                        "  400407:       48 01 c6                add    rsi,rax\n" +
                        "  40040a:       48 d1 fe                sar    rsi,1\n" +
                        "  40040d:       74 11                   je     400420 <register_tm_clones+0x30>\n" +
                        "  40040f:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  400414:       48 85 c0                test   rax,rax\n" +
                        "  400417:       74 07                   je     400420 <register_tm_clones+0x30>\n" +
                        "  400419:       bf 10 30 40 00          mov    edi,0x403010\n" +
                        "  40041e:       ff e0                   jmp    rax\n" +
                        "  400420:       c3                      ret\n" +
                        "  400421:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "  400425:       66 66 2e 0f 1f 84 00    data16 cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  40042c:       00 00 00 00\n" +
                        "\n" +
                        "0000000000400430 <__do_global_dtors_aux>:\n" +
                        "  400430:       f3 0f 1e fa             endbr64\n" +
                        "  400434:       80 3d d1 2b 00 00 00    cmp    BYTE PTR [rip+0x2bd1],0x0        # 40300c <completed.0>\n" +
                        "  40043b:       75 13                   jne    400450 <__do_global_dtors_aux+0x20>\n" +
                        "  40043d:       55                      push   rbp\n" +
                        "  40043e:       48 89 e5                mov    rbp,rsp\n" +
                        "  400441:       e8 7a ff ff ff          call   4003c0 <deregister_tm_clones>\n" +
                        "  400446:       c6 05 bf 2b 00 00 01    mov    BYTE PTR [rip+0x2bbf],0x1        # 40300c <completed.0>\n" +
                        "  40044d:       5d                      pop    rbp\n" +
                        "  40044e:       c3                      ret\n" +
                        "  40044f:       90                      nop\n" +
                        "  400450:       c3                      ret\n" +
                        "  400451:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "  400455:       66 66 2e 0f 1f 84 00    data16 cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  40045c:       00 00 00 00\n" +
                        "\n" +
                        "0000000000400460 <frame_dummy>:\n" +
                        "  400460:       f3 0f 1e fa             endbr64\n" +
                        "  400464:       eb 8a                   jmp    4003f0 <register_tm_clones>\n" +
                        "\n" +
                        "0000000000400466 <vuln>:\n" +
                        "  400466:       55                      push   rbp\n" +
                        "  400467:       48 89 e5                mov    rbp,rsp\n" +
                        "  40046a:       48 83 ec 10             sub    rsp,0x10\n" +
                        "  40046e:       48 8d 45 f0             lea    rax,[rbp-0x10]\n" +
                        "  400472:       ba 64 00 00 00          mov    edx,0x64\n" +
                        "  400477:       48 89 c6                mov    rsi,rax\n" +
                        "  40047a:       bf 01 00 00 00          mov    edi,0x1\n" +
                        "  40047f:       e8 ec fe ff ff          call   400370 <read@plt>\n" +
                        "  400484:       90                      nop\n" +
                        "  400485:       c9                      leave\n" +
                        "  400486:       c3                      ret\n" +
                        "\n" +
                        "0000000000400487 <main>:\n" +
                        "  400487:       55                      push   rbp\n" +
                        "  400488:       48 89 e5                mov    rbp,rsp\n" +
                        "  40048b:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  400490:       e8 d1 ff ff ff          call   400466 <vuln>\n" +
                        "  400495:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  40049a:       5d                      pop    rbp\n" +
                        "  40049b:       c3                      ret\n" +
                        "\n" +
                        "Disassembly of section .fini:\n" +
                        "\n" +
                        "000000000040049c <_fini>:\n" +
                        "  40049c:       f3 0f 1e fa             endbr64\n" +
                        "  4004a0:       48 83 ec 08             sub    rsp,0x8\n" +
                        "  4004a4:       48 83 c4 08             add    rsp,0x8\n" +
                        "  4004a8:       c3                      ret\n";
        levels.add(new Level(
                "1", "Level 1: The Crash", "Can you trigger a Segmentation Fault?",
                "CRASH",
                "Crash Successful!",
                "You successfully overflowed the buffer and corrupted the Return Address, causing the program to jump to invalid memory and crash.",
                6, code1, stack1, presets1,
                new DefensePatch(
                        "    read(1, buff, 100); // User inputs > 24 bytes to crash",
                        "    read(1, buff, 16); // Safely read only 16 bytes max!"),
                objdump1));

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

        String objdump2a =
                "vuln:     file format elf64-x86-64\n" +
                        "\n" +
                        "\n" +
                        "Disassembly of section .init:\n" +
                        "\n" +
                        "000000000040033c <_init>:\n" +
                        "  40033c:       f3 0f 1e fa             endbr64\n" +
                        "  400340:       48 83 ec 08             sub    rsp,0x8\n" +
                        "  400344:       48 8b 05 95 2c 00 00    mov    rax,QWORD PTR [rip+0x2c95]        # 402fe0 <__gmon_start__@Base>\n" +
                        "  40034b:       48 85 c0                test   rax,rax\n" +
                        "  40034e:       74 02                   je     400352 <_init+0x16>\n" +
                        "  400350:       ff d0                   call   rax\n" +
                        "  400352:       48 83 c4 08             add    rsp,0x8\n" +
                        "  400356:       c3                      ret\n" +
                        "\n" +
                        "Disassembly of section .plt:\n" +
                        "\n" +
                        "0000000000400360 <puts@plt-0x10>:\n" +
                        "  400360:       ff 35 8a 2c 00 00       push   QWORD PTR [rip+0x2c8a]        # 402ff0 <_GLOBAL_OFFSET_TABLE_+0x8>\n" +
                        "  400366:       ff 25 8c 2c 00 00       jmp    QWORD PTR [rip+0x2c8c]        # 402ff8 <_GLOBAL_OFFSET_TABLE_+0x10>\n" +
                        "  40036c:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "\n" +
                        "0000000000400370 <puts@plt>:\n" +
                        "  400370:       ff 25 8a 2c 00 00       jmp    QWORD PTR [rip+0x2c8a]        # 403000 <puts@GLIBC_2.2.5>\n" +
                        "  400376:       68 00 00 00 00          push   0x0\n" +
                        "  40037b:       e9 e0 ff ff ff          jmp    400360 <_init+0x24>\n" +
                        "\n" +
                        "0000000000400380 <printf@plt>:\n" +
                        "  400380:       ff 25 82 2c 00 00       jmp    QWORD PTR [rip+0x2c82]        # 403008 <printf@GLIBC_2.2.5>\n" +
                        "  400386:       68 01 00 00 00          push   0x1\n" +
                        "  40038b:       e9 d0 ff ff ff          jmp    400360 <_init+0x24>\n" +
                        "\n" +
                        "0000000000400390 <read@plt>:\n" +
                        "  400390:       ff 25 7a 2c 00 00       jmp    QWORD PTR [rip+0x2c7a]        # 403010 <read@GLIBC_2.2.5>\n" +
                        "  400396:       68 02 00 00 00          push   0x2\n" +
                        "  40039b:       e9 c0 ff ff ff          jmp    400360 <_init+0x24>\n" +
                        "\n" +
                        "Disassembly of section .text:\n" +
                        "\n" +
                        "00000000004003a0 <_start>:\n" +
                        "  4003a0:       f3 0f 1e fa             endbr64\n" +
                        "  4003a4:       31 ed                   xor    ebp,ebp\n" +
                        "  4003a6:       49 89 d1                mov    r9,rdx\n" +
                        "  4003a9:       5e                      pop    rsi\n" +
                        "  4003aa:       48 89 e2                mov    rdx,rsp\n" +
                        "  4003ad:       48 83 e4 f0             and    rsp,0xfffffffffffffff0\n" +
                        "  4003b1:       50                      push   rax\n" +
                        "  4003b2:       54                      push   rsp\n" +
                        "  4003b3:       45 31 c0                xor    r8d,r8d\n" +
                        "  4003b6:       31 c9                   xor    ecx,ecx\n" +
                        "  4003b8:       48 c7 c7 e3 04 40 00    mov    rdi,0x4004e3\n" +
                        "  4003bf:       ff 15 13 2c 00 00       call   QWORD PTR [rip+0x2c13]        # 402fd8 <__libc_start_main@GLIBC_2.34>\n" +
                        "  4003c5:       f4                      hlt\n" +
                        "  4003c6:       66 2e 0f 1f 84 00 00    cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  4003cd:       00 00 00\n" +
                        "\n" +
                        "00000000004003d0 <_dl_relocate_static_pie>:\n" +
                        "  4003d0:       f3 0f 1e fa             endbr64\n" +
                        "  4003d4:       c3                      ret\n" +
                        "  4003d5:       66 2e 0f 1f 84 00 00    cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  4003dc:       00 00 00\n" +
                        "  4003df:       90                      nop\n" +
                        "\n" +
                        "00000000004003e0 <deregister_tm_clones>:\n" +
                        "  4003e0:       b8 20 30 40 00          mov    eax,0x403020\n" +
                        "  4003e5:       48 3d 20 30 40 00       cmp    rax,0x403020\n" +
                        "  4003eb:       74 13                   je     400400 <deregister_tm_clones+0x20>\n" +
                        "  4003ed:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  4003f2:       48 85 c0                test   rax,rax\n" +
                        "  4003f5:       74 09                   je     400400 <deregister_tm_clones+0x20>\n" +
                        "  4003f7:       bf 20 30 40 00          mov    edi,0x403020\n" +
                        "  4003fc:       ff e0                   jmp    rax\n" +
                        "  4003fe:       66 90                   xchg   ax,ax\n" +
                        "  400400:       c3                      ret\n" +
                        "  400401:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "  400405:       66 66 2e 0f 1f 84 00    data16 cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  40040c:       00 00 00 00\n" +
                        "\n" +
                        "0000000000400410 <register_tm_clones>:\n" +
                        "  400410:       be 20 30 40 00          mov    esi,0x403020\n" +
                        "  400415:       48 81 ee 20 30 40 00    sub    rsi,0x403020\n" +
                        "  40041c:       48 89 f0                mov    rax,rsi\n" +
                        "  40041f:       48 c1 ee 3f             shr    rsi,0x3f\n" +
                        "  400423:       48 c1 f8 03             sar    rax,0x3\n" +
                        "  400427:       48 01 c6                add    rsi,rax\n" +
                        "  40042a:       48 d1 fe                sar    rsi,1\n" +
                        "  40042d:       74 11                   je     400440 <register_tm_clones+0x30>\n" +
                        "  40042f:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  400434:       48 85 c0                test   rax,rax\n" +
                        "  400437:       74 07                   je     400440 <register_tm_clones+0x30>\n" +
                        "  400439:       bf 20 30 40 00          mov    edi,0x403020\n" +
                        "  40043e:       ff e0                   jmp    rax\n" +
                        "  400440:       c3                      ret\n" +
                        "  400441:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "  400445:       66 66 2e 0f 1f 84 00    data16 cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  40044c:       00 00 00 00\n" +
                        "\n" +
                        "0000000000400450 <__do_global_dtors_aux>:\n" +
                        "  400450:       f3 0f 1e fa             endbr64\n" +
                        "  400454:       80 3d c1 2b 00 00 00    cmp    BYTE PTR [rip+0x2bc1],0x0        # 40301c <completed.0>\n" +
                        "  40045b:       75 13                   jne    400470 <__do_global_dtors_aux+0x20>\n" +
                        "  40045d:       55                      push   rbp\n" +
                        "  40045e:       48 89 e5                mov    rbp,rsp\n" +
                        "  400461:       e8 7a ff ff ff          call   4003e0 <deregister_tm_clones>\n" +
                        "  400466:       c6 05 af 2b 00 00 01    mov    BYTE PTR [rip+0x2baf],0x1        # 40301c <completed.0>\n" +
                        "  40046d:       5d                      pop    rbp\n" +
                        "  40046e:       c3                      ret\n" +
                        "  40046f:       90                      nop\n" +
                        "  400470:       c3                      ret\n" +
                        "  400471:       0f 1f 40 00             nop    DWORD PTR [rax+0x0]\n" +
                        "  400475:       66 66 2e 0f 1f 84 00    data16 cs nop WORD PTR [rax+rax*1+0x0]\n" +
                        "  40047c:       00 00 00 00\n" +
                        "\n" +
                        "0000000000400480 <frame_dummy>:\n" +
                        "  400480:       f3 0f 1e fa             endbr64\n" +
                        "  400484:       eb 8a                   jmp    400410 <register_tm_clones>\n" +
                        "\n" +
                        "0000000000400486 <vuln>:\n" +
                        "  400486:       55                      push   rbp\n" +
                        "  400487:       48 89 e5                mov    rbp,rsp\n" +
                        "  40048a:       48 83 ec 20             sub    rsp,0x20\n" +
                        "  40048e:       48 b8 53 55 50 45 52    movabs rax,0x45535f5245505553\n" +
                        "  400495:       5f 53 45\n" +
                        "  400498:       48 ba 43 52 45 54 5f    movabs rdx,0x59454b5f54455243\n" +
                        "  40049f:       4b 45 59\n" +
                        "  4004a2:       48 89 45 f0             mov    QWORD PTR [rbp-0x10],rax\n" +
                        "  4004a6:       48 89 55 f8             mov    QWORD PTR [rbp-0x8],rdx\n" +
                        "  4004aa:       bf e8 11 40 00          mov    edi,0x4011e8\n" +
                        "  4004af:       e8 bc fe ff ff          call   400370 <puts@plt>\n" +
                        "  4004b4:       48 8d 45 e0             lea    rax,[rbp-0x20]\n" +
                        "  4004b8:       ba 10 00 00 00          mov    edx,0x10\n" +
                        "  4004bd:       48 89 c6                mov    rsi,rax\n" +
                        "  4004c0:       bf 01 00 00 00          mov    edi,0x1\n" +
                        "  4004c5:       e8 c6 fe ff ff          call   400390 <read@plt>\n" +
                        "  4004ca:       48 8d 45 e0             lea    rax,[rbp-0x20]\n" +
                        "  4004ce:       48 89 c6                mov    rsi,rax\n" +
                        "  4004d1:       bf fa 11 40 00          mov    edi,0x4011fa\n" +
                        "  4004d6:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  4004db:       e8 a0 fe ff ff          call   400380 <printf@plt>\n" +
                        "  4004e0:       90                      nop\n" +
                        "  4004e1:       c9                      leave\n" +
                        "  4004e2:       c3                      ret\n" +
                        "\n" +
                        "00000000004004e3 <main>:\n" +
                        "  4004e3:       55                      push   rbp\n" +
                        "  4004e4:       48 89 e5                mov    rbp,rsp\n" +
                        "  4004e7:       b8 00 00 00 00          mov    eax,0x0\n" +
                        "  4004ec:       e8 95 ff ff ff          call   400486 <vuln>\n" +
                        "  4004f1:       90                      nop\n" +
                        "  4004f2:       5d                      pop    rbp\n" +
                        "  4004f3:       c3                      ret\n" +
                        "\n" +
                        "Disassembly of section .fini:\n" +
                        "\n" +
                        "00000000004004f4 <_fini>:\n" +
                        "  4004f4:       f3 0f 1e fa             endbr64\n" +
                        "  4004f8:       48 83 ec 08             sub    rsp,0x8\n" +
                        "  4004fc:       48 83 c4 08             add    rsp,0x8\n" +
                        "  400500:       c3                      ret\n";

        levels.add(new Level(
                "2a", "Level 2A: Leaking Secrets", "Leak the secret by manipulating null-terminators!",
                "LEAK",
                "Secret Leaked!",
                "Since read() does not append a null byte, providing exactly 16 bytes bridged buff directly into secret_key. printf kept reading memory until it hit the null byte at the end of the secret, leaking it!",
                10, code2a, stack2a, presets2a,
                new DefensePatch(
                        "    read(1, buff, 16); // Safe length, but no null-terminator appended!",
                        "    read(1, buff, 15); buff[15] = '\\0'; // Null-terminator secures the secret!"),
                objdump2a));

        // ── LEVEL 2B: Control Flow Hijack ────────────────────────────────────
        List<CodeLine> code2b = new ArrayList<>();
        code2b.add(new CodeLine("#include <stdio.h>"));
        code2b.add(new CodeLine("void win() {",
                "0000000000400476 <win>:",
                "  400476:       push   rbp",
                "  400477:       48 89 e5        mov    rbp,rsp"));
        code2b.add(new CodeLine("    puts(\"SHELL OBTAINED — flag{ctrl_flow_h1jack}\");",
                "  40047a:       bf b0 11 40 00  mov    edi,0x4011b0",
                "  40047f:       e8 ec fe ff ff  call   400370 <puts@plt>"));
        code2b.add(new CodeLine("}",
                "  400484:       90              nop",
                "  400485:       5d              pop    rbp",
                "  400486:       c3              ret"));
        code2b.add(new CodeLine("void vuln() {",
                "0000000000400487 <vuln>:",
                "  400487:       55              push   rbp",
                "  400488:       48 89 e5        mov    rbp,rsp",
                "  40048b:       48 83 ec 20     sub    rsp,0x20"));
        code2b.add(new CodeLine("    char buf[32];"));
        code2b.add(new CodeLine("    gets(buf);          // no bounds check",
                "  40048f:       48 8d 45 e0     lea    rax,[rbp-0x20]",
                "  400493:       48 89 c7        mov    rdi,rax",
                "  400496:       e8 e5 fe ff ff  call   400380 <gets@plt>"));
        code2b.add(new CodeLine("}",
                "  40049b:       90              nop",
                "  40049c:       c9              leave",
                "  40049d:       c3              ret"));
        code2b.add(new CodeLine("int main() {",
                "000000000040049e <main>:",
                "  40049e:       55              push   rbp",
                "  40049f:       48 89 e5        mov    rbp,rsp"));
        code2b.add(new CodeLine("    vuln();",
                "  4004a2:       b8 00 00 00 00  mov    eax,0x0",
                "  4004a7:       e8 db ff ff ff  call   400487 <vuln>"));
        code2b.add(new CodeLine("}",
                "  4004ac:       b8 00 00 00 00  mov    eax,0x0",
                "  4004b1:       5d              pop    rbp",
                "  4004b2:       c3              ret"));

        // Stack at vuln() entry — index 0 = highest address (top of view)
        // The exploit: junk indices 7→3 (buf[0..7]→saved RBP = 40 bytes), target index 2 (vuln ret addr)
        List<StackBlock> stack2b = new ArrayList<>();
        stack2b.add(new StackBlock("0x7fff8", "main Saved RBP",    "0x7fff0", 8, StackBlock.TYPE_MAIN_FRAME));
        stack2b.add(new StackBlock("0x7fff0", "main Return Addr",  "0x7fc2a", 8, StackBlock.TYPE_MAIN_FRAME));
        stack2b.add(new StackBlock("0x7ffe8", "vuln Return Addr",  "0x4004ac", 8, StackBlock.TYPE_SAFE));
        stack2b.add(new StackBlock("0x7ffe0", "vuln Saved RBP",    "0x7fff8", 8, StackBlock.TYPE_SAFE));
        stack2b.add(new StackBlock("0x7ffd8", "buf[24..31]",       "0x0",    8, StackBlock.TYPE_NEUTRAL));
        stack2b.add(new StackBlock("0x7ffd0", "buf[16..23]",       "0x0",    8, StackBlock.TYPE_NEUTRAL));
        stack2b.add(new StackBlock("0x7ffc8", "buf[ 8..15]",       "0x0",    8, StackBlock.TYPE_NEUTRAL));
        stack2b.add(new StackBlock("0x7ffc0", "buf[ 0.. 7]",       "0x0",    8, StackBlock.TYPE_NEUTRAL));

        String objdump2b =
            "vuln:     file format elf64-x86-64\n\n" +
            "Disassembly of section .plt:\n\n" +
            "0000000000400370 <puts@plt>:\n" +
            "  400370:  ff 25 8a 2c 00 00  jmp    QWORD PTR [rip+0x2c8a]\n" +
            "  400376:  68 00 00 00 00     push   0x0\n" +
            "  40037b:  e9 e0 ff ff ff     jmp    400360 <_init+0x24>\n\n" +
            "0000000000400380 <gets@plt>:\n" +
            "  400380:  ff 25 82 2c 00 00  jmp    QWORD PTR [rip+0x2c82]\n" +
            "  400386:  68 01 00 00 00     push   0x1\n" +
            "  40038b:  e9 d0 ff ff ff     jmp    400360 <_init+0x24>\n\n" +
            "Disassembly of section .text:\n\n" +
            "0000000000400476 <win>:\n" +
            "  400476:  55                 push   rbp\n" +
            "  400477:  48 89 e5           mov    rbp,rsp\n" +
            "  40047a:  bf b0 11 40 00     mov    edi,0x4011b0\n" +
            "  40047f:  e8 ec fe ff ff     call   400370 <puts@plt>\n" +
            "  400484:  90                 nop\n" +
            "  400485:  5d                 pop    rbp\n" +
            "  400486:  c3                 ret\n\n" +
            "0000000000400487 <vuln>:\n" +
            "  400487:  55                 push   rbp\n" +
            "  400488:  48 89 e5           mov    rbp,rsp\n" +
            "  40048b:  48 83 ec 20        sub    rsp,0x20\n" +
            "  40048f:  48 8d 45 e0        lea    rax,[rbp-0x20]\n" +
            "  400493:  48 89 c7           mov    rdi,rax\n" +
            "  400496:  e8 e5 fe ff ff     call   400380 <gets@plt>\n" +
            "  40049b:  90                 nop\n" +
            "  40049c:  c9                 leave\n" +
            "  40049d:  c3                 ret\n\n" +
            "000000000040049e <main>:\n" +
            "  40049e:  55                 push   rbp\n" +
            "  40049f:  48 89 e5           mov    rbp,rsp\n" +
            "  4004a2:  b8 00 00 00 00     mov    eax,0x0\n" +
            "  4004a7:  e8 db ff ff ff     call   400487 <vuln>\n" +
            "  4004ac:  b8 00 00 00 00     mov    eax,0x0\n" +
            "  4004b1:  5d                 pop    rbp\n" +
            "  4004b2:  c3                 ret\n\n" +
            "Disassembly of section .fini:\n\n" +
            "00000000004004b4 <_fini>:\n" +
            "  4004b4:  f3 0f 1e fa        endbr64\n" +
            "  4004b8:  48 83 ec 08        sub    rsp,0x8\n" +
            "  4004bc:  48 83 c4 08        add    rsp,0x8\n" +
            "  4004c0:  c3                 ret\n";

        levels.add(new Level(
                "2b", "Level 2B: Control Flow Hijack",
                "Redirect execution to win() — no shellcode needed!",
                "CFH",
                "Shell Obtained!",
                "You overflowed buf (32 bytes) and saved RBP (8 bytes), then overwrote the return address with win()'s address (0x400476). When vuln() returned, RIP jumped directly to win().",
                8, code2b, stack2b, new ArrayList<>(),
                null,   // no defense patch for this level
                objdump2b));

        // ── LEVEL 3: Return-Oriented Programming (ROP) — real binary ─────────
        List<CodeLine> code3 = new ArrayList<>();
        code3.add(new CodeLine("#include <stdio.h>"));
        code3.add(new CodeLine("#include <unistd.h>"));
        code3.add(new CodeLine(""));
        code3.add(new CodeLine("char *binsh = \"/bin/sh\\00\";",
                "0000000000403010 g  O .data  0000000000000008  binsh",
                "// binsh is a REAL pointer in the .data section — address 0x403010",
                "// Use this address as RDI when calling execve"));
        code3.add(new CodeLine(""));
        code3.add(new CodeLine("void gadget_pop_rax() { __asm__(\"pop %rax; ret\"); }",
                "0000000000400466 <gadget_pop_rax>:",
                "  400466:  55                      push   rbp",
                "  400467:  48 89 e5                mov    rbp,rsp",
                "  40046a:  58                      pop    rax   ← 0x40046a",
                "  40046b:  c3                      ret"));
        code3.add(new CodeLine("void gadget_pop_rdi() { __asm__(\"pop %rdi; ret\"); }",
                "000000000040046f <gadget_pop_rdi>:",
                "  40046f:  55                      push   rbp",
                "  400470:  48 89 e5                mov    rbp,rsp",
                "  400473:  5f                      pop    rdi   ← 0x400473",
                "  400474:  c3                      ret"));
        code3.add(new CodeLine("void gadget_pop_rsi() { __asm__(\"pop %rsi; ret\"); }",
                "0000000000400478 <gadget_pop_rsi>:",
                "  400478:  55                      push   rbp",
                "  400479:  48 89 e5                mov    rbp,rsp",
                "  40047c:  5e                      pop    rsi   ← 0x40047c",
                "  40047d:  c3                      ret"));
        code3.add(new CodeLine("void gadget_pop_rdx() { __asm__(\"pop %rdx; ret\"); }",
                "0000000000400481 <gadget_pop_rdx>:",
                "  400481:  55                      push   rbp",
                "  400482:  48 89 e5                mov    rbp,rsp",
                "  400485:  5a                      pop    rdx   ← 0x400485",
                "  400486:  c3                      ret"));
        code3.add(new CodeLine("void gadget_syscall() { __asm__(\"syscall; ret\"); }",
                "000000000040048a <gadget_syscall>:",
                "  40048a:  55                      push   rbp",
                "  40048b:  48 89 e5                mov    rbp,rsp",
                "  40048e:  0f 05                   syscall      ← 0x40048e",
                "  400490:  c3                      ret"));
        code3.add(new CodeLine(""));
        code3.add(new CodeLine("void vuln() {",
                "0000000000400494 <vuln>:",
                "  400494:  55                      push   rbp",
                "  400495:  48 89 e5                mov    rbp,rsp",
                "  400498:  48 83 ec 20             sub    rsp,0x20  ← buf[32]"));
        code3.add(new CodeLine("    char buf[32];"));
        code3.add(new CodeLine("    gets(buf);",             // highlighted line index 13
                "  40049c:  48 8d 45 e0             lea    rax,[rbp-0x20]",
                "  4004a0:  48 89 c7                mov    rdi,rax",
                "  4004a3:  e8 c8 fe ff ff          call   400370 <gets@plt>"));
        code3.add(new CodeLine("}",
                "  4004a8:  90                      nop",
                "  4004a9:  c9                      leave",
                "  4004aa:  c3                      ret"));
        code3.add(new CodeLine(""));
        code3.add(new CodeLine("int main() {",
                "00000000004004ab <main>:",
                "  4004ab:  55                      push   rbp",
                "  4004ac:  48 89 e5                mov    rbp,rsp"));
        code3.add(new CodeLine("    vuln();",
                "  4004af:  b8 00 00 00 00          mov    eax,0x0",
                "  4004b4:  e8 db ff ff ff          call   400494 <vuln>",
                "  4004b9:  b8 00 00 00 00          mov    eax,0x0  ← ret addr pushed here"));
        code3.add(new CodeLine("    return 0;",
                "  4004b9:  b8 00 00 00 00          mov    eax,0x0",
                "  4004be:  5d                      pop    rbp",
                "  4004bf:  c3                      ret"));
        code3.add(new CodeLine("}"));

        List<StackBlock> stack3 = new ArrayList<>();
        stack3.add(new StackBlock("0x7fff8", "main Saved RBP",  "0x7ffff0", 8, StackBlock.TYPE_MAIN_FRAME));
        stack3.add(new StackBlock("0x7fff0", "main Ret Addr",   "0x7fc2a",  8, StackBlock.TYPE_MAIN_FRAME));
        stack3.add(new StackBlock("0x7ffe8", "vuln Ret Addr",   "0x4004b9", 8, StackBlock.TYPE_SAFE));
        stack3.add(new StackBlock("0x7ffe0", "vuln Saved RBP",  "0x7fff8",  8, StackBlock.TYPE_SAFE));
        stack3.add(new StackBlock("0x7ffd8", "buf[24..31]",     "0x0",      8, StackBlock.TYPE_NEUTRAL));
        stack3.add(new StackBlock("0x7ffd0", "buf[16..23]",     "0x0",      8, StackBlock.TYPE_NEUTRAL));
        stack3.add(new StackBlock("0x7ffc8", "buf[ 8..15]",     "0x0",      8, StackBlock.TYPE_NEUTRAL));
        stack3.add(new StackBlock("0x7ffc0", "buf[ 0.. 7]",     "0x0",      8, StackBlock.TYPE_NEUTRAL));

        // All 5 gadgets are REAL addresses from the compiled binary
        List<RopGadget> gadgets3 = new ArrayList<>();
        gadgets3.add(new RopGadget("0x40046a", "pop rax ; ret",
                "Found at 0x40046a inside gadget_pop_rax().\n" +
                "Pops the top of the stack into RAX — the syscall number register.\n" +
                "In execve(\"/bin/sh\",0,0), RAX must be 59 (0x3b).",
                "pop_rax"));
        gadgets3.add(new RopGadget("0x400473", "pop rdi ; ret",
                "Found at 0x400473 inside gadget_pop_rdi().\n" +
                "Pops the top of the stack into RDI — the first syscall argument.\n" +
                "In execve(\"/bin/sh\",0,0), RDI must point to the /bin/sh string.\n" +
                "Use address 0x403010 (the address of the global 'binsh' variable).",
                "pop_rdi"));
        gadgets3.add(new RopGadget("0x40047c", "pop rsi ; ret",
                "Found at 0x40047c inside gadget_pop_rsi().\n" +
                "Pops the top of the stack into RSI — the second syscall argument.\n" +
                "In execve(\"/bin/sh\",0,0), RSI (argv) must be NULL (0).",
                "pop_rsi"));
        gadgets3.add(new RopGadget("0x400485", "pop rdx ; ret",
                "Found at 0x400485 inside gadget_pop_rdx().\n" +
                "Pops the top of the stack into RDX — the third syscall argument.\n" +
                "In execve(\"/bin/sh\",0,0), RDX (envp) must be NULL (0).",
                "pop_rdx"));
        gadgets3.add(new RopGadget("0x40048e", "syscall",
                "Found at 0x40048e inside gadget_syscall().\n" +
                "Executes a Linux kernel system call using current register values:\n" +
                "  RAX = syscall number  (59 = execve)\n" +
                "  RDI = 1st argument    (pointer to \"/bin/sh\")\n" +
                "  RSI = 2nd argument    (NULL)\n" +
                "  RDX = 3rd argument    (NULL)",
                "syscall"));
        gadgets3.add(new RopGadget("0x403010", "\"/bin/sh\"",
                "REAL address: global char *binsh = \"/bin/sh\" in .data section.\n" +
                "This is a pointer that lives at 0x403010. The bytes at that address\n" +
                "spell out '/','b','i','n','/','s','h','\\0'.\n" +
                "Set RDI = 0x403010 so execve receives the path argument.",
                "/bin/sh"));

        List<String[]> presets3 = new ArrayList<>();
        presets3.add(new String[]{"NX Demo (Shellcode → SIGSEGV)",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\\x90\\x90\\x90\\x31\\xc0\\x50"});

        String objdump3 =
            "vuln:     file format elf64-x86-64\n\n" +
            "── .data section ────────────────────────────────────────────────\n" +
            "0000000000403010 g  O .data  0000000000000008  binsh\n" +
            "  → points to '/bin/sh' string  (use 0x403010 as RDI)\n\n" +
            "── key gadgets (from ROPgadget) ──────────────────────────────────\n" +
            "0x40046a : pop rax ; ret\n" +
            "0x400473 : pop rdi ; ret\n" +
            "0x40047c : pop rsi ; ret\n" +
            "0x400485 : pop rdx ; ret\n" +
            "0x40048e : syscall\n\n" +
            "── disassembly ───────────────────────────────────────────────────\n" +
            "0000000000400466 <gadget_pop_rax>:\n" +
            "  400466:  55           push   rbp\n" +
            "  400467:  48 89 e5     mov    rbp,rsp\n" +
            "  40046a:  58           pop    rax\n" +
            "  40046b:  c3           ret\n\n" +
            "000000000040046f <gadget_pop_rdi>:\n" +
            "  40046f:  55           push   rbp\n" +
            "  400470:  48 89 e5     mov    rbp,rsp\n" +
            "  400473:  5f           pop    rdi\n" +
            "  400474:  c3           ret\n\n" +
            "0000000000400478 <gadget_pop_rsi>:\n" +
            "  400478:  55           push   rbp\n" +
            "  400479:  48 89 e5     mov    rbp,rsp\n" +
            "  40047c:  5e           pop    rsi\n" +
            "  40047d:  c3           ret\n\n" +
            "0000000000400481 <gadget_pop_rdx>:\n" +
            "  400481:  55           push   rbp\n" +
            "  400482:  48 89 e5     mov    rbp,rsp\n" +
            "  400485:  5a           pop    rdx\n" +
            "  400486:  c3           ret\n\n" +
            "000000000040048a <gadget_syscall>:\n" +
            "  40048a:  55           push   rbp\n" +
            "  40048b:  48 89 e5     mov    rbp,rsp\n" +
            "  40048e:  0f 05        syscall\n" +
            "  400490:  c3           ret\n\n" +
            "0000000000400494 <vuln>:\n" +
            "  400494:  55           push   rbp\n" +
            "  400495:  48 89 e5     mov    rbp,rsp\n" +
            "  400498:  48 83 ec 20  sub    rsp,0x20\n" +
            "  40049c:  48 8d 45 e0  lea    rax,[rbp-0x20]\n" +
            "  4004a0:  48 89 c7     mov    rdi,rax\n" +
            "  4004a3:  e8 c8 fe ff  call   400370 <gets@plt>\n" +
            "  4004a8:  90           nop\n" +
            "  4004a9:  c9           leave\n" +
            "  4004aa:  c3           ret\n\n" +
            "00000000004004ab <main>:\n" +
            "  4004ab:  55           push   rbp\n" +
            "  4004ac:  48 89 e5     mov    rbp,rsp\n" +
            "  4004af:  b8 00 00 00  mov    eax,0x0\n" +
            "  4004b4:  e8 db ff ff  call   400494 <vuln>\n" +
            "  4004b9:  b8 00 00 00  mov    eax,0x0   ← saved return address\n" +
            "  4004be:  5d           pop    rbp\n" +
            "  4004bf:  c3           ret\n\n" +
            "── exploit goal ──────────────────────────────────────────────────\n" +
            "  Overflow buf[32] + saved_rbp (8) → overwrite ret addr = 0x4004b9\n" +
            "  Chain:\n" +
            "    pop_rax  → 59          (execve syscall number)\n" +
            "    pop_rdi  → 0x403010    (pointer to \"/bin/sh\" in .data)\n" +
            "    pop_rsi  → 0           (argv = NULL)\n" +
            "    pop_rdx  → 0           (envp = NULL)\n" +
            "    syscall\n";

        levels.add(new Level(
                "3", "Level 3: Return-Oriented Programming",
                "NX is enabled — chain gadgets to bypass it and spawn a shell.",
                "ROP",
                "Shell Obtained!",
                "You bypassed NX by chaining 5 real gadgets from the binary:\n" +
                "  pop_rax → 59  |  pop_rdi → 0x403010 (/bin/sh)  |  pop_rsi → 0  |  pop_rdx → 0  |  syscall\n\n" +
                "No new code was injected. Every byte you executed already lived in the binary. " +
                "NX only prevents running *new* code on the stack — ROP reuses *existing* code " +
                "in executable (.text) sections, completely bypassing the protection.",
                16, code3, stack3, presets3, null, objdump3, gadgets3));


        return levels;
    }
}
