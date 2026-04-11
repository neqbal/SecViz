import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../home/home_screen.dart';
import '../state/auth_controller.dart';
import 'login_screen.dart';

/// Startup gate for authentication.
///
/// If no current user is set, show login/register screen.
/// Once authenticated, route to main home experience.
class AuthGate extends ConsumerWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    if (!auth.isAuthenticated) {
      return const LoginScreen();
    }
    return const HomeScreen();
  }
}
