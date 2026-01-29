import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import SEO from '../components/SEO';
import '../App.css';

const Support = () => {
    const [formData, setFormData] = useState({
        email: '',
        category: 'GENERAL',
        subject: '',
        description: ''
    });
    const [status, setStatus] = useState('IDLE'); // IDLE, SUBMITTING, SUCCESS, ERROR
    const [errorMessage, setErrorMessage] = useState('');

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.id]: e.target.value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus('SUBMITTING');
        setErrorMessage('');

        try {
            // Using env var or falling back to dev api for now.
            // In real prod, this should be configurable.
            const API_URL = import.meta.env.VITE_API_URL || 'https://dev-api-echo.bhanitgaurav.com';

            const response = await fetch(`${API_URL}/public/support`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                setStatus('SUCCESS');
                setFormData({ email: '', category: 'GENERAL', subject: '', description: '' });
            } else {
                const text = await response.text();
                throw new Error(text || "Failed to submit ticket.");
            }
        } catch (error) {
            setStatus('ERROR');
            setErrorMessage(error.message);
        }
    };

    // Form Styles injection for specific support form elements
    const formStyles = `
        .form-group { text-align: left; margin-bottom: 20px; }
        label { font-size: 13px; font-weight: 500; color: #86868b; display: block; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
        input, select, textarea { width: 100%; padding: 14px 16px; border: 1px solid #d2d2d7; border-radius: 12px; font-size: 17px; background: #fff; color: #1d1d1f; box-sizing: border-box; transition: border-color 0.2s, box-shadow 0.2s; font-family: inherit; }
        input:focus, select:focus, textarea:focus { outline: none; border-color: var(--primary-color); box-shadow: 0 0 0 4px rgba(229, 57, 53, 0.1); }
        textarea { resize: vertical; min-height: 100px; }
        button.submit-btn { background-color: var(--primary-color); color: white; border: none; padding: 16px 30px; border-radius: 12px; font-size: 17px; font-weight: 600; cursor: pointer; width: 100%; transition: all 0.2s ease; margin-top: 10px; }
        button.submit-btn:hover { opacity: 0.9; }
        button.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }
        .error-message { color: #ff3b30; margin-top: 16px; font-size: 15px; background: rgba(255, 59, 48, 0.1); padding: 12px; border-radius: 8px; }
        .success-icon { color: #34c759; font-size: 56px; margin-bottom: 16px; display: inline-block; }
        button.secondary { background-color: #f2f2f7; color: #1d1d1f; margin-top: 16px; padding: 12px 24px; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
        button.secondary:hover { background-color: #e5e5ea; }
    `;

    if (status === 'SUCCESS') {
        return (
            <div className="container" style={{ textAlign: 'center' }}>
                <SEO title="Support" />
                <style>{formStyles}</style>
                <div className="success-icon">✓</div>
                <h1>Message Sent</h1>
                <p style={{ color: '#86868b' }}>Thanks for reaching out! We've received your ticket.</p>
                <button className="secondary" onClick={() => setStatus('IDLE')}>Send Another Message</button>

                <div className="footer">
                    <p style={{ marginBottom: '10px' }}>
                        <Link to="/about">About</Link> &bullet;
                        <Link to="/privacy-policy">Privacy</Link> &bullet;
                        <Link to="/terms">Terms</Link>
                    </p>
                    <p>&copy; 2026 Echo App. All rights reserved.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container">
            <SEO title="Support" />
            <style>{formStyles}</style>
            <div className="header">
                <Link to="/about" style={{ textDecoration: 'none' }}>
                    <img src="/logo.png" alt="Echo Logo" className="logo" />
                </Link>
                <h1>Echo Support</h1>
                <p style={{ color: '#86868b' }}>How can we help you today?</p>
            </div>

            <div className="card" style={{ maxWidth: '440px', margin: '0 auto', textAlign: 'center' }}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="email">Your Email</label>
                        <input
                            type="email"
                            id="email"
                            required
                            placeholder="name@example.com"
                            autoComplete="email"
                            value={formData.email}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="category">Topic</label>
                        <select id="category" required value={formData.category} onChange={handleChange}>
                            <option value="GENERAL">General Inquiry</option>
                            <option value="BUG">Report a Bug</option>
                            <option value="ACCOUNT">Account Issue</option>
                            <option value="FEEDBACK">Feedback</option>
                            <option value="CONTENT">Content Report</option>
                            <option value="OTHER">Other</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="subject">Subject</label>
                        <input
                            type="text"
                            id="subject"
                            required
                            placeholder="Brief summary"
                            autoComplete="off"
                            value={formData.subject}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="description">Message</label>
                        <textarea
                            id="description"
                            required
                            rows="5"
                            placeholder="Please describe details..."
                            value={formData.description}
                            onChange={handleChange}
                        ></textarea>
                    </div>

                    <button type="submit" className="submit-btn" disabled={status === 'SUBMITTING'}>
                        {status === 'SUBMITTING' ? 'Submitting...' : 'Submit Ticket'}
                    </button>
                </form>

                {status === 'ERROR' && (
                    <div className="error-message">
                        {errorMessage}
                    </div>
                )}

                <div className="footer" style={{ marginTop: '20px', paddingTop: '20px', borderTop: '1px solid #eee' }}>
                    <p>Or email us directly at <a href="mailto:bhanitgauravapps@gmail.com">bhanitgauravapps@gmail.com</a></p>
                </div>
            </div>

            <div className="footer">
                <p style={{ marginBottom: '10px' }}>
                    <Link to="/about">About</Link>
                    <span> • </span>
                    <Link to="/privacy-policy">Privacy Policy</Link>
                    <span> • </span>
                    <Link to="/terms">Terms of Service</Link>
                </p>
                <p>&copy; 2026 Echo App. All rights reserved.</p>
            </div>
        </div>
    );
};

export default Support;
