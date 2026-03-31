import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import StackCanvas from './StackCanvas.jsx';

export default function LevelPlayer({ level, onWin }) {
    const [step, setStep] = useState(0);
    const [activeLineIndex, setActiveLineIndex] = useState(level.startCodeLine);
    const [stack, setStack] = useState(level.initialStack);
    const [userInput, setUserInput] = useState('');
    const [inputSubmitted, setInputSubmitted] = useState(false);

    const initialEsp = level.initialStack.findIndex(s => s.label.includes("main Saved EBP")) !== -1
        ? level.initialStack.findIndex(s => s.label.includes("main Saved EBP"))
        : level.initialStack.findIndex(s => s.label.includes("main Frame Data"));
    const initialEbp = initialEsp;

    const [espIndex, setEspIndex] = useState(initialEsp);
    const [ebpIndex, setEbpIndex] = useState(initialEbp);

    const [statusTitle, setStatusTitle] = useState("Program Executed");
    const [statusDesc, setStatusDesc] = useState("Execution starts in the main() function. Click 'Next Step' to proceed.");
    const [statusType, setStatusType] = useState("info");

    const [consoleOut, setConsoleOut] = useState([]);
    const [showAssembly, setShowAssembly] = useState(false);
    const consoleRef = useRef(null);

    useEffect(() => {
        if (consoleRef.current) consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
    }, [consoleOut]);

    const renderCodeLine = (line, idx) => {
        let html = line.text;
        html = html.replace(/#include|void|char|int|long|if/g, '<span class="keyword">$&</span>');
        html = html.replace(/vuln|main|read|printf|puts|system|__stack_chk_fail/g, '<span class="function">$&</span>');
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
        if (navigator.vibrate) navigator.vibrate(10);

        switch (step) {
            case 0:
                let nextLineIdx = level.startCodeLine + 1;
                setActiveLineIndex(nextLineIdx);
                if (level.code[nextLineIdx].text.includes("puts")) {
                    setConsoleOut(prev => [...prev, { text: "I will echo whatever you say.", isGarbage: false }]);
                    setStatusTitle("Executing puts()");
                    setStep(1);
                } else {
                    setStatusTitle("Calling vuln()");
                    let baseEsp = initialEsp + 1;
                    let s2 = [...stack];
                    s2[baseEsp].value = level.id === '1' ? "0x400490" : "0x4004ec";
                    setStack(s2);
                    setEspIndex(baseEsp);
                    setStep(2);
                }
                break;

            case 1:
                let callVulnIdx = level.code.findIndex((l, i) => i > level.startCodeLine && l.text.includes("vuln();"));
                setActiveLineIndex(callVulnIdx);
                setStatusTitle("Calling vuln()");
                let baseEsp2 = initialEsp + 1;
                let s2b = [...stack];
                s2b[baseEsp2].value = level.id === '1' ? "0x400490" : "0x4004ec";
                setStack(s2b);
                setEspIndex(baseEsp2);
                setStep(2);
                break;

            case 2:
                let firstVarIdx = level.code.findIndex(l => l.text.includes("char secret_key") || l.text.includes("long fs_canary") || l.text.includes("char buff"));
                setActiveLineIndex(firstVarIdx);
                setStatusTitle("vuln() Local Variables Allocation");

                let vulnEbpIdx = initialEsp + 2;
                let s3 = [...stack];
                s3[vulnEbpIdx].value = "0x7ffe4";
                setStack(s3);
                setEbpIndex(vulnEbpIdx);
                let buff0Idx = stack.findIndex(s => s.label === "buff[0..7]");
                setEspIndex(buff0Idx);

                if (!level.code[firstVarIdx].text.includes("char buff")) {
                    setStep(2.5);
                } else {
                    setStep(3);
                }
                break;

            case 2.5:
                let buffIdx = level.code.findIndex(l => l.text.includes("char buff"));
                setActiveLineIndex(buffIdx);
                setStatusTitle("vuln() Array Allocation");

                let s4 = [...stack];
                if (level.id === '2a') {
                    let sk1 = s4.findIndex(s => s.label === "secret_key[0..7]");
                    let sk2 = s4.findIndex(s => s.label === "secret_key[8..15]");
                    if (sk1 !== -1) s4[sk1].value = "SUPER_SE";
                    if (sk2 !== -1) s4[sk2].value = "CRET_KEY";
                } else if (level.id === '3a') {
                    let cIdx = s4.findIndex(s => s.label.includes("CANARY"));
                    if (cIdx !== -1) s4[cIdx].value = "0x9f2ba321";
                }
                setStack(s4);

                setStep(3);
                break;

            case 3:
                let readIdx = level.code.findIndex(l => l.text.includes("read(1, "));
                let putsInVulnIdx = level.code.findIndex((l, i) => i > activeLineIndex && i < readIdx && l.text.includes("puts"));

                if (putsInVulnIdx !== -1) {
                    setActiveLineIndex(putsInVulnIdx);
                    let textToPrint = level.code[putsInVulnIdx].text.match(/"([^"]+)"/);
                    setConsoleOut(prev => [...prev, { text: textToPrint ? textToPrint[1] : "...", isGarbage: false }]);
                    setStatusTitle("Executing puts()");
                    setStep(3.5);
                } else {
                    setActiveLineIndex(readIdx); // read
                    if (level.id === '2a') {
                        setStatusDesc("read() bounds-checks perfectly to 16 bytes, but doesn't append a null terminator. If you fill exactly 16 bytes, it will bleed into the adjacent secret!");
                    }
                    setStatusTitle("read() Execution");
                    setStep(4);
                }
                break;

            case 3.5:
                let readIdx2 = level.code.findIndex(l => l.text.includes("read(1, "));
                setActiveLineIndex(readIdx2);
                if (level.id === '2a') {
                    setStatusDesc("read() bounds-checks perfectly to 16 bytes, but doesn't append a null terminator. If you fill exactly 16 bytes, it will bleed into the adjacent secret!");
                }
                setStatusTitle("read() Execution");
                setStep(4);
                break;

            case 5:
                let printfIdx = level.code.findIndex(l => l.text.includes("printf"));
                if (printfIdx !== -1) {
                    setActiveLineIndex(printfIdx);
                    setStatusTitle("Executing printf()");
                    let outText = userInput.substring(0, level.id === '2a' ? 16 : 100);
                    let leakGarbage = '';

                    if (level.id === '1' && userInput.length >= 16) {
                        leakGarbage = '\\x40\\x11\\xe8\\x00';
                    } else if (level.id === '2a' && userInput.length === 16) {
                        leakGarbage = 'SUPER_SECRET_KEY';
                    }
                    setConsoleOut(prev => [...prev, { text: outText, isGarbage: false }, leakGarbage ? { text: leakGarbage, isGarbage: true } : null].filter(Boolean));
                } else {
                    let rIdx = level.code.findIndex(l => l.text.includes("read(1, "));
                    let endIdx = level.code.findIndex((l, idx) => l.text === "}" && idx > rIdx);
                    setActiveLineIndex(endIdx);
                    setStatusTitle("vuln() Epilogue checks");
                }
                setStep(6);
                break;

            case 6:
                let endIdx2 = level.code.findIndex((l, i) => l.text === "}" && i > level.startCodeLine - 5);
                setActiveLineIndex(endIdx2);

                setStatusTitle("vuln() Epilogue");

                // Canary Check for Level 3A
                if (level.goal === 'CANARY_ABORT') {
                    let canaryChkIdx = level.code.findIndex(l => l.text.includes("fs_canary !="));
                    if (canaryChkIdx !== -1) {
                        setActiveLineIndex(canaryChkIdx);
                        let canaryBlock = stack.find(s => s.label.includes("CANARY"));
                        if (canaryBlock && canaryBlock.type === 'danger') {
                            if (navigator.vibrate) navigator.vibrate([100, 50, 100]);
                            setStatusType("danger");
                            setStatusTitle("*** stack smashing detected ***: terminated");
                            setStatusDesc("The Canary value was modified by your buffer overflow!");
                            setActiveLineIndex(canaryChkIdx + 1); // __stack_chk_fail()
                            setStep(100);
                            setTimeout(() => onWin(), 3000);
                            return;
                        }
                    }
                }

                let returnedSafely = true;
                let retBlock = stack.find(s => s.label.includes("Return Addr") && !s.label.includes("main"));

                if (userInput.length > 16 && retBlock && retBlock.type === 'danger') {
                    returnedSafely = false;
                }

                if (!returnedSafely) {
                    if (level.goal === 'HIJACK' && retBlock.value.includes("x42\\x11\\x40")) {
                        if (navigator.vibrate) navigator.vibrate([100, 50, 100, 50, 100]);
                        setStatusType("success");
                        setStatusTitle("ROOT SHELL SPAWNED!");
                        setStatusDesc("You redirected execution to win() perfectly! 0x401142 was popped into RIP.");
                        setStep(100);
                        setTimeout(() => onWin(), 3500);
                    } else if (level.goal === 'PIE_FAIL' && retBlock.value.includes("x42\\x11\\x40")) {
                        if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
                        setStatusType("danger");
                        setStatusTitle("Segmentation Fault!");
                        setStatusDesc("You jumped to 0x401142, but win() is NOT located there anymore because PIE randomized the app's base address! Execution crashed.");
                        setStep(100);
                        setTimeout(() => onWin(), 3500);
                    } else {
                        if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
                        setStatusType("danger");
                        setStatusTitle("Segmentation Fault!");
                        setStatusDesc("Execution crashed. The Return Address did not point to a valid function.");
                        setStep(100);
                        if (level.goal === 'CRASH') setTimeout(() => onWin(), 2000);
                    }
                } else {
                    setEspIndex(initialEsp - 1);
                    setEbpIndex(initialEbp);
                    setStep(7);
                }
                break;

            case 7:
                let putsIdx = level.code.findIndex((l, i) => i > level.startCodeLine && l.text.includes("puts(") && i > level.code.findIndex(x => x.text.includes("vuln()")));
                if (putsIdx !== -1) {
                    setActiveLineIndex(putsIdx);
                    setStatusTitle("Returned Safely to main()");
                    setConsoleOut(prev => [...prev, { text: "Goodbye!!!", isGarbage: false }]);
                    setStep(8);
                } else {
                    let endMainIdx = level.code.findIndex((l, i) => i > level.startCodeLine && l.text === "}");
                    setActiveLineIndex(endMainIdx);
                    setStatusTitle("Program Exited");
                    setStep(100);
                    if (level.goal === 'LEAK' && userInput.length === 16) {
                        setTimeout(() => onWin(), 1500);
                    }
                }
                break;

            case 8:
                let mainEndIdx = level.code.findIndex((l, i) => i > level.startCodeLine && l.text === "}");
                setActiveLineIndex(mainEndIdx);
                setStatusTitle("Program Exited");
                setStep(100);

                if (level.goal === 'LEAK' && userInput.length === 16) {
                    setTimeout(() => onWin(), 1500);
                } else if (level.goal === 'LEAK') {
                    setStatusDesc("Program finished safely, but we didn't leak the entire secret. Try submitting exactly 16 bytes.");
                    setStatusType("warn");
                }
                break;
        }
    };

    const handleInputSubmit = (e) => {
        e.preventDefault();
        if (!userInput) return;
        setInputSubmitted(true);

        let newStack = stack.map(s => ({ ...s }));
        let remainingInput = userInput;

        let bufferStartIndex = newStack.findIndex(s => s.label.startsWith("buff[0"));
        let currentIdx = bufferStartIndex;
        let hasOverflowedBuffer = false;

        while (remainingInput.length > 0 && currentIdx >= 0) {
            let chunk = remainingInput.substring(0, 8);
            remainingInput = remainingInput.substring(8);

            let targetBlock = newStack[currentIdx];

            if (!targetBlock.label.startsWith("buff")) {
                hasOverflowedBuffer = true;
                targetBlock.type = "danger";
                if (!targetBlock.label.includes("CORRUPT")) targetBlock.label += " (CORRUPTED)";
            } else {
                targetBlock.type = "filled";
            }

            targetBlock.value = chunk.padEnd(chunk.length === 8 ? 8 : chunk.length + 1, '\\0');
            currentIdx--;

            if (level.id === '2a' && currentIdx < bufferStartIndex - 1) {
                break;
            }
        }

        if (hasOverflowedBuffer) {
            setStatusTitle("BUFFER OVERFLOW!");
            setStatusType("danger");
            if (navigator.vibrate) navigator.vibrate([50, 50, 100]);
        } else {
            setStatusTitle("Input Received safely");
            setStatusType("success");
        }

        setStack(newStack);
        setStep(5);

        // Auto advance if no printf step exists
        let printfIdx = level.code.findIndex(l => l.text.includes("printf"));
        if (printfIdx === -1) {
            setTimeout(handleNextStep, 500); // give it half a sec then move to case 6
        }
    };

    const resetSimulation = () => {
        setStep(0);
        setActiveLineIndex(level.startCodeLine);
        setStack(level.initialStack.map(s => ({ ...s })));
        setUserInput('');
        setInputSubmitted(false);
        setEspIndex(initialEsp);
        setEbpIndex(initialEbp);
        setStatusTitle("Ready");
        setStatusDesc("Click 'Next Step' to start execution.");
        setStatusType("info");
        setConsoleOut([]);
    };

    const currentLineData = level.code[activeLineIndex];

    return (
        <>
            <main className="main-content">
                <section className="panel code-section">
                    <div className="panel-header" style={{ marginBottom: '0.5rem' }}>
                        <div>
                            <div style={{ fontSize: '0.9rem', color: 'var(--accent)', textTransform: 'uppercase', letterSpacing: '1px' }}>{level.title}</div>
                            <div style={{ fontSize: '1rem', fontWeight: 400, color: 'var(--text-muted)' }}>{level.subtitle}</div>
                        </div>
                        <button className="btn btn-secondary btn-small" onClick={() => setShowAssembly(true)}>Peek Assembly</button>
                    </div>
                    <div className="code-container">
                        {level.code.map((line, idx) => renderCodeLine(line, idx))}
                    </div>

                    <div className="console-section">
                        <div className="console-header">
                            <span style={{ color: '#7ee787' }}>●</span><span>Terminal / STDOUT</span>
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

                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '2rem', height: '100%', minWidth: '400px' }}>
                    <section className="panel stack-section" style={{ padding: '0', display: 'flex', flex: 1 }}>
                        <StackCanvas stack={stack} espIndex={espIndex} ebpIndex={ebpIndex} userInput={userInput} step={step} level={level} />
                    </section>

                    <section className="panel controls-section" style={{ flex: 'none' }}>
                        <div className="panel-header">Execution Control</div>
                        <div className={`info-box ${statusType}`}>
                            <div className="info-title">{statusTitle}</div>
                            <div className="info-desc">{statusDesc}</div>

                            {step === 4 && !inputSubmitted && (
                                <form className="input-form" onSubmit={handleInputSubmit}>
                                    <input type="text" className="input-field" placeholder="Enter string..." value={userInput} onChange={(e) => setUserInput(e.target.value)} autoFocus />
                                    <button type="submit" className="btn-submit">Send</button>

                                    <div style={{ display: 'flex', gap: '0.4rem', flexDirection: 'column' }}>
                                        {level.payloadPresets.map((preset, i) => (
                                            <button key={i} type="button" className="btn btn-secondary" style={{ fontSize: '0.75rem', padding: '0.3rem' }} onClick={() => setUserInput(preset.value)}>{preset.label}</button>
                                        ))}
                                    </div>
                                </form>
                            )}
                        </div>

                        <div className="button-group">
                            <button className="btn btn-secondary" onClick={resetSimulation}>Reset</button>
                            <button className="btn btn-primary" onClick={handleNextStep} disabled={step === 4 || step === 100}>Next Step</button>
                        </div>
                    </section>
                </div>
            </main>

            <AnimatePresence>
                {showAssembly && (
                    <>
                        <motion.div className="bottom-sheet-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowAssembly(false)} />
                        <div className="bottom-sheet-container">
                            <motion.div className="bottom-sheet" initial={{ y: "100%" }} animate={{ y: 0 }} exit={{ y: "100%" }} transition={{ type: "spring", damping: 25, stiffness: 300 }} drag="y" dragConstraints={{ top: 0 }} dragElastic={0.2} onDragEnd={(e, i) => { if (i.offset.y > 100) setShowAssembly(false); }}>
                                <div className="sheet-handle" />
                                <div className="sheet-content">
                                    <h3>Assembly: <span className="keyword" style={{ fontSize: '0.85rem' }}>{currentLineData?.text.trim() || "(No instruction mapped)"}</span></h3>
                                    {currentLineData?.asm && currentLineData.asm.length > 0 ? (
                                        currentLineData.asm.map((a, i) => <div key={i} className="assembly-line highlight">{a}</div>)
                                    ) : <div className="assembly-line">No assembly execution for this step.</div>}
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
        </>
    );
}
