////
////  grideeApp 2.swift
////  gridee
////
////  Created by Rishabh on 13/10/25.
////
//
//
//import SwiftUI
//@main
//struct grideeApp: App {
//    @StateObject private var authViewModel = AuthViewModel()
//    @StateObject private var homeViewModel = HomeViewModel()
//    @State private var showSplash = true
//    @StateObject private var themeManager = ThemeManager.shared
//    
//    var body: some Scene {
//        WindowGroup {
//            ZStack {
//                if showSplash {
//                    SplashView()
//                        .transition(.asymmetric(
//                            insertion: .opacity,
//                            removal: .opacity.combined(with: .scale(scale: 0.95))
//                        ))
//                        .zIndex(1)
//                        .onAppear {
//                            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
//                                withAnimation(.easeInOut(duration: 0.8)) {
//                                    showSplash = false
//                                }
//                            }
//                        }
//                } else {
//                    mainContent
//                        .transition(.asymmetric(
//                            insertion: .opacity.combined(with: .scale(scale: 1.05)),
//                            removal: .opacity
//                        ))
//                        .zIndex(0)
//                }
//            }
//            .onAppear {
//                setupApp()
//            }
//        }
//    }
//    
//    // ✅ FIXED: Separate computed property that properly reacts to changes
//    @ViewBuilder
//    private var mainContent: some View {
//        Group {
//            if authViewModel.isAuthenticated {
//                // ✅ DEBUG LOGGING
//                let _ = print("=" + String(repeating: "=", count: 60))
//                let _ = print("📍 GRIDEE APP VIEW EVALUATION")
//                let _ = print("   Current Time: \(Date())")
//                let _ = print("   isAuthenticated: \(authViewModel.isAuthenticated)")
//                let _ = print("   userRole enum: \(authViewModel.userRole)")
//                let _ = print("   userRole rawValue: \(authViewModel.userRole.rawValue)")
//                let _ = print("   userRole == .admin: \(authViewModel.userRole == .admin)")
//                let _ = print("   userRole == .user: \(authViewModel.userRole == .user)")
//                let _ = print("   UserDefaults role: \(UserDefaults.standard.string(forKey: "userRole") ?? "nil")")
//                let _ = print("=" + String(repeating: "=", count: 60))
//                
//                if authViewModel.userRole == .admin {
//                    let _ = print("🔐 SHOWING: AdminQRScannerView")
//                    AdminQRScannerView()
//                        .environmentObject(authViewModel)
//                        .environmentObject(themeManager)
//                        .id(authViewModel.userRole) // ✅ Force refresh when role changes
//                } else {
//                    let _ = print("👤 SHOWING: ContentView (User Home)")
//                    ContentView()
//                        .environmentObject(authViewModel)
//                        .environmentObject(homeViewModel)
//                        .environmentObject(themeManager)
//                        .id(authViewModel.userRole) // ✅ Force refresh when role changes
//                }
//            } else {
//                let _ = print("🔑 SHOWING: LoginView")
//                LoginView()
//                    .environmentObject(authViewModel)
//                    .environmentObject(themeManager)
//            }
//        }
//    }
//
//    private func setupApp() {
//        homeViewModel.setAuthViewModel(authViewModel)
//        print("🔗 App.swift: Connected HomeViewModel to AuthViewModel")
//        
//        // ✅ Print initial state
//        print("📊 App Initial State:")
//        print("   isAuthenticated: \(authViewModel.isAuthenticated)")
//        print("   userRole: \(authViewModel.userRole.rawValue)")
//    }
//}
//
//// MARK: - Splash View
//struct SplashView: View {
//    @State private var scale: CGFloat = 0.5
//    @State private var opacity: Double = 0
//    
//    var body: some View {
//        ZStack {
//            Color.black
//                .ignoresSafeArea()
//            
//            VStack(spacing: 20) {
//                Image(systemName: "car.fill")
//                    .resizable()
//                    .aspectRatio(contentMode: .fit)
//                    .frame(width: 100, height: 100)
//                    .foregroundColor(.white)
//                    .scaleEffect(scale)
//                    .opacity(opacity)
//                
//                Text("GRIDEE")
//                    .font(.system(size: 48, weight: .bold, design: .rounded))
//                    .foregroundColor(.white)
//                    .opacity(opacity)
//                
//                Text("Smart Parking Solution")
//                    .font(.subheadline)
//                    .foregroundColor(.gray)
//                    .opacity(opacity)
//            }
//        }
//        .onAppear {
//            withAnimation(.spring(response: 0.8, dampingFraction: 0.6)) {
//                scale = 1.0
//                opacity = 1.0
//            }
//        }
//    }
//}
//
//// MARK: - Theme Manager
//class ThemeManager: ObservableObject {
//    static let shared = ThemeManager()
//    
//    @Published var isDarkMode: Bool = false
//    
//    private init() {
//        // Initialize theme settings
//        if let savedTheme = UserDefaults.standard.value(forKey: "isDarkMode") as? Bool {
//            isDarkMode = savedTheme
//        }
//    }
//    
//    func toggleTheme() {
//        isDarkMode.toggle()
//        UserDefaults.standard.set(isDarkMode, forKey: "isDarkMode")
//    }
//}
