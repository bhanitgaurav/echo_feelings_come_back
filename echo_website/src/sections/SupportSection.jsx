import React, { useState } from 'react';

const SupportSection = () => {
    const [formData, setFormData] = useState({
        email: '',
        category: 'GENERAL',
        subject: '',
        description: ''
    });
    const [status, setStatus] = useState('IDLE');
    const [errorMessage, setErrorMessage] = useState('');

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.id]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus('SUBMITTING');
        setErrorMessage('');

        try {
            const API_URL = import.meta.env.VITE_API_URL || 'https://dev-api-echo.bhanitgaurav.com';
            const response = await fetch(`${API_URL}/public/support`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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

    return (
        <section id="support" className="section-container">
            <div className="container">
                <div style={{ textAlign: 'center', marginBottom: '60px' }}>
                    <h2 className="text-gradient">Support</h2>
                    <p>We're here to help.</p>
                </div>

                <div className="glass-card" style={{ maxWidth: '600px', margin: '0 auto', padding: '40px' }}>
                    {status === 'SUCCESS' ? (
                        <div style={{ textAlign: 'center', padding: '40px 0' }}>
                            <div style={{ fontSize: '64px', color: '#4ADE80', marginBottom: '20px' }}>✓</div>
                            <h3>Message Sent</h3>
                            <p style={{ marginBottom: '30px' }}>We've received your ticket and will get back to you soon.</p>
                            <button className="btn btn-primary" onClick={() => setStatus('IDLE')}>Send Another Message</button>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label htmlFor="email">Your Email</label>
                                <input type="email" id="email" required placeholder="name@example.com" autoComplete="email"
                                    value={formData.email} onChange={handleChange} />
                            </div>

                            <div className="form-group">
                                <label htmlFor="category">Topic</label>
                                <select id="category" required value={formData.category} onChange={handleChange} className="custom-select">
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
                                <input type="text" id="subject" required placeholder="Brief summary" autoComplete="off"
                                    value={formData.subject} onChange={handleChange} />
                            </div>

                            <div className="form-group">
                                <label htmlFor="description">Message</label>
                                <textarea id="description" required rows="5" placeholder="Details..."
                                    value={formData.description} onChange={handleChange}></textarea>
                            </div>

                            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '20px' }} disabled={status === 'SUBMITTING'}>
                                {status === 'SUBMITTING' ? 'Sending...' : 'Submit Ticket'}
                            </button>

                            {status === 'ERROR' && (
                                <div className="error-message" style={{ marginTop: '20px', color: '#F87171', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px', textAlign: 'center' }}>
                                    {errorMessage}
                                </div>
                            )}
                        </form>
                    )}
                </div>
                <div style={{ textAlign: 'center', marginTop: '40px', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                    Or email us directly at <a href="mailto:bhanitgauravapps@gmail.com" style={{ color: 'var(--primary-glow)' }}>bhanitgauravapps@gmail.com</a>
                </div>
            </div>

            <style>{`
                .form-group { margin-bottom: 24px; text-align: left; }
                label { display: block; margin-bottom: 8px; font-weight: 500; font-size: 0.9rem; color: var(--text-secondary); }
                input, select, textarea {
                    width: 100%;
                    padding: 16px;
                    border-radius: 12px;
                    border: 1px solid var(--border-glass);
                    background: rgba(255, 255, 255, 0.05);
                    font-size: 1rem;
                    font-family: inherit;
                    color: var(--text-primary);
                    transition: all 0.2s ease;
                }
                input:focus, select:focus, textarea:focus {
                    outline: none;
                    background: rgba(255, 255, 255, 0.1);
                    border-color: var(--primary);
                    box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.2);
                }
                /* Custom select arrow if needed */
                select { 
                    -webkit-appearance: none; appearance: none; 
                    background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e"); 
                    background-repeat: no-repeat; 
                    background-position: right 1rem center; 
                    background-size: 1em; 
                }
                option {
                    background-color: var(--bg-card);
                    color: var(--text-primary);
                }
            `}</style>
        </section>
    );
};

export default SupportSection;
