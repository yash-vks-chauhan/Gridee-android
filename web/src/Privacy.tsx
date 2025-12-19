import { Link } from 'react-router-dom';
import { usePageEffects } from './usePageEffects';

export default function Privacy() {
    usePageEffects();

    return (
        <>
            <div className="fog-overlay"></div>

            <div className="parallax-wrapper">
                <div className="page-wrapper">

                    {/* Dynamic Island Sticky Header */}
                    <div className="sticky-header">
                        <span className="sticky-title">Gridee Privacy</span>
                        <span className="sticky-action">Top ↑</span>
                    </div>

                    <div className="hero-header">
                        <h1>Privacy Policy</h1>
                        <div className="last-updated">Last Updated: December 11, 2025</div>
                    </div>

                    <div className="content-card">
                        <h2>Welcome to Gridee</h2>
                        <p>At Gridee, your privacy and trust are our top priorities. This Privacy Policy explains in clear,
                            simple
                            terms how we collect, use, protect, and handle your personal information when you use our smart
                            parking
                            management app.</p>
                        <p><strong>Our Promise:</strong> We believe in complete transparency. We will never sell your personal
                            data,
                            and we only collect what's necessary to provide you with a seamless parking experience.</p>
                    </div>

                    <div className="content-card" id="information">
                        <h2>Information We Collect</h2>
                        <p>We collect specific data points to provide our smart parking services. This includes:</p>
                        <ul>
                            <li><strong>Identity & Account:</strong> Name, email address, phone number, and password
                                (encrypted).
                            </li>
                            <li><strong>Vehicle Data:</strong> Vehicle license plate numbers and vehicle type.</li>
                            <li><strong>Parking Activity:</strong> Parking lot choices, booking history, and QR codes scanned.
                            </li>
                            <li><strong>Financial Data:</strong> Wallet balances, transaction history, and payment status
                                (processed
                                via Razorpay).</li>
                            <li><strong>Technical Data:</strong> JWT authentication tokens stored locally, and device
                                identifiers
                                for app functionality.</li>
                            <li><strong>Camera Data:</strong> Images of license plates for text recognition (processed via ML
                                Kit).
                            </li>
                        </ul>
                    </div>

                    <div className="content-card" id="collection">
                        <h2>Third-Party Services</h2>
                        <p>We partner with trusted third-party providers to deliver our services. We share only necessary data
                            with
                            them:</p>
                        <ul>
                            <li><strong>Razorpay:</strong> Payment processing. We share order IDs and payment amounts. We do not
                                store card details.</li>
                            <li><strong>Google/Apple Sign-In:</strong> Authentication. We receive profile tokens to verify your
                                identity.</li>
                            <li><strong>Google ML Kit:</strong> Text recognition. Used to process license plate images locally
                                on
                                your device.</li>
                            <li><strong>Google AdMob:</strong> Advertising. May collect device identifiers and usage data to
                                show
                                relevant ads.</li>
                        </ul>
                    </div>

                    <div className="content-card" id="permissions">
                        <h2>Location & Permissions</h2>
                        <p><strong>Location:</strong> We do not currently collect real-time GPS location data. If this changes
                            in a
                            future update to enable navigation features, we will request explicit permission.</p>
                        <p><strong>Camera:</strong> Required for scanning QR codes and recognizing vehicle license plates.</p>
                        <p><strong>Storage:</strong> We use local storage (SharedPreferences) to securely store your session
                            tokens
                            (JWT) and user preferences.</p>
                    </div>

                    <div className="content-card" id="protection">
                        <h2>Security & Logging</h2>
                        <p>We take data security seriously. While we are transitioning to full HTTPS encryption for all network
                            traffic, we currently employ the following measures:</p>
                        <ul>
                            <li><strong>Local Encryption:</strong> Sensitive tokens are stored securely on your device.</li>
                            <li><strong>Network Logging:</strong> For debugging and improvement, we may log API request/response
                                bodies. These logs are rotated regularly.</li>
                            <li><strong>Password Hashing:</strong> User passwords are hashed before storage.</li>
                        </ul>
                    </div>

                    <div className="content-card" id="rights">
                        <h2>Your Rights</h2>
                        <p>You have control over your data. While automated deletion tools are coming soon, you can currently:
                        </p>
                        <ul>
                            <li><strong>Request Deletion:</strong> Contact us to manually delete your account and all associated
                                data.</li>
                            <li><strong>Export Data:</strong> Request a copy of your transaction and booking history.</li>
                            <li><strong>Opt-Out:</strong> You can opt-out of personalized advertising via your device settings.
                            </li>
                        </ul>
                    </div>

                    <div className="contact-section">
                        <h2>Questions?</h2>
                        <p>If you have any concerns about your privacy, we are here to help.</p>
                        <div className="email-container">
                            <button className="email-link" id="emailBtn">gridee.business@gmail.com</button>
                        </div>
                    </div>

                </div>
            </div>

            <footer className="site-footer">
                <div className="footer-links" style={{ marginBottom: '16px' }}>
                    <Link to="/" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>Home</Link>
                    <Link to="/about" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>About</Link>
                    <Link to="/privacy" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-primary)', border: 'none' }}>Privacy</Link>
                    <Link to="/data-safety" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>Data Safety</Link>
                </div>
                <p>&copy; 2025 Gridee. All rights reserved.</p>
                <p>Governed by the laws of India.</p>
            </footer>
        </>
    );
}
