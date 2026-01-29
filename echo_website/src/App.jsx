import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import Layout from './components/Layout';
import MainPage from './pages/MainPage';
import Invite from './pages/Invite';
import About from './pages/About';
import PrivacyPolicy from './pages/PrivacyPolicy';
import TermsOfService from './pages/TermsOfService';
import AccountDeletion from './pages/AccountDeletion';
import ScrollToTop from './components/ScrollToTop';
import './App.css';

// Redirect component for legacy routes to anchor tags
const LegacyRedirect = ({ hash }) => {
    return <Navigate to={`/#${hash}`} replace />;
};

import { HelmetProvider } from 'react-helmet-async';

function App() {
    return (
        <HelmetProvider>

            <Router>
                <ScrollToTop />
                <Routes>
                    {/* Invite Standalone */}
                    <Route path="/invite" element={<Invite />} />

                    {/* Shared Layout */}
                    <Route element={<Layout />}>
                        <Route path="/" element={<MainPage />} />
                        <Route path="/about" element={<About />} />
                        <Route path="/privacy-policy" element={<PrivacyPolicy />} />
                        <Route path="/terms" element={<TermsOfService />} />
                        <Route path="/account-deletion" element={<AccountDeletion />} />

                        {/* Redirects */}
                        <Route path="/support" element={<LegacyRedirect hash="support" />} />
                        <Route path="/help" element={<LegacyRedirect hash="support" />} />
                    </Route>
                </Routes>
            </Router>

        </HelmetProvider>
    );
}

export default App;
