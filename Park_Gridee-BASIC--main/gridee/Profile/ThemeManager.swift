//
//  ThemeManager.swift
//  gridee
//
//  Created by admin85 on 03/10/25.
//
//import SwiftUI
//import Combine
//
//class ThemeManager: ObservableObject {
//    @Published var isDarkMode: Bool {
//        didSet {
//            UserDefaults.standard.set(isDarkMode, forKey: "isDarkMode")
//            print("🎨 Theme changed to: \(isDarkMode ? "Dark" : "Light")")
//        }
//    }
//    
//    static let shared = ThemeManager()
//    
//    private init() {
//        self.isDarkMode = UserDefaults.standard.bool(forKey: "isDarkMode")
//        print("🎨 ThemeManager initialized - Dark Mode: \(isDarkMode)")
//    }
//    
//    func toggleDarkMode() {
//        isDarkMode.toggle()
//    }
//    
//    var colorScheme: ColorScheme {
//        isDarkMode ? .dark : .light
//    }
//}
//
import SwiftUI
import Combine

class ThemeManager: ObservableObject {
    @Published var isDarkMode: Bool {
        didSet {
            UserDefaults.standard.set(isDarkMode, forKey: "isDarkMode")
            print("🎨 Theme changed to: \(isDarkMode ? "Dark" : "Light")")
            print("🎨 UserDefaults saved: \(UserDefaults.standard.bool(forKey: "isDarkMode"))") // ✅ Add this
        }
    }
    
    static let shared = ThemeManager()
    
    private init() {
        self.isDarkMode = UserDefaults.standard.bool(forKey: "isDarkMode")
        print("🎨 ThemeManager initialized - Dark Mode: \(isDarkMode)")
    }
    
    func toggleDarkMode() {
        isDarkMode.toggle()
    }
    
    var colorScheme: ColorScheme {
        isDarkMode ? .dark : .light
    }
}
