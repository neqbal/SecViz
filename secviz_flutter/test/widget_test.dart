import 'package:flutter_test/flutter_test.dart';
import 'package:secviz_flutter/main.dart';

void main() {
  testWidgets('SecViz home screen renders', (WidgetTester tester) async {
    await tester.pumpWidget(const SecVizApp());
    await tester.pumpAndSettle();
    expect(find.text('SecViz'), findsOneWidget);
  });
}
