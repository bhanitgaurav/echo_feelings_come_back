import React from 'react';
import { Link } from 'react-router-dom';
import SEO from '../components/SEO';
import '../index.css';

const AccountDeletion = () => {
    return (
        <div className="section-container">
            <SEO title="Account Deletion" description="Instructions on how to delete your Echo account and data." />
            <div className="container" style={{ maxWidth: '800px' }}>
                <h1 style={{ marginBottom: '40px', textAlign: 'center' }}>Account Deletion</h1>

                <div className="card" style={{ textAlign: 'left' }}>
                    <h2>How to Delete Your Echo Account</h2>
                    <p>You can request account deletion in two ways:</p>

                    <h3 style={{ marginTop: '32px' }}>Option 1: In-App (Recommended)</h3>
                    <ol style={{ paddingLeft: '20px', marginBottom: '32px', marginTop: '16px' }}>
                        <li>Open the Echo app</li>
                        <li>Go to <strong>Settings → Support</strong></li>
                        <li>Select <strong>“Delete my account”</strong></li>
                        <li>Submit the request</li>
                    </ol>

                    <h3>Option 2: Support Request</h3>
                    <p style={{ marginTop: '16px' }}>If you cannot access the app, please <Link to="/#support">contact support</Link> with your registered phone number.</p>
                </div>

                <div className="card" style={{ marginTop: '24px', textAlign: 'left' }}>
                    <h2>Data Removal</h2>
                    <p>The following data is <strong>permanently removed</strong>:</p>
                    <ul style={{ paddingLeft: '20px', marginTop: '16px' }}>
                        <li>Authentication identifiers (phone number)</li>
                        <li>Profile photo & personal info</li>
                        <li>All sent and received Echoes</li>
                        <li>Streak data and message history</li>
                    </ul>
                </div>
            </div>
            <style>{`
                 h3 { font-size: 1.2rem; font-weight: 600; margin-bottom: 8px; color: var(--text-primary); }
                 li { margin-bottom: 8px; color: var(--text-secondary); }
             `}</style>
        </div>
    );
};

export default AccountDeletion;
