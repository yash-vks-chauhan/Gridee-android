//
//  NotificationService.swift
//  gridee
//
//  Created by Rishabh on 14/10/25.
//
import Foundation
import UserNotifications

class NotificationService {
    static let shared = NotificationService()
    
    func updateBadgeCount(_ count: Int) {
        DispatchQueue.main.async {
            UNUserNotificationCenter.current().setBadgeCount(count)
        }
    }
    
    func clearBadge() {
        updateBadgeCount(0)
    }
    
    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            print("Notification permission granted: \(granted)")
        }
    }
}


