import Foundation
import CoreImage
import UIKit

class QRCodeGenerator: ObservableObject {
    static let shared = QRCodeGenerator()
    private let context = CIContext()
    
    private init() {}
    
    func generateQRCode(from bookingId: String) -> UIImage {
        // Convert booking ID to data
        let data = Data(bookingId.utf8)
        
        // ✅ FIXED: Use correct CIFilter initialization
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else {
            print("❌ Failed to create QR code filter")
            return UIImage(systemName: "xmark.circle") ?? UIImage()
        }
        
        // Set filter values
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        
        guard let outputImage = filter.outputImage else {
            print("❌ Failed to generate QR code for booking: \(bookingId)")
            return UIImage(systemName: "xmark.circle") ?? UIImage()
        }
        
        // Scale up the QR code
        let scaleX: CGFloat = 10.0
        let scaleY: CGFloat = 10.0
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))
        
        // Convert to UIImage
        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            print("❌ Failed to create CGImage for booking: \(bookingId)")
            return UIImage(systemName: "xmark.circle") ?? UIImage()
        }
        
        print("✅ Generated QR code for booking ID: \(bookingId)")
        return UIImage(cgImage: cgImage)
    }
}
