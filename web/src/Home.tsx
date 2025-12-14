import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { usePageEffects } from './usePageEffects';
import { Smoke } from '@/components/ui/shadcn-io/smoke';
import { motion, AnimatePresence } from 'framer-motion';
import './index.css';

const CountdownUnit = ({ value, label }: { value: number; label: string }) => {
    const formattedValue = String(value).padStart(2, '0');

    return (
        <div className="countdown-item">
            <div style={{
                position: 'relative',
                display: 'inline-flex',
                justifyContent: 'center',
                overflow: 'hidden',
                padding: '10px 0', // Increased padding for mask space
                maskImage: 'linear-gradient(to bottom, transparent 0%, black 25%, black 75%, transparent 100%)',
                WebkitMaskImage: 'linear-gradient(to bottom, transparent 0%, black 25%, black 75%, transparent 100%)'
            }}>
                {/* Ghost element to reserve layout space */}
                <span className="countdown-number" style={{ opacity: 0, visibility: 'hidden', pointerEvents: 'none', position: 'relative', zIndex: -1 }}>
                    88
                </span>

                {/* Animated Numbers */}
                <div style={{ position: 'absolute', inset: 0, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                    <AnimatePresence mode="popLayout" initial={false}>
                        <motion.span
                            key={value}
                            className="countdown-number"
                            initial={{ y: '60%', opacity: 0, filter: 'blur(8px)' }}
                            animate={{ y: '0%', opacity: 1, filter: 'blur(0px)' }}
                            exit={{ y: '-60%', opacity: 0, filter: 'blur(8px)' }}
                            transition={{
                                duration: 0.5,
                                ease: [0.22, 1, 0.36, 1] // "Swift & Smooth" ease
                            }}
                            style={{ position: 'absolute', width: '100%', textAlign: 'center', willChange: 'transform, opacity, filter' }}
                        >
                            {formattedValue}
                        </motion.span>
                    </AnimatePresence>
                </div>
            </div>
            <span className="countdown-label">{label}</span>
        </div>
    );
};

function Home() {
    usePageEffects();

    const [timeLeft, setTimeLeft] = useState({
        days: 0,
        hours: 0,
        minutes: 0,
        seconds: 0
    });

    useEffect(() => {
        // Set body background to black (matches content) to hide top grey bar
        document.body.style.backgroundColor = 'var(--bg-body)';

        const targetDate = new Date('2026-06-15T00:00:00').getTime();

        const updateTimer = () => {
            const now = new Date().getTime();
            const distance = targetDate - now;

            if (distance < 0) {
                return;
            }

            setTimeLeft({
                days: Math.floor(distance / (1000 * 60 * 60 * 24)),
                hours: Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)),
                minutes: Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60)),
                seconds: Math.floor((distance % (1000 * 60)) / 1000)
            });
        };

        updateTimer(); // Initial call
        const interval = setInterval(updateTimer, 1000);

        return () => {
            clearInterval(interval);
            // Reset body background to default (grey) for other pages
            document.body.style.backgroundColor = '';
        };
    }, []);

    return (
        <>
            <div className="home-page">
                <div className="fog-overlay"></div>


                <div className="parallax-wrapper">
                    {/* Smoke Background for the Content Layer - Full Width */}
                    <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 0, opacity: 0.6, pointerEvents: 'none' }}>
                        <Smoke className="pointer-events-none" />
                    </div>
                    <div className="page-wrapper" style={{ position: 'relative', zIndex: 1 }}>
                        {/* Dynamic Island Sticky Header - Hidden via CSS but kept for structure if needed later */}
                        <div className="sticky-header">
                            <span className="sticky-title">Gridee Docs</span>
                            <span className="sticky-action">Top ↑</span>
                        </div>

                        {/* Cinematic Coming Soon */}
                        <div className="coming-soon-wrapper" style={{ marginTop: '30vh', position: 'relative', zIndex: 20 }}>
                            <div className="coming-soon-text">
                                Coming Soon
                            </div>

                            <div id="countdown" className="countdown-container">
                                <CountdownUnit value={timeLeft.days} label="Days" />
                                <CountdownUnit value={timeLeft.hours} label="Hours" />
                                <CountdownUnit value={timeLeft.minutes} label="Minutes" />
                                <CountdownUnit value={timeLeft.seconds} label="Seconds" />
                            </div>
                        </div>
                    </div>
                </div>

                <footer className="site-footer" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                    <div className="footer-links" style={{ marginBottom: '16px' }}>
                        <Link to="/" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-primary)', border: 'none' }}>Home</Link>
                        <Link to="/about" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>About</Link>
                        <Link to="/privacy" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>Privacy</Link>
                    </div>
                    <p>&copy; 2025 Gridee. All rights reserved.</p>
                    <p>Governed by the laws of India.</p>
                </footer>
            </div>
        </>
    );
}

export default Home;
