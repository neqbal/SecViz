import { useState, useEffect } from 'react'
import './index.css'

const CODE_LINES = [
  { text: "void vulnerable_function() {", isFunction: true },
  { text: "    char buffer[16]; // Allocating 16 bytes", isInstruction: true },
  { text: "    ", isInstruction: false },
  { text: "    printf(\"Enter password: \");", isInstruction: true },
  { text: "    gets(buffer); // Unsafe! No bounds checking", isInstruction: true },
  { text: "    ", isInstruction: false },
  { text: "    if (strcmp(buffer, \"secret\") == 0) {", isInstruction: true },
  { text: "        grant_access();", isInstruction: true },
  { text: "    } else {", isInstruction: false },
  { text: "        printf(\"Access Denied.\");", isInstruction: true },
  { text: "    }", isInstruction: false },
  { text: "}", isFunction: true },
  { text: "", isInstruction: false },
  { text: "void win() {", isFunction: true, isTarget: true },
  { text: "    system(\"/bin/sh\"); // We want to jump here!", isInstruction: true, isTarget: true },
  { text: "}", isFunction: true, isTarget: true }
]

const INITIAL_STACK = [
  { id: 5, address: "0x7fffc", label: "Arg 1", value: "0x0", size: 8, type: "safe" },
  { id: 4, address: "0x7fff4", label: "Return Address", value: "0x4010a2 (main+24)", size: 8, type: "safe" },
  { id: 3, address: "0x7ffec", label: "Saved EBP", value: "0x7ffff", size: 8, type: "safe" },
  { id: 2, address: "0x7ffe4", label: "buffer[8..15]", value: "0x0", size: 8, type: "neutral" },
  { id: 1, address: "0x7ffdc", label: "buffer[0..7]", value: "0x0", size: 8, type: "neutral" }
]

function App() {
  const [step, setStep] = useState(0);
  const [activeLineIndex, setActiveLineIndex] = useState(0);
  const [stack, setStack] = useState(INITIAL_STACK);
  const [userInput, setUserInput] = useState('');
  const [inputSubmitted, setInputSubmitted] = useState(false);
  const [espIndex, setEspIndex] = useState(-1);
  const [ebpIndex, setEbpIndex] = useState(-1);
  const [statusTitle, setStatusTitle] = useState("Introduction to Buffer Overflow");
  const [statusDesc, setStatusDesc] = useState("Welcome to SecViz! We will explore how unchecked inputs can overwrite memory. Click 'Next Step' to begin function execution.");
  const [statusType, setStatusType] = useState("info"); // info, danger, success

  // Syntax highlighting helper
  const renderCodeLine = (line, idx) => {
    let html = line.text;
    html = html.replace(/void|char|if|else/g, '<span class="keyword">$&</span>');
    html = html.replace(/vulnerable_function|printf|gets|strcmp|grant_access|system|win/g, '<span class="function">$&</span>');
    html = html.replace(/"[^"]*"/g, '<span class="string">$&</span>');
    html = html.replace(/\/\/.*$/g, '<span class="comment">$&</span>');

    return (
      <div key={idx} className={`code-line ${idx === activeLineIndex ? 'active' : ''} ${line.isTarget ? 'shake' : ''}`}>
        <span className="line-number">{idx + 1}</span>
        <span className="line-content" dangerouslySetInnerHTML={{ __html: html }}></span>
      </div>
    );
  };

  const handleNextStep = () => {
    if (step === 0) {
      setActiveLineIndex(0); // Function entry
      setEspIndex(INITIAL_STACK.length - 1); // Point to arg
      setEbpIndex(INITIAL_STACK.length - 1);
      setStatusTitle("Function Entry");
      setStatusDesc("The program calls vulnerable_function(). The Return Address and previous function's Base Pointer (Saved EBP) are pushed to the stack.");
      setStep(1);
    } 
    else if (step === 1) {
      setActiveLineIndex(1); // Allocating buffer
      setEspIndex(0); // Move ESP down
      setEbpIndex(2); // EBP at Saved EBP
      setStatusTitle("Stack Frame Allocation");
      setStatusDesc("The function allocates 16 bytes for the `buffer` array. The stack pointer (ESP) moves down to reserve this space.");
      setStep(2);
    }
    else if (step === 2) {
      setActiveLineIndex(3); // printf
      setStatusTitle("Asking for Input");
      setStatusDesc("The program prompts the user for a password.");
      setStep(3);
    }
    else if (step === 3) {
      setActiveLineIndex(4); // gets
      setStatusTitle("Unsafe Input Function");
      setStatusDesc("The `gets()` function reads input from the user until a newline is found. It DOES NOT check if the input fits inside the 16-byte buffer! Try entering a string longer than 16 characters (e.g., 24 'A's).");
      setStep(4);
    }
    else if (step === 5) {
      if (userInput.length > 16) {
        setActiveLineIndex(11); // Reached end of function
        setEspIndex(3);
        setStatusTitle("Returning... Wait!");
        setStatusDesc("The function finishes. It looks at the Return Address to know where to execute next. However, the Return Address was corrupted! The processor will now try to execute code at \"AAAA\". Segmentation Fault.");
        setStatusType("danger");
        setStep(6);
      } else {
        setActiveLineIndex(6); // Checking password
        setStatusTitle("Password Check");
        setStatusDesc("The input fits nicely within the buffer. The program safely checks the password.");
        setStep(6); // Normal execution path
      }
    }
  };

  const handleInputSubmit = (e) => {
    e.preventDefault();
    if (!userInput) return;
    
    setInputSubmitted(true);
    let newStack = [...stack];
    
    if (userInput.length <= 8) {
        newStack[4].value = userInput;
        newStack[4].type = "filled";
    } else if (userInput.length <= 16) {
        newStack[4].value = userInput.substring(0, 8);
        newStack[4].type = "filled";
        newStack[3].value = userInput.substring(8);
        newStack[3].type = "filled";
    }

    if (userInput.length > 16) {
        // OVERFLOW HAPPENS
        newStack[4].value = "AAAAAAAA";
        newStack[3].value = "AAAAAAAA";
        newStack[4].type = "filled";
        newStack[3].type = "filled";
        
        let overflow = userInput.substring(16);
        if (overflow.length > 0) {
            newStack[2].value = "0x" + Array.from(overflow.substring(0,8)).map(c => c.charCodeAt(0).toString(16)).join('');
            newStack[2].type = "danger"; // Overwrote EBP
            newStack[2].label = "Saved EBP (CORRUPTED)";
        }
        if (overflow.length > 8) {
            newStack[1].value = "0x" + Array.from(overflow.substring(8, 16)).map(c => c.charCodeAt(0).toString(16)).join('');
            newStack[1].type = "danger"; // Overwrote Return Addr
            newStack[1].label = "Return Addr (CORRUPTED)";
        }

        setStatusTitle("BUFFER OVERFLOW!");
        setStatusDesc(`The input '${userInput}' was ${userInput.length} bytes long, but the buffer only holds 16 bytes! The extra data overflowed outside the buffer and overwrote the Saved EBP and Return Address!`);
        setStatusType("danger");
    } else {
        setStatusTitle("Input Received");
        setStatusDesc("The input fit securely within the 16-byte buffer bounds. The adjacent memory remains safe.");
        setStatusType("success");
    }
    
    setStack(newStack);
    setStep(5);
  };

  const resetSimulation = () => {
    setStep(0);
    setActiveLineIndex(0);
    setStack(INITIAL_STACK.map(s => ({...s, value: "0x0" === s.value ? "0x0" : s.value, type: s.id > 3 ? "safe" : "neutral"})));
    setUserInput('');
    setInputSubmitted(false);
    setEspIndex(-1);
    setEbpIndex(-1);
    setStatusTitle("Simulation Reset");
    setStatusDesc("Click 'Next Step' to start again.");
    setStatusType("info");
  };

  // Quick preset payloads
  const fillPayload = (type) => {
      if(type === 'safe') setUserInput('secret');
      if(type === 'overflow') setUserInput('AAAAAAAAAAAAAAAAAAAAAAAAAAAA');
  };

  return (
    <div className="app-container">
      <header className="header">
        <h1>SecViz</h1>
        <p>Interactive Binary Exploitation Visualization</p>
      </header>

      <main className="main-content">
        
        {/* Code Section */}
        <section className="panel code-section">
          <div className="panel-header">Vulnerable Function</div>
          <div className="code-container">
            {CODE_LINES.map((line, idx) => renderCodeLine(line, idx))}
          </div>
        </section>

        {/* Stack Section */}
        <section className="panel stack-section">
          <div className="panel-header">Memory Stack Frame</div>
          <div className="stack-container">
            {stack.map((block, index) => (
              <div key={block.id} className="stack-frame">
                <div className="stack-address">{block.address}</div>
                <div className={`stack-block ${block.type}`}>
                  <span className="block-label">{block.label}</span>
                  <span className="block-value">{block.value}</span>
                </div>
                {/* Pointer Indicators */}
                <div className="pointer-container" style={{right: '-80px'}}>
                  {espIndex === (stack.length - 1 - index) && <div className="pointer esp">ESP</div>}
                  {ebpIndex === (stack.length - 1 - index) && <div className="pointer ebp">EBP</div>}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Controls Section */}
        <section className="panel controls-section">
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
                <button type="submit" className="btn-submit">Submit</button>
                <div style={{marginTop: '0.5rem', display: 'flex', gap: '0.5rem', width: '100%'}}>
                    <button type="button" className="btn btn-secondary" style={{fontSize: '0.8rem', padding: '0.3rem'}} onClick={() => fillPayload('safe')}>Preset Safe</button>
                    <button type="button" className="btn btn-secondary" style={{fontSize: '0.8rem', padding: '0.3rem'}} onClick={() => fillPayload('overflow')}>Preset Overflow</button>
                </div>
              </form>
            )}
          </div>

          <div className="button-group">
            <button className="btn btn-secondary" onClick={resetSimulation}>Reset</button>
            <button 
              className="btn btn-primary" 
              onClick={handleNextStep}
              disabled={step === 4 || step >= 6}
            >
              Next Step {step >= 6 ? "(Crashing)" : ""}
            </button>
          </div>
        </section>

      </main>
    </div>
  )
}

export default App
