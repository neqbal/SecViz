class AppUser {
  final int id;
  final String username;
  final DateTime createdAt;

  const AppUser({
    required this.id,
    required this.username,
    required this.createdAt,
  });

  factory AppUser.fromMap(Map<String, Object?> map) {
    return AppUser(
      id: map['id'] as int,
      username: map['username'] as String,
      createdAt: DateTime.fromMillisecondsSinceEpoch(
        map['created_at'] as int,
      ),
    );
  }
}
