import SwiftUI

struct SignUpView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var fullName = ""
    @State private var email = ""
    @State private var phoneNumber = ""
    @State private var vehicleNumber = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    
    @State private var isPasswordVisible = false
    @State private var isConfirmPasswordVisible = false
    
    @FocusState private var focusedField: Field?
    
    enum Field {
        case fullName, email, phoneNumber, vehicleNumber, password, confirmPassword
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 20) {
                    Spacer()
                        .frame(height: 20)
                    
                    SignUpHeader()
                    
                    SignUpFormFields(
                        fullName: $fullName,
                        email: $email,
                        phoneNumber: $phoneNumber,
                        vehicleNumber: $vehicleNumber,
                        password: $password,
                        confirmPassword: $confirmPassword,
                        isPasswordVisible: $isPasswordVisible,
                        isConfirmPasswordVisible: $isConfirmPasswordVisible,
                        focusedField: $focusedField
                    )
                    
                    SignUpSubmitButton(
                        isFormValid: isFormValid,
                        isLoading: authViewModel.isLoading,
                        action: handleSignUp
                    )
                    
                    ErrorMessageView(errorMessage: authViewModel.errorMessage)
                    
                    SocialSignUpSection(authViewModel: authViewModel)
                    
                    Spacer()
                        .frame(height: 40)
                }
                .padding(.horizontal, 24)
            }
            .navigationTitle("Create Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    BackButton(dismiss: dismiss)
                }
            }
        }
    }
    
    // MARK: - Computed Properties
    private var isFormValid: Bool {
        !fullName.isEmpty &&
        !email.isEmpty &&
        !phoneNumber.isEmpty &&
        !vehicleNumber.isEmpty &&
        !password.isEmpty &&
        !confirmPassword.isEmpty &&
        password == confirmPassword &&
        password.count >= 6
    }
    
    // MARK: - Actions
    private func handleSignUp() {
        print("🔘 Create Account tapped!")
        authViewModel.signUp(
            name: fullName,
            email: email,
            phone: phoneNumber,
            vehicleNumber: vehicleNumber,
            password: password
        )
    }
}

// MARK: - Header Component
struct SignUpHeader: View {
    var body: some View {
        VStack(spacing: 8) {
            Text("Create Account")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            Text("Join us and start parking smarter")
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
        }
        .padding(.bottom, 24)
    }
}

// MARK: - Form Fields Component
struct SignUpFormFields: View {
    @Binding var fullName: String
    @Binding var email: String
    @Binding var phoneNumber: String
    @Binding var vehicleNumber: String
    @Binding var password: String
    @Binding var confirmPassword: String
    @Binding var isPasswordVisible: Bool
    @Binding var isConfirmPasswordVisible: Bool
    @FocusState.Binding var focusedField: SignUpView.Field?
    
    var body: some View {
        VStack(spacing: 16) {
            NameInputField(fullName: $fullName, focusedField: $focusedField)
            EmailInputField(email: $email, focusedField: $focusedField)
            PhoneInputField(phoneNumber: $phoneNumber, focusedField: $focusedField)
            VehicleInputField(vehicleNumber: $vehicleNumber, focusedField: $focusedField)
            PasswordInputFields(
                password: $password,
                confirmPassword: $confirmPassword,
                isPasswordVisible: $isPasswordVisible,
                isConfirmPasswordVisible: $isConfirmPasswordVisible,
                focusedField: $focusedField
            )
        }
    }
}

// MARK: - Individual Input Fields
struct NameInputField: View {
    @Binding var fullName: String
    @FocusState.Binding var focusedField: SignUpView.Field?
    
    var body: some View {
        InputFieldWrapper(title: "Full Name") {
            TextField("Enter your full name", text: $fullName)
                .textContentType(.name)
                .autocapitalization(.words)
                .focused($focusedField, equals: .fullName)
        }
    }
}

struct EmailInputField: View {
    @Binding var email: String
    @FocusState.Binding var focusedField: SignUpView.Field?
    
    var body: some View {
        InputFieldWrapper(title: "Email") {
            TextField("Enter your email", text: $email)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .autocapitalization(.none)
                .focused($focusedField, equals: .email)
        }
    }
}

struct PhoneInputField: View {
    @Binding var phoneNumber: String
    @FocusState.Binding var focusedField: SignUpView.Field?
    
    var body: some View {
        InputFieldWrapper(title: "Phone Number") {
            TextField("Enter your phone number", text: $phoneNumber)
                .keyboardType(.phonePad)
                .textContentType(.telephoneNumber)
                .focused($focusedField, equals: .phoneNumber)
        }
    }
}

struct VehicleInputField: View {
    @Binding var vehicleNumber: String
    @FocusState.Binding var focusedField: SignUpView.Field?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "car.fill")
                    .foregroundColor(.blue)
                    .font(.system(size: 16))
                Text("Vehicle Number")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
            }
            
            TextField("e.g., KA01AB1234", text: $vehicleNumber)
                .autocapitalization(.allCharacters)
                .padding(16)
                .background(Color(UIColor.systemGray6))
                .cornerRadius(12)
                .focused($focusedField, equals: .vehicleNumber)
            
            VehicleValidationIndicator(vehicleNumber: vehicleNumber)
        }
    }
}

struct PasswordInputFields: View {
    @Binding var password: String
    @Binding var confirmPassword: String
    @Binding var isPasswordVisible: Bool
    @Binding var isConfirmPasswordVisible: Bool
    @FocusState.Binding var focusedField: SignUpView.Field?
    
    var body: some View {
        VStack(spacing: 16) {
            PasswordField(
                title: "Password",
                text: $password,
                isVisible: $isPasswordVisible,
                focusedField: $focusedField,
                field: .password
            )
            
            PasswordField(
                title: "Confirm Password",
                text: $confirmPassword,
                isVisible: $isConfirmPasswordVisible,
                focusedField: $focusedField,
                field: .confirmPassword
            )
            
            PasswordValidationMessage(password: password, confirmPassword: confirmPassword)
        }
    }
}

// MARK: - Reusable Components
struct InputFieldWrapper<Content: View>: View {
    let title: String
    let content: Content
    
    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
            
            content
                .padding(16)
                .background(Color(UIColor.systemGray6))
                .cornerRadius(12)
        }
    }
}

struct PasswordField: View {
    let title: String
    @Binding var text: String
    @Binding var isVisible: Bool
    @FocusState.Binding var focusedField: SignUpView.Field?
    let field: SignUpView.Field
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
            
            HStack {
                Group {
                    if isVisible {
                        TextField("Enter password", text: $text)
                    } else {
                        SecureField("Enter password", text: $text)
                    }
                }
                .textContentType(.newPassword)
                .autocapitalization(.none)
                .focused($focusedField, equals: field)
                
                Button(action: { isVisible.toggle() }) {
                    Image(systemName: isVisible ? "eye" : "eye.slash")
                        .foregroundColor(.gray)
                        .font(.system(size: 16))
                }
            }
            .padding(16)
            .background(Color(UIColor.systemGray6))
            .cornerRadius(12)
        }
    }
}

struct VehicleValidationIndicator: View {
    let vehicleNumber: String
    
    var body: some View {
        if vehicleNumber.count >= 6 {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.caption)
                Text("Vehicle number looks good!")
                    .font(.caption)
                    .foregroundColor(.green)
                Spacer()
            }
        }
    }
}

struct PasswordValidationMessage: View {
    let password: String
    let confirmPassword: String
    
    var body: some View {
        if !confirmPassword.isEmpty && password != confirmPassword {
            HStack {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.red)
                    .font(.caption)
                Text("Passwords do not match")
                    .font(.caption)
                    .foregroundColor(.red)
                Spacer()
            }
            .padding(.top, 4)
        }
    }
}

struct SignUpSubmitButton: View {
    let isFormValid: Bool
    let isLoading: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .frame(width: 20, height: 20)
                } else {
                    Text("Create Account")
                        .font(.headline)
                        .fontWeight(.semibold)
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(submitButtonColor)
            .cornerRadius(12)
        }
        .disabled(!isFormValid || isLoading)
        .padding(.top, 24)
    }
    
    private var submitButtonColor: Color {
        (isFormValid && !isLoading) ? Color.black : Color.gray.opacity(0.6)
    }
}

struct ErrorMessageView: View {
    let errorMessage: String
    
    var body: some View {
        if !errorMessage.isEmpty {
            Text(errorMessage)
                .font(.caption)
                .foregroundColor(.red)
                .padding(.top, 8)
        }
    }
}

struct SocialSignUpSection: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        VStack(spacing: 12) {
            DividerWithText()
            SocialSignUpButtons(authViewModel: authViewModel)
        }
        .padding(.top, 20)
    }
}

struct DividerWithText: View {
    var body: some View {
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
    }
}

struct SocialSignUpButtons: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        VStack(spacing: 12) {
            SocialButton(
                icon: "globe",
                text: "Continue with Google",
                action: { authViewModel.loginWithGoogle() }
            )
            
            SocialButton(
                icon: "applelogo",
                text: "Continue with Apple",
                action: { authViewModel.loginWithApple() }
            )
        }
    }
}

struct SocialButton: View {
    let icon: String
    let text: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .medium))
                Text(text)
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
}

struct BackButton: View {
    let dismiss: DismissAction
    
    var body: some View {
        Button(action: { dismiss() }) {
            HStack {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .medium))
                Text("Back")
                    .font(.system(size: 16))
            }
            .foregroundColor(.primary)
        }
    }
}

// MARK: - Preview
struct SignUpView_Previews: PreviewProvider {
    static var previews: some View {
        SignUpView()
            .environmentObject(AuthViewModel())
    }
}

