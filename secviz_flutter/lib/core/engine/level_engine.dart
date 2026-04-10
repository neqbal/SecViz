import 'dart:convert';
import '../models/models.dart';

// ─────────────────────────────────────────────────────────────────────────────
// Simulation State
// ─────────────────────────────────────────────────────────────────────────────

class SimState {
  final double step;
  final bool waitingForInput;
  final int activeLineIndex;
  final List<StackBlock> stack;
  final int espIndex;
  final int ebpIndex;
  final String statusTitle;
  final String statusDesc;
  final String statusType; // info | success | danger | warn
  final List<(String, bool)> consoleOut;
  final bool isPatched;
  final List<RegisterSnapshot> snapshots;
  final int activeSnapshotIndex;
  final bool timelineVisible;
  final bool getsReached;
  final Map<String, String> simRegs;
  final int activeAsmInstrIdx;
  final bool isTerminal;
  final (String, bool)? pendingToast;
  final List<List<int>> hexRows;
  final String userInput;
  final bool ropChainReady;  // ← user has saved a ROP chain in Level 3

  const SimState({
    this.step = 0,
    this.waitingForInput = false,
    this.activeLineIndex = 0,
    this.stack = const [],
    this.espIndex = 0,
    this.ebpIndex = 0,
    this.statusTitle = 'Program Ready',
    this.statusDesc = 'Highlighted instruction is NEXT to execute. Press ni or n.',
    this.statusType = 'info',
    this.consoleOut = const [],
    this.isPatched = false,
    this.snapshots = const [],
    this.activeSnapshotIndex = -1,
    this.timelineVisible = false,
    this.getsReached = false,
    this.simRegs = const {},
    this.activeAsmInstrIdx = -1,
    this.isTerminal = false,
    this.pendingToast,
    this.hexRows = const [],
    this.userInput = '',
    this.ropChainReady = false,
  });

  SimState copyWith({
    double? step,
    bool? waitingForInput,
    int? activeLineIndex,
    List<StackBlock>? stack,
    int? espIndex,
    int? ebpIndex,
    String? statusTitle,
    String? statusDesc,
    String? statusType,
    List<(String, bool)>? consoleOut,
    bool? isPatched,
    List<RegisterSnapshot>? snapshots,
    int? activeSnapshotIndex,
    bool? timelineVisible,
    bool? getsReached,
    Map<String, String>? simRegs,
    int? activeAsmInstrIdx,
    bool? isTerminal,
    (String, bool)? pendingToast,
    bool clearToast = false,
    List<List<int>>? hexRows,
    String? userInput,
    bool? ropChainReady,
  }) {
    return SimState(
      step: step ?? this.step,
      waitingForInput: waitingForInput ?? this.waitingForInput,
      activeLineIndex: activeLineIndex ?? this.activeLineIndex,
      stack: stack ?? this.stack,
      espIndex: espIndex ?? this.espIndex,
      ebpIndex: ebpIndex ?? this.ebpIndex,
      statusTitle: statusTitle ?? this.statusTitle,
      statusDesc: statusDesc ?? this.statusDesc,
      statusType: statusType ?? this.statusType,
      consoleOut: consoleOut ?? this.consoleOut,
      isPatched: isPatched ?? this.isPatched,
      snapshots: snapshots ?? this.snapshots,
      activeSnapshotIndex: activeSnapshotIndex ?? this.activeSnapshotIndex,
      timelineVisible: timelineVisible ?? this.timelineVisible,
      getsReached: getsReached ?? this.getsReached,
      simRegs: simRegs ?? this.simRegs,
      activeAsmInstrIdx: activeAsmInstrIdx ?? this.activeAsmInstrIdx,
      isTerminal: isTerminal ?? this.isTerminal,
      pendingToast: clearToast ? null : (pendingToast ?? this.pendingToast),
      hexRows: hexRows ?? this.hexRows,
      userInput: userInput ?? this.userInput,
      ropChainReady: ropChainReady ?? this.ropChainReady,
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Level Simulation Engine — port of LevelViewModel.java
// ─────────────────────────────────────────────────────────────────────────────

class LevelEngine {
  // ── Public state ──────────────────────────────────────────────────────────
  SimState _state = const SimState();
  SimState get state => _state;

  // ── Private sim vars ──────────────────────────────────────────────────────
  Level? _level;
  int _initialEsp = 0;
  List<AsmLine> _asmFlat = [];
  final Map<String, int> _addrToAsmIdx = {};
  final Map<int, String> _srcLineToFirstAddr = {};
  final Map<String, int> _addrToSrcLine = {};
  int _pendingAsmIdx = -1;
  bool _execWaiting = false;
  bool _isTerminal = false;
  String? _lastExecutedAddr;

  // ROP sim vars
  String _ropRax = '0x0';
  String _ropRdi = '0x0';
  String _ropRsi = '0x0';
  String _ropRdx = '0x0';
  String _ropRip = 'main';
  List<String> _ropPayload = [];
  int _ropPayloadPos = 0;
  int _ropEspIdx = -1;
  int _ropHopCount = 0;
  Map<String, String> _savedRopValues = {};   // ← saved by saveRopChain()

  // General-purpose register simulation
  final Map<String, int> _simGPR = {
    'rax': 0, 'rbx': 0, 'rcx': 0, 'rdx': 0,
    'rsi': 0, 'rdi': 0, 'rsp': 0, 'rbp': 0, 'rip': 0,
  };

  // Per-instruction register overrides from JSON
  final Map<String, Map<String, String>> _instrRegOverrides = {};

  int _snapshotCount = 0;

  // ── Public API ──────────────────────────────────────────────────────────────

  void init(Level level) {
    _level = level;
    _initialEsp = _findInitialEsp(level);
    _buildAsmMaps();
    reset();
  }

  void loadRegisterJson(String json) {
    _instrRegOverrides.clear();
    try {
      final root = jsonDecode(json) as Map<String, dynamic>;
      for (final entry in root.entries) {
        final addr = entry.key.toLowerCase();
        if (addr.startsWith('_')) continue;
        if (entry.value is! Map) continue;
        final regObj = entry.value as Map<String, dynamic>;
        final regMap = <String, String>{};
        for (final r in regObj.entries) {
          final key = r.key.toUpperCase();
          if (key.startsWith('_')) continue;
          regMap[key] = r.value.toString();
        }
        if (regMap.isNotEmpty) _instrRegOverrides[addr] = regMap;
      }
    } catch (_) {}
  }

  void reset() {
    _execWaiting = false;
    _isTerminal = false;
    _ropRax = '0x0'; _ropRdi = '0x0'; _ropRsi = '0x0'; _ropRdx = '0x0';
    _ropRip = 'main'; _ropPayload = []; _ropPayloadPos = 0;
    _ropEspIdx = -1; _ropHopCount = 0;
    _savedRopValues = {};
    _snapshotCount = 0;
    _lastExecutedAddr = null;
    _pendingAsmIdx = -1;
    _initSimGPR();

    _state = SimState(
      step: 0,
      activeLineIndex: _level!.startCodeLine,
      stack: _deepCopyStack(_level!.initialStack),
      espIndex: _initialEsp,
      ebpIndex: _initialEsp,
      statusTitle: 'Program Ready',
      statusDesc: 'Highlighted instruction is NEXT to execute. Press ni or n.',
      statusType: 'info',
      isPatched: _state.isPatched,
    );

    _syncPendingToSourceLine(_level!.startCodeLine);
    _emitSimRegs();
    _recordSnapshot('Program Ready', 'main');
  }

  void togglePatch() {
    _state = _state.copyWith(isPatched: !_state.isPatched);
  }

  void setUserInput(String input) {
    _state = _state.copyWith(userInput: input);
  }

  /// ni — execute next single assembly instruction
  void handleNextInstruction() {
    if (_execWaiting || _isTerminal) return;

    // Auto-recover: reset to first real instruction if index is invalid
    if (_pendingAsmIdx < 0 || _pendingAsmIdx >= _asmFlat.length) {
      _pendingAsmIdx = _skipToNextReal(0);
      _state = _state.copyWith(activeAsmInstrIdx: _pendingAsmIdx);
    }
    if (_asmFlat.isEmpty || _pendingAsmIdx >= _asmFlat.length) {
      _addConsole('[DBG] asmFlat empty or still out of range (len=${_asmFlat.length}), pending=${_pendingAsmIdx}', false);
      return;
    }

    final toExec = _asmFlat[_pendingAsmIdx];
    final mnem = _extractMnem(toExec.rawText.toLowerCase()) ?? 'null';
    _addConsole('[ni:${toExec.address}] mnem=$mnem → ${toExec.rawText.trim()}', false);
    _lastExecutedAddr = toExec.address;
    int nextIdx = _dispatchAndGetNext(toExec);
    _updateRegistersForInstr(toExec.rawText);

    if (!_execWaiting && !_isTerminal) {
      nextIdx = _skipToNextReal(nextIdx);
      _pendingAsmIdx = nextIdx;
      _state = _state.copyWith(activeAsmInstrIdx: nextIdx);
      _syncSourceToAsm();
      _emitSimRegs();
    }
    _recordSnapshot(
      _state.statusTitle,
      _pendingAsmIdx < _asmFlat.length ? _asmFlat[_pendingAsmIdx].address : '?',
    );
  }

  /// n — execute all instructions for the current C source line
  void handleNextStep() {
    if (_execWaiting || _isTerminal) return;
    // Auto-recover if index is uninitialized
    if (_pendingAsmIdx < 0) {
      _pendingAsmIdx = _skipToNextReal(0);
      _state = _state.copyWith(activeAsmInstrIdx: _pendingAsmIdx);
    }
    if (_pendingAsmIdx >= _asmFlat.length) return;

    final curSrc = _addrToSrcLine[_asmFlat[_pendingAsmIdx].address];
    do {
      final toExec = _asmFlat[_pendingAsmIdx];
      final mnemBefore = _extractMnem(toExec.rawText.toLowerCase());
      _lastExecutedAddr = toExec.address;
      int nextIdx = _dispatchAndGetNext(toExec);
      _updateRegistersForInstr(toExec.rawText);
      if (_execWaiting || _isTerminal) break;

      nextIdx = _skipToNextReal(nextIdx);
      if (nextIdx >= _asmFlat.length) { _isTerminal = true; break; }

      _pendingAsmIdx = nextIdx;
      _state = _state.copyWith(activeAsmInstrIdx: nextIdx);

      final newSrc = _addrToSrcLine[_asmFlat[_pendingAsmIdx].address];
      if (newSrc != null && newSrc != curSrc) break;
      if (newSrc == null && curSrc == null && mnemBefore == 'ret') break;
    } while (!_execWaiting && !_isTerminal);

    if (!_execWaiting) {
      _syncSourceToAsm();
      _emitSimRegs();
    }
    _recordSnapshot(
      _state.statusTitle,
      _pendingAsmIdx < _asmFlat.length ? _asmFlat[_pendingAsmIdx].address : '?',
    );
  }

  /// Submit user input (Level 1 / 2A — read() prompt)
  void submitInput(String input) {
    _state = _state.copyWith(getsReached: false, userInput: input);
    final patched = _state.isPatched;
    final lvlId = _level?.id ?? '';

    var processed = input;
    if (patched) {
      final max = lvlId == '2a' ? 15 : 16;
      if (processed.length > max) processed = processed.substring(0, max);
    }

    var newStack = _deepCopyStack(_state.stack);
    final bufferStartIdx = _findBlockIndex(newStack, 'buff[0');
    var currentIdx = bufferStartIdx;
    var hasOverflowed = false;
    var remaining = processed;

    while (remaining.isNotEmpty && currentIdx >= 0) {
      final block = newStack[currentIdx];
      final chunk = remaining.length >= 8 ? remaining.substring(0, 8) : remaining;
      remaining = remaining.length >= 8 ? remaining.substring(8) : '';
      block.value = chunk;
      block.lastInstruction = 'read() payload';

      final isBufferSlot = block.label.startsWith('buff');
      if (isBufferSlot) {
        block.type = StackBlock.typeFilled;
      } else {
        hasOverflowed = true;
        if (block.label.contains('Saved')) {
          block.type = StackBlock.typeJunk;
        } else if (block.label.contains('Return') || block.label.contains('Ret')) {
          block.type = StackBlock.typeDanger;
        } else {
          block.type = StackBlock.typeJunk;
        }
      }

      // Move to block above (lower index = higher address)
      if (currentIdx > 0) currentIdx--; else break;
    }

    _state = _state.copyWith(stack: newStack);
    _updateHexDump();
    _execWaiting = false;

    String title, desc, sType;
    if (patched) {
      title = 'Input Restricted';
      desc = 'Defense patch active — input was truncated to ${lvlId == '2a' ? 15 : 16} bytes.';
      sType = 'info';
    } else if (hasOverflowed) {
      title = 'Buffer Overflowed!';
      desc = 'Input exceeded the buffer boundary and is corrupting the stack!';
      sType = 'danger';
    } else {
      title = 'Input accepted';
      desc = 'Buffer filled safely (${input.length} bytes).';
      sType = 'info';
    }

    _state = _state.copyWith(
      statusTitle: title,
      statusDesc: desc,
      statusType: sType,
      waitingForInput: false,
    );

    // Resume execution — advance to next instruction after call
    _pendingAsmIdx = _skipToNextReal(_pendingAsmIdx + 1);
    _state = _state.copyWith(activeAsmInstrIdx: _pendingAsmIdx);
    _syncSourceToAsm();
    _emitSimRegs();
    _recordSnapshot(title, _pendingAsmIdx < _asmFlat.length ? _asmFlat[_pendingAsmIdx].address : '?');
    _state = _state.copyWith(isTerminal: false);
    _isTerminal = false;
  }

  /// Submit exploit payload for Level 2B
  void submitPayload(int junkStart, int junkEnd, int targetIdx, String targetAddress) {
    var stack = _deepCopyStack(_state.stack);
    // Mark junk range
    for (int i = junkEnd; i >= junkStart; i--) {
      if (i >= 0 && i < stack.length) {
        stack[i].value = 'JUNK';
        stack[i].type = StackBlock.typeJunk;
      }
    }
    // Set target
    if (targetIdx >= 0 && targetIdx < stack.length) {
      stack[targetIdx].value = targetAddress;
      stack[targetIdx].type = StackBlock.typeTarget;
      stack[targetIdx].lastInstruction = 'payload builder';
    }

    _state = _state.copyWith(
      stack: stack,
      statusTitle: 'Payload injected!',
      statusDesc: 'Return address overwritten with $targetAddress.',
      statusType: 'warn',
      getsReached: false,
      waitingForInput: false,
    );
    _updateHexDump();
    _execWaiting = false;
    _pendingAsmIdx = _skipToNextReal(_pendingAsmIdx + 1);
    _state = _state.copyWith(activeAsmInstrIdx: _pendingAsmIdx);
    _syncSourceToAsm();
    _emitSimRegs();
    _recordSnapshot('Payload injected', _pendingAsmIdx < _asmFlat.length ? _asmFlat[_pendingAsmIdx].address : '?');
    _isTerminal = false;
  }

  /// Step 1: Save the ROP chain gadget values (does NOT write to stack yet).
  /// Sets ropChainReady = true so the payload builder can proceed.
  void saveRopChain(Map<String, String> gadgetValues) {
    _savedRopValues = Map.from(gadgetValues);
    _ropRax = gadgetValues['pop_rax'] ?? '0x0';
    _ropRdi = gadgetValues['pop_rdi'] ?? '0x0';
    _ropRsi = gadgetValues['pop_rsi'] ?? '0x0';
    _ropRdx = gadgetValues['pop_rdx'] ?? '0x0';
    _state = _state.copyWith(
      ropChainReady: true,
      statusTitle: 'ROP Chain Saved',
      statusDesc: 'Chain of ${gadgetValues.length} gadget(s) saved. '
          'Now open the Payload Builder to select junk range and ROP slot.',
      statusType: 'info',
    );
  }

  /// Step 2: Write the saved ROP chain into the stack at user-selected slots.
  void submitRopChain(int junkStart, int junkEnd, int ropSlotStart) {
    final gadgetValues = _savedRopValues;

    // Build ordered address/value list
    final addresses = <String>[];
    for (final g in _level!.ropGadgets) {
      if (g.alias == '/bin/sh') continue;
      if (!gadgetValues.containsKey(g.alias)) continue;
      addresses.add(g.address);
      if (g.alias.startsWith('pop_')) {
        addresses.add(gadgetValues[g.alias] ?? '0x0');
      }
    }

    _ropPayload = addresses;
    _ropPayloadPos = 0;
    _ropEspIdx = _state.espIndex - 1;

    // Write payload into stack using user-selected slots
    // Level 3 chains can be longer than currently modeled stack headroom
    // (from selected return slot up to index 0). Expand safely if needed.
    var stack = _deepCopyStack(_state.stack);
    var espIndex = _state.espIndex;
    var ebpIndex = _state.ebpIndex;
    final slotsAvailable = ropSlotStart + 1;
    final neededSlots = addresses.length;
    final extraHeadSlots =
        neededSlots > slotsAvailable ? (neededSlots - slotsAvailable) : 0;

    if (extraHeadSlots > 0) {
      final currentTopAddr =
          stack.isNotEmpty ? _parseAddr(stack.first.address) : 0x7fff8;
      final padding = List<StackBlock>.generate(extraHeadSlots, (i) {
        final addr = currentTopAddr + ((extraHeadSlots - i) * 8);
        return StackBlock(
          address: _formatHex(addr),
          label: 'rop_pad[$i]',
          value: '0x0',
          type: StackBlock.typeNeutral,
        );
      });
      stack = [...padding, ...stack];
      ropSlotStart += extraHeadSlots;
      junkStart += extraHeadSlots;
      junkEnd += extraHeadSlots;
      espIndex += extraHeadSlots;
      ebpIndex += extraHeadSlots;
    }

    // Mark junk slots
    for (int i = junkStart; i <= junkEnd && i < stack.length; i++) {
      stack[i].value = 'AAAA';
      stack[i].type = StackBlock.typeJunk;
      stack[i].lastInstruction = 'rop junk';
    }

    // Write chain addresses starting at ropSlotStart (decreasing index = up in memory)
    int slot = ropSlotStart;
    for (int ai = 0; ai < addresses.length && slot >= 0; ai++) {
      stack[slot].value = addresses[ai];
      stack[slot].type = StackBlock.typeTarget;
      stack[slot].lastInstruction = 'rop chain';
      slot--;
    }

    _state = _state.copyWith(
      stack: stack,
      espIndex: espIndex,
      ebpIndex: ebpIndex,
      statusTitle: 'ROP chain loaded!',
      statusDesc: 'Gadget chain injected into the stack. Step through to execute it.',
      statusType: 'warn',
      getsReached: false,
      waitingForInput: false,
    );
    _updateHexDump();
    _execWaiting = false;
    _pendingAsmIdx = _skipToNextReal(_pendingAsmIdx + 1);
    _state = _state.copyWith(activeAsmInstrIdx: _pendingAsmIdx);
    _syncSourceToAsm();
    _emitSimRegs();
    _recordSnapshot('ROP chain loaded', _pendingAsmIdx < _asmFlat.length ? _asmFlat[_pendingAsmIdx].address : '?');
    _isTerminal = false;
  }

  /// Rewind simulation to a snapshot (time-travel)
  void rewindToSnapshot(RegisterSnapshot snap) {
    // Restore all visible state
    _state = _state.copyWith(
      step: snap.simStep,
      activeLineIndex: snap.codeLine,
      stack: _deepCopyStack(snap.simStack),
      espIndex: snap.simEsp,
      ebpIndex: snap.simEbp,
      statusTitle: snap.simStatusTitle,
      statusDesc: snap.simStatusDesc,
      statusType: snap.statusType,
      consoleOut: List.from(snap.simConsole),
      simRegs: Map.from(snap.simRegs),
      activeSnapshotIndex: snap.stepIndex,
      snapshots: List<RegisterSnapshot>.from(_state.snapshots).sublist(0, snap.stepIndex + 1),
      isTerminal: false,
    );
    _snapshotCount = snap.stepIndex + 1;

    // Restore internal engine execution state — critical for ni/n to work correctly
    _pendingAsmIdx = snap.simPendingAsmIdx >= 0
        ? snap.simPendingAsmIdx
        : _skipToNextReal(0);
    _execWaiting = snap.simExecWaiting;
    _isTerminal = false;

    // Sync activeAsmInstrIdx in state so the code viewer highlights correctly
    _state = _state.copyWith(
      activeAsmInstrIdx: _pendingAsmIdx,
      waitingForInput: _execWaiting,
    );

    _updateHexDump();
  }

  /// Clear the pending toast after UI reads it
  void clearToast() {
    _state = _state.copyWith(clearToast: true);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Instruction Dispatch Engine
  // ─────────────────────────────────────────────────────────────────────────

  int _dispatchAndGetNext(AsmLine instr) {
    final raw = instr.rawText.toLowerCase();

    // CALL
    if (instr.callTarget != null || raw.contains('call ')) {
      return _handleCall(instr, raw);
    }

    final mnem = _extractMnem(raw);
    if (mnem == null) return _pendingAsmIdx + 1;

    switch (mnem) {
      case 'push':
        if (raw.contains('rbp') || raw.contains('ebp')) _simulatePushRbp();
        break;
      case 'mov':
        if (raw.contains('rbp,rsp') || raw.contains('ebp,esp')) {
          _simulateMovRbpRsp();
        } else if (raw.contains('qword ptr [rbp-') || raw.contains('dword ptr [rbp-')) {
          _simulateMovToStack(raw);
        }
        break;
      case 'sub':
        if (raw.contains('rsp,') || raw.contains('esp,')) _simulateSubRsp(raw);
        break;
      case 'leave':
        _simulateLeave();
        break;
      case 'ret':
        return _simulateRet();
      case 'pop':
        _simulatePop(raw);
        break;
      case 'syscall':
        _simulateSyscall();
        _isTerminal = true;
        return _pendingAsmIdx;
      default:
        break;
    }
    return _pendingAsmIdx + 1;
  }

  int _handleCall(AsmLine instr, String raw) {
    // gets@plt  → pause and show payload dialog
    if (raw.contains('gets@plt') || raw.contains('<gets')) {
      _execWaiting = true;
      _state = _state.copyWith(
        waitingForInput: true,
        getsReached: true,
        step: 4,
        statusTitle: 'gets() — Awaiting Input',
        statusType: 'warn',
        statusDesc: 'gets() reads from stdin with NO bounds check. Choose how to respond.',
      );
      return _pendingAsmIdx;
    }

    // read@plt → pause for simple input
    if (raw.contains('read@plt') || raw.contains('<read')) {
      _execWaiting = true;
      final lvlId = _level?.id ?? '';
      var desc = _state.isPatched
          ? 'read() is bounded to 16 bytes — safe input only.'
          : 'read() reads up to 100 bytes into a 16-byte buffer. Overflow it!';
      if (lvlId == '2a') {
        desc = _state.isPatched
            ? 'read() bounded to 15 bytes + null-terminator — secret protected.'
            : 'read() reads 16 bytes with no null-terminator. Fill exactly 16 to leak the secret!';
      }
      _state = _state.copyWith(
        waitingForInput: true,
        step: 4,
        statusTitle: 'read() — Awaiting Input',
        statusType: 'warn',
        statusDesc: desc,
      );
      return _pendingAsmIdx;
    }

    // puts@plt
    if (raw.contains('puts@plt') || raw.contains('<puts')) {
      _simulatePuts(raw);
      return _pendingAsmIdx + 1;
    }

    // printf@plt
    if (raw.contains('printf@plt') || raw.contains('<printf')) {
      _simulatePrintf();
      return _pendingAsmIdx + 1;
    }

    // win() for Level 2B
    final target = instr.callTarget ?? '';
    if (raw.contains('<win>') || target == '400476') {
      _simulateWin2b();
      _isTerminal = true;
      return _pendingAsmIdx;
    }

    // Local function call
    final targetIdx = _addrToAsmIdx[target];
    if (targetIdx != null) {
      _simulateCallPushReturnAddr();
      final funcName = _extractFuncName(raw);
      _state = _state.copyWith(
        statusTitle: 'call $funcName',
        statusDesc: 'CPU pushes return address onto the stack and jumps to $funcName.',
        step: 2,
      );
      return _skipToNextReal(targetIdx);
    }

    return _pendingAsmIdx + 1;
  }

  // ── Instruction simulation helpers ────────────────────────────────────────

  void _simulatePushRbp() {
    final newEsp = _state.espIndex + 1;
    final s = _deepCopyStack(_state.stack);
    if (newEsp < s.length) {
      final b = s[newEsp];
      final allZeroHex = RegExp(r'^0[xX]0+$').hasMatch(b.value);
      final isEmpty = b.type == StackBlock.typeNeutral ||
          allZeroHex || b.value == '0x...' || b.value.isEmpty;
      if (isEmpty) {
        b.value = _formatHex(_parseRbpValue(s));
        b.type = StackBlock.typeSafe;
        b.lastInstruction = _currentAsmText();
        _state = _state.copyWith(stack: s);
        _updateHexDump();
      }
    }
    _state = _state.copyWith(
      espIndex: newEsp,
      statusTitle: 'push rbp',
      statusDesc: "Saves caller's base pointer on the stack. RSP decrements by 8.",
    );
  }

  void _simulateMovRbpRsp() {
    _state = _state.copyWith(
      ebpIndex: _state.espIndex,
      statusTitle: 'mov rbp, rsp',
      statusDesc: 'Sets the new frame pointer equal to the current stack pointer.',
    );
  }

  void _simulateSubRsp(String raw) {
    final m = RegExp(r',0[xX]([0-9a-fA-F]+)').firstMatch(raw);
    if (m != null) {
      final n = int.parse(m.group(1)!, radix: 16);
      final blocks = n ~/ 8;
      final newEsp = (_state.espIndex + blocks).clamp(0, _safeStackMax());
      _state = _state.copyWith(
        espIndex: newEsp,
        statusTitle: 'sub rsp, 0x${n.toRadixString(16)}',
        statusDesc: 'Allocates $n bytes on the stack for local variables.',
      );
    }
  }

  void _simulateMovToStack(String raw) {
    final offM = RegExp(r'\[rbp-0x([0-9a-fA-F]+)\]').firstMatch(raw);
    if (offM == null) return;
    final byteOffset = int.parse(offM.group(1)!, radix: 16);
    final slotOffset = byteOffset ~/ 8;
    final slotIdx = _state.ebpIndex + slotOffset;

    final s = _deepCopyStack(_state.stack);
    if (slotIdx < 0 || slotIdx >= s.length) return;
    final slot = s[slotIdx];

    String? value;
    if (raw.contains(',rax')) value = _decodeMovAbsValue(_simGPR['rax'] ?? 0);
    else if (raw.contains(',rdx')) value = _decodeMovAbsValue(_simGPR['rdx'] ?? 0);
    else if (raw.contains(',rcx')) value = _decodeMovAbsValue(_simGPR['rcx'] ?? 0);
    else if (raw.contains(',rbx')) value = _decodeMovAbsValue(_simGPR['rbx'] ?? 0);
    else if (raw.contains(',rsi')) value = _decodeMovAbsValue(_simGPR['rsi'] ?? 0);
    else if (raw.contains(',rdi')) value = _decodeMovAbsValue(_simGPR['rdi'] ?? 0);
    if (value == null) return;

    slot.value = value;
    slot.lastInstruction = _currentAsmText();
    if (slot.type != StackBlock.typeWarn) slot.type = StackBlock.typeSafe;
    _state = _state.copyWith(
      stack: s,
      statusTitle: 'Stack ← "$value"',
      statusDesc: 'Writing 8 bytes to [rbp-0x${byteOffset.toRadixString(16)}] = slot "${slot.label}".',
    );
    _updateHexDump();
  }

  String _decodeMovAbsValue(int val) {
    final sb = StringBuffer();
    var allPrintable = true;
    for (int i = 0; i < 8; i++) {
      final b = (val >> (i * 8)) & 0xFF;
      if (b == 0) break;
      if (b < 0x20 || b > 0x7e) { allPrintable = false; break; }
      sb.writeCharCode(b);
    }
    return allPrintable && sb.isNotEmpty ? sb.toString() : '0x${val.toRadixString(16)}';
  }

  void _simulateLeave() {
    final ebp = _state.ebpIndex;
    _state = _state.copyWith(
      espIndex: ebp - 1,
      statusTitle: 'leave — restore stack frame',
      statusDesc: 'mov rsp, rbp restores RSP. pop rbp restores the caller\'s frame pointer.',
      step: 5,
    );
  }

  int _simulateRet() {
    final s = _state.stack;
    final esp = _state.espIndex;
    if (s.isEmpty || esp < 0 || esp >= s.length) {
      _isTerminal = true;
      return _pendingAsmIdx;
    }

    final retBlock = s[esp];
    final retVal = retBlock.value;
    _state = _state.copyWith(espIndex: esp - 1);

    String retAddr = retVal
        .replaceAll('0x', '').replaceAll('0X', '')
        .replaceAll(RegExp(r'[^0-9a-fA-F]'), '')
        .toLowerCase()
        .trimLeft()
        .replaceFirst(RegExp(r'^0+(?=[0-9a-fA-F])'), '');

    final targetFlatIdx = _addrToAsmIdx[retAddr];

    if (targetFlatIdx == null) {
      final corrupted = retBlock.type == StackBlock.typeDanger ||
          retBlock.type == StackBlock.typeJunk ||
          retBlock.type == StackBlock.typeTarget ||
          (!retVal.startsWith('0x') && retVal.isNotEmpty);
      if (corrupted || !retVal.startsWith('0x')) {
        _ropRip = retVal;
        _emitSimRegs();
        _addConsole('', false);
        _addConsole('Program received signal SIGSEGV, Segmentation fault.', false);
        _addConsole('RIP = 0x$retAddr', false);
        _addConsole('[1]    killed  Segmentation fault', false);
        _state = _state.copyWith(
          statusTitle: 'Segmentation Fault!',
          statusType: 'danger',
          statusDesc: 'RIP → 0x$retAddr ("$retVal") is not a valid code address.\nSIGSEGV — process killed.',
          step: 100,
        );
        if (_level?.goal == 'CRASH') _triggerWin(false);
        _isTerminal = true;
        return _pendingAsmIdx;
      }
      _isTerminal = true;
      _state = _state.copyWith(step: 100, statusTitle: 'Program exited');
      return _pendingAsmIdx;
    }

    if (_level?.id == '2b' && retAddr == '400476') {
      _state = _state.copyWith(
        statusTitle: 'RIP hijacked → win()!',
        statusType: 'success',
        statusDesc: 'leave ; ret loaded 0x400476. Jumping to win().',
        step: 6,
      );
    } else if (_ropPayload.isNotEmpty) {
      _state = _state.copyWith(
        statusTitle: 'ret → ROP gadget $retVal',
        statusType: 'info',
        step: 6,
      );
    } else {
      _state = _state.copyWith(
        statusTitle: 'ret — returned to caller',
        statusType: 'success',
        statusDesc: 'Stack frame cleaned up. RIP = $retVal.',
        step: 7,
      );
    }
    return _skipToNextReal(targetFlatIdx);
  }

  void _simulatePop(String raw) {
    final s = _state.stack;
    final esp = _state.espIndex;
    final val = (s.isNotEmpty && esp >= 0 && esp < s.length) ? s[esp].value : '0x0';
    _state = _state.copyWith(espIndex: esp - 1);

    String regName = '';
    if (raw.contains('rax')) { _ropRax = val; regName = 'RAX'; }
    else if (raw.contains('rdi')) { _ropRdi = val; regName = 'RDI'; }
    else if (raw.contains('rsi')) { _ropRsi = val; regName = 'RSI'; }
    else if (raw.contains('rdx')) { _ropRdx = val; regName = 'RDX'; }

    if (regName.isNotEmpty) {
      _state = _state.copyWith(
        statusTitle: 'pop ${regName.toLowerCase()}',
        statusType: 'info',
        statusDesc: '$regName ← $val',
      );
      if (_ropPayload.isNotEmpty) {
        _ropHopCount++;
        _addConsole('', false);
        _addConsole('────────────────────────────[ Gadget #$_ropHopCount ]────────────────────────────', false);
        _addConsole(' ►  ${_asmFlat[_pendingAsmIdx].address}    pop ${regName.toLowerCase()}', false);
        _addConsole('   $regName ← $val', false);
      }
    }
  }

  void _simulateSyscall() {
    final addr = _pendingAsmIdx < _asmFlat.length
        ? '0x${_asmFlat[_pendingAsmIdx].address}'
        : '0x40048e';
    _execSyscall(addr, _deepCopyStack(_state.stack));
    _state = _state.copyWith(step: 100);
  }

  void _execSyscall(String addr, List<StackBlock> stk) {
    int? parseWord(String v) {
      final s = v.trim().toLowerCase();
      if (s.isEmpty) return null;
      if (s.contains('/bin/sh') || s.contains('binsh')) return 0x403010;

      final hexMatch = RegExp(r'0x[0-9a-f]+').firstMatch(s);
      if (hexMatch != null) {
        return int.tryParse(hexMatch.group(0)!.substring(2), radix: 16);
      }

      final decMatch = RegExp(r'\b\d+\b').firstMatch(s);
      if (decMatch != null) {
        return int.tryParse(decMatch.group(0)!);
      }

      // Fallback: bare hex token without 0x prefix.
      final bareHex = RegExp(r'\b[0-9a-f]+\b').firstMatch(s);
      if (bareHex != null) {
        return int.tryParse(bareHex.group(0)!, radix: 16);
      }
      return null;
    }

    final rax = parseWord(_ropRax);
    final rdi = parseWord(_ropRdi);
    final rsi = parseWord(_ropRsi);
    final rdx = parseWord(_ropRdx);

    _addConsole(
      '[DBG syscall] raw: RAX=$_ropRax RDI=$_ropRdi RSI=$_ropRsi RDX=$_ropRdx | '
      'parsed: rax=$rax rdi=$rdi rsi=$rsi rdx=$rdx',
      false,
    );

    final isExecve = rax == 59 &&
        rdi == 0x403010 &&
        (rsi ?? -1) == 0 &&
        (rdx ?? -1) == 0;

    if (isExecve) {
      _addConsole('', false);
      _addConsole('execve("/bin/sh", 0, 0) executed!', true);
      _addConsole(r'$ ', true);
      _addConsole('uid=1000 gid=1000 groups=1000', true);
      _addConsole('SHELL OBTAINED — flag{r0p_g4dget_m4st3r}', true);
      _state = _state.copyWith(
        statusTitle: 'Shell obtained!',
        statusType: 'success',
        statusDesc: 'execve("/bin/sh", 0, 0) called. ROP chain complete. Execution terminated.',
        isTerminal: true,
      );
      _triggerWin(true);
    } else {
      _addConsole('syscall: bad arguments — shell not obtained.', false);
      _addConsole('  RAX=$_ropRax  RDI=$_ropRdi  RSI=$_ropRsi  RDX=$_ropRdx', false);
      _addConsole('  parsed: rax=$rax rdi=$rdi rsi=$rsi rdx=$rdx', false);
      _state = _state.copyWith(
        statusTitle: 'Syscall — Bad Args',
        statusType: 'danger',
        statusDesc: 'Syscall fired but arguments are wrong. RAX must be 0x3b, RDI=0x403010, RSI=0, RDX=0.',
      );
    }
  }

  void _simulateCallPushReturnAddr() {
    final s = _deepCopyStack(_state.stack);
    final newEsp = _state.espIndex + 1;
    final retInstIdx = _skipToNextReal(_pendingAsmIdx + 1);
    final retAddr = retInstIdx < _asmFlat.length ? '0x${_asmFlat[retInstIdx].address}' : '0x0';
    if (newEsp < s.length) {
      s[newEsp].value = retAddr;
      s[newEsp].type = StackBlock.typeSafe;
      s[newEsp].lastInstruction = _currentAsmText();
    }
    _state = _state.copyWith(stack: s, espIndex: newEsp);
    _updateHexDump();
  }

  void _simulatePuts(String raw) {
    final curAddr = _asmFlat[_pendingAsmIdx].address;
    final srcLine = _addrToSrcLine[curAddr];
    if (srcLine != null && _level != null && srcLine < _level!.code.length) {
      final lineText = _level!.code[srcLine].text;
      final quoted = _extractQuoted(lineText);
      if (quoted.isNotEmpty) {
        _addConsole(quoted, false);
        _state = _state.copyWith(
          statusTitle: 'puts()',
          statusDesc: 'Printed: "$quoted"',
        );
        return;
      }
    }
    if (_level?.id == '2b') {
      _addConsole('SHELL OBTAINED — flag{ctrl_flow_h1jack}', false);
      _state = _state.copyWith(
        statusTitle: 'puts() — flag printed!',
        statusType: 'success',
      );
      _triggerWin(true);
      _isTerminal = true;
    }
  }

  void _simulatePrintf() {
    final input = _state.userInput;
    final patched = _state.isPatched;
    final maxLen = _level?.id == '2a' ? (patched ? 15 : 16) : 100;
    final out = input.length > maxLen ? input.substring(0, maxLen) : input;
    _addConsole(out, false);
    final shouldLeak = !patched && input.length >= 16;
    if (_level?.id == '2a' && shouldLeak) {
      _addConsole('SUPER_SECRET_KEY', true);
    }
    _state = _state.copyWith(
      statusTitle: 'printf()',
      statusDesc: shouldLeak
          ? 'printf read past buff into secret_key — data leaked!'
          : 'printf printed the output.',
      statusType: shouldLeak ? 'success' : 'info',
    );
    // Show win toast for data leak but do NOT terminate —
    // let execution continue through nop/leave/ret naturally.
    if (_level?.id == '2a' && shouldLeak) {
      _state = _state.copyWith(
        pendingToast: ('🎉 ${_level?.successTitle ?? 'Secret leaked!'}', true),
      );
      // Do NOT call _triggerWin / set _isTerminal here.
    }
  }


  void _simulateWin2b() {
    _addConsole('SHELL OBTAINED — flag{ctrl_flow_h1jack}', false);
    _state = _state.copyWith(
      statusTitle: 'win() — RIP hijack succeeded!',
      statusType: 'success',
      statusDesc: 'Execution jumped to win(). Flag printed!',
      pendingToast: ('💥 Shell obtained! flag{ctrl_flow_h1jack}', false),
      step: 100,
    );
    _triggerWin(true);
  }

  void _triggerWin(bool isSuccess) {
    _state = _state.copyWith(
      step: 100,
      statusType: isSuccess ? 'success' : 'danger',
      pendingToast: (
        isSuccess ? '🎉 ${_level?.successTitle ?? 'Success!'}' : '💥 Crash Successful!',
        isSuccess
      ),
    );
    _isTerminal = true;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Register simulation helpers (visual only)
  // ─────────────────────────────────────────────────────────────────────────

  void _initSimGPR() {
    for (final r in ['rax','rbx','rcx','rdx','rsi','rdi','rsp','rbp','rip']) {
      _simGPR[r] = 0;
    }
    if (_level != null && _level!.initialStack.isNotEmpty) {
      final topAddr = _parseAddr(_level!.initialStack.first.address);
      _simGPR['rsp'] = topAddr + 8;
      _simGPR['rbp'] = topAddr + 8;
    }
  }

  void _updateRegistersForInstr(String raw) {
    final lo = raw.toLowerCase();
    final mnem = _extractMnem(lo);
    if (mnem == null) return;
    final ops = _extractOperands(lo);

    switch (mnem) {
      case 'mov': case 'movzx': case 'movsx': case 'movabs':
        if (ops == null) break;
        final p = ops.split(',');
        if (p.length >= 2) {
          final dst = _canonReg(p[0].trim());
          if (dst != null) _setGPR(dst, _resolveOp(p.sublist(1).join(',').trim()));
        }
        break;
      case 'lea':
        if (ops == null) break;
        final p = ops.split(',');
        if (p.length >= 2) {
          final dst = _canonReg(p[0].trim());
          if (dst != null) _setGPR(dst, _resolveMemAddr(p.sublist(1).join(',').trim()));
        }
        break;
      case 'xor':
        if (ops == null) break;
        final p = ops.split(',');
        if (p.length >= 2) {
          final d = _canonReg(p[0].trim()), s = _canonReg(p[1].trim());
          if (d != null && d == s) _setGPR(d, 0);
          else if (d != null) _setGPR(d, _gpr(d) ^ _resolveOp(p[1].trim()));
        }
        break;
      case 'add':
        if (ops == null) break;
        final p = ops.split(',');
        if (p.length >= 2) { final d = _canonReg(p[0].trim()); if (d != null) _setGPR(d, _gpr(d) + _resolveOp(p[1].trim())); }
        break;
      case 'sub':
        if (ops == null) break;
        final p = ops.split(',');
        if (p.length >= 2) { final d = _canonReg(p[0].trim()); if (d != null) _setGPR(d, _gpr(d) - _resolveOp(p[1].trim())); }
        break;
      case 'push':
        _setGPR('rsp', _gpr('rsp') - 8);
        break;
      case 'pop':
        _setGPR('rsp', _gpr('rsp') + 8);
        break;
      case 'leave':
        _setGPR('rsp', _gpr('rbp') + 8);
        break;
      case 'call':
        _setGPR('rsp', _gpr('rsp') - 8);
        break;
      case 'ret':
        _setGPR('rsp', _gpr('rsp') + 8);
        break;
      default:
        break;
    }
  }

  void _emitSimRegs() {
    if (_pendingAsmIdx >= 0 && _pendingAsmIdx < _asmFlat.length) {
      final addr = _asmFlat[_pendingAsmIdx].address;
      try { _simGPR['rip'] = int.parse(addr, radix: 16); } catch (_) {}
    }

    final m = <String, String>{
      'RAX': _fmtGPR('rax'), 'RBX': _fmtGPR('rbx'), 'RCX': _fmtGPR('rcx'),
      'RDX': _fmtGPR('rdx'), 'RSI': _fmtGPR('rsi'), 'RDI': _fmtGPR('rdi'),
      'RSP': _fmtGPR('rsp'), 'RBP': _fmtGPR('rbp'),
      'RIP': _ropRip != 'main' ? _ropRip : _fmtGPR('rip'),
    };

    if (_lastExecutedAddr != null) {
      final overrides = _instrRegOverrides[_lastExecutedAddr!.toLowerCase()];
      if (overrides != null) m.addAll(overrides);
    }

    _state = _state.copyWith(simRegs: m);
  }

  String _fmtGPR(String reg) => '0x${(_simGPR[reg] ?? 0).toRadixString(16)}';

  String? _canonReg(String name) {
    final n = name.replaceAll(RegExp(r'\s.*'), '');
    switch (n) {
      case 'rax': case 'eax': case 'ax': case 'al': case 'ah': return 'rax';
      case 'rbx': case 'ebx': case 'bx': case 'bl': case 'bh': return 'rbx';
      case 'rcx': case 'ecx': case 'cx': case 'cl': case 'ch': return 'rcx';
      case 'rdx': case 'edx': case 'dx': case 'dl': case 'dh': return 'rdx';
      case 'rsi': case 'esi': case 'si': case 'sil': return 'rsi';
      case 'rdi': case 'edi': case 'di': case 'dil': return 'rdi';
      case 'rsp': case 'esp': case 'sp': case 'spl': return 'rsp';
      case 'rbp': case 'ebp': case 'bp': case 'bpl': return 'rbp';
      case 'rip': case 'eip': return 'rip';
      default: return null;
    }
  }

  int _resolveOp(String op) {
    op = op.trim().replaceFirst(RegExp(r'\s*#.*$'), '').trim();
    final reg = _canonReg(op);
    if (reg != null) return _gpr(reg);
    if (op.startsWith('0x')) {
      try { return int.parse(op.substring(2), radix: 16); } catch (_) {}
    }
    if (op.startsWith('-0x')) {
      try { return -int.parse(op.substring(3), radix: 16); } catch (_) {}
    }
    try { return int.parse(op); } catch (_) {}
    if (op.startsWith('[')) return _resolveMemAddr(op);
    return 0;
  }

  int _resolveMemAddr(String op) {
    op = op.trim().replaceFirst(RegExp(r'\s*#.*$'), '').trim();
    if (op.startsWith('[') && op.endsWith(']')) op = op.substring(1, op.length - 1).trim();
    final plus = op.lastIndexOf('+');
    final minus = op.lastIndexOf('-');
    final split = plus > minus ? plus : (minus > 0 ? minus : -1);
    if (split > 0) {
      final baseS = op.substring(0, split).trim();
      var offS = op.substring(split).trim();
      final baseReg = _canonReg(baseS);
      final base = baseReg != null ? _gpr(baseReg) : 0;
      int off = 0;
      try {
        offS = offS.replaceFirst('+', '').trim();
        if (offS.startsWith('-0x')) off = -int.parse(offS.substring(3), radix: 16);
        else if (offS.startsWith('0x')) off = int.parse(offS.substring(2), radix: 16);
        else off = int.parse(offS);
      } catch (_) {}
      return base + off;
    }
    final reg = _canonReg(op);
    return reg != null ? _gpr(reg) : 0;
  }

  String? _extractOperands(String rawLower) {
    final colon = rawLower.indexOf(':');
    if (colon < 0) return null;
    // Do NOT trim — keep leading whitespace so parts[1] is mnemonic+operands
    final rest = rawLower.substring(colon + 1);
    final parts = rest.split(RegExp(r'\s{2,}'));
    if (parts.length < 2) return null;

    // parts[1] is either the mnemonic+operands (no hex bytes) or hex bytes
    String instrPart = parts[1].trim();
    // If pure hex, step to parts[2]
    if (RegExp(r'^[0-9a-fA-F ]+$').hasMatch(instrPart) && parts.length >= 3) {
      instrPart = parts[2].trim();
    }
    if (instrPart.isEmpty) return null;

    final sp = instrPart.indexOf(' ');
    if (sp < 0) return null; // single-token instruction, no operands
    var ops = instrPart.substring(sp + 1).trim();
    ops = ops.replaceFirst(RegExp(r'\s*[#←].*$'), '').trim();
    return ops.isEmpty ? null : ops;
  }


  int _gpr(String reg) => _simGPR[reg] ?? 0;
  void _setGPR(String reg, int val) => _simGPR[reg] = val;

  // ─────────────────────────────────────────────────────────────────────────
  // Snapshot / Time-travel
  // ─────────────────────────────────────────────────────────────────────────

  void _recordSnapshot(String label, String addr) {
    final snap = RegisterSnapshot(
      stepIndex: _snapshotCount++,
      label: label,
      rip: _state.simRegs['RIP'] ?? 'main',
      rsp: _state.simRegs['RSP'] ?? '—',
      rbp: _state.simRegs['RBP'] ?? '—',
      statusType: _state.statusType,
      codeLine: _state.activeLineIndex,
      simStep: _state.step,
      simStack: _deepCopyStack(_state.stack),
      simEsp: _state.espIndex,
      simEbp: _state.ebpIndex,
      simStatusTitle: _state.statusTitle,
      simStatusDesc: _state.statusDesc,
      simConsole: List.from(_state.consoleOut),
      simRegs: Map.from(_state.simRegs),
      simPendingAsmIdx: _pendingAsmIdx,  // ← save execution pointer
      simExecWaiting: _execWaiting,      // ← save pause state
    );

    final snaps = List<RegisterSnapshot>.from(_state.snapshots)..add(snap);
    _state = _state.copyWith(
      snapshots: snaps,
      activeSnapshotIndex: snap.stepIndex,
      timelineVisible: true,
    );
  }

  // ─────────────────────────────────────────────────────────────────────────
  // ASM map building
  // ─────────────────────────────────────────────────────────────────────────

  void _buildAsmMaps() {
    _asmFlat = [];
    _addrToAsmIdx.clear();
    _srcLineToFirstAddr.clear();
    _addrToSrcLine.clear();

    if (_level == null) return;

    int flatIdx = 0;
    for (int srcIdx = 0; srcIdx < _level!.code.length; srcIdx++) {
      final cl = _level!.code[srcIdx];
      bool firstForSrc = true;
      for (final rawLine in cl.asm) {
        final line = rawLine.trim();
        // Function header: "0000...address <name>:"
        if (RegExp(r'^[0-9a-fA-F]{8,} <[^>]+>:').hasMatch(line)) {
          _asmFlat.add(AsmLine(flatIdx: flatIdx++, address: '', rawText: rawLine, isHeader: true));
          continue;
        }
        // Instruction: "400466:   push rbp"  or  "  400466:   push rbp"
        final m = RegExp(r'^\s*([0-9a-fA-F]+):\s').firstMatch(line);
        if (m != null) {
          final addr = m.group(1)!.replaceFirst(RegExp(r'^0+(?=[0-9a-fA-F])'), '');
          // Extract call target if present
          String? callTarget;
          final callM = RegExp(r'call\s+([0-9a-fA-F]+)\s').firstMatch(line);
          if (callM != null) {
            callTarget = callM.group(1)!.replaceFirst(RegExp(r'^0+(?=[0-9a-fA-F])'), '');
          }
          _asmFlat.add(AsmLine(
            flatIdx: flatIdx,
            address: addr,
            rawText: rawLine,
            callTarget: callTarget,
          ));
          _addrToAsmIdx[addr] = flatIdx;
          _addrToSrcLine[addr] = srcIdx;
          if (firstForSrc) {
            _srcLineToFirstAddr[srcIdx] = addr;
            firstForSrc = false;
          }
          flatIdx++;
        } else {
          // plain comment lines (// ← annotations)
          _asmFlat.add(AsmLine(flatIdx: flatIdx++, address: '', rawText: rawLine, isHeader: true));
        }
      }
    }
  }

  void _syncPendingToSourceLine(int srcIdx) {
    // Try exact source line first
    var addr = _srcLineToFirstAddr[srcIdx];
    // Fall back: scan nearby lines downward then upward
    if (addr == null) {
      for (int delta = 1; delta < 30 && addr == null; delta++) {
        addr = _srcLineToFirstAddr[srcIdx + delta];
        if (addr == null && srcIdx - delta >= 0) {
          addr = _srcLineToFirstAddr[srcIdx - delta];
        }
      }
    }
    // Final fallback: use very first real instruction in asmFlat
    if (addr == null) {
      final firstReal = _skipToNextReal(0);
      if (firstReal < _asmFlat.length) {
        _pendingAsmIdx = firstReal;
        _state = _state.copyWith(activeAsmInstrIdx: firstReal);
      }
      return;
    }
    final flatIdx = _addrToAsmIdx[addr];
    if (flatIdx == null) return;
    _pendingAsmIdx = flatIdx;
    _state = _state.copyWith(activeAsmInstrIdx: flatIdx);
  }

  void _syncSourceToAsm() {
    if (_pendingAsmIdx < 0 || _pendingAsmIdx >= _asmFlat.length) return;
    final addr = _asmFlat[_pendingAsmIdx].address;
    final srcLine = _addrToSrcLine[addr];
    if (srcLine != null) _state = _state.copyWith(activeLineIndex: srcLine);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Utility helpers
  // ─────────────────────────────────────────────────────────────────────────

  int _findInitialEsp(Level lvl) {
    for (int i = 0; i < lvl.initialStack.length; i++) {
      final lbl = lvl.initialStack[i].label;
      if (lbl == 'main Saved EBP' || lbl == 'main Saved RBP') {
        return (i - 1).clamp(0, lvl.initialStack.length - 1);
      }
    }
    for (int i = 0; i < lvl.initialStack.length; i++) {
      if (lvl.initialStack[i].label.contains('main Saved')) {
        return (i - 1).clamp(0, lvl.initialStack.length - 1);
      }
    }
    return 0;
  }

  int _skipToNextReal(int from) {
    while (from < _asmFlat.length) {
      final a = _asmFlat[from];
      if (!a.isHeader && a.address.isNotEmpty) return from;
      from++;
    }
    return from;
  }

  int _safeStackMax() => _state.stack.isEmpty ? 0 : _state.stack.length - 1;

  int _parseRbpValue(List<StackBlock> s) {
    final ebp = _state.ebpIndex;
    if (s.isNotEmpty && ebp >= 0 && ebp < s.length) return _parseAddr(s[ebp].address);
    return 0x7fff0;
  }

  int _parseAddr(String addr) {
    try {
      return int.parse(addr.replaceAll('0x', '').replaceAll('0X', ''), radix: 16);
    } catch (_) { return 0; }
  }

  String _formatHex(int val) => '0x${val.toRadixString(16)}';

  String? _currentAsmText() {
    if (_asmFlat.isEmpty || _pendingAsmIdx < 0 || _pendingAsmIdx >= _asmFlat.length) return null;
    return _asmFlat[_pendingAsmIdx].rawText.trim();
  }

  String? _extractMnem(String raw) {
    // Exact port of Java's extractMnem():
    //   raw after the colon is NOT trimmed before splitting.
    //   Leading whitespace creates an empty first token, so parts[1] is always
    //   the instruction mnemonic (never a hex byte or empty token).
    //
    // Format: "  400466:       push   rbp"
    //           after colon+1 →  "       push   rbp"
    //           split(\s{2+}) →  ["", "push", "rbp"]   ← parts[1] = "push" ✓
    //
    // Format: "  400466:  55  push   rbp"
    //           after colon+1 →  "  55  push   rbp"
    //           split(\s{2+}) →  ["", "55", "push", "rbp"] ← parts[1] = "55" ✗
    //
    // The hex-bytes format only occurs in levels 2B/3. For those, parts[1] is
    // the hex byte and parts[2] is the mnemonic. We detect this by checking
    // if parts[1] is a pure hex token.
    final colon = raw.indexOf(':');
    if (colon < 0) return null;
    // Do NOT trim before splitting — leading whitespace creates an empty first token
    // so parts[1] is always the mnemonic (or hex-byte dump for objdump-with-bytes).
    final rest = raw.substring(colon + 1);
    final parts = rest.split(RegExp(r'\s{2,}'));
    if (parts.length < 2) return null;

    String instrPart = parts[1].trim();
    // Hex-byte dumps look like '55', 'c9', '48 89 e5' — 1-2 hex chars per byte
    final hexByteRe = RegExp(r'^[0-9a-fA-F]{1,2}(\s[0-9a-fA-F]{2})*$');
    if (hexByteRe.hasMatch(instrPart) && parts.length >= 3) {
      instrPart = parts[2].trim();
    }
    if (instrPart.isEmpty) return null;

    final sp = instrPart.indexOf(' ');
    return (sp > 0 ? instrPart.substring(0, sp) : instrPart).trim();
  }

  String _extractFuncName(String raw) {
    final m = RegExp(r'<([^>]+)>').firstMatch(raw);
    return m?.group(1) ?? 'function';
  }

  String _extractQuoted(String text) {
    final m = RegExp(r'"([^"]+)"').firstMatch(text);
    return m?.group(1) ?? '';
  }

  int _findBlockIndex(List<StackBlock> blocks, String labelPrefix) {
    for (int i = 0; i < blocks.length; i++) {
      if (blocks[i].label.startsWith(labelPrefix)) return i;
    }
    return -1;
  }

  List<StackBlock> _deepCopyStack(List<StackBlock> s) =>
      s.map((b) => b.copyWith()).toList();

  void _addConsole(String text, bool isSuccess) {
    final console = List<(String, bool)>.from(_state.consoleOut)..add((text, isSuccess));
    _state = _state.copyWith(consoleOut: console);
  }

  void _updateHexDump() {
    // Simple hex representation of stack values for the hex dump panel
    final rows = <List<int>>[];
    for (final block in _state.stack) {
      final bytes = <int>[];
      for (int i = 0; i < 8; i++) {
        try {
          final v = _parseAddr(block.value);
          bytes.add((v >> (i * 8)) & 0xFF);
        } catch (_) {
          bytes.add(block.value.length > i ? block.value.codeUnitAt(i) : 0);
        }
      }
      rows.add(bytes);
    }
    _state = _state.copyWith(hexRows: rows);
  }

  // ── Accessors for UI ──────────────────────────────────────────────────────

  List<AsmLine> get asmFlat => _asmFlat;
  Level? get level => _level;
  bool get isPatched => _state.isPatched;
}
