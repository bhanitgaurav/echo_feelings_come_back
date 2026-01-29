import React from 'react';
import '../index.css';
import SEO from '../components/SEO';

const About = () => {
    return (
        <div className="section-container">
            <SEO title="About Echo" description="Echo is a quiet space for honest feelings. Not a chat app, but a place to appreciate and reflect." />

            <div className="container" style={{ maxWidth: '900px' }}>
                {/* Header */}
                <div style={{ textAlign: 'center', marginBottom: '80px' }} className="animate-fade-up">
                    <h1 className="text-gradient" style={{ marginBottom: '24px' }}>Echo — Feelings Come Back</h1>
                    <p style={{ fontSize: '1.25rem', color: 'var(--text-primary)', maxWidth: '600px', margin: '0 auto' }}>
                        A quiet space where feelings return without names.
                    </p>
                </div>

                {/* Section 1: The Core Philosophy */}
                <div className="glass-card animate-fade-up delay-100" style={{ padding: '60px', marginBottom: '40px' }}>
                    <h2 style={{ fontSize: '2rem', marginBottom: '32px' }}>A Quiet Space for Honest Feelings</h2>
                    <p style={{ fontSize: '1.1rem', marginBottom: '24px' }}>
                        Values are often spoken, but feelings are often felt in silence. Echo is a place to appreciate, to reflect, and to understand how we affect each other — gently, anonymously, and honestly.
                    </p>
                    <p style={{ fontSize: '1.25rem', fontStyle: 'italic', color: 'var(--text-primary)', borderLeft: '4px solid var(--primary)', paddingLeft: '24px', margin: '40px 0' }}>
                        "Not every feeling needs a conversation. Some just need to be acknowledged."
                    </p>
                    <p style={{ fontSize: '1.1rem' }}>
                        Echo helps you express appreciation, signal feelings, and build emotional awareness — without confrontation, pressure, or exposure.
                    </p>
                </div>

                {/* Section 2: Not A Chat App (Grid Layout) */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '40px', marginBottom: '60px' }}>
                    <div className="glass-card animate-fade-up delay-200" style={{ padding: '40px' }}>
                        <div className="icon-box" style={{ background: 'rgba(239, 68, 68, 0.1)', color: '#F87171' }}>
                            <i className="fas fa-ban"></i>
                        </div>
                        <h3>What Echo is NOT</h3>
                        <ul className="custom-list">
                            <li>It's NOT a chat app.</li>
                            <li>It's NOT about performance or likes.</li>
                            <li>It's NOT about social pressure.</li>
                        </ul>
                    </div>

                    <div className="glass-card animate-fade-up delay-300" style={{ padding: '40px' }}>
                        <div className="icon-box" style={{ background: 'rgba(16, 185, 129, 0.1)', color: '#34D399' }}>
                            <i className="fas fa-spa"></i>
                        </div>
                        <h3>What Echo IS</h3>
                        <ul className="custom-list">
                            <li>It IS about <span className="text-highlight">presence</span>.</li>
                            <li>It IS about emotional safety.</li>
                            <li>It IS about healthier relationships.</li>
                        </ul>
                    </div>
                </div>

                {/* Section 3: When Echo Fits Best */}
                <div className="glass-card animate-fade-up delay-200" style={{ padding: '60px', marginBottom: '40px', background: 'linear-gradient(145deg, rgba(255, 255, 255, 0.05) 0%, rgba(139, 92, 246, 0.05) 100%)' }}>
                    <h2 style={{ marginBottom: '32px' }}>Small Moments Matter</h2>
                    <p style={{ marginBottom: '32px' }}>Echo exists for those moments when words feel too heavy, or silence feels too distant.</p>

                    <div className="use-case-grid">
                        <div className="use-case-item">
                            <div className="check-icon">✓</div>
                            <p>When you want to appreciate someone without making it awkward.</p>
                        </div>
                        <div className="use-case-item">
                            <div className="check-icon">✓</div>
                            <p>When you want to express honesty without starting conflict.</p>
                        </div>
                        <div className="use-case-item">
                            <div className="check-icon">✓</div>
                            <p>When you want to reflect on how emotions move between people.</p>
                        </div>
                    </div>
                </div>

                {/* Section 4: Privacy Footer */}
                <div style={{ textAlign: 'center', marginTop: '80px', opacity: 0.8 }} className="animate-fade-up delay-300">
                    <h3 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Privacy & Trust</h3>
                    <p style={{ maxWidth: '600px', margin: '0 auto' }}>
                        Echo is built with privacy as a foundation. Messages are anonymous by design, and personal data is handled with care.
                        It’s about saying what matters — safely.
                    </p>
                </div>
            </div>

            <style>{`
                .icon-box {
                    width: 50px; height: 50px;
                    border-radius: 12px;
                    display: flex; align-items: center; justify-content: center;
                    font-size: 1.25rem;
                    margin-bottom: 24px;
                }
                .custom-list {
                    list-style: none;
                    margin-top: 16px;
                }
                .custom-list li {
                    margin-bottom: 12px;
                    padding-left: 24px;
                    position: relative;
                    color: var(--text-secondary);
                }
                .custom-list li::before {
                    content: "•";
                    color: var(--primary);
                    font-weight: bold;
                    position: absolute;
                    left: 0;
                }
                .text-highlight {
                    color: var(--primary-light);
                    font-weight: 600;
                }
                .use-case-grid {
                    display: flex;
                    flex-direction: column;
                    gap: 16px;
                }
                .use-case-item {
                    display: flex;
                    align-items: flex-start;
                    gap: 16px;
                    background: rgba(255, 255, 255, 0.03);
                    padding: 16px;
                    border-radius: 12px;
                }
                .check-icon {
                    color: #10B981;
                    font-weight: bold;
                    background: rgba(16, 185, 129, 0.1);
                    width: 24px; height: 24px;
                    border-radius: 50%;
                    display: flex; align-items: center; justify-content: center;
                    font-size: 0.8rem;
                    flex-shrink: 0;
                }
            `}</style>
        </div>
    );
};

export default About;
