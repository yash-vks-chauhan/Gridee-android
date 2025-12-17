//import SwiftUI
//import Foundation
//
//// MARK: - User Model (Local App Model)
////struct User: Codable {
////    let id: String
////    let fullName: String
////    let email: String
////    let phone: String
////    var vehicleNumbers: [String]?
////    
////    var vehicleNumber: String? {
////        if let vehicles = vehicleNumbers, !vehicles.isEmpty {
////            return vehicles.first
////        }
////        return nil
////    }
////    
////    // ✅ SAFE ACCESS: Get vehicles array safely
////    var safeVehicleNumbers: [String] {
////        return vehicleNumbers ?? []
////    }
////}
//
//// MARK: - Users Model (Backend Response Model) - ADDED BACK
//
//// MARK: - Authentication ViewModel
//class AuthViewModel: ObservableObject {
//    @Published var isAuthenticated = false
//    @Published var currentUser: Users?
//    @Published var isLoading = false
//    @Published var errorMessage = ""
//    
//    init() {
//        checkAuthenticationStatus()
//    }
//    
//    // MARK: - Check Authentication Status
//    func checkAuthenticationStatus() {
//        isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
//        if isAuthenticated {
//            loadUserData()
//            
//            // ✅ FETCH FRESH USER DATA FROM BACKEND ON APP LAUNCH
//            if let user = currentUser {
//                fetchUserProfile(userId: user.id)
//            }
//        }
//    }
//    
//    // MARK: - Load User Data
//    private func loadUserData() {
//        if let userData = UserDefaults.standard.data(forKey: "userData"),
//           let decodedUser = try? JSONDecoder().decode(Users.self, from: userData) {
//            currentUser = decodedUser
//            print("✅ Loaded user from storage: \(decodedUser.name) - ID: \(decodedUser.id)")
//        }
//    }
//    
//    // MARK: - Save User Data
//    private func saveUserData(_ user: Users) {
//        if let encoded = try? JSONEncoder().encode(user) {
//            UserDefaults.standard.set(encoded, forKey: "userData")
//            print("✅ User data saved to storage")
//        }
//        // ✅ ALSO SAVE USER ID SEPARATELY FOR EASY ACCESS
//        saveUserId(user.id)
//    }
//    
//    // ✅ CRITICAL: Save User ID separately
//    private func saveUserId(_ userId: String) {
//        UserDefaults.standard.set(userId, forKey: "currentUserId")
//        print("✅ User ID saved separately: \(userId)")
//    }
//    
//    // ✅ CRITICAL: Get Current User ID
//    func getCurrentUserId() -> String? {
//        if let userId = currentUser?.id {
//            print("✅ Got user ID from currentUser: \(userId)")
//            return userId
//        }
//        
//        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
//            print("✅ Got user ID from UserDefaults: \(userId)")
//            return userId
//        }
//        
//        print("❌ No user ID found in AuthViewModel")
//        return nil
//    }
//    
//    // ✅ UPDATED: Get Current User Vehicles (Handle null vehicleNumbers)
//    func getCurrentUserVehicles() -> [String] {
//        guard let user = currentUser else {
//            print("❌ No current user in AuthViewModel")
//            return []
//        }
//        
//        // Handle null vehicleNumbers from backend
//        if let vehicleNumbers = user.vehicleNumbers, !vehicleNumbers.isEmpty {
//            print("✅ Got vehicles from vehicleNumbers array: \(vehicleNumbers)")
//            return vehicleNumbers
//        }
//        
//        // Fallback: Create array from single vehicleNumber if exists
//        if let singleVehicle = user.vehicleNumber, !singleVehicle.isEmpty {
//            print("✅ Created vehicle array from single vehicle: [\(singleVehicle)]")
//            return [singleVehicle]
//        }
//        
//        print("⚠️ No vehicles found for user, creating default")
//        // Create a default vehicle based on user ID
//        let defaultVehicle = "USER_\(String(user.id.prefix(6)))"
//        return [defaultVehicle]
//    }
//    
//    // ✅ UPDATED: Get Current User Vehicle (First vehicle)
//    func getCurrentUserVehicle() -> String? {
//        let vehicles = getCurrentUserVehicles()
//        let firstVehicle = vehicles.first
//        print("✅ Current user vehicle: \(firstVehicle ?? "none")")
//        return firstVehicle
//    }
//    
//    // MARK: - ✅ NEW: Vehicle Management Methods (ADDED FOR MY VEHICLES UI)
//    
//    // ✅ Update user vehicles array
//    func updateUserVehicles(_ vehicleNumbers: [String]) {
//        guard var user = currentUser else {
//            print("❌ No current user to update vehicles")
//            return
//        }
//        
//        print("🚗 Updating user vehicles: \(vehicleNumbers)")
//        user.vehicleNumbers = vehicleNumbers
//        self.currentUser = user
//        
//        // Save to UserDefaults (your existing pattern)
//        saveUserData(user)
//        
//        print("✅ Updated user vehicles: \(vehicleNumbers)")
//    }
//    
//    // ✅ Add single vehicle to existing array
//    func addUserVehicle(_ vehicleNumber: String) {
//        guard !vehicleNumber.isEmpty else {
//            print("❌ Cannot add empty vehicle")
//            return
//        }
//        
//        var currentVehicles = getCurrentUserVehicles()
//        
//        // Check if vehicle already exists
//        if !currentVehicles.contains(vehicleNumber) {
//            currentVehicles.append(vehicleNumber)
//            updateUserVehicles(currentVehicles)
//            print("✅ Added vehicle: \(vehicleNumber)")
//        } else {
//            print("⚠️ Vehicle \(vehicleNumber) already exists")
//        }
//    }
//    
//    // ✅ Remove vehicle from array
//    func removeUserVehicle(_ vehicleNumber: String) {
//        var currentVehicles = getCurrentUserVehicles()
//        currentVehicles.removeAll { $0 == vehicleNumber }
//        updateUserVehicles(currentVehicles)
//        print("✅ Removed vehicle: \(vehicleNumber)")
//    }
//    
//    // END OF NEW VEHICLE MANAGEMENT METHODS
//    
//    // MARK: - Login with Backend Integration
//    func login(email: String, password: String) {
//        print("🔐 Login attempt for: \(email)")
//        isLoading = true
//        errorMessage = ""
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/users/login") else {
//            print("❌ Invalid URL for login endpoint")
//            self.errorMessage = "Invalid server URL"
//            self.isLoading = false
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        let body: [String: Any] = [
//            "email": email,
//            "password": password
//        ]
//        
//        do {
//            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
//            request.httpBody = jsonData
//            
//            if let jsonString = String(data: jsonData, encoding: .utf8) {
//                print("📤 Sending login JSON: \(jsonString)")
//            }
//        } catch {
//            print("❌ Failed to serialize login JSON: \(error.localizedDescription)")
//            self.errorMessage = "Failed to prepare login data"
//            self.isLoading = false
//            return
//        }
//        
//        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                
//                if let error = error {
//                    print("❌ Login network error: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse else {
//                    print("❌ Invalid response from server")
//                    self?.errorMessage = "Invalid response from server"
//                    return
//                }
//                
//                print("🔁 Login HTTPStatus: \(httpResp.statusCode)")
//                
//                if !(200...299).contains(httpResp.statusCode) {
//                    if let d = data, let s = String(data: d, encoding: .utf8) {
//                        print("Server response:\n\(s)")
//                    }
//                    self?.errorMessage = (httpResp.statusCode == 401) ? "Invalid email or password" : "Server error: \(httpResp.statusCode)"
//                    return
//                }
//                
//                guard let data = data else {
//                    print("⚠️ No data received from server")
//                    self?.errorMessage = "No response data"
//                    return
//                }
//                
//                // ✅ IMPROVED: Better JSON parsing with debug info
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📥 RAW LOGIN RESPONSE:\n\(jsonString)")
//                }
//                
//                do {
//                    let loggedInUser = try JSONDecoder().decode(Users.self, from: data)
//                    print("✅ Login successful: \(loggedInUser.email) with ID: \(loggedInUser.id)")
//                    print("✅ User vehicles: \(loggedInUser.safeVehicleNumbers)")
//                    
//                    self?.isAuthenticated = true
//                    self?.currentUser = loggedInUser
//                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
//                    self?.saveUserData(loggedInUser)
//                    
//                    // ✅ IMMEDIATE: Log successful setup
//                    print("✅ Authentication setup complete - User ID: \(loggedInUser.id)")
//                    
//                } catch {
//                    print("❌ Failed to decode login response: \(error.localizedDescription)")
//                    if let raw = String(data: data, encoding: .utf8) {
//                        print("RAW RESPONSE:\n\(raw)")
//                    }
//                    self?.errorMessage = "Failed to parse server response"
//                }
//            }
//        }.resume()
//    }
//    
//    // MARK: - Sign Up with Vehicle Number
//    func signUp(name: String, email: String, phone: String, vehicleNumber: String, password: String) {
//        print("📝 SignUp attempt for: \(name) - Vehicle: \(vehicleNumber)")
//        isLoading = true
//        errorMessage = ""
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/users/register") else {
//            print("❌ Invalid URL for registration endpoint")
//            self.errorMessage = "Invalid server URL"
//            self.isLoading = false
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        let body: [String: Any] = [
//            "name": name,
//            "email": email,
//            "phone": phone,
//            "vehicleNumber": vehicleNumber,
//            "passwordHash": password,
//        ]
//        
//        do {
//            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
//            request.httpBody = jsonData
//            
//            if let jsonString = String(data: jsonData, encoding: .utf8) {
//                print("📤 Sending registration JSON: \(jsonString)")
//            }
//        } catch {
//            print("❌ Failed to serialize registration JSON: \(error.localizedDescription)")
//            self.errorMessage = "Failed to prepare registration data"
//            self.isLoading = false
//            return
//        }
//        
//        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                
//                if let error = error {
//                    print("❌ SignUp network error: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse else {
//                    print("❌ Invalid response from server")
//                    self?.errorMessage = "Invalid response from server"
//                    return
//                }
//                
//                if !(200...299).contains(httpResp.statusCode) {
//                    print("❌ Server returned status code: \(httpResp.statusCode)")
//                    if let data = data, let respStr = String(data: data, encoding: .utf8) {
//                        print("Server response:\n\(respStr)")
//                    }
//                    self?.errorMessage = "Server error: \(httpResp.statusCode)"
//                    return
//                }
//                
//                if let data = data {
//                    do {
//                        let registeredUser = try JSONDecoder().decode(Users.self, from: data)
//                        print("✅ SignUp successful: \(registeredUser.email) with ID: \(registeredUser.id)")
//                        
//                        self?.isAuthenticated = true
//                        self?.currentUser = registeredUser
//                        UserDefaults.standard.set(true, forKey: "isAuthenticated")
//                        self?.saveUserData(registeredUser)
//                        
//                    } catch {
//                        print("❌ Failed to decode response: \(error.localizedDescription)")
//                        self?.errorMessage = "Failed to parse server response"
//                    }
//                } else {
//                    print("⚠️ No data received from server")
//                    self?.errorMessage = "No response data"
//                }
//            }
//        }.resume()
//    }
//    
//    // MARK: - Logout
//    func logout() {
//        print("🚪 User logged out")
//        isAuthenticated = false
//        currentUser = nil
//        errorMessage = ""
//        UserDefaults.standard.set(false, forKey: "isAuthenticated")
//        UserDefaults.standard.removeObject(forKey: "userData")
//        UserDefaults.standard.removeObject(forKey: "currentUserId") // ✅ Clear user ID
//    }
//    
//    // MARK: - Google Sign-In (Updated with multiple vehicles)
//    func loginWithGoogle() {
//        print("🌐 Google Sign-In initiated")
//        isLoading = true
//        errorMessage = ""
//        
//        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
//            print("✅ Google Sign-In successful")
//            let user = Users(
//                id: "google_" + UUID().uuidString,
//                name: "Rishabh Singh",
//                email: "rishabh@gmail.com",
//                phone: "+91 98765 43210",
//                vehicleNumbers: ["KA01AB1234", "KA02CD5678"], // ✅ Multiple vehicles
//                firstUser: false,
//                walletCoins: 0,
//                createdAt: ISO8601DateFormatter().string(from: Date()),
//                passwordHash: nil
//            )
//            
//            self.isAuthenticated = true
//            self.currentUser = user
//            UserDefaults.standard.set(true, forKey: "isAuthenticated")
//            self.saveUserData(user)
//            self.isLoading = false
//        }
//    }
//    
//    // MARK: - Apple Sign-In (Updated with multiple vehicles)
//    func loginWithApple() {
//        print("🍎 Apple Sign-In initiated")
//        isLoading = true
//        errorMessage = ""
//        
//        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
//            print("✅ Apple Sign-In successful")
//            let user = Users(
//                id: "apple_" + UUID().uuidString,
//                name: "Rishabh Singh",
//                email: "rishabh@icloud.com",
//                phone: "+91 98765 43210",
//                vehicleNumbers: ["KA01AB1234", "KA02CD5678"], // ✅ Multiple vehicles
//                firstUser: false,
//                walletCoins: 0,
//                createdAt: ISO8601DateFormatter().string(from: Date()),
//                passwordHash: nil
//            )
//            
//            self.isAuthenticated = true
//            self.currentUser = user
//            UserDefaults.standard.set(true, forKey: "isAuthenticated")
//            self.saveUserData(user)
//            self.isLoading = false
//        }
//    }
//    
//    // MARK: - Password Reset
//    func resetPassword(email: String) {
//        print("🔄 Password reset requested for: \(email)")
//        isLoading = true
//        errorMessage = ""
//        
//        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
//            if !email.isEmpty {
//                print("✅ Password reset email sent")
//                self.errorMessage = "Password reset link sent to \(email)"
//            } else {
//                print("❌ Password reset failed: Invalid email")
//                self.errorMessage = "Please enter a valid email address"
//            }
//            self.isLoading = false
//        }
//    }
//    
//    // MARK: - Update User Profile
//    func updateProfile(name: String, phone: String, vehicleNumber: String) {
//        print("📝 Profile update for: \(name)")
//        guard let user = currentUser else { return }
//        
//        isLoading = true
//        errorMessage = ""
//        
//        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
//            // ✅ UPDATED: Handle both single and multiple vehicles
//            var updatedVehicles = user.safeVehicleNumbers
//            if !updatedVehicles.contains(vehicleNumber) {
//                updatedVehicles.append(vehicleNumber)
//            }
//            
//            let updatedUser = Users(
//                id: user.id,
//                name: name,
//                email: user.email,
//                phone: phone,
//                vehicleNumbers: updatedVehicles,
//                firstUser: user.firstUser,
//                walletCoins: user.walletCoins,
//                createdAt: user.createdAt,
//                passwordHash: user.passwordHash
//            )
//            
//            self.currentUser = updatedUser
//            self.saveUserData(updatedUser)
//            print("✅ Profile updated successfully")
//            self.isLoading = false
//        }
//    }
//    
//    // MARK: - Clear Error Message
//    func clearError() {
//        errorMessage = ""
//    }
//    
//    // MARK: - Fetch User Profile from Backend
//    func fetchUserProfile(userId: String) {
//        print("🔄 Fetching user profile for ID: \(userId)")
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(userId)") else {
//            print("❌ Invalid URL for user profile endpoint")
//            self.errorMessage = "Invalid server URL"
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Fetch user profile error: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse,
//                      (200...299).contains(httpResp.statusCode) else {
//                    print("❌ Server returned invalid status code for user profile")
//                    self?.errorMessage = "Failed to fetch user profile"
//                    return
//                }
//                
//                if let data = data {
//                    do {
//                        let fetchedUser = try JSONDecoder().decode(Users.self, from: data)
//                        print("✅ User profile fetched successfully: \(fetchedUser.name)")
//                        print("✅ User vehicles: \(fetchedUser.safeVehicleNumbers)")
//                        
//                        // ✅ UPDATE CURRENT USER WITH REAL BACKEND DATA
//                        self?.currentUser = fetchedUser
//                        self?.saveUserData(fetchedUser)
//                    } catch {
//                        print("❌ Failed to decode user profile: \(error.localizedDescription)")
//                        self?.errorMessage = "Failed to parse user profile data"
//                    }
//                }
//            }
//        }.resume()
//    }
//    
//    // MARK: - Update User Profile on Backend
//    func updateUserProfileOnBackend(name: String, phone: String, vehicleNumber: String) {
//        print("📝 Updating user profile on backend: \(name)")
//        
//        guard let user = currentUser else {
//            print("❌ No current user to update")
//            errorMessage = "No user logged in"
//            return
//        }
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(user.id)") else {
//            print("❌ Invalid URL for user update endpoint")
//            self.errorMessage = "Invalid server URL"
//            return
//        }
//        
//        isLoading = true
//        errorMessage = ""
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "PUT"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        let body: [String: Any] = [
//            "name": name,
//            "phone": phone,
//            "vehicleNumber": vehicleNumber
//        ]
//        
//        do {
//            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
//            request.httpBody = jsonData
//            
//            if let jsonString = String(data: jsonData, encoding: .utf8) {
//                print("📤 Sending user update JSON: \(jsonString)")
//            }
//        } catch {
//            print("❌ Failed to serialize update JSON: \(error.localizedDescription)")
//            self.errorMessage = "Failed to prepare update data"
//            self.isLoading = false
//            return
//        }
//        
//        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                
//                if let error = error {
//                    print("❌ Update user profile error: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse,
//                      (200...299).contains(httpResp.statusCode) else {
//                    print("❌ Server returned error status for profile update")
//                    self?.errorMessage = "Failed to update profile"
//                    return
//                }
//                
//                if let data = data {
//                    do {
//                        let updatedUser = try JSONDecoder().decode(Users.self, from: data)
//                        print("✅ User profile updated successfully on backend")
//                        
//                        // ✅ UPDATE CURRENT USER WITH UPDATED DATA FROM BACKEND
//                        self?.currentUser = updatedUser
//                        self?.saveUserData(updatedUser)
//                    } catch {
//                        print("❌ Failed to decode updated user: \(error.localizedDescription)")
//                        // Still update locally even if decode fails
//                        var updatedVehicles = user.safeVehicleNumbers
//                        if !updatedVehicles.contains(vehicleNumber) {
//                            updatedVehicles.append(vehicleNumber)
//                        }
//                        
//                        let updatedUser = Users(
//                            id: user.id,
//                            name: name,
//                            email: user.email,
//                            phone: phone,
//                            vehicleNumbers: updatedVehicles,
//                            firstUser: user.firstUser,
//                            walletCoins: user.walletCoins,
//                            createdAt: user.createdAt,
//                            passwordHash: user.passwordHash
//                        )
//                        self?.currentUser = updatedUser
//                        self?.saveUserData(updatedUser)
//                    }
//                } else {
//                    // No response data but successful status - update locally
//                    var updatedVehicles = user.safeVehicleNumbers
//                    if !updatedVehicles.contains(vehicleNumber) {
//                        updatedVehicles.append(vehicleNumber)
//                    }
//                    
//                    let updatedUser = Users(
//                        id: user.id,
//                        name: name,
//                        email: user.email,
//                        phone: phone,
//                        vehicleNumbers: updatedVehicles,
//                        firstUser: user.firstUser,
//                        walletCoins: user.walletCoins,
//                        createdAt: user.createdAt,
//                        passwordHash: user.passwordHash
//                    )
//                    self?.currentUser = updatedUser
//                    self?.saveUserData(updatedUser)
//                }
//            }
//        }.resume()
//    }
//}



import SwiftUI
import Foundation

// MARK: - Authentication ViewModel
class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var currentUser: Users?
    @Published var isLoading = false
    @Published var errorMessage = ""
    
    init() {
        checkAuthenticationStatus()
    }
    
    // MARK: - Check Authentication Status
    func checkAuthenticationStatus() {
        isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
        if isAuthenticated {
            loadUserData()
            
            // ✅ FIXED: Only fetch if we have a current user
            if let user = currentUser {
                fetchUserProfileFromBackend(userId: user.id)
            }
        }
    }
    
    // MARK: - Load User Data
    private func loadUserData() {
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let decodedUser = try? JSONDecoder().decode(Users.self, from: userData) {
            currentUser = decodedUser
            print("✅ Loaded user from storage: \(decodedUser.name) - ID: \(decodedUser.id)")
        }
    }
    
    // MARK: - Save User Data
    private func saveUserData(_ user: Users) {
        if let encoded = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(encoded, forKey: "userData")
            print("✅ User data saved to storage")
        }
        saveUserId(user.id)
    }
    
    private func saveUserId(_ userId: String) {
        UserDefaults.standard.set(userId, forKey: "currentUserId")
        print("✅ User ID saved separately: \(userId)")
    }
    
    func getCurrentUserId() -> String? {
        if let userId = currentUser?.id {
            print("✅ Got user ID from currentUser: \(userId)")
            return userId
        }
        
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            print("✅ Got user ID from UserDefaults: \(userId)")
            return userId
        }
        
        print("❌ No user ID found in AuthViewModel")
        return nil
    }
    
    func getCurrentUserVehicles() -> [String] {
        guard let user = currentUser else {
            print("❌ No current user in AuthViewModel")
            return []
        }
        
        if let vehicleNumbers = user.vehicleNumbers, !vehicleNumbers.isEmpty {
            print("✅ Got vehicles from vehicleNumbers array: \(vehicleNumbers)")
            return vehicleNumbers
        }
        
        if let singleVehicle = user.vehicleNumber, !singleVehicle.isEmpty {
            print("✅ Created vehicle array from single vehicle: [\(singleVehicle)]")
            return [singleVehicle]
        }
        
        print("⚠️ No vehicles found for user, creating default")
        let defaultVehicle = "USER_\(String(user.id.prefix(6)))"
        return [defaultVehicle]
    }
    
    func getCurrentUserVehicle() -> String? {
        let vehicles = getCurrentUserVehicles()
        let firstVehicle = vehicles.first
        print("✅ Current user vehicle: \(firstVehicle ?? "none")")
        return firstVehicle
    }
    
    // MARK: - Vehicle Management Methods
    func updateUserVehicles(_ vehicleNumbers: [String]) {
        guard var user = currentUser else {
            print("❌ No current user to update vehicles")
            return
        }
        
        print("🚗 Updating user vehicles: \(vehicleNumbers)")
        user.vehicleNumbers = vehicleNumbers
        self.currentUser = user
        
        saveUserData(user)
        print("✅ Updated user vehicles: \(vehicleNumbers)")
    }
    
    func addUserVehicle(_ vehicleNumber: String) {
        guard !vehicleNumber.isEmpty else {
            print("❌ Cannot add empty vehicle")
            return
        }
        
        var currentVehicles = getCurrentUserVehicles()
        
        if !currentVehicles.contains(vehicleNumber) {
            currentVehicles.append(vehicleNumber)
            updateUserVehicles(currentVehicles)
            print("✅ Added vehicle: \(vehicleNumber)")
        } else {
            print("⚠️ Vehicle \(vehicleNumber) already exists")
        }
    }
    
    func removeUserVehicle(_ vehicleNumber: String) {
        var currentVehicles = getCurrentUserVehicles()
        currentVehicles.removeAll { $0 == vehicleNumber }
        updateUserVehicles(currentVehicles)
        print("✅ Removed vehicle: \(vehicleNumber)")
    }
    
    // MARK: - Login with Authentication Headers
    func login(email: String, password: String) {
        print("🔐 Login attempt for: \(email)")
        isLoading = true
        errorMessage = ""
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/users/login") else {
            print("❌ Invalid URL for login endpoint")
            self.errorMessage = "Invalid server URL"
            self.isLoading = false
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // ✅ Add Basic Auth header for Spring Security
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
            print("🔐 Added Basic Auth header for login")
        }
        
        let body: [String: Any] = [
            "email": email,
            "password": password
        ]
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
            request.httpBody = jsonData
            
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("📤 Sending login JSON: \(jsonString)")
            }
        } catch {
            print("❌ Failed to serialize login JSON: \(error.localizedDescription)")
            self.errorMessage = "Failed to prepare login data"
            self.isLoading = false
            return
        }
        
        // ✅ Use APIService's custom session with SSL support
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                if let error = error {
                    print("❌ Login network error: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse else {
                    print("❌ Invalid response from server")
                    self?.errorMessage = "Invalid response from server"
                    return
                }
                
                print("🔁 Login HTTPStatus: \(httpResp.statusCode)")
                
                if !(200...299).contains(httpResp.statusCode) {
                    if let d = data, let s = String(data: d, encoding: .utf8) {
                        print("Server response:\n\(s)")
                    }
                    self?.errorMessage = (httpResp.statusCode == 401) ? "Invalid email or password" : "Server error: \(httpResp.statusCode)"
                    return
                }
                
                guard let data = data else {
                    print("⚠️ No data received from server")
                    self?.errorMessage = "No response data"
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📥 RAW LOGIN RESPONSE:\n\(jsonString)")
                }
                
                do {
                    let loggedInUser = try JSONDecoder().decode(Users.self, from: data)
                    print("✅ Login successful: \(loggedInUser.email) with ID: \(loggedInUser.id)")
                    print("✅ User vehicles: \(loggedInUser.safeVehicleNumbers)")
                    
                    self?.isAuthenticated = true
                    self?.currentUser = loggedInUser
                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
                    self?.saveUserData(loggedInUser)
                    
                    print("✅ Authentication setup complete - User ID: \(loggedInUser.id)")
                    
                } catch {
                    print("❌ Failed to decode login response: \(error.localizedDescription)")
                    if let raw = String(data: data, encoding: .utf8) {
                        print("RAW RESPONSE:\n\(raw)")
                    }
                    self?.errorMessage = "Failed to parse server response"
                }
            }
        }.resume()
    }
    
    // MARK: - Sign Up with Authentication Headers
    func signUp(name: String, email: String, phone: String, vehicleNumber: String, password: String) {
        print("📝 SignUp attempt for: \(name) - Vehicle: \(vehicleNumber)")
        isLoading = true
        errorMessage = ""
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/users/register") else {
            print("❌ Invalid URL for registration endpoint")
            self.errorMessage = "Invalid server URL"
            self.isLoading = false
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // ✅ Add Basic Auth header for Spring Security
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
            print("🔐 Added Basic Auth header for signup")
        }
        
        let body: [String: Any] = [
            "name": name,
            "email": email,
            "phone": phone,
            "vehicleNumber": vehicleNumber,
            "passwordHash": password,
        ]
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
            request.httpBody = jsonData
            
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("📤 Sending registration JSON: \(jsonString)")
            }
        } catch {
            print("❌ Failed to serialize registration JSON: \(error.localizedDescription)")
            self.errorMessage = "Failed to prepare registration data"
            self.isLoading = false
            return
        }
        
        // ✅ Use APIService's custom session with SSL support
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                if let error = error {
                    print("❌ SignUp network error: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse else {
                    print("❌ Invalid response from server")
                    self?.errorMessage = "Invalid response from server"
                    return
                }
                
                if !(200...299).contains(httpResp.statusCode) {
                    print("❌ Server returned status code: \(httpResp.statusCode)")
                    if let data = data, let respStr = String(data: data, encoding: .utf8) {
                        print("Server response:\n\(respStr)")
                    }
                    self?.errorMessage = "Server error: \(httpResp.statusCode)"
                    return
                }
                
                if let data = data {
                    do {
                        let registeredUser = try JSONDecoder().decode(Users.self, from: data)
                        print("✅ SignUp successful: \(registeredUser.email) with ID: \(registeredUser.id)")
                        
                        self?.isAuthenticated = true
                        self?.currentUser = registeredUser
                        UserDefaults.standard.set(true, forKey: "isAuthenticated")
                        self?.saveUserData(registeredUser)
                        
                    } catch {
                        print("❌ Failed to decode response: \(error.localizedDescription)")
                        self?.errorMessage = "Failed to parse server response"
                    }
                } else {
                    print("⚠️ No data received from server")
                    self?.errorMessage = "No response data"
                }
            }
        }.resume()
    }
    
    // MARK: - Logout
    func logout() {
        print("🚪 User logged out")
        isAuthenticated = false
        currentUser = nil
        errorMessage = ""
        UserDefaults.standard.set(false, forKey: "isAuthenticated")
        UserDefaults.standard.removeObject(forKey: "userData")
        UserDefaults.standard.removeObject(forKey: "currentUserId")
    }
    
    // MARK: - Google Sign-In (Mock)
    func loginWithGoogle() {
        print("🌐 Google Sign-In initiated")
        isLoading = true
        errorMessage = ""
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            print("✅ Google Sign-In successful")
            let user = Users(
                id: "google_" + UUID().uuidString,
                name: "Google User",
                email: "user@gmail.com",
                phone: "+91 98765 43210",
                vehicleNumbers: ["KA01AB1234", "KA02CD5678"],
                firstUser: false,
                walletCoins: 0,
                createdAt: ISO8601DateFormatter().string(from: Date()),
                passwordHash: nil
            )
            
            self.isAuthenticated = true
            self.currentUser = user
            UserDefaults.standard.set(true, forKey: "isAuthenticated")
            self.saveUserData(user)
            self.isLoading = false
        }
    }
    
    // MARK: - Apple Sign-In (Mock)
    func loginWithApple() {
        print("🍎 Apple Sign-In initiated")
        isLoading = true
        errorMessage = ""
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            print("✅ Apple Sign-In successful")
            let user = Users(
                id: "apple_" + UUID().uuidString,
                name: "Apple User",
                email: "user@icloud.com",
                phone: "+91 98765 43210",
                vehicleNumbers: ["KA01AB1234", "KA02CD5678"],
                firstUser: false,
                walletCoins: 0,
                createdAt: ISO8601DateFormatter().string(from: Date()),
                passwordHash: nil
            )
            
            self.isAuthenticated = true
            self.currentUser = user
            UserDefaults.standard.set(true, forKey: "isAuthenticated")
            self.saveUserData(user)
            self.isLoading = false
        }
    }
    
    // MARK: - Password Reset
    func resetPassword(email: String) {
        print("🔄 Password reset requested for: \(email)")
        isLoading = true
        errorMessage = ""
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            if !email.isEmpty {
                print("✅ Password reset email sent")
                self.errorMessage = "Password reset link sent to \(email)"
            } else {
                print("❌ Password reset failed: Invalid email")
                self.errorMessage = "Please enter a valid email address"
            }
            self.isLoading = false
        }
    }
    
    // MARK: - Update User Profile (Local)
    func updateProfile(name: String, phone: String, vehicleNumber: String) {
        print("📝 Profile update for: \(name)")
        guard let user = currentUser else { return }
        
        isLoading = true
        errorMessage = ""
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            var updatedVehicles = user.safeVehicleNumbers
            if !updatedVehicles.contains(vehicleNumber) {
                updatedVehicles.append(vehicleNumber)
            }
            
            let updatedUser = Users(
                id: user.id,
                name: name,
                email: user.email,
                phone: phone,
                vehicleNumbers: updatedVehicles,
                firstUser: user.firstUser,
                walletCoins: user.walletCoins,
                createdAt: user.createdAt,
                passwordHash: user.passwordHash
            )
            
            self.currentUser = updatedUser
            self.saveUserData(updatedUser)
            print("✅ Profile updated successfully")
            self.isLoading = false
        }
    }
    
    // MARK: - Clear Error Message
    func clearError() {
        errorMessage = ""
    }
    
    // MARK: - ✅ FIXED: Fetch User Profile from Backend
    func fetchUserProfileFromBackend(userId: String) {
        print("🔄 Fetching user profile for ID: \(userId)")
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(userId)") else {
            print("❌ Invalid URL for user profile endpoint")
            self.errorMessage = "Invalid server URL"
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // ✅ Add Basic Auth header
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Fetch user profile error: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    print("❌ Server returned invalid status code for user profile")
                    self?.errorMessage = "Failed to fetch user profile"
                    return
                }
                
                if let data = data {
                    do {
                        let fetchedUser = try JSONDecoder().decode(Users.self, from: data)
                        print("✅ User profile fetched successfully: \(fetchedUser.name)")
                        print("✅ User vehicles: \(fetchedUser.safeVehicleNumbers)")
                        
                        self?.currentUser = fetchedUser
                        self?.saveUserData(fetchedUser)
                    } catch {
                        print("❌ Failed to decode user profile: \(error.localizedDescription)")
                        self?.errorMessage = "Failed to parse user profile data"
                    }
                }
            }
        }.resume()
    }
    
    // MARK: - Update User Profile on Backend
    func updateUserProfileOnBackend(name: String, phone: String, vehicleNumber: String) {
        print("📝 Updating user profile on backend: \(name)")
        
        guard let user = currentUser else {
            print("❌ No current user to update")
            errorMessage = "No user logged in"
            return
        }
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(user.id)") else {
            print("❌ Invalid URL for user update endpoint")
            self.errorMessage = "Invalid server URL"
            return
        }
        
        isLoading = true
        errorMessage = ""
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // ✅ Add Basic Auth header
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        let body: [String: Any] = [
            "name": name,
            "phone": phone,
            "vehicleNumber": vehicleNumber
        ]
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
            request.httpBody = jsonData
            
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("📤 Sending user update JSON: \(jsonString)")
            }
        } catch {
            print("❌ Failed to serialize update JSON: \(error.localizedDescription)")
            self.errorMessage = "Failed to prepare update data"
            self.isLoading = false
            return
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                if let error = error {
                    print("❌ Update user profile error: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    print("❌ Server returned error status for profile update")
                    self?.errorMessage = "Failed to update profile"
                    return
                }
                
                if let data = data {
                    do {
                        let updatedUser = try JSONDecoder().decode(Users.self, from: data)
                        print("✅ User profile updated successfully on backend")
                        
                        self?.currentUser = updatedUser
                        self?.saveUserData(updatedUser)
                    } catch {
                        print("❌ Failed to decode updated user: \(error.localizedDescription)")
                        // Still update locally even if decode fails
                        self?.updateProfileLocally(name: name, phone: phone, vehicleNumber: vehicleNumber, user: user)
                    }
                } else {
                    // No response data but successful status - update locally
                    self?.updateProfileLocally(name: name, phone: phone, vehicleNumber: vehicleNumber, user: user)
                }
            }
        }.resume()
    }
    
    // MARK: - Helper method for local profile update
    private func updateProfileLocally(name: String, phone: String, vehicleNumber: String, user: Users) {
        var updatedVehicles = user.safeVehicleNumbers
        if !updatedVehicles.contains(vehicleNumber) {
            updatedVehicles.append(vehicleNumber)
        }
        
        let updatedUser = Users(
            id: user.id,
            name: name,
            email: user.email,
            phone: phone,
            vehicleNumbers: updatedVehicles,
            firstUser: user.firstUser,
            walletCoins: user.walletCoins,
            createdAt: user.createdAt,
            passwordHash: user.passwordHash
        )
        self.currentUser = updatedUser
        self.saveUserData(updatedUser)
    }
}
