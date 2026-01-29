import React from 'react';
import '../index.css';

const HeroSection = () => {
    return (
        <section id="hero" className="section-container" style={{ paddingTop: '160px', overflow: 'hidden' }}>
            <div className="container">
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', alignItems: 'center', gap: '60px' }}>

                    {/* Left Content */}
                    <div style={{ textAlign: 'left', zIndex: 2 }}>
                        <div className="animate-fade-up">
                            <span style={{
                                display: 'inline-block',
                                padding: '8px 16px',
                                background: 'rgba(139, 92, 246, 0.1)',
                                color: '#A78BFA',
                                borderRadius: '30px',
                                fontSize: '0.875rem',
                                fontWeight: '600',
                                marginBottom: '24px',
                                border: '1px solid rgba(139, 92, 246, 0.2)'
                            }}>
                                ✨ A quiet space for honest feelings
                            </span>

                            <h1 style={{ marginBottom: '24px', lineHeight: '1.2' }}>
                                Echo — <br />
                                <span className="text-gradient">Feelings Come Back</span>
                            </h1>

                            <p style={{ fontSize: '1.25rem', marginBottom: '40px', maxWidth: '540px', color: 'var(--text-secondary)' }}>
                                A safe, anonymous way to share emotions with people you already know.
                                <br /><br />
                                <span style={{ fontStyle: 'italic', opacity: 0.9 }}>"Not every feeling needs a conversation. Some just need to be acknowledged."</span>
                            </p>

                            <div className="store-buttons">
                                <a href="https://play.google.com/store/apps/details?id=com.bhanit.apps.echo&referrer=utm_source%3Dwebsite%26utm_medium%3Decho_website%26utm_campaign%3Dwebsite_visit"
                                    className="btn-store btn-glow">
                                    <i className="fab fa-google-play" style={{ color: '#fff' }}></i>
                                    <div>
                                        <span>GET IT ON</span>
                                        <strong>Google Play</strong>
                                    </div>
                                </a>

                                <a href="https://apps.apple.com/in/app/echo-feelings-come-back/id6757484280?ct=echo_website_about"
                                    className="btn-store btn-glow">
                                    <i className="fab fa-apple" style={{ color: '#fff' }}></i>
                                    <div>
                                        <span>Download on the</span>
                                        <strong>App Store</strong>
                                    </div>
                                </a>
                            </div>
                        </div>
                    </div>

                    {/* Right Visual (Phone Mockup) */}
                    <div className="animate-fade-up delay-200" style={{ position: 'relative', display: 'flex', justifyContent: 'center' }}>
                        {/* Ambient Glow */}
                        <div style={{
                            position: 'absolute',
                            top: '50%', left: '50%',
                            transform: 'translate(-50%, -50%)',
                            width: '300px', height: '300px',
                            background: 'radial-gradient(circle, rgba(139, 92, 246, 0.4) 0%, transparent 70%)',
                            filter: 'blur(60px)',
                            zIndex: 1
                        }}></div>

                        {/* Phone Container */}
                        <div className="glass-card" style={{
                            width: '300px',
                            height: '600px',
                            borderRadius: '40px',
                            border: '8px solid rgba(255,255,255,0.1)',
                            position: 'relative',
                            zIndex: 2,
                            padding: '0',
                            overflow: 'hidden',
                            background: '#0F172A',
                            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)'
                        }}>
                            {/* Screen Content */}
                            <div style={{
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                background: 'linear-gradient(to bottom, #1E1B4B, #0F172A)'
                            }}>
                                <img src="/logo.png" alt="Echo App" style={{
                                    width: '120px',
                                    height: '120px',
                                    filter: 'drop-shadow(0 0 20px rgba(139, 92, 246, 0.5))',
                                    animation: 'pulse 3s infinite ease-in-out'
                                }} />
                                <h3 style={{ marginTop: '24px', fontSize: '1.5rem' }}>Echo</h3>
                                <p style={{ fontSize: '0.875rem', opacity: 0.6 }}>Connect with Feeling</p>

                                <div style={{ marginTop: '40px', width: '80%', padding: '16px', borderRadius: '16px', background: 'rgba(255,255,255,0.05)', backdropFilter: 'blur(10px)' }}>
                                    <div style={{ width: '40%', height: '8px', background: 'rgba(255,255,255,0.2)', borderRadius: '4px', marginBottom: '8px' }}></div>
                                    <div style={{ width: '80%', height: '8px', background: 'rgba(255,255,255,0.1)', borderRadius: '4px' }}></div>
                                </div>
                                <div style={{ marginTop: '12px', width: '80%', padding: '16px', borderRadius: '16px', background: 'rgba(255,255,255,0.05)', backdropFilter: 'blur(10px)' }}>
                                    <div style={{ width: '50%', height: '8px', background: 'rgba(255,255,255,0.2)', borderRadius: '4px', marginBottom: '8px' }}></div>
                                    <div style={{ width: '70%', height: '8px', background: 'rgba(255,255,255,0.1)', borderRadius: '4px' }}></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <style>{`
                @keyframes pulse {
                    0% { transform: scale(1); filter: drop-shadow(0 0 20px rgba(139, 92, 246, 0.5)); }
                    50% { transform: scale(1.05); filter: drop-shadow(0 0 30px rgba(139, 92, 246, 0.8)); }
                    100% { transform: scale(1); filter: drop-shadow(0 0 20px rgba(139, 92, 246, 0.5)); }
                }
            `}</style>
        </section>
    );
};

export default HeroSection;
