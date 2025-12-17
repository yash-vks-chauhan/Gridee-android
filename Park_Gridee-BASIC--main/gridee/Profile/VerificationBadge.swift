

import SwiftUI

struct VerificationBadge: View {
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "checkmark.circle.fill")
                .font(.caption)
                .foregroundColor(.green)
            Text("Verified")
                .font(.caption)
                .foregroundColor(.green)
   
        }
    }
}
