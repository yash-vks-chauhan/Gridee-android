import { Link } from 'react-router-dom';
import { usePageEffects } from './usePageEffects';

export default function DataSafety() {
    usePageEffects();

    return (
        <>
            <div className="fog-overlay"></div>

            <div className="parallax-wrapper">
                <div className="page-wrapper">

                    {/* Dynamic Island Sticky Header */}
                    <div className="sticky-header">
                        <span className="sticky-title">Gridee Data Safety</span>
                        <span className="sticky-action">Top ↑</span>
                    </div>

                    <div className="hero-header">
                        <h1>Your data, your control.</h1>
                        <div className="last-updated">Transparency in how we handle and protect your information.</div>
                    </div>

                    <div className="content-card" id="data-usage">
                        <h2>Data Usage Policy</h2>
                        <p>Your privacy is paramount. Here is how we treat your data:</p>
                        <ul>
                            <li><strong>No Selling:</strong> We do not sell, rent, or trade your personal information to third parties.</li>
                            <li><strong>Service Focus:</strong> Your data is used exclusively to facilitate parking bookings, verify identity for security, and process payments.</li>
                            <li><strong>Strict Access:</strong> Only authorized systems and personnel have access to user data strictly on a need-to-know basis.</li>
                        </ul>
                    </div>

                    <div className="content-card" id="security-measures">
                        <h2>Security Measures</h2>
                        <p>We employ robust security standards to keep your data safe:</p>
                        <ul>
                            <li><strong>Encryption:</strong> All data in transit is protected using industry-standard TLS protocols.</li>
                            <li><strong>Secure Auth:</strong> User sessions are managed via secure, encrypted JWT tokens stored locally on your device.</li>
                            <li><strong>Payment Safety:</strong> We do not store your credit card or banking details. All transactions are processed securely through Razorpay.</li>
                        </ul>
                    </div>

                    <div className="content-card" id="account-deletion">
                        <h2>Account Deletion</h2>
                        <p>If you wish to remove your presence from our platform, we provide a straightforward process to permanently delete your account and all associated data.</p>
                        <p><strong>To initiate deletion, please email us at <a href="https://mail.google.com/mail/?view=cm&fs=1&to=gridee.business@gmail.com&su=Account%20Deletion%20Request&body=Hello%20Gridee%20Team%2C%0D%0A%0D%0AI%20would%20like%20to%20request%20the%20deletion%20of%20my%20account.%0D%0A%0D%0A---%20Account%20Details%20---%0D%0AUsername%3A%20%0D%0ARegistered%20Email%3A%20%0D%0AMobile%20Number%3A%20%0D%0A%0D%0AI%20understand%20this%20process%20is%20irreversible%20and%20will%20permanently%20delete%20all%20my%20data." target="_blank" rel="noopener noreferrer" className="tooltip-container" data-tooltip="Click to open Gmail" style={{ textDecoration: 'none', borderBottom: '1px solid var(--accent)' }}>gridee.business@gmail.com</a> with the following details:</strong></p>
                        <ul>
                            <li><strong>Username:</strong> Your registered Gridee username.</li>
                            <li><strong> registered Email:</strong> The email address linked to your account.</li>
                            <li><strong>Mobile Number:</strong> The phone number associated with the account.</li>
                        </ul>
                        <p><strong>Important Notes:</strong></p>
                        <ul>
                            <li><strong>Processing Time:</strong> The deletion process typically takes 15-20 days to ensure all records are scrubbed from our active and backup systems.</li>
                            <li><strong>Irreversible:</strong> Once the email is sent and the process begins, it cannot be undone.</li>
                            <li><strong>Permanent Loss:</strong> All account history, wallet balances, and preferences will be permanently deleted and cannot be retrieved.</li>
                        </ul>
                    </div>





                    <div className="contact-section">
                        <h2>Contact Privacy Team</h2>
                        <p>For specific questions regarding data safety:</p>
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
                    <Link to="/privacy" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-secondary)', border: 'none' }}>Privacy</Link>
                    <Link to="/data-safety" style={{ margin: '0 12px', fontSize: '13px', color: 'var(--text-primary)', border: 'none' }}>Data Safety</Link>
                </div>
                <p>&copy; 2025 Gridee. All rights reserved.</p>
                <p>Governed by the laws of India.</p>
            </footer>
        </>
    );
}
