//import SwiftUI
//
//struct ProfileOptionsSection: View {
//    @State private var showingVehicles = false
//    @State private var showingNotifications = false
//    @State private var showingSettings = false
//    @State private var showingHelpSupport = false
//    
//    var body: some View {
//        Section {
//            ProfileOptionRow(
//                icon: "car.fill",
//                title: "My Vehicles",
//                subtitle: "Manage your vehicles",
//                action: { showingVehicles = true }
//            )
//            ProfileOptionRow(
//                icon: "bell.fill",
//                title: "Notifications",
//                subtitle: "Notification preferences",
//                action: { showingNotifications = true }
//            )
//            
//            ProfileOptionRow(
//                icon: "gearshape.fill",
//                title: "Settings",
//                subtitle: "App preferences",
//                action: { showingSettings = true }
//            )
//            ProfileOptionRow(
//                icon: "questionmark.circle.fill",
//                title: "Help & Support",
//                subtitle: "Get help and support",
//                action: { showingHelpSupport = true }
//            )
//        }
//        // Sheet presentations for each option
//        .sheet(isPresented: $showingVehicles) {
//            MyVehiclesView()
//        }
//        .sheet(isPresented: $showingNotifications) {
//            NotificationsView()
//        }
//        
//        .sheet(isPresented: $showingSettings) {
//            SettingsView()
//        }
//        .sheet(isPresented: $showingHelpSupport) {
//            HelpSupportView()
//        }
//    }
//}
//
//// MARK: - Placeholder Views (Create these as separate files)
//
//struct NotificationsView: View {
//    @Environment(\.dismiss) private var dismiss
//    
//    var body: some View {
//        NavigationView {
//            VStack {
//                Text("Notifications Settings")
//                    .font(.title)
//                    .padding()
//                
//                Text("Notification preferences will go here")
//                    .foregroundColor(.secondary)
//                
//                Spacer()
//            }
//            .navigationTitle("Notifications")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Close") { dismiss() }
//                }
//            }
//        }
//    }
//}
//
//#Preview {
//    ProfileOptionsSection()
//}


import SwiftUI

struct ProfileOptionsSection: View {
    @State private var showingVehicles = false
    @State private var showingNotifications = false
    @State private var showingSettings = false
    @State private var showingHelpSupport = false
    @Environment(\.colorScheme) var colorScheme
    
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
        .listRowBackground(cardBackgroundColor)
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
    
    // ✅ Adaptive card background
    private var cardBackgroundColor: Color {
        colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(UIColor.secondarySystemGroupedBackground)
    }
}

// MARK: - Placeholder Views

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

#Preview {
    ProfileOptionsSection()
}
