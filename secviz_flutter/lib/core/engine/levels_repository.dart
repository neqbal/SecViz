import '../models/models.dart';

/// Complete port of LevelsRepository.java — all 4 level definitions.
class LevelsRepository {
  static List<Level>? _cache;

  static List<Level> getLevels() {
    _cache ??= _buildLevels();
    return _cache!;
  }

  static List<Level> _buildLevels() => [_level1(), _level2a(), _level2b(), _level3()];

  // ── LEVEL 1: The Crash ──────────────────────────────────────────────────────

  static Level _level1() {
    final code = [
      const CodeLine('#include <stdio.h>'),
      const CodeLine('#include <unistd.h>'),
      const CodeLine('void vuln() {'),
      CodeLine('    char buff[16];', asm: const [
        '0000000000400466 <vuln>:',
        '  400466:       push   rbp',
        '  400467:       mov    rbp,rsp',
        '  40046a:       sub    rsp,0x10',
      ]),
      CodeLine('    read(1, buff, 100); // User inputs > 24 bytes to crash', asm: const [
        '  40046e:       lea    rax,[rbp-0x10]',
        '  400472:       mov    edx,0x64',
        '  400477:       mov    rsi,rax',
        '  40047a:       mov    edi,0x1',
        '  40047f:       call   400370 <read@plt>',
      ]),
      CodeLine('}', asm: const [
        '  400484:       nop',
        '  400485:       leave',
        '  400486:       ret',
      ]),
      CodeLine('int main() {', asm: const [
        '0000000000400487 <main>:',
        '  400487:       push   rbp',
        '  400488:       mov    rbp,rsp',
      ]),
      CodeLine('    vuln();', asm: const [
        '  40048b:       call   400466 <vuln>',
      ]),
      CodeLine('}', asm: const [
        '  400490:       mov    eax,0x0',
        '  400495:       pop    rbp',
        '  400496:       ret',
      ]),
    ];

    final stack = [
      StackBlock(address: '0x7fff4', label: 'main Frame Data', value: '0x...', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7ffec', label: 'main Return Addr', value: '0x7fa2b', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7ffe4', label: 'main Saved EBP', value: '0x7fff0', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7ffdc', label: 'vuln Return Addr', value: '0x00000', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffd4', label: 'vuln Saved EBP', value: '0x00000', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffcc', label: 'buff[8..15]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffc4', label: 'buff[0..7]', value: '0x0', type: StackBlock.typeNeutral),
    ];

    return Level(
      id: '1',
      title: 'Level 1: The Crash',
      subtitle: 'Can you trigger a Segmentation Fault?',
      goal: 'CRASH',
      successTitle: 'Crash Successful!',
      successDesc: 'You successfully overflowed the buffer and corrupted the Return Address, causing the program to jump to invalid memory and crash.',
      startCodeLine: 6,
      code: code,
      initialStack: stack,
      payloadPresets: [['Safe', 'Hello!'], ['Crash', 'AAAAAAAAAAAAAAAA++++++++BBBBBBBB']],
      defensePatch: const DefensePatch(
        '    read(1, buff, 100); // User inputs > 24 bytes to crash',
        '    read(1, buff, 16); // Safely read only 16 bytes max!',
      ),
      objdump: _objdump1,
      ropGadgets: const [],
      hint: '💡 buff is 16 bytes. saved RBP is 8 bytes.\n'
          'To reach the return address you need 16 + 8 = 24 bytes of junk, '
          'then any extra bytes will corrupt the return address.\n\n'
          'Try entering 25+ characters — the last ones will spill into the return address slot and crash the process.',
    );
  }

  // ── LEVEL 2A: Leaking Secrets ────────────────────────────────────────────────

  static Level _level2a() {
    final code = [
      const CodeLine('#include <stdio.h>'),
      const CodeLine('#include <unistd.h>'),
      const CodeLine('void vuln() {'),
      const CodeLine('    // Compiler places secret right above buff in the stack'),
      CodeLine('    char secret_key[16] = "SUPER_SECRET_KEY";', asm: const [
        '0000000000400486 <vuln>:',
        '  400486:       push   rbp',
        '  400487:       mov    rbp,rsp',
        '  40048a:       sub    rsp,0x20',
        '  40048e:       movabs rax,0x45535f5245505553',
        '  400498:       movabs rdx,0x59454b5f54455243',
        '  4004a2:       mov    QWORD PTR [rbp-0x10],rax',
        '  4004a6:       mov    QWORD PTR [rbp-0x8],rdx',
      ]),
      const CodeLine('    char buff[16];'),
      CodeLine('    puts("Enter your name:");', asm: const [
        '  4004aa:       mov    edi,0x4011e8',
        '  4004af:       call   400370 <puts@plt>',
      ]),
      CodeLine('    read(1, buff, 16); // Safe length, but no null-terminator appended!', asm: const [
        '  4004b4:       lea    rax,[rbp-0x20]',
        '  4004b8:       mov    edx,0x10',
        '  4004bd:       mov    rsi,rax',
        '  4004c0:       mov    edi,0x1',
        '  4004c5:       call   400390 <read@plt>',
      ]),
      CodeLine('    printf("Hello %s\\n", buff); // Will print the name AND the secret key', asm: const [
        '  4004ca:       lea    rax,[rbp-0x20]',
        '  4004ce:       mov    rsi,rax',
        '  4004d1:       mov    edi,0x4011f9',
        '  4004d6:       mov    eax,0x0',
        '  4004db:       call   400380 <printf@plt>',
      ]),
      CodeLine('}', asm: const [
        '  4004e0:       nop',
        '  4004e1:       leave',
        '  4004e2:       ret',
      ]),
      CodeLine('int main() {', asm: const [
        '00000000004004e3 <main>:',
        '  4004e3:       push   rbp',
        '  4004e4:       mov    rbp,rsp',
      ]),
      CodeLine('    vuln();', asm: const [
        '  4004e7:       call   400486 <vuln>',
      ]),
      CodeLine('}', asm: const [
        '  4004ec:       mov    eax,0x0',
        '  4004f1:       pop    rbp',
        '  4004f2:       ret',
      ]),
    ];

    final stack = [
      StackBlock(address: '0x7fffc', label: 'main Return Addr', value: '0x7fa2b', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7fff4', label: 'main Saved EBP', value: '0x7fff0', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7ffec', label: 'vuln Return Addr', value: '0x00000', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffe4', label: 'vuln Saved EBP', value: '0x00000', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffdc', label: 'secret_key[8..15]', value: '0x0', type: StackBlock.typeWarn),
      StackBlock(address: '0x7ffd4', label: 'secret_key[0..7]', value: '0x0', type: StackBlock.typeWarn),
      StackBlock(address: '0x7ffcc', label: 'buff[8..15]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffc4', label: 'buff[0..7]', value: '0x0', type: StackBlock.typeNeutral),
    ];

    return Level(
      id: '2a',
      title: 'Level 2A: Leaking Secrets',
      subtitle: 'Leak the secret by manipulating null-terminators!',
      goal: 'LEAK',
      successTitle: 'Secret Leaked!',
      successDesc: "Since read() does not append a null byte, providing exactly 16 bytes bridged buff directly into secret_key. printf kept reading memory until it hit the null byte at the end of the secret, leaking it!",
      startCodeLine: 10,
      code: code,
      initialStack: stack,
      payloadPresets: [['Safe', 'Hi!'], ['Leak Secret', 'AAAAAAAAAAAAAAAA']],
      defensePatch: const DefensePatch(
        '    read(1, buff, 16); // Safe length, but no null-terminator appended!',
        "    read(1, buff, 15); buff[15] = '\\0'; // Null-terminator secures the secret!",
      ),
      ropGadgets: const [],
      hint: '💡 buff is 16 bytes, secret_key is right above it in memory.\n'
          'read() does NOT add a null terminator — so if you fill buff completely, '
          'printf("%s") will keep reading past buff into secret_key until it finds \\0.\n\n'
          "Enter exactly 16 characters (use the 'Leak Secret' preset) and watch printf print the secret!",
    );
  }

  // ── LEVEL 2B: Control Flow Hijack ────────────────────────────────────────────

  static Level _level2b() {
    final code = [
      const CodeLine('#include <stdio.h>'),
      CodeLine('void win() {', asm: const [
        '0000000000400476 <win>:',
        '  400476:       push   rbp',
        '  400477:       48 89 e5        mov    rbp,rsp',
      ]),
      CodeLine('    puts("SHELL OBTAINED — flag{ctrl_flow_h1jack}");', asm: const [
        '  40047a:       bf b0 11 40 00  mov    edi,0x4011b0',
        '  40047f:       e8 ec fe ff ff  call   400370 <puts@plt>',
      ]),
      CodeLine('}', asm: const [
        '  400484:       90              nop',
        '  400485:       5d              pop    rbp',
        '  400486:       c3              ret',
      ]),
      CodeLine('void vuln() {', asm: const [
        '0000000000400487 <vuln>:',
        '  400487:       55              push   rbp',
        '  400488:       48 89 e5        mov    rbp,rsp',
        '  40048b:       48 83 ec 20     sub    rsp,0x20',
      ]),
      const CodeLine('    char buf[32];'),
      CodeLine('    gets(buf);          // no bounds check', asm: const [
        '  40048f:       48 8d 45 e0     lea    rax,[rbp-0x20]',
        '  400493:       48 89 c7        mov    rdi,rax',
        '  400496:       e8 e5 fe ff ff  call   400380 <gets@plt>',
      ]),
      CodeLine('}', asm: const [
        '  40049b:       90              nop',
        '  40049c:       c9              leave',
        '  40049d:       c3              ret',
      ]),
      CodeLine('int main() {', asm: const [
        '000000000040049e <main>:',
        '  40049e:       55              push   rbp',
        '  40049f:       48 89 e5        mov    rbp,rsp',
      ]),
      CodeLine('    vuln();', asm: const [
        '  4004a2:       b8 00 00 00 00  mov    eax,0x0',
        '  4004a7:       e8 db ff ff ff  call   400487 <vuln>',
      ]),
      CodeLine('}', asm: const [
        '  4004ac:       b8 00 00 00 00  mov    eax,0x0',
        '  4004b1:       5d              pop    rbp',
        '  4004b2:       c3              ret',
      ]),
    ];

    final stack = [
      StackBlock(address: '0x7fff8', label: 'main Saved RBP', value: '0x7fff0', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7fff0', label: 'main Return Addr', value: '0x7fc2a', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7ffe8', label: 'vuln Return Addr', value: '0x4004ac', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffe0', label: 'vuln Saved RBP', value: '0x7fff8', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffd8', label: 'buf[24..31]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffd0', label: 'buf[16..23]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffc8', label: 'buf[ 8..15]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffc0', label: 'buf[ 0.. 7]', value: '0x0', type: StackBlock.typeNeutral),
    ];

    return Level(
      id: '2b',
      title: 'Level 2B: Control Flow Hijack',
      subtitle: 'Redirect execution to win() — no shellcode needed!',
      goal: 'CFH',
      successTitle: 'Shell Obtained!',
      successDesc: 'You overflowed buf (32 bytes) and saved RBP (8 bytes), then overwrote the return address with win\'s address (0x400476). When vuln() returned, RIP jumped directly to win().',
      startCodeLine: 8,
      code: code,
      initialStack: stack,
      payloadPresets: const [],
      defensePatch: null,
      objdump: _objdump2b,
      ropGadgets: const [],
      hint: '💡 buf is 32 bytes. saved RBP is 8 bytes above it.\n'
          'To reach the return address you need 32 + 8 = 40 bytes of junk, '
          'then write win()\'s address.\n\n'
          'Open the Objdump viewer ({ }) to find win()\'s address. '
          'Then use Generate Payload: select buf[0..7] → buf[24..31] + saved RBP as junk, '
          "and place win()'s address at 'vuln Return Addr'.",
    );
  }

  // ── LEVEL 3: Return-Oriented Programming ─────────────────────────────────────

  static Level _level3() {
    final code = [
      const CodeLine('#include <stdio.h>'),
      const CodeLine('#include <unistd.h>'),
      const CodeLine(''),
      CodeLine('char *binsh = "/bin/sh\\00";', asm: const [
        '0000000000403010 g  O .data  0000000000000008  binsh',
        '// binsh is a REAL pointer in the .data section — address 0x403010',
        '// Use this address as RDI when calling execve',
      ]),
      const CodeLine(''),
      CodeLine('void gadget_pop_rax() { __asm__("pop %rax; ret"); }', asm: const [
        '0000000000400466 <gadget_pop_rax>:',
        '  400466:  55                      push   rbp',
        '  400467:  48 89 e5                mov    rbp,rsp',
        '  40046a:  58                      pop    rax   ← 0x40046a',
        '  40046b:  c3                      ret',
      ]),
      CodeLine('void gadget_pop_rdi() { __asm__("pop %rdi; ret"); }', asm: const [
        '000000000040046f <gadget_pop_rdi>:',
        '  40046f:  55                      push   rbp',
        '  400470:  48 89 e5                mov    rbp,rsp',
        '  400473:  5f                      pop    rdi   ← 0x400473',
        '  400474:  c3                      ret',
      ]),
      CodeLine('void gadget_pop_rsi() { __asm__("pop %rsi; ret"); }', asm: const [
        '0000000000400478 <gadget_pop_rsi>:',
        '  400478:  55                      push   rbp',
        '  400479:  48 89 e5                mov    rbp,rsp',
        '  40047c:  5e                      pop    rsi   ← 0x40047c',
        '  40047d:  c3                      ret',
      ]),
      CodeLine('void gadget_pop_rdx() { __asm__("pop %rdx; ret"); }', asm: const [
        '0000000000400481 <gadget_pop_rdx>:',
        '  400481:  55                      push   rbp',
        '  400482:  48 89 e5                mov    rbp,rsp',
        '  400485:  5a                      pop    rdx   ← 0x400485',
        '  400486:  c3                      ret',
      ]),
      CodeLine('void gadget_syscall() { __asm__("syscall; ret"); }', asm: const [
        '000000000040048a <gadget_syscall>:',
        '  40048a:  55                      push   rbp',
        '  40048b:  48 89 e5                mov    rbp,rsp',
        '  40048e:  0f 05                   syscall      ← 0x40048e',
        '  400490:  c3                      ret',
      ]),
      const CodeLine(''),
      CodeLine('void vuln() {', asm: const [
        '0000000000400494 <vuln>:',
        '  400494:  55                      push   rbp',
        '  400495:  48 89 e5                mov    rbp,rsp',
        '  400498:  48 83 ec 20             sub    rsp,0x20  ← buf[32]',
      ]),
      const CodeLine('    char buf[32];'),
      CodeLine('    gets(buf);', asm: const [
        '  40049c:  48 8d 45 e0             lea    rax,[rbp-0x20]',
        '  4004a0:  48 89 c7                mov    rdi,rax',
        '  4004a3:  e8 c8 fe ff ff          call   400370 <gets@plt>',
      ]),
      CodeLine('}', asm: const [
        '  4004a8:  90                      nop',
        '  4004a9:  c9                      leave',
        '  4004aa:  c3                      ret',
      ]),
      const CodeLine(''),
      CodeLine('int main() {', asm: const [
        '00000000004004ab <main>:',
        '  4004ab:  55                      push   rbp',
        '  4004ac:  48 89 e5                mov    rbp,rsp',
      ]),
      CodeLine('    vuln();', asm: const [
        '  4004af:  b8 00 00 00 00          mov    eax,0x0',
        '  4004b4:  e8 db ff ff ff          call   400494 <vuln>',
        '  4004b9:  b8 00 00 00 00          mov    eax,0x0  ← ret addr pushed here',
      ]),
      CodeLine('    return 0;', asm: const [
        '  4004b9:  b8 00 00 00 00          mov    eax,0x0',
        '  4004be:  5d                      pop    rbp',
        '  4004bf:  c3                      ret',
      ]),
      const CodeLine('}'),
    ];

    final stack = [
      StackBlock(address: '0x7fff8', label: 'main Saved RBP', value: '0x7ffff0', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7fff0', label: 'main Ret Addr', value: '0x7fc2a', type: StackBlock.typeMainFrame),
      StackBlock(address: '0x7ffe8', label: 'vuln Ret Addr', value: '0x4004b9', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffe0', label: 'vuln Saved RBP', value: '0x7fff8', type: StackBlock.typeSafe),
      StackBlock(address: '0x7ffd8', label: 'buf[24..31]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffd0', label: 'buf[16..23]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffc8', label: 'buf[ 8..15]', value: '0x0', type: StackBlock.typeNeutral),
      StackBlock(address: '0x7ffc0', label: 'buf[ 0.. 7]', value: '0x0', type: StackBlock.typeNeutral),
    ];

    const gadgets = [
      RopGadget(
        address: '0x40046a', asm: 'pop rax ; ret',
        description: 'Found at 0x40046a inside gadget_pop_rax().\nPops the top of the stack into RAX — the syscall number register.\nIn execve("/bin/sh",0,0), RAX must be 59 (0x3b).',
        alias: 'pop_rax',
      ),
      RopGadget(
        address: '0x400473', asm: 'pop rdi ; ret',
        description: 'Found at 0x400473 inside gadget_pop_rdi().\nPops the top of the stack into RDI — the first syscall argument.\nIn execve("/bin/sh",0,0), RDI must point to the /bin/sh string.\nUse address 0x403010.',
        alias: 'pop_rdi',
      ),
      RopGadget(
        address: '0x40047c', asm: 'pop rsi ; ret',
        description: 'Found at 0x40047c inside gadget_pop_rsi().\nPops the top of the stack into RSI — the second syscall argument.\nIn execve("/bin/sh",0,0), RSI (argv) must be NULL (0).',
        alias: 'pop_rsi',
      ),
      RopGadget(
        address: '0x400485', asm: 'pop rdx ; ret',
        description: 'Found at 0x400485 inside gadget_pop_rdx().\nPops the top of the stack into RDX — the third syscall argument.\nIn execve("/bin/sh",0,0), RDX (envp) must be NULL (0).',
        alias: 'pop_rdx',
      ),
      RopGadget(
        address: '0x40048e', asm: 'syscall',
        description: 'Found at 0x40048e inside gadget_syscall().\nExecutes a Linux kernel system call using current register values:\n  RAX = syscall number  (59 = execve)\n  RDI = 1st argument    (pointer to "/bin/sh")\n  RSI = 2nd argument    (NULL)\n  RDX = 3rd argument    (NULL)',
        alias: 'syscall',
      ),
      RopGadget(
        address: '0x403010', asm: '"/bin/sh"',
        description: 'REAL address: global char *binsh = "/bin/sh" in .data section.\nSet RDI = 0x403010 so execve receives the path argument.',
        alias: '/bin/sh',
      ),
    ];

    return Level(
      id: '3',
      title: 'Level 3: Return-Oriented Programming',
      subtitle: 'NX is enabled — chain gadgets to bypass it and spawn a shell.',
      goal: 'ROP',
      successTitle: 'Shell Obtained!',
      successDesc: 'You bypassed NX by chaining 5 real gadgets from the binary:\n'
          '  pop_rax → 59  |  pop_rdi → 0x403010 (/bin/sh)  |  pop_rsi → 0  |  pop_rdx → 0  |  syscall\n\n'
          'No new code was injected. Every byte you executed already lived in the binary.',
      startCodeLine: 16,
      code: code,
      initialStack: stack,
      payloadPresets: [['NX Demo (Shellcode → SIGSEGV)', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\\x90\\x90\\x90\\x31\\xc0\\x50']],
      defensePatch: null,
      objdump: _objdump3,
      ropGadgets: gadgets,
      hint: '💡 Goal: execve("/bin/sh", 0, 0) via syscall (RAX=59).\n\n'
          'Chain order:\n'
          '  1. pop rax ; ret  → 0x3b (59)\n'
          '  2. pop rdi ; ret  → 0x403010 (address of /bin/sh string)\n'
          '  3. pop rsi ; ret  → 0x0\n'
          '  4. pop rdx ; ret  → 0x0\n'
          '  5. syscall\n\n'
          'All gadget addresses are in the binary. Use the ROP Scanner to find them.',
    );
  }

  // ── Objdump strings (abbreviated for Level 1 & 2A, full for 2B & 3) ─────────

  static const _objdump1 = '''vuln:     file format elf64-x86-64

Disassembly of section .text:

0000000000400466 <vuln>:
  400466:       push   rbp
  400467:       mov    rbp,rsp
  40046a:       sub    rsp,0x10
  40046e:       lea    rax,[rbp-0x10]
  400472:       mov    edx,0x64
  400477:       mov    rsi,rax
  40047a:       mov    edi,0x1
  40047f:       call   400370 <read@plt>
  400484:       nop
  400485:       leave
  400486:       ret

0000000000400487 <main>:
  400487:       push   rbp
  400488:       mov    rbp,rsp
  40048b:       call   400466 <vuln>
  400490:       mov    eax,0x0
  400495:       pop    rbp
  400496:       ret''';

  static const _objdump2b = '''vuln:     file format elf64-x86-64

Disassembly of section .plt:

0000000000400370 <puts@plt>:
  400370:  ff 25 8a 2c 00 00  jmp    QWORD PTR [rip+0x2c8a]

0000000000400380 <gets@plt>:
  400380:  ff 25 82 2c 00 00  jmp    QWORD PTR [rip+0x2c82]

Disassembly of section .text:

0000000000400476 <win>:
  400476:  55                 push   rbp
  400477:  48 89 e5           mov    rbp,rsp
  40047a:  bf b0 11 40 00     mov    edi,0x4011b0
  40047f:  e8 ec fe ff ff     call   400370 <puts@plt>
  400484:  90                 nop
  400485:  5d                 pop    rbp
  400486:  c3                 ret

0000000000400487 <vuln>:
  400487:  55                 push   rbp
  400488:  48 89 e5           mov    rbp,rsp
  40048b:  48 83 ec 20        sub    rsp,0x20
  40048f:  48 8d 45 e0        lea    rax,[rbp-0x20]
  400493:  48 89 c7           mov    rdi,rax
  400496:  e8 e5 fe ff ff     call   400380 <gets@plt>
  40049b:  90                 nop
  40049c:  c9                 leave
  40049d:  c3                 ret

000000000040049e <main>:
  40049e:  55                 push   rbp
  40049f:  48 89 e5           mov    rbp,rsp
  4004a2:  b8 00 00 00 00     mov    eax,0x0
  4004a7:  e8 db ff ff ff     call   400487 <vuln>
  4004ac:  b8 00 00 00 00     mov    eax,0x0
  4004b1:  5d                 pop    rbp
  4004b2:  c3                 ret''';

  static const _objdump3 = '''vuln:     file format elf64-x86-64

── .data section ────────────────────────────────────────────────
0000000000403010 g  O .data  0000000000000008  binsh
  → points to '/bin/sh' string  (use 0x403010 as RDI)

── key gadgets (from ROPgadget) ──────────────────────────────────
0x40046a : pop rax ; ret
0x400473 : pop rdi ; ret
0x40047c : pop rsi ; ret
0x400485 : pop rdx ; ret
0x40048e : syscall

── disassembly ───────────────────────────────────────────────────
0000000000400466 <gadget_pop_rax>:
  40046a:  58           pop    rax
  40046b:  c3           ret

000000000040046f <gadget_pop_rdi>:
  400473:  5f           pop    rdi
  400474:  c3           ret

0000000000400478 <gadget_pop_rsi>:
  40047c:  5e           pop    rsi
  40047d:  c3           ret

0000000000400481 <gadget_pop_rdx>:
  400485:  5a           pop    rdx
  400486:  c3           ret

000000000040048a <gadget_syscall>:
  40048e:  0f 05        syscall
  400490:  c3           ret

0000000000400494 <vuln>:
  400494:  55           push   rbp
  400495:  48 89 e5     mov    rbp,rsp
  400498:  48 83 ec 20  sub    rsp,0x20
  40049c:  48 8d 45 e0  lea    rax,[rbp-0x20]
  4004a0:  48 89 c7     mov    rdi,rax
  4004a3:  e8 c8 fe ff  call   400370 <gets@plt>
  4004a8:  90           nop
  4004a9:  c9           leave
  4004aa:  c3           ret

00000000004004ab <main>:
  4004ab:  55           push   rbp
  4004ac:  48 89 e5     mov    rbp,rsp
  4004af:  b8 00 00 00  mov    eax,0x0
  4004b4:  e8 db ff ff  call   400494 <vuln>
  4004b9:  b8 00 00 00  mov    eax,0x0   ← saved return address
  4004be:  5d           pop    rbp
  4004bf:  c3           ret

── exploit goal ──────────────────────────────────────────────────
  Overflow buf[32] + saved_rbp (8) → overwrite ret addr = 0x4004b9
  Chain:
    pop_rax  → 59          (execve syscall number)
    pop_rdi  → 0x403010    (pointer to "/bin/sh" in .data)
    pop_rsi  → 0           (argv = NULL)
    pop_rdx  → 0           (envp = NULL)
    syscall''';
}
