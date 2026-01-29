import React from 'react';

const AccountDeletionSection = () => {
    return (
        <section id="account-deletion" className="section-container">
            <div className="container" style={{ maxWidth: '800px' }}>
                <div className="header">
                    <h1>Account Deletion</h1>
                    <p className="subtitle">Control your data.</p>
                </div>

                <div className="card">
                    <h2><i className="fas fa-trash-alt" style={{ marginRight: '10px', color: 'var(--primary-color)' }}></i> How to Delete Your Account</h2>
                    <p>You can request account deletion in two ways:</p>
                    <h3 style={{ fontSize: '1.1rem', marginTop: '20px' }}>Option 1: In-App (Recommended)</h3>
                    <ol style={{ textAlign: 'left', paddingLeft: '20px' }}>
                        <li>Open the Echo app</li>
                        <li>Go to <strong>Settings → Support</strong></li>
                        <li>Select <strong>“Delete my account”</strong></li>
                        <li>Submit the request</li>
                    </ol>

                    <h3 style={{ fontSize: '1.1rem', marginTop: '20px' }}>Option 2: Support Request</h3>
                    <p>Contact support through the form above or email us. Please include your registered phone number.</p>
                </div>

                <div className="card">
                    <h2><i className="fas fa-eraser" style={{ marginRight: '10px', color: 'var(--primary-color)' }}></i> Data Removal</h2>
                    <p>All personal data (profile, messages, history) is <strong>permanently removed</strong>. Anonymous usage stats may be retained.</p>
                </div>
            </div>
        </section>
    );
};

export default AccountDeletionSection;
