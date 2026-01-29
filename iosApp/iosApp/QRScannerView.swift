import UIKit
import AVFoundation

class QRScannerView: UIView, AVCaptureMetadataOutputObjectsDelegate {

    private let session = AVCaptureSession()
    private let output = AVCaptureMetadataOutput()
    private let sessionQueue = DispatchQueue(label: "camera_session_queue")
    
    var onScanned: ((String) -> Void)?
    private var isScanning = true

    override init(frame: CGRect) {
        super.init(frame: frame)
        print("SWIFT_BRIDGE: QRScannerView initialized")

        // Check Permissions
        let authStatus = AVCaptureDevice.authorizationStatus(for: .video)
        print("SWIFT_BRIDGE: Authorization status: \(authStatus.rawValue)")
        
        switch authStatus {
        case .authorized:
            setupCamera()
            reportPermissionStatus()
        case .notDetermined:
            print("SWIFT_BRIDGE: Requesting access...")
            let contextSelf = self // Capture self strongly for the async block if needed, or use weak
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                print("SWIFT_BRIDGE: Access granted: \(granted)")
                DispatchQueue.main.async {
                    if granted {
                        self?.setupCamera()
                    }
                    self?.reportPermissionStatus()
                }
            }
        case .denied, .restricted:
            print("SWIFT_BRIDGE: Access DENIED or RESTRICTED")
            reportPermissionStatus()
        @unknown default:
            print("SWIFT_BRIDGE: Unknown auth status")
            reportPermissionStatus()
        }
    }
    
    private func setupCamera() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            print("SWIFT_BRIDGE: Setting up camera on session queue...")
            
            self.session.beginConfiguration()
            // Use 1080p for better frame rate/video processing
            self.session.sessionPreset = .hd1920x1080
            
            guard let device = AVCaptureDevice.default(for: .video) else {
                print("SWIFT_BRIDGE: No video device found")
                self.session.commitConfiguration()
                return
            }
            
            // Configure Autofocus (Device Lock)
            do {
                try device.lockForConfiguration()
                if device.isFocusModeSupported(.continuousAutoFocus) {
                    device.focusMode = .continuousAutoFocus
                }
                device.unlockForConfiguration()
            } catch {
                print("SWIFT_BRIDGE: Could not lock device for configuration: \(error)")
            }
            
            // Add Input
            do {
                let input = try AVCaptureDeviceInput(device: device)
                if self.session.canAddInput(input) {
                    self.session.addInput(input)
                }
            } catch {
                print("SWIFT_BRIDGE: Error creating device input: \(error)")
                self.session.commitConfiguration()
                return
            }
            
            // Add Output
            if self.session.canAddOutput(self.output) {
                self.session.addOutput(self.output)
                self.output.setMetadataObjectsDelegate(self, queue: self.sessionQueue)
                
                if self.output.availableMetadataObjectTypes.contains(.qr) {
                    self.output.metadataObjectTypes = [.qr]
                }
            }
            
            self.session.commitConfiguration()
            
            // Add Zoom (Device Lock)
            do {
                try device.lockForConfiguration()
                device.videoZoomFactor = min(1.3, device.activeFormat.videoMaxZoomFactor)
                device.unlockForConfiguration()
            } catch {
                print("SWIFT_BRIDGE: Zoom error: \(error)")
            }
            
            // Create Preview Layer on Main Thread
            DispatchQueue.main.async {
                let preview = AVCaptureVideoPreviewLayer(session: self.session)
                preview.videoGravity = .resizeAspectFill
                preview.frame = self.bounds
                self.layer.addSublayer(preview)
            }
            
            // Start Session (already on sessionQueue, so we call directly or let helper do it)
            if !self.session.isRunning {
                self.session.startRunning()
                print("SWIFT_BRIDGE: Session started (setup complete)")
            }
        }
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    
    override func willMove(toWindow newWindow: UIWindow?) {
        super.willMove(toWindow: newWindow)
        if newWindow == nil {
            print("SWIFT_BRIDGE: View removed from window. Stopping session.")
            stopSession()
        } else {
             print("SWIFT_BRIDGE: View added to window. Starting session.")
             startSession()
        }
    }
    
    deinit {
        print("SWIFT_BRIDGE: QRScannerView deinit. Stopping session.")
        stopSession()
    }
    
    private func startSession() {
        let session = self.session
        sessionQueue.async { [weak self] in
            // Safe to access self here for state, but session is captured
            self?.isScanning = true
            
            if !session.isRunning {
                session.startRunning()
                print("SWIFT_BRIDGE: Session started")
            }
        }
    }
    
    private func stopSession() {
        // Capture session locally to avoid weak-self issues in deinit
        let session = self.session 
        sessionQueue.async {
            if session.isRunning {
                session.stopRunning()
                print("SWIFT_BRIDGE: Session stopped.")
            }
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        
        if let preview = layer.sublayers?.first as? AVCaptureVideoPreviewLayer {
            preview.frame = bounds
            
            // Ensure full screen scanning
            output.rectOfInterest = CGRect(x: 0, y: 0, width: 1, height: 1)
            
            // Fix Orientation safely
             if let windowScene = window?.windowScene {
                 let orientation = windowScene.interfaceOrientation
                 if preview.connection?.isVideoOrientationSupported == true {
                     switch orientation {
                     case .portrait:
                         preview.connection?.videoOrientation = .portrait
                     case .portraitUpsideDown:
                         preview.connection?.videoOrientation = .portraitUpsideDown
                     case .landscapeLeft:
                         preview.connection?.videoOrientation = .landscapeLeft
                     case .landscapeRight:
                         preview.connection?.videoOrientation = .landscapeRight
                     default:
                         preview.connection?.videoOrientation = .portrait
                     }
                 }
             }
        }
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        // Run on sessionQueue (background)
        if !isScanning { return }
        
        for obj in metadataObjects {
             if let readable = obj as? AVMetadataMachineReadableCodeObject {
                 if let value = readable.stringValue {
                     if readable.type == .qr {
                         isScanning = false // Lock immediately on background thread
                         print("SWIFT_BRIDGE: Valid QR found: \(value)")
                         
                         // Stop session directly here (since we are on sessionQueue) 
                         // or call stopSession() which asyncs to same queue (safe)
                         stopSession()
                         
                         // Invoke callback on Main Thread
                         DispatchQueue.main.async { [weak self] in
                             self?.onScanned?(value)
                         }
                         return
                     }
                 }
             }
        }
    }
    
    // MARK: - Public Control Methods
    
    var onPermissionStatus: ((Bool) -> Void)?
    
    func reportPermissionStatus() {
        let authStatus = AVCaptureDevice.authorizationStatus(for: .video)
        let granted = (authStatus == .authorized)
        print("SWIFT_BRIDGE: Reporting permission status: \(granted)")
        DispatchQueue.main.async { [weak self] in
             self?.onPermissionStatus?(granted)
        }
    }
    
    func toggleTorch(on: Bool) {
        sessionQueue.async { [weak self] in
            guard let device = AVCaptureDevice.default(for: .video), device.hasTorch else { 
                print("SWIFT_BRIDGE: No torch available")
                return 
            }
            
            do {
                try device.lockForConfiguration()
                device.torchMode = on ? .on : .off
                device.unlockForConfiguration()
                print("SWIFT_BRIDGE: Torch set to \(on)")
            } catch {
                print("SWIFT_BRIDGE: Torch error: \(error)")
            }
        }
    }
}
