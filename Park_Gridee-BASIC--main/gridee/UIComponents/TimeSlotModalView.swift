//
//
//import SwiftUI
//
//struct TimeSlotModalView: View {
//    @Binding var selectedTime: Date
//    @Binding var isPresented: Bool
//    
//    @State private var checkInHourSelection: Int = 16
//    @State private var checkInMinuteSelection: Int = 0
//    @State private var checkOutHourSelection: Int = 17
//    @State private var checkOutMinuteSelection: Int = 0
//    @State private var bookingDate = Date()
//    
//    var onConfirm: ((Date, Date) -> Void)?
//    
//    private let minuteOptions = [0, 30]
//    
//    private var availabilityText: String {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        
//        if currentHour >= 20 || currentHour < 8 {
//            return "Available Tomorrow: 8:00 AM - 8:00 PM"
//        } else {
//            return "Available Today: Now - 8:00 PM"
//        }
//    }
//    
//    private var durationInMinutes: Int {
//        let checkInMinutes = checkInHourSelection * 60 + checkInMinuteSelection
//        let checkOutMinutes = checkOutHourSelection * 60 + checkOutMinuteSelection
//        return max(checkOutMinutes - checkInMinutes, 0)
//    }
//    
//    private var formattedDuration: String {
//        let totalMinutes = durationInMinutes
//        let hours = totalMinutes / 60
//        let minutes = totalMinutes % 60
//        
//        if hours > 0 && minutes > 0 {
//            return "\(hours)h \(minutes)m"
//        } else if hours > 0 {
//            return "\(hours)h"
//        } else {
//            return "\(minutes)m"
//        }
//    }
//    
//    private func getCheckInHours() -> [Int] {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        let startHour = max(currentHour, 8)
//        
//        // If current time is past 7 PM, return empty array
//        guard startHour <= 19 else {
//            return []
//        }
//        
//        return Array(startHour...19)
//    }
//
//    private func getCheckOutHours() -> [Int] {
//        // Start from 9 (one hour after minimum check-in of 8) to 20
//        return Array(9...20)
//    }
//
//    
//    var body: some View {
//        VStack(spacing: 20) {
//            Text("Select Time Slot")
//                .font(.title2)
//                .fontWeight(.bold)
//                .padding(.top, 12)
//            
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
//            // Time pickers
//            HStack(spacing: 20) {
//                // Check-in
//                VStack(spacing: 8) {
//                    Label("Start", systemImage: "clock")
//                        .font(.caption)
//                        .foregroundColor(.green)
//                    
//                    HStack(spacing: 4) {
//                        Picker("", selection: $checkInHourSelection) {
//                            ForEach(getCheckInHours(), id: \.self) { h in
//                                Text(String(format: "%02d", h))
//                                    .tag(h)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 50, height: 80)
//                        .clipped()
//                        .onChange(of: checkInHourSelection) { _, _ in
//                            validateCheckOutTime()
//                        }
//                        
//                        Text(":")
//                            .fontWeight(.semibold)
//                        
//                        Picker("", selection: $checkInMinuteSelection) {
//                            ForEach(minuteOptions, id: \.self) { m in
//                                Text(String(format: "%02d", m))
//                                    .tag(m)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 45, height: 80)
//                        .clipped()
//                        .onChange(of: checkInMinuteSelection) { _, _ in
//                            validateCheckOutTime()
//                        }
//                    }
//                }
//                
//                Image(systemName: "arrow.right")
//                    .foregroundColor(.secondary)
//                    .font(.title3)
//                
//                // Check-out
//                VStack(spacing: 8) {
//                    Label("End", systemImage: "clock.badge.checkmark")
//                        .font(.caption)
//                        .foregroundColor(.orange)
//                    
//                    HStack(spacing: 4) {
//                        Picker("", selection: $checkOutHourSelection) {
//                            ForEach(getCheckOutHours(), id: \.self) { h in
//                                Text(String(format: "%02d", h))
//                                    .tag(h)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 50, height: 80)
//                        .clipped()
//                        
//                        Text(":")
//                            .fontWeight(.semibold)
//                        
//                        Picker("", selection: $checkOutMinuteSelection) {
//                            ForEach(minuteOptions, id: \.self) { m in
//                                Text(String(format: "%02d", m))
//                                    .tag(m)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 45, height: 80)
//                        .clipped()
//                    }
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
//            Button {
//                confirmSelection()
//            } label: {
//                HStack {
//                    Image(systemName: "checkmark.circle.fill")
//                    Text("Confirm")
//                }
//                .font(.headline)
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(Color.blue)
//                .foregroundColor(.white)
//                .cornerRadius(12)
//            }
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
//        if currentHour >= 20 || currentHour < 8 {
//            bookingDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
//            checkInHourSelection = 8
//            checkInMinuteSelection = 0
//            checkOutHourSelection = 9
//            checkOutMinuteSelection = 0
//        } else {
//            bookingDate = now
//            checkInHourSelection = max(currentHour, 8)
//            checkInMinuteSelection = 0
//            checkOutHourSelection = checkInHourSelection + 1
//            checkOutMinuteSelection = 0
//        }
//    }
//    
//    private func validateCheckOutTime() {
//        let checkInMinutes = checkInHourSelection * 60 + checkInMinuteSelection
//        var checkOutMinutes = checkOutHourSelection * 60 + checkOutMinuteSelection
//        
//        if checkOutMinutes < checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
//            checkOutMinutes = checkInMinutes + 60
//            checkOutHourSelection = min(checkOutMinutes / 60, 20)
//            checkOutMinuteSelection = checkOutMinutes % 60
//            
//            if checkOutMinuteSelection != 0 && checkOutMinuteSelection != 30 {
//                checkOutMinuteSelection = 30
//            }
//        }
//    }
//    
//    private func confirmSelection() {
//        let calendar = Calendar.current
//        
//        let startTime = calendar.date(
//            bySettingHour: checkInHourSelection,
//            minute: checkInMinuteSelection,
//            second: 0,
//            of: bookingDate
//        ) ?? Date()
//        
//        let endTime = calendar.date(
//            bySettingHour: checkOutHourSelection,
//            minute: checkOutMinuteSelection,
//            second: 0,
//            of: bookingDate
//        ) ?? Date()
//        
//        selectedTime = startTime
//        onConfirm?(startTime, endTime)
//        isPresented = false
//    }
//}

//import SwiftUI
//
//struct TimeSlotModalView: View {
//    @Binding var selectedStartTime: Date   // ✅ CHANGED: Now accepts start time
//    @Binding var selectedEndTime: Date
//    @Binding var isPresented: Bool
//    
//    @State private var checkInHourSelection: Int = 8
//    @State private var checkInMinuteSelection: Int = 0
//    @State private var checkOutHourSelection: Int = 9
//    @State private var checkOutMinuteSelection: Int = 0
//    @State private var bookingDate = Date()
//    
//    
//    
//    var onConfirm: ((Date, Date) -> Void)?
//    
//    
//    private let minuteOptions = [0, 30]
//    
//    private var availabilityText: String {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        
//        if currentHour >= 20 || currentHour < 8 {
//            return "Available Tomorrow: 8:00 AM - 8:00 PM"
//        } else {
//            return "Available Today: Now - 8:00 PM"
//        }
//    }
//    
//    private var durationInMinutes: Int {
//        let checkInMinutes = checkInHourSelection * 60 + checkInMinuteSelection
//        let checkOutMinutes = checkOutHourSelection * 60 + checkOutMinuteSelection
//        return max(checkOutMinutes - checkInMinutes, 0)
//    }
//    
//    private var formattedDuration: String {
//        let totalMinutes = durationInMinutes
//        let hours = totalMinutes / 60
//        let minutes = totalMinutes % 60
//        
//        if hours > 0 && minutes > 0 {
//            return "\(hours)h \(minutes)m"
//        } else if hours > 0 {
//            return "\(hours)h"
//        } else {
//            return "\(minutes)m"
//        }
//    }
//    
//    // ✅ FIXED: Use bookingDate to determine available hours
//    private func getCheckInHours() -> [Int] {
//        let calendar = Calendar.current
//        let now = Date()
//        
//        // Check if booking date is today
//        let isToday = calendar.isDate(bookingDate, inSameDayAs: now)
//        
//        if isToday {
//            // If booking for today, start from current hour or 8 AM
//            let currentHour = calendar.component(.hour, from: now)
//            let startHour = max(currentHour, 8)
//            
//            // If it's past 7 PM, still show available hours for today
//            guard startHour <= 19 else {
//                return [19] // Show last hour if too late
//            }
//            
//            return Array(startHour...19)
//        } else {
//            // If booking for tomorrow or later, show full range
//            return Array(8...19)
//        }
//    }
//
//    private func getCheckOutHours() -> [Int] {
//        return Array(9...20)
//    }
//
//    
//    var body: some View {
//        VStack(spacing: 20) {
//            Text("Select Time Slot")
//                .font(.title2)
//                .fontWeight(.bold)
//                .padding(.top, 12)
//            
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
//            // Time pickers
//            HStack(spacing: 20) {
//                // Check-in
//                VStack(spacing: 8) {
//                    Label("Start", systemImage: "clock")
//                        .font(.caption)
//                        .foregroundColor(.green)
//                    
//                    HStack(spacing: 4) {
//                        Picker("", selection: $checkInHourSelection) {
//                            ForEach(getCheckInHours(), id: \.self) { h in
//                                Text(String(format: "%02d", h))
//                                    .tag(h)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 50, height: 80)
//                        .clipped()
//                        .onChange(of: checkInHourSelection) { _, _ in
//                            validateCheckOutTime()
//                        }
//                        
//                        Text(":")
//                            .fontWeight(.semibold)
//                        
//                        Picker("", selection: $checkInMinuteSelection) {
//                            ForEach(minuteOptions, id: \.self) { m in
//                                Text(String(format: "%02d", m))
//                                    .tag(m)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 45, height: 80)
//                        .clipped()
//                        .onChange(of: checkInMinuteSelection) { _, _ in
//                            validateCheckOutTime()
//                        }
//                    }
//                }
//                
//                Image(systemName: "arrow.right")
//                    .foregroundColor(.secondary)
//                    .font(.title3)
//                
//                // Check-out
//                VStack(spacing: 8) {
//                    Label("End", systemImage: "clock.badge.checkmark")
//                        .font(.caption)
//                        .foregroundColor(.orange)
//                    
//                    HStack(spacing: 4) {
//                        Picker("", selection: $checkOutHourSelection) {
//                            ForEach(getCheckOutHours(), id: \.self) { h in
//                                Text(String(format: "%02d", h))
//                                    .tag(h)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 50, height: 80)
//                        .clipped()
//                        
//                        Text(":")
//                            .fontWeight(.semibold)
//                        
//                        Picker("", selection: $checkOutMinuteSelection) {
//                            ForEach(minuteOptions, id: \.self) { m in
//                                Text(String(format: "%02d", m))
//                                    .tag(m)
//                            }
//                        }
//                        .pickerStyle(.wheel)
//                        .frame(width: 45, height: 80)
//                        .clipped()
//                    }
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
//            Button {
//                confirmSelection()
//            } label: {
//                HStack {
//                    Image(systemName: "checkmark.circle.fill")
//                    Text("Confirm")
//                }
//                .font(.headline)
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(Color.blue)
//                .foregroundColor(.white)
//                .cornerRadius(12)
//            }
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
//        // ✅ FIXED: Set booking date and times correctly
//        if currentHour >= 20 || currentHour < 8 {
//            // After 8 PM or before 8 AM - book for tomorrow
//            bookingDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
//            checkInHourSelection = 8
//            checkInMinuteSelection = 0
//            checkOutHourSelection = 9
//            checkOutMinuteSelection = 0
//        } else {
//            // During business hours - book for today
//            bookingDate = now
//            let startHour = max(currentHour, 8)
//            
//            // Ensure start hour is valid
//            if startHour <= 19 {
//                checkInHourSelection = startHour
//                checkInMinuteSelection = 0
//                checkOutHourSelection = min(startHour + 1, 20)
//                checkOutMinuteSelection = 0
//            } else {
//                // Edge case: very close to 8 PM
//                checkInHourSelection = 19
//                checkInMinuteSelection = 0
//                checkOutHourSelection = 20
//                checkOutMinuteSelection = 0
//            }
//        }
//        
//        validateCheckOutTime()
//    }
//    
//    private func validateCheckOutTime() {
//        let checkInMinutes = checkInHourSelection * 60 + checkInMinuteSelection
//        var checkOutMinutes = checkOutHourSelection * 60 + checkOutMinuteSelection
//        
//        // Ensure minimum 1 hour duration
//        if checkOutMinutes < checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
//            checkOutMinutes = checkInMinutes + 60
//            checkOutHourSelection = min(checkOutMinutes / 60, 20)
//            checkOutMinuteSelection = checkOutMinutes % 60
//            
//            // Round to nearest 30 minutes
//            if checkOutMinuteSelection != 0 && checkOutMinuteSelection != 30 {
//                checkOutMinuteSelection = (checkOutMinuteSelection < 30) ? 0 : 30
//            }
//        }
//    }
//    
//    private func confirmSelection() {
//        let calendar = Calendar.current
//        
//        let startTime = calendar.date(
//            bySettingHour: checkInHourSelection,
//            minute: checkInMinuteSelection,
//            second: 0,
//            of: bookingDate
//        ) ?? Date()
//        
//        let endTime = calendar.date(
//            bySettingHour: checkOutHourSelection,
//            minute: checkOutMinuteSelection,
//            second: 0,
//            of: bookingDate
//        ) ?? Date()
//        
//        // ✅ UPDATE BOTH BINDINGS
//        selectedStartTime = startTime
//        selectedEndTime = endTime
//        
//        onConfirm?(startTime, endTime)
//        isPresented = false
//    }
//
//}

import SwiftUI

struct TimeSlotModalView: View {
    @Binding var selectedStartTime: Date
    @Binding var selectedEndTime: Date
    @Binding var isPresented: Bool
    
    @State private var checkInHourSelection: Int = 8
    @State private var checkInMinuteSelection: Int = 0
    @State private var checkOutHourSelection: Int = 9
    @State private var checkOutMinuteSelection: Int = 0
    @State private var bookingDate = Date()
    
    var onConfirm: ((Date, Date) -> Void)?
    
    private let minuteOptions = [0, 30]
    
    private var availabilityText: String {
        let calendar = Calendar.current
        let currentHour = calendar.component(.hour, from: Date())
        
        if currentHour >= 20 || currentHour < 8 {
            return "Available Tomorrow: 8:00 AM - 8:00 PM"
        } else {
            return "Available Today: Now - 8:00 PM"
        }
    }
    
    private var durationInMinutes: Int {
        let checkInMinutes = checkInHourSelection * 60 + checkInMinuteSelection
        let checkOutMinutes = checkOutHourSelection * 60 + checkOutMinuteSelection
        return max(checkOutMinutes - checkInMinutes, 0)
    }
    
    private var formattedDuration: String {
        let totalMinutes = durationInMinutes
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        
        if hours > 0 && minutes > 0 {
            return "\(hours)h \(minutes)m"
        } else if hours > 0 {
            return "\(hours)h"
        } else {
            return "\(minutes)m"
        }
    }
    
    private func getCheckInHours() -> [Int] {
        let calendar = Calendar.current
        let now = Date()
        let isToday = calendar.isDate(bookingDate, inSameDayAs: now)
        
        if isToday {
            let currentHour = calendar.component(.hour, from: now)
            let startHour = max(currentHour, 8)
            
            guard startHour <= 19 else {
                return [19]
            }
            
            return Array(startHour...19)
        } else {
            return Array(8...19)
        }
    }

    private func getCheckOutHours() -> [Int] {
        return Array(9...20)
    }
    
    var body: some View {
        VStack(spacing: 20) {
            Text("Select Time Slot")
                .font(.title2)
                .fontWeight(.bold)
                .padding(.top, 12)
            
            HStack {
                Image(systemName: "calendar")
                    .foregroundColor(.blue)
                Text(availabilityText)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Divider()
            
            HStack(spacing: 20) {
                VStack(spacing: 8) {
                    Label("Start", systemImage: "clock")
                        .font(.caption)
                        .foregroundColor(.green)
                    
                    HStack(spacing: 4) {
                        Picker("", selection: $checkInHourSelection) {
                            ForEach(getCheckInHours(), id: \.self) { h in
                                Text(String(format: "%02d", h))
                                    .tag(h)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 50, height: 80)
                        .clipped()
                        .onChange(of: checkInHourSelection) { _, _ in
                            validateCheckOutTime()
                        }
                        
                        Text(":")
                            .fontWeight(.semibold)
                        
                        Picker("", selection: $checkInMinuteSelection) {
                            ForEach(minuteOptions, id: \.self) { m in
                                Text(String(format: "%02d", m))
                                    .tag(m)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 45, height: 80)
                        .clipped()
                        .onChange(of: checkInMinuteSelection) { _, _ in
                            validateCheckOutTime()
                        }
                    }
                }
                
                Image(systemName: "arrow.right")
                    .foregroundColor(.secondary)
                    .font(.title3)
                
                VStack(spacing: 8) {
                    Label("End", systemImage: "clock.badge.checkmark")
                        .font(.caption)
                        .foregroundColor(.orange)
                    
                    HStack(spacing: 4) {
                        Picker("", selection: $checkOutHourSelection) {
                            ForEach(getCheckOutHours(), id: \.self) { h in
                                Text(String(format: "%02d", h))
                                    .tag(h)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 50, height: 80)
                        .clipped()
                        
                        Text(":")
                            .fontWeight(.semibold)
                        
                        Picker("", selection: $checkOutMinuteSelection) {
                            ForEach(minuteOptions, id: \.self) { m in
                                Text(String(format: "%02d", m))
                                    .tag(m)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(width: 45, height: 80)
                        .clipped()
                    }
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
            
            Button {
                confirmSelection()
            } label: {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                    Text("Confirm")
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
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
    
    // ✅ FIXED: Proper time rounding algorithm
    private func initializeTimes() {
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        let currentMinute = calendar.component(.minute, from: now)
        
        if currentHour >= 20 || currentHour < 8 {
            // After 8 PM or before 8 AM - book for tomorrow
            bookingDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
            checkInHourSelection = 8
            checkInMinuteSelection = 0
            checkOutHourSelection = 9
            checkOutMinuteSelection = 0
        } else {
            // During business hours - use correct rounding
            bookingDate = now
            
            // ✅ Calculate next valid 30-minute slot
            let nextSlot = getNextValidTimeSlot(hour: currentHour, minute: currentMinute)
            
            checkInHourSelection = nextSlot.hour
            checkInMinuteSelection = nextSlot.minute
            
            // Check-out is 1 hour later
            let endSlot = addHoursToSlot(hour: nextSlot.hour, minute: nextSlot.minute, hours: 1)
            checkOutHourSelection = endSlot.hour
            checkOutMinuteSelection = endSlot.minute
            
            print("🕐 TimeSlotModal initialized:")
            print("   Current: \(currentHour):\(String(format: "%02d", currentMinute))")
            print("   Check-in: \(checkInHourSelection):\(String(format: "%02d", checkInMinuteSelection))")
            print("   Check-out: \(checkOutHourSelection):\(String(format: "%02d", checkOutMinuteSelection))")
        }
        
        validateCheckOutTime()
    }
    
    // ✅ ADD THIS: Correct rounding algorithm
    private func getNextValidTimeSlot(hour: Int, minute: Int) -> (hour: Int, minute: Int) {
        // Add 5-minute buffer
        var totalMinutes = (hour * 60) + minute + 5
        
        // Round up to next 30-minute slot
        let remainder = totalMinutes % 30
        if remainder != 0 {
            totalMinutes += (30 - remainder)
        }
        
        var nextHour = totalMinutes / 60
        let nextMinute = totalMinutes % 60
        
        // Cap at 8 PM
        if nextHour >= 20 {
            nextHour = 8
            return (nextHour, 0)
        }
        
        // Ensure minimum 8 AM
        if nextHour < 8 {
            nextHour = 8
            return (nextHour, 0)
        }
        
        return (nextHour, nextMinute)
    }
    
    // ✅ ADD THIS: Helper to add hours
    private func addHoursToSlot(hour: Int, minute: Int, hours: Int) -> (hour: Int, minute: Int) {
        let newHour = (hour + hours) % 24
        
        // Cap at 8 PM
        if newHour > 20 || (newHour == 20 && minute > 0) {
            return (20, 0)
        }
        
        return (newHour, minute)
    }
    
    private func validateCheckOutTime() {
        let checkInMinutes = checkInHourSelection * 60 + checkInMinuteSelection
        var checkOutMinutes = checkOutHourSelection * 60 + checkOutMinuteSelection
        
        // Ensure minimum 1 hour duration
        if checkOutMinutes < checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
            checkOutMinutes = checkInMinutes + 60
            checkOutHourSelection = min(checkOutMinutes / 60, 20)
            checkOutMinuteSelection = checkOutMinutes % 60
            
            // Round to nearest 30 minutes
            if checkOutMinuteSelection != 0 && checkOutMinuteSelection != 30 {
                checkOutMinuteSelection = (checkOutMinuteSelection < 30) ? 0 : 30
            }
        }
    }
    
    private func confirmSelection() {
        let calendar = Calendar.current
        
        let startTime = calendar.date(
            bySettingHour: checkInHourSelection,
            minute: checkInMinuteSelection,
            second: 0,
            of: bookingDate
        ) ?? Date()
        
        let endTime = calendar.date(
            bySettingHour: checkOutHourSelection,
            minute: checkOutMinuteSelection,
            second: 0,
            of: bookingDate
        ) ?? Date()
        
        selectedStartTime = startTime
        selectedEndTime = endTime
        
        onConfirm?(startTime, endTime)
        isPresented = false
    }
}

