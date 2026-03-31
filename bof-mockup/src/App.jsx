import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import './index.css'

const CODE_DATA = [
  { text: "#include <stdio.h>", asm: [] },
  { text: "#include <unistd.h>", asm: [] },
  { text: "", asm: [] },
  { text: "void vuln() {", asm: [] },
  { text: "    char buff[16];", asm: ["0000000000400486 <vuln>:", "  400486:       push   rbp", "  400487:       mov    rbp,rsp", "  40048a:       sub    rsp,0x10"] },
  { text: "", asm: [] },
  { text: "    read(1, buff, 100);", asm: ["  40048e:       lea    rax,[rbp-0x10]", "  400492:       mov    edx,0x64", "  400497:       mov    rsi,rax", "  40049a:       mov    edi,0x1", "  40049f:       call   400390 <read@plt>"] },
  { text: "", asm: [] },
  { text: "    printf(\"%s\", buff);", asm: ["  4004a4:       lea    rax,[rbp-0x10]", "  4004a8:       mov    rsi,rax", "  4004ab:       mov    edi,0x4011e8", "  4004b0:       mov    eax,0x0", "  4004b5:       call   400380 <printf@plt>"] },
  { text: "}", asm: ["  4004ba:       nop", "  4004bb:       leave", "  4004bc:       ret"] },
  { text: "", asm: [] },
  { text: "int main() {", asm: ["00000000004004bd <main>:", "  4004bd:       push   rbp", "  4004be:       mov    rbp,rsp"] },
  { text: "    puts(\"I will echo whatever you say.\");", asm: ["  4004c1:       mov    edi,0x4011f0", "  4004c6:       call   400370 <puts@plt>"] },
  { text: "", asm: [] },
  { text: "    vuln();", asm: ["  4004cb:       call   400486 <vuln>"] },
  { text: "", asm: [] },
  { text: "    puts(\"Goodbye!!!\");", asm: ["  4004d0:       mov    edi,0x401218", "  4004d5:       call   400370 <puts@plt>"] },
  { text: "}", asm: ["  4004da:       mov    eax,0x0", "  4004df:       pop    rbp", "  4004e0:       ret"] }
]

const INITIAL_STACK = [
  { id: 7, address: "0x7fff4", label: "main Frame Data", value: "0x...", size: 8, type: "main-frame" },
  { id: 6, address: "0x7ffec", label: "main Return Addr", value: "0x7fa2b", size: 8, type: "main-frame" },
  { id: 5, address: "0x7ffe4", label: "main Saved EBP", value: "0x7fff0", size: 8, type: "main-frame" },
  { id: 4, address: "0x7ffdc", label: "vuln Return Addr", value: "0x00000", size: 8, type: "safe" },
  { id: 3, address: "0x7ffd4", label: "vuln Saved EBP", value: "0x00000", size: 8, type: "safe" },
  { id: 2, address: "0x7ffcc", label: "buff[8..15]", value: "0x0", size: 8, type: "neutral" },
  { id: 1, address: "0x7ffc4", label: "buff[0..7]", value: "0x0", size: 8, type: "neutral" }
]

function App() {
  const [step, setStep] = useState(0);
  const [activeLineIndex, setActiveLineIndex] = useState(11); // Start at main()
  const [stack, setStack] = useState(INITIAL_STACK);
  const [userInput, setUserInput] = useState('');
  const [inputSubmitted, setInputSubmitted] = useState(false);
  const [espIndex, setEspIndex] = useState(4); // Points to main stack frame initially
  const [ebpIndex, setEbpIndex] = useState(4);

  const [statusTitle, setStatusTitle] = useState("Program Executed");
  const [statusDesc, setStatusDesc] = useState("Execution starts in the main() function. The stack frame for main is created. Click 'Next Step' to proceed.");
  const [statusType, setStatusType] = useState("info");

  const [consoleOut, setConsoleOut] = useState([]);
  const [showAssembly, setShowAssembly] = useState(false);

  const consoleRef = useRef(null);

  useEffect(() => {
    if (consoleRef.current) {
      consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
    }
  }, [consoleOut]);

  // Syntax highlighting helper
  const renderCodeLine = (line, idx) => {
    let html = line.text;
    html = html.replace(/#include|void|char|int/g, '<span class="keyword">$&</span>');
    html = html.replace(/vuln|main|read|printf|puts/g, '<span class="function">$&</span>');
    html = html.replace(/"([^"]*)"/g, '<span class="string">"$&"</span>').replace(/"<span class="string">"([^"]*)"<\/span>"/g, '<span class="string">"$1"</span>');
    html = html.replace(/\b([0-9]+)\b/g, '<span class="number">$&</span>');

    return (
      <div key={idx} className="line-wrapper">
        <div className={`code-line ${idx === activeLineIndex ? 'active' : ''}`}>
          <span className="line-number">{idx + 1}</span>
          <span className="line-content" dangerouslySetInnerHTML={{ __html: html }}></span>
        </div>
      </div>
    );
  };

  const handleNextStep = () => {
    // Check haptic feedback manually when stepping
    if (navigator.vibrate) navigator.vibrate(10); // Light tick

    switch (step) {
      case 0:
        setActiveLineIndex(12); // puts("I will echo...");
        setConsoleOut(prev => [...prev, { text: "I will echo whatever you say.", isGarbage: false }]);
        setStatusTitle("Executing puts()");
        setStatusDesc("The string is printed to standard output.");
        setStep(1);
        break;

      case 1:
        setActiveLineIndex(14); // vuln()
        setStatusTitle("Calling vuln()");
        setStatusDesc("main() makes a call to vuln(). The processor will push the return address (0x4004d0) so it knows where to return later.");

        let s2 = [...stack];
        s2[3].value = "0x4004d0"; // vuln Return Addr
        setStack(s2);
        setEspIndex(3);
        setStep(2);
        break;

      case 2:
        setActiveLineIndex(4); // char buff[16] (includes push rbp, etc in asm)
        setStatusTitle("vuln() Function Prologue");
        setStatusDesc("It sets up its stack frame: pushes the old EBP, sets EBP to ESP, and subtracts 0x10 (16) from ESP to make room for `buff[16]`.");

        let s3 = [...stack];
        s3[4].value = "0x7ffe4"; // Saved EBP from main
        setStack(s3);
        setEbpIndex(4);
        setEspIndex(6); // ESP points down to buff[0..7]
        setStep(3);
        break;

      case 3:
        setActiveLineIndex(6); // read(1, buff, 100)
        setStatusTitle("Unsafe read() call");
        setStatusDesc("`read` asks for up to 100 bytes from the user, but `buff` is only 16 bytes! Enter your input below. read() does NOT add a null terminator automatically like gets().");
        setStep(4); // Waiting for input
        break;

      case 5:
        // Input submitted, proceed to printf
        setActiveLineIndex(8); // printf("%s", buff)
        setStatusTitle("Executing printf()");
        setStatusDesc("printf() prints the string starting at `buff` until it hits a null byte '\\x00'. If `read` didn't place a null byte (or you overwrote past it), it will keep reading memory and print garbage/addresses!");

        // Compute output block
        let outText = userInput;
        let hasGarbage = false;
        if (userInput.length >= 16) {
          hasGarbage = true;
        }

        setConsoleOut(prev => [...prev, { text: outText, isGarbage: false }, hasGarbage ? { text: "\\x40\\x11\\xe8\\x00...", isGarbage: true } : null].filter(Boolean));

        setStep(6);
        break;

      case 6:
        setActiveLineIndex(9); // } vuln end
        setStatusTitle("vuln() Epilogue (leave/ret)");
        setStatusDesc("vuln() now prepares to return to main. It popping the Saved EBP and jumping to the Return Address in the stack.");

        if (userInput.length > 16) {
          if (navigator.vibrate) navigator.vibrate([200, 100, 200]); // heavy vibrate on crash
          setStatusType("danger");
          setStatusTitle("Segmentation Fault!");
          setStatusDesc("The Return Address was overwritten with user data! The CPU jumps to an invalid/garbage address. Execution crashes.");
          setStep(100); // Dead
        } else {
          setEspIndex(3);
          setEbpIndex(4); // Back to main
          setStep(7);
        }
        break;

      case 7:
        setActiveLineIndex(16); // puts(Goodbye)
        setStatusTitle("Returned Safely to main()");
        setStatusDesc("Since the buffer wasn't overflowed over the Return Address, execution continues normally.");
        setConsoleOut(prev => [...prev, { text: "Goodbye!!!", isGarbage: false }]);
        setStep(8);
        break;

      case 8:
        setActiveLineIndex(17); // } main end
        setStatusTitle("Program Exited Safely");
        setStatusDesc("Execution finished.");
        setStep(100); // Dead
        break;
    }
  };

  const handleInputSubmit = (e) => {
    e.preventDefault();
    if (!userInput) return;

    setInputSubmitted(true);
    let newStack = [...stack];

    // Fill buffer properly
    if (userInput.length <= 8) {
      newStack[6].value = userInput;
      newStack[6].type = "filled";
    } else if (userInput.length <= 16) {
      newStack[6].value = userInput.substring(0, 8);
      newStack[6].type = "filled";
      newStack[5].value = userInput.substring(8);
      newStack[5].type = "filled";
    }

    if (userInput.length > 16) {
      // OVERFLOW HAPPENS
      if (navigator.vibrate) navigator.vibrate([50, 50, 100]); // Warning vib
      newStack[6].value = "AAAAAAAA";
      newStack[5].value = "AAAAAAAA";
      newStack[6].type = "filled";
      newStack[5].type = "filled";

      let overflow = userInput.substring(16);
      if (overflow.length > 0) {
        newStack[4].value = "0x" + Array.from(overflow.substring(0, 8)).map(c => c.charCodeAt(0).toString(16)).join('');
        newStack[4].type = "danger"; // Overwrote EBP
        newStack[4].label = "vuln Saved EBP (CORRUPTED)";
      }
      if (overflow.length > 8) {
        newStack[3].value = "0x" + Array.from(overflow.substring(8, 16)).map(c => c.charCodeAt(0).toString(16)).join('');
        newStack[3].type = "danger"; // Overwrote Return Addr
        newStack[3].label = "vuln Return Addr (CORRUPTED)";
      }

      setStatusTitle("BUFFER OVERFLOW!");
      setStatusDesc(`You wrote ${userInput.length} bytes into a 16-byte buffer! The extra data overflowed and overwrote the Saved EBP and Return Address! Notice the stack values below.`);
      setStatusType("danger");
    } else {
      setStatusTitle("Input Received");
      setStatusDesc("Input was completely safely placed inside `buff`. Stack boundaries respected.");
      setStatusType("success");
    }

    setStack(newStack);
    setStep(5);
  };

  const resetSimulation = () => {
    setStep(0);
    setActiveLineIndex(11);
    setStack(INITIAL_STACK.map(s => ({ ...s, value: s.id < 5 ? "0x0" : s.value, type: s.id < 5 ? "neutral" : s.type })));
    setUserInput('');
    setInputSubmitted(false);
    setEspIndex(4);
    setEbpIndex(4);
    setStatusTitle("Simulation Reset");
    setStatusDesc("Click 'Next Step' to start again.");
    setStatusType("info");
    setConsoleOut([]);
  };

  const fillPayload = (type) => {
    if (type === 'safe') setUserInput('Hello!');
    if (type === 'leak') setUserInput('AAAAAAAAAAAAAAAA'); // exactly 16, no null, will leak EBP/Ret
    if (type === 'overflow') setUserInput('AAAAAAAAAAAAAAAA++++++++BBBBBBBB'); // 32 chars
  };

  const currentLineData = CODE_DATA[activeLineIndex];

  return (
    <div className="app-container">
      <header className="header">
        <h1>SecViz</h1>
        <p>Interactive Binary Exploitation Visualization</p>
      </header>

      <main className="main-content">

        {/* Code Section */}
        <section className="panel code-section">
          <div className="panel-header">
            <span>Vulnerable C Program</span>
            <button
              className="btn btn-secondary btn-small"
              onClick={() => setShowAssembly(true)}
            >
              Peek Assembly
            </button>
          </div>
          <div className="code-container">
            {CODE_DATA.map((line, idx) => renderCodeLine(line, idx))}
          </div>

          <div className="console-section">
            <div className="console-header">
              <span style={{ color: '#7ee787' }}>●</span>
              <span>Terminal / STDOUT</span>
            </div>
            <div className="console-body" ref={consoleRef}>
              {consoleOut.map((out, i) => (
                <span key={i} className={out.isGarbage ? 'garbage text-pulse' : ''}>
                  {out.text}{out.text.endsWith('\n') ? '' : '\n'}
                </span>
              ))}
            </div>
          </div>
        </section>

        {/* Stack & Controls Section */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '2rem', height: '100%', minWidth: '400px' }}>

          {/* Stack Section */}
          <section className="panel stack-section">
            <div className="panel-header">Memory Stack Frame</div>
            <div className="stack-container">
              {stack.map((block, index) => {
                const isEspHere = espIndex === (stack.length - 1 - index);
                const isEbpHere = ebpIndex === (stack.length - 1 - index);

                return (
                  <div key={block.id} className={`stack-frame ${block.type === 'main-frame' ? 'opacity-50' : ''}`}>
                    <div className="stack-address">{block.address}</div>
                    <div className={`stack-block ${block.type}`}>
                      <span className="block-label">{block.label}</span>
                      <span className="block-value">{block.value}</span>
                    </div>
                    {/* Pointer Indicators (Framer Motion!) */}
                    <div className="pointer-container">
                      {isEspHere && (
                        <motion.div
                          layoutId="ESP_POINTER"
                          transition={{ type: "spring", stiffness: 400, damping: 25 }}
                          className="pointer esp"
                        >
                          ESP
                        </motion.div>
                      )}
                      {isEbpHere && (
                        <motion.div
                          layoutId="EBP_POINTER"
                          transition={{ type: "spring", stiffness: 400, damping: 25 }}
                          className={`pointer ebp ${isEspHere ? 'stagger' : ''}`}
                        >
                          EBP
                        </motion.div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </section>

          {/* Controls Section */}
          <section className="panel controls-section" style={{ flex: 'none' }}>
            <div className="panel-header">Execution Control</div>

            <div className={`info-box ${statusType}`}>
              <div className="info-title">{statusTitle}</div>
              <div className="info-desc">{statusDesc}</div>

              {step === 4 && !inputSubmitted && (
                <form className="input-form" onSubmit={handleInputSubmit}>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="Enter string..."
                    value={userInput}
                    onChange={(e) => setUserInput(e.target.value)}
                    autoFocus
                  />
                  <button type="submit" className="btn-submit">Send</button>

                  <div style={{ display: 'flex', gap: '0.4rem', flexDirection: 'column' }}>
                    <button type="button" className="btn btn-secondary" style={{ fontSize: '0.75rem', padding: '0.3rem' }} onClick={() => fillPayload('safe')}>Safe (6b)</button>
                    <button type="button" className="btn btn-secondary" style={{ fontSize: '0.75rem', padding: '0.3rem' }} onClick={() => fillPayload('leak')}>Leak (16b)</button>
                    <button type="button" className="btn btn-secondary" style={{ fontSize: '0.75rem', padding: '0.3rem' }} onClick={() => fillPayload('overflow')}>Crash (32b)</button>
                  </div>
                </form>
              )}
            </div>

            <div className="button-group">
              <button className="btn btn-secondary" onClick={resetSimulation}>Reset</button>
              <button
                className="btn btn-primary"
                onClick={handleNextStep}
                disabled={step === 4 || step === 100}
              >
                Next Step
              </button>
            </div>
          </section>
        </div>

      </main>

      {/* Swipeable Bottom Sheet for Assembly */}
      <AnimatePresence>
        {showAssembly && (
          <>
            <motion.div
              className="bottom-sheet-backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowAssembly(false)}
            />
            <div className="bottom-sheet-container">
              <motion.div
                className="bottom-sheet"
                initial={{ y: "100%" }}
                animate={{ y: 0 }}
                exit={{ y: "100%" }}
                transition={{ type: "spring", damping: 25, stiffness: 300 }}
                drag="y"
                dragConstraints={{ top: 0 }}
                dragElastic={0.2}
                onDragEnd={(e, info) => {
                  // Swipe down to close
                  if (info.offset.y > 100 || info.velocity.y > 500) {
                    setShowAssembly(false);
                  }
                }}
              >
                <div className="sheet-handle" />
                <div className="sheet-content">
                  <h3>Assembly: <span className="keyword" style={{ fontSize: '0.85rem' }}>{currentLineData.text.trim() || "(No instruction mapped)"}</span></h3>

                  {currentLineData.asm && currentLineData.asm.length > 0 ? (
                    currentLineData.asm.map((a, i) => (
                      <div key={i} className="assembly-line highlight">{a}</div>
                    ))
                  ) : (
                    <div className="assembly-line">No assembly execution for this step.</div>
                  )}

                  <h3 style={{ marginTop: '2rem' }}>CPU Registers</h3>
                  <div style={{ display: 'flex', gap: '2rem', padding: '1rem', background: '#0a0a0a', borderRadius: '8px', border: '1px solid #30363d' }}>
                    <div><strong>RSP:</strong> <span className="keyword">{stack[stack.length - 1 - espIndex]?.value || "Unknown"}</span></div>
                    <div><strong>RBP:</strong> <span className="keyword">{stack[stack.length - 1 - ebpIndex]?.value || "Unknown"}</span></div>
                  </div>
                </div>
              </motion.div>
            </div>
          </>
        )}
      </AnimatePresence>

    </div>
  )
}

export default App
