//
//
//import SwiftUI
//
//struct MyVehiclesView: View {
//    @Environment(\.dismiss) private var dismiss
//    
//    @StateObject private var vehicleManager = VehicleManager()
//    @State private var showingVehicleEditor = false
//    @State private var vehicleEditorMode: VehicleEditorView.Mode = .add
//    @State private var editingIndex: Int? = nil
//    @State private var showingDeleteAlert = false
//    @State private var deleteIndex: Int? = nil
//
//    var body: some View {
//        NavigationView {
//            ZStack {
//                Color(UIColor.systemGroupedBackground)
//                    .ignoresSafeArea()
//                
//                VStack(spacing: 0) {
//                    if vehicleManager.vehicles.isEmpty {
//                        EmptyVehiclesView {
//                            vehicleEditorMode = .add
//                            editingIndex = nil
//                            showingVehicleEditor = true
//                        }
//                        .frame(maxWidth: .infinity, maxHeight: .infinity)
//                    } else {
//                        VehicleListView(
//                            vehicles: vehicleManager.vehicles,
//                            onEdit: { index in
//                                editingIndex = index
//                                vehicleEditorMode = .edit(existing: vehicleManager.vehicles[index])
//                                showingVehicleEditor = true
//                            },
//                            onDelete: { index in
//                                deleteIndex = index
//                                showingDeleteAlert = true
//                            }
//                        )
//                    }
//                    
//                    // Fixed bottom CTA
//                    AddVehicleCTA {
//                        vehicleEditorMode = .add
//                        editingIndex = nil
//                        showingVehicleEditor = true
//                    }
//                }
//            }
//            .navigationTitle("My Vehicles")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Close") { dismiss() }
//                        .foregroundColor(.primary)
//                }
//                
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    if !vehicleManager.vehicles.isEmpty {
//                        Button {
//                            vehicleEditorMode = .add
//                            editingIndex = nil
//                            showingVehicleEditor = true
//                        } label: {
//                            Image(systemName: "plus")
//                                .foregroundColor(.accentColor)
//                        }
//                    }
//                }
//            }
//            .sheet(isPresented: $showingVehicleEditor) {
//                VehicleEditorView(
//                    mode: vehicleEditorMode,
//                    existingVehicles: vehicleManager.vehicles
//                ) { result in
//                    handleVehicleSave(result)
//                } onCancel: {
//                    showingVehicleEditor = false
//                    editingIndex = nil
//                }
//            }
//            .alert("Delete Vehicle", isPresented: $showingDeleteAlert) {
//                Button("Cancel", role: .cancel) {
//                    deleteIndex = nil
//                }
//                Button("Delete", role: .destructive) {
//                    if let index = deleteIndex {
//                        withAnimation(.easeInOut(duration: 0.3)) {
//                            vehicleManager.removeVehicle(at: index)
//                        }
//                    }
//                    deleteIndex = nil
//                }
//            } message: {
//                Text("Are you sure you want to delete this vehicle? This action cannot be undone.")
//            }
//        }
//    }
//    
//    private func handleVehicleSave(_ result: VehicleEditorView.SaveResult) {
//        switch result {
//        case .success(let vehicle):
//            withAnimation(.easeInOut(duration: 0.3)) {
//                if case .edit = vehicleEditorMode, let idx = editingIndex {
//                    vehicleManager.updateVehicle(at: idx, with: vehicle)
//                } else {
//                    vehicleManager.addVehicle(vehicle)
//                }
//            }
//            editingIndex = nil
//            showingVehicleEditor = false
//        case .error(let message):
//            // Handle error case - could show an alert or toast
//            print("Error saving vehicle: \(message)")
//        }
//    }
//}
//
//// MARK: - Vehicle Manager
//class VehicleManager: ObservableObject {
//    @Published var vehicles: [Vehicle] = [
//        Vehicle(
//            id: UUID(),
//            registrationNumber: "KA01AB1234",
//            vehicleName: "Jupiter",
//            color: .blue
//        ),
//        Vehicle(
//            id: UUID(),
//            registrationNumber: "DL05XY7890",
//            vehicleName: "Royal Enfield",
//            color: .black
//        )
//    ]
//    
//    func addVehicle(_ vehicle: Vehicle) {
//        vehicles.append(vehicle)
//    }
//    
//    func updateVehicle(at index: Int, with vehicle: Vehicle) {
//        guard index < vehicles.count else { return }
//        vehicles[index] = vehicle
//    }
//    
//    func removeVehicle(at index: Int) {
//        guard index < vehicles.count else { return }
//        vehicles.remove(at: index)
//    }
//}
//
//// MARK: - Simplified Vehicle Model
//struct Vehicle: Identifiable, Equatable {
//    let id: UUID
//    var registrationNumber: String
//    var vehicleName: String
//    var color: Color
//    
//    static func == (lhs: Vehicle, rhs: Vehicle) -> Bool {
//        return lhs.id == rhs.id &&
//               lhs.registrationNumber == rhs.registrationNumber &&
//               lhs.vehicleName == rhs.vehicleName
//    }
//}
//
//// MARK: - Vehicle List View
//private struct VehicleListView: View {
//    let vehicles: [Vehicle]
//    let onEdit: (Int) -> Void
//    let onDelete: (Int) -> Void
//    
//    var body: some View {
//        List {
//            ForEach(Array(vehicles.enumerated()), id: \.element.id) { index, vehicle in
//                VehicleRowView(vehicle: vehicle)
//                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
//                        Button(role: .destructive) {
//                            onDelete(index)
//                        } label: {
//                            Label("Delete", systemImage: "trash")
//                        }
//                        
//                        Button {
//                            onEdit(index)
//                        } label: {
//                            Label("Edit", systemImage: "pencil")
//                        }
//                        .tint(.blue)
//                    }
//                    .contentShape(Rectangle())
//                    .onTapGesture {
//                        onEdit(index)
//                    }
//            }
//        }
//        .listStyle(.insetGrouped)
//    }
//}
//
//// MARK: - Empty State View
//private struct EmptyVehiclesView: View {
//    let onAdd: () -> Void
//
//    var body: some View {
//        VStack(spacing: 24) {
//            Spacer()
//            
//            VStack(spacing: 16) {
//                Image(systemName: "car.2.fill")
//                    .resizable()
//                    .scaledToFit()
//                    .frame(width: 80, height: 80)
//                    .foregroundStyle(.gray.opacity(0.6))
//                
//                VStack(spacing: 8) {
//                    Text("No Vehicles Added")
//                        .font(.title2)
//                        .fontWeight(.semibold)
//                        .foregroundColor(.primary)
//                    
//                    Text("Add your vehicle details to quickly select them when booking services.")
//                        .font(.subheadline)
//                        .multilineTextAlignment(.center)
//                        .foregroundColor(.secondary)
//                        .padding(.horizontal, 32)
//                }
//            }
//            
//            Spacer()
//            
//            Button(action: onAdd) {
//                HStack(spacing: 8) {
//                    Image(systemName: "plus.circle.fill")
//                    Text("Add Your First Vehicle")
//                        .fontWeight(.semibold)
//                }
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(Color.accentColor)
//                .foregroundColor(.white)
//                .cornerRadius(12)
//                .padding(.horizontal, 24)
//            }
//            
//            Spacer()
//        }
//    }
//}
//
//// MARK: - Simplified Vehicle Row View
//private struct VehicleRowView: View {
//    let vehicle: Vehicle
//
//    var body: some View {
//        HStack(spacing: 16) {
//            // Vehicle color indicator
//            ZStack {
//                RoundedRectangle(cornerRadius: 12)
//                    .fill(vehicle.color.opacity(0.2))
//                    .frame(width: 50, height: 50)
//                
//                RoundedRectangle(cornerRadius: 8)
//                    .fill(vehicle.color)
//                    .frame(width: 32, height: 32)
//                
//                Image(systemName: "car.fill")
//                    .foregroundColor(.white)
//                    .font(.caption)
//            }
//            
//            // Vehicle details
//            VStack(alignment: .leading, spacing: 4) {
//                Text(vehicle.registrationNumber)
//                    .font(.headline)
//                    .fontWeight(.semibold)
//                
//                Text(vehicle.vehicleName)
//                    .font(.subheadline)
//                    .fontWeight(.medium)
//                    .foregroundColor(.secondary)
//            }
//            
//            Spacer()
//            
//            Image(systemName: "chevron.right")
//                .foregroundColor(.secondary)
//                .font(.caption)
//        }
//        .padding(.vertical, 8)
//    }
//}
//
//// MARK: - Add Vehicle CTA
//private struct AddVehicleCTA: View {
//    let action: () -> Void
//    
//    var body: some View {
//        VStack(spacing: 0) {
//            Divider()
//            
//            Button(action: action) {
//                HStack(spacing: 8) {
//                    Image(systemName: "plus.circle.fill")
//                        .font(.title3)
//                    Text("Add New Vehicle")
//                        .fontWeight(.semibold)
//                }
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(Color.accentColor)
//                .foregroundColor(.white)
//                .cornerRadius(12)
//                .padding(.horizontal, 16)
//                .padding(.vertical, 12)
//            }
//        }
//        .background(Color(UIColor.systemGroupedBackground))
//    }
//}
//
//// MARK: - Simplified Vehicle Editor View
//private struct VehicleEditorView: View {
//    enum Mode: Equatable {
//        case add
//        case edit(existing: Vehicle)
//    }
//    
//    enum SaveResult {
//        case success(Vehicle)
//        case error(String)
//    }
//
//    let mode: Mode
//    let existingVehicles: [Vehicle]
//    let onSave: (SaveResult) -> Void
//    let onCancel: () -> Void
//
//    @State private var registrationNumber: String = ""
//    @State private var vehicleName: String = ""
//    @State private var selectedColor: Color = .blue
//    @State private var validationErrors: [String: String] = [:]
//    @FocusState private var focusedField: Field?
//    
//    enum Field: CaseIterable {
//        case registration, vehicleName
//    }
//
//    var body: some View {
//        NavigationView {
//            Form {
//                // Registration Number Section
//                Section {
//                    HStack {
//                        Image(systemName: "number.circle.fill")
//                            .foregroundColor(.accentColor)
//                            .frame(width: 24)
//                        
//                        TextField("e.g., KA01AB1234", text: $registrationNumber)
//                            .focused($focusedField, equals: .registration)
//                            .textInputAutocapitalization(.characters)
//                            .disableAutocorrection(true)
//                            .onSubmit { focusedField = .vehicleName }
//                            .onChange(of: registrationNumber) { _, _ in
//                                validateRegistrationNumber()
//                            }
//                    }
//                    
//                    if let error = validationErrors["registration"] {
//                        Text(error)
//                            .foregroundColor(.red)
//                            .font(.caption)
//                    }
//                } header: {
//                    Text("Registration Number *")
//                } footer: {
//                    Text("Enter your vehicle's registration number as it appears on your license plate")
//                }
//
//                // Vehicle Name Section
//                Section {
//                    HStack {
//                        Image(systemName: "car.fill")
//                            .foregroundColor(.secondary)
//                            .frame(width: 24)
//                        
//                        TextField("e.g., Yamaha FZ", text: $vehicleName)
//                            .focused($focusedField, equals: .vehicleName)
//                            .onSubmit { focusedField = nil }
//                            .onChange(of: vehicleName) { _, _ in
//                                validateVehicleName()
//                            }
//                    }
//                    
//                    if let error = validationErrors["vehicleName"] {
//                        Text(error)
//                            .foregroundColor(.red)
//                            .font(.caption)
//                    }
//                } header: {
//                    Text("Vehicle Name *")
//                } footer: {
//                    Text("Enter the brand and model name of your vehicle")
//                }
//
//                // Color Section
//                Section {
//                    HStack {
//                        Image(systemName: "paintbrush.fill")
//                            .foregroundColor(.secondary)
//                            .frame(width: 24)
//                        
//                        ColorPicker("Vehicle Color", selection: $selectedColor, supportsOpacity: false)
//                    }
//                } header: {
//                    Text("Vehicle Color")
//                } footer: {
//                    Text("Choose the primary color of your vehicle")
//                }
//
//                // Save Button Section
//                Section {
//                    Button(action: saveVehicle) {
//                        Text(saveButtonText)
//                            .frame(maxWidth: .infinity)
//                            .padding()
//                            .background(isFormValid ? Color.accentColor : Color.gray)
//                            .foregroundColor(.white)
//                            .cornerRadius(8)
//                    }
//                    .disabled(!isFormValid)
//                }
//            }
//            .navigationTitle(navigationTitle)
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") { onCancel() }
//                }
//                
//                ToolbarItem(placement: .keyboard) {
//                    Button("Done") {
//                        focusedField = nil
//                    }
//                }
//            }
//            .onAppear {
//                setupInitialValues()
//            }
//        }
//    }
//
//    private var navigationTitle: String {
//        switch mode {
//        case .add: return "Add Vehicle"
//        case .edit: return "Edit Vehicle"
//        }
//    }
//    
//    private var saveButtonText: String {
//        switch mode {
//        case .add: return "Add Vehicle"
//        case .edit: return "Save Changes"
//        }
//    }
//    
//    private var isFormValid: Bool {
//        let requiredFieldsValid = !registrationNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
//                                 !vehicleName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
//        
//        let noValidationErrors = validationErrors.isEmpty
//        
//        return requiredFieldsValid && noValidationErrors
//    }
//    
//    private func setupInitialValues() {
//        switch mode {
//        case .add:
//            registrationNumber = ""
//            vehicleName = ""
//            selectedColor = .blue
//        case .edit(let existing):
//            registrationNumber = existing.registrationNumber
//            vehicleName = existing.vehicleName
//            selectedColor = existing.color
//        }
//    }
//    
//    private func validateRegistrationNumber() {
//        let trimmed = registrationNumber.trimmingCharacters(in: .whitespacesAndNewlines)
//        
//        guard !trimmed.isEmpty else {
//            validationErrors.removeValue(forKey: "registration")
//            return
//        }
//        
//        // Check for duplicates
//        if case .add = mode {
//            if existingVehicles.contains(where: { $0.registrationNumber.lowercased() == trimmed.lowercased() }) {
//                validationErrors["registration"] = "This vehicle is already added"
//                return
//            }
//        } else if case .edit(let existing) = mode {
//            if existingVehicles.contains(where: {
//                $0.registrationNumber.lowercased() == trimmed.lowercased() && $0.id != existing.id
//            }) {
//                validationErrors["registration"] = "This vehicle is already added"
//                return
//            }
//        }
//        
//        // Basic format validation
//        let pattern = "^[A-Z]{2}[0-9]{1,2}[A-Z]{1,2}[0-9]{4}$"
//        let regex = try? NSRegularExpression(pattern: pattern)
//        let range = NSRange(location: 0, length: trimmed.count)
//        
//        if regex?.firstMatch(in: trimmed, options: [], range: range) == nil {
//            validationErrors["registration"] = "Invalid format (e.g., KA01AB1234)"
//        } else {
//            validationErrors.removeValue(forKey: "registration")
//        }
//    }
//    
//    private func validateVehicleName() {
//        let trimmed = vehicleName.trimmingCharacters(in: .whitespacesAndNewlines)
//        
//        guard !trimmed.isEmpty else {
//            validationErrors.removeValue(forKey: "vehicleName")
//            return
//        }
//        
//        if trimmed.count < 2 {
//            validationErrors["vehicleName"] = "Vehicle name must be at least 2 characters"
//        } else {
//            validationErrors.removeValue(forKey: "vehicleName")
//        }
//    }
//    
//    private func saveVehicle() {
//        let trimmedRegistration = registrationNumber.trimmingCharacters(in: .whitespacesAndNewlines)
//        let trimmedVehicleName = vehicleName.trimmingCharacters(in: .whitespacesAndNewlines)
//        
//        guard !trimmedRegistration.isEmpty,
//              !trimmedVehicleName.isEmpty,
//              validationErrors.isEmpty else {
//            onSave(.error("Please fix validation errors"))
//            return
//        }
//        
//        let vehicle: Vehicle
//        if case .edit(let existing) = mode {
//            vehicle = Vehicle(
//                id: existing.id,
//                registrationNumber: trimmedRegistration,
//                vehicleName: trimmedVehicleName,
//                color: selectedColor
//            )
//        } else {
//            vehicle = Vehicle(
//                id: UUID(),
//                registrationNumber: trimmedRegistration,
//                vehicleName: trimmedVehicleName,
//                color: selectedColor
//            )
//        }
//        
//        onSave(.success(vehicle))
//    }
//}
//

import SwiftUI

struct MyVehiclesView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var vehicleManager = SharedVehicleManager.shared
    @EnvironmentObject var authViewModel: AuthViewModel
    
    @State private var showingAddVehicle = false
    @State private var showingDeleteAlert = false
    @State private var deleteIndex: Int? = nil
    @State private var isLoading = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    if vehicleManager.isLoading {
                        ProgressView("Loading vehicles...")
                            .padding()
                    } else if vehicleManager.vehicles.isEmpty {
                        EmptyVehiclesView {
                            showingAddVehicle = true
                        }
                    } else {
                        VehicleListView(
                            vehicles: vehicleManager.vehicles,
                            onDelete: { index in
                                deleteIndex = index
                                showingDeleteAlert = true
                            }
                        )
                    }
                    
                    if !vehicleManager.vehicles.isEmpty {
                        AddVehicleCTA {
                            showingAddVehicle = true
                        }
                    }
                }
            }
            .navigationTitle("My Vehicles")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { dismiss() }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !vehicleManager.vehicles.isEmpty {
                        Button {
                            showingAddVehicle = true
                        } label: {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
            .sheet(isPresented: $showingAddVehicle) {
                SimpleAddVehicleSheet()
            }
            .alert("Delete Vehicle", isPresented: $showingDeleteAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Delete", role: .destructive) {
                    if let index = deleteIndex {
                        vehicleManager.removeVehicle(at: index)
                    }
                }
            } message: {
                Text("Are you sure you want to delete this vehicle?")
            }
        }
        .onAppear {
            // Fetch vehicles from backend
            if let userId = authViewModel.getCurrentUserId() {
                vehicleManager.fetchVehiclesFromBackend(userId: userId) { _ in }
            }
        }
    }
}

// MARK: - Vehicle List
struct VehicleListView: View {
    let vehicles: [VehicleData]
    let onDelete: (Int) -> Void
    
    var body: some View {
        List {
            ForEach(Array(vehicles.enumerated()), id: \.element.id) { index, vehicle in
                VehicleRow(vehicle: vehicle)
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            onDelete(index)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
            }
        }
        .listStyle(.insetGrouped)
    }
}

// MARK: - Vehicle Row
struct VehicleRow: View {
    let vehicle: VehicleData
    
    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.blue.opacity(0.2))
                    .frame(width: 50, height: 50)
                
                Image(systemName: "car.fill")
                    .foregroundColor(.blue)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(vehicle.registrationNumber)
                    .font(.headline)
                
                Text("Vehicle")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Empty State
struct EmptyVehiclesView: View {
    let onAdd: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            Image(systemName: "car.2.fill")
                .font(.system(size: 80))
                .foregroundColor(.gray.opacity(0.6))
            
            VStack(spacing: 8) {
                Text("No Vehicles Added")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Add your vehicle registration number to quickly select it when booking.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            Spacer()
            
            Button(action: onAdd) {
                HStack {
                    Image(systemName: "plus.circle.fill")
                    Text("Add Your First Vehicle")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
                .padding(.horizontal, 24)
            }
            
            Spacer()
        }
    }
}

// MARK: - Add Vehicle CTA
struct AddVehicleCTA: View {
    let action: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            Divider()
            
            Button(action: action) {
                HStack {
                    Image(systemName: "plus.circle.fill")
                    Text("Add New Vehicle")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
                .padding(16)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}

// MARK: - Simple Add Vehicle Sheet (Only Registration Number)
struct SimpleAddVehicleSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var vehicleManager = SharedVehicleManager.shared
    
    @State private var registrationNumber = ""
    @State private var isSaving = false
    @State private var showError = false
    @State private var errorMessage = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Vehicle Number (e.g., KA01AB1234)", text: $registrationNumber)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                } header: {
                    Text("Registration Number")
                } footer: {
                    Text("Enter your vehicle registration number")
                }
            }
            .navigationTitle("Add Vehicle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .disabled(isSaving)
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    if isSaving {
                        ProgressView()
                            .scaleEffect(0.8)
                    } else {
                        Button("Save") {
                            addVehicle()
                        }
                        .disabled(registrationNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") { }
            } message: {
                Text(errorMessage)
            }
        }
    }
    
    private func addVehicle() {
        let trimmed = registrationNumber.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        
        isSaving = true
        
        vehicleManager.addVehicleToBackend(trimmed) { success, message in
            DispatchQueue.main.async {
                self.isSaving = false
                
                if success {
                    print("✅ Vehicle added: \(trimmed)")
                    dismiss()
                } else {
                    errorMessage = message ?? "Failed to add vehicle"
                    showError = true
                }
            }
        }
    }
}
