import React, { useState } from 'react';

const LegalSection = () => {
    const [activeTab, setActiveTab] = useState('privacy');

    return (
        <section id="legal" className="section-container" style={{ background: '#f5f5f7' }}>
            <div className="container" style={{ maxWidth: '800px' }}>
                <div className="header">
                    <h1>Legal</h1>
                    <div className="tabs">
                        <button className={`tab-btn ${activeTab === 'privacy' ? 'active' : ''}`} onClick={() => setActiveTab('privacy')}>Privacy Policy</button>
                        <button className={`tab-btn ${activeTab === 'terms' ? 'active' : ''}`} onClick={() => setActiveTab('terms')}>Terms of Service</button>
                    </div>
                </div>

                {activeTab === 'privacy' ? (
                    <div className="legal-content">
                        {/* Privacy Content */}
                        <div className="card policy-card">
                            <h2>1. About Echo</h2>
                            <p>Echo allows users to share anonymous feelings. It is not a social network or ad platform.</p>
                        </div>
                        <div className="card policy-card">
                            <h2>2. Information We Collect</h2>
                            <ul>
                                <li><strong>Provided:</strong> Phone number (auth only), optional profile photo.</li>
                                <li><strong>Messages:</strong> Anonymous. Recipients cannot see identity.</li>
                                <li><strong>Contacts:</strong> Optional access to find friends. Not stored for marketing.</li>
                                <li><strong>Usage:</strong> Crash logs and analytics to improve stability.</li>
                            </ul>
                        </div>
                        <div className="card policy-card">
                            <h2>3. Data Usage & Sharing</h2>
                            <p>We use data to provide the service. We do not sell personal data. We share limited data only with essential service providers (e.g. storage, auth).</p>
                        </div>
                        <div className="card policy-card">
                            <h2>4. Your Rights</h2>
                            <p>You can manage permissions, notifications, and request data deletion at any time.</p>
                        </div>
                        <div className="card policy-card">
                            <h2>5. Children</h2>
                            <p>Echo is not for children under 13.</p>
                        </div>
                    </div>
                ) : (
                    <div className="legal-content">
                        {/* Terms Content */}
                        <div className="card policy-card">
                            <h2>1. Acceptance</h2>
                            <p>By using Echo, you agree to these Terms.</p>
                        </div>
                        <div className="card policy-card">
                            <h2>2. User Conduct</h2>
                            <p>You must not harass, abuse, or harm others. You are responsible for the content you send.</p>
                        </div>
                        <div className="card policy-card">
                            <h2>3. Termination</h2>
                            <p>We may suspend accounts that violate these terms.</p>
                        </div>
                        <div className="card policy-card">
                            <h2>4. Changes</h2>
                            <p>We may modify these terms at any time. Continued use implies acceptance.</p>
                        </div>
                    </div>
                )}
            </div>
            <style>{`
                .tabs { display: flex; justify-content: center; gap: 10px; margin-top: 20px; }
                .tab-btn { background: none; border: none; padding: 10px 20px; font-size: 16px; font-weight: 600; color: #86868b; cursor: pointer; border-bottom: 2px solid transparent; }
                .tab-btn.active { color: #1d1d1f; border-bottom-color: var(--primary-color); }
                .tab-btn:hover { color: #1d1d1f; }
                .legal-content .card { text-align: left; }
                .legal-content h2 { font-size: 1.2rem; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
            `}</style>
        </section>
    );
};

export default LegalSection;
