//
//  AppDelegate.swift
//  gridee
//
//  Created by Rishabh on 14/10/25.
//

import UIKit
import Firebase
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        // Initialize Firebase
        FirebaseApp.configure()
        print("✅ Firebase initialized")
        
        // Set up notification delegates
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        
        // Request notification permission from user
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error = error {
                print("❌ Notification permission error: \(error.localizedDescription)")
                return
            }
            
            print("🔔 Notification permission granted: \(granted)")
            
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        
        return true
    }
    
    // MARK: - APNs Registration
    
    // Called when APNs token is successfully registered
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        print("✅ APNs device token registered")
    }
    
    // Called when APNs registration fails
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("❌ APNs registration failed: \(error.localizedDescription)")
    }
    
    // MARK: - Firebase Cloud Messaging
    
    // Called when FCM token is received or refreshed
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            print("⚠️ FCM token is nil")
            return
        }
        
        print("🔔 FCM Token received: \(token)")
        
        // Save token locally
        UserDefaults.standard.set(token, forKey: "fcmToken")
        
        // Send token to your backend
        sendFCMTokenToBackend(token)
    }
    
    // MARK: - Notification Handling
    
    // Handle notification when app is in FOREGROUND
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        
        print("📬 Notification received (foreground): \(userInfo)")
        
        // Show notification even when app is open
        completionHandler([.banner, .sound, .badge])
    }
    
    // Handle notification TAP (when user clicks on notification)
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        
        print("📬 Notification tapped: \(userInfo)")
        
        // Handle deep linking or navigation here
        // Example: Navigate to booking details if notification contains booking ID
        if let bookingId = userInfo["bookingId"] as? String {
            print("🔗 Navigate to booking: \(bookingId)")
            // Add navigation logic here
        }
        
        completionHandler()
    }
    
    // MARK: - Send FCM Token to Backend
    
    private func sendFCMTokenToBackend(_ token: String) {
        // Get user ID from UserDefaults
        guard let userId = UserDefaults.standard.string(forKey: "userId") else {
            print("⚠️ User ID not found, cannot send FCM token")
            return
        }
        
        // Create URL for backend endpoint
        guard let url = URL(string: "https://10.223.212.195:8443/api/users/\(userId)/fcm-token") else {
            print("❌ Invalid backend URL")
            return
        }
        
        // Create request
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Add JWT token for authentication if available
        if let jwtToken = UserDefaults.standard.string(forKey: "jwtToken") {
            request.setValue("Bearer \(jwtToken)", forHTTPHeaderField: "Authorization")
        }
        
        // Create request body
        let body: [String: String] = ["fcmToken": token]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            print("❌ Failed to serialize FCM token: \(error)")
            return
        }
        
        // Send request to backend
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("❌ Failed to send FCM token: \(error.localizedDescription)")
                return
            }
            
            if let httpResponse = response as? HTTPURLResponse {
                if (200...299).contains(httpResponse.statusCode) {
                    print("✅ FCM token successfully sent to backend")
                } else {
                    print("❌ Backend returned error: \(httpResponse.statusCode)")
                    if let data = data, let responseString = String(data: data, encoding: .utf8) {
                        print("   Response: \(responseString)")
                    }
                }
            }
        }.resume()
    }
}
