import { Link } from 'react-router-dom';
import { usePageEffects } from './usePageEffects';

export default function About() {
    usePageEffects();

    return (
        <>
            <div className="fog-overlay"></div>

            <div className="parallax-wrapper">
                <div className="page-wrapper">

                    {/* Dynamic Island Sticky Header */}
                    <div className="sticky-header">
                        <span className="sticky-title">Gridee About</span>
                        <span className="sticky-action">Top ↑</span>
                    </div>

                    <div className="hero-header">
                        <h1>Gridee — smart parking and cashless check-ins.</h1>
                        <div className="last-updated">Book spots, scan to enter/exit, manage your wallet in-app.</div>
                    </div>

                    <div className="content-card" id="what-we-do">
                        <h2>What We Do</h2>
                        <p>Smart parking management for drivers and operators; streamlined entry/exit with QR and license-plate
                            scans; cashless payments and wallet top-ups.</p>
                    </div>

                    <div className="content-card" id="core-features">
                        <h2>Core Features</h2>
                        <ul>
                            <li><strong>Book & Manage:</strong> Book and manage parking spots with live availability UI.</li>
                            <li><strong>Secure Sign-in:</strong> Secure sign-in (email/password + Google; Apple flow
                                scaffolded).</li>
                            <li><strong>Wallet & Payments:</strong> Wallet & payments via Razorpay checkout (order/payment IDs;
                                no card storage).</li>
                            <li><strong>Smart Scanning:</strong> QR and license-plate scanning (CameraX + ML Kit OCR) for
                                check-in/out.</li>
                            <li><strong>Operator Dashboard:</strong> Operator dashboard for vehicle-based check-ins/outs.</li>
                            <li><strong>Rewards:</strong> Rewarded video option (AdMob test ID) to credit wallet.</li>
                            <li><strong>Privacy Controls:</strong> Privacy controls screen (toggles for data collection,
                                location toggle UI, analytics, marketing emails).</li>
                        </ul>
                    </div>

                    <div className="content-card" id="how-it-works">
                        <h2>How It Works</h2>
                        <p>Three simple steps to smarter parking:</p>
                        <ul>
                            <li><strong>Step 1:</strong> Sign up/login to the app.</li>
                            <li><strong>Step 2:</strong> Choose your preferred parking lot and spot.</li>
                            <li><strong>Step 3:</strong> Pay and scan at the gate (QR or license plate) to enter.</li>
                        </ul>
                    </div>

                    <div className="content-card" id="technology">
                        <h2>Technology</h2>
                        <p>Built with robust modern tech stack:</p>
                        <ul>
                            <li><strong>Android App:</strong> Native Kotlin development.</li>
                            <li><strong>Vision:</strong> CameraX + ML Kit for OCR, ZXing for QR scanning.</li>
                            <li><strong>Networking:</strong> Retrofit/OkHttp for reliable API communication.</li>
                            <li><strong>Integrations:</strong> Razorpay SDK for payments, Google Mobile Ads SDK for rewards.
                            </li>
                        </ul>
                    </div>

                    <div className="content-card" id="data-privacy">
                        <h2>Data & Privacy Snapshot</h2>
                        <p>We value your data privacy:</p>
                        <ul>
                            <li><strong>Auth:</strong> JWT-based auth tokens stored locally.</li>
                            <li><strong>Storage:</strong> Shared preferences used for user/session data.</li>
                            <li><strong>Camera:</strong> Used strictly for QR/plate scans.</li>
                            <li><strong>Payments:</strong> Handled securely by Razorpay.</li>
                            <li><strong>Ads:</strong> AdMob rewarded ads present.</li>
                            <li><strong>Location:</strong> UI present but location permission not requested/used yet.</li>
                        </ul>
                    </div>

                    <div className="content-card" id="availability">
                        <h2>Availability</h2>
                        <p><strong>Current:</strong> Android app is live.</p>
                        <p><strong>Coming Soon:</strong> Maps and precise location features are in progress.</p>
                    </div>

                    <div className="content-card" id="governance">
                        <h2>Governance</h2>
                        <p>Built in India; aligned with our privacy policy and terms.</p>
                    </div>

                    <div className="contact-section">
                        <h2>Contact Us</h2>
                        <p>For business inquiries or support:</p>
                        <div className="email-container">
                            <button className="email-link" id="emailBtn">gridee.business@gmail.com</button>
                            <div className="copy-tooltip">Copied!</div>
                        </div>
                    </div>

                </div>
            </div>

            <footer className="site-footer">
                <div className="footer-links" style={{ marginBottom: '16px' }}>
                    <Link to="/" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>Home</Link>
                    <Link to="/about" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-primary)', border: 'none' }}>About</Link>
                    <Link to="/privacy" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>Privacy</Link>
                </div>
                <p>&copy; 2025 Gridee. All rights reserved.</p>
                <p>Governed by the laws of India.</p>
            </footer>
        </>
    );
}
