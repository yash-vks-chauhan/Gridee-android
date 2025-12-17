

import SwiftUI

struct ProfileAvatarView: View {
    let user: Users?
    
    private var initials: String {
        guard let user = user else { return "RS" }
        let components = user.name.split(separator: " ")
        let firstInitial = components.first?.first ?? "R"
        let lastInitial = components.count > 1 ? String(components.last?.first ?? "S") : "S"
        return String(firstInitial) + String(lastInitial)
    }
    
    var body: some View {
        Circle()
            .fill(Color.black)
            .frame(width: 64, height: 64)
            .overlay(
                Text(initials)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            )
    }
}
