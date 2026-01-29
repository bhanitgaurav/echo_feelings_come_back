import React from 'react';

const FeaturesSection = () => {
    return (
        <section className="section-container" style={{ position: 'relative' }}>
            {/* Background Decoration */}
            <div style={{
                position: 'absolute',
                top: '50%', left: '0',
                width: '100%', height: '100%',
                background: 'radial-gradient(ellipse at bottom, rgba(59, 130, 246, 0.1) 0%, transparent 70%)',
                zIndex: -1,
                pointerEvents: 'none'
            }}></div>

            <div className="container" style={{ textAlign: 'center' }}>
                <div style={{ maxWidth: '700px', margin: '0 auto 80px' }} className="animate-fade-up">
                    <h2 className="text-gradient">Designed for Emotional Safety</h2>
                    <p>Echo isn't about performance or visibility. It's about presence.</p>
                </div>

                <div className="features-grid">
                    {/* Card 1 */}
                    <div className="glass-card animate-fade-up delay-100">
                        <div className="icon-box">
                            <i className="fas fa-user-secret"></i>
                        </div>
                        <h3>Anonymous by Design</h3>
                        <p>Identities are never revealed to the recipient. Share honestly without the fear of judgment or exposure.</p>
                    </div>

                    {/* Card 2 */}
                    <div className="glass-card animate-fade-up delay-100">
                        <div className="icon-box" style={{ color: '#F472B6', background: 'rgba(244, 114, 182, 0.1)' }}>
                            <i className="fas fa-hand-holding-heart"></i>
                        </div>
                        <h3>Non-Confrontational</h3>
                        <p>No direct chats. No public profiles. No pressure. Just a quiet space to express appreciation and signal feelings.</p>
                    </div>

                    {/* Card 3 */}
                    <div className="glass-card animate-fade-up delay-200">
                        <div className="icon-box" style={{ color: '#3B82F6', background: 'rgba(59, 130, 246, 0.1)' }}>
                            <i className="fas fa-spa"></i>
                        </div>
                        <h3>Gentle Habits</h3>
                        <p>Build emotional awareness and healthier relationships through thoughtful response, not endless scrolling or reaction.</p>
                    </div>

                    {/* Card 4 */}
                    <div className="glass-card animate-fade-up delay-200">
                        <div className="icon-box" style={{ color: '#F59E0B', background: 'rgba(245, 158, 11, 0.1)' }}>
                            <i className="fas fa-chart-line"></i>
                        </div>
                        <h3>Emotional Insights</h3>
                        <p>Visualize your emotional journey. Track your presence, kindness, and response streaks to understand your impact.</p>
                    </div>

                    {/* Card 5 */}
                    <div className="glass-card animate-fade-up delay-300">
                        <div className="icon-box" style={{ color: '#10B981', background: 'rgba(16, 185, 129, 0.1)' }}>
                            <i className="fas fa-user-friends"></i>
                        </div>
                        <h3>Trusted Connections</h3>
                        <p>Connect with people you already know. Echo helps you deepen existing bonds rather than chasing strangers.</p>
                    </div>

                    {/* Card 6 */}
                    <div className="glass-card animate-fade-up delay-300">
                        <div className="icon-box" style={{ color: '#8B5CF6', background: 'rgba(139, 92, 246, 0.1)' }}>
                            <i className="fas fa-shield-alt"></i>
                        </div>
                        <h3>Private & Secure</h3>
                        <p>Your data stays yours. No ads, no selling of personal info, and complete control over your notifications.</p>
                    </div>
                </div>

                <div style={{ marginTop: '100px', textAlign: 'center' }} className="animate-fade-up delay-300">
                    <p style={{ fontSize: '1.5rem', fontWeight: '300', fontStyle: 'italic', maxWidth: '800px', margin: '0 auto', color: 'var(--text-primary)' }}>
                        "A place to appreciate, to reflect, and to understand how we affect each other — gently, anonymously, and honestly."
                    </p>
                </div>
            </div>

            <style>{`
                .features-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: 30px;
                }
                .icon-box {
                    width: 70px;
                    height: 70px;
                    background: rgba(139, 92, 246, 0.1);
                    border-radius: 20px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-bottom: 24px;
                    color: var(--primary);
                    font-size: 28px;
                    transition: transform 0.3s ease;
                }
                .glass-card:hover .icon-box {
                    transform: scale(1.1) rotate(5deg);
                }
            `}</style>
        </section>
    );
};

export default FeaturesSection;
