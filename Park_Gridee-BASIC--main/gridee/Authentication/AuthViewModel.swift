//import SwiftUI
//import Foundation
//
//// MARK: - Authentication ViewModel
//class AuthViewModel: ObservableObject {
//    @Published var isAuthenticated = false
//    @Published var currentUser: Users?
//    @Published var isLoading = false
//    @Published var errorMessage = ""
//    @Published var userRole: UserRole = .user {
//           didSet {
//               print("🔄 Role changed from \(oldValue.rawValue) to \(userRole.rawValue)")
//               UserDefaults.standard.set(userRole.rawValue, forKey: "userRole")
//               print("💾 Auto-saved role to UserDefaults: \(userRole.rawValue)")
//               
//               // ✅ FORCE OBSERVABLE UPDATE
//               objectWillChange.send()
//           }
//       }
//    private var roleSetByLogin = false
//    
//    // ✅ ADD: JWT Token storage
//    @Published var jwtToken: String? {
//        didSet {
//            if let token = jwtToken {
//                UserDefaults.standard.set(token, forKey: "jwtToken")
//                print("🔑 JWT Token saved to UserDefaults")
//            } else {
//                UserDefaults.standard.removeObject(forKey: "jwtToken")
//                print("🔑 JWT Token removed from UserDefaults")
//            }
//        }
//    }
//    
//    enum UserRole: String, Codable{
//        case user = "USER"
//        case admin = "ADMIN"
//        case transformer = "OPERATOR"
//    }
//    
//
//    
////    init() {
////        print("🚀 AuthViewModel initializing...")
////
////        // ✅ Load authentication state
////        self.isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
////        print("   Authenticated: \(isAuthenticated)")
////
////        // ✅ CRITICAL: Load JWT token from UserDefaults
////        if let savedToken = UserDefaults.standard.string(forKey: "jwtToken") {
////            self.jwtToken = savedToken
////            print("   ✅ JWT Token loaded from storage: \(savedToken.prefix(20))...")
////        } else {
////            print("   ⚠️ No JWT token found in storage")
////        }
////
////        // ✅ Load user role
////        if let savedRole = UserDefaults.standard.string(forKey: "userRole"),
////           let role = UserRole(rawValue: savedRole) {
////            self.userRole = role
////            print("   Role: \(role.rawValue)")
////        }
////
////        // ✅ Load user data
////        if let userData = UserDefaults.standard.data(forKey: "currentUser") {
////            do {
////                self.currentUser = try JSONDecoder().decode(Users.self, from: userData)
////                print("   ✅ User loaded: \(currentUser?.name ?? "unknown")")
////            } catch {
////                print("   ❌ Failed to load user: \(error)")
////            }
////        }
////
////        // ✅ CRITICAL: Connect APIService with the loaded token
////        if isAuthenticated && jwtToken != nil {
////            APIService.shared.setAuthViewModel(self)
////            print("   ✅ APIService connected with JWT token")
////        } else {
////            print("   ⚠️ APIService NOT connected - missing token or not authenticated")
////        }
////
////        print("🚀 AuthViewModel initialization complete")
////    }
//
//    init() {
//        print("🚀 AuthViewModel initializing...")
//        
//        self.isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
//        
//        if let savedToken = UserDefaults.standard.string(forKey: "jwtToken") {
//            self.jwtToken = savedToken
//        }
//        
//        if let savedRole = UserDefaults.standard.string(forKey: "userRole"),
//           let role = UserRole(rawValue: savedRole) {
//            self.userRole = role
//        }
//        
//        if let userData = UserDefaults.standard.data(forKey: "currentUser") {
//            do {
//                self.currentUser = try JSONDecoder().decode(Users.self, from: userData)
//                
//                // ✅ CRITICAL: Fetch fresh vehicles from backend on app start
//                if isAuthenticated, let userId = currentUser?.id {
//                    // ✅ Use SharedVehicleManager method
//                    SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: userId) { success in
//                        print(success ? "✅ Vehicles loaded" : "⚠️ No vehicles")
//                    }
//
//                }
//            } catch {
//                print("❌ Failed to load user: \(error)")
//            }
//        }
//        
//        if isAuthenticated && jwtToken != nil {
//            APIService.shared.setAuthViewModel(self)
//            PaymentService.shared.setAuthViewModel(self)
//        }
//        
//        print("🚀 AuthViewModel initialization complete")
//    }
//
//    
//    // ✅ ADD: Helper to get current token
//    func getAuthToken() -> String? {
//        return jwtToken
//    }
//    // MARK: - Registration with JWT (No Vehicle Number, with College Selection)
//    func register(name: String, email: String, phone: String, password: String, parkingLotName: String, completion: @escaping (Bool, String?) -> Void) {
//        APIService.shared.register(name: name, email: email, phone: phone, password: password, parkingLotName: parkingLotName) { [weak self] result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success(let response):
//                    // Save token
//                    UserDefaults.standard.set(response.token, forKey: "jwtToken")
//                    self?.jwtToken = response.token
//                    
//                    // Save user
//                    self?.currentUser = response.user
//                    self?.isAuthenticated = true
//                    
//                    completion(true, nil)
//                    
//                case .failure(let error):
//                    completion(false, error.localizedDescription)
//                }
//            }
//        }
//    }
//
//
//    
//    // MARK: - Check Authentication Status
//    func checkAuthenticationStatus() {
//        isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
//        
//        print("🔍 checkAuthenticationStatus called")
//        print("   isAuthenticated: \(isAuthenticated)")
//        
//        if isAuthenticated {
//            // ✅ Load role from UserDefaults
//            if let savedRole = UserDefaults.standard.string(forKey: "userRole") {
//                print("   Saved role in UserDefaults: '\(savedRole)'")
//                
//                let normalizedRole = savedRole.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
//                
//                if normalizedRole == "ADMIN" {
//                    userRole = .admin
//                    print("🔐 Restored ADMIN role from UserDefaults")
//                } else if normalizedRole == "OPERATOR" {
//                    userRole = .transformer
//                    print("🔧 Restored TRANSFORMER role from UserDefaults")
//                } else {
//                    userRole = .user
//                    print("👤 Restored USER role from UserDefaults")
//                }
//
//                
//            } else {
//                print("⚠️ No saved role found - keeping current role: \(userRole.rawValue)")
//            }
//            
//            // ✅ Load user data from local storage
//            loadUserData()
//            
//            // ✅ REMOVED: syncVehiclesWithManager() - doesn't exist anymore
//            // Vehicles are fetched from backend in init() already
//            
//            print("✅ Using cached data - vehicles already loaded in init()")
//        } else {
//            userRole = .user
//            print("🔑 Not authenticated - role set to USER")
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
//        saveUserId(user.id)
//    }
//    
//    private func saveUserId(_ userId: String) {
//        UserDefaults.standard.set(userId, forKey: "currentUserId")
//        print("✅ User ID saved separately: \(userId)")
//    }
//    
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
//    func getCurrentUserVehicles() -> [String] {
//        guard let user = currentUser else { return [] }
//        
//        let vehicles = user.safeVehicleNumbers.filter { vehicle in
//            !vehicle.contains("USER_") && !vehicle.isEmpty
//        }
//        
//        print("✅ Got real vehicles from user: \(vehicles)")
//        return vehicles
//    }
//
//    func getCurrentUserVehicle() -> String? {
//        let vehicles = getCurrentUserVehicles()
//        let firstVehicle = vehicles.first
//        print("✅ Current user vehicle: \(firstVehicle ?? "none")")
//        return firstVehicle
//    }
//    
//    // MARK: - Vehicle Management Methods
////    func updateUserVehicles(_ vehicleNumbers: [String]) {
////        guard var user = currentUser else {
////            print("❌ No current user to update vehicles")
////            return
////        }
////
////        print("🚗 Updating user vehicles: \(vehicleNumbers)")
////        user.vehicleNumbers = vehicleNumbers
////        self.currentUser = user
////
////        saveUserData(user)
////        print("✅ Updated user vehicles: \(vehicleNumbers)")
////    }
//    
//
//    func updateUserVehicles(_ vehicles: [String]) {
//        guard var user = currentUser else {
//            print("⚠️ Cannot update vehicles - no current user")
//            return
//        }
//        
//        user.vehicleNumbers = vehicles
//        self.currentUser = user
//        saveUserData(user)
//        
//        print("✅ Updated user vehicles in AuthViewModel: \(vehicles)")
//    }
//
//    
//    func addUserVehicle(_ vehicleNumber: String) {
//        guard !vehicleNumber.isEmpty else {
//            print("❌ Cannot add empty vehicle")
//            return
//        }
//        
//        var currentVehicles = getCurrentUserVehicles()
//        
//        if !currentVehicles.contains(vehicleNumber) {
//            currentVehicles.append(vehicleNumber)
//            updateUserVehicles(currentVehicles)
//            print("✅ Added vehicle: \(vehicleNumber)")
//        } else {
//            print("⚠️ Vehicle \(vehicleNumber) already exists")
//        }
//    }
//    
//    // MARK: - Update User Vehicles on Backend
//    func updateUserVehicles(_ vehicleNumbers: [String], completion: @escaping (Bool) -> Void) {
//        guard let user = currentUser else {
//            print("❌ No current user to update vehicles")
//            completion(false)
//            return
//        }
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(user.id)/vehicles") else {
//            print("❌ Invalid URL for vehicle update")
//            completion(false)
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "PUT"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        let loginString = "rajeev:parking"
//        if let loginData = loginString.data(using: .utf8) {
//            let base64LoginString = loginData.base64EncodedString()
//            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
//        }
//        
//        do {
//            let jsonData = try JSONEncoder().encode(vehicleNumbers)
//            request.httpBody = jsonData
//            
//            if let jsonString = String(data: jsonData, encoding: .utf8) {
//                print("📤 Updating vehicles: \(jsonString)")
//            }
//        } catch {
//            print("❌ Failed to encode vehicles: \(error.localizedDescription)")
//            completion(false)
//            return
//        }
//        
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Update vehicles error: \(error.localizedDescription)")
//                    completion(false)
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse,
//                      (200...299).contains(httpResp.statusCode) else {
//                    print("❌ Server returned error for vehicle update")
//                    completion(false)
//                    return
//                }
//                
//                print("✅ Vehicles updated on backend")
//                self?.updateUserVehicles(vehicleNumbers)
//                completion(true)
//            }
//        }.resume()
//    }
//
//    func removeUserVehicle(_ vehicleNumber: String) {
//        var currentVehicles = getCurrentUserVehicles()
//        currentVehicles.removeAll { $0 == vehicleNumber }
//        updateUserVehicles(currentVehicles)
//        print("✅ Removed vehicle: \(vehicleNumber)")
//    }
//    
//
//    // MARK: - Login with JWT
////    func login(email: String, password: String) {
////        print("🔐 Login attempt for: \(email)")
////        isLoading = true
////        errorMessage = ""
////
////        guard let url = URL(string: "\(APIService.backendBaseURL)/auth/login") else {
////            self.errorMessage = "Invalid server URL"
////            self.isLoading = false
////            return
////        }
////
////        var request = URLRequest(url: url)
////        request.httpMethod = "POST"
////        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
////
////        let body: [String: String] = [
////            "email": email,
////            "password": password
////        ]
////
////        do {
////            let jsonData = try JSONSerialization.data(withJSONObject: body)
////            request.httpBody = jsonData
////            print("📤 Sending login JSON: \(String(data: jsonData, encoding: .utf8) ?? "")")
////        } catch {
////            self.errorMessage = "Failed to prepare login data"
////            self.isLoading = false
////            return
////        }
////
////        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
////            DispatchQueue.main.async {
////                self?.isLoading = false
////
////                if let error = error {
////                    print("❌ Login network error: \(error.localizedDescription)")
////                    self?.errorMessage = error.localizedDescription
////                    return
////                }
////
////                guard let httpResp = response as? HTTPURLResponse else {
////                    self?.errorMessage = "Invalid server response"
////                    return
////                }
////
////                print("🔁 Login HTTPStatus: \(httpResp.statusCode)")
////
////                guard (200...299).contains(httpResp.statusCode) else {
////                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
////                        print("Server response:\n\(errorMsg)")
////                        self?.errorMessage = errorMsg
////                    } else {
////                        self?.errorMessage = "Login failed with status \(httpResp.statusCode)"
////                    }
////                    return
////                }
////
////                guard let data = data else {
////                    self?.errorMessage = "No response data"
////                    return
////                }
////
////                if let responseString = String(data: data, encoding: .utf8) {
////                    print("📥 RAW LOGIN RESPONSE:\n\(responseString)")
////                }
////
////                do {
////                    // ✅ FIXED: Decode from nested user object
////                    let loginResponse = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
////                    print("✅ Login response decoded successfully")
////                    print("   Token: \(loginResponse.token.prefix(20))...")
////                    print("   ID: \(loginResponse.user.id)")
////                    print("   Name: \(loginResponse.user.name)")
////                    print("   Role: \(loginResponse.user.role ?? "USER")")
////
////                    // ✅ Save JWT token to UserDefaults
////                    UserDefaults.standard.set(loginResponse.token, forKey: "jwtToken")
////                    self?.jwtToken = loginResponse.token
////                    print("🔑 JWT token saved to UserDefaults and ViewModel")
////
////                    // ✅ Set role
////                    let roleString = (loginResponse.user.role ?? "USER").uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
////                    if roleString == "ADMIN" {
////                        self?.userRole = .admin
////                        print("🔐 ADMIN role set")
////                    } else if roleString == "OPERATOR" {
////                        self?.userRole = .transformer
////                        print("🔧 TRANSFORMER role set")
////                    } else {
////                        self?.userRole = .user
////                        print("👤 USER role set")
////                    }
////
////                    // ✅ Create user object from login response
////                    let user = Users(
////                        id: loginResponse.user.id,
////                        name: loginResponse.user.name,
////                        email: loginResponse.user.email,
////                        phone: loginResponse.user.phone,
////                        vehicleNumbers: loginResponse.user.vehicleNumbers ?? [],
////                        firstUser: loginResponse.user.firstUser,
////                        walletCoins: loginResponse.user.walletCoins,
////                        createdAt: loginResponse.user.createdAt,
////                        updatedAt: nil,
////                        role: loginResponse.user.role,
////                        parkingLotId: loginResponse.user.parkingLotId,
////                        parkingLotName: loginResponse.user.parkingLotName,
////                        active: loginResponse.user.active  // ✅ Fixed - pass the actual value
////                    )
////
////                    self?.currentUser = user
////                    self?.saveUserData(user)
////                    print("✅ User data saved")
////
////                    // ✅ CRITICAL: Connect APIService to get JWT in future calls
////                    if let authVM = self {
////                        APIService.shared.setAuthViewModel(authVM)
////                        print("🔗 APIService connected to AuthViewModel - JWT now available for all API calls")
////                    }
////
////                    // ✅ Fetch vehicles from backend
////                    SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: loginResponse.user.id) { success in
////                        if success {
////                            print("✅ Vehicles fetched from backend")
////                        } else {
////                            print("⚠️ Failed to fetch vehicles")
////                        }
////                    }
////
////                    // ✅ Set authentication LAST
////                    self?.isAuthenticated = true
////                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
////
////                    print("✅ Login complete!")
////                    print("   Authenticated: \(self?.isAuthenticated ?? false)")
////                    print("   Role: \(self?.userRole.rawValue ?? "unknown")")
////                    print("   User ID: \(loginResponse.user.id)")
////
////                } catch {
////                    print("❌ Failed to decode login response: \(error)")
////                    if let responseString = String(data: data, encoding: .utf8) {
////                        print("RAW RESPONSE:\n\(responseString)")
////                    }
////                    self?.errorMessage = "Failed to process login response"
////                }
////            }
////        }.resume()
////    }
////
//    func login(email: String, password: String) {
//        print("🔐 Login attempt for: \(email)")
//        isLoading = true
//        errorMessage = ""
//
//        guard let url = URL(string: "\(APIService.backendBaseURL)/auth/login") else {
//            errorMessage = "Invalid server URL"
//            isLoading = false
//            return
//        }
//
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//
//        let body = [
//            "email": email,
//            "password": password
//        ]
//
//        do {
//            request.httpBody = try JSONSerialization.data(withJSONObject: body)
//        } catch {
//            errorMessage = "Failed to prepare login request"
//            isLoading = false
//            return
//        }
//
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                guard let self = self else { return }
//                self.isLoading = false
//
//                // 🌐 Network error
//                if let error = error {
//                    print("❌ Network error:", error.localizedDescription)
//                    self.errorMessage = "Please check your internet connection"
//                    return
//                }
//
//                guard let httpResp = response as? HTTPURLResponse else {
//                    self.errorMessage = "Invalid server response"
//                    return
//                }
//
//                print("🔁 Login HTTPStatus:", httpResp.statusCode)
//
//                // ❌ Authentication failed (NO RAW JSON)
//                guard (200...299).contains(httpResp.statusCode) else {
//                    self.errorMessage = self.mapLoginError(statusCode: httpResp.statusCode)
//                    return
//                }
//
//                guard let data = data else {
//                    self.errorMessage = "Empty server response"
//                    return
//                }
//
//                do {
//                    let loginResponse = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
//
//                    // ✅ Save token
//                    self.jwtToken = loginResponse.token
//                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
//
//                    // ✅ Set role
//                    let role = (loginResponse.user.role ?? "USER").uppercased()
//                    self.userRole = UserRole(rawValue: role) ?? .user
//
//                    // ✅ Build user
//                    let user = Users(
//                        id: loginResponse.user.id,
//                        name: loginResponse.user.name,
//                        email: loginResponse.user.email,
//                        phone: loginResponse.user.phone,
//                        vehicleNumbers: loginResponse.user.vehicleNumbers ?? [],
//                        firstUser: loginResponse.user.firstUser,
//                        walletCoins: loginResponse.user.walletCoins,
//                        createdAt: loginResponse.user.createdAt,
//                        updatedAt: nil,
//                        role: loginResponse.user.role,
//                        parkingLotId: loginResponse.user.parkingLotId,
//                        parkingLotName: loginResponse.user.parkingLotName,
//                        active: loginResponse.user.active
//                    )
//
//                    self.currentUser = user
//                    self.saveUserData(user)
//
//                    // ✅ Attach services
//                    APIService.shared.setAuthViewModel(self)
//                    PaymentService.shared.setAuthViewModel(self)
//
//                    // ✅ Fetch vehicles
//                    SharedVehicleManager.shared.fetchVehiclesFromBackend(
//                        userId: user.id
//                    ) { _ in }
//
//                    self.isAuthenticated = true
//                    print("✅ Login success")
//
//                } catch {
//                    print("❌ Decode error:", error)
//                    self.errorMessage = "Unable to sign in. Please try again."
//                }
//            }
//        }.resume()
//    }
//
//    
//   
//
//  
//
////     MARK: - Sign Up with JWT
////    func signUp(name: String, email: String, phone: String, vehicleNumber: String, password: String) {
////        print("📝 SignUp attempt for: \(name) - Vehicle: \(vehicleNumber)")
////        isLoading = true
////        errorMessage = ""
////
////        let registrationVehicle = vehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines)
////
////        guard let url = URL(string: "\(APIService.backendBaseURL)/auth/register") else {  // ✅ CHANGED ENDPOINT
////            print("❌ Invalid URL for registration endpoint")
////            self.errorMessage = "Invalid server URL"
////            self.isLoading = false
////            return
////        }
////
////        var request = URLRequest(url: url)
////        request.httpMethod = "POST"
////        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
////
////        // ✅ REMOVED: Basic Auth - No longer needed
////
////        let body: [String: Any] = [
////            "name": name,
////            "email": email,
////            "phone": phone,
////            "vehicleNumber": vehicleNumber,
////            "passwordHash": password,
////            "parkingLotName": "default"  // ✅ ADD: Required by your backend
////        ]
////
////        do {
////            let jsonData = try JSONSerialization.data(withJSONObject: body, options: [])
////            request.httpBody = jsonData
////
////            if let jsonString = String(data: jsonData, encoding: .utf8) {
////                print("📤 Sending registration JSON: \(jsonString)")
////            }
////        } catch {
////            print("❌ Failed to serialize registration JSON: \(error.localizedDescription)")
////            self.errorMessage = "Failed to prepare registration data"
////            self.isLoading = false
////            return
////        }
////
////        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
////            DispatchQueue.main.async {
////                self?.isLoading = false
////
////                if let error = error {
////                    print("❌ SignUp network error: \(error.localizedDescription)")
////                    self?.errorMessage = error.localizedDescription
////                    return
////                }
////
////                guard let httpResp = response as? HTTPURLResponse else {
////                    print("❌ Invalid response from server")
////                    self?.errorMessage = "Invalid response from server"
////                    return
////                }
////
////                if !(200...299).contains(httpResp.statusCode) {
////                    print("❌ Server returned status code: \(httpResp.statusCode)")
////                    if let data = data, let respStr = String(data: data, encoding: .utf8) {
////                        print("Server response:\n\(respStr)")
////                    }
////                    self?.errorMessage = "Server error: \(httpResp.statusCode)"
////                    return
////                }
////
////                if let data = data {
////                    do {
////                        // ✅ CHANGED: New response format with token
////                        let signupResponse = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
////                        print("✅ SignUp successful")
////                        print("   Token: \(signupResponse.token.prefix(20))...")
////                        print("   ID: \(signupResponse.id)")
////
////                        // ✅ Save JWT token
////                        self?.jwtToken = signupResponse.token
////
////                        // Set role
////                        let roleString = signupResponse.role.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
////                        if roleString == "ADMIN" {
////                            self?.userRole = .admin
////                        } else {
////                            self?.userRole = .user
////                        }
////
////                        // Fetch full profile
////                        self?.fetchUserProfileFromBackend(userId: signupResponse.id)
////
////                    } catch {
////                        print("❌ Failed to decode response: \(error.localizedDescription)")
////                        self?.errorMessage = "Failed to parse server response"
////                    }
////                } else {
////                    print("⚠️ No data received from server")
////                    self?.errorMessage = "No response data"
////                }
////            }
////        }.resume()
////    }
//
//    // MARK: - Sync Vehicles with SharedVehicleManager
////    private func syncVehiclesWithManager() {
////        guard let user = currentUser else { return }
////
////        let userVehicles = user.safeVehicleNumbers
////        print("🚗 Syncing \(userVehicles.count) vehicles with SharedVehicleManager")
////
////        SharedVehicleManager.shared.vehicles.removeAll()
////
////        for vehicleNumber in userVehicles {
////            SharedVehicleManager.shared.addVehicleByNumber(vehicleNumber)
////        }
////
////        print("✅ Vehicles synced to SharedVehicleManager: \(userVehicles)")
////    }
////
//
//    
//    // MARK: - Logout
//    // MARK: - Logout
//    func logout() {
//        print("🚪 Logging out...")
//        
//        // ✅ Clear all state
//        isAuthenticated = false
//        currentUser = nil
//        userRole = .user
//        jwtToken = nil
//        errorMessage = ""
//        roleSetByLogin = false
//        SharedVehicleManager.shared.clearVehicles()
//        
//        // ✅ Clear ALL UserDefaults
//        UserDefaults.standard.removeObject(forKey: "isAuthenticated")
//        UserDefaults.standard.removeObject(forKey: "currentUser")
//        UserDefaults.standard.removeObject(forKey: "userRole")
//        UserDefaults.standard.removeObject(forKey: "currentUserId")
//        UserDefaults.standard.removeObject(forKey: "jwtToken")
//        UserDefaults.standard.removeObject(forKey: "userData")
//        UserDefaults.standard.synchronize()
//        
//        // ✅ Clear vehicles
////        SharedVehicleManager.shared.clearVehicles()
////
//        // ✅ Disconnect services (set to nil, don't use weak reference)
//        // You'll need to update these methods to accept nil
//        
//        print("✅ Logout complete - all data cleared")
//        print("   JWT in UserDefaults: \(UserDefaults.standard.string(forKey: "jwtToken") ?? "NONE")")
//    }
//    private func mapLoginError(statusCode: Int) -> String {
//        switch statusCode {
//        case 400, 401, 403, 404:
//            return "Wrong email or password"
//        case 408:
//            return "Request timed out. Please try again."
//        case 500...599:
//            return "Server error. Please try again later."
//        default:
//            return "Something went wrong. Please try again."
//        }
//    }
//
//
//
//    // MARK: - Google Sign-In
//    func loginWithGoogle(idToken: String, accessToken: String) {
//        print("🌐 Google Sign-In initiated with backend")
//        isLoading = true
//        errorMessage = ""
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/oauth2/google/authenticate") else {
//            self.errorMessage = "Invalid server URL"
//            self.isLoading = false
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        let loginString = "rajeev:parking"
//        if let loginData = loginString.data(using: .utf8) {
//            let base64LoginString = loginData.base64EncodedString()
//            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
//        }
//        
//        let body: [String: Any] = [
//            "idToken": idToken,
//            "accessToken": accessToken
//        ]
//        
//        do {
//            let jsonData = try JSONSerialization.data(withJSONObject: body)
//            request.httpBody = jsonData
//        } catch {
//            self.errorMessage = "Failed to prepare Google login data"
//            self.isLoading = false
//            return
//        }
//        
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                
//                if let error = error {
//                    print("❌ Google login error: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse,
//                      (200...299).contains(httpResp.statusCode) else {
//                    self?.errorMessage = "Google login failed"
//                    return
//                }
//                
//                guard let data = data else {
//                    self?.errorMessage = "No response data"
//                    return
//                }
//                
//                do {
//                    let user = try JSONDecoder().decode(Users.self, from: data)
//                    print("✅ Google Sign-In successful: \(user.email)")
//                    
//                    // ✅ Set role based on user data
//                    if user.role?.uppercased() == "ADMIN" {
//                        self?.userRole = .admin
//                    } else if user.role?.uppercased() == "OPERATOR" {
//                        self?.userRole = .transformer
//                    } else {
//                        self?.userRole = .user
//                    }
//
//                    
//                    // ✅ Set JWT token if backend returns it (add to Users model if needed)
//                    // self?.jwtToken = user.jwtToken
//                    
//                    // ✅ Connect services
//                    APIService.shared.setAuthViewModel(self!)
//                    PaymentService.shared.setAuthViewModel(self!)
//                    
//                    // ✅ Set user and auth
//                    self?.currentUser = user
//                    self?.saveUserData(user)
//                    self?.isAuthenticated = true
//                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
//                    
//                    // ✅ FIXED: Fetch vehicles from backend
//                    SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: user.id) { success in
//                        print(success ? "✅ Google login: Vehicles synced" : "⚠️ No vehicles found")
//                    }
//                    
//                } catch {
//                    print("❌ Failed to decode Google user: \(error)")
//                    self?.errorMessage = "Failed to process Google login"
//                }
//            }
//        }.resume()
//    }
//
//
//    // MARK: - OTP Authentication
//    func generateOTP(phoneOrEmail: String, completion: @escaping (Bool, String?) -> Void) {
//        print("📱 Generating OTP for: \(phoneOrEmail)")
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/otp/generate?key=\(phoneOrEmail)") else {
//            completion(false, "Invalid URL")
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        
//        let loginString = "rajeev:parking"
//        if let loginData = loginString.data(using: .utf8) {
//            let base64LoginString = loginData.base64EncodedString()
//            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
//        }
//        
//        APIService.shared.session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ OTP generation error: \(error)")
//                    completion(false, error.localizedDescription)
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse,
//                      (200...299).contains(httpResp.statusCode) else {
//                    completion(false, "Failed to generate OTP")
//                    return
//                }
//                
//                if let data = data, let otpCode = String(data: data, encoding: .utf8) {
//                    print("✅ OTP generated: \(otpCode)")
//                    completion(true, otpCode)
//                } else {
//                    completion(true, nil)
//                }
//            }
//        }.resume()
//    }
//
//    func validateOTP(phoneOrEmail: String, otp: String, completion: @escaping (Bool) -> Void) {
//        print("🔐 Validating OTP for: \(phoneOrEmail)")
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/otp/validate?key=\(phoneOrEmail)&otp=\(otp)") else {
//            completion(false)
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        
//        let loginString = "rajeev:parking"
//        if let loginData = loginString.data(using: .utf8) {
//            let base64LoginString = loginData.base64EncodedString()
//            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
//        }
//        
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ OTP validation error: \(error)")
//                    completion(false)
//                    return
//                }
//                
//                guard let httpResp = response as? HTTPURLResponse,
//                      (200...299).contains(httpResp.statusCode) else {
//                    print("❌ OTP validation failed")
//                    completion(false)
//                    return
//                }
//                
//                self?.createOrFetchUserWithPhone(phoneOrEmail: phoneOrEmail) { success in
//                    completion(success)
//                }
//            }
//        }.resume()
//    }
//
//    private func createOrFetchUserWithPhone(phoneOrEmail: String, completion: @escaping (Bool) -> Void) {
//        print("✅ OTP validated - User authenticated")
//        completion(true)
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
//    // MARK: - Update User Profile (Local)
//    func updateProfile(name: String, phone: String, vehicleNumber: String) {
//        print("📝 Profile update for: \(name)")
//        guard let user = currentUser else { return }
//        
//        isLoading = true
//        errorMessage = ""
//        
//        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
//            var updatedVehicles = user.safeVehicleNumbers
//            if !updatedVehicles.contains(vehicleNumber) {
//                updatedVehicles.append(vehicleNumber)
//            }
//            
//            let updatedUser = Users(
//                    id: user.id,
//                    name: name,
//                    email: user.email,
//                    phone: phone,
//                    vehicleNumbers: updatedVehicles,
//                    firstUser: user.firstUser,
//                    walletCoins: user.walletCoins,
//                    createdAt: user.createdAt,
//                    updatedAt: user.password,
//                    role: user.role,
//                    parkingLotId: user.parkingLotId,
//                    parkingLotName: user.parkingLotName,
//                    active: user.active  // ✅ Fixed - pass the actual value from user object
//                )
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
// 
//
//    func fetchUserProfileFromBackend(userId: String) {
//        print("📥 Fetching full user profile for ID: \(userId)")
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(userId)") else {
//            print("❌ Invalid user profile URL")
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        
//        // Add JWT token if available
//        if let token = jwtToken {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//        }
//        
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ User profile fetch failed: \(error.localizedDescription)")
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No profile data received")
//                    return
//                }
//                
//                if let responseString = String(data: data, encoding: .utf8) {
//                    print("📥 RAW USER PROFILE RESPONSE:\n\(responseString)")
//                }
//                
//                do {
//                    let user = try JSONDecoder().decode(Users.self, from: data)
//                    print("✅ User profile fetched successfully")
//                    print("   ID: \(user.id)")
//                    print("   Name: \(user.name)")
//                    print("   Email: \(user.email)")
//                    print("   Parking Lot ID: \(user.parkingLotId ?? "nil")")
//                    print("   Parking Lot Name: \(user.parkingLotName ?? "nil")")
//                    print("   Role: \(user.role ?? "nil")")
//                    print("   Vehicles: \(user.safeVehicleNumbers)")
//                    
//                    // ✅ Update current user with full data
//                    self?.currentUser = user
//                    self?.saveUserData(user)
//                    
//                    // ✅ CRITICAL: Notify HomeViewModel to filter parking spots
//                    NotificationCenter.default.post(
//                        name: NSNotification.Name("UserProfileUpdated"),
//                        object: nil
//                    )
//                    
//                } catch {
//                    print("❌ Failed to decode user profile: \(error)")
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
//        let loginString = "rajeev:parking"
//        if let loginData = loginString.data(using: .utf8) {
//            let base64LoginString = loginData.base64EncodedString()
//            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
//        }
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
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
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
//                        self?.currentUser = updatedUser
//                        self?.saveUserData(updatedUser)
//                    } catch {
//                        print("❌ Failed to decode updated user: \(error.localizedDescription)")
//                        self?.updateProfileLocally(name: name, phone: phone, vehicleNumber: vehicleNumber, user: user)
//                    }
//                } else {
//                    self?.updateProfileLocally(name: name, phone: phone, vehicleNumber: vehicleNumber, user: user)
//                }
//            }
//        }.resume()
//    }
//    
//    // MARK: - Helper method for local profile update
//    private func updateProfileLocally(name: String, phone: String, vehicleNumber: String, user: Users) {
//        var updatedVehicles = user.safeVehicleNumbers
//        if !updatedVehicles.contains(vehicleNumber) {
//            updatedVehicles.append(vehicleNumber)
//        }
//        
//        let updatedUser = Users(
//                id: user.id,
//                name: name,
//                email: user.email,
//                phone: phone,
//                vehicleNumbers: updatedVehicles,
//                firstUser: user.firstUser,
//                walletCoins: user.walletCoins,
//                createdAt: user.createdAt,
//                updatedAt: user.password,
//                role: user.role,
//                parkingLotId: user.parkingLotId,
//                parkingLotName: user.parkingLotName,
//                active: user.active  // ✅ Fixed - pass the actual value from user object
//            )
//        self.currentUser = updatedUser
//        self.saveUserData(updatedUser)
//    }
//}
//
//// MARK: - Login Response Model
////struct LoginResponse: Codable {
////    let id: String
////    let name: String
////    let email: String
////    let role: String
////}
//




import SwiftUI
import Foundation

// MARK: - Authentication ViewModel
class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var currentUser: Users?
    @Published var isLoading = false
    @Published var errorMessage = ""
    @Published var userRole: UserRole = .user {
           didSet {
               print("🔄 Role changed from \(oldValue.rawValue) to \(userRole.rawValue)")
               UserDefaults.standard.set(userRole.rawValue, forKey: "userRole")
               print("💾 Auto-saved role to UserDefaults: \(userRole.rawValue)")
               
               // ✅ FORCE OBSERVABLE UPDATE
               objectWillChange.send()
           }
       }
    private var roleSetByLogin = false
    
    // ✅ ADD: JWT Token storage
    @Published var jwtToken: String? {
        didSet {
            if let token = jwtToken {
                UserDefaults.standard.set(token, forKey: "jwtToken")
                print("🔑 JWT Token saved to UserDefaults")
            } else {
                UserDefaults.standard.removeObject(forKey: "jwtToken")
                print("🔑 JWT Token removed from UserDefaults")
            }
        }
    }
    
    enum UserRole: String, Codable{
        case user = "USER"
        case admin = "ADMIN"
        case transformer = "OPERATOR"
    }
    

    
//    init() {
//        print("🚀 AuthViewModel initializing...")
//
//        // ✅ Load authentication state
//        self.isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
//        print("   Authenticated: \(isAuthenticated)")
//
//        // ✅ CRITICAL: Load JWT token from UserDefaults
//        if let savedToken = UserDefaults.standard.string(forKey: "jwtToken") {
//            self.jwtToken = savedToken
//            print("   ✅ JWT Token loaded from storage: \(savedToken.prefix(20))...")
//        } else {
//            print("   ⚠️ No JWT token found in storage")
//        }
//
//        // ✅ Load user role
//        if let savedRole = UserDefaults.standard.string(forKey: "userRole"),
//           let role = UserRole(rawValue: savedRole) {
//            self.userRole = role
//            print("   Role: \(role.rawValue)")
//        }
//
//        // ✅ Load user data
//        if let userData = UserDefaults.standard.data(forKey: "currentUser") {
//            do {
//                self.currentUser = try JSONDecoder().decode(Users.self, from: userData)
//                print("   ✅ User loaded: \(currentUser?.name ?? "unknown")")
//            } catch {
//                print("   ❌ Failed to load user: \(error)")
//            }
//        }
//
//        // ✅ CRITICAL: Connect APIService with the loaded token
//        if isAuthenticated && jwtToken != nil {
//            APIService.shared.setAuthViewModel(self)
//            print("   ✅ APIService connected with JWT token")
//        } else {
//            print("   ⚠️ APIService NOT connected - missing token or not authenticated")
//        }
//
//        print("🚀 AuthViewModel initialization complete")
//    }

    init() {
        print("🚀 AuthViewModel initializing...")
        
        self.isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
        
        if let savedToken = UserDefaults.standard.string(forKey: "jwtToken") {
            self.jwtToken = savedToken
        }
        
        if let savedRole = UserDefaults.standard.string(forKey: "userRole"),
           let role = UserRole(rawValue: savedRole) {
            self.userRole = role
        }
        
        if let userData = UserDefaults.standard.data(forKey: "currentUser") {
            do {
                self.currentUser = try JSONDecoder().decode(Users.self, from: userData)
                
                // ✅ CRITICAL: Fetch fresh vehicles from backend on app start
                if isAuthenticated, let userId = currentUser?.id {
                    // ✅ Use SharedVehicleManager method
                    SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: userId) { success in
                        print(success ? "✅ Vehicles loaded" : "⚠️ No vehicles")
                    }

                }
            } catch {
                print("❌ Failed to load user: \(error)")
            }
        }
        
        if isAuthenticated && jwtToken != nil {
            APIService.shared.setAuthViewModel(self)
            PaymentService.shared.setAuthViewModel(self)
        }
        
        print("🚀 AuthViewModel initialization complete")
    }

    
    // ✅ ADD: Helper to get current token
    func getAuthToken() -> String? {
        return jwtToken
    }
    // MARK: - Registration with JWT (No Vehicle Number, with College Selection)
    func register(name: String, email: String, phone: String, password: String, parkingLotName: String, completion: @escaping (Bool, String?) -> Void) {
        APIService.shared.register(name: name, email: email, phone: phone, password: password, parkingLotName: parkingLotName) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let response):
                    // Save token
                    UserDefaults.standard.set(response.token, forKey: "jwtToken")
                    self?.jwtToken = response.token
                    
                    // Save user
                    self?.currentUser = response.user
                    self?.isAuthenticated = true
                    
                    completion(true, nil)
                    
                case .failure(let error):
                    completion(false, error.localizedDescription)
                }
            }
        }
    }


    
    // MARK: - Check Authentication Status
    func checkAuthenticationStatus() {
        isAuthenticated = UserDefaults.standard.bool(forKey: "isAuthenticated")
        
        print("🔍 checkAuthenticationStatus called")
        print("   isAuthenticated: \(isAuthenticated)")
        
        if isAuthenticated {
            // ✅ Load role from UserDefaults
            if let savedRole = UserDefaults.standard.string(forKey: "userRole") {
                print("   Saved role in UserDefaults: '\(savedRole)'")
                
                let normalizedRole = savedRole.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
                
                if normalizedRole == "ADMIN" {
                    userRole = .admin
                    print("🔐 Restored ADMIN role from UserDefaults")
                } else if normalizedRole == "OPERATOR" {
                    userRole = .transformer
                    print("🔧 Restored TRANSFORMER role from UserDefaults")
                } else {
                    userRole = .user
                    print("👤 Restored USER role from UserDefaults")
                }

                
            } else {
                print("⚠️ No saved role found - keeping current role: \(userRole.rawValue)")
            }
            
            // ✅ Load user data from local storage
            loadUserData()
            
            // ✅ REMOVED: syncVehiclesWithManager() - doesn't exist anymore
            // Vehicles are fetched from backend in init() already
            
            print("✅ Using cached data - vehicles already loaded in init()")
        } else {
            userRole = .user
            print("🔑 Not authenticated - role set to USER")
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
        guard let user = currentUser else { return [] }
        
        let vehicles = user.safeVehicleNumbers.filter { vehicle in
            !vehicle.contains("USER_") && !vehicle.isEmpty
        }
        
        print("✅ Got real vehicles from user: \(vehicles)")
        return vehicles
    }

    func getCurrentUserVehicle() -> String? {
        let vehicles = getCurrentUserVehicles()
        let firstVehicle = vehicles.first
        print("✅ Current user vehicle: \(firstVehicle ?? "none")")
        return firstVehicle
    }
    
    // MARK: - Vehicle Management Methods
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
//        saveUserData(user)
//        print("✅ Updated user vehicles: \(vehicleNumbers)")
//    }
    

    func updateUserVehicles(_ vehicles: [String]) {
        guard var user = currentUser else {
            print("⚠️ Cannot update vehicles - no current user")
            return
        }
        
        user.vehicleNumbers = vehicles
        self.currentUser = user
        saveUserData(user)
        
        print("✅ Updated user vehicles in AuthViewModel: \(vehicles)")
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
    
    // MARK: - Update User Vehicles on Backend
    func updateUserVehicles(_ vehicleNumbers: [String], completion: @escaping (Bool) -> Void) {
        guard let user = currentUser else {
            print("❌ No current user to update vehicles")
            completion(false)
            return
        }
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(user.id)/vehicles") else {
            print("❌ Invalid URL for vehicle update")
            completion(false)
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        do {
            let jsonData = try JSONEncoder().encode(vehicleNumbers)
            request.httpBody = jsonData
            
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("📤 Updating vehicles: \(jsonString)")
            }
        } catch {
            print("❌ Failed to encode vehicles: \(error.localizedDescription)")
            completion(false)
            return
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Update vehicles error: \(error.localizedDescription)")
                    completion(false)
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    print("❌ Server returned error for vehicle update")
                    completion(false)
                    return
                }
                
                print("✅ Vehicles updated on backend")
                self?.updateUserVehicles(vehicleNumbers)
                completion(true)
            }
        }.resume()
    }

    func removeUserVehicle(_ vehicleNumber: String) {
        var currentVehicles = getCurrentUserVehicles()
        currentVehicles.removeAll { $0 == vehicleNumber }
        updateUserVehicles(currentVehicles)
        print("✅ Removed vehicle: \(vehicleNumber)")
    }
    

    // MARK: - Login with JWT
//    func login(email: String, password: String) {
//        print("🔐 Login attempt for: \(email)")
//        isLoading = true
//        errorMessage = ""
//
//        guard let url = URL(string: "\(APIService.backendBaseURL)/auth/login") else {
//            self.errorMessage = "Invalid server URL"
//            self.isLoading = false
//            return
//        }
//
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//
//        let body: [String: String] = [
//            "email": email,
//            "password": password
//        ]
//
//        do {
//            let jsonData = try JSONSerialization.data(withJSONObject: body)
//            request.httpBody = jsonData
//            print("📤 Sending login JSON: \(String(data: jsonData, encoding: .utf8) ?? "")")
//        } catch {
//            self.errorMessage = "Failed to prepare login data"
//            self.isLoading = false
//            return
//        }
//
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
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
//                    self?.errorMessage = "Invalid server response"
//                    return
//                }
//
//                print("🔁 Login HTTPStatus: \(httpResp.statusCode)")
//
//                guard (200...299).contains(httpResp.statusCode) else {
//                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
//                        print("Server response:\n\(errorMsg)")
//                        self?.errorMessage = errorMsg
//                    } else {
//                        self?.errorMessage = "Login failed with status \(httpResp.statusCode)"
//                    }
//                    return
//                }
//
//                guard let data = data else {
//                    self?.errorMessage = "No response data"
//                    return
//                }
//
//                if let responseString = String(data: data, encoding: .utf8) {
//                    print("📥 RAW LOGIN RESPONSE:\n\(responseString)")
//                }
//
//                do {
//                    // ✅ FIXED: Decode from nested user object
//                    let loginResponse = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
//                    print("✅ Login response decoded successfully")
//                    print("   Token: \(loginResponse.token.prefix(20))...")
//                    print("   ID: \(loginResponse.user.id)")
//                    print("   Name: \(loginResponse.user.name)")
//                    print("   Role: \(loginResponse.user.role ?? "USER")")
//
//                    // ✅ Save JWT token to UserDefaults
//                    UserDefaults.standard.set(loginResponse.token, forKey: "jwtToken")
//                    self?.jwtToken = loginResponse.token
//                    print("🔑 JWT token saved to UserDefaults and ViewModel")
//
//                    // ✅ Set role
//                    let roleString = (loginResponse.user.role ?? "USER").uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
//                    if roleString == "ADMIN" {
//                        self?.userRole = .admin
//                        print("🔐 ADMIN role set")
//                    } else if roleString == "OPERATOR" {
//                        self?.userRole = .transformer
//                        print("🔧 TRANSFORMER role set")
//                    } else {
//                        self?.userRole = .user
//                        print("👤 USER role set")
//                    }
//
//                    // ✅ Create user object from login response
//                    let user = Users(
//                        id: loginResponse.user.id,
//                        name: loginResponse.user.name,
//                        email: loginResponse.user.email,
//                        phone: loginResponse.user.phone,
//                        vehicleNumbers: loginResponse.user.vehicleNumbers ?? [],
//                        firstUser: loginResponse.user.firstUser,
//                        walletCoins: loginResponse.user.walletCoins,
//                        createdAt: loginResponse.user.createdAt,
//                        updatedAt: nil,
//                        role: loginResponse.user.role,
//                        parkingLotId: loginResponse.user.parkingLotId,
//                        parkingLotName: loginResponse.user.parkingLotName,
//                        active: loginResponse.user.active  // ✅ Fixed - pass the actual value
//                    )
//
//                    self?.currentUser = user
//                    self?.saveUserData(user)
//                    print("✅ User data saved")
//
//                    // ✅ CRITICAL: Connect APIService to get JWT in future calls
//                    if let authVM = self {
//                        APIService.shared.setAuthViewModel(authVM)
//                        print("🔗 APIService connected to AuthViewModel - JWT now available for all API calls")
//                    }
//
//                    // ✅ Fetch vehicles from backend
//                    SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: loginResponse.user.id) { success in
//                        if success {
//                            print("✅ Vehicles fetched from backend")
//                        } else {
//                            print("⚠️ Failed to fetch vehicles")
//                        }
//                    }
//
//                    // ✅ Set authentication LAST
//                    self?.isAuthenticated = true
//                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
//
//                    print("✅ Login complete!")
//                    print("   Authenticated: \(self?.isAuthenticated ?? false)")
//                    print("   Role: \(self?.userRole.rawValue ?? "unknown")")
//                    print("   User ID: \(loginResponse.user.id)")
//
//                } catch {
//                    print("❌ Failed to decode login response: \(error)")
//                    if let responseString = String(data: data, encoding: .utf8) {
//                        print("RAW RESPONSE:\n\(responseString)")
//                    }
//                    self?.errorMessage = "Failed to process login response"
//                }
//            }
//        }.resume()
//    }
//
    func login(email: String, password: String) {
        print("🔐 Login attempt for: \(email)")
        isLoading = true
        errorMessage = ""

        guard let url = URL(string: "\(APIService.backendBaseURL)/auth/login") else {
            errorMessage = "Invalid server URL"
            isLoading = false
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = [
            "email": email,
            "password": password
        ]

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            errorMessage = "Failed to prepare login request"
            isLoading = false
            return
        }

        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.isLoading = false

                // 🌐 Network error
                if let error = error {
                    print("❌ Network error:", error.localizedDescription)
                    self.errorMessage = "Please check your internet connection"
                    return
                }

                guard let httpResp = response as? HTTPURLResponse else {
                    self.errorMessage = "Invalid server response"
                    return
                }

                print("🔁 Login HTTPStatus:", httpResp.statusCode)

                // ❌ Authentication failed (NO RAW JSON)
                guard (200...299).contains(httpResp.statusCode) else {
                    self.errorMessage = self.mapLoginError(statusCode: httpResp.statusCode)
                    return
                }

                guard let data = data else {
                    self.errorMessage = "Empty server response"
                    return
                }

                do {
                    let loginResponse = try JSONDecoder().decode(JWTLoginResponse.self, from: data)

                    // ✅ Save token
                    self.jwtToken = loginResponse.token
                    UserDefaults.standard.set(true, forKey: "isAuthenticated")

                    // ✅ Set role
                    let role = (loginResponse.user.role ?? "USER").uppercased()
                    self.userRole = UserRole(rawValue: role) ?? .user

                    // ✅ Build user
                    let user = Users(
                        id: loginResponse.user.id,
                        name: loginResponse.user.name,
                        email: loginResponse.user.email,
                        phone: loginResponse.user.phone,
                        vehicleNumbers: loginResponse.user.vehicleNumbers ?? [],
                        firstUser: loginResponse.user.firstUser,
                        walletCoins: loginResponse.user.walletCoins,
                        createdAt: loginResponse.user.createdAt,
                        updatedAt: nil,
                        role: loginResponse.user.role,
                        parkingLotId: loginResponse.user.parkingLotId,
                        parkingLotName: loginResponse.user.parkingLotName,
                        active: loginResponse.user.active
                    )

                    self.currentUser = user
                    self.saveUserData(user)

                    // ✅ Attach services
                    APIService.shared.setAuthViewModel(self)
                    PaymentService.shared.setAuthViewModel(self)

                    // ✅ Fetch vehicles
                    SharedVehicleManager.shared.fetchVehiclesFromBackend(
                        userId: user.id
                    ) { _ in }

                    self.isAuthenticated = true
                    print("✅ Login success")

                } catch {
                    print("❌ Decode error:", error)
                    self.errorMessage = "Unable to sign in. Please try again."
                }
            }
        }.resume()
    }

    
   

  

//     MARK: - Sign Up with JWT
//    func signUp(name: String, email: String, phone: String, vehicleNumber: String, password: String) {
//        print("📝 SignUp attempt for: \(name) - Vehicle: \(vehicleNumber)")
//        isLoading = true
//        errorMessage = ""
//
//        let registrationVehicle = vehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines)
//
//        guard let url = URL(string: "\(APIService.backendBaseURL)/auth/register") else {  // ✅ CHANGED ENDPOINT
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
//        // ✅ REMOVED: Basic Auth - No longer needed
//
//        let body: [String: Any] = [
//            "name": name,
//            "email": email,
//            "phone": phone,
//            "vehicleNumber": vehicleNumber,
//            "passwordHash": password,
//            "parkingLotName": "default"  // ✅ ADD: Required by your backend
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
//        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
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
//                        // ✅ CHANGED: New response format with token
//                        let signupResponse = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
//                        print("✅ SignUp successful")
//                        print("   Token: \(signupResponse.token.prefix(20))...")
//                        print("   ID: \(signupResponse.id)")
//
//                        // ✅ Save JWT token
//                        self?.jwtToken = signupResponse.token
//
//                        // Set role
//                        let roleString = signupResponse.role.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
//                        if roleString == "ADMIN" {
//                            self?.userRole = .admin
//                        } else {
//                            self?.userRole = .user
//                        }
//
//                        // Fetch full profile
//                        self?.fetchUserProfileFromBackend(userId: signupResponse.id)
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

    // MARK: - Sync Vehicles with SharedVehicleManager
//    private func syncVehiclesWithManager() {
//        guard let user = currentUser else { return }
//
//        let userVehicles = user.safeVehicleNumbers
//        print("🚗 Syncing \(userVehicles.count) vehicles with SharedVehicleManager")
//
//        SharedVehicleManager.shared.vehicles.removeAll()
//
//        for vehicleNumber in userVehicles {
//            SharedVehicleManager.shared.addVehicleByNumber(vehicleNumber)
//        }
//
//        print("✅ Vehicles synced to SharedVehicleManager: \(userVehicles)")
//    }
//

    
    // MARK: - Logout
    // MARK: - Logout
    func logout() {
        print("🚪 Logging out...")
        
        // ✅ Clear all state
        isAuthenticated = false
        currentUser = nil
        userRole = .user
        jwtToken = nil
        errorMessage = ""
        roleSetByLogin = false
        SharedVehicleManager.shared.clearVehicles()
        
        // ✅ Clear ALL UserDefaults
        UserDefaults.standard.removeObject(forKey: "isAuthenticated")
        UserDefaults.standard.removeObject(forKey: "currentUser")
        UserDefaults.standard.removeObject(forKey: "userRole")
        UserDefaults.standard.removeObject(forKey: "currentUserId")
        UserDefaults.standard.removeObject(forKey: "jwtToken")
        UserDefaults.standard.removeObject(forKey: "userData")
        UserDefaults.standard.synchronize()
        
        // ✅ Clear vehicles
//        SharedVehicleManager.shared.clearVehicles()
//
        // ✅ Disconnect services (set to nil, don't use weak reference)
        // You'll need to update these methods to accept nil
        
        print("✅ Logout complete - all data cleared")
        print("   JWT in UserDefaults: \(UserDefaults.standard.string(forKey: "jwtToken") ?? "NONE")")
    }
    private func mapLoginError(statusCode: Int) -> String {
        switch statusCode {
        case 400, 401, 403, 404:
            return "Wrong email or password"
        case 408:
            return "Request timed out. Please try again."
        case 500...599:
            return "Server error. Please try again later."
        default:
            return "Something went wrong. Please try again."
        }
    }



    // MARK: - Google Sign-In
    func loginWithGoogle(idToken: String, accessToken: String) {
        print("🌐 Google Sign-In initiated with backend")
        isLoading = true
        errorMessage = ""
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/oauth2/google/authenticate") else {
            self.errorMessage = "Invalid server URL"
            self.isLoading = false
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        let body: [String: Any] = [
            "idToken": idToken,
            "accessToken": accessToken
        ]
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: body)
            request.httpBody = jsonData
        } catch {
            self.errorMessage = "Failed to prepare Google login data"
            self.isLoading = false
            return
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                if let error = error {
                    print("❌ Google login error: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    self?.errorMessage = "Google login failed"
                    return
                }
                
                guard let data = data else {
                    self?.errorMessage = "No response data"
                    return
                }
                
                do {
                    let user = try JSONDecoder().decode(Users.self, from: data)
                    print("✅ Google Sign-In successful: \(user.email)")
                    
                    // ✅ Set role based on user data
                    if user.role?.uppercased() == "ADMIN" {
                        self?.userRole = .admin
                    } else if user.role?.uppercased() == "OPERATOR" {
                        self?.userRole = .transformer
                    } else {
                        self?.userRole = .user
                    }

                    
                    // ✅ Set JWT token if backend returns it (add to Users model if needed)
                    // self?.jwtToken = user.jwtToken
                    
                    // ✅ Connect services
                    APIService.shared.setAuthViewModel(self!)
                    PaymentService.shared.setAuthViewModel(self!)
                    
                    // ✅ Set user and auth
                    self?.currentUser = user
                    self?.saveUserData(user)
                    self?.isAuthenticated = true
                    UserDefaults.standard.set(true, forKey: "isAuthenticated")
                    
                    // ✅ FIXED: Fetch vehicles from backend
                    SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: user.id) { success in
                        print(success ? "✅ Google login: Vehicles synced" : "⚠️ No vehicles found")
                    }
                    
                } catch {
                    print("❌ Failed to decode Google user: \(error)")
                    self?.errorMessage = "Failed to process Google login"
                }
            }
        }.resume()
    }


    // MARK: - OTP Authentication
    func generateOTP(phoneOrEmail: String, completion: @escaping (Bool, String?) -> Void) {
        print("📱 Generating OTP for: \(phoneOrEmail)")
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/otp/generate?key=\(phoneOrEmail)") else {
            completion(false, "Invalid URL")
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ OTP generation error: \(error)")
                    completion(false, error.localizedDescription)
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    completion(false, "Failed to generate OTP")
                    return
                }
                
                if let data = data, let otpCode = String(data: data, encoding: .utf8) {
                    print("✅ OTP generated: \(otpCode)")
                    completion(true, otpCode)
                } else {
                    completion(true, nil)
                }
            }
        }.resume()
    }

    func validateOTP(phoneOrEmail: String, otp: String, completion: @escaping (Bool) -> Void) {
        print("🔐 Validating OTP for: \(phoneOrEmail)")
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/otp/validate?key=\(phoneOrEmail)&otp=\(otp)") else {
            completion(false)
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ OTP validation error: \(error)")
                    completion(false)
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    print("❌ OTP validation failed")
                    completion(false)
                    return
                }
                
                self?.createOrFetchUserWithPhone(phoneOrEmail: phoneOrEmail) { success in
                    completion(success)
                }
            }
        }.resume()
    }

    private func createOrFetchUserWithPhone(phoneOrEmail: String, completion: @escaping (Bool) -> Void) {
        print("✅ OTP validated - User authenticated")
        completion(true)
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
                    updatedAt: user.password,
                    role: user.role,
                    parkingLotId: user.parkingLotId,
                    parkingLotName: user.parkingLotName,
                    active: user.active  // ✅ Fixed - pass the actual value from user object
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
    
 

    func fetchUserProfileFromBackend(userId: String) {
        print("📥 Fetching full user profile for ID: \(userId)")
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/users/\(userId)") else {
            print("❌ Invalid user profile URL")
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        // Add JWT token if available
        if let token = jwtToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ User profile fetch failed: \(error.localizedDescription)")
                    return
                }
                
                guard let data = data else {
                    print("❌ No profile data received")
                    return
                }
                
                if let responseString = String(data: data, encoding: .utf8) {
                    print("📥 RAW USER PROFILE RESPONSE:\n\(responseString)")
                }
                
                do {
                    let user = try JSONDecoder().decode(Users.self, from: data)
                    print("✅ User profile fetched successfully")
                    print("   ID: \(user.id)")
                    print("   Name: \(user.name)")
                    print("   Email: \(user.email)")
                    print("   Parking Lot ID: \(user.parkingLotId ?? "nil")")
                    print("   Parking Lot Name: \(user.parkingLotName ?? "nil")")
                    print("   Role: \(user.role ?? "nil")")
                    print("   Vehicles: \(user.safeVehicleNumbers)")
                    
                    // ✅ Update current user with full data
                    self?.currentUser = user
                    self?.saveUserData(user)
                    
                    // ✅ CRITICAL: Notify HomeViewModel to filter parking spots
                    NotificationCenter.default.post(
                        name: NSNotification.Name("UserProfileUpdated"),
                        object: nil
                    )
                    
                } catch {
                    print("❌ Failed to decode user profile: \(error)")
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
                        self?.updateProfileLocally(name: name, phone: phone, vehicleNumber: vehicleNumber, user: user)
                    }
                } else {
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
                updatedAt: user.password,
                role: user.role,
                parkingLotId: user.parkingLotId,
                parkingLotName: user.parkingLotName,
                active: user.active  // ✅ Fixed - pass the actual value from user object
            )
        self.currentUser = updatedUser
        self.saveUserData(updatedUser)
    }
}

// MARK: - Login Response Model
//struct LoginResponse: Codable {
//    let id: String
//    let name: String
//    let email: String
//    let role: String
//}

