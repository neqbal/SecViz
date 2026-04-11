import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

/// Central app database entry point.
///
/// Keep this class focused on connection lifecycle and schema setup.
/// Feature modules should interact through repositories.
class AppDatabase {
  static const _dbName = 'secviz.db';
  static const _dbVersion = 2;

  Database? _db;

  Future<Database> get database async {
    _db ??= await _open();
    return _db!;
  }

  Future<Database> _open() async {
    final dbDir = await getDatabasesPath();
    final dbPath = p.join(dbDir, _dbName);
    return openDatabase(
      dbPath,
      version: _dbVersion,
      onCreate: (db, version) async {
        await _createUsersTable(db);
        await _createLevelFeedbackTable(db);
      },
      onUpgrade: (db, oldVersion, newVersion) async {
        if (oldVersion < 2) {
          await _createLevelFeedbackTable(db);
        }
      },
    );
  }

  Future<void> _createUsersTable(Database db) async {
    await db.execute('''
      CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL UNIQUE COLLATE NOCASE,
        created_at INTEGER NOT NULL
      )
    ''');
  }

  Future<void> _createLevelFeedbackTable(Database db) async {
    await db.execute('''
      CREATE TABLE level_feedback (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        level_id TEXT NOT NULL,
        rating INTEGER,
        review TEXT,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL,
        UNIQUE(user_id, level_id)
      )
    ''');
  }

  Future<void> close() async {
    final db = _db;
    if (db != null) {
      await db.close();
      _db = null;
    }
  }
}
