import React, { useEffect, useState } from 'react';
import SEO from '../components/SEO';
import '../index.css';

const TermsOfService = () => {
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchContent = async () => {
            try {
                const API_URL = import.meta.env.VITE_API_URL || 'https://dev-api-echo.bhanitgaurav.com';
                const response = await fetch(`${API_URL}/content/TERMS`);

                if (response.ok) {
                    const data = await response.json();
                    if (data.content && data.content.content) {
                        setContent(data.content.content);
                    } else if (data.content) {
                        setContent(data.content);
                    }
                } else {
                    throw new Error('Failed to load content');
                }
            } catch (err) {
                console.error("Error fetching terms:", err);
                setError('Unable to load terms of service at this time.');
            } finally {
                setLoading(false);
            }
        };

        fetchContent();
    }, []);

    return (
        <div className="section-container">
            <SEO title="Terms of Service" />
            <div className="container" style={{ maxWidth: '800px' }}>
                <div className="header" style={{ marginBottom: '40px', textAlign: 'center' }}>
                    <h1 className="text-gradient">Terms of Service</h1>
                </div>

                <div className="card glass-card" style={{ padding: '60px 40px' }}>
                    {loading ? (
                        <div style={{ textAlign: 'center', padding: '40px' }}>
                            <div className="spinner" style={{
                                width: '40px', height: '40px',
                                border: '4px solid rgba(139, 92, 246, 0.1)',
                                borderTop: '4px solid #8B5CF6',
                                borderRadius: '50%',
                                margin: '0 auto',
                                animation: 'spin 1s linear infinite'
                            }}></div>
                        </div>
                    ) : error ? (
                        <div style={{ color: '#F87171', textAlign: 'center' }}>
                            <p>{error}</p>
                            <p style={{ fontSize: '0.9rem', marginTop: '10px' }}>Please try refreshing the page.</p>
                        </div>
                    ) : (
                        <div className="legal-content" dangerouslySetInnerHTML={{ __html: content }} />
                    )}
                </div>
            </div>
            <style>{`
                 .legal-content {
                     text-align: left;
                     line-height: 1.8;
                     color: var(--text-secondary);
                     font-size: 1.05rem;
                 }
                 /* Make sure headers break out of the flow properly */
                 .legal-content h1, 
                 .legal-content h2, 
                 .legal-content h3 { 
                     color: var(--text-primary);
                     margin-top: 2.5rem; 
                     margin-bottom: 1.25rem; 
                     line-height: 1.3;
                 }
                 .legal-content h1 { font-size: 2rem; border-bottom: 1px solid var(--border-glass); padding-bottom: 16px; margin-top: 0; }
                 .legal-content h2 { font-size: 1.5rem; }
                 .legal-content h3 { font-size: 1.25rem; }
                 
                 /* Handle Paragraphs */
                 .legal-content p { 
                     margin-bottom: 1.5rem; 
                     color: var(--text-secondary);
                 }
                 
                 /* Handle Lists */
                 .legal-content ul, .legal-content ol { 
                     margin-bottom: 1.5rem; 
                     padding-left: 24px; 
                 }
                 .legal-content li { 
                     margin-bottom: 0.5rem; 
                     padding-left: 8px;
                 }
                 
                 /* Handle standard links */
                 .legal-content a { color: var(--primary); text-decoration: underline; }
                 
                 /* IMPORTANT: Handle basic line breaks from backend if it's not HTML */
                 .legal-content {
                    white-space: pre-line; /* Fallback for raw text with newlines */
                 }
                 /* Reset white-space for block elements to avoid double spacing if HTML is used */
                 .legal-content p, 
                 .legal-content h1, 
                 .legal-content h2, 
                 .legal-content div,
                 .legal-content ul,
                 .legal-content li {
                     white-space: normal;
                 }
             `}</style>
        </div>
    );
};

export default TermsOfService;
