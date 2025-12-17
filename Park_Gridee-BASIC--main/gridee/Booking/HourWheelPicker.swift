//
//
//
//import SwiftUI
//
//struct HourWheelPicker: View {
//    @Binding var checkInHour: Int
//    @Binding var checkInMinute: Int
//    @Binding var checkOutHour: Int
//    @Binding var checkOutMinute: Int
//    var bookingDate: Date = Date() // ✅ ADDED: Pass date from parent
//    
//    @Environment(\.colorScheme) var colorScheme
//    
//    private let minuteOptions = [0, 30]
//    
//    var body: some View {
//        VStack(spacing: 20) {
//            // ✅ UPDATED: Date badge with dynamic date
//            HStack(spacing: 8) {
//                Image(systemName: "calendar.circle.fill")
//                    .font(.body)
//                    .foregroundColor(.blue)
//                Text(formatDate(bookingDate))
//                    .font(.subheadline)
//                    .fontWeight(.semibold)
//                    .foregroundColor(.primary)
//            }
//            .padding(.horizontal, 20)
//            .padding(.vertical, 10)
//            .background(
//                Capsule()
//                    .fill(Color.blue.opacity(colorScheme == .dark ? 0.2 : 0.15))
//            )
//            
//            // Time slot pickers
//            HStack(spacing: 20) {
//                // Check-in
//                TimeSlotCard(
//                    label: "CHECK-IN",
//                    hour: checkInHour,
//                    minute: checkInMinute,
//                    color: .green,
//                    icon: "arrow.down.circle.fill",
//                    hours: getCheckInHours(),
//                    onHourChange: { newHour in
//                        checkInHour = newHour
//                        validateCheckOutTime()
//                    },
//                    onMinuteChange: { newMinute in
//                        checkInMinute = newMinute
//                        validateCheckOutTime()
//                    }
//                )
//                
//                // Connector dots
//                VStack(spacing: 3) {
//                    ForEach(0..<3, id: \.self) { _ in
//                        Circle()
//                            .fill(Color.secondary.opacity(0.4))
//                            .frame(width: 3, height: 3)
//                    }
//                }
//                .padding(.top, 28)
//                
//                // Check-out
//                TimeSlotCard(
//                    label: "CHECK-OUT",
//                    hour: checkOutHour,
//                    minute: checkOutMinute,
//                    color: .orange,
//                    icon: "arrow.up.circle.fill",
//                    hours: getCheckOutHours(),
//                    onHourChange: { newHour in
//                        checkOutHour = newHour
//                    },
//                    onMinuteChange: { newMinute in
//                        checkOutMinute = newMinute
//                    }
//                )
//            }
//            
//            // Duration display
//            VStack(spacing: 10) {
//                HStack {
//                    Text("Duration")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                        .fontWeight(.semibold)
//                        .textCase(.uppercase)
//                    
//                    Spacer()
//                    
//                    HStack(spacing: 6) {
//                        Image(systemName: "clock.fill")
//                            .font(.caption)
//                            .foregroundColor(.blue)
//                        Text(formatDuration())
//                            .font(.callout)
//                            .fontWeight(.bold)
//                            .foregroundColor(.primary)
//                    }
//                }
//                
//                // Progress bar
//                GeometryReader { geometry in
//                    ZStack(alignment: .leading) {
//                        Capsule()
//                            .fill(Color.secondary.opacity(colorScheme == .dark ? 0.2 : 0.15))
//                            .frame(height: 5)
//                        
//                        Capsule()
//                            .fill(
//                                LinearGradient(
//                                    colors: [.green, .blue, .orange],
//                                    startPoint: .leading,
//                                    endPoint: .trailing
//                                )
//                            )
//                            .frame(
//                                width: min(geometry.size.width * CGFloat(calculateDurationInMinutes()) / 720, geometry.size.width),
//                                height: 5
//                            )
//                            .animation(.spring(response: 0.4, dampingFraction: 0.7), value: checkOutHour)
//                            .animation(.spring(response: 0.4, dampingFraction: 0.7), value: checkOutMinute)
//                    }
//                }
//                .frame(height: 5)
//            }
//            .padding(.horizontal, 20)
//            .padding(.top, 4)
//        }
//        .padding(.vertical, 24)
//        .padding(.horizontal, 20)
//        .background(
//            RoundedRectangle(cornerRadius: 20)
//                .fill(colorScheme == .dark ? Color(.systemGray6).opacity(0.5) : Color(.systemGray6))
//                .shadow(color: .black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 12, x: 0, y: 4)
//        )
//    }
//    
//    // ✅ ADDED: Format date helper
//    private func formatDate(_ date: Date) -> String {
//        let formatter = DateFormatter()
//        formatter.dateFormat = "d MMM yyyy"
//        return formatter.string(from: date)
//    }
//    
//    private func validateCheckOutTime() {
//        let checkInMinutes = checkInHour * 60 + checkInMinute
//        var checkOutMinutes = checkOutHour * 60 + checkOutMinute
//        
//        if checkOutMinutes < checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
//            checkOutMinutes = checkInMinutes + 60
//            checkOutHour = min(checkOutMinutes / 60, 20)
//            checkOutMinute = checkOutMinutes % 60
//            
//            if checkOutMinute != 0 && checkOutMinute != 30 {
//                checkOutMinute = 30
//            }
//        }
//    }
//    
//    private func getCheckInHours() -> [Int] {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        let startHour = max(currentHour, 8)
//        return Array(startHour...19)
//    }
//    
//    private func getCheckOutHours() -> [Int] {
//        return Array(8...20)
//    }
//    
//    private func calculateDurationInMinutes() -> Int {
//        let checkInMinutes = checkInHour * 60 + checkInMinute
//        let checkOutMinutes = checkOutHour * 60 + checkOutMinute
//        return max(checkOutMinutes - checkInMinutes, 0)
//    }
//    
//    private func formatDuration() -> String {
//        let totalMinutes = calculateDurationInMinutes()
//        let hours = totalMinutes / 60
//        let minutes = totalMinutes % 60
//        
//        if minutes == 0 {
//            return "\(hours)h"
//        } else {
//            return "\(hours)h \(minutes)m"
//        }
//    }
//}
//
//// MARK: - Time Slot Card with Hour and Minute Pickers
//struct TimeSlotCard: View {
//    let label: String
//    let hour: Int
//    let minute: Int
//    let color: Color
//    let icon: String
//    let hours: [Int]
//    let onHourChange: (Int) -> Void
//    let onMinuteChange: (Int) -> Void
//    
//    @Environment(\.colorScheme) var colorScheme
//    @State private var selectedHour: Int
//    @State private var selectedMinute: Int
//    
//    private let minutes = [0, 30]
//    
//    init(label: String, hour: Int, minute: Int, color: Color, icon: String, hours: [Int], onHourChange: @escaping (Int) -> Void, onMinuteChange: @escaping (Int) -> Void) {
//        self.label = label
//        self.hour = hour
//        self.minute = minute
//        self.color = color
//        self.icon = icon
//        self.hours = hours
//        self.onHourChange = onHourChange
//        self.onMinuteChange = onMinuteChange
//        _selectedHour = State(initialValue: hour)
//        _selectedMinute = State(initialValue: minute)
//    }
//    
//    var body: some View {
//        VStack(spacing: 10) {
//            // Label
//            HStack(spacing: 5) {
//                Image(systemName: icon)
//                    .font(.system(size: 10))
//                    .foregroundColor(color)
//                Text(label)
//                    .font(.system(size: 10, weight: .bold))
//                    .foregroundColor(color)
//                    .tracking(0.5)
//            }
//            
//            // Time picker
//            HStack(spacing: 4) {
//                // Hour picker
//                Picker("", selection: $selectedHour) {
//                    ForEach(hours, id: \.self) { h in
//                        Text(String(format: "%02d", h))
//                            .font(.body)
//                            .tag(h)
//                    }
//                }
//                .pickerStyle(.wheel)
//                .frame(width: 60, height: 100)
//                .clipped()
//                .onChange(of: selectedHour) { _, newValue in
//                    onHourChange(newValue)
//                }
//                
//                Text(":")
//                    .font(.title3)
//                    .fontWeight(.semibold)
//                    .foregroundColor(.primary)
//                    .padding(.bottom, 4)
//                
//                // Minute picker
//                Picker("", selection: $selectedMinute) {
//                    ForEach(minutes, id: \.self) { m in
//                        Text(String(format: "%02d", m))
//                            .font(.body)
//                            .tag(m)
//                    }
//                }
//                .pickerStyle(.wheel)
//                .frame(width: 45, height: 100)
//                .clipped()
//                .onChange(of: selectedMinute) { _, newValue in
//                    onMinuteChange(newValue)
//                }
//            }
//            .frame(width: 120)
//            .background(
//                RoundedRectangle(cornerRadius: 14)
//                    .fill(colorScheme == .dark ? Color(.systemGray5) : Color(.systemBackground))
//            )
//            .overlay(
//                RoundedRectangle(cornerRadius: 14)
//                    .strokeBorder(
//                        LinearGradient(
//                            colors: [color.opacity(0.6), color.opacity(0.2)],
//                            startPoint: .topLeading,
//                            endPoint: .bottomTrailing
//                        ),
//                        lineWidth: 2
//                    )
//            )
//            .shadow(color: color.opacity(0.15), radius: 8, x: 0, y: 2)
//        }
//    }
//}


import SwiftUI

struct HourWheelPicker: View {
    @Binding var checkInHour: Int
    @Binding var checkInMinute: Int
    @Binding var checkOutHour: Int
    @Binding var checkOutMinute: Int
    var bookingDate: Date = Date()
    
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        VStack(spacing: 20) {
            // Date badge
            HStack(spacing: 8) {
                Image(systemName: "calendar.circle.fill")
                    .font(.body)
                    .foregroundColor(.blue)
                Text(formatDate(bookingDate))
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(Color.blue.opacity(colorScheme == .dark ? 0.2 : 0.15))
            )
            
            // Time slot pickers
            HStack(spacing: 20) {
                // Check-in
                TimeSlotCard(
                    label: "CHECK-IN",
                    hour: $checkInHour,
                    minute: $checkInMinute,
                    color: .green,
                    icon: "arrow.down.circle.fill",
                    hours: getCheckInHours(),
                    onHourChange: { newHour in
                        checkInHour = newHour
                        validateCheckOutTime()
                    },
                    onMinuteChange: { newMinute in
                        checkInMinute = newMinute
                        validateCheckOutTime()
                    }
                )
                
                // Connector dots
                VStack(spacing: 3) {
                    ForEach(0..<3, id: \.self) { _ in
                        Circle()
                            .fill(Color.secondary.opacity(0.4))
                            .frame(width: 3, height: 3)
                    }
                }
                .padding(.top, 28)
                
                // Check-out
                TimeSlotCard(
                    label: "CHECK-OUT",
                    hour: $checkOutHour,
                    minute: $checkOutMinute,
                    color: .orange,
                    icon: "arrow.up.circle.fill",
                    hours: getCheckOutHours(),
                    onHourChange: { newHour in
                        checkOutHour = newHour
                    },
                    onMinuteChange: { newMinute in
                        checkOutMinute = newMinute
                    }
                )
            }
            
            // Duration display
            VStack(spacing: 10) {
                HStack {
                    Text("Duration")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .fontWeight(.semibold)
                        .textCase(.uppercase)
                    
                    Spacer()
                    
                    HStack(spacing: 6) {
                        Image(systemName: "clock.fill")
                            .font(.caption)
                            .foregroundColor(.blue)
                        Text(formatDuration())
                            .font(.callout)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                    }
                }
                
                // Progress bar
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.secondary.opacity(colorScheme == .dark ? 0.2 : 0.15))
                            .frame(height: 5)
                        
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [.green, .blue, .orange],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(
                                width: min(geometry.size.width * CGFloat(calculateDurationInMinutes()) / 720, geometry.size.width),
                                height: 5
                            )
                            .animation(.spring(response: 0.4, dampingFraction: 0.7), value: checkOutHour)
                            .animation(.spring(response: 0.4, dampingFraction: 0.7), value: checkOutMinute)
                    }
                }
                .frame(height: 5)
            }
            .padding(.horizontal, 20)
            .padding(.top, 4)
        }
        .padding(.vertical, 24)
        .padding(.horizontal, 20)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(colorScheme == .dark ? Color(.systemGray6).opacity(0.5) : Color(.systemGray6))
                .shadow(color: .black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 12, x: 0, y: 4)
        )
        .onAppear {
            // Initialize with valid hours on appear
            let validCheckInHours = getCheckInHours()
            if !validCheckInHours.contains(checkInHour) {
                checkInHour = validCheckInHours.first ?? 8
            }
            
            let validCheckOutHours = getCheckOutHours()
            if !validCheckOutHours.contains(checkOutHour) {
                checkOutHour = validCheckOutHours.first ?? 9
            }
            
            validateCheckOutTime()
        }
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM yyyy"
        return formatter.string(from: date)
    }
    
    private func validateCheckOutTime() {
        let checkInMinutes = checkInHour * 60 + checkInMinute
        var checkOutMinutes = checkOutHour * 60 + checkOutMinute
        
        if checkOutMinutes < checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
            checkOutMinutes = checkInMinutes + 60
            checkOutHour = min(checkOutMinutes / 60, 20)
            checkOutMinute = checkOutMinutes % 60
            
            if checkOutMinute != 0 && checkOutMinute != 30 {
                checkOutMinute = (checkOutMinute < 30) ? 0 : 30
            }
        }
    }
    
    // ✅ FIXED: Use bookingDate instead of Date()
    private func getCheckInHours() -> [Int] {
        let calendar = Calendar.current
        let now = Date()
        
        // Check if booking date is today
        let isToday = calendar.isDate(bookingDate, inSameDayAs: now)
        
        if isToday {
            let currentHour = calendar.component(.hour, from: now)
            let currentMinute = calendar.component(.minute, from: now)
            
            // ✅ Calculate minimum available hour based on current time + buffer
            let minHour: Int
            if currentMinute >= 25 {
                // If past :25 minutes, next available slot is next hour
                // Example: 19:06 -> can still book 19:30, so minHour = 19
                //          19:26 -> can't book 19:30, so minHour = 20
                minHour = max(currentHour + 1, 8)
            } else {
                // Before :25, current hour is still available
                minHour = max(currentHour, 8)
            }
            
            // If it's past 7:25 PM (19:25), no slots available for today
            guard minHour <= 19 else {
                return [8] // Return 8 AM as fallback (will be for tomorrow)
            }
            
            print("📊 Available check-in hours: \(Array(minHour...19))")
            return Array(minHour...19)
        } else {
            // If booking for future date, show full range 8 AM - 7 PM
            return Array(8...19)
        }
    }

    private func getCheckOutHours() -> [Int] {
        return Array(9...20)
    }
    
    private func calculateDurationInMinutes() -> Int {
        let checkInMinutes = checkInHour * 60 + checkInMinute
        let checkOutMinutes = checkOutHour * 60 + checkOutMinute
        return max(checkOutMinutes - checkInMinutes, 0)
    }
    
    private func formatDuration() -> String {
        let totalMinutes = calculateDurationInMinutes()
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        
        if minutes == 0 {
            return "\(hours)h"
        } else {
            return "\(hours)h \(minutes)m"
        }
    }
}

// MARK: - Time Slot Card
// MARK: - Time Slot Card
struct TimeSlotCard: View {
    let label: String
    @Binding var hour: Int
    @Binding var minute: Int
    let color: Color
    let icon: String
    let hours: [Int]
    let onHourChange: (Int) -> Void
    let onMinuteChange: (Int) -> Void
    
    @Environment(\.colorScheme) var colorScheme
    
    // ✅ NEW: Dynamic minute calculation
    private var availableMinutes: [Int] {
        // Check if this is a check-in picker for today's current hour
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        let currentMinute = calendar.component(.minute, from: now)
        
        // If selected hour is the current hour (for today's bookings)
        if hour == currentHour && label == "CHECK-IN" {
            // If current minute is 0-25, allow both :00 and :30
            if currentMinute < 25 {
                return [0, 30]
            }
            // If current minute is 26-54, only allow :30
            else if currentMinute < 55 {
                return [30]
            }
            // If current minute is 55-59, no valid slots in this hour
            else {
                return [0] // Will trigger hour increment
            }
        }
        
        // For all other cases, show both options
        return [0, 30]
    }
    
    var body: some View {
        VStack(spacing: 10) {
            // Label
            HStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 10))
                    .foregroundColor(color)
                Text(label)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(color)
                    .tracking(0.5)
            }
            
            // Time picker
            HStack(spacing: 4) {
                // Hour picker
                Picker("", selection: $hour) {
                    ForEach(hours, id: \.self) { h in
                        Text(String(format: "%02d", h))
                            .font(.body)
                            .tag(h)
                    }
                }
                .pickerStyle(.wheel)
                .frame(width: 60, height: 100)
                .clipped()
                .onChange(of: hour) { _, newValue in
                    // ✅ When hour changes, validate minute selection
                    if !availableMinutes.contains(minute) {
                        minute = availableMinutes.first ?? 0
                    }
                    onHourChange(newValue)
                }
                
                Text(":")
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .padding(.bottom, 4)
                
                // Minute picker
                Picker("", selection: $minute) {
                    ForEach(availableMinutes, id: \.self) { m in
                        Text(String(format: "%02d", m))
                            .font(.body)
                            .tag(m)
                    }
                }
                .pickerStyle(.wheel)
                .frame(width: 45, height: 100)
                .clipped()
                .onChange(of: minute) { _, newValue in
                    onMinuteChange(newValue)
                }
            }
            .frame(width: 120)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(colorScheme == .dark ? Color(.systemGray5) : Color(.systemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(
                        LinearGradient(
                            colors: [color.opacity(0.6), color.opacity(0.2)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 2
                    )
            )
            .shadow(color: color.opacity(0.15), radius: 8, x: 0, y: 2)
        }
        .onAppear {
            // Validate hour on appear
            if !hours.contains(hour) {
                hour = hours.first ?? 8
            }
            
            // ✅ Validate minute on appear
            if !availableMinutes.contains(minute) {
                minute = availableMinutes.first ?? 0
            }
        }
    }
}
