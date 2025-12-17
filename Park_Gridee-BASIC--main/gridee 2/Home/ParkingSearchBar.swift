//import SwiftUI
//
//struct ParkingSearchBar: View {
//    @Binding var searchText: String
//    @Binding var isSearching: Bool
//    
//    @FocusState private var isSearchFieldFocused: Bool
//    @State private var showCancelButton: Bool = false
//    
//    var body: some View {
//        HStack(spacing: 0) {
//            // ✅ FIXED: Properly sized search field container
//            HStack(spacing: 10) {
//                // Search Icon - smaller and more refined
//                Image(systemName: "magnifyingglass")
//                    .font(.system(size: 16, weight: .medium))
////                    .foregroundColor(.secondary)
//                    .foregroundColor(AppColors.secondaryText) 
//                
//                // Search TextField - better sizing
//                TextField("Search parking locations...", text: $searchText)
//                    .font(.system(size: 15))
//                    .foregroundColor(AppColors.primaryText)
//                    .textFieldStyle(PlainTextFieldStyle())
//                    .focused($isSearchFieldFocused)
//                    .submitLabel(.search)
//                    .onSubmit {
//                        performSearch()
//                    }
//                
//                // Clear Button - properly sized
//                if !searchText.isEmpty {
//                    Button(action: {
//                        withAnimation(.easeInOut(duration: 0.2)) {
//                            searchText = ""
//                        }
//                    }) {
//                        Image(systemName: "xmark.circle.fill")
//                            .font(.system(size: 16))
//                            .foregroundColor(.secondary)
//                    }
//                    .transition(.scale.combined(with: .opacity))
//                }
//            }
//            .padding(.horizontal, 12)
//            .padding(.vertical, 10) // ✅ FIXED: Reduced from 12 to 10
//            .background(
//                RoundedRectangle(cornerRadius: 12) // ✅ FIXED: Reduced from 16 to 12
//                    .fill(Color(.systemGray6))
//                    .overlay(
//                        RoundedRectangle(cornerRadius: 12)
//                            .stroke(Color.clear, lineWidth: 0)
//                    )
//            )
//            .onTapGesture {
//                isSearchFieldFocused = true
//            }
//            
//            // ✅ Cancel Button (iOS style)
//            if showCancelButton {
//                Button("Cancel") {
//                    withAnimation(.easeInOut(duration: 0.3)) {
//                        searchText = ""
//                        isSearchFieldFocused = false
//                        isSearching = false
//                        showCancelButton = false
//                    }
//                }
//                .font(.system(size: 16))
//                .foregroundColor(.blue)
//                .padding(.leading, 10)
//                .transition(.move(edge: .trailing).combined(with: .opacity))
//            }
//        }
//        .padding(.horizontal, 16)
//        .padding(.vertical, 8) // ✅ FIXED: Added container padding
//        .onChange(of: isSearchFieldFocused) { focused in
//            withAnimation(.easeInOut(duration: 0.3)) {
//                showCancelButton = focused
//                isSearching = focused || !searchText.isEmpty
//            }
//        }
//        .onChange(of: searchText) { text in
//            isSearching = !text.isEmpty || isSearchFieldFocused
//        }
//    }
//    
//    private func performSearch() {
//        isSearchFieldFocused = false
//    }
//}
//
//// ✅ FIXED: Enhanced Search Results with better spacing
//struct ParkingSearchResults: View {
//    let searchText: String
//    let parkingSpots: [ParkingSpot]
//    let onBookTap: (ParkingSpot) -> Void
//    
//    var filteredSpots: [ParkingSpot] {
//        if searchText.isEmpty {
//            return []
//        }
//        
//        return parkingSpots.filter { spot in
//            let locationName = getLocationName(for: spot.id).lowercased()
//            let searchLower = searchText.lowercased()
//            
//            return locationName.contains(searchLower) ||
//                   spot.id.lowercased().contains(searchLower) ||
//                   (spot.available != nil && String(spot.available!).contains(searchText))
//        }
//    }
//    
//    var body: some View {
//        VStack(alignment: .leading, spacing: 16) {
//            if !searchText.isEmpty {
//                // ✅ FIXED: Compact search header
//                HStack {
//                    Text("Results for \"\(searchText)\"")
//                        .font(.subheadline)
//                        .fontWeight(.medium)
//                        .foregroundColor(.secondary)
//                    
//                    Spacer()
//                    
//                    Text("\(filteredSpots.count) found")
//                        .font(.caption)
//                        .fontWeight(.semibold)
//                        .foregroundColor(.green)
//                        .padding(.horizontal, 8)
//                        .padding(.vertical, 3)
//                        .background(Color.green.opacity(0.1))
//                        .clipShape(Capsule())
//                }
//                .padding(.horizontal, 16)
//                .padding(.top, 8)
//                
//                if filteredSpots.isEmpty {
//                    // ✅ FIXED: Compact no results state
//                    VStack(spacing: 16) {
//                        Image(systemName: "magnifyingglass")
//                            .font(.system(size: 40))
//                            .foregroundColor(.secondary)
//                        
//                        VStack(spacing: 8) {
//                            Text("No parking locations found")
//                                .font(.headline)
//                                .fontWeight(.semibold)
//                            
//                            Text("Try \"TP Avenue\" or \"Medical College\"")
//                                .font(.subheadline)
//                                .foregroundColor(.secondary)
//                                .multilineTextAlignment(.center)
//                        }
//                    }
//                    .frame(maxWidth: .infinity)
//                    .padding(.vertical, 32)
//                    .padding(.horizontal, 24)
//                } else {
//                    // ✅ FIXED: Compact search results
//                    LazyVStack(spacing: 12) {
//                        ForEach(filteredSpots, id: \.id) { spot in
//                            SearchResultCard(
//                                parkingSpot: spot,
//                                searchText: searchText,
//                                onBookTap: { onBookTap(spot) }
//                            )
//                        }
//                    }
//                    .padding(.horizontal, 16)
//                }
//            }
//        }
//    }
//    
//    private func getLocationName(for spotId: String) -> String {
//        switch spotId {
//        case "ps1":
//            return "TP Avenue Parking"
//        case "ps2":
//            return "Medical College"
//        case "ps3":
//            return "City Center Parking"
//        default:
//            return "Parking Location"
//        }
//    }
//}
//
//// ✅ FIXED: Compact Search Result Card matching your exact style
//struct SearchResultCard: View {
//    let parkingSpot: ParkingSpot
//    let searchText: String
//    let onBookTap: () -> Void
//    
//    var body: some View {
//        HStack(spacing: 16) {
//            // ✅ Parking Icon (exactly matches your cards)
//            ZStack {
//                Circle()
//                    .fill(.green)
//                    .frame(width: 50, height: 50)
//                
//                Text("P")
//                    .font(.title2)
//                    .fontWeight(.bold)
//                    .foregroundColor(.white)
//            }
//            
//            // ✅ Parking Info (exactly matches your layout)
//            VStack(alignment: .leading, spacing: 4) {
//                Text(getLocationName())
//                    .font(.title3)
//                    .fontWeight(.bold)
//                    .foregroundColor(.primary)
//                
//                Text("\(parkingSpot.available ?? 0) Available")
//                    .font(.subheadline)
//                    .fontWeight(.semibold)
//                    .foregroundColor(.green)
//                
//                Text("Capacity: \(parkingSpot.capacity ?? 0) • ₹2.5/hour")
//                    .font(.caption)
//                    .foregroundColor(.secondary)
//            }
//            
//            Spacer()
//            
//            // ✅ Book Button (exactly matches your style)
//            Button("Book") {
//                onBookTap()
//            }
//            .font(.subheadline)
//            .fontWeight(.semibold)
//            .foregroundColor(.white)
//            .padding(.horizontal, 24)
//            .padding(.vertical, 10)
//            .background(.black)
//            .clipShape(RoundedRectangle(cornerRadius: 20))
//        }
//        .padding(16) // ✅ FIXED: Matches your card padding
//        .background(
//            RoundedRectangle(cornerRadius: 16)
//                .fill(Color.white)
//                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
//        )
//    }
//    
//    private func getLocationName() -> String {
//        switch parkingSpot.id {
//        case "ps1":
//            return "TP Avenue Parking"
//        case "ps2":
//            return "Medical College"
//        case "ps3":
//            return "City Center Parking"
//        default:
//            return "Parking Location"
//        }
//    }
//}
//
//// ✅ Preview
//struct ParkingSearchBar_Previews: PreviewProvider {
//    static var previews: some View {
//        VStack {
//            ParkingSearchBar(
//                searchText: .constant(""),
//                isSearching: .constant(false)
//            )
//            
//            Spacer()
//        }
//        .padding()
//        .background(Color(.systemGroupedBackground))
//    }
//}


import SwiftUI

struct ParkingSearchBar: View {
    @Environment(\.colorScheme) var colorScheme
    @Binding var searchText: String
    @Binding var isSearching: Bool
    
    @FocusState private var isSearchFieldFocused: Bool
    @State private var showCancelButton: Bool = false
    
    var body: some View {
        HStack(spacing: 0) {
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(AppColors.secondaryText)
                
                TextField("Search parking locations...", text: $searchText)
                    .font(.system(size: 15))
                    .foregroundColor(AppColors.primaryText)
                    .textFieldStyle(PlainTextFieldStyle())
                    .focused($isSearchFieldFocused)
                    .submitLabel(.search)
                    .onSubmit {
                        performSearch()
                    }
                
                if !searchText.isEmpty {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            searchText = ""
                        }
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 16))
                            .foregroundColor(AppColors.secondaryText)
                    }
                    .transition(.scale.combined(with: .opacity))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(AppColors.searchBarBackground)
            )
            .onTapGesture {
                isSearchFieldFocused = true
            }
            
            if showCancelButton {
                Button("Cancel") {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        searchText = ""
                        isSearchFieldFocused = false
                        isSearching = false
                        showCancelButton = false
                    }
                }
                .font(.system(size: 16))
                .foregroundColor(.blue)
                .padding(.leading, 10)
                .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .onChange(of: isSearchFieldFocused) { _, focused in
            withAnimation(.easeInOut(duration: 0.3)) {
                showCancelButton = focused
                isSearching = focused || !searchText.isEmpty
            }
        }
        .onChange(of: searchText) { _, text in
            isSearching = !text.isEmpty || isSearchFieldFocused
        }
    }
    
    private func performSearch() {
        isSearchFieldFocused = false
    }
}

struct ParkingSearchResults: View {
    @Environment(\.colorScheme) var colorScheme
    let searchText: String
    let parkingSpots: [ParkingSpot]
    let onBookTap: (ParkingSpot) -> Void
    
    var filteredSpots: [ParkingSpot] {
        if searchText.isEmpty {
            return []
        }
        
        return parkingSpots.filter { spot in
            let locationName = getLocationName(for: spot.id).lowercased()
            let searchLower = searchText.lowercased()
            
            return locationName.contains(searchLower) ||
                   spot.id.lowercased().contains(searchLower) ||
                   (spot.available != nil && String(spot.available!).contains(searchText))
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !searchText.isEmpty {
                HStack {
                    Text("Results for \"\(searchText)\"")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(AppColors.secondaryText)
                    
                    Spacer()
                    
                    Text("\(filteredSpots.count) found")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.green)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.green.opacity(0.1))
                        .clipShape(Capsule())
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                
                if filteredSpots.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 40))
                            .foregroundColor(AppColors.secondaryText)
                        
                        VStack(spacing: 8) {
                            Text("No parking locations found")
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(AppColors.primaryText)
                            
                            Text("Try \"TP Avenue\" or \"Medical College\"")
                                .font(.subheadline)
                                .foregroundColor(AppColors.secondaryText)
                                .multilineTextAlignment(.center)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                    .padding(.horizontal, 24)
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(filteredSpots, id: \.id) { spot in
                            SearchResultCard(
                                parkingSpot: spot,
                                searchText: searchText,
                                onBookTap: { onBookTap(spot) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }
        }
    }
    
    private func getLocationName(for spotId: String) -> String {
        switch spotId {
        case "ps1": return "TP Avenue Parking"
        case "ps2": return "Medical College"
        case "ps3": return "City Center Parking"
        default: return "Parking Location"
        }
    }
}

struct SearchResultCard: View {
    @Environment(\.colorScheme) var colorScheme
    let parkingSpot: ParkingSpot
    let searchText: String
    let onBookTap: () -> Void
    
    private var cardBackground: Color {
        colorScheme == .dark ? AppColors.cardBackground : .white
    }
    
    private var circleBackground: Color {
        colorScheme == .dark ? Color(UIColor.tertiarySystemBackground) : .black
    }
    
    private var buttonBackground: Color {
        colorScheme == .dark ? .white : .black
    }
    
    private var buttonText: Color {
        colorScheme == .dark ? .black : .white
    }
    
    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(circleBackground)
                    .frame(width: 50, height: 50)
                
                Text("P")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(getLocationName())
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.primaryText)
                
                Text("\(parkingSpot.available ?? 0) Available")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)
                
                Text("Capacity: \(parkingSpot.capacity ?? 0) • ₹2.5/hour")
                    .font(.caption)
                    .foregroundColor(AppColors.secondaryText)
            }
            
            Spacer()
            
            Button("Book") {
                onBookTap()
            }
            .font(.subheadline)
            .fontWeight(.semibold)
            .foregroundColor(buttonText)
            .padding(.horizontal, 24)
            .padding(.vertical, 10)
            .background(buttonBackground)
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(cardBackground)
                .shadow(
                    color: colorScheme == .dark ? .clear : .black.opacity(0.05),
                    radius: 8,
                    x: 0,
                    y: 2
                )
        )
    }
    
    private func getLocationName() -> String {
        switch parkingSpot.id {
        case "ps1": return "TP Avenue Parking"
        case "ps2": return "Medical College"
        case "ps3": return "City Center Parking"
        default: return "Parking Location"
        }
    }
}

struct ParkingSearchBar_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            ParkingSearchBar(
                searchText: .constant(""),
                isSearching: .constant(false)
            )
            
            Spacer()
        }
        .padding()
        .background(AppColors.primaryBackground)
    }
}
