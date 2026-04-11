import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../shared/theme/app_theme.dart';
import '../state/auth_controller.dart';
import '../state/auth_state.dart';

class LoginScreen extends ConsumerWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(authControllerProvider);
    final controller = ref.read(authControllerProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.bgPrimary,
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: AppColors.bgSurface,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'SecViz Login',
                      style: GoogleFonts.inter(
                        color: AppColors.textPrimary,
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Enter username only. No password required.',
                      style: TextStyle(
                        color: AppColors.textMuted,
                        fontSize: 12,
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      autocorrect: false,
                      enableSuggestions: false,
                      textInputAction: TextInputAction.done,
                      onChanged: controller.setUsername,
                      onSubmitted: (_) {
                        if (state.canLogin) controller.login();
                      },
                      style: GoogleFonts.firaCode(
                        color: AppColors.textPrimary,
                        fontSize: 13,
                      ),
                      decoration: const InputDecoration(
                        labelText: 'Username',
                        hintText: 'e.g. alice',
                        isDense: true,
                        prefixIcon: Icon(Icons.person_outline),
                      ),
                    ),
                    const SizedBox(height: 12),
                    _LookupStatus(state: state),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: state.canLogin
                                ? () => controller.login()
                                : null,
                            icon: const Icon(Icons.login, size: 16),
                            label: Text(
                              state.submitting ? 'Logging in…' : 'Login',
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: state.canRegister
                                ? () => controller.register()
                                : null,
                            icon: const Icon(Icons.person_add_alt_1, size: 16),
                            label: Text(
                              state.submitting ? 'Registering…' : 'Register',
                            ),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: AppColors.warning,
                              side: const BorderSide(color: AppColors.warning),
                            ),
                          ),
                        ),
                      ],
                    ),
                    if (state.errorMessage != null) ...[
                      const SizedBox(height: 10),
                      Text(
                        state.errorMessage!,
                        style: const TextStyle(
                          color: AppColors.danger,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _LookupStatus extends StatelessWidget {
  final AuthState state;

  const _LookupStatus({required this.state});

  @override
  Widget build(BuildContext context) {
    final text = _statusText(state);
    final color = _statusColor(state.lookupStatus);
    final icon = _statusIcon(state.lookupStatus);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(0.45)),
      ),
      child: Row(
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: TextStyle(color: color, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  static String _statusText(AuthState s) {
    switch (s.lookupStatus) {
      case UsernameLookupStatus.idle:
        return 'Enter a username to check account status.';
      case UsernameLookupStatus.checking:
        return 'Checking user in database...';
      case UsernameLookupStatus.exists:
        return 'User exists. Login is enabled.';
      case UsernameLookupStatus.missing:
        return 'User not found. Register is enabled.';
      case UsernameLookupStatus.error:
        return 'Database check failed.';
    }
  }

  static Color _statusColor(UsernameLookupStatus st) {
    switch (st) {
      case UsernameLookupStatus.idle:
        return AppColors.textMuted;
      case UsernameLookupStatus.checking:
        return AppColors.accent;
      case UsernameLookupStatus.exists:
        return AppColors.success;
      case UsernameLookupStatus.missing:
        return AppColors.warning;
      case UsernameLookupStatus.error:
        return AppColors.danger;
    }
  }

  static IconData _statusIcon(UsernameLookupStatus st) {
    switch (st) {
      case UsernameLookupStatus.idle:
        return Icons.info_outline;
      case UsernameLookupStatus.checking:
        return Icons.sync;
      case UsernameLookupStatus.exists:
        return Icons.check_circle_outline;
      case UsernameLookupStatus.missing:
        return Icons.person_search_outlined;
      case UsernameLookupStatus.error:
        return Icons.error_outline;
    }
  }
}
