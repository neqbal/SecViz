package com.example.secviz.crypto;

import android.os.Bundle;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.example.secviz.R;

public class CryptoHashCollision extends AppCompatActivity {

    private final String TARGET_STRING = "hello_world";
    private final String TARGET_HASH   = "1475"; // Pre-calculated hash13 of "hello_world"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_activity_hash_collision);

        EditText  etCodeEditor = findViewById(R.id.etCodeEditor);
        Button    btnRunCode   = findViewById(R.id.btnRunCode);
        TextView  tvConsole    = findViewById(R.id.tvConsole);

        btnRunCode.setOnClickListener(v -> {
            String userCode = etCodeEditor.getText().toString();
            tvConsole.setText("> Running script...\n");

            Context rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            try {
                Scriptable scope = rhino.initStandardObjects();

                String injectHashFunc =
                        "function hash13(str) { " +
                        "  var hash = 0; " +
                        "  for (var i = 0; i < str.length; i++) { " +
                        "    hash = ((hash << 5) - hash) + str.charCodeAt(i); " +
                        "    hash = hash & 0x1FFF; " +
                        "  } " +
                        "  var hex = hash.toString(16).toUpperCase();" +
                        "  return hex; " +
                        "}";
                rhino.evaluateString(scope, injectHashFunc, "Init", 1, null);

                Object result    = rhino.evaluateString(scope, userCode, "UserScript", 1, null);
                String resultStr = Context.toString(result);
                tvConsole.append("> Returned: " + resultStr + "\n");

                if (!resultStr.equals("undefined") && !resultStr.equals(TARGET_STRING)) {
                    Object validationObj = rhino.evaluateString(scope, "hash13('" + resultStr + "')", "Validation", 1, null);
                    String computedHash  = Context.toString(validationObj);
                    if (computedHash.equals(TARGET_HASH)) {
                        tvConsole.append("\n🎉 SUCCESS! Collision found!\nInput '" + resultStr + "' results in hash " + TARGET_HASH);
                    } else {
                        tvConsole.append("\n❌ Failed. '" + resultStr + "' hashes to " + computedHash + ", not " + TARGET_HASH);
                    }
                } else if (resultStr.equals(TARGET_STRING)) {
                    tvConsole.append("\n❌ Failed. You must find a DIFFERENT string than the target.");
                }

            } catch (Exception e) {
                tvConsole.append("> Error: " + e.getMessage());
            } finally {
                Context.exit();
            }
        });
    }
}
