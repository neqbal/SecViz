import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/database/database_providers.dart';
import '../data/user_repository.dart';
import 'auth_state.dart';

final userRepositoryProvider = Provider<UserRepository>((ref) {
  return UserRepository(ref.read(appDatabaseProvider));
});

final authControllerProvider =
    StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(ref.read(userRepositoryProvider));
});

class AuthController extends StateNotifier<AuthState> {
  final UserRepository _users;
  int _lookupToken = 0;

  AuthController(this._users) : super(const AuthState());

  Future<void> setUsername(String value) async {
    final input = value;
    final trimmed = input.trim();
    state = state.copyWith(
      usernameInput: input,
      submitting: false,
      clearError: true,
    );

    if (trimmed.isEmpty) {
      state = state.copyWith(
        lookupStatus: UsernameLookupStatus.idle,
        clearFoundUser: true,
      );
      return;
    }

    final token = ++_lookupToken;
    state = state.copyWith(
      lookupStatus: UsernameLookupStatus.checking,
      clearFoundUser: true,
      clearError: true,
    );

    try {
      final user = await _users.findByUsername(trimmed);
      if (token != _lookupToken) return;
      if (user != null) {
        state = state.copyWith(
          lookupStatus: UsernameLookupStatus.exists,
          foundUser: user,
        );
      } else {
        state = state.copyWith(
          lookupStatus: UsernameLookupStatus.missing,
          clearFoundUser: true,
        );
      }
    } catch (_) {
      if (token != _lookupToken) return;
      state = state.copyWith(
        lookupStatus: UsernameLookupStatus.error,
        errorMessage: 'Unable to check user in database.',
        clearFoundUser: true,
      );
    }
  }

  Future<void> login() async {
    final user = state.foundUser;
    if (user == null || !state.canLogin) return;
    state = state.copyWith(submitting: true, clearError: true);
    state = state.copyWith(
      submitting: false,
      currentUser: user,
      clearError: true,
    );
  }

  Future<void> register() async {
    final username = state.usernameInput.trim();
    if (username.isEmpty || !state.canRegister) return;

    state = state.copyWith(submitting: true, clearError: true);
    try {
      final created = await _users.createUser(username);
      state = state.copyWith(
        submitting: false,
        lookupStatus: UsernameLookupStatus.exists,
        foundUser: created,
        currentUser: created,
        clearError: true,
      );
    } catch (_) {
      // If another request created the user in between, resolve by lookup.
      final existing = await _users.findByUsername(username);
      if (existing != null) {
        state = state.copyWith(
          submitting: false,
          lookupStatus: UsernameLookupStatus.exists,
          foundUser: existing,
          currentUser: existing,
          clearError: true,
        );
        return;
      }
      state = state.copyWith(
        submitting: false,
        lookupStatus: UsernameLookupStatus.error,
        errorMessage: 'Unable to register user.',
      );
    }
  }

  Future<void> logout() async {
    state = state.copyWith(
      clearCurrentUser: true,
      usernameInput: '',
      lookupStatus: UsernameLookupStatus.idle,
      clearFoundUser: true,
      clearError: true,
      submitting: false,
    );
  }
}
