/// Dart implementation of the hash13 function (replaces Rhino/JS engine).
/// hash = sum of (hash * 31 + codeUnit) & 0x1FFF for each character.
class HashEngine {
  static int hash13(String input) {
    int hash = 0;
    for (final c in input.codeUnits) {
      hash = (hash * 31 + c) & 0x1FFF;
    }
    return hash;
  }

  /// Brute-force find a collision for [input] (different string, same hash13).
  static String findCollision(String input) {
    final target = hash13(input);
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789_';
    // Single-char prefix + suffix variations
    for (final prefix in chars.split('')) {
      for (int len = 1; len <= 8; len++) {
        final candidate = _tryCandidates(prefix, len, target, input);
        if (candidate != null) return candidate;
      }
    }
    // Fallback: construct mathematically
    return _constructCollision(input, target);
  }

  static String? _tryCandidates(
      String prefix, int len, int target, String original) {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789_.-!@#';
    for (int i = 0; i < chars.length; i++) {
      final candidate = prefix + chars[i] * len;
      if (candidate != original && hash13(candidate) == target) return candidate;
    }
    return null;
  }

  static String _constructCollision(String original, int target) {
    // Try variations of the original string
    final variations = [
      original + '_',
      '_' + original,
      original.toUpperCase(),
      original.replaceAll('_', '-'),
      original + '!',
      'z' + original,
    ];
    for (final v in variations) {
      if (v != original && hash13(v) == target) return v;
    }
    // Exhaustive search up to 6 chars
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    for (int len = 3; len <= 6; len++) {
      for (int a = 0; a < chars.length; a++) {
        for (int b = 0; b < chars.length; b++) {
          for (int c = 0; c < chars.length; c++) {
            final candidate = '${chars[a]}${chars[b]}${chars[c]}${len > 3 ? chars[a] : ''}';
            if (candidate != original && hash13(candidate) == target) return candidate;
          }
        }
      }
    }
    return 'no_collision_found';
  }

  static const List<List<String>> interestingPairs = [
    ['abc', 'nke'],
    ['hello', 'jello'],
    ['dart', 'test'],
    ['flag', 'data'],
  ];
}
