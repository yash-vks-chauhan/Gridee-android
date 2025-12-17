//
//  OTPVerificationView.swift
//  gridee
//
//  Created by Rishabh on 12/10/25.
//
import SwiftUI

struct OTPVerificationView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var phoneOrEmail = ""
    @State private var otpCode = ""
    @State private var isOTPSent = false
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var countdown = 60
    @State private var timer: Timer?
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()
                
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "message.circle.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)
                    
                    Text(isOTPSent ? "Enter OTP" : "Phone Verification")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text(isOTPSent ? "Enter the code sent to \(phoneOrEmail)" : "We'll send you a verification code")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                }
                .padding(.bottom, 32)
                
                if !isOTPSent {
                    // Phone/Email Input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Phone or Email")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        TextField("Enter phone or email", text: $phoneOrEmail)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                    }
                    
                    Button(action: sendOTP) {
                        Text(isLoading ? "Sending..." : "Send OTP")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(phoneOrEmail.isEmpty || isLoading ? Color.gray : Color.blue)
                            .cornerRadius(12)
                    }
                    .disabled(phoneOrEmail.isEmpty || isLoading)
                } else {
                    // OTP Input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Verification Code")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        TextField("Enter 6-digit code", text: $otpCode)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.center)
                            .font(.title2)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                    }
                    
                    Button(action: verifyOTP) {
                        Text(isLoading ? "Verifying..." : "Verify OTP")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(otpCode.count != 6 || isLoading ? Color.gray : Color.blue)
                            .cornerRadius(12)
                    }
                    .disabled(otpCode.count != 6 || isLoading)
                    
                    // Resend OTP
                    HStack {
                        Text("Didn't receive code?")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                        
                        Button(action: sendOTP) {
                            Text(countdown > 0 ? "Resend in \(countdown)s" : "Resend")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(countdown > 0 ? .gray : .blue)
                        }
                        .disabled(countdown > 0)
                    }
                    .padding(.top, 8)
                }
                
                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .padding()
                }
                
                Spacer()
            }
            .padding(.horizontal, 24)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        HStack {
                            Image(systemName: "chevron.left")
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
    
    private func sendOTP() {
        isLoading = true
        errorMessage = ""
        
        authViewModel.generateOTP(phoneOrEmail: phoneOrEmail) { success, otp in
            isLoading = false
            
            if success {
                isOTPSent = true
                startCountdown()
                print("✅ OTP sent (for dev: \(otp ?? "hidden"))")
            } else {
                errorMessage = "Failed to send OTP"
            }
        }
    }
    
    private func verifyOTP() {
        isLoading = true
        errorMessage = ""
        
        authViewModel.validateOTP(phoneOrEmail: phoneOrEmail, otp: otpCode) { success in
            isLoading = false
            
            if success {
                print("✅ OTP verified successfully")
                dismiss()
            } else {
                errorMessage = "Invalid OTP. Please try again."
            }
        }
    }
    
    private func startCountdown() {
        countdown = 60
        timer?.invalidate()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if countdown > 0 {
                countdown -= 1
            } else {
                timer?.invalidate()
            }
        }
    }
}

