import '../models/app_user.dart';

enum UsernameLookupStatus {
  idle,
  checking,
  exists,
  missing,
  error,
}

class AuthState {
  final String usernameInput;
  final UsernameLookupStatus lookupStatus;
  final AppUser? foundUser;
  final AppUser? currentUser;
  final bool submitting;
  final String? errorMessage;

  const AuthState({
    this.usernameInput = '',
    this.lookupStatus = UsernameLookupStatus.idle,
    this.foundUser,
    this.currentUser,
    this.submitting = false,
    this.errorMessage,
  });

  bool get canLogin =>
      !submitting &&
      usernameInput.trim().isNotEmpty &&
      lookupStatus == UsernameLookupStatus.exists &&
      foundUser != null;

  bool get canRegister =>
      !submitting &&
      usernameInput.trim().isNotEmpty &&
      lookupStatus == UsernameLookupStatus.missing;

  bool get isAuthenticated => currentUser != null;

  AuthState copyWith({
    String? usernameInput,
    UsernameLookupStatus? lookupStatus,
    AppUser? foundUser,
    bool clearFoundUser = false,
    AppUser? currentUser,
    bool clearCurrentUser = false,
    bool? submitting,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AuthState(
      usernameInput: usernameInput ?? this.usernameInput,
      lookupStatus: lookupStatus ?? this.lookupStatus,
      foundUser: clearFoundUser ? null : (foundUser ?? this.foundUser),
      currentUser: clearCurrentUser ? null : (currentUser ?? this.currentUser),
      submitting: submitting ?? this.submitting,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }
}
