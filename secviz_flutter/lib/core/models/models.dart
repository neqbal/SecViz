// Core data models — ported from Java data layer

class StackBlock {
  static const typeMainFrame = 'main-frame';
  static const typeSafe = 'safe';
  static const typeWarn = 'warn';
  static const typeDanger = 'danger';
  static const typeNeutral = 'neutral';
  static const typeFilled = 'filled';
  static const typeJunk = 'junk';
  static const typeTarget = 'target';

  final String address;
  final String label;
  String value;
  String type;
  final int size;
  String? lastInstruction;

  StackBlock({
    required this.address,
    required this.label,
    required this.value,
    required this.type,
    this.size = 8,
    this.lastInstruction,
  });

  StackBlock copyWith({
    String? address,
    String? label,
    String? value,
    String? type,
    int? size,
    String? lastInstruction,
    bool clearLastInstruction = false,
  }) {
    return StackBlock(
      address: address ?? this.address,
      label: label ?? this.label,
      value: value ?? this.value,
      type: type ?? this.type,
      size: size ?? this.size,
      lastInstruction:
          clearLastInstruction ? null : (lastInstruction ?? this.lastInstruction),
    );
  }
}

class AsmLine {
  final int flatIdx;
  final String address;
  final String rawText;
  final bool isHeader;
  final String? callTarget;

  const AsmLine({
    required this.flatIdx,
    required this.address,
    required this.rawText,
    this.isHeader = false,
    this.callTarget,
  });
}

class CodeLine {
  final String text;
  final List<String> asm;

  const CodeLine(this.text, {this.asm = const []});
}

class DefensePatch {
  final String before;
  final String after;
  const DefensePatch(this.before, this.after);
}

class RopGadget {
  final String address;
  final String asm;
  final String description;
  final String alias;

  const RopGadget({
    required this.address,
    required this.asm,
    required this.description,
    required this.alias,
  });
}

class RegisterSnapshot {
  final int stepIndex;
  final String label;
  final String rip;
  final String rsp;
  final String rbp;
  final String statusType; // info | success | danger | warn
  final int codeLine;

  // Full simulation state for time-travel
  final double simStep;
  final List<StackBlock> simStack;
  final int simEsp;
  final int simEbp;
  final String simStatusTitle;
  final String simStatusDesc;
  final List<(String, bool)> simConsole;
  final Map<String, String> simRegs;
  final int simPendingAsmIdx;  // ← engine execution pointer
  final bool simExecWaiting;   // ← was engine paused waiting for input?

  const RegisterSnapshot({
    required this.stepIndex,
    required this.label,
    required this.rip,
    required this.rsp,
    required this.rbp,
    required this.statusType,
    required this.codeLine,
    required this.simStep,
    required this.simStack,
    required this.simEsp,
    required this.simEbp,
    required this.simStatusTitle,
    required this.simStatusDesc,
    required this.simConsole,
    required this.simRegs,
    this.simPendingAsmIdx = -1,
    this.simExecWaiting = false,
  });
}

enum LevelType {
  standard,
  formatString,
}

class Level {
  final String id;
  final String title;
  final String subtitle;
  final String goal; // CRASH | LEAK | CFH | ROP | FMT
  final LevelType type;
  final String successTitle;
  final String successDesc;
  final int startCodeLine;
  final List<CodeLine> code;
  final List<StackBlock> initialStack;
  final List<List<String>> payloadPresets; // [[label, value], ...]
  final DefensePatch? defensePatch;
  final String? objdump;
  final List<RopGadget> ropGadgets;
  final String? hint;

  const Level({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.goal,
    this.type = LevelType.standard,
    required this.successTitle,
    required this.successDesc,
    required this.startCodeLine,
    required this.code,
    required this.initialStack,
    required this.payloadPresets,
    this.defensePatch,
    this.objdump,
    this.ropGadgets = const [],
    this.hint,
  });
}
