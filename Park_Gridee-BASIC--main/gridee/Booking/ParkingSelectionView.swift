////
////  ParkingSelectionView.swift
////  gridee
////
////  Created by admin85 on 13/09/25.
////
//import SwiftUI
//
//struct ParkingSelectionView: View {
//    @Environment(\.dismiss) private var dismiss
//    @StateObject private var viewModel = ParkingSelectionView()
//    @State private var searchText = ""
//    @State private var selectedParking: ParkingSpot?
//    @State private var showingCreateBooking = false
//    
//    var filteredParkings: [ParkingSpot] {
//        if searchText.isEmpty {
//            return viewModel.parkingSpots
//        } else {
//            return viewModel.parkingSpots.filter { spot in
//                (spot.zoneName?.lowercased().contains(searchText.lowercased()) ?? false) ||
//                spot.id.lowercased().contains(searchText.lowercased())
//            }
//        }
//    }
//    
//    var body: some View {
//        NavigationView {
//            VStack(spacing: 0) {
//                // MARK: - Search Bar
//                SearchBar(text: $searchText)
//                    .padding()
//                
//                // MARK: - Location Header
//                HStack {
//                    Image(systemName: "location.fill")
//                        .foregroundColor(.blue)
//                    Text("Parking Spots Near You")
//                        .font(.headline)
//                        .fontWeight(.semibold)
//                    Spacer()
//                    Text("\(filteredParkings.count) spots")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//                .padding(.horizontal)
//                .padding(.bottom, 8)
//                
//                // MARK: - Parking List
//                if viewModel.isLoading {
//                    LoadingParkingList()
//                } else if filteredParkings.isEmpty {
//                    EmptyParkingList(searchText: searchText)
//                } else {
//                    ScrollView {
//                        LazyVStack(spacing: 12) {
//                            ForEach(filteredParkings) { parking in
//                                ParkingSpotCard(
//                                    parking: parking,
//                                    onSelect: {
//                                        selectedParking = parking
//                                        showingCreateBooking = true
//                                    }
//                                )
//                            }
//                        }
//                        .padding()
//                    }
//                }
//                
//                Spacer()
//            }
//            .background(Color(UIColor.systemGroupedBackground))
//            .navigationTitle("Select Parking")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") {
//                        dismiss()
//                    }
//                    .foregroundColor(.red)
//                }
//            }
//        }
//        .sheet(isPresented: $showingCreateBooking) {
//            if let parking = selectedParking {
//                CreateBookingView(selectedParking: parking)
//            }
//        }
//        .onAppear {
//            viewModel.loadParkingSpots()
//        }
//    }
//}
//
//// MARK: - Search Bar Component
//struct SearchBar: View {
//    @Binding var text: String
//    
//    var body: some View {
//        HStack {
//            Image(systemName: "magnifyingglass")
//                .foregroundColor(.gray)
//            
//            TextField("Search parking spots...", text: $text)
//                .textFieldStyle(PlainTextFieldStyle())
//            
//            if !text.isEmpty {
//                Button(action: { text = "" }) {
//                    Image(systemName: "xmark.circle.fill")
//                        .foregroundColor(.gray)
//                }
//            }
//        }
//        .padding(.horizontal, 16)
//        .padding(.vertical, 12)
//        .background(Color(UIColor.systemGray6))
//        .cornerRadius(12)
//    }
//}
//
//// MARK: - Parking Spot Card
//struct ParkingSpotCard: View {
//    let parking: ParkingSpot
//    let onSelect: () -> Void
//    
//    private var availabilityColor: Color {
//        let available = parking.available ?? 0
//        if available > 10 { return .green }
//        else if available > 5 { return .orange }
//        else if available > 0 { return .red }
//        else { return .gray }
//    }
//    
//    private var pricePerHour: String {
//        // You can customize this based on your pricing logic
//        "₹2.5/hour"
//    }
//    
//    var body: some View {
//        Button(action: onSelect) {
//            VStack(spacing: 16) {
//                HStack(spacing: 16) {
//                    // Parking Image/Icon
//                    ZStack {
//                        RoundedRectangle(cornerRadius: 12)
//                            .fill(availabilityColor.opacity(0.1))
//                            .frame(width: 80, height: 80)
//                        
//                        VStack {
//                            Image(systemName: "parkingsign")
//                                .font(.system(size: 24, weight: .medium))
//                                .foregroundColor(availabilityColor)
//                            
//                            Text("\(parking.available ?? 0)")
//                                .font(.caption)
//                                .fontWeight(.bold)
//                                .foregroundColor(availabilityColor)
//                        }
//                    }
//                    
//                    // Parking Details
//                    VStack(alignment: .leading, spacing: 4) {
//                        Text(parking.zoneName ?? "Parking Zone \(parking.id.prefix(4))")
//                            .font(.headline)
//                            .fontWeight(.semibold)
//                            .foregroundColor(.primary)
//                            .multilineTextAlignment(.leading)
//                        
//                        HStack {
//                            Image(systemName: "car.fill")
//                                .font(.caption)
//                                .foregroundColor(.secondary)
//                            Text("\(parking.available ?? 0) available")
//                                .font(.subheadline)
//                                .foregroundColor(availabilityColor)
//                            
//                            Spacer()
//                            
//                            Text(pricePerHour)
//                                .font(.subheadline)
//                                .fontWeight(.semibold)
//                                .foregroundColor(.primary)
//                        }
//                        
//                        HStack {
//                            Image(systemName: "location.fill")
//                                .font(.caption)
//                                .foregroundColor(.blue)
//                            Text("0.5 km away")
//                                .font(.caption)
//                                .foregroundColor(.secondary)
//                            
//                            Spacer()
//                            
//                            Text("Capacity: \(parking.capacity ?? 0)")
//                                .font(.caption)
//                                .foregroundColor(.secondary)
//                        }
//                        
//                        // Availability Status
//                        HStack {
//                            Circle()
//                                .fill(availabilityColor)
//                                .frame(width: 8, height: 8)
//                            
//                            Text(availabilityStatus)
//                                .font(.caption)
//                                .foregroundColor(availabilityColor)
//                            
//                            Spacer()
//                        }
//                    }
//                    
//                    // Select Button
//                    VStack {
//                        Image(systemName: "chevron.right")
//                            .font(.system(size: 16, weight: .medium))
//                            .foregroundColor(.secondary)
//                        
//                        Spacer()
//                        
//                        Text("Select")
//                            .font(.caption)
//                            .fontWeight(.semibold)
//                            .foregroundColor(.blue)
//                            .padding(.horizontal, 12)
//                            .padding(.vertical, 6)
//                            .background(Color.blue.opacity(0.1))
//                            .cornerRadius(8)
//                    }
//                }
//            }
//            .padding()
//            .background(Color.white)
//            .cornerRadius(16)
//            .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
//        }
//        .buttonStyle(PlainButtonStyle())
//        .disabled((parking.available ?? 0) == 0)
//        .opacity((parking.available ?? 0) == 0 ? 0.6 : 1.0)
//    }
//    
//    private var availabilityStatus: String {
//        let available = parking.available ?? 0
//        if available > 10 { return "High Availability" }
//        else if available > 5 { return "Medium Availability" }
//        else if available > 0 { return "Low Availability" }
//        else { return "Full" }
//    }
//}
//
//// MARK: - Loading and Empty States
//struct LoadingParkingList: View {
//    var body: some View {
//        VStack(spacing: 20) {
//            ProgressView()
//                .scaleEffect(1.2)
//            
//            Text("Finding parking spots...")
//                .font(.subheadline)
//                .foregroundColor(.secondary)
//        }
//        .frame(maxWidth: .infinity, maxHeight: .infinity)
//        .padding()
//    }
//}
//
//struct EmptyParkingList: View {
//    let searchText: String
//    
//    var body: some View {
//        VStack(spacing: 20) {
//            Image(systemName: searchText.isEmpty ? "parkingsign" : "magnifyingglass")
//                .font(.system(size: 60))
//                .foregroundColor(.gray.opacity(0.5))
//            
//            Text(searchText.isEmpty ? "No Parking Spots Available" : "No Results Found")
//                .font(.title2)
//                .fontWeight(.medium)
//                .foregroundColor(.gray)
//            
//            Text(searchText.isEmpty ?
//                 "There are no parking spots available at the moment." :
//                 "Try searching with different keywords.")
//                .font(.subheadline)
//                .foregroundColor(.secondary)
//                .multilineTextAlignment(.center)
//        }
//        .frame(maxWidth: .infinity, maxHeight: .infinity)
//        .padding()
//    }
//}
//


//import SwiftUI
//import Foundation
//struct ParkingSelectionView: View {
//    @Environment(\.dismiss) private var dismiss
//    //    @StateObject private var viewModel = ParkingSelectionView()
//    // ✅ Fixed - use ViewModel, not View
//    @StateObject private var viewModel = ParkingSpot(from: viewModel.ObservableObject, )
//    
//    @State private var parkingSpots: [ParkingSpot] = []
//    @State private var isLoading = false
//    @State private var errorMessage = ""
//    @State private var searchText = ""
//    @State private var selectedParking: ParkingSpot?
//    @State private var showingCreateBooking = false
//    
//    // Replace your existing ParkingSelectionViewModel class with this
//    class ParkingSelectionViewModel: ObservableObject {
//        @Published var parkingSpots: [ParkingSpot] = []
//        @Published var isLoading = false
//        @Published var errorMessage = ""
//        
//        init() {
//            loadParkingSpots()
//        }
//        
//        // ✅ Make sure this method is public (no private keyword)
//        func loadParkingSpots() {
//            print("Loading parking spots...")
//            isLoading = true
//            
//            // Try API first, fall back to mock data
//            APIService.fetchParkingSpots { (spots: [ParkingSpot]?, error: Error?) in
//                DispatchQueue.main.async {
//                    self.isLoading = false
//                    
//                    if let spots = spots {
//                        print("✅ Fetched \(spots.count) parking spots from API")
//                        self.parkingSpots = spots.sorted { ($0.available ?? 0) > ($1.available ?? 0) }
//                    } else {
//                        print("❌ API failed, loading mock data: \(error?.localizedDescription ?? "Unknown error")")
//                        self.loadMockData()
//                    }
//                }
//            }
//        }
//        
//        private func loadMockData() {
//            parkingSpots = [
//                ParkingSpot(
//                    id: "1",
//                    lotId: "lot-1",
//                    zoneName: "TP Avenue Parking",
//                    capacity: 20,
//                    available: 15,
//                    location: "0.5 km away"
//                ),
//                ParkingSpot(
//                    id: "2",
//                    lotId: "lot-2",
//                    zoneName: "City Center Mall",
//                    capacity: 15,
//                    available: 8,
//                    location: "1.2 km away"
//                ),
//                ParkingSpot(
//                    id: "3",
//                    lotId: "lot-3",
//                    zoneName: "Metro Station Parking",
//                    capacity: 12,
//                    available: 3,
//                    location: "0.8 km away"
//                ),
//                ParkingSpot(
//                    id: "4",
//                    lotId: "lot-4",
//                    zoneName: "Beach Road Parking",
//                    capacity: 10,
//                    available: 0,
//                    location: "2.1 km away"
//                ),
//                ParkingSpot(
//                    id: "5",
//                    lotId: "lot-5",
//                    zoneName: "Business District",
//                    capacity: 18,
//                    available: 12,
//                    location: "1.5 km away"
//                )
//            ]
//        }
//    }
//    
//    var filteredParkings: [ParkingSpot] {
//        if searchText.isEmpty {
//            return parkingSpots
//        } else {
//            return parkingSpots.filter { spot in
//                (spot.zoneName?.lowercased().contains(searchText.lowercased()) ?? false) ||
//                spot.id.lowercased().contains(searchText.lowercased())
//            }
//        }
//    }
//    
//    var body: some View {
//        NavigationView {
//            VStack(spacing: 0) {
//                // MARK: - Search Bar
//                SearchBar(text: $searchText)
//                    .padding()
//                
//                // MARK: - Location Header
//                HStack {
//                    Image(systemName: "location.fill")
//                        .foregroundColor(.blue)
//                    Text("Parking Spots Near You")
//                        .font(.headline)
//                        .fontWeight(.semibold)
//                    Spacer()
//                    Text("\(filteredParkings.count) spots")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//                .padding(.horizontal)
//                .padding(.bottom, 8)
//                
//                // MARK: - Parking List
//                if isLoading {
//                    LoadingParkingList()
//                } else if filteredParkings.isEmpty {
//                    EmptyParkingList(searchText: searchText) {
//                        // Refresh action for empty state
//                        viewModel.loadParkingSpots()
//                    }
//                } else {
//                    ScrollView {
//                        LazyVStack(spacing: 12) {
//                            ForEach(filteredParkings) { parking in
//                                ParkingSpotCard(
//                                    parking: parking,
//                                    onSelect: {
//                                        selectedParking = parking
//                                        showingCreateBooking = true
//                                    }
//                                )
//                            }
//                        }
//                        .padding()
//                    }
//                }
//                
//                Spacer()
//            }
//            .background(Color(UIColor.systemGroupedBackground))
//            .navigationTitle("Select Parking")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") {
//                        dismiss()
//                    }
//                    .foregroundColor(.red)
//                }
//                
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    Button("Refresh") {
//                        viewModel.loadParkingSpots()
//                        
////                            .foregroundColor(.blue)
//                    }
//                }
////                .refreshable {
////                    viewModel.loadParkingSpots()
////                }
//            }
//            .sheet(isPresented: $showingCreateBooking) {
//                if let parking = selectedParking {
//                    CreateBookingView(selectedParking: parking)
//                }
//            }
//            .alert("Error", isPresented: .constant(!errorMessage.isEmpty)) {
//                Button("OK") {
//                    errorMessage = ""
//                }
//            } message: {
//                Text(errorMessage)
//            }
//            .onAppear {
//                viewModel.loadParkingSpots()
//            }
//        }
//    }
//    
//    // MARK: - Search Bar Component
//    struct SearchBar: View {
//        @Binding var text: String
//        
//        var body: some View {
//            HStack {
//                Image(systemName: "magnifyingglass")
//                    .foregroundColor(.gray)
//                
//                TextField("Search parking spots...", text: $text)
//                    .textFieldStyle(PlainTextFieldStyle())
//                    .autocorrectionDisabled()
//                
//                if !text.isEmpty {
//                    Button(action: { text = "" }) {
//                        Image(systemName: "xmark.circle.fill")
//                            .foregroundColor(.gray)
//                    }
//                }
//            }
//            .padding(.horizontal, 16)
//            .padding(.vertical, 12)
//            .background(Color(UIColor.systemGray6))
//            .cornerRadius(12)
//        }
//    }
//    
//    // MARK: - Parking Spot Card
//    struct ParkingSpotCard: View {
//        let parking: ParkingSpot
//        let onSelect: () -> Void
//        
//        private var availabilityColor: Color {
//            let available = parking.available ?? 0
//            if available > 10 { return .green }
//            else if available > 5 { return .orange }
//            else if available > 0 { return .red }
//            else { return .gray }
//        }
//        
//        private var pricePerHour: String {
//            if let price = parking.pricePerHour {
//                return "₹\(String(format: "%.1f", price))/hour"
//            }
//            return "₹2.5/hour"
//        }
//        
//        var body: some View {
//            Button(action: onSelect) {
//                VStack(spacing: 16) {
//                    HStack(spacing: 16) {
//                        // Parking Image/Icon
//                        ZStack {
//                            RoundedRectangle(cornerRadius: 12)
//                                .fill(availabilityColor.opacity(0.1))
//                                .frame(width: 80, height: 80)
//                            
//                            VStack(spacing: 4) {
//                                Image(systemName: "parkingsign")
//                                    .font(.system(size: 24, weight: .medium))
//                                    .foregroundColor(availabilityColor)
//                                
//                                Text("\(parking.available ?? 0)")
//                                    .font(.caption)
//                                    .fontWeight(.bold)
//                                    .foregroundColor(availabilityColor)
//                            }
//                        }
//                        
//                        // Parking Details
//                        VStack(alignment: .leading, spacing: 6) {
//                            Text(parking.zoneName ?? "Parking Zone \(parking.id.prefix(4))")
//                                .font(.headline)
//                                .fontWeight(.semibold)
//                                .foregroundColor(.primary)
//                                .multilineTextAlignment(.leading)
//                            
//                            HStack {
//                                Image(systemName: "car.fill")
//                                    .font(.caption)
//                                    .foregroundColor(.secondary)
//                                Text("\(parking.available ?? 0) available")
//                                    .font(.subheadline)
//                                    .foregroundColor(availabilityColor)
//                                
//                                Spacer()
//                                
//                                Text(pricePerHour)
//                                    .font(.subheadline)
//                                    .fontWeight(.semibold)
//                                    .foregroundColor(.primary)
//                            }
//                            
//                            HStack {
//                                Image(systemName: "location.fill")
//                                    .font(.caption)
//                                    .foregroundColor(.blue)
//                                Text(parking.location ?? "0.5 km away")
//                                    .font(.caption)
//                                    .foregroundColor(.secondary)
//                                
//                                Spacer()
//                                
//                                Text("Capacity: \(parking.capacity ?? 0)")
//                                    .font(.caption)
//                                    .foregroundColor(.secondary)
//                            }
//                            
//                            // Availability Status
//                            HStack {
//                                Circle()
//                                    .fill(availabilityColor)
//                                    .frame(width: 8, height: 8)
//                                
//                                Text(availabilityStatus)
//                                    .font(.caption)
//                                    .foregroundColor(availabilityColor)
//                                
//                                Spacer()
//                            }
//                        }
//                        
//                        // Select Button
//                        VStack(spacing: 8) {
//                            Image(systemName: "chevron.right")
//                                .font(.system(size: 16, weight: .medium))
//                                .foregroundColor(.secondary)
//                            
//                            Spacer()
//                            
//                            if (parking.available ?? 0) > 0 {
//                                Text("Select")
//                                    .font(.caption)
//                                    .fontWeight(.semibold)
//                                    .foregroundColor(.blue)
//                                    .padding(.horizontal, 12)
//                                    .padding(.vertical, 6)
//                                    .background(Color.blue.opacity(0.1))
//                                    .cornerRadius(8)
//                            } else {
//                                Text("Full")
//                                    .font(.caption)
//                                    .fontWeight(.semibold)
//                                    .foregroundColor(.white)
//                                    .padding(.horizontal, 12)
//                                    .padding(.vertical, 6)
//                                    .background(Color.gray)
//                                    .cornerRadius(8)
//                            }
//                        }
//                    }
//                }
//                .padding()
//                .background(Color.white)
//                .cornerRadius(16)
//                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
//            }
//            .buttonStyle(PlainButtonStyle())
//            .disabled((parking.available ?? 0) == 0)
//            .opacity((parking.available ?? 0) == 0 ? 0.6 : 1.0)
//        }
//        
//        private var availabilityStatus: String {
//            let available = parking.available ?? 0
//            if available > 10 { return "High Availability" }
//            else if available > 5 { return "Medium Availability" }
//            else if available > 0 { return "Low Availability" }
//            else { return "Full" }
//        }
//    }
//    
//    // MARK: - Loading and Empty States
//    struct LoadingParkingList: View {
//        var body: some View {
//            VStack(spacing: 20) {
//                ProgressView()
//                    .scaleEffect(1.2)
//                
//                Text("Finding parking spots...")
//                    .font(.subheadline)
//                    .foregroundColor(.secondary)
//            }
//            .frame(maxWidth: .infinity, maxHeight: .infinity)
//            .padding()
//        }
//    }
//    
//    struct EmptyParkingList: View {
//        let searchText: String
//        let onRefresh: () -> Void
//        
//        var body: some View {
//            VStack(spacing: 20) {
//                Image(systemName: searchText.isEmpty ? "parkingsign" : "magnifyingglass")
//                    .font(.system(size: 60))
//                    .foregroundColor(.gray.opacity(0.5))
//                
//                Text(searchText.isEmpty ? "No Parking Spots Available" : "No Results Found")
//                    .font(.title2)
//                    .fontWeight(.medium)
//                    .foregroundColor(.gray)
//                
//                Text(searchText.isEmpty ?
//                     "There are no parking spots available at the moment." :
//                        "Try searching with different keywords.")
//                .font(.subheadline)
//                .foregroundColor(.secondary)
//                .multilineTextAlignment(.center)
//                
//                if searchText.isEmpty {
//                    Button("Refresh") {
//                        onRefresh()
//                    }
//                    .font(.headline)
//                    .foregroundColor(.white)
//                    .padding()
//                    .background(Color.blue)
//                    .cornerRadius(12)
//                }
//            }
//            .frame(maxWidth: .infinity, maxHeight: .infinity)
//            .padding()
//        }
//    }
//    
//    // MARK: - Preview
//    struct ParkingSelectionView_Previews: PreviewProvider {
//        static var previews: some View {
//            ParkingSelectionView()
//        }
//    }
//}
//import SwiftUI
//
//struct ParkingSelectionView: View {
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @State private var selectedLot: String? = nil
//    @State private var selectedSpot: String? = nil
//    @State private var showingNextStep: Bool = false
//
//    var body: some View {
//        NavigationView {
//            Form {
//                Section(header: Text("Select Parking Lot")) {
//                    Picker("Parking Lot", selection: $selectedLot) {
//                        ForEach(homeViewModel.parkingLots, id: \.id) { lot in
//                            Text(lot.zoneName ?? lot.id)
//                                .tag(Optional(lot.id))
//                        }
//                    }
//                }
//                Section(header: Text("Select Parking Spot")) {
//                    Picker("Select Spot", selection: $selectedSpot) {
//                        if let lotId = selectedLot {
//                            ForEach(homeViewModel.parkingSpots.filter { $0.lotId == lotId }, id: \.id) { spot in
//                                Text(spot.zoneName ?? "Spot \(spot.id.prefix(4))")
//                                    .tag(Optional(spot.id))
//                            }
//                        } else {
//                            Text("Select a lot first")
//                        }
//                    }
//                }
//            }
//            .navigationTitle("Select Parking")
//            .toolbar {
//                ToolbarItem(placement: .confirmationAction) {
//                    Button("Next") {
//                        showingNextStep = true
//                    }
//                    .disabled(selectedLot == nil || selectedSpot == nil)
//                }
//                ToolbarItem(placement: .cancellationAction) {
//                    Button("Cancel") {
//                        dismiss()
//                    }
//                }
//            }
//            .sheet(isPresented: $showingNextStep) {
//                CreateBookingView(selectedParking: selectedParking)
//                    .environmentObject(homeViewModel)
//            }
//        }
//    }
//
//    // Finds the selected parking spot entity from the ViewModel data.
//    var selectedParking: ParkingSpot? {
//        guard let spotId = selectedSpot else { return nil }
//        return homeViewModel.parkingSpots.first { $0.id == spotId }
//    }
//}


//import SwiftUI
//
//struct ParkingSelectionView: View {
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @State private var selectedLot: String? = nil
//    @State private var selectedSpot: String? = nil
//    @State private var showingNextStep: Bool = false
//
//    // Pre-filtered spots based on selected lot
//    var filteredSpots: [ParkingSpot] {
//        if let lotId = selectedLot {
//            return homeViewModel.parkingSpots.filter { $0.lotId == lotId }
//        }
//        return []
//    }
//
//    var body: some View {
//        NavigationView {
//            Form {
//                Section(header: Text("Select Parking Lot")) {
//                    // Ensure unique lot IDs for the Picker options
//                    let uniqueLotIds = Array(Set(homeViewModel.parkingSpots.compactMap { $0.lotId })).sorted()
//                    Picker("Parking Lot", selection: $selectedLot) {
//                        ForEach(uniqueLotIds, id: \.self) { lotId in
//                            Text(lotId).tag(Optional(lotId))
//                        }
//                    }
//                }
//
//                Section(header: Text("Select Parking Spot")) {
//                    Picker("Select Spot", selection: $selectedSpot) {
//                        if filteredSpots.isEmpty {
//                            Text("Select a lot first")
//                        } else {
//                            ForEach(filteredSpots, id: \.id) { spot in
//                                Text(spot.zoneName ?? "Spot \(spot.id.prefix(4))")
//                                    .tag(Optional(spot.id))
//                            }
//                        }
//                    }
//                }
//            }
//            .navigationTitle("Select Parking")
//            .toolbar {
//                ToolbarItem(placement: .confirmationAction) {
//                    Button("Next") {
//                        showingNextStep = true
//                    }
//                    .disabled(selectedLot == nil || selectedSpot == nil)
//                }
//                ToolbarItem(placement: .cancellationAction) {
//                    Button("Cancel") {
//                        dismiss()
//                    }
//                }
//            }
//            .sheet(isPresented: $showingNextStep) {
//                CreateBookingView(selectedParking: selectedParking)
//                    .environmentObject(homeViewModel)
//            }
//        }
//    }
//
//    var selectedParking: ParkingSpot? {
//        guard let spotId = selectedSpot else { return nil }
//        return homeViewModel.parkingSpots.first { $0.id == spotId }
//    }
//}
