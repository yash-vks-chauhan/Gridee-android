import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { usePageEffects } from './usePageEffects';
import { Smoke } from '@/components/ui/shadcn-io/smoke';
import './index.css';

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
                                <div className="countdown-item">
                                    <span className="countdown-number" id="days">{String(timeLeft.days).padStart(2, '0')}</span>
                                    <span className="countdown-label">Days</span>
                                </div>
                                <div className="countdown-item">
                                    <span className="countdown-number" id="hours">{String(timeLeft.hours).padStart(2, '0')}</span>
                                    <span className="countdown-label">Hours</span>
                                </div>
                                <div className="countdown-item">
                                    <span className="countdown-number" id="minutes">{String(timeLeft.minutes).padStart(2, '0')}</span>
                                    <span className="countdown-label">Minutes</span>
                                </div>
                                <div className="countdown-item">
                                    <span className="countdown-number" id="seconds">{String(timeLeft.seconds).padStart(2, '0')}</span>
                                    <span className="countdown-label">Seconds</span>
                                </div>
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
