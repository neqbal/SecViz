import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../shared/theme/app_theme.dart';
import '../../core/engine/levels_repository.dart';
import '../binary_exploitation/screens/level_screen.dart';
import '../cryptography/screens/crypto_home_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 160,
            pinned: true,
            backgroundColor: AppColors.bgSurface,
            surfaceTintColor: Colors.transparent,
            flexibleSpace: FlexibleSpaceBar(
              titlePadding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
              title: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'SecViz',
                    style: GoogleFonts.inter(
                      color: AppColors.accent,
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.5,
                    ),
                  ),
                  Text(
                    'Cybersecurity Training Platform',
                    style: GoogleFonts.inter(
                        color: AppColors.textMuted, fontSize: 11),
                  ),
                ],
              ),
              background: CustomPaint(painter: _GridBgPainter()),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.all(16),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                _SectionHeader('Modules'),
                const SizedBox(height: 12),
                // Binary Exploitation Card
                _ModuleCard(
                  title: 'Binary Exploitation',
                  subtitle: 'Buffer overflows, ROP chains, control flow hijacking',
                  icon: Icons.memory,
                  gradient: const [Color(0xFF1A0D0D), Color(0xFF2A0F0F)],
                  borderColor: AppColors.danger,
                  badge: '4 Levels',
                  onTap: () => _showLevelSelector(context),
                ),
                const SizedBox(height: 12),
                // Cryptography Card
                _ModuleCard(
                  title: 'Cryptography',
                  subtitle: 'Key exchange, hash collisions, avalanche effect',
                  icon: Icons.lock_outline,
                  gradient: const [Color(0xFF0A1524), Color(0xFF0D1F2E)],
                  borderColor: AppColors.accent,
                  badge: '3 Tools',
                  onTap: () => Navigator.push(context,
                      MaterialPageRoute(builder: (_) => const CryptoHomeScreen())),
                ),
                const SizedBox(height: 32),
                _SectionHeader('Quick Start — Binary Exploitation'),
                const SizedBox(height: 12),
                // Level cards
                ...LevelsRepository.getLevels().map((l) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: _LevelCard(
                    levelId: l.id,
                    title: l.title,
                    subtitle: l.subtitle,
                    goal: l.goal,
                  ),
                )),
              ]),
            ),
          ),
        ],
      ),
    );
  }

  void _showLevelSelector(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.bgSurface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _LevelSelectorSheet(),
    );
  }
}

class _LevelSelectorSheet extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final levels = LevelsRepository.getLevels();
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40, height: 4,
              decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Choose Level',
              style: TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 17,
                  fontWeight: FontWeight.w700)),
          const SizedBox(height: 16),
          ...levels.map((l) => ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Container(
              width: 36, height: 36,
              decoration: BoxDecoration(
                color: AppColors.bgElevated,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.border),
              ),
              child: Center(
                child: Text(l.id.toUpperCase(),
                    style: GoogleFonts.firaCode(
                        color: AppColors.accent, fontSize: 10,
                        fontWeight: FontWeight.w700)),
              ),
            ),
            title: Text(l.title,
                style: const TextStyle(
                    color: AppColors.textPrimary, fontSize: 13,
                    fontWeight: FontWeight.w600)),
            subtitle: Text(l.subtitle,
                style: const TextStyle(
                    color: AppColors.textMuted, fontSize: 11)),
            trailing: const Icon(Icons.chevron_right,
                color: AppColors.textMuted, size: 18),
            onTap: () {
              Navigator.pop(context);
              Navigator.push(context,
                  MaterialPageRoute(builder: (_) => LevelScreen(levelId: l.id)));
            },
          )),
        ],
      ),
    );
  }
}

class _ModuleCard extends StatefulWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final List<Color> gradient;
  final Color borderColor;
  final String badge;
  final VoidCallback onTap;

  const _ModuleCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.gradient,
    required this.borderColor,
    required this.badge,
    required this.onTap,
  });

  @override
  State<_ModuleCard> createState() => _ModuleCardState();
}

class _ModuleCardState extends State<_ModuleCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _hoverCtrl;
  late Animation<double> _hoverAnim;

  @override
  void initState() {
    super.initState();
    _hoverCtrl = AnimationController(
        vsync: this, duration: const Duration(milliseconds: 150));
    _hoverAnim = CurvedAnimation(parent: _hoverCtrl, curve: Curves.easeInOut);
  }

  @override
  void dispose() {
    _hoverCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _hoverAnim,
      builder: (ctx, child) {
        return Transform.scale(
          scale: 1.0 + _hoverAnim.value * 0.01,
          child: child,
        );
      },
      child: GestureDetector(
        onTapDown: (_) => _hoverCtrl.forward(),
        onTapUp: (_) { _hoverCtrl.reverse(); widget.onTap(); },
        onTapCancel: () => _hoverCtrl.reverse(),
        child: Container(
          height: 120,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: widget.gradient,
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
                color: widget.borderColor.withOpacity(0.4), width: 1),
          ),
          child: Stack(
            children: [
              // Background icon
              Positioned(
                right: -10,
                bottom: -10,
                child: Icon(widget.icon,
                    size: 80,
                    color: widget.borderColor.withOpacity(0.08)),
              ),
              // Content
              Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(widget.icon,
                            size: 20, color: widget.borderColor),
                        const SizedBox(width: 8),
                        Text(widget.title,
                            style: GoogleFonts.inter(
                                color: AppColors.textPrimary,
                                fontSize: 16,
                                fontWeight: FontWeight.w700)),
                        const Spacer(),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: widget.borderColor.withOpacity(0.15),
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(
                                color: widget.borderColor.withOpacity(0.4)),
                          ),
                          child: Text(widget.badge,
                              style: TextStyle(
                                  color: widget.borderColor,
                                  fontSize: 10,
                                  fontWeight: FontWeight.w700)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(widget.subtitle,
                        style: const TextStyle(
                            color: AppColors.textSecondary,
                            fontSize: 12,
                            height: 1.4)),
                    const Spacer(),
                    Row(
                      children: [
                        Text('Start →',
                            style: TextStyle(
                                color: widget.borderColor,
                                fontSize: 12,
                                fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LevelCard extends StatelessWidget {
  final String levelId;
  final String title;
  final String subtitle;
  final String goal;

  const _LevelCard({
    required this.levelId,
    required this.title,
    required this.subtitle,
    required this.goal,
  });

  Color get _goalColor {
    switch (goal) {
      case 'CRASH': return AppColors.danger;
      case 'LEAK': return AppColors.warning;
      case 'CFH': return AppColors.purple;
      case 'ROP': return AppColors.success;
      default: return AppColors.accent;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.bgCard,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => Navigator.push(context,
            MaterialPageRoute(builder: (_) => LevelScreen(levelId: levelId))),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              Container(
                width: 40, height: 40,
                decoration: BoxDecoration(
                  color: _goalColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: _goalColor.withOpacity(0.3)),
                ),
                child: Center(
                  child: Text(levelId.toUpperCase(),
                      style: GoogleFonts.firaCode(
                          color: _goalColor,
                          fontSize: 10,
                          fontWeight: FontWeight.w800)),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style: const TextStyle(
                            color: AppColors.textPrimary,
                            fontSize: 13,
                            fontWeight: FontWeight.w600)),
                    const SizedBox(height: 2),
                    Text(subtitle,
                        style: const TextStyle(
                            color: AppColors.textMuted, fontSize: 11),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                decoration: BoxDecoration(
                  color: _goalColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(goal,
                    style: TextStyle(
                        color: _goalColor,
                        fontSize: 9,
                        fontWeight: FontWeight.w700)),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.chevron_right,
                  color: AppColors.textMuted, size: 18),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String text;
  const _SectionHeader(this.text);

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const SizedBox(width: 2),
        Text(text,
            style: const TextStyle(
                color: AppColors.textMuted,
                fontSize: 11,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.0)),
        const SizedBox(width: 8),
        const Expanded(child: Divider(color: AppColors.border)),
      ],
    );
  }
}

// Simple dot-grid background painter for the AppBar
class _GridBgPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height),
        Paint()..color = AppColors.bgSurface);
    final paint = Paint()..color = AppColors.border;
    const step = 24.0;
    for (double x = 0; x < size.width; x += step) {
      for (double y = 0; y < size.height; y += step) {
        canvas.drawCircle(Offset(x, y), 1, paint);
      }
    }
    // Accent glow
    canvas.drawRect(
      Rect.fromLTWH(0, 0, size.width, size.height),
      Paint()
        ..shader = RadialGradient(
          center: Alignment.topLeft,
          radius: 1.2,
          colors: [
            AppColors.accent.withOpacity(0.08),
            Colors.transparent,
          ],
        ).createShader(Rect.fromLTWH(0, 0, size.width, size.height)),
    );
  }

  @override
  bool shouldRepaint(_) => false;
}
