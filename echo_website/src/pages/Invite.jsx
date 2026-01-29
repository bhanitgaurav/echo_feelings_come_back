import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import SEO from '../components/SEO';
import '../App.css';

const Invite = () => {
    const [searchParams] = useSearchParams();
    const [statusMessage, setStatusMessage] = useState('');
    const [platform, setPlatform] = useState('DESKTOP'); // DESKTOP, ANDROID, IOS

    const code = searchParams.get('code') || 'WELCOME';
    const ANDROID_PACKAGE = "com.bhanit.apps.echo";
    // const IOS_APP_ID = "id6757484280"; // Unused in this logic currently but kept for ref
    const APP_SCHEME = "echoapp://invite";

    useEffect(() => {
        const userAgent = navigator.userAgent || navigator.vendor || window.opera;
        if (/android/i.test(userAgent)) {
            setPlatform('ANDROID');
        } else if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {
            setPlatform('IOS');
        } else {
            setPlatform('DESKTOP');
        }
    }, []);

    const copyCode = () => {
        navigator.clipboard.writeText(code).then(() => {
            setStatusMessage("Code copied!");
            setTimeout(() => setStatusMessage(""), 2000);
        }).catch(err => {
            console.error('Failed to copy: ', err);
        });
    };

    const handleDownload = (e) => {
        copyCode();

        if (platform === 'ANDROID') {
            // Android Strategy: Try deep link, fallback to store
            e.preventDefault(); // Prevent default since we might do fancy stuff

            // Construct Store URL with referrer
            let currentParams = new URLSearchParams(searchParams);
            if (!currentParams.has("code") && code && code !== "WELCOME") {
                currentParams.set("code", code);
            }
            let referrerString = currentParams.toString();
            if (!referrerString) referrerString = "utm_source=invite";
            const encodedReferrer = encodeURIComponent(referrerString);

            const playStoreUrl = `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE}&referrer=${encodedReferrer}`;
            const appUrl = `${APP_SCHEME}?code=${code}`;

            // Redirect
            window.location.href = playStoreUrl;

        } else if (platform === 'IOS') {
            // iOS Strategy
            // Just let the link click happen (which will be set in the render)
        } else {
            // Desktop
            e.preventDefault();
            alert("Please open this link on your mobile device.");
        }
    };

    // Determine Button Text & Href based on platform
    let btnText = "Download Mobile App";
    let btnHref = "#";

    if (platform === 'ANDROID') {
        btnText = "Open / Download on Android";
        // Href is handled in handleDownload but we can set a fallback
        btnHref = "#";
    } else if (platform === 'IOS') {
        btnText = "Copy Code & Download on iOS";
        btnHref = `https://apps.apple.com/in/app/echo-feelings-come-back/id6757484280?ct=invite_${code}`;
    }

    // Styles specific to invite page
    const inviteStyles = {
        codeBox: {
            background: 'var(--bg-card)',
            padding: '15px',
            borderRadius: '10px',
            fontFamily: 'monospace',
            fontSize: '24px',
            letterSpacing: '2px',
            marginBottom: '20px',
            cursor: 'pointer',
            border: '2px dashed #ccc',
            display: 'inline-block',
            minWidth: '200px',
            color: 'var(--text-primary)',
            borderColor: 'var(--border-color)'
        },
        container: {
            maxWidth: '400px',
            textAlign: 'center',
            margin: '40px auto',
            color: 'var(--text-primary)'
        }
    };

    return (
        <div className="container" style={inviteStyles.container}>
            <SEO title="You're Invited!" description="Join me on Echo to exchange honest feelings anonymously." />
            <div className="header">
                <img src="/logo.png" alt="Echo Logo" className="logo" />
            </div>

            <h1>You're Invited!</h1>
            <p style={{ marginBottom: '30px', color: 'var(--text-secondary)' }}>
                Join me on Echo to exchange honest feelings anonymously — without the pressure of a conversation.
            </p>

            <div style={inviteStyles.codeBox} onClick={copyCode} className="code-box-themed">
                {code}
            </div>

            <a href={btnHref} className="store-btn" style={{ width: '100%', justifyContent: 'center', textAlign: 'center', background: '#007AFF' }} onClick={handleDownload}>
                {btnText}
            </a>

            <div style={{ marginTop: '10px', color: '#007AFF', minHeight: '20px' }}>
                {statusMessage}
            </div>

            <div className="footer" style={{ marginTop: '40px' }}>
                <p>&copy; 2026 Echo App.</p>
            </div>
        </div>
    );
};

export default Invite;
