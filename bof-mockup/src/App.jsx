import React, { useState } from 'react';
import { LEVELS } from './levels.js';
import LevelPlayer from './components/LevelPlayer.jsx';
import './index.css';

export default function App() {
  const [levelIdx, setLevelIdx] = useState(0);

  const handleNextLevel = () => {
    if (levelIdx < LEVELS.length - 1) setLevelIdx(levelIdx + 1);
  };

  return (
    <div className="app-container">
      <header className="header">
        <h1>SecViz</h1>
        <p>Interactive Binary Exploitation Visualization</p>
      </header>

      <LevelPlayer key={levelIdx} level={LEVELS[levelIdx]} onWin={() => { }} onNextLevel={handleNextLevel} />
    </div>
  );
}
