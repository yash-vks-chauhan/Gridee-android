//
//  QRCodeScannerView.swift
//  gridee
//
//  Created by Rishabh on 13/10/25.
//


import SwiftUI
import AVFoundation

struct QRCodeScannerView: UIViewRepresentable {
    @ObservedObject var scanner: QRScannerViewModel
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .black
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        if let captureSession = scanner.getCaptureSession() {
            // Remove existing preview layers
            uiView.layer.sublayers?.forEach { layer in
                if layer is AVCaptureVideoPreviewLayer {
                    layer.removeFromSuperlayer()
                }
            }
            
            // Add new preview layer
            let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
            previewLayer.frame = uiView.bounds
            previewLayer.videoGravity = .resizeAspectFill
            uiView.layer.addSublayer(previewLayer)
        }
    }
}
