import React, { useEffect, useRef } from 'react';
import { lerp } from '../utils/math.js';

export default function StackCanvas({ stack, espIndex, ebpIndex, userInput, step, level }) {
    const canvasRef = useRef(null);
    const animState = useRef({ fillHeight: 0, espY: 0, ebpY: 0, time: 0 });

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const dpr = window.devicePixelRatio || 1;
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);

        let animationFrameId;
        const blockHeight = Math.min(60, (rect.height - 40) / stack.length);
        const blockWidth = rect.width * 0.55;
        const startX = 80;
        const startY = 20;

        const draw = () => {
            ctx.clearRect(0, 0, rect.width, rect.height);
            animState.current.time += 0.05;

            const bufferSize = level.id === '2a' ? Math.min(userInput.length, 16) : userInput.length;
            const targetFillHeight = (step >= 5 && bufferSize > 0) ? (bufferSize / 8) * blockHeight : 0;
            const targetEspY = startY + (stack.length - 1 - espIndex) * blockHeight + blockHeight / 2;
            const targetEbpY = startY + (stack.length - 1 - ebpIndex) * blockHeight + blockHeight / 2;

            animState.current.fillHeight = lerp(animState.current.fillHeight, targetFillHeight, 0.1);
            animState.current.espY = lerp(animState.current.espY, targetEspY, 0.15);
            animState.current.ebpY = lerp(animState.current.ebpY, targetEbpY, 0.15);

            stack.forEach((block, index) => {
                const visualIndex = stack.length - 1 - index;
                const y = startY + visualIndex * blockHeight;

                ctx.fillStyle = block.type === 'main-frame' ? 'rgba(163, 113, 247, 0.15)' :
                    block.type === 'safe' ? 'rgba(35, 134, 54, 0.1)' :
                        block.type === 'warn' ? 'rgba(210, 153, 34, 0.15)' :
                            'rgba(33, 38, 45, 0.8)';

                if (block.type === 'danger') ctx.fillStyle = 'rgba(248, 81, 73, 0.15)';

                ctx.strokeStyle = block.type === 'danger' ? 'rgba(248, 81, 73, 0.6)' :
                    block.type === 'warn' ? 'rgba(210, 153, 34, 0.6)' :
                        'rgba(255, 255, 255, 0.1)';
                ctx.lineWidth = 2;
                ctx.fillRect(startX, y, blockWidth, blockHeight);
                ctx.strokeRect(startX, y, blockWidth, blockHeight);

                ctx.fillStyle = '#8b949e';
                ctx.font = '12px "Fira Code", monospace';
                ctx.textAlign = 'right';
                ctx.fillText(block.address, startX - 10, y + blockHeight / 2 + 4);

                ctx.fillStyle = '#c9d1d9';
                ctx.textAlign = 'left';
                ctx.font = '13px "Inter", sans-serif';
                ctx.fillText(block.label, startX + 15, y + 22);

                ctx.fillStyle = block.type === 'danger' ? '#ff7b72' :
                    block.type === 'warn' ? '#d29922' : '#fff';
                ctx.font = 'bold 14px "Fira Code", monospace';
                let val = block.value;
                if (val.includes('\\0')) {
                    val = val.replace(/\\0/g, '·');
                }
                ctx.fillText(val, startX + 15, y + blockHeight - 12);
            });

            // Fluid Simulation
            let bufferBottomIndex = stack.findIndex(s => s.label.startsWith("buff[0"));
            if (bufferBottomIndex !== -1 && animState.current.fillHeight > 0.5) {
                let visualBottomIndex = stack.length - 1 - bufferBottomIndex;
                const bufferVisualBottomY = startY + visualBottomIndex * blockHeight + blockHeight;
                const bufferVisualTopY = startY + (visualBottomIndex - 1) * blockHeight;
                const totalBufferHeight = bufferVisualBottomY - bufferVisualTopY;

                const fluidTopY = bufferVisualBottomY - animState.current.fillHeight;
                const isOverflow = animState.current.fillHeight > totalBufferHeight;

                ctx.save();
                ctx.beginPath();
                ctx.rect(startX, 0, blockWidth, rect.height);
                ctx.clip();

                const gradient = ctx.createLinearGradient(0, Math.max(0, fluidTopY), 0, bufferVisualBottomY);
                if (isOverflow) {
                    gradient.addColorStop(0, 'rgba(248, 81, 73, 0.6)');
                    gradient.addColorStop(1, 'rgba(248, 81, 73, 0.2)');
                } else {
                    gradient.addColorStop(0, 'rgba(88, 166, 255, 0.6)');
                    gradient.addColorStop(1, 'rgba(88, 166, 255, 0.2)');
                }

                ctx.fillStyle = gradient;
                ctx.beginPath();
                ctx.moveTo(startX, bufferVisualBottomY);
                ctx.lineTo(startX, fluidTopY);

                for (let i = 0; i <= blockWidth; i += 5) {
                    const waveOffset = Math.sin(animState.current.time + i * 0.05) * 4;
                    ctx.lineTo(startX + i, fluidTopY + waveOffset);
                }

                ctx.lineTo(startX + blockWidth, bufferVisualBottomY);
                ctx.closePath();
                ctx.fill();
                ctx.restore();
            }

            // Draw pointers
            const drawPointer = (name, index, y, color, isStaggered) => {
                const pointerLeft = startX + blockWidth + 15;
                const pointerY = y + (isStaggered ? 15 : -10);
                const boxWidth = 45;
                const boxHeight = 22;

                ctx.fillStyle = color;
                ctx.fillRect(pointerLeft + 10, pointerY - boxHeight / 2, boxWidth, boxHeight);

                ctx.beginPath();
                ctx.moveTo(pointerLeft + 10, pointerY - boxHeight / 2);
                ctx.lineTo(pointerLeft, pointerY);
                ctx.lineTo(pointerLeft + 10, pointerY + boxHeight / 2);
                ctx.fill();

                ctx.fillStyle = '#0d1117';
                ctx.font = 'bold 12px "Inter", sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText(name, pointerLeft + 10 + boxWidth / 2, pointerY + 4);
            };

            if (espIndex >= 0) drawPointer("ESP", espIndex, animState.current.espY, "#58a6ff", false);
            if (ebpIndex >= 0) drawPointer("EBP", ebpIndex, animState.current.ebpY, "#a371f7", Math.abs(animState.current.espY - animState.current.ebpY) < 10);

            animationFrameId = requestAnimationFrame(draw);
        };

        draw();
        return () => cancelAnimationFrame(animationFrameId);
    }, [stack, espIndex, ebpIndex, userInput, step, level]);

    return (
        <div style={{ width: '100%', height: '100%', overflow: 'hidden', borderRadius: '8px' }}>
            <canvas ref={canvasRef} style={{ width: '100%', height: '100%', display: 'block' }} />
        </div>
    );
}
