import '../../../core/database/app_database.dart';
import 'package:sqflite/sqflite.dart';
import '../models/app_user.dart';

class UserRepository {
  final AppDatabase _database;

  const UserRepository(this._database);

  Future<AppUser?> findByUsername(String username) async {
    final normalized = username.trim();
    if (normalized.isEmpty) return null;

    final db = await _database.database;
    final rows = await db.query(
      'users',
      where: 'username = ?',
      whereArgs: [normalized],
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return AppUser.fromMap(rows.first);
  }

  Future<AppUser> createUser(String username) async {
    final normalized = username.trim();
    if (normalized.isEmpty) {
      throw ArgumentError('Username cannot be empty');
    }

    final db = await _database.database;
    final nowMs = DateTime.now().millisecondsSinceEpoch;
    final id = await db.insert(
      'users',
      {
        'username': normalized,
        'created_at': nowMs,
      },
      conflictAlgorithm: ConflictAlgorithm.abort,
    );

    return AppUser(
      id: id,
      username: normalized,
      createdAt: DateTime.fromMillisecondsSinceEpoch(nowMs),
    );
  }
}
