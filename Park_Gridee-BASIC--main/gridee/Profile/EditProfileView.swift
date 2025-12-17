
import SwiftUI

struct EditProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var editedName = ""
    @State private var editedPhone = ""
    @State private var editedEmail = ""
    @State private var editedVehicleNumber = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Edit Profile")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.top, 20)
                
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Full Name")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        TextField("Enter your full name", text: $editedName)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Email")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        TextField("Enter your email", text: $editedEmail)
                            .keyboardType(.emailAddress)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Phone Number")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        TextField("Enter your phone number", text: $editedPhone)
                            .keyboardType(.phonePad)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: "car.fill")
                                .foregroundColor(.blue)
                            Text("Vehicle Number")
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                        TextField("Enter vehicle number", text: $editedVehicleNumber)
                            .autocapitalization(.allCharacters)
                            .padding(16)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 24)
                
                Button(action: {
                    authViewModel.updateProfile(
                        name: editedName,
                        phone: editedPhone,
                        vehicleNumber: editedVehicleNumber
                    )
                    dismiss()
                }) {
                    Text("Save Changes")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.black)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 24)
                .padding(.top, 20)
                
                Spacer()
            }
            .navigationTitle("Edit Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            editedName = authViewModel.currentUser?.name ?? ""
            editedPhone = authViewModel.currentUser?.phone ?? ""
            editedEmail = authViewModel.currentUser?.email ?? ""
//            editedVehicleNumber = authViewModel.currentUser?.vehicleNumbers ?? ""
        }
    }
}
