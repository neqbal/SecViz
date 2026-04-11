class LevelFeedback {
  final int id;
  final int userId;
  final String levelId;
  final int? rating;
  final String? review;
  final DateTime createdAt;
  final DateTime updatedAt;

  const LevelFeedback({
    required this.id,
    required this.userId,
    required this.levelId,
    required this.rating,
    required this.review,
    required this.createdAt,
    required this.updatedAt,
  });

  factory LevelFeedback.fromMap(Map<String, Object?> map) {
    return LevelFeedback(
      id: map['id'] as int,
      userId: map['user_id'] as int,
      levelId: map['level_id'] as String,
      rating: map['rating'] as int?,
      review: map['review'] as String?,
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['created_at'] as int),
      updatedAt: DateTime.fromMillisecondsSinceEpoch(map['updated_at'] as int),
    );
  }
}
