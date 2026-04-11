import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:secviz_flutter/main.dart';

void main() {
  testWidgets('login screen renders on startup', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: SecVizApp()),
    );
    await tester.pumpAndSettle();
    expect(find.text('SecViz Login'), findsOneWidget);
    expect(find.text('Login'), findsOneWidget);
    expect(find.text('Register'), findsOneWidget);
  });
}
