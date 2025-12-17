import SwiftUI

struct HourWheelPicker: View {
    @Binding var checkInHour: Int
    @Binding var checkInMinute: Int
    @Binding var checkOutHour: Int
    @Binding var checkOutMinute: Int
    
    @Environment(\.colorScheme) var colorScheme
    
    // Time slots in 30-minute intervals
    private let minuteOptions = [0, 30]
    
    var body: some View {
        VStack(spacing: 20) {
            // Date badge
            HStack(spacing: 8) {
                Image(systemName: "calendar.circle.fill")
                    .font(.body)
                    .foregroundColor(.blue)
                Text("10 Oct 2025")
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
                    hour: checkInHour,
                    minute: checkInMinute,
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
                    hour: checkOutHour,
                    minute: checkOutMinute,
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
    }
    
    // Validate minimum 1-hour duration
//    private func validateCheckOutTime() {
//        let checkInMinutes = checkInHour * 60 + checkInMinute
//        var checkOutMinutes = checkOutHour * 60 + checkOutMinute
//        
//        // Ensure at least 60 minutes gap
//        if checkOutMinutes <= checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
//            checkOutMinutes = checkInMinutes + 60
//            checkOutHour = min(checkOutMinutes / 60, 20)
//            checkOutMinute = checkOutMinutes % 60
//            
//            // Snap to valid minute (0 or 30)
//            if checkOutMinute != 0 && checkOutMinute != 30 {
//                checkOutMinute = 30
//            }
//        }
//    }
    
    // ✅ FIXED: Validate minimum 1-hour duration
    private func validateCheckOutTime() {
        let checkInMinutes = checkInHour * 60 + checkInMinute
        var checkOutMinutes = checkOutHour * 60 + checkOutMinute
        
        // ✅ FIXED: Changed from <= to < (allow exactly 60 minutes)
        if checkOutMinutes < checkInMinutes || (checkOutMinutes - checkInMinutes) < 60 {
            checkOutMinutes = checkInMinutes + 60
            checkOutHour = min(checkOutMinutes / 60, 20)
            checkOutMinute = checkOutMinutes % 60
            
            // Snap to valid minute (0 or 30)
            if checkOutMinute != 0 && checkOutMinute != 30 {
                checkOutMinute = 30
            }
        }
    }

    
    private func getCheckInHours() -> [Int] {
        let calendar = Calendar.current
        let currentHour = calendar.component(.hour, from: Date())
        let startHour = max(currentHour, 8)
        return Array(startHour...19)
    }
    
    private func getCheckOutHours() -> [Int] {
        return Array(8...20)
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

// MARK: - Time Slot Card with Hour and Minute Pickers
struct TimeSlotCard: View {
    let label: String
    let hour: Int
    let minute: Int
    let color: Color
    let icon: String
    let hours: [Int]
    let onHourChange: (Int) -> Void
    let onMinuteChange: (Int) -> Void
    
    @Environment(\.colorScheme) var colorScheme
    @State private var selectedHour: Int
    @State private var selectedMinute: Int
    
    private let minutes = [0, 30]
    
    init(label: String, hour: Int, minute: Int, color: Color, icon: String, hours: [Int], onHourChange: @escaping (Int) -> Void, onMinuteChange: @escaping (Int) -> Void) {
        self.label = label
        self.hour = hour
        self.minute = minute
        self.color = color
        self.icon = icon
        self.hours = hours
        self.onHourChange = onHourChange
        self.onMinuteChange = onMinuteChange
        _selectedHour = State(initialValue: hour)
        _selectedMinute = State(initialValue: minute)
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
                // Hour picker - ✅ 24-HOUR FORMAT
                Picker("", selection: $selectedHour) {
                    ForEach(hours, id: \.self) { h in
                        Text(String(format: "%02d", h))  // ✅ CHANGED: Shows 08, 09, 10, etc.
                            .font(.body)
                            .tag(h)
                    }
                }
                .pickerStyle(.wheel)
                .frame(width: 60, height: 100)
                .clipped()
                .onChange(of: selectedHour) { _, newValue in
                    onHourChange(newValue)
                }
                
                Text(":")
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .padding(.bottom, 4)
                
                // Minute picker
                Picker("", selection: $selectedMinute) {
                    ForEach(minutes, id: \.self) { m in
                        Text(String(format: "%02d", m))
                            .font(.body)
                            .tag(m)
                    }
                }
                .pickerStyle(.wheel)
                .frame(width: 45, height: 100)
                .clipped()
                .onChange(of: selectedMinute) { _, newValue in
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
    }
}
