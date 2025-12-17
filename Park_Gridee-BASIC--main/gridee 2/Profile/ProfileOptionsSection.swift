//import SwiftUI
//
//struct ProfileOptionsSection: View {
//    @EnvironmentObject var homeViewModel: HomeViewModel // ✅ Add this
//    @State private var showingMyVehicles = false // ✅ Add this
//    
//    var body: some View {
//        Section {
//            ProfileOptionRow(
//                icon: "car.fill",
//                title: "My Vehicles",
//                subtitle: "Manage your vehicles",
//                action: {
//                    showingMyVehicles = true // ✅ Updated action
//                }
//            )
//            ProfileOptionRow(
//                icon: "bell.fill",
//                title: "Notifications",
//                subtitle: "Notification preferences",
//                action: { print("Notifications tapped") }
//            )
//            ProfileOptionRow(
//                icon: "lock.shield.fill",
//                title: "Privacy & Security",
//                subtitle: "Account security settings",
//                action: { print("Privacy & Security tapped") }
//            )
//            ProfileOptionRow(
//                icon: "gearshape.fill",
//                title: "Settings",
//                subtitle: "App preferences",
//                action: { print("Settings tapped") }
//            )
//            ProfileOptionRow(
//                icon: "questionmark.circle.fill",
//                title: "Help & Support",
//                subtitle: "Get help and support",
//                action: { print("Help & Support tapped") }
//            )
//        }
//        // ✅ Add the sheet for My Vehicles
//        .sheet(isPresented: $showingMyVehicles) {
//            MyVehiclesView()
//                .environmentObject(homeViewModel)
//        }
//    }
//}


import SwiftUI

struct ProfileOptionsSection: View {
    @State private var showingVehicles = false
    @State private var showingNotifications = false
    @State private var showingPrivacySecurity = false
    @State private var showingSettings = false
    @State private var showingHelpSupport = false
    
    var body: some View {
        Section {
            ProfileOptionRow(
                icon: "car.fill",
                title: "My Vehicles",
                subtitle: "Manage your vehicles",
                action: { showingVehicles = true }
            )
            ProfileOptionRow(
                icon: "bell.fill",
                title: "Notifications",
                subtitle: "Notification preferences",
                action: { showingNotifications = true }
            )
            
            ProfileOptionRow(
                icon: "gearshape.fill",
                title: "Settings",
                subtitle: "App preferences",
                action: { showingSettings = true }
            )
            ProfileOptionRow(
                icon: "questionmark.circle.fill",
                title: "Help & Support",
                subtitle: "Get help and support",
                action: { showingHelpSupport = true }
            )
        }
        // Sheet presentations for each option
        .sheet(isPresented: $showingVehicles) {
            MyVehiclesView()
        }
        .sheet(isPresented: $showingNotifications) {
            NotificationsView()
        }
        
        .sheet(isPresented: $showingSettings) {
            SettingsView()
        }
        .sheet(isPresented: $showingHelpSupport) {
            HelpSupportView()
        }
    }
}

// MARK: - Placeholder Views (Create these as separate files)

struct NotificationsView: View {
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Notifications Settings")
                    .font(.title)
                    .padding()
                
                Text("Notification preferences will go here")
                    .foregroundColor(.secondary)
                
                Spacer()
            }
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}



struct HelpSupportView: View {
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Help & Support")
                    .font(.title)
                    .padding()
                
                Text("Help and support options will go here")
                    .foregroundColor(.secondary)
                
                Spacer()
            }
            .navigationTitle("Help & Support")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
