//
//
//
//import SwiftUI
//
//struct TimeSlotModalView: View {
//    @Binding var selectedTime: Date
//    @Binding var isPresented: Bool
//    
//    @State private var startTime: Date = Date()
//    @State private var endTime: Date = Date().addingTimeInterval(3600)
//    
//    var onConfirm: ((Date, Date) -> Void)?
//
//    // ✅ SMART DATE LOGIC
//    private var bookingDate: Date {
//        let calendar = Calendar.current
//        let now = Date()
//        let currentHour = calendar.component(.hour, from: now)
//        
//        // After 8 PM (20:00), allow booking for next day
//        if currentHour >= 20 {
//            return calendar.date(byAdding: .day, value: 1, to: now) ?? now
//        }
//        
//        // Before 8 PM, book for today
//        return now
//    }
//    
//    private var allowedTimeRange: ClosedRange<Date> {
//        let calendar = Calendar.current
//        let now = Date()
//        let currentHour = calendar.component(.hour, from: now)
//        let bookingDay = bookingDate
//        
//        // After 8 PM - full range for next day (8 AM to 8 PM)
//        if currentHour >= 20 {
//            let startTime = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: bookingDay)!
//            let endTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: bookingDay)!
//            return startTime...endTime
//        }
//        
//        // Before 8 PM - from current time to 8 PM today
//        let currentTime = now
//        let endTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: bookingDay)!
//        
//        return currentTime...endTime
//    }
//    
//    private var isValidTimeRange: Bool {
//        return endTime > startTime
//    }
//    
//    private var availabilityText: String {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        
//        if currentHour >= 20 {
//            return "Available Tomorrow: 8:00 AM - 8:00 PM"
//        } else {
//            return "Available Today: Now - 8:00 PM"
//        }
//    }
//
//    var body: some View {
//        VStack(spacing: 20) {
//            Text("Select Time Slot")
//                .font(.title2)
//                .fontWeight(.bold)
//                .padding(.top, 12)
//            
//            // ✅ DYNAMIC AVAILABILITY TEXT
//            HStack {
//                Image(systemName: "calendar")
//                    .foregroundColor(.blue)
//                Text(availabilityText)
//                    .font(.subheadline)
//                    .foregroundColor(.secondary)
//            }
//
//            Divider()
//            
//            HStack(spacing: 16) {
//                VStack(spacing: 8) {
//                    Label("Start", systemImage: "clock")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                    
//                    DatePicker(
//                        "",
//                        selection: $startTime,
//                        in: allowedTimeRange,
//                        displayedComponents: .hourAndMinute
//                    )
//                    .datePickerStyle(CompactDatePickerStyle())
//                    .labelsHidden()
//                }
//                
//                Image(systemName: "arrow.right")
//                    .foregroundColor(.secondary)
//                    .font(.title3)
//                
//                VStack(spacing: 8) {
//                    Label("End", systemImage: "clock.badge.checkmark")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                    
//                    DatePicker(
//                        "",
//                        selection: $endTime,
//                        in: startTime...allowedTimeRange.upperBound,
//                        displayedComponents: .hourAndMinute
//                    )
//                    .datePickerStyle(CompactDatePickerStyle())
//                    .labelsHidden()
//                }
//            }
//            .padding()
//            .background(Color(.systemGray6))
//            .cornerRadius(12)
//            
//            HStack {
//                Image(systemName: "timer")
//                    .foregroundColor(.blue)
//                Text("Duration: \(formattedDuration)")
//                    .font(.subheadline)
//                    .fontWeight(.medium)
//                    .foregroundColor(.blue)
//            }
//            .padding(.vertical, 8)
//            .padding(.horizontal, 16)
//            .background(Color.blue.opacity(0.1))
//            .cornerRadius(8)
//            
//            if !isValidTimeRange {
//                HStack {
//                    Image(systemName: "exclamationmark.triangle.fill")
//                        .foregroundColor(.orange)
//                    Text("End time must be after start time")
//                        .font(.caption)
//                        .foregroundColor(.orange)
//                }
//            }
//
//            Button {
//                if isValidTimeRange {
//                    selectedTime = startTime
//                    onConfirm?(startTime, endTime)
//                    isPresented = false
//                }
//            } label: {
//                HStack {
//                    Image(systemName: "checkmark.circle.fill")
//                    Text("Confirm")
//                }
//                .font(.headline)
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(isValidTimeRange ? Color.blue : Color.gray)
//                .foregroundColor(.white)
//                .cornerRadius(12)
//            }
//            .disabled(!isValidTimeRange)
//            .padding(.bottom, 8)
//        }
//        .padding(24)
//        .background(.ultraThinMaterial)
//        .cornerRadius(20)
//        .shadow(radius: 32)
//        .padding(.horizontal, 32)
//        .onAppear {
//            initializeTimes()
//        }
//    }
//    
//    private func initializeTimes() {
//        let calendar = Calendar.current
//        let now = Date()
//        let currentHour = calendar.component(.hour, from: now)
//        
//        if currentHour >= 20 {
//            // After 8 PM - set to 8 AM tomorrow
//            let tomorrow = calendar.date(byAdding: .day, value: 1, to: now)!
//            startTime = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: tomorrow)!
//            endTime = calendar.date(bySettingHour: 9, minute: 0, second: 0, of: tomorrow)!
//        } else {
//            // Before 8 PM - set to current time
//            startTime = now
//            endTime = calendar.date(byAdding: .hour, value: 1, to: now) ?? now.addingTimeInterval(3600)
//            
//            // Ensure end time doesn't exceed 8 PM
//            let maxEndTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: now)!
//            if endTime > maxEndTime {
//                endTime = maxEndTime
//            }
//        }
//    }
//    
//    private var formattedDuration: String {
//        let interval = endTime.timeIntervalSince(startTime)
//        let hours = Int(interval) / 3600
//        let minutes = (Int(interval) % 3600) / 60
//        
//        if hours > 0 && minutes > 0 {
//            return "\(hours)h \(minutes)m"
//        } else if hours > 0 {
//            return "\(hours)h"
//        } else {
//            return "\(minutes)m"
//        }
//    }
//}



import SwiftUI

struct TimeSlotModalView: View {
    @Binding var selectedTime: Date
    @Binding var isPresented: Bool
    
    @State private var startTime: Date = Date()
    @State private var endTime: Date = Date().addingTimeInterval(3600)
    
    var onConfirm: ((Date, Date) -> Void)?

    // ✅ FIXED: SMART DATE LOGIC
    private var bookingDate: Date {
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        
        // After 8 PM (20:00) or before 8 AM, allow booking for next day
        if currentHour >= 20 || currentHour < 8 {
            return calendar.date(byAdding: .day, value: 1, to: now) ?? now
        }
        
        // Between 8 AM and 8 PM, book for today
        return now
    }
    
    private var allowedTimeRange: ClosedRange<Date> {
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        let bookingDay = bookingDate
        
        // ✅ FIXED: After 8 PM OR before 8 AM - full range for next day (8 AM to 8 PM)
        if currentHour >= 20 || currentHour < 8 {
            let startTime = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: bookingDay)!
            let endTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: bookingDay)!
            return startTime...endTime
        }
        
        // ✅ FIXED: Between 8 AM and 8 PM - from current time OR 8 AM (whichever is later) to 8 PM today
        let eightAM = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: bookingDay)!
        let eightPM = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: bookingDay)!
        
        // Use current time if after 8 AM, otherwise use 8 AM
        let effectiveStartTime = now > eightAM ? now : eightAM
        
        return effectiveStartTime...eightPM
    }
    
    private var isValidTimeRange: Bool {
        return endTime > startTime
    }
    
    private var availabilityText: String {
        let calendar = Calendar.current
        let currentHour = calendar.component(.hour, from: Date())
        
        // ✅ FIXED: Show tomorrow if after 8 PM OR before 8 AM
        if currentHour >= 20 || currentHour < 8 {
            return "Available Tomorrow: 8:00 AM - 8:00 PM"
        } else {
            return "Available Today: Now - 8:00 PM"
        }
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("Select Time Slot")
                .font(.title2)
                .fontWeight(.bold)
                .padding(.top, 12)
            
            // ✅ DYNAMIC AVAILABILITY TEXT
            HStack {
                Image(systemName: "calendar")
                    .foregroundColor(.blue)
                Text(availabilityText)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Divider()
            
            HStack(spacing: 16) {
                VStack(spacing: 8) {
                    Label("Start", systemImage: "clock")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    DatePicker(
                        "",
                        selection: $startTime,
                        in: allowedTimeRange,
                        displayedComponents: .hourAndMinute
                    )
                    .datePickerStyle(CompactDatePickerStyle())
                    .labelsHidden()
                }
                
                Image(systemName: "arrow.right")
                    .foregroundColor(.secondary)
                    .font(.title3)
                
                VStack(spacing: 8) {
                    Label("End", systemImage: "clock.badge.checkmark")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    DatePicker(
                        "",
                        selection: $endTime,
                        in: startTime...allowedTimeRange.upperBound,
                        displayedComponents: .hourAndMinute
                    )
                    .datePickerStyle(CompactDatePickerStyle())
                    .labelsHidden()
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            
            HStack {
                Image(systemName: "timer")
                    .foregroundColor(.blue)
                Text("Duration: \(formattedDuration)")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.blue)
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 16)
            .background(Color.blue.opacity(0.1))
            .cornerRadius(8)
            
            if !isValidTimeRange {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.orange)
                    Text("End time must be after start time")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }

            Button {
                if isValidTimeRange {
                    selectedTime = startTime
                    onConfirm?(startTime, endTime)
                    isPresented = false
                }
            } label: {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                    Text("Confirm")
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding()
                .background(isValidTimeRange ? Color.blue : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(!isValidTimeRange)
            .padding(.bottom, 8)
        }
        .padding(24)
        .background(.ultraThinMaterial)
        .cornerRadius(20)
        .shadow(radius: 32)
        .padding(.horizontal, 32)
        .onAppear {
            initializeTimes()
        }
    }
    
    // ✅ FIXED: Initialize times correctly
    private func initializeTimes() {
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        
        // ✅ FIXED: After 8 PM OR before 8 AM - set to 8 AM tomorrow
        if currentHour >= 20 || currentHour < 8 {
            let tomorrow = calendar.date(byAdding: .day, value: 1, to: now)!
            startTime = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: tomorrow)!
            endTime = calendar.date(bySettingHour: 9, minute: 0, second: 0, of: tomorrow)!
        } else {
            // ✅ FIXED: Between 8 AM and 8 PM - set to current time (or 8 AM if somehow before)
            let eightAM = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: now)!
            startTime = now > eightAM ? now : eightAM
            
            // Set end time to 1 hour after start
            endTime = calendar.date(byAdding: .hour, value: 1, to: startTime) ?? startTime.addingTimeInterval(3600)
            
            // Ensure end time doesn't exceed 8 PM
            let maxEndTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: now)!
            if endTime > maxEndTime {
                endTime = maxEndTime
            }
        }
    }
    
    private var formattedDuration: String {
        let interval = endTime.timeIntervalSince(startTime)
        let hours = Int(interval) / 3600
        let minutes = (Int(interval) % 3600) / 60
        
        if hours > 0 && minutes > 0 {
            return "\(hours)h \(minutes)m"
        } else if hours > 0 {
            return "\(hours)h"
        } else {
            return "\(minutes)m"
        }
    }
}
