import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    
    @State private var emailOrPhone = ""
    @State private var password = ""
    @State private var isPasswordVisible = false
    @State private var showingSignUp = false

    @FocusState private var focusedField: Field?
    
    enum Field {
        case emailPhone
        case password
    }
    
    // MARK: - Layout Constants (Single Source of Truth)
    private enum Spacing {
        static let screenPadding: CGFloat = 24
        static let section: CGFloat = 28
        static let field: CGFloat = 16
        static let small: CGFloat = 12
        static let buttonGap: CGFloat = 18   // Apple ↔ Google spacing
    }
    
    // MARK: - Form Validation
    private var isFormValid: Bool {
        !emailOrPhone.isEmpty &&
        !password.isEmpty &&
        !authViewModel.isLoading
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: Spacing.section) {
                    
                    // MARK: - Header
                    VStack(spacing: 8) {
                        Text("Welcome!")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                        
                        Text("Sign in to your account")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    
                    // MARK: - Form Fields
                    VStack(spacing: Spacing.field) {
                        
                        // Email / Phone
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email or Phone")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            TextField("Enter your email or phone", text: $emailOrPhone)
                                .onChange(of: emailOrPhone) { _ in
                                    authViewModel.clearError()
                                }
                                .keyboardType(.emailAddress)
                                .textContentType(.username)
                                .autocapitalization(.none)
                                .padding(16)
                                .background(Color(UIColor.systemGray6))
                                .cornerRadius(10)
                                .focused($focusedField, equals: .emailPhone)
                        }
                        
                        // Password
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Password")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            HStack {
                                Group {
                                    if isPasswordVisible {
                                        TextField("Enter your password", text: $password)
                                    } else {
                                        SecureField("Enter your password", text: $password)
                                    }
                                }
                                .onChange(of: password) { _ in
                                    authViewModel.clearError()
                                }
                                .textContentType(.password)
                                .autocapitalization(.none)
                                .focused($focusedField, equals: .password)
                                
                                Button {
                                    isPasswordVisible.toggle()
                                } label: {
                                    Image(systemName: isPasswordVisible ? "eye.slash.fill" : "eye.fill")
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(10)
                        }
                    }
                    
                    // MARK: - Forgot Password
                    HStack {
                        Spacer()
                        Button("Forgot Password?") {
                            print("Forgot Password tapped")
                        }
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)
                    }
                    .padding(.top, Spacing.small)
                    
                    // MARK: - Sign In Button
                    Button {
                        authViewModel.login(
                            email: emailOrPhone,
                            password: password
                        )
                    } label: {
                        HStack {
                            if authViewModel.isLoading {
                                ProgressView()
                                    .progressViewStyle(
                                        CircularProgressViewStyle(tint: .white)
                                    )
                            } else {
                                Text("Sign In")
                                    .font(.headline)
                                    .fontWeight(.semibold)
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(
                            isFormValid
                            ? Color.blue
                            : Color.gray.opacity(0.3)
                        )
                        .cornerRadius(10)
                    }
                    .disabled(!isFormValid)
                    .buttonStyle(ScaleButtonStyle())
                    
                    // MARK: - Error Message
                    if !authViewModel.errorMessage.isEmpty {
                        Text(authViewModel.errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                    
                    // MARK: - Divider
                    HStack {
                        Rectangle()
                            .frame(height: 1)
                            .foregroundColor(.gray.opacity(0.3))
                        
                        Text("or")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                            .padding(.horizontal, 16)
                        
                        Rectangle()
                            .frame(height: 1)
                            .foregroundColor(.gray.opacity(0.3))
                    }
                    .padding(.vertical, Spacing.small)
                    
                    // MARK: - Social Login Buttons
                    VStack(spacing: Spacing.buttonGap) {
                        
                        // Apple
                        Button {
                            // authViewModel.loginWithApple()
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "applelogo")
                                    .font(.system(size: 18, weight: .medium))
                                Text("Continue with Apple")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )
                        }
                        .buttonStyle(ScaleButtonStyle())
                        
                        // Google
                        Button {
                            handleGoogleSignIn()
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "globe")
                                    .font(.system(size: 18, weight: .medium))
                                Text("Continue with Google")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )
                        }
                        .buttonStyle(ScaleButtonStyle())
                    }
                    
                    // MARK: - Sign Up
                    HStack(spacing: 4) {
                        Text("Don't have an account?")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                        
                        Button("Create Account") {
                            showingSignUp = true
                        }
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.blue)
                    }
                }
                .padding(.horizontal, Spacing.screenPadding)
                .padding(.vertical, Spacing.section)
            }
            .scrollIndicators(.hidden)
            .background(Color(UIColor.systemBackground))
            .onTapGesture {
                focusedField = nil
            }
        }
        .sheet(isPresented: $showingSignUp) {
            SignUpView()
                .environmentObject(authViewModel)
        }
    }
    
    // MARK: - Google Sign In
    private func handleGoogleSignIn() {
        guard
            let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
            let rootVC = windowScene.windows.first?.rootViewController
        else { return }
        
        GoogleSignInManager.shared.signIn(
            presentingViewController: rootVC
        ) { idToken, accessToken, error in
            if let error = error {
                authViewModel.errorMessage = error.localizedDescription
                return
            }
            
            if let idToken, let accessToken {
                authViewModel.loginWithGoogle(
                    idToken: idToken,
                    accessToken: accessToken
                )
            }
        }
    }
}

// MARK: - Button Press Animation
struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .opacity(configuration.isPressed ? 0.85 : 1)
            .animation(.easeInOut(duration: 0.12), value: configuration.isPressed)
    }
}
