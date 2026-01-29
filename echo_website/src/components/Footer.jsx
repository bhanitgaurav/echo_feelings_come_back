import React from 'react';
import { Link } from 'react-router-dom';

const Footer = () => {
    return (
        <footer className="main-footer">
            <div className="container">
                <div className="footer-grid">
                    <div className="footer-brand">
                        <img src="/logo.png" alt="Echo" style={{ height: '50px', borderRadius: '12px', marginBottom: '16px' }} />
                        <h3>Echo</h3>
                        <p style={{ fontSize: '0.9rem', maxWidth: '300px' }}>
                            Connect with feeling. Anonymous, safe, and real interactions with the people you know.
                        </p>
                    </div>

                    <div className="footer-col">
                        <h4>Company</h4>
                        <Link to="/about">About Us</Link>
                        <Link to="/#support">Contact Support</Link>
                    </div>

                    <div className="footer-col">
                        <h4>Legal</h4>
                        <Link to="/privacy-policy">Privacy Policy</Link>
                        <Link to="/terms">Terms of Service</Link>
                        <Link to="/account-deletion">Data Safety</Link>
                    </div>

                    <div className="footer-col">
                        <h4>Get the App</h4>
                        <a href="https://play.google.com/store/apps/details?id=com.bhanit.apps.echo&referrer=utm_source%3Dwebsite%26utm_medium%3Decho_website%26utm_campaign%3Dwebsite_visit" target="_blank" rel="noopener noreferrer">
                            <i className="fab fa-google-play" style={{ marginRight: '8px' }}></i> Google Play
                        </a>
                        <a href="https://apps.apple.com/in/app/echo-feelings-come-back/id6757484280?ct=echo_website_about" target="_blank" rel="noopener noreferrer">
                            <i className="fab fa-apple" style={{ marginRight: '8px' }}></i> App Store
                        </a>
                    </div>
                </div>

                <div className="footer-bottom">
                    <p className="copyright">&copy; {new Date().getFullYear()} Echo App. All rights reserved.</p>
                </div>
            </div>

            <style>{`
                .main-footer {
                    background: var(--bg-card);
                    border-top: 1px solid var(--border-color);
                    padding: 80px 0 40px;
                    margin-top: auto; /* Push to bottom if content is short */
                }
                .footer-grid {
                    display: grid;
                    grid-template-columns: 2fr 1fr 1fr 1fr;
                    gap: 40px;
                    margin-bottom: 60px;
                }
                .footer-col h4 {
                    font-size: 1rem;
                    margin-bottom: 20px;
                    color: var(--text-primary);
                }
                .footer-col a {
                    display: block;
                    color: var(--text-secondary);
                    margin-bottom: 12px;
                    font-size: 0.95rem;
                }
                .footer-col a:hover {
                    color: var(--primary);
                    padding-left: 5px; /* subtle nudge */
                }
                .footer-bottom {
                    border-top: 1px solid var(--border-color);
                    padding-top: 30px;
                    text-align: center;
                    font-size: 0.85rem;
                    color: var(--text-tertiary);
                }
                
                @media (max-width: 768px) {
                    .footer-grid { grid-template-columns: 1fr; text-align: center; gap: 40px; }
                     .footer-brand { display: flex; flex-direction: column; align-items: center; }
                }
            `}</style>
        </footer>
    );
};

export default Footer;
