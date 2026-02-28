import SwiftUI
import AVFoundation

struct CameraPreview: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CameraPreviewController {
        CameraPreviewController()
    }

    func updateUIViewController(_ uiViewController: CameraPreviewController, context: Context) {}
}

class CameraPreviewController: UIViewController {
    private let captureSession = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?

    override func viewDidLoad() {
        super.viewDidLoad()
        configureCaptureSession()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession.startRunning()
        }
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        captureSession.stopRunning()
    }

    private func configureCaptureSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = .medium

        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: device),
              captureSession.canAddInput(input) else {
            captureSession.commitConfiguration()
            return
        }

        captureSession.addInput(input)

        let layer = AVCaptureVideoPreviewLayer(session: captureSession)
        layer.videoGravity = .resizeAspectFill
        layer.frame = view.bounds
        view.layer.addSublayer(layer)
        previewLayer = layer

        captureSession.commitConfiguration()
    }
}
