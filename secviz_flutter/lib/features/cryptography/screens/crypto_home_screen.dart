import 'package:flutter/material.dart';
import '../../../shared/theme/app_theme.dart';
import 'key_exchange_screen.dart';
import 'hash_collision_screen.dart';
import 'avalanche_screen.dart';

class CryptoHomeScreen extends StatelessWidget {
  const CryptoHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      appBar: AppBar(
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Cryptography', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
            Text('Interactive Tools', style: TextStyle(fontSize: 10, color: AppColors.textMuted)),
          ],
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _CryptoCard(
            title: 'Key Exchange Simulator',
            subtitle: 'Visualize Diffie-Hellman using a color-mixing analogy',
            icon: Icons.palette,
            color: AppColors.accent,
            onTap: () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => const KeyExchangeScreen())),
          ),
          const SizedBox(height: 12),
          _CryptoCard(
            title: 'Hash Collision Challenge',
            subtitle: 'Find two inputs with the same custom hash function output',
            icon: Icons.tag,
            color: AppColors.warning,
            onTap: () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => const HashCollisionScreen())),
          ),
          const SizedBox(height: 12),
          _CryptoCard(
            title: 'Avalanche Effect Demo',
            subtitle: 'Toggle input bits and see how ciphertext changes dramatically',
            icon: Icons.bolt,
            color: AppColors.purple,
            onTap: () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => const AvalancheScreen())),
          ),
        ],
      ),
    );
  }
}

class _CryptoCard extends StatelessWidget {
  final String title, subtitle;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  const _CryptoCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.bgCard,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: color.withOpacity(0.3)),
          ),
          child: Row(
            children: [
              Container(
                width: 48, height: 48,
                decoration: BoxDecoration(
                  color: color.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: color, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style: const TextStyle(
                            color: AppColors.textPrimary,
                            fontSize: 14,
                            fontWeight: FontWeight.w700)),
                    const SizedBox(height: 4),
                    Text(subtitle,
                        style: const TextStyle(
                            color: AppColors.textMuted,
                            fontSize: 12,
                            height: 1.3)),
                  ],
                ),
              ),
              Icon(Icons.arrow_forward_ios,
                  color: color.withOpacity(0.7), size: 16),
            ],
          ),
        ),
      ),
    );
  }
}
