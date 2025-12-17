import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var emailOrPhone = ""
    @State private var password = ""
    @State private var isPasswordVisible = false
    @State private var showingSignUp = false
    @FocusState private var focusedField: Field?
    
    enum Field {
        case emailPhone, password
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()
                
                // Header
                VStack(spacing: 8) {
                    Text("Welcome Back")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    Text("Sign in to your account")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
                .padding(.bottom, 20)
                
                // Form Fields
                VStack(spacing: 16) {
                    // Email or Phone Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Email or Phone")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                        
                        TextField("Enter your email or phone", text: $emailOrPhone)
                            .keyboardType(.emailAddress)
                            .textContentType(.username)
                            .autocapitalization(.none)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                            .focused($focusedField, equals: .emailPhone)
                    }
                    
                    // Password Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Password")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                        
                        HStack {
                            Group {
                                if isPasswordVisible {
                                    TextField("Enter your password", text: $password)
                                } else {
                                    SecureField("Enter your password", text: $password)
                                }
                            }
                            .textContentType(.password)
                            .autocapitalization(.none)
                            .focused($focusedField, equals: .password)
                            
                            Button(action: { isPasswordVisible.toggle() }) {
                                Image(systemName: isPasswordVisible ? "eye" : "eye.slash")
                                    .foregroundColor(.gray)
                                    .font(.system(size: 16))
                            }
                        }
                        .padding(16)
                        .background(Color(UIColor.systemGray6))
                        .cornerRadius(12)
                    }
                }
                
                // Forgot Password
                HStack {
                    Spacer()
                    Button(action: {
                        print("Forgot Password tapped")
                    }) {
                        Text("Forgot Password?")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.blue)
                    }
                }
                .padding(.top, 8)
                
                // Sign In Button
                Button(action: {
                    authViewModel.login(email: emailOrPhone, password: password)
                }) {
                    HStack {
                        if authViewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .frame(width: 20, height: 20)
                        } else {
                            Text("Sign In")
                                .font(.headline)
                                .fontWeight(.semibold)
                        }
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        (emailOrPhone.isEmpty || password.isEmpty || authViewModel.isLoading)
                            ? Color.gray.opacity(0.6)
                            : Color.black
                    )
                    .cornerRadius(12)
                }
                .disabled(emailOrPhone.isEmpty || password.isEmpty || authViewModel.isLoading)
                .padding(.top, 8)
                
                // Error Message
                if !authViewModel.errorMessage.isEmpty {
                    Text(authViewModel.errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .padding(.horizontal, 4)
                }
                
                // Divider
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
                .padding(.vertical, 8)
                
                // Social Login Buttons
                VStack(spacing: 12) {
                    // Continue with Google
                    Button(action: {
                        authViewModel.loginWithGoogle()
                    }) {
                        HStack(spacing: 12) {
                            Image(systemName: "globe")
                                .font(.system(size: 18, weight: .medium))
                            Text("Continue with Google")
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .background(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )
                    }
                    
                    // Continue with Apple
                    Button(action: {
                        authViewModel.loginWithApple()
                    }) {
                        HStack(spacing: 12) {
                            Image(systemName: "applelogo")
                                .font(.system(size: 18, weight: .medium))
                            Text("Continue with Apple")
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .background(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )
                    }
                }
                
                Spacer()
                
                // Sign Up Link
                HStack {
                    Text("Don't have an account?")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                    Button(action: {
                        showingSignUp = true
                    }) {
                        Text("Create Account")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                    }
                }
                .padding(.bottom, 20)
            }
            .padding(.horizontal, 24)
            .background(Color(UIColor.systemBackground))
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .onTapGesture {
                focusedField = nil
            }
        }
        .sheet(isPresented: $showingSignUp) {
            SignUpView()
                .environmentObject(authViewModel)
        }
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
            .environmentObject(AuthViewModel())
    }
}

