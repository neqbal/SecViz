import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../shared/theme/app_theme.dart';

/// Avalanche Effect Demo — toggle input bits and see ciphertext change >50% of bits.
class AvalancheScreen extends StatefulWidget {
  const AvalancheScreen({super.key});

  @override
  State<AvalancheScreen> createState() => _AvalancheScreenState();
}

class _AvalancheScreenState extends State<AvalancheScreen> {
  static const _inputLen = 16; // 2 bytes = 16 bits for demonstration
  List<bool> _bits = List.filled(_inputLen, false);
  final _keyCtrl = TextEditingController(text: 'SECVIZ_AES_KEY16');
  bool _showDiff = true;

  int get _inputValue {
    int v = 0;
    for (final b in _bits) {
      v = (v << 1) | (b ? 1 : 0);
    }
    return v;
  }

  // Simulated "ciphertext" — not real AES, but avalanche-demonstrating LFSR
  @override
  void dispose() {
    _keyCtrl.dispose();
    super.dispose();
  }

  List<int> _encrypt(int input, String key) {
    final seed = key.codeUnits.fold(0, (a, b) => (a * 31 + b) & 0xFFFFFFFF);
    final rng = Random(seed ^ input);
    return List.generate(16, (_) => rng.nextInt(256));
  }

  List<int> get _original => _encrypt(0, _keyCtrl.text);
  List<int> get _modified => _encrypt(_inputValue, _keyCtrl.text);

  int _diffBits(List<int> a, List<int> b) {
    int count = 0;
    for (int i = 0; i < min(a.length, b.length); i++) {
      int xor = a[i] ^ b[i];
      while (xor != 0) { count += xor & 1; xor >>= 1; }
    }
    return count;
  }

  String _toHexStr(List<int> bytes) =>
      bytes.map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase()).join(' ');

  @override
  Widget build(BuildContext context) {
    final original = _original;
    final modified = _modified;
    final diff = _diffBits(original, modified);
    final totalBits = original.length * 8;
    final pct = (diff / totalBits * 100).toStringAsFixed(1);
    final avalanche = diff >= totalBits * 0.4;

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: AppBar(title: const Text('Avalanche Effect Demo')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.purple.withOpacity(0.06),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.purple.withOpacity(0.2)),
              ),
              child: const Text(
                'The avalanche effect: changing even 1 bit in the input should change '
                '~50% of the output bits. Toggle bits below to see the effect.',
                style: TextStyle(color: AppColors.textSecondary, fontSize: 12, height: 1.5),
              ),
            ),
            const SizedBox(height: 24),
            // Key field
            const Text('Encryption Key',
                style: TextStyle(color: AppColors.textMuted, fontSize: 11)),
            const SizedBox(height: 6),
            TextField(
              controller: _keyCtrl,
              onChanged: (v) => setState(() {}),
              style: GoogleFonts.firaCode(color: AppColors.textPrimary, fontSize: 12),
              decoration: const InputDecoration(isDense: true),
            ),
            const SizedBox(height: 20),
            const Text('Input Bits (toggle to flip)',
                style: TextStyle(color: AppColors.textMuted, fontSize: 11)),
            const SizedBox(height: 8),
            // Bit toggles
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: List.generate(_inputLen, (i) {
                return GestureDetector(
                  onTap: () => setState(() => _bits[i] = !_bits[i]),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 150),
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: _bits[i]
                          ? AppColors.purple.withOpacity(0.3)
                          : AppColors.bgElevated,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: _bits[i]
                            ? AppColors.purple
                            : AppColors.border,
                        width: _bits[i] ? 2 : 1,
                      ),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          _bits[i] ? '1' : '0',
                          style: TextStyle(
                            color: _bits[i] ? AppColors.purple : AppColors.textMuted,
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          'b${_inputLen - 1 - i}',
                          style: const TextStyle(
                              color: AppColors.textMuted, fontSize: 8),
                        ),
                      ],
                    ),
                  ),
                );
              }),
            ),
            const SizedBox(height: 24),
            // Ciphertext comparison
            const Text('Ciphertext Comparison',
                style: TextStyle(
                    color: AppColors.textMuted,
                    fontSize: 11,
                    fontWeight: FontWeight.w600)),
            const SizedBox(height: 10),
            _CiphertextRow(
              label: 'Original (all zeros input)',
              bytes: original,
              diffBytes: modified,
              showDiff: _showDiff,
              isOriginal: true,
            ),
            const SizedBox(height: 8),
            _CiphertextRow(
              label: 'Modified (your bit flips)',
              bytes: modified,
              diffBytes: original,
              showDiff: _showDiff,
              isOriginal: false,
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Spacer(),
                const Text('Show diff: ',
                    style: TextStyle(color: AppColors.textMuted, fontSize: 11)),
                Switch(
                  value: _showDiff,
                  onChanged: (v) => setState(() => _showDiff = v),
                  activeColor: AppColors.purple,
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Stat
            AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: (avalanche ? AppColors.success : AppColors.warning)
                    .withOpacity(0.08),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: avalanche ? AppColors.success : AppColors.warning,
                  width: avalanche ? 1.5 : 1,
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    avalanche ? Icons.bolt : Icons.warning_amber_rounded,
                    color: avalanche ? AppColors.success : AppColors.warning,
                    size: 24,
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '$diff / $totalBits bits changed ($pct%)',
                        style: TextStyle(
                          color: avalanche ? AppColors.success : AppColors.warning,
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        avalanche
                            ? 'Avalanche effect demonstrated! ✓'
                            : 'Flip more bits to see the avalanche.',
                        style: const TextStyle(
                            color: AppColors.textMuted, fontSize: 11),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: () => setState(() => _bits = List.filled(_inputLen, false)),
              icon: const Icon(Icons.restart_alt, size: 16),
              label: const Text('Reset All Bits'),
              style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textMuted),
            ),
          ],
        ),
      ),
    );
  }
}

class _CiphertextRow extends StatelessWidget {
  final String label;
  final List<int> bytes;
  final List<int> diffBytes;
  final bool showDiff;
  final bool isOriginal;

  const _CiphertextRow({
    required this.label,
    required this.bytes,
    required this.diffBytes,
    required this.showDiff,
    required this.isOriginal,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.bgElevated,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: AppColors.textMuted, fontSize: 10)),
          const SizedBox(height: 6),
          Wrap(
            spacing: 4,
            runSpacing: 4,
            children: List.generate(bytes.length, (i) {
              final changed = showDiff && i < diffBytes.length && bytes[i] != diffBytes[i];
              return Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 3),
                decoration: BoxDecoration(
                  color: changed
                      ? (isOriginal ? AppColors.danger : AppColors.success)
                          .withOpacity(0.2)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(
                    color: changed
                        ? (isOriginal ? AppColors.danger : AppColors.success)
                            .withOpacity(0.6)
                        : AppColors.border,
                  ),
                ),
                child: Text(
                  bytes[i].toRadixString(16).padLeft(2, '0').toUpperCase(),
                  style: GoogleFonts.firaCode(
                    color: changed
                        ? (isOriginal ? AppColors.danger : AppColors.success)
                        : AppColors.textMuted,
                    fontSize: 10,
                    fontWeight: changed ? FontWeight.w700 : FontWeight.normal,
                  ),
                ),
              );
            }),
          ),
        ],
      ),
    );
  }
}
