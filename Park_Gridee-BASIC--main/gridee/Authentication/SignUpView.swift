//import SwiftUI
//
//struct SignUpView: View {
//    @EnvironmentObject var authViewModel: AuthViewModel
//    @Environment(\.dismiss) private var dismiss
//
//    @State private var name = ""
//    @State private var email = ""
//    @State private var phone = ""
//    @State private var password = ""
//    @State private var confirmPassword = ""
//    @State private var selectedParkingLot = ""
//
//    @State private var parkingLots: [String] = []
//    @State private var isLoadingParkingLots = true
//
//    // MARK: - Layout constants (FIXED HEIGHT SYSTEM)
//    private enum Layout {
//        static let horizontal: CGFloat = 20
//        static let sectionGap: CGFloat = 18
//        static let rowHeight: CGFloat = 44   // 👈 KEY FIX
//        static let cardRadius: CGFloat = 12
//        static let buttonHeight: CGFloat = 50
//    }
//
//    var body: some View {
//        NavigationView {
//            ScrollView {
//                VStack(alignment: .leading, spacing: Layout.sectionGap) {
//
//                    // MARK: Header
//                    VStack(alignment: .leading, spacing: 6) {
//                        Text("Create Account")
//                            .font(.largeTitle)
//                            .fontWeight(.bold)
//
//                        Text("Fill in your details to get started")
//                            .font(.body)
//                            .foregroundColor(.secondary)
//                    }
//
//                    // MARK: Personal Info
//                    sectionTitle("PERSONAL INFORMATION")
//                    card {
//                        fixedField("Full Name", text: $name)
//                        Divider()
//                        fixedField("Email", text: $email)
//                        Divider()
//                        fixedField("Phone Number", text: $phone)
//                    }
//
//                    // MARK: Parking Lot
//                    sectionTitle("PARKING LOT")
//                    card {
//                        if isLoadingParkingLots {
//                            HStack {
//                                ProgressView()
//                                Text("Loading…")
//                                    .foregroundColor(.secondary)
//                            }
//                            .frame(height: Layout.rowHeight)
//                        } else {
//                            Menu {
//                                ForEach(parkingLots, id: \.self) { lot in
//                                    Button(lot) {
//                                        selectedParkingLot = lot
//                                    }
//                                }
//                            } label: {
//                                HStack {
//                                    Text(
//                                        selectedParkingLot.isEmpty
//                                        ? "Select parking lot"
//                                        : selectedParkingLot
//                                    )
//                                    .foregroundColor(
//                                        selectedParkingLot.isEmpty
//                                        ? .secondary
//                                        : .blue
//                                    )
//
//                                    Spacer()
//                                    Image(systemName: "chevron.up.chevron.down")
//                                        .foregroundColor(.secondary)
//                                }
//                            }
//                            .frame(height: Layout.rowHeight)
//
//                            if !selectedParkingLot.isEmpty {
//                                Divider()
//                                HStack(spacing: 8) {
//                                    Image(systemName: "checkmark.circle.fill")
//                                        .foregroundColor(.green)
//                                    Text(selectedParkingLot)
//                                        .font(.caption)
//                                        .foregroundColor(.secondary)
//                                }
//                                .frame(height: Layout.rowHeight)
//                            }
//                        }
//                    }
//
//                    // MARK: Security
//                    sectionTitle("SECURITY")
//                    card {
//                        fixedSecureField("Password", text: $password)
//                        Divider()
//                        fixedSecureField("Confirm Password", text: $confirmPassword)
//                    }
//
//                    // MARK: Sign Up Button
//                    Button(action: handleSignUp) {
//                        Text("Sign Up")
//                            .font(.headline)
//                            .frame(maxWidth: .infinity)
//                            .frame(height: Layout.buttonHeight)
//                    }
//                    .foregroundColor(.white)
//                    .background(isFormValid() ? Color.blue : Color.gray.opacity(0.3))
//                    .cornerRadius(Layout.cardRadius)
//                    .disabled(!isFormValid())
//
//                    Text("You can add your vehicle details later in the profile section.")
//                        .font(.footnote)
//                        .foregroundColor(.secondary)
//                }
//                .padding(.horizontal, Layout.horizontal)
//                .padding(.top, Layout.horizontal)
//            }
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") { dismiss() }
//                }
//            }
//            .onAppear(perform: loadParkingLots)
//        }
//    }
//
//    // MARK: - Reusable components (FIXED HEIGHT)
//
//    private func sectionTitle(_ title: String) -> some View {
//        Text(title)
//            .font(.caption)
//            .foregroundColor(.secondary)
//    }
//
//    private func card<Content: View>(
//        @ViewBuilder content: () -> Content
//    ) -> some View {
//        VStack(spacing: 0) {
//            content()
//                .padding(.horizontal, 12)
//        }
//        .background(Color(UIColor.secondarySystemBackground))
//        .cornerRadius(Layout.cardRadius)
//    }
//
//    private func fixedField(_ placeholder: String, text: Binding<String>) -> some View {
//        TextField(placeholder, text: text)
//            .frame(height: Layout.rowHeight)
//            .autocapitalization(.none)
//    }
//
//    private func fixedSecureField(_ placeholder: String, text: Binding<String>) -> some View {
//        SecureField(placeholder, text: text)
//            .frame(height: Layout.rowHeight)
//    }
//
//    // MARK: - Logic
//
//    private func isFormValid() -> Bool {
//        !name.isEmpty &&
//        !email.isEmpty &&
//        !phone.isEmpty &&
//        !password.isEmpty &&
//        !confirmPassword.isEmpty &&
//        !selectedParkingLot.isEmpty &&
//        password == confirmPassword &&
//        password.count >= 6
//    }
//
//    private func handleSignUp() {
//        authViewModel.register(
//            name: name,
//            email: email,
//            phone: phone,
//            password: password,
//            parkingLotName: selectedParkingLot
//        ) { _, _ in }
//    }
//
//    private func loadParkingLots() {
//        isLoadingParkingLots = true
//        APIService.shared.fetchParkingLots { result in
//            DispatchQueue.main.async {
//                isLoadingParkingLots = false
//                if case .success(let lots) = result {
//                    parkingLots = lots
//                    if lots.count == 1 {
//                        selectedParkingLot = lots[0]
//                    }
//                }
//            }
//        }
//    }
//}

import SwiftUI

struct SignUpView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var selectedParkingLot = ""

    @State private var parkingLots: [String] = []
    @State private var isLoadingParkingLots = true

    // MARK: - Layout constants (FIXED HEIGHT SYSTEM)
    private enum Layout {
        static let horizontal: CGFloat = 20
        static let sectionGap: CGFloat = 18
        static let rowHeight: CGFloat = 44   // 👈 KEY FIX
        static let cardRadius: CGFloat = 12
        static let buttonHeight: CGFloat = 50
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: Layout.sectionGap) {

                    // MARK: Header
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Create Account")
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text("Fill in your details to get started")
                            .font(.body)
                            .foregroundColor(.secondary)
                    }

                    // MARK: Personal Info
                    sectionTitle("PERSONAL INFORMATION")
                    card {
                        fixedField("Full Name", text: $name)
                        Divider()
                        fixedField("Email", text: $email)
                        Divider()
                        fixedField("Phone Number", text: $phone)
                    }

                    // MARK: Parking Lot
                    sectionTitle("PARKING LOT")
                    card {
                        if isLoadingParkingLots {
                            HStack {
                                ProgressView()
                                Text("Loading…")
                                    .foregroundColor(.secondary)
                            }
                            .frame(height: Layout.rowHeight)
                        } else {
                            Menu {
                                ForEach(parkingLots, id: \.self) { lot in
                                    Button(lot) {
                                        selectedParkingLot = lot
                                    }
                                }
                            } label: {
                                HStack {
                                    Text(
                                        selectedParkingLot.isEmpty
                                        ? "Select parking lot"
                                        : selectedParkingLot
                                    )
                                    .foregroundColor(
                                        selectedParkingLot.isEmpty
                                        ? .secondary
                                        : .blue
                                    )

                                    Spacer()
                                    Image(systemName: "chevron.up.chevron.down")
                                        .foregroundColor(.secondary)
                                }
                            }
                            .frame(height: Layout.rowHeight)

                            if !selectedParkingLot.isEmpty {
                                Divider()
                                HStack(spacing: 8) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.green)
                                    Text(selectedParkingLot)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .frame(height: Layout.rowHeight)
                            }
                        }
                    }

                    // MARK: Security
                    sectionTitle("SECURITY")
                    card {
                        fixedSecureField("Password", text: $password)
                        Divider()
                        fixedSecureField("Confirm Password", text: $confirmPassword)
                    }

                    // MARK: Sign Up Button
                    Button(action: handleSignUp) {
                        Text("Sign Up")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .frame(height: Layout.buttonHeight)
                    }
                    .foregroundColor(.white)
                    .background(isFormValid() ? Color.blue : Color.gray.opacity(0.3))
                    .cornerRadius(Layout.cardRadius)
                    .disabled(!isFormValid())

                    Text("You can add your vehicle details later in the profile section.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, Layout.horizontal)
                .padding(.top, Layout.horizontal)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
            .onAppear(perform: loadParkingLots)
        }
    }

    // MARK: - Reusable components (FIXED HEIGHT)

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.caption)
            .foregroundColor(.secondary)
    }

    private func card<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(spacing: 0) {
            content()
                .padding(.horizontal, 12)
        }
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(Layout.cardRadius)
    }

    private func fixedField(_ placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .frame(height: Layout.rowHeight)
            .autocapitalization(.none)
    }

    private func fixedSecureField(_ placeholder: String, text: Binding<String>) -> some View {
        SecureField(placeholder, text: text)
            .frame(height: Layout.rowHeight)
    }

    // MARK: - Logic

    private func isFormValid() -> Bool {
        !name.isEmpty &&
        !email.isEmpty &&
        !phone.isEmpty &&
        !password.isEmpty &&
        !confirmPassword.isEmpty &&
        !selectedParkingLot.isEmpty &&
        password == confirmPassword &&
        password.count >= 6
    }

    private func handleSignUp() {
        authViewModel.register(
            name: name,
            email: email,
            phone: phone,
            password: password,
            parkingLotName: selectedParkingLot
        ) { _, _ in }
    }

    private func loadParkingLots() {
        isLoadingParkingLots = true
        APIService.shared.fetchParkingLots { result in
            DispatchQueue.main.async {
                isLoadingParkingLots = false
                if case .success(let lots) = result {
                    parkingLots = lots
                    if lots.count == 1 {
                        selectedParkingLot = lots[0]
                    }
                }
            }
        }
    }
}
