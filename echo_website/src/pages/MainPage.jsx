import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import HeroSection from '../sections/HeroSection';
import FeaturesSection from '../sections/FeaturesSection';
import SupportSection from '../sections/SupportSection';
import SEO from '../components/SEO';
import '../index.css';

const MainPage = () => {
    const { hash } = useLocation();

    useEffect(() => {
        if (hash) {
            const id = hash.replace('#', '');
            const element = document.getElementById(id);
            if (element) {
                setTimeout(() => element.scrollIntoView({ behavior: 'smooth' }), 100);
            }
        }
    }, [hash]);

    return (
        <div className="main-page">
            <SEO />
            <HeroSection />
            <FeaturesSection />
            <SupportSection />
        </div>
    );
};

export default MainPage;
