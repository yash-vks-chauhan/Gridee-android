//
//import SwiftUI
//
//struct ProfileOptionRow: View {
//    let icon: String
//    let title: String
//    let subtitle: String
//    let action: () -> Void
//    
//    var body: some View {
//        Button(action: action) {
//            HStack(spacing: 16) {
//                OptionIcon(icon: icon)
//                OptionContent(title: title, subtitle: subtitle)
//                Spacer()
//                ChevronIcon()
//            }
//            .padding(.vertical, 8)
//            .contentShape(Rectangle())
//        }
//        .buttonStyle(PlainButtonStyle())
//    }
//}
//
//struct OptionIcon: View {
//    let icon: String
//    
//    var body: some View {
//        Circle()
//            .fill(Color.gray.opacity(0.1))
//            .frame(width: 40, height: 40)
//            .overlay(
//                Image(systemName: icon)
//                    .font(.system(size: 18, weight: .medium))
//                    .foregroundColor(.gray)
//            )
//    }
//}
//
//struct OptionContent: View {
//    let title: String
//    let subtitle: String
//    
//    var body: some View {
//        VStack(alignment: .leading, spacing: 2) {
//            Text(title)
//                .font(.body)
//                .fontWeight(.medium)
//                .foregroundColor(.primary)
//                .multilineTextAlignment(.leading)
//            Text(subtitle)
//                .font(.caption)
//                .foregroundColor(.gray)
//                .multilineTextAlignment(.leading)
//        }
//    }
//}
//
//struct ChevronIcon: View {
//    var body: some View {
//        Image(systemName: "chevron.right")
//            .font(.system(size: 14, weight: .medium))
//            .foregroundColor(.secondary)
//    }
//}


import SwiftUI

struct ProfileOptionRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                OptionIcon(icon: icon)
                OptionContent(title: title, subtitle: subtitle)
                Spacer()
                ChevronIcon()
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct OptionIcon: View {
    let icon: String
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        Circle()
            .fill(iconBackgroundColor)
            .frame(width: 40, height: 40)
            .overlay(
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(iconColor)
            )
    }
    
    // ✅ Adaptive icon background
    private var iconBackgroundColor: Color {
        colorScheme == .dark ? Color.gray.opacity(0.2) : Color.gray.opacity(0.1)
    }
    
    // ✅ Adaptive icon color
    private var iconColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.8) : Color.gray
    }
}

struct OptionContent: View {
    let title: String
    let subtitle: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.body)
                .fontWeight(.medium)
                .foregroundColor(.primary) // ✅ Adaptive text
                .multilineTextAlignment(.leading)
            Text(subtitle)
                .font(.caption)
                .foregroundColor(.secondary) // ✅ Adaptive subtitle
                .multilineTextAlignment(.leading)
        }
    }
}

struct ChevronIcon: View {
    var body: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(.secondary) // ✅ Adaptive chevron
    }
}
