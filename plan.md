# SecViz — Curriculum Implementation Plan

> This document outlines the full content roadmap for the SecViz interactive
> binary exploitation teaching app.  
> Inner implementation details (code, adapters, ViewModel changes) are tracked
> separately in `task.md`.  
> **Level 1 is already implemented.** Everything below is planned future work.

---

## Level 1 — Secondary Teaching Modules
*"Understanding the Stack"*

> **Design decision (locked):** Secondary modules appear in a dedicated **Theory tab**
> inside each level screen — separate from the main Exploit tab. The Theory tab is
> always accessible (not gated behind beating the level), and modules unlock
> sequentially as the player progresses through the main simulation.

These are short, focused sub-lessons that provide the conceptual foundation
every later level depends on.

---

### 1-A  How Memory is Laid Out on the Stack

**Goal:** The learner understands that the stack grows *downward*, that addresses
decrease as the stack grows, and that each frame has a well-defined region.

**Simulated scenario:**
```c
void outer() {
    int x = 10;
    inner();
}
void inner() {
    int y = 20;
}
```

**Interactive stages:**

| Step | What happens | UI highlight |
|------|-------------|--------------|
| 1 | `outer()` is called. RSP decreases by 16. | Stack canvas animates new frame region, address labels update. |
| 2 | `x = 10` is stored at `[rbp-0x4]`. | Stack block for `x` lights up; hex dump shows `0x0000000A`. |
| 3 | `inner()` is called. A new frame appears *below* `outer`'s frame. | Two-frame stack view with a visual divider between frames. |
| 4 | `y = 20` stored at `[rbp-0x4]` in *inner's* frame. | Player sees same offset, different absolute address. Teaches relative vs absolute. |
| 5 | `inner()` returns. Its frame is "popped" (RSP moves back up). | The inner frame blocks fade/collapse. |

**Key concept callout cards:**
- "The stack grows DOWN. Higher RSP = less stack used."
- "Each variable lives at a *negative offset from RBP*."
- "Frames are not erased — their bytes linger until overwritten."

---

### 1-B  How Function Calls Work

**Goal:** Demystify `call` and `ret`; show that `call` is just `push RIP; jmp`.

**Simulated scenario:**
```asm
; In caller:
  call   vuln          ; pushes next RIP, jumps to vuln
; In vuln:
  push   rbp
  mov    rbp, rsp
  ...
  pop    rbp
  ret                  ; pops saved RIP → resumes caller
```

**Interactive stages:**

| Step | What happens | UI highlight |
|------|-------------|--------------|
| 1 | Highlight `call vuln` in the code viewer. | Assembly line pulses. |
| 2 | CPU pushes `RIP+5` (next instruction) onto the stack. | New stack block labelled **Return Address** appears, shows value. |
| 3 | RIP jumps to `vuln`. | Code viewer scrolls to `vuln`. Register panel: RIP updates. |
| 4 | `push rbp` — old RBP saved. | Stack block **Saved RBP** appears. |
| 5 | `mov rbp, rsp` — new base pointer set. | RBP register updates to match RSP. |
| 6 | Player is shown the full frame layout so far: `[ret addr][saved rbp][locals...]` | Stack blocks colour-coded by type. |

**Key concept callout cards:**
- "`call` = `push RIP; jmp`. Nothing magic."
- "RBP anchors the frame. RSP moves freely."

---

### 1-C  How Space is Created for Local Variables

**Goal:** Show `sub rsp, N` is *all* it takes; memory is not zeroed.

**Simulated scenario:**
```asm
sub    rsp, 0x20      ; reserve 32 bytes for locals
```

**Interactive stages:**

| Step | What happens | UI highlight |
|------|-------------|--------------|
| 1 | Before `sub`, RSP value shown. Stack blocks above are "in use". | |
| 2 | `sub rsp, 0x20` executes. RSP decreases. 4 new uninitialized blocks appear. | Blocks shown with `??` / random-looking hex values and a "⚠ uninitialized" badge. |
| 3 | Individual variables are assigned (`mov [rbp-8], rdi`). | Blocks update with real values. |
| 4 | Player sees that *un-assigned* bytes still hold old data. | One block intentionally left `??` to drive the point home. |

**Key concept callout cards:**
- "`sub rsp, N` just moves a pointer. RAM is not cleared."
- "Reading uninitialized memory = undefined behaviour / info leak."

---

### 1-D  How a Function Returns (`leave; ret`)

**Goal:** Demystify `leave` (which most compilers emit) and `ret`.

**Simulated scenario:**
```asm
leave    ; = mov rsp, rbp; pop rbp
ret      ; = pop rip
```

**Interactive stages:**

| Step | What happens | UI highlight |
|------|-------------|--------------|
| 1 | Frame is fully set up. Player sees `[ret addr][saved rbp][locals]`. | |
| 2 | `leave` fires. `RSP ← RBP`. All local blocks "reclaimed" (fade). | |
| 3 | `pop rbp` restores old RBP. Saved-RBP block pops off. | Register panel: RBP restores. |
| 4 | `ret`. Return-address block pops. `RIP ← saved value`. | Code viewer jumps back to caller. Stack is clean. |
| 5 | **Interactive quiz:** "What value did `ret` pop into RIP?" Player picks from stack blocks. | |

**Key concept callout cards:**
- "`leave` = `mov rsp, rbp; pop rbp`. One instruction, two steps."
- "`ret` hands control to whoever is at the top of the stack."

---

## Level 2 — Control Flow Hijack
*"Redirect Execution to a Win Function"*

### Overview

The classic buffer overflow that overwrites the saved return address to point at
a `win()` function that already exists in the binary. No shellcode needed — the
address is handed to the player.

### Learning Goals
- Understand that overflowing a buffer reaches the return address.
- Calculate exact offset from buffer start to saved RIP.
- Craft a payload: `[padding × N] + [address of win()]`.
- Appreciate why "no execute" (NX) doesn't protect this case.

### Simulated C Code
```c
void win() {
    // "You exploited me!"
    puts("SHELL OBTAINED — flag{ctrl_flow_h1jack}");
}

void vuln() {
    char buf[32];
    gets(buf);          // no bounds check
}

int main() {
    vuln();
    return 0;
}
```

### Stack Layout at `vuln` Entry
```
 High addr
 ┌──────────────────┐
 │  ret addr (main) │  ← target: overwrite with &win
 ├──────────────────┤
 │  saved RBP       │  ← +8 bytes of padding
 ├──────────────────┤
 │  buf[24..31]     │
 │  buf[16..23]     │
 │  buf[ 8..15]     │
 │  buf[ 0.. 7]     │  ← first byte of input
 └──────────────────┘
 Low addr
```
Offset = 32 (buf) + 8 (saved RBP) = **40 bytes** before reaching return address.

### Interactive Stages

> **Design decision (locked):** The player must find `win()`'s address themselves
> by reading the `objdump -d` output provided in-app. No address is handed to them.
> This simulates real reconnaissance and builds the mental model of "the binary
> has a fixed symbol table".

| # | Stage | What the player does | UI feedback |
|---|-------|----------------------|-------------|
| 1 | **Setup** | Read the C source in the code viewer; identify `gets(buf)` as the sink. | `gets` line highlighted in red. Callout: "No bound check." |
| 2 | **Measure the buffer** | Tap stack blocks to count the 32-byte `buf` region. | Stack blocks count up; offset badge shows running total. |
| 3 | **Find `win()` in objdump** | Switch to the **Objdump tab** (scrollable disassembly view). Locate `<win>:` label and note the address. | Address column highlighted; hint: "Look for the `<win>` symbol." |
| 4 | **Enter the address** | In the payload builder, type the address manually (e.g. `0x004011b6`) into the hex input field. The input auto-formats to 8-byte little-endian. | Preview shows `b6 11 40 00 00 00 00 00`. Stack animation plays: buf fills → overflow goes red → ret addr block turns amber → player's address written in. |
| 5 | **Fire!** | Tap "Send Payload". | RIP flashes amber → new address → flashes green as execution arrives at `win()`. Console prints flag. |
| 6 | **Reflection** | "Why didn't NX stop this?" | Explainer card: "NX blocks *injected* bytes from running, but `win()` already lives in the executable segment." |

### UI / UX Notes
- **Objdump tab:** A scrollable `RecyclerView` of monospace disassembly lines.
  Symbol labels (`<win>:`, `<vuln>:`) are rendered in the accent colour.
  The player can long-press an address to copy it to the payload address field.
- **Payload builder:** Two input zones:
  1. **Padding** — a stepper (`−` / `+`) that adjusts the fill length and shows
     the corresponding padding string (`AAAA…` × N).
  2. **Target address** — a hex `EditText`; validates 1–8 bytes; live preview
     shows the little-endian byte sequence that will be written.
- **Stack canvas** overflow animation: bytes inside `buf` = green, overflow
  bytes past `buf` = red, saved-RBP zone = amber, return-address block starts
  green and turns red when overwritten by the player's value.
- RIP register: amber on overwrite → green flash on successful jump to `win`.

---

## Level 3 — Return-Oriented Programming (ROP)
*"Chain Gadgets to Bypass NX"*

### Overview

NX is enabled. There is no `win()`. The player must invoke `execve("/bin/sh", 0, 0)`
by chaining existing code snippets ("gadgets") already present in the binary.

### Learning Goals
- Understand why injecting shellcode fails when NX is on.
- Understand what a ROP gadget is (`pop rdi; ret`, etc.).
- Build a simple ROP chain to set registers and invoke a syscall.

### Simulated Scenario (x86-64 Linux, static binary)
```
Gadgets available (discovered by the app):
  0x401234 : pop rdi ; ret
  0x401240 : pop rsi ; ret
  0x401248 : pop rdx ; ret
  0x401252 : pop rax ; ret
  0x401256 : syscall

String "/bin/sh\0" at 0x402000

Goal:
  RDI = 0x402000  (pointer to "/bin/sh")
  RSI = 0         (argv = NULL)
  RDX = 0         (envp = NULL)
  RAX = 59        (execve syscall number)
  syscall
```

### Interactive Stages

> **Design decision (locked):** The chain is built in a **text-based editor**.
> Each line is one 8-byte stack value — either a gadget address or a raw value.
> The editor parses aliases (e.g. `pop_rdi`, `/bin/sh`, `59`) and resolves them
> to addresses + hex bytes shown in a live preview column beside the editor.

| # | Stage | What the player does | UI feedback |
|---|-------|----------------------|-------------|
| 1 | **NX Demo** | Send pre-filled shellcode payload. | "Execution prevented — SIGSEGV" banner. Explainer: NX/PROT_EXEC. |
| 2 | **Gadget Discovery** | Tap "Scan Gadgets". | Animated scan of `.text`; gadget list panel slides up. Each gadget shows address + instructions. |
| 3 | **Inspect a gadget** | Tap any gadget row. | Mini assembly viewer + annotation: "This gadget moves a value from the stack into RDI, then returns." |
| 4 | **Build the chain** | In the **ROP chain text editor**, type one entry per line. Aliases resolve live. | Right column shows: alias → resolved hex bytes. Validation errors underlined in red. |
| 5 | **Stack preview** | Tap "Preview". | Stack canvas renders the payload as consecutive 8-byte blocks, each labelled. |
| 6 | **Execute** | Tap "Send Payload". | RIP hops: gadget₁ → gadget₂ → … → `syscall`. Register panel updates each hop. Console prints shell. |
| 7 | **Reflection** | "Why does this work?" | Explainer card: "We never wrote new code. We reused existing executable bytes — NX is bypassed entirely." |

### Text Chain Editor Format
```
# ROP chain editor — one 8-byte value per line
# aliases: pop_rdi  pop_rsi  pop_rdx  pop_rax  syscall  /bin/sh

pop_rdi          # 0x0000000000401234  → pop rdi ; ret
/bin/sh          # 0x0000000000402000  → ptr to "/bin/sh\0"
pop_rsi          # 0x0000000000401240  → pop rsi ; ret
0                # 0x0000000000000000
pop_rdx          # 0x0000000000401248  → pop rdx ; ret
0                # 0x0000000000000000
pop_rax          # 0x0000000000401252  → pop rax ; ret
59               # 0x000000000000003b  (execve)
syscall          # 0x0000000000401256  → syscall
```
- Lines starting with `#` are comments (shown in dim text).
- Blank lines allowed for readability.
- Each non-comment line resolves to exactly 8 bytes; validation bar shows
  total payload length and byte count.

---

### Secondary Module 3-A — ROP Chaining Deep Dive
*(unlocked after beating Level 3; details TBD)*

Placeholder topic list:
- `ret2plt` / `ret2libc` walkthrough
- Stack pivot (`xchg rsp, rax; ret`)
- Gadget search methodology — ROPgadget, ropper (demoed inside app)
- Chaining a `puts(got_entry)` to leak libc base before the real chain

---

## Level 4 — Security Mitigations
*"The Binary Fights Back"*

### Overview

Same overflow primitive, but the binary now has **Stack Canary** and **PIE**
enabled. The player learns what each mitigation does, why the naive overflow
fails, and conceptually how each is bypassed.

> Inner details TBD. Planned module breakdown:

### Module 4-A — Stack Canary
- What the canary is (random qword placed between locals and return address at function entry).
- Why any overflow that reaches the return address also overwrites the canary.
- `__stack_chk_fail` → `abort()` demo.
- Conceptual bypass: information-leak bug lets you *read* the canary → preserve it in your payload.
- Simulated format-string or partial-overwrite to illustrate the idea.

### Module 4-B — PIE / ASLR
- What PIE is: binary loads at a random base each run.
- Why the hardcoded `&win` address from Level 2 no longer works.
- Conceptual bypass: an info-leak to discover the runtime base → compute real address.
- Simulation: show two "runs" with different base addresses, then show how a
  single leaked pointer collapses the randomness.

### Module 4-C — Combining Both Mitigations
- Two-stage exploit: first leak canary + base address, then overwrite return address.
- Reinforces that mitigations must each be defeated before control is taken.

---

## Implementation Priority Order

```
Phase 1 — Foundation (before any new level code):
  ├── Module 1-A  Stack memory layout
  ├── Module 1-B  Function calls (call / ret)
  ├── Module 1-C  Local variable allocation (sub rsp, N)
  └── Module 1-D  Function return (leave; ret)

Phase 2 — New main level:
  └── Level 2     Control flow hijack to win()

Phase 3 — After Level 2 is stable:
  └── Level 3     ROP basics + gadget chain builder

Phase 4 — After Level 3:
  ├── Module 3-A  ROP chaining deep dive (details TBD)
  └── Level 4     Mitigations — canary + PIE
```

---

## Data Model Notes

- Each secondary module should be a `SubLevel` entity referencing a parent
  `Level` by index, with its own `steps[]` list and optional quiz checkpoints.
- `LevelManager` should expose `List<SubLevel> getSubLevels(int levelIndex)`.
- The level-select screen should render sub-levels as expandable rows under
  their parent level card.
- All addresses in simulations must be **fake but realistic** (e.g. `0x00401234`)
  and clearly labelled as illustrative.
- Consider a `ConceptCard` data class to hold title + body text for the
  "key concept callout" cards shown between stages.

---

---

## Design Decisions Log

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Secondary module navigation | **Separate Theory tab** per level | Always accessible; not gated. Separates conceptual learning from the active exploit flow. |
| Level 2 address discovery | **Player types address manually** after reading provided objdump | Builds real recon habit; teaches symbol tables and disassembly output. |
| Level 3 chain builder | **Text-based editor** with alias resolution | More authentic to real exploit dev (pwntools scripts); scales to complex chains without UI clutter. |

---

*Last updated: 2026-04-01*
