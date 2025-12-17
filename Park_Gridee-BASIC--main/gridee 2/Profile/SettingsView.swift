////
////  SettingsView.swift
////  gridee
////
////  Created by admin85 on 25/09/25.
////
////
////  SettingsView.swift
////  gridee
////
////  Created by Utkarsh Dubey on 23/09/25.
////
//
//import SwiftUI
//
//struct SettingsView: View {
//    @Environment(\.dismiss) private var dismiss
//    
//    // App Preferences
//    @State private var darkModeEnabled = false
//    @State private var notificationsEnabled = true
//    @State private var locationEnabled = true
//    @State private var autoBookingEnabled = false
//    @EnvironmentObject var themeManager: ThemeManager
//    @EnvironmentObject var authViewModel: AuthViewModel
//    // Booking Preferences
//    @State private var selectedDefaultDuration = 2
//    @State private var reminderTime = 15
//    
//    // Display Preferences
//    @State private var showDistanceInKM = true
//    @State private var show24HourFormat = false
//    
//    // Alert States
//    @State private var showingClearCache = false
//    @State private var showingResetSettings = false
//    
//    private let durationOptions = [1, 2, 3, 4, 6, 8]
//    private let reminderOptions = [5, 10, 15, 30, 60]
//
//    var body: some View {
//        NavigationView {
//            List {
//                // App Preferences Section
//                Section {
//                    SettingsToggleRow(
//                        icon: "moon.fill",
//                        title: "Dark Mode",
//                        subtitle: "Use dark theme for the app",
//                        isOn: $darkModeEnabled,
//                        iconColor: .purple
//                    )
//                    
//                    SettingsToggleRow(
//                        icon: "location.fill",
//                        title: "Location Services",
//                        subtitle: "Find nearest parking spots automatically",
//                        isOn: $locationEnabled,
//                        iconColor: .blue
//                    )
//                    
//                    SettingsToggleRow(
//                        icon: "bell.fill",
//                        title: "Push Notifications",
//                        subtitle: "Receive parking alerts and reminders",
//                        isOn: $notificationsEnabled,
//                        iconColor: .orange
//                    )
//                } header: {
//                    Text("App Preferences")
//                } footer: {
//                    Text("Customize your app experience and permissions")
//                }
//
//                // Booking Settings Section
//                Section {
//                    HStack {
//                        Image(systemName: "clock.fill")
//                            .foregroundColor(.green)
//                            .frame(width: 24, height: 24)
//                        
//                        VStack(alignment: .leading, spacing: 2) {
//                            Text("Default Booking Duration")
//                                .font(.headline)
//                                .foregroundColor(.primary)
//                            Text("Hours")
//                                .font(.caption)
//                                .foregroundColor(.secondary)
//                        }
//                        
//                        Spacer()
//                        
//                        Picker("Duration", selection: $selectedDefaultDuration) {
//                            ForEach(durationOptions, id: \.self) { duration in
//                                Text("\(duration)h").tag(duration)
//                            }
//                        }
//                        .pickerStyle(.menu)
//                    }
//                    .padding(.vertical, 4)
//                    
//                    HStack {
//                        Image(systemName: "alarm.fill")
//                            .foregroundColor(.red)
//                            .frame(width: 24, height: 24)
//                        
//                        VStack(alignment: .leading, spacing: 2) {
//                            Text("Reminder Before Expiry")
//                                .font(.headline)
//                                .foregroundColor(.primary)
//                            Text("Minutes")
//                                .font(.caption)
//                                .foregroundColor(.secondary)
//                        }
//                        
//                        Spacer()
//                        
//                        Picker("Reminder", selection: $reminderTime) {
//                            ForEach(reminderOptions, id: \.self) { time in
//                                Text("\(time)m").tag(time)
//                            }
//                        }
//                        .pickerStyle(.menu)
//                    }
//                    .padding(.vertical, 4)
//                } header: {
//                    Text("Booking Preferences")
//                } footer: {
//                    Text("Configure default booking behavior and timing")
//                }
//
//                // Display Settings Section
//                Section {
//                    SettingsToggleRow(
//                        icon: "ruler.fill",
//                        title: "Distance in Kilometers",
//                        subtitle: "Show distances in KM instead of meters",
//                        isOn: $showDistanceInKM,
//                        iconColor: .blue
//                    )
//                    
//                    SettingsToggleRow(
//                        icon: "clock.badge.fill",
//                        title: "24-Hour Time Format",
//                        subtitle: "Use 24-hour time display",
//                        isOn: $show24HourFormat,
//                        iconColor: .green
//                    )
//                } header: {
//                    Text("Display Settings")
//                } footer: {
//                    Text("Customize how information is displayed")
//                }
//
//                // Campus Integration Section
//                Section {
//                    NavigationRow(
//                        icon: "building.columns.fill",
//                        title: "Campus Integration",
//                        subtitle: "Link with student/faculty ID system",
//                        iconColor: .indigo
//                    ) {
//                        print("Campus integration tapped")
//                    }
//                    
//                    NavigationRow(
//                        icon: "creditcard.fill",
//                        title: "Payment Methods",
//                        subtitle: "Manage cards and campus wallet",
//                        iconColor: .green
//                    ) {
//                        print("Payment methods tapped")
//                    }
//                    
//                    NavigationRow(
//                        icon: "person.crop.circle.fill",
//                        title: "Account Type",
//                        subtitle: "Student, Faculty, or Staff verification",
//                        iconColor: .orange
//                    ) {
//                        print("Account type tapped")
//                    }
//                } header: {
//                    Text("Campus Settings")
//                } footer: {
//                    Text("Settings specific to campus parking system")
//                }
//
//                // Data & Storage Section
//                Section {
//                    NavigationRow(
//                        icon: "externaldrive.fill",
//                        title: "Clear Cache",
//                        subtitle: "Free up 42 MB of cached data",
//                        iconColor: .gray
//                    ) {
//                        showingClearCache = true
//                    }
//                    
//                    NavigationRow(
//                        icon: "arrow.clockwise.circle.fill",
//                        title: "Sync Settings",
//                        subtitle: "Backup preferences to cloud",
//                        iconColor: .cyan
//                    ) {
//                        print("Sync settings tapped")
//                    }
//                } header: {
//                    Text("Data & Storage")
//                }
//
//                // Advanced Section
//                Section {
//                    NavigationRow(
//                        icon: "arrow.counterclockwise.circle.fill",
//                        title: "Reset All Settings",
//                        subtitle: "Restore app to default configuration",
//                        iconColor: .red
//                    ) {
//                        showingResetSettings = true
//                    }
//                } header: {
//                    Text("Advanced")
//                } footer: {
//                    Text("Gridee Parking v2.1.0 • Built for campus community")
//                }
//            }
//            .navigationTitle("Settings")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Close") { dismiss() }
//                }
//            }
//            .alert("Clear Cache", isPresented: $showingClearCache) {
//                Button("Cancel", role: .cancel) { }
//                Button("Clear", role: .destructive) {
//                    print("Cache cleared")
//                }
//            } message: {
//                Text("This will clear 42 MB of cached data including offline maps and recent searches.")
//            }
//            .alert("Reset Settings", isPresented: $showingResetSettings) {
//                Button("Cancel", role: .cancel) { }
//                Button("Reset", role: .destructive) {
//                    print("Settings reset")
//                }
//            } message: {
//                Text("This will restore all app settings to their default values. Your account and vehicles will not be affected.")
//            }
//        }
//    }
//}
//
//// MARK: - Settings Row Components
//
//private struct SettingsToggleRow: View {
//    let icon: String
//    let title: String
//    let subtitle: String
//    @Binding var isOn: Bool
//    let iconColor: Color
//    var disabled: Bool = false
//    
//    var body: some View {
//        HStack(spacing: 12) {
//            Image(systemName: icon)
//                .foregroundColor(iconColor)
//                .frame(width: 24, height: 24)
//            
//            VStack(alignment: .leading, spacing: 2) {
//                Text(title)
//                    .font(.headline)
//                    .foregroundColor(.primary)
//                
//                Text(subtitle)
//                    .font(.caption)
//                    .foregroundColor(.secondary)
//            }
//            
//            Spacer()
//            
//            Toggle("", isOn: $isOn)
//                .disabled(disabled)
//        }
//        .padding(.vertical, 4)
//    }
//}
//
//private struct NavigationRow: View {
//    let icon: String
//    let title: String
//    let subtitle: String
//    let iconColor: Color
//    let action: () -> Void
//    
//    var body: some View {
//        Button(action: action) {
//            HStack(spacing: 12) {
//                Image(systemName: icon)
//                    .foregroundColor(iconColor)
//                    .frame(width: 24, height: 24)
//                
//                VStack(alignment: .leading, spacing: 2) {
//                    Text(title)
//                        .font(.headline)
//                        .foregroundColor(.primary)
//                    
//                    Text(subtitle)
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//                
//                Spacer()
//                
//                Image(systemName: "chevron.right")
//                    .foregroundColor(.secondary)
//                    .font(.caption)
//            }
//            .padding(.vertical, 4)
//        }
//        .buttonStyle(PlainButtonStyle())
//    }
//}
//



//
//  SettingsView.swift
//  gridee
//
//  Created by admin85 on 25/09/25.
//

import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var themeManager: ThemeManager  // ✅ Added
    @EnvironmentObject var authViewModel: AuthViewModel
    
    // App Preferences
    @State private var notificationsEnabled = true
    @State private var locationEnabled = true
    @State private var autoBookingEnabled = false
    
    // Booking Preferences
    @State private var selectedDefaultDuration = 2
    @State private var reminderTime = 15
    
    // Display Preferences
    @State private var showDistanceInKM = true
    @State private var show24HourFormat = false
    
    // Alert States
    @State private var showingClearCache = false
    @State private var showingResetSettings = false
    
    private let durationOptions = [1, 2, 3, 4, 6, 8]
    private let reminderOptions = [5, 10, 15, 30, 60]

    var body: some View {
        NavigationView {
            List {
                // App Preferences Section
                Section {
                    // ✅ UPDATED: Bind to themeManager
                    SettingsToggleRow(
                        icon: themeManager.isDarkMode ? "moon.fill" : "sun.max.fill",
                        title: "Dark Mode",
                        subtitle: "Use dark theme for the app",
                        isOn: $themeManager.isDarkMode,
                        iconColor: themeManager.isDarkMode ? .yellow : .orange
                    )
                    
                    SettingsToggleRow(
                        icon: "location.fill",
                        title: "Location Services",
                        subtitle: "Find nearest parking spots automatically",
                        isOn: $locationEnabled,
                        iconColor: .blue
                    )
                    
                    SettingsToggleRow(
                        icon: "bell.fill",
                        title: "Push Notifications",
                        subtitle: "Receive parking alerts and reminders",
                        isOn: $notificationsEnabled,
                        iconColor: .orange
                    )
                } header: {
                    Text("App Preferences")
                } footer: {
                    Text("Customize your app experience and permissions")
                }

                // Booking Settings Section
                Section {
                    HStack {
                        Image(systemName: "clock.fill")
                            .foregroundColor(.green)
                            .frame(width: 24, height: 24)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Default Booking Duration")
                                .font(.headline)
                                .foregroundColor(.primary)
                            Text("Hours")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Picker("Duration", selection: $selectedDefaultDuration) {
                            ForEach(durationOptions, id: \.self) { duration in
                                Text("\(duration)h").tag(duration)
                            }
                        }
                        .pickerStyle(.menu)
                    }
                    .padding(.vertical, 4)
                    
                    HStack {
                        Image(systemName: "alarm.fill")
                            .foregroundColor(.red)
                            .frame(width: 24, height: 24)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Reminder Before Expiry")
                                .font(.headline)
                                .foregroundColor(.primary)
                            Text("Minutes")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Picker("Reminder", selection: $reminderTime) {
                            ForEach(reminderOptions, id: \.self) { time in
                                Text("\(time)m").tag(time)
                            }
                        }
                        .pickerStyle(.menu)
                    }
                    .padding(.vertical, 4)
                } header: {
                    Text("Booking Preferences")
                } footer: {
                    Text("Configure default booking behavior and timing")
                }

                // Display Settings Section
                Section {
                    SettingsToggleRow(
                        icon: "ruler.fill",
                        title: "Distance in Kilometers",
                        subtitle: "Show distances in KM instead of meters",
                        isOn: $showDistanceInKM,
                        iconColor: .blue
                    )
                    
                    SettingsToggleRow(
                        icon: "clock.badge.fill",
                        title: "24-Hour Time Format",
                        subtitle: "Use 24-hour time display",
                        isOn: $show24HourFormat,
                        iconColor: .green
                    )
                } header: {
                    Text("Display Settings")
                } footer: {
                    Text("Customize how information is displayed")
                }

                // Campus Integration Section
                Section {
                    NavigationRow(
                        icon: "building.columns.fill",
                        title: "Campus Integration",
                        subtitle: "Link with student/faculty ID system",
                        iconColor: .indigo
                    ) {
                        print("Campus integration tapped")
                    }
                    
                    NavigationRow(
                        icon: "creditcard.fill",
                        title: "Payment Methods",
                        subtitle: "Manage cards and campus wallet",
                        iconColor: .green
                    ) {
                        print("Payment methods tapped")
                    }
                    
                    NavigationRow(
                        icon: "person.crop.circle.fill",
                        title: "Account Type",
                        subtitle: "Student, Faculty, or Staff verification",
                        iconColor: .orange
                    ) {
                        print("Account type tapped")
                    }
                } header: {
                    Text("Campus Settings")
                } footer: {
                    Text("Settings specific to campus parking system")
                }

                // Data & Storage Section
                Section {
                    NavigationRow(
                        icon: "externaldrive.fill",
                        title: "Clear Cache",
                        subtitle: "Free up 42 MB of cached data",
                        iconColor: .gray
                    ) {
                        showingClearCache = true
                    }
                    
                    NavigationRow(
                        icon: "arrow.clockwise.circle.fill",
                        title: "Sync Settings",
                        subtitle: "Backup preferences to cloud",
                        iconColor: .cyan
                    ) {
                        print("Sync settings tapped")
                    }
                } header: {
                    Text("Data & Storage")
                }

                // Advanced Section
                Section {
                    NavigationRow(
                        icon: "arrow.counterclockwise.circle.fill",
                        title: "Reset All Settings",
                        subtitle: "Restore app to default configuration",
                        iconColor: .red
                    ) {
                        showingResetSettings = true
                    }
                } header: {
                    Text("Advanced")
                } footer: {
                    Text("Gridee Parking v2.1.0 • Built for campus community")
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { dismiss() }
                }
            }
            .alert("Clear Cache", isPresented: $showingClearCache) {
                Button("Cancel", role: .cancel) { }
                Button("Clear", role: .destructive) {
                    print("Cache cleared")
                }
            } message: {
                Text("This will clear 42 MB of cached data including offline maps and recent searches.")
            }
            .alert("Reset Settings", isPresented: $showingResetSettings) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) {
                    resetAllSettings()
                }
            } message: {
                Text("This will restore all app settings to their default values. Your account and vehicles will not be affected.")
            }
        }
    }
    
    // ✅ ADDED: Reset settings function
    private func resetAllSettings() {
        themeManager.isDarkMode = false
        notificationsEnabled = true
        locationEnabled = true
        autoBookingEnabled = false
        selectedDefaultDuration = 2
        reminderTime = 15
        showDistanceInKM = true
        show24HourFormat = false
        print("✅ All settings reset to defaults")
    }
}

// MARK: - Settings Row Components

private struct SettingsToggleRow: View {
    let icon: String
    let title: String
    let subtitle: String
    @Binding var isOn: Bool
    let iconColor: Color
    var disabled: Bool = false
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(iconColor)
                .frame(width: 24, height: 24)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Toggle("", isOn: $isOn)
                .disabled(disabled)
        }
        .padding(.vertical, 4)
    }
}

private struct NavigationRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let iconColor: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .foregroundColor(iconColor)
                    .frame(width: 24, height: 24)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
                    .font(.caption)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
