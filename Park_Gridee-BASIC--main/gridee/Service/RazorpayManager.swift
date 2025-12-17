//
//  RazorpayManager.swift
//  gridee
//
//  Created by Rishabh on 11/10/25.
//

import Foundation
import Razorpay
import UIKit

class RazorpayManager: NSObject {
    static let shared = RazorpayManager()
    
    private var razorpay: RazorpayCheckout!
    private var onPaymentSuccess: ((String, String) -> Void)?
    private var onPaymentFailure: (() -> Void)?
    private var currentOrderId: String = ""
    
    private override init() {
        super.init()
    }
    
    func initializeRazorpay() {
        razorpay = RazorpayCheckout.initWithKey("rzp_test_RS3SKqbTQFAamy", andDelegate: self)
    }
    
    func openCheckout(orderId: String, amount: Double, userName: String, userEmail: String, userPhone: String, onSuccess: @escaping (String, String) -> Void, onFailure: @escaping () -> Void) {
        
        self.currentOrderId = orderId
        self.onPaymentSuccess = onSuccess
        self.onPaymentFailure = onFailure
        
        let options: [String: Any] = [
            "key": "rzp_test_RS3SKqbTQFAamy",
            "amount": Int(amount * 100),
            "currency": "INR",
            "name": "Gridee Parking",
            "description": "Add Money to Wallet",
            "order_id": orderId,
            "prefill": [
                "contact": userPhone,
                "email": userEmail,
                "name": userName
            ],
            "theme": [
                "color": "#0066FF"
            ]
        ]
        
        // Get root view controller to present Razorpay
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let rootViewController = windowScene.windows.first?.rootViewController {
                self.razorpay.open(options, displayController: rootViewController)
            } else {
                print("❌ Razorpay not initialized or no root view controller")
                onFailure()
            }
        }
    }
}

extension RazorpayManager: RazorpayPaymentCompletionProtocol {
    func onPaymentSuccess(_ payment_id: String) {
        print("✅ Razorpay payment success: \(payment_id)")
        onPaymentSuccess?(payment_id, currentOrderId)
    }
    
    func onPaymentError(_ code: Int32, description str: String) {
        print("❌ Razorpay payment failed: \(str)")
        onPaymentFailure?()
    }
}
