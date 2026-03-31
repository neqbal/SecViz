import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { LEVELS } from './levels.js';
import LevelPlayer from './components/LevelPlayer.jsx';
import './index.css';

export default function App() {
  const [levelIdx, setLevelIdx] = useState(0);
  const [showWin, setShowWin] = useState(false);

  const handleNextLevel = () => {
    setShowWin(false);
    if (levelIdx < LEVELS.length - 1) setLevelIdx(levelIdx + 1);
  };

  return (
    <div className="app-container">
      <header className="header">
        <h1>SecViz</h1>
        <p>Interactive Binary Exploitation Visualization</p>
      </header>

      <LevelPlayer key={levelIdx} level={LEVELS[levelIdx]} onWin={() => setShowWin(true)} />

      <AnimatePresence>
        {showWin && (
          <motion.div
            className="bottom-sheet-backdrop"
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          >
            <motion.div
              className="panel" style={{ maxWidth: '600px', width: '100%', textAlign: 'center', border: '1px solid var(--accent)', background: 'var(--bg-color)', backdropFilter: 'blur(20px)' }}
              initial={{ scale: 0.9, y: 20 }} animate={{ scale: 1, y: 0 }}
            >
              <h2 style={{ color: 'var(--success)', marginBottom: '1rem', fontSize: '2rem' }}>{LEVELS[levelIdx].successTitle}</h2>
              <p style={{ color: 'var(--text-muted)', lineHeight: '1.6', marginBottom: '2rem' }}>{LEVELS[levelIdx].successDesc}</p>

              {levelIdx < LEVELS.length - 1 ? (
                <button className="btn btn-primary" onClick={handleNextLevel}>Proceed to Next Scenario →</button>
              ) : (
                <div style={{ color: 'var(--accent)', fontSize: '1.2rem', fontWeight: 600 }}>🎉 You have completed all SecViz modules! 🎉</div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
