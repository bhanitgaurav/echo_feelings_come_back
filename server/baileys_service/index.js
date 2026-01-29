const {
    default: makeWASocket,
    useMultiFileAuthState,
    DisconnectReason
} = require('@whiskeysockets/baileys');
const express = require('express');
const QRCode = require('qrcode');
const pino = require('pino');
const fs = require('fs');

const app = express();
app.use(express.json());

const PORT = 3000;
const AUTH_DIR = 'auth_info_baileys';

let sock;
let currentQR = null;
let isConnected = false;

async function connectToWhatsApp() {
    const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);

    sock = makeWASocket({
        auth: state,
        printQRInTerminal: false, // Explicitly set to false (use /qr endpoint or manual logging)
        logger: pino({ level: 'silent' }),
        browser: ["Echo Server", "Chrome", "1.0.0"] // Custom browser validation
    });

    sock.ev.on('creds.update', saveCreds);

    sock.ev.on('connection.update', async (update) => {
        const { connection, lastDisconnect, qr } = update;

        if (qr) {
            currentQR = qr;
            isConnected = false;
            console.log('\n=== SCAN QR CODE BELOW ===\n');
            QRCode.toString(qr, { type: 'terminal', small: true }, (err, url) => {
                if (err) console.error('Failed to print QR:', err);
                else console.log(url);
            });
        }

        if (connection === 'close') {
            const shouldReconnect = lastDisconnect.error?.output?.statusCode !== DisconnectReason.loggedOut;
            console.log('Connection closed due to ', lastDisconnect.error, ', reconnecting ', shouldReconnect);

            isConnected = false;
            currentQR = null;

            if (shouldReconnect) {
                // simple reconnect logic
                setTimeout(connectToWhatsApp, 5000); // retry after 5s
            } else {
                console.log('Logged out. Clearing session and restarting...');
                // Handle logout: specific request by user
                if (fs.existsSync(AUTH_DIR)) {
                    fs.rmSync(AUTH_DIR, { recursive: true, force: true });
                }
                connectToWhatsApp();
            }
        } else if (connection === 'open') {
            console.log('Opened connection');
            isConnected = true;
            currentQR = null; // Clear QR on successful connection
        }
    });
}

connectToWhatsApp();

// API Endpoints

// 1. Send OTP
app.post('/send-otp', async (req, res) => {
    console.log('Received OTP request:', req.body);
    const { phone, otp } = req.body;

    if (!isConnected || !sock) {
        return res.status(503).json({ error: 'WhatsApp service not connected' });
    }

    if (!phone || !otp) {
        return res.status(400).json({ error: 'Missing phone or otp' });
    }

    try {
        // Strip non-numeric characters (remove +)
        const cleanPhone = phone.replace(/\D/g, '');
        const id = `${cleanPhone}@s.whatsapp.net`;

        console.log(`Checking existence for JID: ${id}`);
        // Add a timeout to onWhatsApp
        const existsPromise = sock.onWhatsApp(id);
        const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error('onWhatsApp timeout')), 10000));

        const exists = await Promise.race([existsPromise, timeoutPromise]);

        console.log('Existence check result:', JSON.stringify(exists));

        if (exists && exists[0]?.exists) {
            console.log('Number exists, sending message...');
            const jidToUse = exists[0].jid;
            const message = `Your verification code is ${otp}.\nThis code expires in 5 minutes. Do not share this code with anyone.`;
            await sock.sendMessage(jidToUse, { text: message });
            console.log('Message sent successfully');
            res.json({ success: true, message: 'OTP Sent' });
        } else {
            console.log('Number not found on WhatsApp or check failed');
            // If check fails but we want to try sending anyway (robustness):
            // await sock.sendMessage(id, { text: `Your OTP is: ${otp}` }); 
            // but for now, let's respect the check.
            res.status(404).json({ error: 'Number is not on WhatsApp' });
        }

    } catch (error) {
        console.error('Error sending message:', error);
        res.status(500).json({ error: 'Failed to send message: ' + error.message });
    }
});

// 2. Get Status
app.get('/status', (req, res) => {
    res.json({
        isConnected,
        hasQR: !!currentQR
    });
});

// 3. Get QR
app.get('/qr', async (req, res) => {
    // No-Cache headers
    res.set('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    res.set('Pragma', 'no-cache');
    res.set('Expires', '0');

    if (isConnected) {
        return res.status(400).send('Already Connected to WhatsApp');
    }

    if (!currentQR) {
        return res.status(503).send('Initializing... please wait or check server logs.');
    }

    try {
        // Return as HTML with embedded image
        const url = await QRCode.toDataURL(currentQR);
        const html = `
            <html>
                <head><title>WhatsApp Login</title></head>
                <body style="display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
                    <div style="text-align:center;background:white;padding:2rem;border-radius:10px;box-shadow:0 4px 6px rgba(0,0,0,0.1);">
                        <h2>Scan with WhatsApp</h2>
                        <img src="${url}" alt="Scan me" />
                        <p>Open WhatsApp > Linked Devices > Link a Device</p>
                        <p onclick="window.location.reload()" style="cursor:pointer;color:blue;text-decoration:underline;">Refresh</p>
                    </div>
                </body>
            </html>
        `;
        res.send(html);
    } catch (err) {
        res.status(500).send('Error generating QR');
    }
});

app.listen(PORT, () => {
    console.log(`Baileys Service running on port ${PORT}`);
});
