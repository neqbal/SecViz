export const LEVELS = [
    {
        id: '1',
        title: 'Level 1: The Crash',
        subtitle: 'Can you trigger a Segmentation Fault?',
        goal: 'CRASH',
        successTitle: 'Crash Successful!',
        successDesc: 'You successfully overflowed the buffer and corrupted the Return Address, causing the program to jump to invalid memory and crash. In the next level, we will learn how to weaponize this memory boundary issue.',
        defensePatch: {
            vulnText: "    read(1, buff, 100); // User inputs > 24 bytes to crash",
            safeText: "    read(1, buff, 16); // Safely read only 16 bytes max!"
        },
        startCodeLine: 6,
        code: [
            { text: "#include <stdio.h>", asm: [] },
            { text: "#include <unistd.h>", asm: [] },
            { text: "void vuln() {", asm: [] },
            { text: "    char buff[16];", asm: ["0000000000400466 <vuln>:", "  400466:       push   rbp", "  400467:       mov    rbp,rsp", "  40046a:       sub    rsp,0x10"] },
            { text: "    read(1, buff, 100); // User inputs > 24 bytes to crash", asm: ["  40046e:       lea    rax,[rbp-0x10]", "  400472:       mov    edx,0x64", "  400477:       mov    rsi,rax", "  40047a:       mov    edi,0x1", "  40047f:       call   400370 <read@plt>"] },
            { text: "}", asm: ["  400484:       nop", "  400485:       leave", "  400486:       ret"] },
            { text: "int main() {", asm: ["0000000000400487 <main>:", "  400487:       push   rbp", "  400488:       mov    rbp,rsp"] },
            { text: "    vuln();", asm: ["  40048b:       call   400466 <vuln>"] },
            { text: "}", asm: ["  400490:       mov    eax,0x0", "  400495:       pop    rbp", "  400496:       ret"] }
        ],
        initialStack: [
            { id: 7, address: "0x7fff4", label: "main Frame Data", value: "0x...", size: 8, type: "main-frame" },
            { id: 6, address: "0x7ffec", label: "main Return Addr", value: "0x7fa2b", size: 8, type: "main-frame" },
            { id: 5, address: "0x7ffe4", label: "main Saved EBP", value: "0x7fff0", size: 8, type: "main-frame" },
            { id: 4, address: "0x7ffdc", label: "vuln Return Addr", value: "0x00000", size: 8, type: "safe" },
            { id: 3, address: "0x7ffd4", label: "vuln Saved EBP", value: "0x00000", size: 8, type: "safe" },
            { id: 2, address: "0x7ffcc", label: "buff[8..15]", value: "0x0", size: 8, type: "neutral" },
            { id: 1, address: "0x7ffc4", label: "buff[0..7]", value: "0x0", size: 8, type: "neutral" }
        ],
        payloadPresets: [
            { label: 'Safe', value: 'Hello!' },
            { label: 'Crash', value: 'AAAAAAAAAAAAAAAA++++++++BBBBBBBB' }
        ]
    },
    {
        id: '2a',
        title: 'Level 2A: Leaking Secrets',
        subtitle: 'Leak the secret by manipulating null-terminators!',
        goal: 'LEAK',
        successTitle: 'Secret Leaked!',
        successDesc: 'Since read() does not append a null byte, providing exactly 16 bytes bridged `buff` directly into `secret_key`. printf kept reading memory until it hit the null byte at the end of the secret, leaking it to the console!',
        defensePatch: {
            vulnText: "    read(1, buff, 16); // Safe length, but no null-terminator appended!",
            safeText: "    read(1, buff, 15); buff[15] = '\\0'; // Null-terminator secures the secret!"
        },
        startCodeLine: 10,
        code: [
            { text: "#include <stdio.h>", asm: [] },
            { text: "#include <unistd.h>", asm: [] },
            { text: "void vuln() {", asm: [] },
            { text: "    // Compiler places secret right above buff in the stack", asm: [] },
            { text: "    char secret_key[16] = \"SUPER_SECRET_KEY\";", asm: ["0000000000400486 <vuln>:", "  400486:       push   rbp", "  400487:       mov    rbp,rsp", "  40048a:       sub    rsp,0x20", "  40048e:       movabs rax,0x45535f5245505553", "  400498:       movabs rdx,0x59454b5f54455243", "  4004a2:       mov    QWORD PTR [rbp-0x10],rax", "  4004a6:       mov    QWORD PTR [rbp-0x8],rdx"] },
            { text: "    char buff[16];", asm: [] },
            { text: "    puts(\"Enter your name:\");", asm: ["  4004aa:       mov    edi,0x4011e8", "  4004af:       call   400370 <puts@plt>"] },
            { text: "    read(1, buff, 16); // Safe length, but no null-terminator appended!", asm: ["  4004b4:       lea    rax,[rbp-0x20]", "  4004b8:       mov    edx,0x10", "  4004bd:       mov    rsi,rax", "  4004c0:       mov    edi,0x1", "  4004c5:       call   400390 <read@plt>"] },
            { text: "    printf(\"Hello %s\\n\", buff); // Will print the name AND the secret key", asm: ["  4004ca:       lea    rax,[rbp-0x20]", "  4004ce:       mov    rsi,rax", "  4004d1:       mov    edi,0x4011f9", "  4004d6:       mov    eax,0x0", "  4004db:       call   400380 <printf@plt>"] },
            { text: "}", asm: ["  4004e0:       nop", "  4004e1:       leave", "  4004e2:       ret"] },
            { text: "int main() {", asm: ["00000000004004e3 <main>:", "  4004e3:       push   rbp", "  4004e4:       mov    rbp,rsp"] },
            { text: "    vuln();", asm: ["  4004e7:       call   400486 <vuln>"] },
            { text: "}", asm: ["  4004ec:       mov    eax,0x0", "  4004f1:       pop    rbp", "  4004f2:       ret"] }
        ],
        initialStack: [
            { id: 9, address: "0x7fff4", label: "main Saved EBP", value: "0x7fff0", size: 8, type: "main-frame" },
            { id: 8, address: "0x7ffec", label: "vuln Return Addr", value: "0x00000", size: 8, type: "safe" },
            { id: 7, address: "0x7ffe4", label: "vuln Saved EBP", value: "0x00000", size: 8, type: "safe" },
            { id: 6, address: "0x7ffdc", label: "secret_key[8..15]", value: "0x0", size: 8, type: "warn" },
            { id: 5, address: "0x7ffd4", label: "secret_key[0..7]", value: "0x0", size: 8, type: "warn" },
            { id: 4, address: "0x7ffcc", label: "buff[8..15]", value: "0x0", size: 8, type: "neutral" },
            { id: 3, address: "0x7ffc4", label: "buff[0..7]", value: "0x0", size: 8, type: "neutral" }
        ],
        payloadPresets: [
            { label: 'Safe', value: 'Hi!' },
            { label: 'Leak Secret', value: 'AAAAAAAAAAAAAAAA' }
        ]
    }
];
