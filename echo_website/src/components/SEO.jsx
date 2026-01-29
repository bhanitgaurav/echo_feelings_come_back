import React from 'react';
import { Helmet } from 'react-helmet-async';

const SEO = ({ title, description, url = 'https://echo.bhanitgaurav.com' }) => {
    const siteTitle = 'Echo - Connect with Feeling';
    const siteDescription = 'Share anonymous feelings and appreciations with people you know. Designed for emotional safety and real connection.';

    // Construct full title: "Page Title | Echo" or just "Echo..."
    const fullTitle = title ? `${title} | Echo` : siteTitle;
    const finalDescription = description || siteDescription;
    const finalUrl = url;
    const image = 'https://echo.bhanitgaurav.com/logo.png';

    return (
        <Helmet>
            {/* Standard Metadata */}
            <title>{fullTitle}</title>
            <meta name="description" content={finalDescription} />
            <link rel="canonical" href={finalUrl} />

            {/* Open Graph / Facebook */}
            <meta property="og:type" content="website" />
            <meta property="og:url" content={finalUrl} />
            <meta property="og:title" content={fullTitle} />
            <meta property="og:description" content={finalDescription} />
            <meta property="og:image" content={image} />

            {/* Twitter */}
            <meta property="twitter:card" content="summary_large_image" />
            <meta property="twitter:url" content={finalUrl} />
            <meta property="twitter:title" content={fullTitle} />
            <meta property="twitter:description" content={finalDescription} />
            <meta property="twitter:image" content={image} />
        </Helmet>
    );
};

export default SEO;
