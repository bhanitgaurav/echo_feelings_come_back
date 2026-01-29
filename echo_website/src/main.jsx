import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>,
)

// Remove splash screen after app mounts
const splashScreen = document.getElementById('splash-screen');
if (splashScreen) {
    // Small delay to ensure smooth transition
    setTimeout(() => {
        splashScreen.classList.add('splash-hidden');
        setTimeout(() => {
            splashScreen.remove();
        }, 500); // Wait for transition out
    }, 500); // Minimum splash display time
}
