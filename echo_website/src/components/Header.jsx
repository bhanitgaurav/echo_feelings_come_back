import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';


const Header = () => {

    const [scrolled, setScrolled] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const location = useLocation();
    const isHome = location.pathname === '/';

    useEffect(() => {
        const handleScroll = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    // Close menu when route changes
    useEffect(() => {
        setIsMenuOpen(false);
    }, [location]);

    const scrollTo = (id) => {
        setIsMenuOpen(false); // Close menu if open
        if (!isHome) {
            return;
        }
        const element = document.getElementById(id);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth' });
        }
    };

    return (
        <nav className={`navbar ${scrolled ? 'glass visible' : ''}`}>
            <div className="container nav-content">
                <Link to="/" className="nav-logo" onClick={() => { isHome && window.scrollTo({ top: 0, behavior: 'smooth' }); setIsMenuOpen(false); }}>
                    <img src="/logo.png" alt="Echo" />
                    <span>Echo</span>
                </Link>

                {/* Mobile Toggle */}
                <div className="mobile-toggle" onClick={() => setIsMenuOpen(!isMenuOpen)}>
                    <i className={`fas ${isMenuOpen ? 'fa-times' : 'fa-bars'}`}></i>
                </div>

                <div className="nav-actions">
                    {/* Navigation Links - Desktop */}
                    <div className="nav-links desktop-only">
                        <Link to="/about">About</Link>
                        {isHome ? (
                            <button onClick={() => scrollTo('support')}>Support</button>
                        ) : (
                            <Link to="/#support">Support</Link>
                        )}
                    </div>



                    {/* CTA */}
                    <a href="https://play.google.com/store/apps/details?id=com.bhanit.apps.echo"
                        className="btn btn-primary btn-sm desktop-only"
                        style={{ padding: '8px 20px', fontSize: '0.9rem' }}>
                        Get App
                    </a>
                </div>
            </div>

            {/* Mobile Menu */}
            <div className={`mobile-menu ${isMenuOpen ? 'open' : ''}`}>
                <div className="mobile-nav-links">
                    <Link to="/about">About</Link>
                    {isHome ? (
                        <button onClick={() => scrollTo('support')}>Support</button>
                    ) : (
                        <Link to="/#support">Support</Link>
                    )}
                    <div className="mobile-cta" style={{ marginTop: '20px' }}>
                        <a href="https://play.google.com/store/apps/details?id=com.bhanit.apps.echo" className="btn btn-primary" style={{ width: '100%' }}>
                            Get App
                        </a>
                    </div>
                </div>
            </div>

            <style>{`
                .navbar {
                    position: fixed;
                    top: 0;
                    width: 100%;
                    z-index: 1001; /* Increased z-index */
                    transition: all 0.3s ease;
                    height: var(--header-height);
                }
                .nav-content {
                    height: 100%;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    position: relative;
                    z-index: 1002;
                    background: transparent;
                }
                .nav-logo {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    font-weight: 700;
                    font-size: 1.4rem;
                    color: var(--text-primary);
                }
                .nav-logo img { height: 40px; border-radius: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
                
                .nav-actions { display: flex; align-items: center; gap: 20px; }
                .nav-links { display: flex; gap: 24px; }
                .nav-links button, .nav-links a {
                    background: none;
                    border: none;
                    font-size: 1rem;
                    font-weight: 500;
                    color: var(--text-secondary);
                    cursor: pointer;
                    transition: color 0.2s;
                    font-family: inherit;
                }
                .nav-links button:hover, .nav-links a:hover { color: var(--primary); }

                /* Mobile Toggle */
                .mobile-toggle {
                    display: none;
                    font-size: 1.5rem;
                    color: var(--text-primary);
                    cursor: pointer;
                    padding: 10px;
                }

                /* Mobile Menu */
                .mobile-menu {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100vh;
                    background: var(--bg-body);
                    padding-top: var(--header-height);
                    transform: translateY(-100%);
                    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    z-index: 1000;
                    display: flex;
                    flex-direction: column;
                    padding: 100px 24px 40px;
                    opacity: 0;
                }
                .mobile-menu.open {
                    transform: translateY(0);
                    opacity: 1;
                }
                .mobile-nav-links {
                    display: flex;
                    flex-direction: column;
                    gap: 24px;
                    font-size: 1.2rem;
                    text-align: center;
                }
                .mobile-nav-links button, .mobile-nav-links a {
                    background: none;
                    border: none;
                    font-size: 1.5rem;
                    font-weight: 600;
                    color: var(--text-primary);
                }

                @media (max-width: 768px) {
                    .desktop-only { display: none !important; }
                    .mobile-toggle { display: block; order: 3; margin-left: 20px; }
                    .nav-actions { gap: 10px; }
                }
            `}</style>
        </nav>
    );
};

export default Header;
