import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../shared/theme/app_theme.dart';
import '../logic/hash_engine.dart';

/// Hash Collision Challenge — find two different inputs with the same hash13 output.
class HashCollisionScreen extends StatefulWidget {
  const HashCollisionScreen({super.key});

  @override
  State<HashCollisionScreen> createState() => _HashCollisionScreenState();
}

class _HashCollisionScreenState extends State<HashCollisionScreen> {
  final _input1Ctrl = TextEditingController(text: 'hello_world');
  final _input2Ctrl = TextEditingController(text: '');
  String _hash1 = '', _hash2 = '';
  bool _collision = false;
  String _result = '';

  @override
  void dispose() {
    _input1Ctrl.dispose();
    _input2Ctrl.dispose();
    super.dispose();
  }

  void _check() {
    final a = _input1Ctrl.text.trim();
    final b = _input2Ctrl.text.trim();
    if (a.isEmpty || b.isEmpty) return;

    final h1 = HashEngine.hash13(a);
    final h2 = HashEngine.hash13(b);
    setState(() {
      _hash1 = '0x${h1.toRadixString(16).padLeft(4, '0')}';
      _hash2 = '0x${h2.toRadixString(16).padLeft(4, '0')}';
      _collision = a != b && h1 == h2;
      if (_collision) {
        _result = '🎉 COLLISION FOUND! Both inputs hash to $_hash1';
      } else if (h1 == h2) {
        _result = 'Same input — try a different second string.';
      } else {
        _result = 'No collision yet. Keep trying!';
      }
    });
  }

  void _tryKnownCollisions() {
    setState(() {
      _input1Ctrl.text = 'hello_world';
      _input2Ctrl.text = HashEngine.findCollision('hello_world');
      _check();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: AppBar(title: const Text('Hash Collision Challenge')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.warning.withOpacity(0.06),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.warning.withOpacity(0.2)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Hash Function: hash13()',
                      style: TextStyle(color: AppColors.warning, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 6),
                  Text(
                    'hash = 0\nfor each char c:\n  hash = (hash * 31 + c) & 0x1FFF',
                    style: GoogleFonts.firaCode(color: AppColors.textSecondary, fontSize: 11, height: 1.5),
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'Goal: find two different strings that produce the same 13-bit hash.',
                    style: TextStyle(color: AppColors.textMuted, fontSize: 11),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            // Input 1
            _InputRow(
              label: 'Input 1 (locked: "hello_world")',
              controller: _input1Ctrl,
              readOnly: true,
              hash: _hash1,
            ),
            const SizedBox(height: 12),
            // Input 2
            _InputRow(
              label: 'Input 2 (your collision candidate)',
              controller: _input2Ctrl,
              readOnly: false,
              hash: _hash2,
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _check,
                    icon: const Icon(Icons.check_circle_outline, size: 16),
                    label: const Text('Check Collision'),
                  ),
                ),
                const SizedBox(width: 8),
                OutlinedButton(
                  onPressed: _tryKnownCollisions,
                  style: OutlinedButton.styleFrom(foregroundColor: AppColors.warning),
                  child: const Text('Hint: Auto-find'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_result.isNotEmpty)
              AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: (_collision ? AppColors.success : AppColors.bgElevated)
                      .withOpacity(_collision ? 0.12 : 1.0),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: _collision
                        ? AppColors.success
                        : AppColors.border,
                    width: _collision ? 2 : 1,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      _collision ? Icons.celebration : Icons.info_outline,
                      color: _collision ? AppColors.success : AppColors.textMuted,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        _result,
                        style: TextStyle(
                          color: _collision ? AppColors.success : AppColors.textSecondary,
                          fontSize: 13,
                          fontWeight: _collision ? FontWeight.w700 : FontWeight.normal,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 24),
            // Hash table preview
            const Text('Try These:',
                style: TextStyle(color: AppColors.textMuted, fontSize: 11, fontWeight: FontWeight.w600)),
            const SizedBox(height: 8),
            ...HashEngine.interestingPairs.map((pair) {
              final h1 = HashEngine.hash13(pair[0]);
              final h2 = HashEngine.hash13(pair[1]);
              return Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: InkWell(
                  onTap: () {
                    setState(() {
                      _input1Ctrl.text = pair[0];
                      _input2Ctrl.text = pair[1];
                      _check();
                    });
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                    decoration: BoxDecoration(
                      color: AppColors.bgElevated,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Row(
                      children: [
                        Text('"${pair[0]}"',
                            style: GoogleFonts.firaCode(
                                color: AppColors.textSecondary, fontSize: 11)),
                        const Text(' + ', style: TextStyle(color: AppColors.textMuted)),
                        Text('"${pair[1]}"',
                            style: GoogleFonts.firaCode(
                                color: AppColors.textSecondary, fontSize: 11)),
                        const Spacer(),
                        Text(
                          h1 == h2 ? '✓ collision' : 'hash: 0x${h1.toRadixString(16)}',
                          style: TextStyle(
                            color: h1 == h2 ? AppColors.success : AppColors.textMuted,
                            fontSize: 10,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}

class _InputRow extends StatelessWidget {
  final String label;
  final TextEditingController controller;
  final bool readOnly;
  final String hash;

  const _InputRow({
    required this.label,
    required this.controller,
    required this.readOnly,
    required this.hash,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: AppColors.textMuted, fontSize: 11)),
        const SizedBox(height: 6),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: controller,
                readOnly: readOnly,
                style: GoogleFonts.firaCode(color: AppColors.textPrimary, fontSize: 13),
                decoration: InputDecoration(
                  fillColor: readOnly ? AppColors.bgSurface : AppColors.bgElevated,
                  isDense: true,
                ),
              ),
            ),
            if (hash.isNotEmpty) ...[
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.bgElevated,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: AppColors.border),
                ),
                child: Text(hash,
                    style: GoogleFonts.firaCode(
                        color: AppColors.accent, fontSize: 11)),
              ),
            ],
          ],
        ),
      ],
    );
  }
}
