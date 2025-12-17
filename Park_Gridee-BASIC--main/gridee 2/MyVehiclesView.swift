////
////  MyVehiclesView.swift
////  gridee
////
////  Created by admin85 on 12/09/25.
////
//import SwiftUI
//
////struct MyVehiclesView: View {
////    @StateObject private var vehicleViewModel = VehicleViewModel()
////    @Environment(\.dismiss) private var dismiss
////    
////    var body: some View {
////        NavigationView {
////            VStack(spacing: 0) {
////                // HEADER
////                HStack {
////                    Button("Close") {
////                        dismiss()
////                    }
////                    .foregroundColor(.blue)
////                    
////                    Spacer()
////                    
////                    Text("My Vehicles")
////                        .font(.headline)
////                        .fontWeight(.semibold)
////                    
////                    Spacer()
////                    
////                    Button("Add") {
////                        vehicleViewModel.showingAddVehicle = true
////                    }
////                    .foregroundColor(.blue)
////                    .fontWeight(.semibold)
////                }
////                .padding()
////                .background(Color.white)
////                .shadow(color: .black.opacity(0.1), radius: 1, x: 0, y: 1)
////                
////                // VEHICLES LIST
////                if vehicleViewModel.isLoading && vehicleViewModel.vehicles.isEmpty {
////                    LoadingVehiclesView()
////                } else if vehicleViewModel.vehicles.isEmpty {
////                    EmptyVehiclesView {
////                        vehicleViewModel.showingAddVehicle = true
////                    }
////                } else {
////                    ScrollView {
////                        LazyVStack(spacing: 16) {
////                            ForEach(vehicleViewModel.vehicles) { vehicle in
////                                VehicleCard(
////                                    vehicle: vehicle,
////                                    onSetDefault: {
////                                        vehicleViewModel.setDefaultVehicle(vehicleId: vehicle.id)
////                                    },
////                                    onDelete: {
////                                        vehicleViewModel.deleteVehicle(vehicleId: vehicle.id)
////                                    }
////                                )
////                            }
////                        }
////                        .padding()
////                    }
////                }
////                
////                Spacer()
////            }
////            .background(Color(UIColor.systemGroupedBackground))
////            .sheet(isPresented: $vehicleViewModel.showingAddVehicle) {
////                AddVehicleView(vehicleViewModel: vehicleViewModel)
////            }
////            .alert("Error", isPresented: .constant(!vehicleViewModel.errorMessage.isEmpty)) {
////                Button("OK") {
////                    vehicleViewModel.errorMessage = ""
////                }
////            } message: {
////                Text(vehicleViewModel.errorMessage)
////            }
////        }
////        .onAppear {
////            vehicleViewModel.loadVehicles()
////        }
////    }
////}
////
////// MARK: - Loading & Empty States
////struct LoadingVehiclesView: View {
////    var body: some View {
////        VStack(spacing: 16) {
////            ProgressView()
////                .progressViewStyle(CircularProgressViewStyle())
////            Text("Loading your vehicles...")
////                .foregroundColor(.secondary)
////        }
////        .frame(maxWidth: .infinity, maxHeight: .infinity)
////    }
////}
////
////struct EmptyVehiclesView: View {
////    let onAddVehicle: () -> Void
////    
////    var body: some View {
////        VStack(spacing: 20) {
////            Image(systemName: "car.fill")
////                .font(.system(size: 60))
////                .foregroundColor(.gray)
////            
////            Text("No Vehicles Added")
////                .font(.title2)
////                .fontWeight(.semibold)
////            
////            Text("Add your first vehicle to start booking parking spots")
////                .font(.subheadline)
////                .foregroundColor(.secondary)
////                .multilineTextAlignment(.center)
////            
////            Button("Add Vehicle") {
////                onAddVehicle()
////            }
////            .font(.headline)
////            .foregroundColor(.white)
////            .padding()
////            .background(Color.blue)
////            .clipShape(RoundedRectangle(cornerRadius: 10))
////        }
////        .padding()
////        .frame(maxWidth: .infinity, maxHeight: .infinity)
////    }
////}
////
////// MARK: - Preview
////struct MyVehiclesView_Previews: PreviewProvider {
////    static var previews: some View {
////        MyVehiclesView()
////    }
////}
//// MARK: - Comprehensive Vehicle Management View
//struct MyVehiclesView: View {
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var vehicleViewModel: VehicleViewModel
//    
//    // MARK: - View State
//    @State private var showingAddVehicle = false
//    @State private var searchText = ""
//    @State private var selectedVehicle: Vehicle? = nil
//    @State private var showingDeleteConfirmation = false
//    @State private var vehicleToDelete: Vehicle? = nil
//    
//    var body: some View {
//        NavigationView {
//            VStack(spacing: 0) {
//                // MARK: - Main Content
//                if vehicleViewModel.isLoading && vehicleViewModel.vehicles.isEmpty {
//                    LoadingView()
//                } else if filteredVehicles.isEmpty && searchText.isEmpty {
//                    EmptyVehiclesView(showingAddVehicle: $showingAddVehicle)
//                } else {
//                    VehicleListContent()
//                }
//            }
//            .navigationTitle("My Vehicles")
//            .navigationBarTitleDisplayMode(.large)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Done") { dismiss() }
//                }
//                
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    Button(action: { showingAddVehicle = true }) {
//                        Image(systemName: "plus.circle.fill")
//                            .font(.title2)
//                    }
//                }
//            }
//            .searchable(text: $searchText, prompt: "Search vehicles...")
//            .sheet(isPresented: $showingAddVehicle) {
//                AddVehicleView()
//                    .environmentObject(vehicleViewModel)
//            }
//            .alert("Delete Vehicle", isPresented: $showingDeleteConfirmation, presenting: vehicleToDelete) { vehicle in
//                Button("Delete", role: .destructive) {
//                    vehicleViewModel.deleteVehicle(vehicleId: vehicle.id)
//                }
//                Button("Cancel", role: .cancel) { }
//            } message: { vehicle in
//                Text("Are you sure you want to delete \(vehicle.vehicleNumber)? This action cannot be undone.")
//            }
//            .alert("Error", isPresented: vehicleViewModel.errorBinding) {
//                Button("OK") { }
//                Button("Retry") {
//                    vehicleViewModel.retryLastOperation()
//                }
//            } message: {
//                Text(vehicleViewModel.errorMessage)
//            }
//        }
//        .onAppear {
//            vehicleViewModel.loadVehicles()
//        }
//    }
//    
//    // MARK: - Vehicle List Content
//    @ViewBuilder
//    private func VehicleListContent() -> some View {
//        List {
//            // MARK: - Summary Section
//            if !vehicleViewModel.vehicles.isEmpty {
//                Section {
//                    VehicleSummaryCard()
//                }
//                .listRowInsets(EdgeInsets())
//                .listRowBackground(Color.clear)
//            }
//            
//            // MARK: - Vehicles Section
//            Section {
//                if filteredVehicles.isEmpty {
//                    Text("No vehicles match your search")
//                        .foregroundColor(.secondary)
//                        .frame(maxWidth: .infinity, alignment: .center)
//                        .padding()
//                } else {
//                    ForEach(filteredVehicles, id: \.id) { vehicle in
//                        VehicleRowView(
//                            vehicle: vehicle,
//                            onSetDefault: { vehicleViewModel.setDefaultVehicle(vehicleId: vehicle.id) },
//                            onDelete: {
//                                vehicleToDelete = vehicle
//                                showingDeleteConfirmation = true
//                            },
//                            isLoading: vehicleViewModel.isLoading
//                        )
//                    }
//                }
//            } header: {
//                HStack {
//                    Text("Vehicles (\(filteredVehicles.count))")
//                    Spacer()
//                }
//            }
//        }
//        .listStyle(.insetGrouped)
//        .refreshable {
//            vehicleViewModel.loadVehicles()
//        }
//    }
//    
//    // MARK: - Filtered Vehicles
//    private var filteredVehicles: [Vehicle] {
//        if searchText.isEmpty {
//            return vehicleViewModel.vehicles.sorted { lhs, rhs in
//                if lhs.isDefault != rhs.isDefault {
//                    return lhs.isDefault
//                }
//                return lhs.vehicleNumber < rhs.vehicleNumber
//            }
//        } else {
//            return vehicleViewModel.searchVehicles(query: searchText)
//        }
//    }
//}
//
//// MARK: - Vehicle Summary Card
//struct VehicleSummaryCard: View {
//    @EnvironmentObject var vehicleViewModel: VehicleViewModel
//    
//    var body: some View {
//        VStack(spacing: 16) {
//            HStack {
//                VStack(alignment: .leading, spacing: 4) {
//                    Text("Total Vehicles")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                    Text("\(vehicleViewModel.vehicleCount)")
//                        .font(.title)
//                        .fontWeight(.bold)
//                }
//                
//                Spacer()
//                
//                if let defaultVehicle = vehicleViewModel.defaultVehicle {
//                    VStack(alignment: .trailing, spacing: 4) {
//                        Text("Default Vehicle")
//                            .font(.caption)
//                            .foregroundColor(.secondary)
//                        Text(defaultVehicle.vehicleNumber)
//                            .font(.subheadline)
//                            .fontWeight(.semibold)
//                    }
//                }
//            }
//            
//            // Quick Stats
//            HStack(spacing: 20) {
//                StatItem(
//                    count: vehicleViewModel.vehicles(ofType: "Car").count,
//                    label: "Cars",
//                    icon: "car.fill"
//                )
//                
//                StatItem(
//                    count: vehicleViewModel.vehicles(ofType: "Motorcycle").count,
//                    label: "Bikes",
//                    icon: "motorcycle.fill"
//                )
//                
//                StatItem(
//                    count: vehicleViewModel.vehicles(ofType: "Truck").count,
//                    label: "Trucks",
//                    icon: "truck.box.fill"
//                )
//                
//                Spacer()
//            }
//        }
//        .padding()
//        .background(Color.blue.opacity(0.1))
//        .cornerRadius(12)
//        .padding(.horizontal)
//    }
//}
//
//// MARK: - Stat Item for Summary
//struct StatItem: View {
//    let count: Int
//    let label: String
//    let icon: String
//    
//    var body: some View {
//        VStack(spacing: 4) {
//            HStack(spacing: 4) {
//                Image(systemName: icon)
//                    .font(.caption)
//                Text("\(count)")
//                    .font(.headline)
//                    .fontWeight(.semibold)
//            }
//            .foregroundColor(.blue)
//            
//            Text(label)
//                .font(.caption)
//                .foregroundColor(.secondary)
//        }
//    }
//}
//
//// MARK: - Enhanced Vehicle Row View
//struct VehicleRowView: View {
//    let vehicle: Vehicle
//    let onSetDefault: () -> Void
//    let onDelete: () -> Void
//    let isLoading: Bool
//    
//    var body: some View {
//        HStack(spacing: 16) {
//            // Vehicle Icon
//            Circle()
//                .fill(vehicleTypeColor.opacity(0.1))
//                .frame(width: 48, height: 48)
//                .overlay {
//                    Image(systemName: vehicleTypeIcon)
//                        .font(.title2)
//                        .foregroundColor(vehicleTypeColor)
//                }
//            
//            // Vehicle Details
//            VStack(alignment: .leading, spacing: 4) {
//                HStack {
//                    Text(vehicle.vehicleNumber)
//                        .font(.headline)
//                        .fontWeight(.semibold)
//                    
//                    if vehicle.isDefault {
//                        Text("DEFAULT")
//                            .font(.caption)
//                            .padding(.horizontal, 6)
//                            .padding(.vertical, 2)
//                            .background(Color.green.opacity(0.2))
//                            .foregroundColor(.green)
//                            .cornerRadius(4)
//                    }
//                }
//                
//                Text(vehicle.vehicleType)
//                    .font(.subheadline)
//                    .foregroundColor(.secondary)
//                
//                if let brand = vehicle.brand, let model = vehicle.model {
//                    Text("\(brand) \(model)")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//                
//                if let color = vehicle.color {
//                    Text("Color: \(color)")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//            }
//            
//            Spacer()
//            
//            // Action Buttons
//            VStack(spacing: 8) {
//                if !vehicle.isDefault {
//                    Button("Set Default") {
//                        onSetDefault()
//                    }
//                    .font(.caption)
//                    .foregroundColor(.blue)
//                    .disabled(isLoading)
//                }
//                
//                Button(action: onDelete) {
//                    Image(systemName: "trash")
//                        .font(.caption)
//                        .foregroundColor(.red)
//                }
//                .disabled(isLoading)
//            }
//        }
//        .padding(.vertical, 4)
//    }
//    
//    private var vehicleTypeIcon: String {
//        switch vehicle.vehicleType.lowercased() {
//        case "car": return "car.fill"
//        case "motorcycle": return "motorcycle.fill"
//        case "truck": return "truck.box.fill"
//        case "suv": return "suv.side.fill"
//        case "van": return "van.side.fill"
//        case "bus": return "bus.fill"
//        default: return "car.fill"
//        }
//    }
//    
//    private var vehicleTypeColor: Color {
//        switch vehicle.vehicleType.lowercased() {
//        case "car": return .blue
//        case "motorcycle": return .green
//        case "truck": return .orange
//        case "suv": return .purple
//        default: return .gray
//        }
//    }
//}
//
//// MARK: - Empty Vehicles View
//struct EmptyVehiclesView: View {
//    @Binding var showingAddVehicle: Bool
//    
//    var body: some View {
//        VStack(spacing: 24) {
//            // Illustration
//            Image(systemName: "car.2.fill")
//                .font(.system(size: 80))
//                .foregroundColor(.gray.opacity(0.5))
//            
//            // Text Content
//            VStack(spacing: 12) {
//                Text("No Vehicles Added")
//                    .font(.title2)
//                    .fontWeight(.semibold)
//                
//                Text("Add your first vehicle to start booking parking spots and manage your transportation.")
//                    .font(.body)
//                    .foregroundColor(.secondary)
//                    .multilineTextAlignment(.center)
//                    .padding(.horizontal)
//            }
//            
//            // Add Vehicle Button
//            Button(action: { showingAddVehicle = true }) {
//                HStack {
//                    Image(systemName: "plus")
//                    Text("Add Your First Vehicle")
//                }
//                .font(.headline)
//                .foregroundColor(.white)
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(Color.blue)
//                .cornerRadius(12)
//            }
//            .padding(.horizontal, 40)
//        }
//        .frame(maxWidth: .infinity, maxHeight: .infinity)
//        .background(Color(UIColor.systemGroupedBackground))
//    }
//}
//
//// MARK: - Loading View
//struct LoadingView: View {
//    var body: some View {
//        VStack(spacing: 16) {
//            ProgressView()
//                .scaleEffect(1.2)
//            Text("Loading vehicles...")
//                .font(.subheadline)
//                .foregroundColor(.secondary)
//        }
//        .frame(maxWidth: .infinity, maxHeight: .infinity)
//    }
//}
//
//
