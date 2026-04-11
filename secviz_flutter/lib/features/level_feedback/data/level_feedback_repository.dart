import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sqflite/sqflite.dart';

import '../../../core/database/database_providers.dart';
import '../../../core/database/app_database.dart';
import '../models/level_feedback.dart';

final levelFeedbackRepositoryProvider = Provider<LevelFeedbackRepository>((ref) {
  return LevelFeedbackRepository(ref.read(appDatabaseProvider));
});

class LevelFeedbackRepository {
  final AppDatabase _database;

  const LevelFeedbackRepository(this._database);

  Future<LevelFeedback?> getByUserAndLevel({
    required int userId,
    required String levelId,
  }) async {
    final db = await _database.database;
    final rows = await db.query(
      'level_feedback',
      where: 'user_id = ? AND level_id = ?',
      whereArgs: [userId, levelId],
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return LevelFeedback.fromMap(rows.first);
  }

  Future<void> upsert({
    required int userId,
    required String levelId,
    int? rating,
    String? review,
  }) async {
    final db = await _database.database;
    final existing = await getByUserAndLevel(userId: userId, levelId: levelId);
    final nowMs = DateTime.now().millisecondsSinceEpoch;

    if (existing == null) {
      await db.insert(
        'level_feedback',
        {
          'user_id': userId,
          'level_id': levelId,
          'rating': rating,
          'review': review,
          'created_at': nowMs,
          'updated_at': nowMs,
        },
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
      return;
    }

    await db.update(
      'level_feedback',
      {
        'rating': rating ?? existing.rating,
        'review': review ?? existing.review,
        'updated_at': nowMs,
      },
      where: 'id = ?',
      whereArgs: [existing.id],
    );
  }
}
