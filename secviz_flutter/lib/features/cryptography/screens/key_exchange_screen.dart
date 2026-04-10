import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../shared/theme/app_theme.dart';

/// Diffie-Hellman key exchange visualized as color mixing.
class KeyExchangeScreen extends StatefulWidget {
  const KeyExchangeScreen({super.key});

  @override
  State<KeyExchangeScreen> createState() => _KeyExchangeScreenState();
}

class _KeyExchangeScreenState extends State<KeyExchangeScreen>
    with TickerProviderStateMixin {
  // Public shared color
  double _pubR = 0.5, _pubG = 0.3, _pubB = 0.8;

  // Alice secret
  double _aliceR = 0.9, _aliceG = 0.1, _aliceB = 0.2;

  // Bob secret
  double _bobR = 0.1, _bobG = 0.8, _bobB = 0.6;

  bool _revealed = false;
  late AnimationController _revealCtrl;
  late Animation<double> _revealAnim;

  @override
  void initState() {
    super.initState();
    _revealCtrl = AnimationController(
        vsync: this, duration: const Duration(milliseconds: 600));
    _revealAnim = CurvedAnimation(parent: _revealCtrl, curve: Curves.easeInOut);
  }

  @override
  void dispose() {
    _revealCtrl.dispose();
    super.dispose();
  }

  Color get _publicColor => Color.fromARGB(
      255, (_pubR * 255).round(), (_pubG * 255).round(), (_pubB * 255).round());

  Color get _alicePrivate => Color.fromARGB(
      255, (_aliceR * 255).round(), (_aliceG * 255).round(), (_aliceB * 255).round());

  Color get _bobPrivate => Color.fromARGB(
      255, (_bobR * 255).round(), (_bobG * 255).round(), (_bobB * 255).round());

  // Alice mixes public + her private
  Color get _aliceMixed => Color.fromARGB(
      255,
      ((_pubR + _aliceR) / 2 * 255).round(),
      ((_pubG + _aliceG) / 2 * 255).round(),
      ((_pubB + _aliceB) / 2 * 255).round());

  // Bob mixes public + his private
  Color get _bobMixed => Color.fromARGB(
      255,
      ((_pubR + _bobR) / 2 * 255).round(),
      ((_pubG + _bobG) / 2 * 255).round(),
      ((_pubB + _bobB) / 2 * 255).round());

  // Shared secret: Alice mixes Bob's mixed with her private (and vice-versa)
  Color get _sharedAlice => Color.fromARGB(
      255,
      ((_bobMixed.red / 255 + _aliceR) / 2 * 255).round(),
      ((_bobMixed.green / 255 + _aliceG) / 2 * 255).round(),
      ((_bobMixed.blue / 255 + _aliceB) / 2 * 255).round());

  Color get _sharedBob => Color.fromARGB(
      255,
      ((_aliceMixed.red / 255 + _bobR) / 2 * 255).round(),
      ((_aliceMixed.green / 255 + _bobG) / 2 * 255).round(),
      ((_aliceMixed.blue / 255 + _bobB) / 2 * 255).round());

  double _colorAccuracy(Color a, Color b) {
    final dr = (a.red - b.red).abs() / 255.0;
    final dg = (a.green - b.green).abs() / 255.0;
    final db = (a.blue - b.blue).abs() / 255.0;
    return ((1 - (dr + dg + db) / 3) * 100).clamp(0, 100);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: AppBar(title: const Text('Key Exchange Simulator')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Explanation card
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.accent.withOpacity(0.06),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.accent.withOpacity(0.2)),
              ),
              child: const Text(
                'Colors represent numbers in Diffie-Hellman key exchange. '
                'Alice and Bob each mix the shared public color with their private color. '
                'They exchange mixed colors and mix again to derive the same shared secret.',
                style: TextStyle(
                    color: AppColors.textSecondary, fontSize: 12, height: 1.5),
              ),
            ),
            const SizedBox(height: 24),
            // Public color
            _SectionLabel('Shared Public Color', color: AppColors.textPrimary),
            const SizedBox(height: 8),
            _ColorSliders(
              r: _pubR, g: _pubG, b: _pubB,
              onChanged: (r, g, b) => setState(() { _pubR = r; _pubG = g; _pubB = b; }),
              color: _publicColor,
            ),
            const SizedBox(height: 24),
            // Alice + Bob side by side
            Row(
              children: [
                Expanded(
                  child: _PersonPanel(
                    name: 'Alice',
                    color: AppColors.pink,
                    privateColor: _alicePrivate,
                    mixedColor: _aliceMixed,
                    r: _aliceR, g: _aliceG, b: _aliceB,
                    onChanged: (r, g, b) =>
                        setState(() { _aliceR = r; _aliceG = g; _aliceB = b; }),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _PersonPanel(
                    name: 'Bob',
                    color: AppColors.accent,
                    privateColor: _bobPrivate,
                    mixedColor: _bobMixed,
                    r: _bobR, g: _bobG, b: _bobB,
                    onChanged: (r, g, b) =>
                        setState(() { _bobR = r; _bobG = g; _bobB = b; }),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            // Reveal button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () {
                  setState(() => _revealed = !_revealed);
                  _revealed
                      ? _revealCtrl.forward()
                      : _revealCtrl.reverse();
                },
                icon: Icon(_revealed ? Icons.visibility_off : Icons.visibility),
                label: Text(_revealed ? 'Hide Shared Secret' : 'Reveal Shared Secret'),
              ),
            ),
            const SizedBox(height: 16),
            // Shared secrets
            AnimatedBuilder(
              animation: _revealAnim,
              builder: (ctx, _) {
                return Opacity(
                  opacity: _revealAnim.value,
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: _SharedSecretBox(
                                label: "Alice's Derived Secret",
                                color: _sharedAlice),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _SharedSecretBox(
                                label: "Bob's Derived Secret",
                                color: _sharedBob),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.bgElevated,
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: AppColors.border),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.analytics_outlined,
                                color: AppColors.accent, size: 18),
                            const SizedBox(width: 8),
                            Text(
                              'Match Accuracy: ${_colorAccuracy(_sharedAlice, _sharedBob).toStringAsFixed(1)}%',
                              style: TextStyle(
                                color: _colorAccuracy(_sharedAlice, _sharedBob) > 95
                                    ? AppColors.success
                                    : AppColors.warning,
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _ColorSliders extends StatelessWidget {
  final double r, g, b;
  final Color color;
  final Function(double, double, double) onChanged;

  const _ColorSliders(
      {required this.r, required this.g, required this.b,
       required this.color, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 60, height: 60,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppColors.border),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            children: [
              _Slider('R', r, Colors.red, (v) => onChanged(v, g, b)),
              _Slider('G', g, Colors.green, (v) => onChanged(r, v, b)),
              _Slider('B', b, Colors.blue, (v) => onChanged(r, g, v)),
            ],
          ),
        ),
      ],
    );
  }
}

class _Slider extends StatelessWidget {
  final String label;
  final double value;
  final Color color;
  final ValueChanged<double> onChanged;

  const _Slider(this.label, this.value, this.color, this.onChanged);

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 14,
          child: Text(label,
              style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.w700)),
        ),
        Expanded(
          child: SliderTheme(
            data: SliderThemeData(
              trackHeight: 3,
              thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
              activeTrackColor: color,
              inactiveTrackColor: color.withOpacity(0.2),
              thumbColor: color,
              overlayShape: const RoundSliderOverlayShape(overlayRadius: 10),
            ),
            child: Slider(value: value, onChanged: onChanged),
          ),
        ),
        SizedBox(
          width: 30,
          child: Text((value * 255).round().toString(),
              style: const TextStyle(color: AppColors.textMuted, fontSize: 10),
              textAlign: TextAlign.right),
        ),
      ],
    );
  }
}

class _PersonPanel extends StatelessWidget {
  final String name;
  final Color color;
  final Color privateColor, mixedColor;
  final double r, g, b;
  final Function(double, double, double) onChanged;

  const _PersonPanel(
      {required this.name, required this.color, required this.privateColor,
       required this.mixedColor, required this.r, required this.g,
       required this.b, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.bgCard,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.person, color: color, size: 16),
              const SizedBox(width: 4),
              Text(name, style: TextStyle(color: color, fontSize: 13, fontWeight: FontWeight.w700)),
            ],
          ),
          const SizedBox(height: 10),
          const Text('Private Secret', style: TextStyle(color: AppColors.textMuted, fontSize: 10)),
          const SizedBox(height: 6),
          _ColorSliders(r: r, g: g, b: b, color: privateColor, onChanged: onChanged),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _ColorBox('Public +\nPrivate', mixedColor, size: 48),
            ],
          ),
        ],
      ),
    );
  }
}

class _ColorBox extends StatelessWidget {
  final String label;
  final Color color;
  final double size;

  const _ColorBox(this.label, this.color, {this.size = 60});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: size, height: size,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppColors.border),
          ),
        ),
        const SizedBox(height: 4),
        Text(label,
            style: const TextStyle(color: AppColors.textMuted, fontSize: 9),
            textAlign: TextAlign.center),
      ],
    );
  }
}

class _SharedSecretBox extends StatelessWidget {
  final String label;
  final Color color;
  const _SharedSecretBox({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.bgElevated,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          Container(
            width: 60, height: 60,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.border),
              boxShadow: [BoxShadow(color: color.withOpacity(0.4), blurRadius: 12)],
            ),
          ),
          const SizedBox(height: 8),
          Text(label,
              style: const TextStyle(
                  color: AppColors.textMuted, fontSize: 10, height: 1.3),
              textAlign: TextAlign.center),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String text;
  final Color color;
  const _SectionLabel(this.text, {required this.color});

  @override
  Widget build(BuildContext context) =>
      Text(text, style: TextStyle(color: color, fontSize: 13, fontWeight: FontWeight.w700));
}
