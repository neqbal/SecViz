import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../shared/theme/app_theme.dart';
import '../../core/engine/levels_repository.dart';
import '../../core/models/models.dart';
import '../binary_exploitation/screens/level_screen.dart';
import '../binary_exploitation/screens/format_string_screen.dart';
import '../binary_exploitation/screens/format_string2_screen.dart';
import '../cryptography/screens/key_exchange_screen.dart';
import '../cryptography/screens/hash_collision_screen.dart';
import '../cryptography/screens/avalanche_screen.dart';
import '../auth/state/auth_controller.dart';
import '../level_feedback/data/level_feedback_repository.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    final levels = LevelsRepository.getLevels();
    final currentUser = ref.watch(authControllerProvider).currentUser;
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: AppColors.bgPrimary,
        body: NestedScrollView(
          headerSliverBuilder: (ctx, innerScrolled) => [
            SliverAppBar(
              expandedHeight: 176,
              pinned: true,
              backgroundColor: AppColors.bgSurface,
              surfaceTintColor: Colors.transparent,
              actions: [
                if (currentUser != null)
                  IconButton(
                    icon: const Icon(Icons.logout_rounded, color: AppColors.textPrimary),
                    onPressed: () => ref.read(authControllerProvider.notifier).logout(),
                    tooltip: 'Logout',
                  ),
              ],
              flexibleSpace: FlexibleSpaceBar(
                titlePadding: const EdgeInsets.fromLTRB(20, 0, 20, 68),
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
              bottom: PreferredSize(
                preferredSize: const Size.fromHeight(52),
                child: Container(
                  color: AppColors.bgSurface,
                  padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                  child: TabBar(
                    dividerColor: Colors.transparent,
                    indicatorSize: TabBarIndicatorSize.tab,
                    labelColor: AppColors.textPrimary,
                    unselectedLabelColor: AppColors.textMuted,
                    labelStyle: GoogleFonts.inter(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                    ),
                    unselectedLabelStyle: GoogleFonts.inter(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                    indicator: BoxDecoration(
                      color: AppColors.bgElevated,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppColors.border),
                    ),
                    tabs: const [
                      Tab(text: 'Binary Exploitation'),
                      Tab(text: 'Cryptography'),
                    ],
                  ),
                ),
              ),
            ),
          ],
          body: TabBarView(
            children: [
              ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  const _SectionHeader('Binary Exploitation'),
                  const SizedBox(height: 12),
                  _ModuleCard(
                    title: 'Binary Exploitation',
                    subtitle: 'Buffer overflows, ROP chains, control flow hijacking',
                    icon: Icons.memory,
                    gradient: const [Color(0xFF1A0D0D), Color(0xFF2A0F0F)],
                    borderColor: AppColors.danger,
                    badge: '\${levels.length} Levels',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const LevelScreen(levelId: '1')),
                    ),
                  ),
                  const SizedBox(height: 28),
                  const _SectionHeader('Quick Start Levels'),
                  const SizedBox(height: 12),
                  ...levels.map((l) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: _LevelCard(
                      levelId: l.id,
                      title: l.title,
                      subtitle: l.subtitle,
                      goal: l.goal,
                      type: l.type,
                      onLongPress: currentUser == null
                          ? null
                          : () => _showLevelFeedbackActions(
                                levelId: l.id,
                                levelTitle: l.title,
                                userId: currentUser.id,
                              ),
                    ),
                  )),
                ],
              ),
              ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  const _SectionHeader('Cryptography'),
                  const SizedBox(height: 12),
                  _ModuleCard(
                    title: 'Cryptography',
                    subtitle: 'Key exchange, hash collisions, avalanche effect',
                    icon: Icons.lock_outline,
                    gradient: const [Color(0xFF0A1524), Color(0xFF0D1F2E)],
                    borderColor: AppColors.accent,
                    badge: '3 Tools',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const KeyExchangeScreen()),
                    ),
                  ),
                  const SizedBox(height: 28),
                  const _SectionHeader('Quick Start Tools'),
                  const SizedBox(height: 12),
                  _CryptoToolCard(
                    title: 'Key Exchange Simulator',
                    subtitle: 'Diffie-Hellman color mixing visualization',
                    icon: Icons.palette,
                    color: AppColors.accent,
                    onTap: () => Navigator.push(context,
                        MaterialPageRoute(builder: (_) => const KeyExchangeScreen())),
                    onLongPress: currentUser == null
                        ? null
                        : () => _showLevelFeedbackActions(
                              levelId: 'crypto_key_exchange',
                              levelTitle: 'Key Exchange Simulator',
                              userId: currentUser.id,
                            ),
                  ),
                  const SizedBox(height: 8),
                  _CryptoToolCard(
                    title: 'Hash Collision Challenge',
                    subtitle: 'Find collisions in the custom hash13 function',
                    icon: Icons.tag,
                    color: AppColors.warning,
                    onTap: () => Navigator.push(context,
                        MaterialPageRoute(builder: (_) => const HashCollisionScreen())),
                    onLongPress: currentUser == null
                        ? null
                        : () => _showLevelFeedbackActions(
                              levelId: 'crypto_hash_collision',
                              levelTitle: 'Hash Collision Challenge',
                              userId: currentUser.id,
                            ),
                  ),
                  const SizedBox(height: 8),
                  _CryptoToolCard(
                    title: 'Avalanche Effect Demo',
                    subtitle: 'Flip bits and observe ciphertext diffusion',
                    icon: Icons.bolt,
                    color: AppColors.purple,
                    onTap: () => Navigator.push(context,
                        MaterialPageRoute(builder: (_) => const AvalancheScreen())),
                    onLongPress: currentUser == null
                        ? null
                        : () => _showLevelFeedbackActions(
                              levelId: 'crypto_avalanche',
                              levelTitle: 'Avalanche Effect Demo',
                              userId: currentUser.id,
                            ),
                  ),
                ],
              ),
            ],
          ),
        ),
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

  Future<void> _showLevelFeedbackActions({
    required int userId,
    required String levelId,
    required String levelTitle,
  }) async {
    final action = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: AppColors.bgSurface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 14),
              Text(
                levelTitle,
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 10),
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.star_rate_rounded,
                    color: AppColors.warning),
                title: const Text(
                  'Rate the level',
                  style: TextStyle(color: AppColors.textPrimary, fontSize: 13),
                ),
                onTap: () => Navigator.pop(ctx, 'rate'),
              ),
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.rate_review_outlined,
                    color: AppColors.accent),
                title: const Text(
                  'Write a review',
                  style: TextStyle(color: AppColors.textPrimary, fontSize: 13),
                ),
                onTap: () => Navigator.pop(ctx, 'review'),
              ),
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.format_list_bulleted_rounded,
                    color: AppColors.success),
                title: const Text(
                  'See other reviews',
                  style: TextStyle(color: AppColors.textPrimary, fontSize: 13),
                ),
                onTap: () => Navigator.pop(ctx, 'see_reviews'),
              ),
            ],
          ),
        );
      },
    );

    if (!mounted || action == null) return;
    if (action == 'rate') {
      await _showRateDialog(userId: userId, levelId: levelId, levelTitle: levelTitle);
    } else if (action == 'review') {
      await _showReviewDialog(
          userId: userId, levelId: levelId, levelTitle: levelTitle);
    } else if (action == 'see_reviews') {
      await _showOtherReviewsDialog(levelId: levelId, levelTitle: levelTitle);
    }
  }

  Future<void> _showRateDialog({
    required int userId,
    required String levelId,
    required String levelTitle,
  }) async {
    final repo = ref.read(levelFeedbackRepositoryProvider);
    final existing = await repo.getByUserAndLevel(userId: userId, levelId: levelId);
    if (!mounted) return;

    var selected = (existing?.rating ?? 0).clamp(0, 5);
    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setDialogState) {
            return AlertDialog(
              backgroundColor: AppColors.bgSurface,
              title: Text(
                'Rate $levelTitle',
                style: const TextStyle(color: AppColors.textPrimary),
              ),
              content: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(5, (i) {
                  final star = i + 1;
                  return IconButton(
                    onPressed: () => setDialogState(() => selected = star),
                    icon: Icon(
                      star <= selected ? Icons.star : Icons.star_border,
                      color: AppColors.warning,
                    ),
                  );
                }),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(ctx, false),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: selected > 0 ? () => Navigator.pop(ctx, true) : null,
                  child: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );

    if (saved != true) return;
    await repo.upsert(userId: userId, levelId: levelId, rating: selected);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Rating saved')),
    );
  }

  Future<void> _showReviewDialog({
    required int userId,
    required String levelId,
    required String levelTitle,
  }) async {
    final repo = ref.read(levelFeedbackRepositoryProvider);
    final existing = await repo.getByUserAndLevel(userId: userId, levelId: levelId);
    if (!mounted) return;

    final controller = TextEditingController(text: existing?.review ?? '');
    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) {
        return Dialog(
          backgroundColor: AppColors.bgSurface,
          insetPadding: const EdgeInsets.symmetric(horizontal: 22, vertical: 22),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.rate_review_outlined,
                          size: 16, color: AppColors.accent),
                      const SizedBox(width: 6),
                      const Text(
                        'Write Review',
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    levelTitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: AppColors.textMuted,
                      fontSize: 11,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Container(
                    decoration: BoxDecoration(
                      color: AppColors.bgElevated,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: TextField(
                      controller: controller,
                      minLines: 4,
                      maxLines: 5,
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontSize: 12,
                        height: 1.35,
                      ),
                      decoration: const InputDecoration(
                        hintText: 'Share what worked well and what can improve',
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 10,
                        ),
                        isDense: true,
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      TextButton(
                        onPressed: () => Navigator.pop(ctx, false),
                        style: TextButton.styleFrom(
                          minimumSize: const Size(68, 34),
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        ),
                        child: const Text('Cancel'),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        onPressed: () => Navigator.pop(ctx, true),
                        style: FilledButton.styleFrom(
                          minimumSize: const Size(68, 34),
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          backgroundColor: AppColors.accent,
                          foregroundColor: AppColors.bgPrimary,
                        ),
                        child: const Text('Save'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );

    if (saved == true) {
      final review = controller.text.trim();
      await repo.upsert(
        userId: userId,
        levelId: levelId,
        review: review.isEmpty ? '' : review,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Review saved')),
        );
      }
    }
    controller.dispose();
  }

  Future<void> _showOtherReviewsDialog({
    required String levelId,
    required String levelTitle,
  }) async {
    final repo = ref.read(levelFeedbackRepositoryProvider);
    final reviews = await repo.getReviewsForLevel(levelId);
    if (!mounted) return;

    await showDialog(
      context: context,
      builder: (ctx) {
        return Dialog(
          backgroundColor: AppColors.bgSurface,
          insetPadding: const EdgeInsets.symmetric(horizontal: 22, vertical: 22),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420, maxHeight: 500),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(14, 16, 14, 16),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.format_list_bulleted_rounded,
                          size: 16, color: AppColors.success),
                      const SizedBox(width: 8),
                      const Text(
                        'User Reviews',
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const Spacer(),
                      IconButton(
                        icon: const Icon(Icons.close, size: 20, color: AppColors.textMuted),
                        onPressed: () => Navigator.pop(ctx),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    levelTitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: AppColors.textMuted,
                      fontSize: 12,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (reviews.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 20),
                      child: Center(
                        child: Text(
                          'No reviews yet. Be the first!',
                          style: TextStyle(color: AppColors.textMuted, fontSize: 13),
                        ),
                      ),
                    )
                  else
                    Flexible(
                      child: ListView.separated(
                        shrinkWrap: true,
                        itemCount: reviews.length,
                        separatorBuilder: (_, __) => const Padding(
                          padding: EdgeInsets.symmetric(vertical: 8),
                          child: Divider(color: AppColors.border, height: 1),
                        ),
                        itemBuilder: (ctx, i) {
                          final r = reviews[i];
                          return Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Text(
                                    r.username,
                                    style: const TextStyle(
                                      color: AppColors.accent,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 12,
                                    ),
                                  ),
                                  const Spacer(),
                                  if (r.rating != null && r.rating! > 0)
                                    Row(
                                      children: List.generate(5, (starIdx) {
                                        return Icon(
                                          starIdx < r.rating!
                                              ? Icons.star
                                              : Icons.star_border,
                                          size: 12,
                                          color: AppColors.warning,
                                        );
                                      }),
                                    )
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(
                                r.review!,
                                style: const TextStyle(
                                  color: AppColors.textPrimary,
                                  fontSize: 12,
                                  height: 1.3,
                                ),
                              ),
                            ],
                          );
                        },
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
      },
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
              if (l.type == LevelType.formatString) {
                Navigator.push(context,
                    MaterialPageRoute(builder: (_) => const FormatStringScreen()));
              } else if (l.type == LevelType.formatString2) {
                Navigator.push(context,
                    MaterialPageRoute(builder: (_) => const FormatString2Screen()));
              } else {
                Navigator.push(context,
                    MaterialPageRoute(builder: (_) => LevelScreen(levelId: l.id)));
              }
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
  final LevelType type;
  final VoidCallback? onLongPress;

  const _LevelCard({
    required this.levelId,
    required this.title,
    required this.subtitle,
    required this.goal,
    required this.type,
    this.onLongPress,
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
        onTap: () {
          if (type == LevelType.formatString) {
            Navigator.push(context,
                MaterialPageRoute(builder: (_) => const FormatStringScreen()));
          } else if (type == LevelType.formatString2) {
            Navigator.push(context,
                MaterialPageRoute(builder: (_) => const FormatString2Screen()));
          } else {
            Navigator.push(context,
                MaterialPageRoute(builder: (_) => LevelScreen(levelId: levelId)));
          }
        },
        onLongPress: onLongPress,
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

class _CryptoToolCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const _CryptoToolCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.bgCard,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        onLongPress: onLongPress,
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: color.withOpacity(0.28)),
          ),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: color.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: color.withOpacity(0.35)),
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        color: AppColors.textMuted,
                        fontSize: 11,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: color.withOpacity(0.75), size: 18),
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
