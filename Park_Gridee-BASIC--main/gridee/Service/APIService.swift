//import Foundation
//
//enum APIError: Error {
//    case badURL
//    case badResponse
//    case decodingError(Error)
//    case serverError(Int)
//    case unknown(Error)
//    case authenticationRequired
//    case invalidURL
//    case networkError(Error)
//    case noData
//    case invalidResponse
//
//    var localizedDescription: String {
//        switch self {
//        case .badURL:
//            return "Invalid URL"
//        case .badResponse:
//            return "Bad response"
//        case .decodingError(let error):
//            return "Decode error: \(error.localizedDescription)"
//        case .serverError(let code):
//            return "Server error: \(code)"
//        case .unknown(let error):
//            return "Unknown error: \(error.localizedDescription)"
//        case .authenticationRequired:
//            return "Authentication required"
//        case .invalidURL:
//            return "Invalid URL"
//        case .networkError(let error):
//            return "Network error: \(error.localizedDescription)"
//        case .noData:
//            return "No data returned"
//        case .invalidResponse:
//            return "Invalid Response"
//        }
//    }
//}
// 
//// MARK: - SSL Delegate
//class SSLAllowingSessionDelegate: NSObject, URLSessionDelegate {
//    func urlSession(_ session: URLSession,
//                    didReceive challenge: URLAuthenticationChallenge,
//                    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
//        
//        let authMethod = challenge.protectionSpace.authenticationMethod
//        
//        if authMethod == NSURLAuthenticationMethodServerTrust {
//            if let serverTrust = challenge.protectionSpace.serverTrust {
//                print("✅ Accepting SSL certificate for: \(challenge.protectionSpace.host)")
//                completionHandler(.useCredential, URLCredential(trust: serverTrust))
//                return
//            }
//        }
//        
//        if authMethod == NSURLAuthenticationMethodHTTPBasic {
//            if let username = UserDefaults.standard.string(forKey: "api_username"),
//               let password = UserDefaults.standard.string(forKey: "api_password") {
//                print("✅ Using Basic Auth credentials")
//                let credential = URLCredential(user: username, password: password, persistence: .forSession)
//                completionHandler(.useCredential, credential)
//                return
//            }
//        }
//        
//        completionHandler(.performDefaultHandling, nil)
//    }
//}
//
//// MARK: - QR Validation Result
//
//// MARK: - API Service
//class APIService {
//    static let shared = APIService()
////    s/*tatic let backendBaseURL = "https://172.20.10.4:8443/api"*/
////    static let backendBaseURL = "https://gridee.onrender.com/api"
//    static let backendBaseURL = "https://gridee.onrender.com/api"
////    static let backendBaseURL = "http://127.0.0.1:8080/api"
//
//    let session: URLSession
//    
//    private let apiUsername = "rajeev"
//    private let apiPassword = "parking"
//    
//    // ✅ JWT Token reference
//    var authViewModel: AuthViewModel?
//    
//    private init() {
//        UserDefaults.standard.set(apiUsername, forKey: "api_username")
//        UserDefaults.standard.set(apiPassword, forKey: "api_password")
//        
//        let config = URLSessionConfiguration.default
//        config.timeoutIntervalForRequest = 30
//        config.timeoutIntervalForResource = 60
//        
//        self.session = URLSession(configuration: config, delegate: SSLAllowingSessionDelegate(), delegateQueue: nil)
//        
//        print("🚀 APIService initialized with credentials: \(apiUsername)")
//        print("🌐 Backend URL: \(APIService.backendBaseURL)")
//    }
//    
//    // ✅ Set AuthViewModel reference
//    func setAuthViewModel(_ viewModel: AuthViewModel) {
//        self.authViewModel = viewModel
//        print("🔗 AuthViewModel connected to APIService")
//        if let token = viewModel.jwtToken {
//            print("🔑 JWT Token available: \(token.prefix(20))...")
//        } else {
//            print("⚠️ No JWT token available yet")
//        }
//    }
//    private let publicEndpoints = [
//        "/auth/register",
//        "/auth/login",
//        "/parking-lots",
//        "/parking-lots/list/by-names"
//    ]
//    
//    // ✅ Check if endpoint is public (no JWT required)
//    private func isPublicEndpoint(_ endpoint: String) -> Bool {
//        return publicEndpoints.contains { endpoint.contains($0) }
//    }
//    
//    // MARK: - Authentication Headers
//    
//    // ✅ Smart auth header - tries JWT first, falls back to Basic Auth
//    private func createAuthHeader() -> String {
//        // Priority 1: Try JWT token
//        if let token = authViewModel?.jwtToken {
//            print("🔑 Using JWT Bearer token: \(token.prefix(20))...")
//            return "Bearer \(token)"
//        }
//        
//        // Priority 2: Fallback to Basic Auth
//        print("🔑 Using Basic Auth (no JWT token available)")
//        return createBasicAuthHeader()
//    }
//    
//    // ✅ Basic Auth fallback
//    private func createBasicAuthHeader() -> String {
//        let loginString = "\(apiUsername):\(apiPassword)"
//        guard let loginData = loginString.data(using: .utf8) else {
//            print("❌ Failed to create auth data")
//            return ""
//        }
//        let base64LoginString = loginData.base64EncodedString()
//        return "Basic \(base64LoginString)"
//    }
//    
//    // ✅ Helper to add auth header
//    //    private func addAuthHeader(to request: inout URLRequest) {
//    //        request.setValue(createAuthHeader(), forHTTPHeaderField: "Authorization")
//    //    }
//    // ✅ FIXED: Make addAuthHeader work with static method
//    func addAuthHeader(to request: inout URLRequest, endpoint: String = "") {
//        if isPublicEndpoint(endpoint) {
//            print("🌐 PUBLIC: \(endpoint)")
//            return
//        }
//        
//        // ✅ Try JWT first
//        if let token = UserDefaults.standard.string(forKey: "jwtToken"), !token.isEmpty {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("✅ JWT: \(token.prefix(30))...")
//            return
//        }
//        
//        // ⚠️ TEST: If JWT fails, try Basic Auth
//        print("⚠️ Falling back to Basic Auth")
//        let loginString = "rajeev:parking"
//        if let loginData = loginString.data(using: .utf8) {
//            let base64 = loginData.base64EncodedString()
//            request.setValue("Basic \(base64)", forHTTPHeaderField: "Authorization")
//            print("✅ Basic Auth added")
//        }
//    }
//    
//    
//    
//    // ✅ FIXED: Static method properly sets JWT
////    static func fetchParkingSpots(completion: @escaping ([ParkingSpot]?, Error?) -> Void) {
////        guard let url = URL(string: backendBaseURL + "/parking-spots") else {
////            completion(nil, APIError.badURL)
////            return
////        }
////        
////        var request = URLRequest(url: url)
////        request.httpMethod = "GET"
////        request.setValue("application/json", forHTTPHeaderField: "Accept")
////        
////        // ✅ Use the helper function
////        shared.addAuthHeader(to: &request, endpoint: "/parking-spots")
////        
////        print("🚀 Fetching parking spots with JWT: \(request.value(forHTTPHeaderField: "Authorization") != nil ? "YES" : "NO")")
////        
////        shared.session.dataTask(with: request) { data, response, error in
////            DispatchQueue.main.async {
////                guard let httpResponse = response as? HTTPURLResponse else {
////                    completion(nil, APIError.badResponse)
////                    return
////                }
////                
////                if httpResponse.statusCode == 403 {
////                    print("❌ 403 Forbidden - JWT token rejected by backend")
////                    print("   Check if token is expired or invalid")
////                    completion(nil, APIError.authenticationRequired)
////                    return
////                }
////                
////                if let error = error {
////                    completion(nil, error)
////                    return
////                }
////                
////                guard (200...299).contains(httpResponse.statusCode) else {
////                    completion(nil, APIError.serverError(httpResponse.statusCode))
////                    return
////                }
////                
////                guard let data = data else {
////                    completion(nil, APIError.noData)
////                    return
////                }
////                
////                do {
////                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
////                    print("✅ Got \(spots.count) parking spots")
////                    completion(spots, nil)
////                } catch {
////                    completion(nil, error)
////                }
////            }
////        }.resume()
////    }
////
//    static func fetchParkingSpots(completion: @escaping (Result<[ParkingSpot], APIError>) -> Void) {
//        // Build URL
//        let endpoint = "/parking-spots"
//        guard let url = URL(string: backendBaseURL + endpoint) else {
//            print("❌ fetchParkingSpots: Invalid URL")
//            completion(.failure(.badURL))
//            return
//        }
//
//        // Build request
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//
//        // 🔑 Attach JWT (same as wallet call)
//        if let token = UserDefaults.standard.string(forKey: "jwtToken"), !token.isEmpty {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("🔑 fetchParkingSpots using JWT: \(token.prefix(20))…")
//        } else {
//            print("❌ fetchParkingSpots: No JWT token found")
//            completion(.failure(.authenticationRequired))
//            return
//        }
//
//        print("🚀 GET \(url.absoluteString)")
//
//        // Execute request
//        shared.session.dataTask(with: request) { data, response, error in
//            // Network / transport error
//            if let error = error {
//                print("❌ fetchParkingSpots network error: \(error.localizedDescription)")
//                completion(.failure(.networkError(error)))
//                return
//            }
//
//            // Response validation
//            guard let httpResponse = response as? HTTPURLResponse else {
//                print("❌ fetchParkingSpots: No HTTPURLResponse")
//                completion(.failure(.badResponse))
//                return
//            }
//
//            print("📡 fetchParkingSpots status: \(httpResponse.statusCode)")
//
//            if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//                print("❌ fetchParkingSpots: Auth failure (status \(httpResponse.statusCode))")
//                completion(.failure(.authenticationRequired))
//                return
//            }
//
//            guard (200...299).contains(httpResponse.statusCode) else {
//                print("❌ fetchParkingSpots: Server error \(httpResponse.statusCode)")
//                completion(.failure(.serverError(httpResponse.statusCode)))
//                return
//            }
//
//            guard let data = data else {
//                print("❌ fetchParkingSpots: No data")
//                completion(.failure(.noData))
//                return
//            }
//
//            // Decode JSON
//            do {
//                let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
//                print("✅ fetchParkingSpots decoded \(spots.count) spots")
//                completion(.success(spots))
//            } catch {
//                print("❌ fetchParkingSpots decoding error: \(error.localizedDescription)")
//                completion(.failure(.decodingError(error)))
//            }
//        }.resume()
//    }
//
//    
//    // MARK: - Users
//    func fetchUsers(completion: @escaping (Result<[Users], APIError>) -> Void) {
//        makeRequest(endpoint: "/users", completion: completion)
//    }
//    
//    func register(name: String, email: String, phone: String, password: String, parkingLotName: String, completion: @escaping (Result<JWTLoginResponse, APIError>) -> Void) {
//        guard let url = URL(string: APIService.backendBaseURL + "/auth/register") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        let body: [String: Any] = [
//            "name": name,
//            "email": email,
//            "phone": phone,
//            "password": password,
//            "parkingLotName": parkingLotName
//        ]
//        
//        guard let jsonData = try? JSONSerialization.data(withJSONObject: body) else {
//            completion(.failure(.badResponse))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.httpBody = jsonData
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse,
//                      (200...299).contains(httpResponse.statusCode),
//                      let data = data else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                do {
//                    let response = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
//                    completion(.success(response))
//                } catch {
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    
//    // MARK: - Parking Spots
//    private func getParkingSpots(completion: @escaping ([ParkingSpot]?, Error?) -> Void) {
//        guard let url = URL(string: Self.backendBaseURL + "/parking-spots") else {
//            print("❌ Invalid parking spots URL")
//            completion(nil, APIError.badURL)
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        
//        // ✅ ADD JWT TOKEN
//        if let token = UserDefaults.standard.string(forKey: "jwtToken") {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("🔑 JWT token added to parking spots request")
//        } else if let token = authViewModel?.jwtToken {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("🔑 JWT token added to parking spots request")
//        }
//        
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
//        
//        print("🚀 Fetching parking spots from: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error.localizedDescription)")
//                    completion(nil, error)
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid HTTP response")
//                    completion(nil, APIError.badResponse)
//                    return
//                }
//                
//                print("📡 Parking spots response status: \(httpResponse.statusCode)")
//                
//                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//                    print("❌ Authentication failed for parking spots")
//                    completion(nil, APIError.authenticationRequired)
//                    return
//                }
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    print("❌ Server error: \(httpResponse.statusCode)")
//                    completion(nil, APIError.serverError(httpResponse.statusCode))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No data received")
//                    completion(nil, APIError.badResponse)
//                    return
//                }
//                
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📋 Raw parking spots response:")
//                    print(jsonString)
//                }
//                
//                do {
//                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
//                    print("✅ Successfully decoded \(spots.count) parking spots")
//                    completion(spots, nil)
//                } catch {
//                    print("❌ JSON decoding error: \(error)")
//                    completion(nil, error)
//                }
//            }
//        }.resume()
//    }
//    
////    func fetchAvailableSpots(
////        lotId: String,
////        startTime: Date,
////        endTime: Date,
////        completion: @escaping (Result<[ParkingSpot], APIError>) -> Void
////    ) {
////        let formatter = ISO8601DateFormatter()
////        formatter.formatOptions = [.withInternetDateTime]
////        formatter.timeZone = TimeZone.current
////        
////        let startTimeString = formatter.string(from: startTime)
////        let endTimeString = formatter.string(from: endTime)
////        
////        guard let encodedStartTime = startTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
////              let encodedEndTime = endTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
////            completion(.failure(.badURL))
////            return
////        }
////        
////        let endpoint = "/parking-spots/available?lotId=\(lotId)&startTime=\(encodedStartTime)&endTime=\(encodedEndTime)"
////        
////        print("📡 Fetching available spots:")
////        print("   Endpoint: \(endpoint)")
////        
////        // ✅ CREATE REQUEST WITH JWT
////        guard let url = URL(string: Self.backendBaseURL + endpoint) else {
////            completion(.failure(.badURL))
////            return
////        }
////        
////        var request = URLRequest(url: url)
////        request.httpMethod = "POST"
////        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
////        
////        // ✅ ADD JWT TOKEN
////        if let token = UserDefaults.standard.string(forKey: "jwtToken") {
////            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
////            print("🔑 JWT token added to available spots request")
////        } else if let token = authViewModel?.jwtToken {
////            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
////            print("🔑 JWT token added to available spots request")
////        }
////        
////        session.dataTask(with: request) { data, response, error in
////            DispatchQueue.main.async {
////                if let error = error {
////                    print("❌ Network error: \(error.localizedDescription)")
////                    completion(.failure(.networkError(error)))
////                    return
////                }
////                
////                guard let httpResponse = response as? HTTPURLResponse else {
////                    completion(.failure(.badResponse))
////                    return
////                }
////                
////                guard (200...299).contains(httpResponse.statusCode) else {
////                    completion(.failure(.serverError(httpResponse.statusCode)))
////                    return
////                }
////                
////                guard let data = data else {
////                    completion(.failure(.noData))
////                    return
////                }
////                
////                do {
////                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
////                    print("✅ Fetched \(spots.count) available spots")
////                    completion(.success(spots))
////                } catch {
////                    print("❌ Decode error: \(error)")
////                    completion(.failure(.decodingError(error)))
////                }
////            }
////        }.resume()
////    }
////
//    func fetchAvailableSpots(
//        lotId: String,
//        startTime: Date,
//        endTime: Date,
//        completion: @escaping (Result<[ParkingSpot], APIError>) -> Void
//    ) {
//        // Format dates to ISO8601
//        let formatter = ISO8601DateFormatter()
//        formatter.formatOptions = [.withInternetDateTime]
//        formatter.timeZone = TimeZone.current
//        
//        let startTimeString = formatter.string(from: startTime)
//        let endTimeString = formatter.string(from: endTime)
//        
//        // URL-encode the query parameters
//        guard let encodedStartTime = startTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
//              let encodedEndTime = endTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
//            print("❌ fetchAvailableSpots: Failed to encode time parameters")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        // Build endpoint
//        let endpoint = "/parking-spots/available?lotId=\(lotId)&startTime=\(encodedStartTime)&endTime=\(encodedEndTime)"
//        
//        guard let url = URL(string: Self.backendBaseURL + endpoint) else {
//            print("❌ fetchAvailableSpots: Invalid URL")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        // Build request
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"  // ✅ Changed to GET (usually availability is a GET, not POST)
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        // 🔑 Add JWT token (same pattern as fetchParkingSpots)
//        if let token = UserDefaults.standard.string(forKey: "jwtToken"), !token.isEmpty {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("🔑 fetchAvailableSpots using JWT: \(token.prefix(20))…")
//        } else {
//            print("❌ fetchAvailableSpots: No JWT token found")
//            completion(.failure(.authenticationRequired))
//            return
//        }
//        
//        print("🚀 GET \(url.absoluteString)")
//        print("   Lot: \(lotId)")
//        print("   Start: \(startTimeString)")
//        print("   End: \(endTimeString)")
//        
//        // Execute request
//        session.dataTask(with: request) { data, response, error in
//            // Network error
//            if let error = error {
//                print("❌ fetchAvailableSpots network error: \(error.localizedDescription)")
//                completion(.failure(.networkError(error)))
//                return
//            }
//            
//            // Validate HTTP response
//            guard let httpResponse = response as? HTTPURLResponse else {
//                print("❌ fetchAvailableSpots: No HTTPURLResponse")
//                completion(.failure(.badResponse))
//                return
//            }
//            
//            print("📡 fetchAvailableSpots status: \(httpResponse.statusCode)")
//            
//            // Check for auth failure
//            if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//                print("❌ fetchAvailableSpots: Auth failure (status \(httpResponse.statusCode))")
//                completion(.failure(.authenticationRequired))
//                return
//            }
//            
//            // Check for server error
//            guard (200...299).contains(httpResponse.statusCode) else {
//                print("❌ fetchAvailableSpots: Server error \(httpResponse.statusCode)")
//                completion(.failure(.serverError(httpResponse.statusCode)))
//                return
//            }
//            
//            // Check for data
//            guard let data = data else {
//                print("❌ fetchAvailableSpots: No data")
//                completion(.failure(.noData))
//                return
//            }
//            
//            // Decode JSON
//            do {
//                let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
//                print("✅ fetchAvailableSpots decoded \(spots.count) spots")
//                completion(.success(spots))
//            } catch {
//                print("❌ fetchAvailableSpots decoding error: \(error.localizedDescription)")
//                completion(.failure(.decodingError(error)))
//            }
//        }.resume()
//    }
//
//    
//    // MARK: - Bookings
//    func fetchBookings(completion: @escaping (Result<[Bookings], APIError>) -> Void) {
//        guard let userId = getCurrentUserId() else {
//            print("❌ No user ID found for bookings")
//            completion(.failure(.authenticationRequired))
//            return
//        }
//        fetchUserBookings(userId: userId, completion: completion)
//    }
//    
//    // ✅ FIXED: Fetch User Bookings with proper endpoint
//    //    func fetchUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
//    //        // ✅ Use /all endpoint instead of whatever you're using now
//    //        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/all") else {
//    //            completion(.failure(.badURL))
//    //            return
//    //        }
//    //
//    //        var request = URLRequest(url: url)
//    //        request.httpMethod = "GET"
//    //        request.setValue("application/json", forHTTPHeaderField: "Accept")
//    //
//    //        // ✅ Add JWT token - CRITICAL!
//    //        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/all")
//    //
//    //        print("📡 Fetching bookings from: \(url.absoluteString)")
//    //
//    //        session.dataTask(with: request) { data, response, error in
//    //            DispatchQueue.main.async {
//    //                if let error = error {
//    //                    print("❌ Network error: \(error)")
//    //                    completion(.failure(.networkError(error)))
//    //                    return
//    //                }
//    //
//    //                guard let httpResponse = response as? HTTPURLResponse else {
//    //                    completion(.failure(.badResponse))
//    //                    return
//    //                }
//    //
//    //                print("📡 Response status: \(httpResponse.statusCode)")
//    //
//    //                // ✅ Debug 403 issue
//    //                if httpResponse.statusCode == 403 {
//    //                    print("❌ 403 Forbidden - JWT might be invalid or expired")
//    //                    print("📋 Auth header: \(request.value(forHTTPHeaderField: "Authorization") ?? "MISSING")")
//    //                    completion(.failure(.authenticationRequired))
//    //                    return
//    //                }
//    //
//    //                guard (200...299).contains(httpResponse.statusCode) else {
//    //                    print("❌ Server error: \(httpResponse.statusCode)")
//    //                    completion(.failure(.serverError(httpResponse.statusCode)))
//    //                    return
//    //                }
//    //
//    //                guard let data = data else {
//    //                    completion(.failure(.noData))
//    //                    return
//    //                }
//    //
//    //                do {
//    //                    let bookings = try JSONDecoder().decode([Bookings].self, from: data)
//    //                    print("✅ Fetched \(bookings.count) bookings")
//    //                    completion(.success(bookings))
//    //                } catch {
//    //                    print("❌ Decode error: \(error)")
//    //                    completion(.failure(.decodingError(error)))
//    //                }
//    //            }
//    //        }.resume()
//    //    }
//    
//    
//    func fetchAllUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
//        makeRequest(endpoint: "/users/\(userId)/all-bookings", completion: completion)
//    }
//    
//    func fetchBookingHistory(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
//        makeRequest(endpoint: "/users/\(userId)/all-bookings/history", completion: completion)
//    }
//    
//    func fetchBookingById(userId: String, bookingId: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)", completion: completion)
//    }
//    // MARK: - Public Parking Spots (No Authentication Required)
//    
//    /// Fetch parking spots without authentication (used during signup)
//    //    func fetchParkingSpotsPublic(completion: @escaping (Result<[ParkingSpot], APIError>) -> Void) {
//    //        guard let url = URL(string: Self.backendBaseURL + "/parking-spots") else {
//    //            print("❌ Invalid parking spots URL")
//    //            completion(.failure(.badURL))
//    //            return
//    //        }
//    //
//    //        var request = URLRequest(url: url)
//    //        // ✅ NO AUTH HEADER - Public endpoint for signup
//    //        request.setValue("application/json", forHTTPHeaderField: "Accept")
//    //        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
//    //
//    //        print("🚀 Fetching parking spots (public) from: \(url.absoluteString)")
//    //
//    //        session.dataTask(with: request) { data, response, error in
//    //            DispatchQueue.main.async {
//    //                if let error = error {
//    //                    print("❌ Network error: \(error.localizedDescription)")
//    //                    completion(.failure(.networkError(error)))
//    //                    return
//    //                }
//    //
//    //                guard let httpResponse = response as? HTTPURLResponse else {
//    //                    print("❌ Invalid HTTP response")
//    //                    completion(.failure(.badResponse))
//    //                    return
//    //                }
//    //
//    //                print("📡 Parking spots (public) response status: \(httpResponse.statusCode)")
//    //
//    //                guard (200...299).contains(httpResponse.statusCode) else {
//    //                    print("❌ Server error: \(httpResponse.statusCode)")
//    //                    completion(.failure(.serverError(httpResponse.statusCode)))
//    //                    return
//    //                }
//    //
//    //                guard let data = data else {
//    //                    print("❌ No data received")
//    //                    completion(.failure(.noData))
//    //                    return
//    //                }
//    //
//    //                if let jsonString = String(data: data, encoding: .utf8) {
//    //                    print("📋 Raw parking spots (public) response:")
//    //                    print(jsonString)
//    //                }
//    //
//    //                do {
//    //                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
//    //                    print("✅ Successfully decoded \(spots.count) parking spots (public)")
//    //                    completion(.success(spots))
//    //                } catch {
//    //                    print("❌ JSON decoding error: \(error)")
//    //                    completion(.failure(.decodingError(error)))
//    //                }
//    //            }
//    //        }.resume()
//    //    }
//    
//    /// Fetch available parking lot names for signup dropdown
//    // MARK: - Fetch Parking Lot Names (Public - No Authentication)
//    
//    /// Fetch all parking lot names for signup dropdown
//    /// Endpoint: GET /api/parking-lots/list/by-names
//    // ✅ NEW: Fetch parking lots (NO JWT required - public endpoint)
//    // ✅ FIXED: Fetch parking lots WITHOUT JWT token
//    func fetchParkingLots(completion: @escaping (Result<[String], APIError>) -> Void) {
//        guard let url = URL(string: APIService.backendBaseURL + "/parking-lots/list/by-names") else {
//            completion(.failure(.badURL))
//           
//
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        // ❌ DON'T add JWT token here
//        // This is a public endpoint for signup
//        
//        print("📥 Fetching parking lots (no auth)...")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📥 Response: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    print("❌ Server error: \(httpResponse.statusCode)")
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    // Decode based on your backend response format
//                    if let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
//                        let lotNames = jsonArray.compactMap { $0["name"] as? String }
//                        print("✅ Loaded \(lotNames.count) parking lots")
//                        completion(.success(lotNames))
//                    } else if let jsonArray = try JSONSerialization.jsonObject(with: data) as? [String] {
//                        print("✅ Loaded \(jsonArray.count) parking lots")
//                        completion(.success(jsonArray))
//                    } else {
//                        completion(.failure(.invalidResponse))
//                    }
//                } catch {
//                    print("❌ JSON error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    
//    //    func createBooking(
//    //        spotId: String,
//    //        userId: String,
//    //        lotId: String,
//    //        vehicleNumber: String?,
//    //        checkInTime: String,
//    //        checkOutTime: String,
//    //        completion: @escaping (Result<Bookings, APIError>) -> Void
//    //    ) {
//    //        guard let currentUserId = getCurrentUserId(), userId == currentUserId else {
//    //            print("❌ User ID mismatch: \(userId) vs \(getCurrentUserId() ?? "nil")")
//    //            completion(.failure(.authenticationRequired))
//    //            return
//    //        }
//    //
//    //        guard !spotId.isEmpty, !userId.isEmpty, !lotId.isEmpty else {
//    //            print("❌ Missing required booking parameters")
//    //            completion(.failure(.badResponse))
//    //            return
//    //        }
//    //
//    //        var queryItems = [
//    //            URLQueryItem(name: "spotId", value: spotId),
//    //            URLQueryItem(name: "userId", value: userId),
//    //            URLQueryItem(name: "lotId", value: lotId),
//    //            URLQueryItem(name: "checkInTime", value: checkInTime),
//    //            URLQueryItem(name: "checkOutTime", value: checkOutTime)
//    //        ]
//    //
//    //        if let vehicle = vehicleNumber, !vehicle.isEmpty {
//    //            queryItems.append(URLQueryItem(name: "vehicleNumber", value: vehicle))
//    //        }
//    //
//    //        guard var components = URLComponents(string: Self.backendBaseURL + "/bookings/\(userId)/create") else {
//    //            print("❌ Failed to create booking URL components")
//    //            completion(.failure(.badURL))
//    //            return
//    //        }
//    //
//    //        components.queryItems = queryItems
//    //
//    //        guard let url = components.url else {
//    //            print("❌ Failed to create final booking URL")
//    //            completion(.failure(.badURL))
//    //            return
//    //        }
//    //
//    //        var request = URLRequest(url: url)
//    //        request.httpMethod = "POST"
//    //        addAuthHeader(to: &request)
//    //        request.setValue("application/json", forHTTPHeaderField: "Accept")
//    //        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
//    //
//    //        print("🚀 Creating booking at: \(url.absoluteString)")
//    //
//    //        session.dataTask(with: request) { data, response, error in
//    //            DispatchQueue.main.async {
//    //                if let error = error {
//    //                    print("❌ Booking network error: \(error.localizedDescription)")
//    //                    completion(.failure(.unknown(error)))
//    //                    return
//    //                }
//    //
//    //                guard let httpResponse = response as? HTTPURLResponse else {
//    //                    print("❌ Invalid booking response")
//    //                    completion(.failure(.badResponse))
//    //                    return
//    //                }
//    //
//    //                print("📡 Booking response status: \(httpResponse.statusCode)")
//    //
//    //                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//    //                    print("❌ Booking authentication failed")
//    //                    completion(.failure(.authenticationRequired))
//    //                    return
//    //                }
//    //
//    //                guard let data = data else {
//    //                    print("❌ No booking data received")
//    //                    completion(.failure(.badResponse))
//    //                    return
//    //                }
//    //
//    //                if let jsonString = String(data: data, encoding: .utf8) {
//    //                    print("📋 Raw booking response:")
//    //                    print(jsonString)
//    //                }
//    //
//    //                if (200...299).contains(httpResponse.statusCode) {
//    //                    do {
//    //                        let booking = try JSONDecoder().decode(Bookings.self, from: data)
//    //                        print("✅ Booking created successfully: \(booking.id)")
//    //                        completion(.success(booking))
//    //                    } catch {
//    //                        print("❌ Booking JSON decoding error: \(error)")
//    //                        print("⚠️ Attempting to extract booking ID from raw response")
//    //
//    //                        if let jsonObject = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
//    //                           let bookingId = jsonObject["id"] as? String {
//    //                            print("✅ Extracted real booking ID: \(bookingId)")
//    //                            let fallbackBooking = self.createFallbackBooking(
//    //                                bookingId: bookingId,
//    //                                spotId: spotId,
//    //                                userId: userId,
//    //                                lotId: lotId,
//    //                                vehicleNumber: vehicleNumber ?? "UNKNOWN",
//    //                                checkInTime: checkInTime,
//    //                                checkOutTime: checkOutTime
//    //                            )
//    //                            completion(.success(fallbackBooking))
//    //                        } else {
//    //                            print("❌ Could not extract booking ID from response")
//    //                            completion(.failure(.decodingError(error)))
//    //                        }
//    //                    }
//    //                } else {
//    //                    print("❌ Booking server error: \(httpResponse.statusCode)")
//    //                    completion(.failure(.serverError(httpResponse.statusCode)))
//    //                }
//    //            }
//    //        }.resume()
//    //    }
//    // ✅ CREATE BOOKING - Send as JSON body, not query params
//    //    func createBooking(spotId: String, userId: String, lotId: String,
//    //                       vehicleNumber: String, checkInTime: String, checkOutTime: String,
//    //                       completion: @escaping (Result<Bookings, APIError>) -> Void) {
//    //
//    //        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/create") else {
//    //            completion(.failure(.badURL))
//    //            return
//    //        }
//    //
//    //        var request = URLRequest(url: url)
//    //        request.httpMethod = "POST"
//    //        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//    //
//    //        // ✅ Add JWT token
//    //        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/create")
//    //
//    //        // ✅ Create JSON body (NOT query parameters!)
//    //        let body: [String: Any] = [
//    //            "spotId": spotId,
//    //            "lotId": lotId,
//    //            "checkInTime": checkInTime,
//    //            "checkOutTime": checkOutTime,
//    //            "vehicleNumber": vehicleNumber
//    //        ]
//    //
//    //        do {
//    //            request.httpBody = try JSONSerialization.data(withJSONObject: body)
//    //            print("📤 Creating booking:")
//    //            print("   Spot: \(spotId)")
//    //            print("   Vehicle: \(vehicleNumber)")
//    //            print("   Check-in: \(checkInTime)")
//    //            print("   Check-out: \(checkOutTime)")
//    //        } catch {
//    //            completion(.failure(.unknown(error)))
//    //            return
//    //        }
//    //
//    //        session.dataTask(with: request) { data, response, error in
//    //            DispatchQueue.main.async {
//    //                if let error = error {
//    //                    print("❌ Network error: \(error)")
//    //                    completion(.failure(.networkError(error)))
//    //                    return
//    //                }
//    //
//    //                guard let httpResponse = response as? HTTPURLResponse else {
//    //                    completion(.failure(.badResponse))
//    //                    return
//    //                }
//    //
//    //                print("📡 Booking Response: \(httpResponse.statusCode)")
//    //
//    //                guard (200...299).contains(httpResponse.statusCode) else {
//    //                    print("❌ Server error: \(httpResponse.statusCode)")
//    //                    completion(.failure(.serverError(httpResponse.statusCode)))
//    //                    return
//    //                }
//    //
//    //                guard let data = data else {
//    //                    completion(.failure(.noData))
//    //                    return
//    //                }
//    //
//    //                do {
//    //                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//    //                    print("✅ Booking created: \(booking.id)")
//    //                    completion(.success(booking))
//    //                } catch {
//    //                    print("❌ Decode error: \(error)")
//    //                    completion(.failure(.decodingError(error)))
//    //                }
//    //            }
//    //        }.resume()
//    //    }
//    // ✅ MARK: - Booking API Methods (Matching Backend Controller)
//    
//    // ✅ Create Booking
//    // ✅ FIXED: Create Booking - Send as JSON body
//    func createBooking(
//        spotId: String,
//        userId: String,
//        lotId: String,
//        vehicleNumber: String,
//        checkInTime: String,
//        checkOutTime: String,
//        completion: @escaping (Result<Bookings, APIError>) -> Void
//    ) {
//        // Build URL
//        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/create") else {
//            print("❌ Invalid booking URL")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        // Create request
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        // Add JWT token
//        addAuthHeader(to: &request, endpoint: "bookings/\(userId)/create")
//        
//        // ✅ FIX: Create JSON body (NOT query parameters!)
//        let body: [String: Any] = [
//            "spotId": spotId,
//            "lotId": lotId,
//            "checkInTime": checkInTime,
//            "checkOutTime": checkOutTime,
//            "vehicleNumber": vehicleNumber
//        ]
//        
//        
//        // Encode to JSON
//        do {
//            request.httpBody = try JSONSerialization.data(withJSONObject: body)
//            
//            print("📋 Creating booking for vehicle: \(vehicleNumber)")
//            print("✅ JWT: \(request.value(forHTTPHeaderField: "Authorization")?.prefix(30) ?? "NONE")")
//            print("📤 Creating booking at: \(url.absoluteString)")
//            print("📦 Body: \(body)")
//            
//        } catch {
//            print("❌ Failed to encode booking body")
//            completion(.failure(.badResponse))  // ✅ CHANGED: Use .badResponse instead of .unknownError
//            return
//        }
//
//        
//        // Execute request
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                // Handle network error
//                if let error = error {
//                    print("❌ Network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                // Validate HTTP response
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Response: \(httpResponse.statusCode)")
//                
//                // Check for auth errors
//                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//                    print("❌ Authentication failed")
//                    completion(.failure(.authenticationRequired))
//                    return
//                }
//                
//                // Check for data
//                guard let data = data else {
//                    print("❌ No data")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                // Print raw response for debugging
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📋 Raw response: \(jsonString)")
//                }
//                
//                // Check for success
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    let errorMsg = String(data: data, encoding: .utf8) ?? "Unknown error"
//                    print("❌ Server error \(httpResponse.statusCode): \(errorMsg)")
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                // Decode booking
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    print("✅ Booking created: \(booking.id) - Status: \(booking.status)")
//                    completion(.success(booking))
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//
//    // ✅ Fetch All Bookings for User (GET /api/bookings/{userId}/all)
//    func fetchUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/all") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/all")
//        
//        print("📡 Fetching bookings from: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Booking fetch response: \(httpResponse.statusCode)")
//                
//                if httpResponse.statusCode == 403 {
//                    print("❌ 403 Forbidden - JWT token issue")
//                    completion(.failure(.authenticationRequired))
//                    return
//                }
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    let bookings = try JSONDecoder().decode([Bookings].self, from: data)
//                    print("✅ Fetched \(bookings.count) bookings")
//                    for booking in bookings {
//                        print("   - \(booking.id): \(booking.status)")
//                    }
//                    completion(.success(bookings))
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    // ✅ Get Booking by ID (GET /api/bookings/{userId}/{bookingId})
//    func getBooking(userId: String, bookingId: String,
//                    completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/\(bookingId)") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/\(bookingId)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                guard let httpResponse = response as? HTTPURLResponse,
//                      (200...299).contains(httpResponse.statusCode),
//                      let data = data else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    completion(.success(booking))
//                } catch {
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    // ✅ Cancel Booking (POST /api/bookings/{userId}/{bookingId}/cancel)
//    // ✅ In APIService.swift - Make sure this exists
//    // ✅ KEEP ONLY THIS ONE - Delete any other cancelBooking methods
//    // ✅ KEEP ONLY THIS ONE - Delete both old ones
//    func cancelBooking(userId: String, bookingId: String,
//                       completion: @escaping (Result<Void, APIError>) -> Void) {
//        // ✅ Backend endpoint from your controller: POST /api/bookings/{userId}/{bookingId}/cancel
//        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/\(bookingId)/cancel") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/\(bookingId)/cancel")
//        
//        print("🗑️ Cancelling booking at: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Bad response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Cancel response: \(httpResponse.statusCode)")
//                
//                // ✅ Accept both 200-299 and 204 No Content
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    print("❌ Server error: \(httpResponse.statusCode)")
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                print("✅ Booking cancelled successfully")
//                completion(.success(()))
//            }
//        }.resume()
//    }
//    
//    
//    
//    // ✅ Extend Booking (PUT /api/bookings/{userId}/{bookingId}/extend)
//    func extendBooking(userId: String, bookingId: String, newCheckOutTime: String,
//                       completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/\(bookingId)/extend") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "PUT"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/\(bookingId)/extend")
//        
//        let body: [String: String] = ["newCheckOutTime": newCheckOutTime]
//        
//        do {
//            request.httpBody = try JSONSerialization.data(withJSONObject: body)
//        } catch {
//            completion(.failure(.unknown(error)))
//            return
//        }
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                guard let httpResponse = response as? HTTPURLResponse,
//                      (200...299).contains(httpResponse.statusCode),
//                      let data = data else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    print("✅ Booking extended")
//                    completion(.success(booking))
//                } catch {
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    // ✅ Get Booking History (GET /api/bookings/{userId}/all/history)
//    func getBookingHistory(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/all/history") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/all/history")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                guard let httpResponse = response as? HTTPURLResponse,
//                      (200...299).contains(httpResponse.statusCode),
//                      let data = data else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                do {
//                    let bookings = try JSONDecoder().decode([Bookings].self, from: data)
//                    print("✅ Fetched booking history: \(bookings.count) bookings")
//                    completion(.success(bookings))
//                } catch {
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    
//    private func createFallbackBooking(
//        bookingId: String,
//        spotId: String,
//        userId: String,
//        lotId: String,
//        vehicleNumber: String,
//        checkInTime: String,
//        checkOutTime: String
//    ) -> Bookings {
//        let fallbackData: [String: Any] = [
//            "id": bookingId,
//            "spotId": spotId,
//            "userId": userId,
//            "lotId": lotId,
//            "status": "PENDING",
//            "vehicleNumbers": vehicleNumber,
//            "checkInTime": checkInTime,
//            "checkOutTime": checkOutTime,
//            "totalHours": 1.0,
//            "totalAmount": 5.0,
//            "amount": 5.0,
//            "qrCodeScanned": false
//        ]
//        
//        do {
//            let data = try JSONSerialization.data(withJSONObject: fallbackData)
//            let booking = try JSONDecoder().decode(Bookings.self, from: data)
//            print("✅ Created fallback booking with ID: \(bookingId)")
//            return booking
//        } catch {
//            print("❌ Failed to create fallback booking: \(error)")
//            fatalError("Critical error creating fallback booking")
//        }
//    }
//    
//    // MARK: - Additional Booking Operations
//    
//    func confirmBooking(userId: String, bookingId: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        let endpoint = "/users/\(userId)/bookings/\(bookingId)/confirm"
//        
//        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
//            print("❌ Invalid confirm booking URL")
//            completion(.failure(.invalidURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        addAuthHeader(to: &request)
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
//        
//        print("🚀 Confirming booking at: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Confirm booking network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid confirm booking response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Confirm booking response status: \(httpResponse.statusCode)")
//                
//                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//                    print("❌ Confirm booking authentication failed")
//                    completion(.failure(.authenticationRequired))
//                    return
//                }
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    print("❌ Confirm booking server error: \(httpResponse.statusCode)")
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No confirm booking data received")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📋 Raw confirm booking response:")
//                    print(jsonString)
//                }
//                
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    print("✅ Booking confirmed successfully: \(booking.id) - Status: \(booking.status)")
//                    completion(.success(booking))
//                } catch {
//                    print("❌ Confirm booking decoding error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    //    func cancelBooking(userId: String, bookingId: String, completion: @escaping (Result<Bool, APIError>) -> Void) {
//    //        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)/cancel") else {
//    //            completion(.failure(.badURL))
//    //            return
//    //        }
//    //
//    //        var request = URLRequest(url: url)
//    //        request.httpMethod = "POST"
//    //        addAuthHeader(to: &request)
//    //
//    //        session.dataTask(with: request) { _, response, error in
//    //            DispatchQueue.main.async {
//    //                if let error = error {
//    //                    completion(.failure(.unknown(error)))
//    //                    return
//    //                }
//    //
//    //                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 {
//    //                    completion(.success(true))
//    //                } else {
//    //                    completion(.failure(.badResponse))
//    //                }
//    //            }
//    //        }.resume()
//    //    }
//    
//    func validateQRCheckIn(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        let endpoint = "/users/\(userId)/bookings/\(bookingId)/validate-qr-checkin"
//        
//        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
//            print("❌ Invalid URL")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        addAuthHeader(to: &request)
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        let body: [String: String] = ["qrCode": qrCode]
//        request.httpBody = try? JSONEncoder().encode(body)
//        
//        print("🚀 Step 1: Validating QR code")
//        print("   URL: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Validation response status: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
//                        print("📋 Error response: \(errorMsg)")
//                    }
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No data in response")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    let validationResult = try JSONDecoder().decode(QrValidationResult.self, from: data)
//                    print("✅ Validation Result:")
//                    print("   Valid: \(validationResult.valid)")
//                    print("   Message: \(validationResult.message ?? "nil")")
//                    if let penalty = validationResult.penalty {
//                        print("   Penalty: ₹\(penalty)")
//                    }
//                    
//                    if validationResult.valid {
//                        print("🚀 Step 2: Performing check-in")
//                        self.performCheckIn(userId: userId, bookingId: bookingId, qrCode: qrCode, completion: completion)
//                    } else {
//                        print("❌ QR validation failed: \(validationResult.message ?? "Unknown error")")
//                        completion(.failure(.serverError(400)))
//                    }
//                } catch {
//                    print("❌ Failed to decode validation result: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    func validateQRCheckOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        let endpoint = "/users/\(userId)/bookings/\(bookingId)/validate-qr-checkout"
//        
//        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
//            print("❌ Invalid URL")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        addAuthHeader(to: &request)
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        let body: [String: String] = ["qrCode": qrCode]
//        request.httpBody = try? JSONEncoder().encode(body)
//        
//        print("🚀 Step 1: Validating QR code for checkout")
//        print("   URL: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Validation response status: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
//                        print("📋 Error response: \(errorMsg)")
//                    }
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No data in response")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    let validationResult = try JSONDecoder().decode(QrValidationResult.self, from: data)
//                    print("✅ Validation Result:")
//                    print("   Valid: \(validationResult.valid)")
//                    print("   Message: \(validationResult.message ?? "nil")")
//                    if let penalty = validationResult.penalty {
//                        print("   Penalty: ₹\(penalty)")
//                    }
//                    
//                    if validationResult.valid {
//                        print("🚀 Step 2: Performing checkout")
//                        self.performCheckOut(userId: userId, bookingId: bookingId, qrCode: qrCode, completion: completion)
//                    } else {
//                        print("❌ QR validation failed: \(validationResult.message ?? "Unknown error")")
//                        completion(.failure(.serverError(400)))
//                    }
//                } catch {
//                    print("❌ Failed to decode validation result: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    private func performCheckOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        let endpoint = "/users/\(userId)/bookings/\(bookingId)/checkout"
//        
//        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
//            print("❌ Invalid checkout URL")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        addAuthHeader(to: &request)
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        let body: [String: String] = ["qrCode": qrCode]
//        request.httpBody = try? JSONEncoder().encode(body)
//        
//        print("   URL: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Checkout network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid checkout response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Checkout response status: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
//                        print("📋 Checkout error: \(errorMsg)")
//                    }
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No checkout data")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📋 Raw checkout response:")
//                    print(jsonString)
//                }
//                
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    print("✅ Checkout completed successfully!")
//                    print("   Status: \(booking.status)")
//                    print("   Check-out Time: \(booking.checkOutTime ?? "nil")")
//                    print("   Total Amount: ₹\(booking.totalAmount)")
//                    completion(.success(booking))
//                } catch {
//                    print("❌ Checkout decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    private func performCheckIn(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        let endpoint = "/users/\(userId)/bookings/\(bookingId)/checkin"
//        
//        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
//            print("❌ Invalid check-in URL")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        addAuthHeader(to: &request)
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        let body: [String: String] = ["qrCode": qrCode]
//        request.httpBody = try? JSONEncoder().encode(body)
//        
//        print("   URL: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Check-in network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid check-in response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Check-in response status: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
//                        print("📋 Check-in error: \(errorMsg)")
//                    }
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No check-in data")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📋 Raw check-in response:")
//                    print(jsonString)
//                }
//                
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    print("✅ Check-in completed successfully!")
//                    print("   Status: \(booking.status)")
//                    print("   Check-in Time: \(booking.checkInTime ?? "nil")")
//                    completion(.success(booking))
//                } catch {
//                    print("❌ Check-in decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    func getPenaltyInfo(userId: String, bookingId: String, completion: @escaping (Result<Double, APIError>) -> Void) {
//        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)/penalty") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        addAuthHeader(to: &request)
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    completion(.failure(.unknown(error)))
//                    return
//                }
//                
//                guard let data = data,
//                      let penalty = try? JSONDecoder().decode(Double.self, from: data) else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                completion(.success(penalty))
//            }
//        }.resume()
//    }
//    
//    func updateBookingStatus(userId: String, bookingId: String, status: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
//        let bodyDict: [String: Any] = ["status": status]
//        guard let bodyData = try? JSONSerialization.data(withJSONObject: bodyDict) else {
//            completion(.failure(.badResponse))
//            return
//        }
//        
//        makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)", method: "PUT", body: bodyData, completion: completion)
//    }
//    
//    func deleteBooking(userId: String, bookingId: String, completion: @escaping (Result<Bool, APIError>) -> Void) {
//        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "DELETE"
//        addAuthHeader(to: &request)
//        
//        session.dataTask(with: request) { _, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    completion(.failure(.unknown(error)))
//                    return
//                }
//                
//                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 {
//                    completion(.success(true))
//                } else {
//                    completion(.failure(.badResponse))
//                }
//            }
//        }.resume()
//    }
//    
//    // MARK: - Configuration
//    //    func fetchParkingConfig(completion: @escaping (Result<ParkingConfig, APIError>) -> Void) {
//    //        makeRequest(endpoint: "/config/parking-rates", completion: completion)
//    //    }
//    
//    // MARK: - Wallet Methods
//    func fetchWallet(userId: String, completion: @escaping (Result<Wallet, APIError>) -> Void) {
//        print("📡 Fetching wallet for user: \(userId)")
//        makeRequest(endpoint: "/users/\(userId)/wallet", completion: completion)
//    }
//    
//    func fetchWalletTransactions(userId: String, completion: @escaping (Result<[Transactions], APIError>) -> Void) {
//        print("📡 Fetching wallet transactions for user: \(userId)")
//        makeRequest(endpoint: "/users/\(userId)/wallet/transactions", completion: completion)
//    }
//    
//    func topUpWallet(userId: String, amount: Double, completion: @escaping (Result<Wallet, APIError>) -> Void) {
//        print("📡 Topping up wallet for user: \(userId), amount: \(amount)")
//        
//        let bodyDict: [String: Any] = ["amount": amount]
//        guard let bodyData = try? JSONSerialization.data(withJSONObject: bodyDict) else {
//            print("❌ Failed to serialize wallet topup data")
//            completion(.failure(.badResponse))
//            return
//        }
//        
//        makeRequest(endpoint: "/users/\(userId)/wallet/topup", method: "POST", body: bodyData, completion: completion)
//    }
//    
//    // MARK: - Helper Methods
//    private func getCurrentUserId() -> String? {
//        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
//            return userId
//        }
//        
//        if let userData = UserDefaults.standard.data(forKey: "userData"),
//           let user = try? JSONDecoder().decode(Users.self, from: userData) {
//            UserDefaults.standard.set(user.id, forKey: "currentUserId")
//            return user.id
//        }
//        
//        return nil
//    }
//    
//    // In APIService.swift
//    
//    // ✅ GET /api/users/{userId}/vehicles
//    // ✅ FIXED: Handle response as dictionary
//    func fetchUserVehicles(userId: String, completion: @escaping (Result<[String], APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/vehicles") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        // ✅ Add JWT token
//        addAuthHeader(to: &request, endpoint: "/users/\(userId)/vehicles")
//        
//        print("📥 Fetching vehicles for user: \(userId)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Response: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                // ✅ Debug: Print raw response
//                if let jsonString = String(data: data, encoding: .utf8) {
//                    print("📋 Raw response: \(jsonString)")
//                }
//                
//                do {
//                    // ✅ FIXED: Decode as dictionary first, then extract vehicleNumbers
//                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
//                       let vehicleNumbers = json["vehicleNumbers"] as? [String] {
//                        print("✅ Fetched \(vehicleNumbers.count) vehicles: \(vehicleNumbers)")
//                        completion(.success(vehicleNumbers))
//                    } else {
//                        // ✅ Fallback: Try to decode as direct array
//                        let vehicles = try JSONDecoder().decode([String].self, from: data)
//                        print("✅ Fetched \(vehicles.count) vehicles (direct array)")
//                        completion(.success(vehicles))
//                    }
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    
//    // ✅ PUT /api/users/{userId}/add-vehicles
//    func addVehicleToBackend(userId: String, vehicleNumber: String, completion: @escaping (Result<[String], APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/add-vehicles") else {
//            completion(.failure(.invalidURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "PUT"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        if let token = authViewModel?.jwtToken {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//        }
//        
//        let body: [String] = [vehicleNumber]
//        
//        do {
//            request.httpBody = try JSONEncoder().encode(body)
//        } catch {
//            completion(.failure(.unknown(error)))
//            return
//        }
//        
//        print("🚗 Adding vehicle to backend: \(vehicleNumber)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Response status: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    let vehicles = try JSONDecoder().decode([String].self, from: data)
//                    print("✅ Backend returned \(vehicles.count) vehicles")
//                    completion(.success(vehicles))
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    
//    
//    // MARK: - Core Network Method
//    private func makeRequest<T: Decodable>(
//        endpoint: String,
//        method: String = "GET",
//        queryItems: [URLQueryItem]? = nil,
//        body: Data? = nil,
//        completion: @escaping (Result<T, APIError>) -> Void
//    ) {
//        guard var components = URLComponents(string: Self.backendBaseURL + endpoint) else {
//            print("❌ Invalid URL components for endpoint: \(endpoint)")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        components.queryItems = queryItems
//        
//        guard let url = components.url else {
//            print("❌ Failed to create URL for endpoint: \(endpoint)")
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = method
//        addAuthHeader(to: &request)
//        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
//        
//        if let body = body {
//            request.httpBody = body
//            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        } else {
//            request.setValue("application/json", forHTTPHeaderField: "Accept")
//        }
//        
//        print("🚀 \(method) request to: \(url.absoluteString)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Request failed: \(error.localizedDescription)")
//                    completion(.failure(.unknown(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    print("❌ Invalid HTTP response")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Response status: \(httpResponse.statusCode)")
//                
//                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
//                    print("❌ Authentication failed")
//                    completion(.failure(.authenticationRequired))
//                    return
//                }
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    print("❌ Server error: \(httpResponse.statusCode)")
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No data received")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                do {
//                    let result = try JSONDecoder().decode(T.self, from: data)
//                    print("✅ Successfully decoded response")
//                    completion(.success(result))
//                } catch {
//                    print("❌ JSON decoding failed: \(error)")
//                    if let jsonString = String(data: data, encoding: .utf8) {
//                        print("📋 Raw response: \(jsonString)")
//                    }
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    func extendBooking(
//        userId: String,
//        bookingId: String,
//        newCheckOutTime: String,
//        completion: @escaping (Result<Bookings, Error>) -> Void
//    ) {
//        let urlString = "\(APIService.backendBaseURL)/users/\(userId)/bookings/\(bookingId)/extend"
//        
//        guard let url = URL(string: urlString) else {
//            completion(.failure(APIError.invalidURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "PUT"
//        addAuthHeader(to: &request)
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        let body: [String: Any] = [
//            "newCheckOutTime": newCheckOutTime
//        ]
//        
//        do {
//            request.httpBody = try JSONSerialization.data(withJSONObject: body)
//        } catch {
//            completion(.failure(error))
//            return
//        }
//        
//        print("🔄 Extending booking: \(bookingId) to new checkout: \(newCheckOutTime)")
//        
//        session.dataTask(with: request) { data, response, error in
//            if let error = error {
//                print("❌ Extend booking error: \(error)")
//                completion(.failure(error))
//                return
//            }
//            
//            guard let httpResponse = response as? HTTPURLResponse else {
//                completion(.failure(APIError.badResponse))
//                return
//            }
//            
//            guard (200...299).contains(httpResponse.statusCode) else {
//                print("❌ Server returned status code: \(httpResponse.statusCode)")
//                completion(.failure(APIError.serverError(httpResponse.statusCode)))
//                return
//            }
//            
//            guard let data = data else {
//                completion(.failure(APIError.noData))
//                return
//            }
//            
//            do {
//                let updatedBooking = try JSONDecoder().decode(Bookings.self, from: data)
//                print("✅ Booking extended successfully")
//                completion(.success(updatedBooking))
//            } catch {
//                print("❌ Decode error: \(error)")
//                completion(.failure(error))
//            }
//        }.resume()
//    }
//    // In APIService.swift new function taaki nayi vehicle apne londe daal ske apne ape mein
//    
//    
//    //    func addVehicleToBackend(userId: String, vehicleNumber: String, completion: @escaping (Result<[String], APIError>) -> Void) {
//    //        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/add-vehicles") else {
//    //            completion(.failure(.invalidURL))
//    //            return
//    //        }
//    //
//    //        var request = URLRequest(url: url)
//    //        request.httpMethod = "PUT"
//    //        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//    //
//    //        // ✅ Add JWT token for authentication
//    //        if let token = authViewModel?.jwtToken {
//    //            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//    //        }
//    //
//    //        // ✅ Send as array (your backend expects List<String>)
//    //        let body: [String] = [vehicleNumber]
//    //
//    //        do {
//    //            request.httpBody = try JSONEncoder().encode(body)
//    //        } catch {
//    //            completion(.failure(.unknown(error)))
//    //            return
//    //        }
//    //
//    //        print("🚗 Adding vehicle to backend: \(vehicleNumber)")
//    //        print("   URL: \(url.absoluteString)")
//    //
//    //        session.dataTask(with: request) { data, response, error in
//    //            DispatchQueue.main.async {
//    //                if let error = error {
//    //                    print("❌ Network error: \(error)")
//    //                    completion(.failure(.networkError(error)))
//    //                    return
//    //                }
//    //
//    //                guard let httpResponse = response as? HTTPURLResponse else {
//    //                    print("❌ Invalid response")
//    //                    completion(.failure(.badResponse))
//    //                    return
//    //                }
//    //
//    //                print("📡 Response status: \(httpResponse.statusCode)")
//    //
//    //                guard (200...299).contains(httpResponse.statusCode) else {
//    //                    print("❌ Server error: \(httpResponse.statusCode)")
//    //                    completion(.failure(.serverError(httpResponse.statusCode)))
//    //                    return
//    //                }
//    //
//    //                guard let data = data else {
//    //                    print("❌ No data")
//    //                    completion(.failure(.noData))
//    //                    return
//    //                }
//    //
//    //                do {
//    //                    // ✅ Backend returns List<String> of all vehicles
//    //                    let vehicles = try JSONDecoder().decode([String].self, from: data)
//    //                    print("✅ Vehicle added successfully. Total vehicles: \(vehicles)")
//    //                    completion(.success(vehicles))
//    //                } catch {
//    //                    print("❌ Decode error: \(error)")
//    //                    completion(.failure(.decodingError(error)))
//    //                }
//    //            }
//    //        }.resume()
//    //    }
//    // In APIService.swift
//    
//    func fetchUserProfileFromBackend(userId: String, completion: @escaping (Result<Users, APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)") else {
//            completion(.failure(.invalidURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        
//        // Add JWT token
//        if let token = authViewModel?.jwtToken {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//        }
//        
//        print("📥 Fetching user profile from backend: \(userId)")
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse,
//                      (200...299).contains(httpResponse.statusCode) else {
//                    print("❌ Server error")
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                guard let data = data else {
//                    print("❌ No data")
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    let user = try JSONDecoder().decode(Users.self, from: data)
//                    print("✅ User profile decoded successfully")
//                    print("   Vehicles count: \(user.vehicleNumbers?.count ?? 0)")
//                    completion(.success(user))
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    // ✅ FIXED: Now accepts userId parameter
//    func getBookingById(userId: String, bookingId: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
//        // ✅ Use the format: GET /api/users/{userId}/bookings/{bookingId}
//        let endpoint = "/users/\(userId)/bookings/\(bookingId)"
//        
//        guard let url = URL(string: "\(APIService.backendBaseURL)\(endpoint)") else {
//            print("❌ Invalid URL")
//            print("   Base: \(APIService.backendBaseURL)")
//            print("   Endpoint: \(endpoint)")
//            completion(.failure(APIError.invalidURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        addAuthHeader(to: &request)
//        
//        print("\n========== GET BOOKING BY ID ==========")
//        print("📍 Full URL: \(url.absoluteString)")
//        print("👤 User ID: \(userId)")
//        print("🔑 Booking ID: \(bookingId)")
//        print("=======================================\n")
//        
//        session.dataTask(with: request) { data, response, error in
//            if let error = error {
//                print("❌ Network error: \(error.localizedDescription)")
//                DispatchQueue.main.async {
//                    completion(.failure(error))
//                }
//                return
//            }
//            
//            guard let httpResponse = response as? HTTPURLResponse else {
//                DispatchQueue.main.async {
//                    completion(.failure(APIError.badResponse))
//                }
//                return
//            }
//            
//            print("📊 Status: \(httpResponse.statusCode)")
//            
//            guard let data = data else {
//                print("❌ No data")
//                DispatchQueue.main.async {
//                    completion(.failure(APIError.noData))
//                }
//                return
//            }
//            
//            if let jsonString = String(data: data, encoding: .utf8) {
//                print("📄 Response: \(jsonString)")
//            }
//            
//            if (200...299).contains(httpResponse.statusCode) {
//                do {
//                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                    print("\n✅ Booking fetched successfully!")
//                    print("   Booking ID: \(booking.id)")
//                    print("   User ID: \(booking.userId)")
//                    print("   Status: \(booking.status)")
//                    print("   Vehicle: \(booking.vehicleNumber ?? "N/A")")
//                    print("=======================================\n")
//                    
//                    DispatchQueue.main.async {
//                        completion(.success(booking))
//                    }
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    DispatchQueue.main.async {
//                        completion(.failure(error))
//                    }
//                }
//            } else {
//                let errorMsg = String(data: data, encoding: .utf8) ?? "Unknown error"
//                print("❌ HTTP Error \(httpResponse.statusCode): \(errorMsg)")
//                DispatchQueue.main.async {
//                    completion(.failure(APIError.serverError(httpResponse.statusCode)))
//                }
//            }
//        }.resume()
//    }
//    
//    // ✅ Add Vehicle to Backend
//    // ✅ Add Vehicle to Backend
//    // ✅ Make sure this is also correct
//    func addVehiclesToBackend(userId: String, vehicleNumbers: [String],
//                              completion: @escaping (Result<[String], APIError>) -> Void) {
//        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/add-vehicles") else {
//            completion(.failure(.badURL))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "PUT"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        // ✅ Add JWT token
//        addAuthHeader(to: &request, endpoint: "/users/\(userId)/add-vehicles")
//        
//        do {
//            request.httpBody = try JSONSerialization.data(withJSONObject: vehicleNumbers)
//            print("📤 Sending vehicles: \(vehicleNumbers)")
//        } catch {
//            completion(.failure(.unknown(error)))
//            return
//        }
//        
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//                
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//                
//                print("📡 Response: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                guard let data = data else {
//                    completion(.failure(.noData))
//                    return
//                }
//                
//                do {
//                    // ✅ FIXED: Handle dictionary response
//                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
//                       let vehicles = json["vehicleNumbers"] as? [String] {
//                        print("✅ Vehicles saved: \(vehicles)")
//                        completion(.success(vehicles))
//                    } else {
//                        completion(.failure(.decodingError(NSError())))
//                    }
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//    
//    struct CheckInRequestDto: Codable {
//        let mode: String
//        let qrCode: String?
//        let vehicleNumber: String?
//        let pin: String?
//        
//        enum CodingKeys: String, CodingKey {
//            case mode, qrCode, vehicleNumber, pin
//        }
//        
//        init(mode: String, qrCode: String? = nil, vehicleNumber: String? = nil, pin: String? = nil) {
//            self.mode = mode
//            self.qrCode = qrCode
//            self.vehicleNumber = vehicleNumber
//            self.pin = pin
//        }
//    }
//    
//    // ✅ Check-in for operator (VEHICLE_NUMBER)
//    func checkInOperator(vehicleNumber: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
//        print("🚀 === CHECK-IN API CALL STARTED ===")
//        print("📍 Vehicle Number: \(vehicleNumber)")
//        
//        // Construct URL
//        let endpoint = "/bookings/checkin"
//        let fullURL = "\(APIService.backendBaseURL)\(endpoint)"
//        print("🌐 Full URL: \(fullURL)")
//        
//        guard let url = URL(string: fullURL) else {
//            print("❌ Invalid URL: \(fullURL)")
//            completion(.failure(NSError(domain: "URLError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"])))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        print("✅ HTTP Method: POST")
//        print("✅ Content-Type: application/json")
//        
//        // Add auth header
//        addAuthHeader(to: &request, endpoint: endpoint)
//        
//        // Log all headers (mask JWT for security)
//        print("📋 Request Headers:")
//        request.allHTTPHeaderFields?.forEach { key, value in
//            if key.lowercased() == "authorization" {
//                let maskedToken = String(value.prefix(20)) + "..."
//                print("   \(key): \(maskedToken)")
//            } else {
//                print("   \(key): \(value)")
//            }
//        }
//        
//        // Create request DTO
//        let dto = CheckInRequestDto(
//            mode: "VEHICLE_NUMBER",
//            qrCode: nil,
//            vehicleNumber: vehicleNumber,
//            pin: nil
//        )
//        print("📦 Request DTO created:")
//        print("   mode: VEHICLE_NUMBER")
//        print("   vehicleNumber: \(vehicleNumber)")
//        print("   qrCode: nil")
//        print("   pin: nil")
//        
//        // Encode to JSON
//        guard let jsonData = try? JSONEncoder().encode(dto) else {
//            print("❌ Failed to encode CheckInRequestDto")
//            completion(.failure(NSError(domain: "EncodingError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode check-in request"])))
//            return
//        }
//        
//        // Print the exact JSON being sent
//        if let jsonString = String(data: jsonData, encoding: .utf8) {
//            print("📤 REQUEST BODY JSON:")
//            print(jsonString)
//        }
//        
//        print("📊 Request body size: \(jsonData.count) bytes")
//        
//        request.httpBody = jsonData
//        
//        print("🚀 Sending network request...")
//        print("⏰ Request sent at: \(Date())")
//        
//        // Make the API call
//        let startTime = Date()
//        session.dataTask(with: request) { data, response, error in
//            let endTime = Date()
//            let responseTime = endTime.timeIntervalSince(startTime)
//            print("⏱️ Response received in \(String(format: "%.2f", responseTime))s")
//            
//            // Handle network errors
//            if let error = error {
//                print("❌ NETWORK ERROR:")
//                print("   Description: \(error.localizedDescription)")
//                print("   Domain: \((error as NSError).domain)")
//                print("   Code: \((error as NSError).code)")
//                completion(.failure(error))
//                return
//            }
//            
//            // Check HTTP response
//            if let httpResponse = response as? HTTPURLResponse {
//                print("📡 HTTP RESPONSE:")
//                print("   Status Code: \(httpResponse.statusCode)")
//                print("   Status Description: \(HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode))")
//                
//                // Log response headers
//                print("📋 Response Headers:")
//                httpResponse.allHeaderFields.forEach { key, value in
//                    print("   \(key): \(value)")
//                }
//                
//                // Handle rate limiting (429)
//                if httpResponse.statusCode == 429 {
//                    print("⚠️ RATE LIMIT EXCEEDED!")
//                    let rateLimitError = NSError(
//                        domain: "APIError",
//                        code: 429,
//                        userInfo: [NSLocalizedDescriptionKey: "Rate limit exceeded. Please wait."]
//                    )
//                    completion(.failure(rateLimitError))
//                    return
//                }
//                
//                // Handle error status codes (400-599)
//                if httpResponse.statusCode >= 400 {
//                    let errorMessage = String(data: data ?? Data(), encoding: .utf8) ?? "Unknown error"
//                    print("❌ SERVER ERROR RESPONSE:")
//                    print("   Status: \(httpResponse.statusCode)")
//                    print("   Body: \(errorMessage)")
//                    
//                    let apiError = NSError(
//                        domain: "APIError",
//                        code: httpResponse.statusCode,
//                        userInfo: [NSLocalizedDescriptionKey: errorMessage]
//                    )
//                    completion(.failure(apiError))
//                    return
//                }
//            } else {
//                print("⚠️ No HTTP response available")
//            }
//            
//            // Validate data
//            guard let data = data else {
//                print("❌ NO DATA received from server")
//                completion(.failure(NSError(domain: "NoDataError", code: 2, userInfo: [NSLocalizedDescriptionKey: "No data received from server"])))
//                return
//            }
//            
//            print("📥 Response data size: \(data.count) bytes")
//            
//            // Print raw response
//            if let jsonString = String(data: data, encoding: .utf8) {
//                print("📥 RAW RESPONSE BODY:")
//                print(jsonString)
//            }
//            
//            // Decode response
//            print("🔄 Attempting to decode Bookings object...")
//            do {
//                let decoder = JSONDecoder()
//                let booking = try decoder.decode(Bookings.self, from: data)
//                print("✅ CHECK-IN SUCCESSFUL!")
//                print("   Booking ID: \(booking.id ?? "nil")")
//                print("   Vehicle Number: \(booking.vehicleNumber ?? "nil")")
//                print("   Status: \(booking.status ?? "nil")")
//                print("   Check-in Time: \(booking.checkInTime?.description ?? "nil")")
//                print("🎉 === CHECK-IN API CALL COMPLETED ===")
//                completion(.success(booking))
//            } catch let decodingError as DecodingError {
//                print("❌ DECODING ERROR:")
//                switch decodingError {
//                case .keyNotFound(let key, let context):
//                    print("   Missing key: '\(key.stringValue)'")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                case .typeMismatch(let type, let context):
//                    print("   Type mismatch for type: \(type)")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                case .valueNotFound(let type, let context):
//                    print("   Value not found for type: \(type)")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                case .dataCorrupted(let context):
//                    print("   Data corrupted")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                @unknown default:
//                    print("   Unknown decoding error: \(decodingError)")
//                }
//                completion(.failure(decodingError))
//            } catch {
//                print("❌ UNEXPECTED ERROR during decoding:")
//                print("   \(error.localizedDescription)")
//                completion(.failure(error))
//            }
//        }.resume()
//    }
//    
//    // ✅ Check-out for operator (VEHICLE_NUMBER)
//    func checkOutOperator(vehicleNumber: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
//        print("🚀 === CHECK-OUT API CALL STARTED ===")
//        print("📍 Vehicle Number: \(vehicleNumber)")
//        
//        // Construct URL
//        let endpoint = "/bookings/checkout"
//        let fullURL = "\(APIService.backendBaseURL)\(endpoint)"
//        print("🌐 Full URL: \(fullURL)")
//        
//        guard let url = URL(string: fullURL) else {
//            print("❌ Invalid URL: \(fullURL)")
//            completion(.failure(NSError(domain: "URLError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"])))
//            return
//        }
//        
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        print("✅ HTTP Method: POST")
//        print("✅ Content-Type: application/json")
//        
//        // Add auth header
//        addAuthHeader(to: &request, endpoint: endpoint)
//        
//        // Log all headers (mask JWT for security)
//        print("📋 Request Headers:")
//        request.allHTTPHeaderFields?.forEach { key, value in
//            if key.lowercased() == "authorization" {
//                let maskedToken = String(value.prefix(20)) + "..."
//                print("   \(key): \(maskedToken)")
//            } else {
//                print("   \(key): \(value)")
//            }
//        }
//        
//        // Create request DTO
//        let dto = CheckInRequestDto(
//            mode: "VEHICLE_NUMBER",
//            qrCode: nil,
//            vehicleNumber: vehicleNumber,
//            pin: nil
//        )
//        print("📦 Request DTO created:")
//        print("   mode: VEHICLE_NUMBER")
//        print("   vehicleNumber: \(vehicleNumber)")
//        print("   qrCode: nil")
//        print("   pin: nil")
//        
//        // Encode to JSON
//        guard let jsonData = try? JSONEncoder().encode(dto) else {
//            print("❌ Failed to encode CheckInRequestDto")
//            completion(.failure(NSError(domain: "EncodingError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode check-out request"])))
//            return
//        }
//        
//        // Print the exact JSON being sent
//        if let jsonString = String(data: jsonData, encoding: .utf8) {
//            print("📤 REQUEST BODY JSON:")
//            print(jsonString)
//        }
//        
//        print("📊 Request body size: \(jsonData.count) bytes")
//        
//        request.httpBody = jsonData
//        
//        print("🚀 Sending network request...")
//        print("⏰ Request sent at: \(Date())")
//        
//        // Make the API call
//        let startTime = Date()
//        session.dataTask(with: request) { data, response, error in
//            let endTime = Date()
//            let responseTime = endTime.timeIntervalSince(startTime)
//            print("⏱️ Response received in \(String(format: "%.2f", responseTime))s")
//            
//            // Handle network errors
//            if let error = error {
//                print("❌ NETWORK ERROR:")
//                print("   Description: \(error.localizedDescription)")
//                print("   Domain: \((error as NSError).domain)")
//                print("   Code: \((error as NSError).code)")
//                completion(.failure(error))
//                return
//            }
//            
//            // Check HTTP response
//            if let httpResponse = response as? HTTPURLResponse {
//                print("📡 HTTP RESPONSE:")
//                print("   Status Code: \(httpResponse.statusCode)")
//                print("   Status Description: \(HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode))")
//                
//                // Log response headers
//                print("📋 Response Headers:")
//                httpResponse.allHeaderFields.forEach { key, value in
//                    print("   \(key): \(value)")
//                }
//                
//                // Handle rate limiting (429)
//                if httpResponse.statusCode == 429 {
//                    print("⚠️ RATE LIMIT EXCEEDED!")
//                    let rateLimitError = NSError(
//                        domain: "APIError",
//                        code: 429,
//                        userInfo: [NSLocalizedDescriptionKey: "Rate limit exceeded. Please wait."]
//                    )
//                    completion(.failure(rateLimitError))
//                    return
//                }
//                
//                // Handle error status codes (400-599)
//                if httpResponse.statusCode >= 400 {
//                    let errorMessage = String(data: data ?? Data(), encoding: .utf8) ?? "Unknown error"
//                    print("❌ SERVER ERROR RESPONSE:")
//                    print("   Status: \(httpResponse.statusCode)")
//                    print("   Body: \(errorMessage)")
//                    
//                    let apiError = NSError(
//                        domain: "APIError",
//                        code: httpResponse.statusCode,
//                        userInfo: [NSLocalizedDescriptionKey: errorMessage]
//                    )
//                    completion(.failure(apiError))
//                    return
//                }
//            } else {
//                print("⚠️ No HTTP response available")
//            }
//            
//            // Validate data
//            guard let data = data else {
//                print("❌ NO DATA received from server")
//                completion(.failure(NSError(domain: "NoDataError", code: 2, userInfo: [NSLocalizedDescriptionKey: "No data received from server"])))
//                return
//            }
//            
//            print("📥 Response data size: \(data.count) bytes")
//            
//            // Print raw response
//            if let jsonString = String(data: data, encoding: .utf8) {
//                print("📥 RAW RESPONSE BODY:")
//                print(jsonString)
//            }
//            
//            // Decode response
//            print("🔄 Attempting to decode Bookings object...")
//            do {
//                let decoder = JSONDecoder()
//                let booking = try decoder.decode(Bookings.self, from: data)
//                print("✅ CHECK-OUT SUCCESSFUL!")
//                print("   Booking ID: \(booking.id ?? "nil")")
//                print("   Vehicle Number: \(booking.vehicleNumber ?? "nil")")
//                print("   Status: \(booking.status ?? "nil")")
//                print("   Check-out Time: \(booking.checkOutTime?.description ?? "nil")")
//                print("   Total Amount: ₹\(booking.totalAmount ?? 0.0)")
//                print("🎉 === CHECK-OUT API CALL COMPLETED ===")
//                completion(.success(booking))
//            } catch let decodingError as DecodingError {
//                print("❌ DECODING ERROR:")
//                switch decodingError {
//                case .keyNotFound(let key, let context):
//                    print("   Missing key: '\(key.stringValue)'")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                case .typeMismatch(let type, let context):
//                    print("   Type mismatch for type: \(type)")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                case .valueNotFound(let type, let context):
//                    print("   Value not found for type: \(type)")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                case .dataCorrupted(let context):
//                    print("   Data corrupted")
//                    print("   Context: \(context.debugDescription)")
//                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
//                @unknown default:
//                    print("   Unknown decoding error: \(decodingError)")
//                }
//                completion(.failure(decodingError))
//            } catch {
//                print("❌ UNEXPECTED ERROR during decoding:")
//                print("   \(error.localizedDescription)")
//                completion(.failure(error))
//            }
//        }.resume()
//    }
//}
//
//        
//


import Foundation

enum APIError: Error {
    case badURL
    case badResponse
    case decodingError(Error)
    case serverError(Int)
    case unknown(Error)
    case authenticationRequired
    case invalidURL
    case networkError(Error)
    case noData
    case invalidResponse

    var localizedDescription: String {
        switch self {
        case .badURL:
            return "Invalid URL"
        case .badResponse:
            return "Bad response"
        case .decodingError(let error):
            return "Decode error: \(error.localizedDescription)"
        case .serverError(let code):
            return "Server error: \(code)"
        case .unknown(let error):
            return "Unknown error: \(error.localizedDescription)"
        case .authenticationRequired:
            return "Authentication required"
        case .invalidURL:
            return "Invalid URL"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .noData:
            return "No data returned"
        case .invalidResponse:
            return "Invalid Response"
        }
    }
}
 
// MARK: - SSL Delegate
class SSLAllowingSessionDelegate: NSObject, URLSessionDelegate {
    func urlSession(_ session: URLSession,
                    didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        
        let authMethod = challenge.protectionSpace.authenticationMethod
        
        if authMethod == NSURLAuthenticationMethodServerTrust {
            if let serverTrust = challenge.protectionSpace.serverTrust {
                print("✅ Accepting SSL certificate for: \(challenge.protectionSpace.host)")
                completionHandler(.useCredential, URLCredential(trust: serverTrust))
                return
            }
        }
        
        if authMethod == NSURLAuthenticationMethodHTTPBasic {
            if let username = UserDefaults.standard.string(forKey: "api_username"),
               let password = UserDefaults.standard.string(forKey: "api_password") {
                print("✅ Using Basic Auth credentials")
                let credential = URLCredential(user: username, password: password, persistence: .forSession)
                completionHandler(.useCredential, credential)
                return
            }
        }
        
        completionHandler(.performDefaultHandling, nil)
    }
}

// MARK: - QR Validation Result

// MARK: - API Service
class APIService {
    static let shared = APIService()
//    s/*tatic let backendBaseURL = "https://172.20.10.4:8443/api"*/
 static let backendBaseURL = "https://gridee.onrender.com/api"
//    static let backendBaseURL = "http://"
////    static let backendBaseURL = "http://127.0.0.1:8080/api"

    let session: URLSession
    
    private let apiUsername = "rajeev"
    private let apiPassword = "parking"
    
    // ✅ JWT Token reference
    var authViewModel: AuthViewModel?
    
    private init() {
        UserDefaults.standard.set(apiUsername, forKey: "api_username")
        UserDefaults.standard.set(apiPassword, forKey: "api_password")
        
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        
        self.session = URLSession(configuration: config, delegate: SSLAllowingSessionDelegate(), delegateQueue: nil)
        
        print("🚀 APIService initialized with credentials: \(apiUsername)")
        print("🌐 Backend URL: \(APIService.backendBaseURL)")
    }
    
    // ✅ Set AuthViewModel reference
    func setAuthViewModel(_ viewModel: AuthViewModel) {
        self.authViewModel = viewModel
        print("🔗 AuthViewModel connected to APIService")
        if let token = viewModel.jwtToken {
            print("🔑 JWT Token available: \(token.prefix(20))...")
        } else {
            print("⚠️ No JWT token available yet")
        }
    }
    private let publicEndpoints = [
        "/auth/register",
        "/auth/login",
        "/parking-lots",
        "/parking-lots/list/by-names"
    ]
    
    // ✅ Check if endpoint is public (no JWT required)
    private func isPublicEndpoint(_ endpoint: String) -> Bool {
        return publicEndpoints.contains { endpoint.contains($0) }
    }
    
    // MARK: - Authentication Headers
    
    // ✅ Smart auth header - tries JWT first, falls back to Basic Auth
    private func createAuthHeader() -> String {
        // Priority 1: Try JWT token
        if let token = authViewModel?.jwtToken {
            print("🔑 Using JWT Bearer token: \(token.prefix(20))...")
            return "Bearer \(token)"
        }
        
        // Priority 2: Fallback to Basic Auth
        print("🔑 Using Basic Auth (no JWT token available)")
        return createBasicAuthHeader()
    }
    
    // ✅ Basic Auth fallback
    private func createBasicAuthHeader() -> String {
        let loginString = "\(apiUsername):\(apiPassword)"
        guard let loginData = loginString.data(using: .utf8) else {
            print("❌ Failed to create auth data")
            return ""
        }
        let base64LoginString = loginData.base64EncodedString()
        return "Basic \(base64LoginString)"
    }
    
    // ✅ Helper to add auth header
    //    private func addAuthHeader(to request: inout URLRequest) {
    //        request.setValue(createAuthHeader(), forHTTPHeaderField: "Authorization")
    //    }
    // ✅ FIXED: Make addAuthHeader work with static method
    func addAuthHeader(to request: inout URLRequest, endpoint: String = "") {
        if isPublicEndpoint(endpoint) {
            print("🌐 PUBLIC: \(endpoint)")
            return
        }
        
        // ✅ Try JWT first
        if let token = UserDefaults.standard.string(forKey: "jwtToken"), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            print("✅ JWT: \(token.prefix(30))...")
            return
        }
        
        // ⚠️ TEST: If JWT fails, try Basic Auth
        print("⚠️ Falling back to Basic Auth")
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64 = loginData.base64EncodedString()
            request.setValue("Basic \(base64)", forHTTPHeaderField: "Authorization")
            print("✅ Basic Auth added")
        }
    }
    
    
    
    // ✅ FIXED: Static method properly sets JWT
//    static func fetchParkingSpots(completion: @escaping ([ParkingSpot]?, Error?) -> Void) {
//        guard let url = URL(string: backendBaseURL + "/parking-spots") else {
//            completion(nil, APIError.badURL)
//            return
//        }
//
//        var request = URLRequest(url: url)
//        request.httpMethod = "GET"
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//
//        // ✅ Use the helper function
//        shared.addAuthHeader(to: &request, endpoint: "/parking-spots")
//
//        print("🚀 Fetching parking spots with JWT: \(request.value(forHTTPHeaderField: "Authorization") != nil ? "YES" : "NO")")
//
//        shared.session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(nil, APIError.badResponse)
//                    return
//                }
//
//                if httpResponse.statusCode == 403 {
//                    print("❌ 403 Forbidden - JWT token rejected by backend")
//                    print("   Check if token is expired or invalid")
//                    completion(nil, APIError.authenticationRequired)
//                    return
//                }
//
//                if let error = error {
//                    completion(nil, error)
//                    return
//                }
//
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    completion(nil, APIError.serverError(httpResponse.statusCode))
//                    return
//                }
//
//                guard let data = data else {
//                    completion(nil, APIError.noData)
//                    return
//                }
//
//                do {
//                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
//                    print("✅ Got \(spots.count) parking spots")
//                    completion(spots, nil)
//                } catch {
//                    completion(nil, error)
//                }
//            }
//        }.resume()
//    }
//
    static func fetchParkingSpots(completion: @escaping (Result<[ParkingSpot], APIError>) -> Void) {
        // Build URL
        let endpoint = "/parking-spots"
        guard let url = URL(string: backendBaseURL + endpoint) else {
            print("❌ fetchParkingSpots: Invalid URL")
            completion(.failure(.badURL))
            return
        }

        // Build request
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        // 🔑 Attach JWT (same as wallet call)
        if let token = UserDefaults.standard.string(forKey: "jwtToken"), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            print("🔑 fetchParkingSpots using JWT: \(token.prefix(20))…")
        } else {
            print("❌ fetchParkingSpots: No JWT token found")
            completion(.failure(.authenticationRequired))
            return
        }

        print("🚀 GET \(url.absoluteString)")

        // Execute request
        shared.session.dataTask(with: request) { data, response, error in
            // Network / transport error
            if let error = error {
                print("❌ fetchParkingSpots network error: \(error.localizedDescription)")
                completion(.failure(.networkError(error)))
                return
            }

            // Response validation
            guard let httpResponse = response as? HTTPURLResponse else {
                print("❌ fetchParkingSpots: No HTTPURLResponse")
                completion(.failure(.badResponse))
                return
            }

            print("📡 fetchParkingSpots status: \(httpResponse.statusCode)")

            if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                print("❌ fetchParkingSpots: Auth failure (status \(httpResponse.statusCode))")
                completion(.failure(.authenticationRequired))
                return
            }

            guard (200...299).contains(httpResponse.statusCode) else {
                print("❌ fetchParkingSpots: Server error \(httpResponse.statusCode)")
                completion(.failure(.serverError(httpResponse.statusCode)))
                return
            }

            guard let data = data else {
                print("❌ fetchParkingSpots: No data")
                completion(.failure(.noData))
                return
            }

            // Decode JSON
            do {
                let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
                print("✅ fetchParkingSpots decoded \(spots.count) spots")
                completion(.success(spots))
            } catch {
                print("❌ fetchParkingSpots decoding error: \(error.localizedDescription)")
                completion(.failure(.decodingError(error)))
            }
        }.resume()
    }

    
    // MARK: - Users
    func fetchUsers(completion: @escaping (Result<[Users], APIError>) -> Void) {
        makeRequest(endpoint: "/users", completion: completion)
    }
    
    func register(name: String, email: String, phone: String, password: String, parkingLotName: String, completion: @escaping (Result<JWTLoginResponse, APIError>) -> Void) {
        guard let url = URL(string: APIService.backendBaseURL + "/auth/register") else {
            completion(.failure(.badURL))
            return
        }
        
        let body: [String: Any] = [
            "name": name,
            "email": email,
            "phone": phone,
            "password": password,
            "parkingLotName": parkingLotName
        ]
        
        guard let jsonData = try? JSONSerialization.data(withJSONObject: body) else {
            completion(.failure(.badResponse))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonData
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode),
                      let data = data else {
                    completion(.failure(.badResponse))
                    return
                }
                
                do {
                    let response = try JSONDecoder().decode(JWTLoginResponse.self, from: data)
                    completion(.success(response))
                } catch {
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    
    // MARK: - Parking Spots
    private func getParkingSpots(completion: @escaping ([ParkingSpot]?, Error?) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/parking-spots") else {
            print("❌ Invalid parking spots URL")
            completion(nil, APIError.badURL)
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        // ✅ ADD JWT TOKEN
        if let token = UserDefaults.standard.string(forKey: "jwtToken") {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            print("🔑 JWT token added to parking spots request")
        } else if let token = authViewModel?.jwtToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            print("🔑 JWT token added to parking spots request")
        }
        
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        print("🚀 Fetching parking spots from: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error.localizedDescription)")
                    completion(nil, error)
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid HTTP response")
                    completion(nil, APIError.badResponse)
                    return
                }
                
                print("📡 Parking spots response status: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Authentication failed for parking spots")
                    completion(nil, APIError.authenticationRequired)
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error: \(httpResponse.statusCode)")
                    completion(nil, APIError.serverError(httpResponse.statusCode))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data received")
                    completion(nil, APIError.badResponse)
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw parking spots response:")
                    print(jsonString)
                }
                
                do {
                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
                    print("✅ Successfully decoded \(spots.count) parking spots")
                    completion(spots, nil)
                } catch {
                    print("❌ JSON decoding error: \(error)")
                    completion(nil, error)
                }
            }
        }.resume()
    }
    
//    func fetchAvailableSpots(
//        lotId: String,
//        startTime: Date,
//        endTime: Date,
//        completion: @escaping (Result<[ParkingSpot], APIError>) -> Void
//    ) {
//        let formatter = ISO8601DateFormatter()
//        formatter.formatOptions = [.withInternetDateTime]
//        formatter.timeZone = TimeZone.current
//
//        let startTimeString = formatter.string(from: startTime)
//        let endTimeString = formatter.string(from: endTime)
//
//        guard let encodedStartTime = startTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
//              let encodedEndTime = endTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
//            completion(.failure(.badURL))
//            return
//        }
//
//        let endpoint = "/parking-spots/available?lotId=\(lotId)&startTime=\(encodedStartTime)&endTime=\(encodedEndTime)"
//
//        print("📡 Fetching available spots:")
//        print("   Endpoint: \(endpoint)")
//
//        // ✅ CREATE REQUEST WITH JWT
//        guard let url = URL(string: Self.backendBaseURL + endpoint) else {
//            completion(.failure(.badURL))
//            return
//        }
//
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//
//        // ✅ ADD JWT TOKEN
//        if let token = UserDefaults.standard.string(forKey: "jwtToken") {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("🔑 JWT token added to available spots request")
//        } else if let token = authViewModel?.jwtToken {
//            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
//            print("🔑 JWT token added to available spots request")
//        }
//
//        session.dataTask(with: request) { data, response, error in
//            DispatchQueue.main.async {
//                if let error = error {
//                    print("❌ Network error: \(error.localizedDescription)")
//                    completion(.failure(.networkError(error)))
//                    return
//                }
//
//                guard let httpResponse = response as? HTTPURLResponse else {
//                    completion(.failure(.badResponse))
//                    return
//                }
//
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//
//                guard let data = data else {
//                    completion(.failure(.noData))
//                    return
//                }
//
//                do {
//                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
//                    print("✅ Fetched \(spots.count) available spots")
//                    completion(.success(spots))
//                } catch {
//                    print("❌ Decode error: \(error)")
//                    completion(.failure(.decodingError(error)))
//                }
//            }
//        }.resume()
//    }
//
    func fetchAvailableSpots(
        lotId: String,
        startTime: Date,
        endTime: Date,
        completion: @escaping (Result<[ParkingSpot], APIError>) -> Void
    ) {
        // ✅ Formatter compatible with Spring ZonedDateTime
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withTimeZone]

        let startTimeString = formatter.string(from: startTime)
        let endTimeString = formatter.string(from: endTime)

        // ✅ Endpoint (POST)
        let endpoint = "/api/parking-spots/available"

        guard let url = URL(string: Self.backendBaseURL + endpoint) else {
            completion(.failure(.badURL))
            return
        }

        // ✅ Build request
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // 🔑 JWT
        guard let token = UserDefaults.standard.string(forKey: "jwtToken"),
              !token.isEmpty else {
            completion(.failure(.authenticationRequired))
            return
        }
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        // ✅ IMPORTANT:
        // Spring @RequestParam with POST still reads query params
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "lotId", value: lotId),
            URLQueryItem(name: "startTime", value: startTimeString),
            URLQueryItem(name: "endTime", value: endTimeString)
        ]
        request.url = components.url

        print("🚀 POST \(request.url!.absoluteString)")
        print("   LotId: \(lotId)")
        print("   Start: \(startTimeString)")
        print("   End: \(endTimeString)")

        // ✅ Execute
        session.dataTask(with: request) { data, response, error in

            if let error = error {
                completion(.failure(.networkError(error)))
                return
            }

            guard let httpResponse = response as? HTTPURLResponse else {
                completion(.failure(.badResponse))
                return
            }

            print("📡 fetchAvailableSpots status:", httpResponse.statusCode)

            if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                completion(.failure(.authenticationRequired))
                return
            }

            guard (200...299).contains(httpResponse.statusCode) else {
                completion(.failure(.serverError(httpResponse.statusCode)))
                return
            }

            guard let data = data else {
                completion(.failure(.noData))
                return
            }

            do {
                let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
                print("✅ Decoded \(spots.count) available spots")
                completion(.success(spots))
            } catch {
                completion(.failure(.decodingError(error)))
            }
        }.resume()
    }

    
    // MARK: - Bookings
    func fetchBookings(completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        guard let userId = getCurrentUserId() else {
            print("❌ No user ID found for bookings")
            completion(.failure(.authenticationRequired))
            return
        }
        fetchUserBookings(userId: userId, completion: completion)
    }
    
    // ✅ FIXED: Fetch User Bookings with proper endpoint
    //    func fetchUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
    //        // ✅ Use /all endpoint instead of whatever you're using now
    //        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/all") else {
    //            completion(.failure(.badURL))
    //            return
    //        }
    //
    //        var request = URLRequest(url: url)
    //        request.httpMethod = "GET"
    //        request.setValue("application/json", forHTTPHeaderField: "Accept")
    //
    //        // ✅ Add JWT token - CRITICAL!
    //        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/all")
    //
    //        print("📡 Fetching bookings from: \(url.absoluteString)")
    //
    //        session.dataTask(with: request) { data, response, error in
    //            DispatchQueue.main.async {
    //                if let error = error {
    //                    print("❌ Network error: \(error)")
    //                    completion(.failure(.networkError(error)))
    //                    return
    //                }
    //
    //                guard let httpResponse = response as? HTTPURLResponse else {
    //                    completion(.failure(.badResponse))
    //                    return
    //                }
    //
    //                print("📡 Response status: \(httpResponse.statusCode)")
    //
    //                // ✅ Debug 403 issue
    //                if httpResponse.statusCode == 403 {
    //                    print("❌ 403 Forbidden - JWT might be invalid or expired")
    //                    print("📋 Auth header: \(request.value(forHTTPHeaderField: "Authorization") ?? "MISSING")")
    //                    completion(.failure(.authenticationRequired))
    //                    return
    //                }
    //
    //                guard (200...299).contains(httpResponse.statusCode) else {
    //                    print("❌ Server error: \(httpResponse.statusCode)")
    //                    completion(.failure(.serverError(httpResponse.statusCode)))
    //                    return
    //                }
    //
    //                guard let data = data else {
    //                    completion(.failure(.noData))
    //                    return
    //                }
    //
    //                do {
    //                    let bookings = try JSONDecoder().decode([Bookings].self, from: data)
    //                    print("✅ Fetched \(bookings.count) bookings")
    //                    completion(.success(bookings))
    //                } catch {
    //                    print("❌ Decode error: \(error)")
    //                    completion(.failure(.decodingError(error)))
    //                }
    //            }
    //        }.resume()
    //    }
    
    
    func fetchAllUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        makeRequest(endpoint: "/users/\(userId)/all-bookings", completion: completion)
    }
    
    func fetchBookingHistory(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        makeRequest(endpoint: "/users/\(userId)/all-bookings/history", completion: completion)
    }
    
    func fetchBookingById(userId: String, bookingId: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)", completion: completion)
    }
    // MARK: - Public Parking Spots (No Authentication Required)
    
    /// Fetch parking spots without authentication (used during signup)
    //    func fetchParkingSpotsPublic(completion: @escaping (Result<[ParkingSpot], APIError>) -> Void) {
    //        guard let url = URL(string: Self.backendBaseURL + "/parking-spots") else {
    //            print("❌ Invalid parking spots URL")
    //            completion(.failure(.badURL))
    //            return
    //        }
    //
    //        var request = URLRequest(url: url)
    //        // ✅ NO AUTH HEADER - Public endpoint for signup
    //        request.setValue("application/json", forHTTPHeaderField: "Accept")
    //        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
    //
    //        print("🚀 Fetching parking spots (public) from: \(url.absoluteString)")
    //
    //        session.dataTask(with: request) { data, response, error in
    //            DispatchQueue.main.async {
    //                if let error = error {
    //                    print("❌ Network error: \(error.localizedDescription)")
    //                    completion(.failure(.networkError(error)))
    //                    return
    //                }
    //
    //                guard let httpResponse = response as? HTTPURLResponse else {
    //                    print("❌ Invalid HTTP response")
    //                    completion(.failure(.badResponse))
    //                    return
    //                }
    //
    //                print("📡 Parking spots (public) response status: \(httpResponse.statusCode)")
    //
    //                guard (200...299).contains(httpResponse.statusCode) else {
    //                    print("❌ Server error: \(httpResponse.statusCode)")
    //                    completion(.failure(.serverError(httpResponse.statusCode)))
    //                    return
    //                }
    //
    //                guard let data = data else {
    //                    print("❌ No data received")
    //                    completion(.failure(.noData))
    //                    return
    //                }
    //
    //                if let jsonString = String(data: data, encoding: .utf8) {
    //                    print("📋 Raw parking spots (public) response:")
    //                    print(jsonString)
    //                }
    //
    //                do {
    //                    let spots = try JSONDecoder().decode([ParkingSpot].self, from: data)
    //                    print("✅ Successfully decoded \(spots.count) parking spots (public)")
    //                    completion(.success(spots))
    //                } catch {
    //                    print("❌ JSON decoding error: \(error)")
    //                    completion(.failure(.decodingError(error)))
    //                }
    //            }
    //        }.resume()
    //    }
    
    /// Fetch available parking lot names for signup dropdown
    // MARK: - Fetch Parking Lot Names (Public - No Authentication)
    
    /// Fetch all parking lot names for signup dropdown
    /// Endpoint: GET /api/parking-lots/list/by-names
    // ✅ NEW: Fetch parking lots (NO JWT required - public endpoint)
    // ✅ FIXED: Fetch parking lots WITHOUT JWT token
    func fetchParkingLots(completion: @escaping (Result<[String], APIError>) -> Void) {
        guard let url = URL(string: APIService.backendBaseURL + "/parking-lots/list/by-names") else {
            completion(.failure(.badURL))
           

            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // ❌ DON'T add JWT token here
        // This is a public endpoint for signup
        
        print("📥 Fetching parking lots (no auth)...")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📥 Response: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    // Decode based on your backend response format
                    if let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                        let lotNames = jsonArray.compactMap { $0["name"] as? String }
                        print("✅ Loaded \(lotNames.count) parking lots")
                        completion(.success(lotNames))
                    } else if let jsonArray = try JSONSerialization.jsonObject(with: data) as? [String] {
                        print("✅ Loaded \(jsonArray.count) parking lots")
                        completion(.success(jsonArray))
                    } else {
                        completion(.failure(.invalidResponse))
                    }
                } catch {
                    print("❌ JSON error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    
    //    func createBooking(
    //        spotId: String,
    //        userId: String,
    //        lotId: String,
    //        vehicleNumber: String?,
    //        checkInTime: String,
    //        checkOutTime: String,
    //        completion: @escaping (Result<Bookings, APIError>) -> Void
    //    ) {
    //        guard let currentUserId = getCurrentUserId(), userId == currentUserId else {
    //            print("❌ User ID mismatch: \(userId) vs \(getCurrentUserId() ?? "nil")")
    //            completion(.failure(.authenticationRequired))
    //            return
    //        }
    //
    //        guard !spotId.isEmpty, !userId.isEmpty, !lotId.isEmpty else {
    //            print("❌ Missing required booking parameters")
    //            completion(.failure(.badResponse))
    //            return
    //        }
    //
    //        var queryItems = [
    //            URLQueryItem(name: "spotId", value: spotId),
    //            URLQueryItem(name: "userId", value: userId),
    //            URLQueryItem(name: "lotId", value: lotId),
    //            URLQueryItem(name: "checkInTime", value: checkInTime),
    //            URLQueryItem(name: "checkOutTime", value: checkOutTime)
    //        ]
    //
    //        if let vehicle = vehicleNumber, !vehicle.isEmpty {
    //            queryItems.append(URLQueryItem(name: "vehicleNumber", value: vehicle))
    //        }
    //
    //        guard var components = URLComponents(string: Self.backendBaseURL + "/bookings/\(userId)/create") else {
    //            print("❌ Failed to create booking URL components")
    //            completion(.failure(.badURL))
    //            return
    //        }
    //
    //        components.queryItems = queryItems
    //
    //        guard let url = components.url else {
    //            print("❌ Failed to create final booking URL")
    //            completion(.failure(.badURL))
    //            return
    //        }
    //
    //        var request = URLRequest(url: url)
    //        request.httpMethod = "POST"
    //        addAuthHeader(to: &request)
    //        request.setValue("application/json", forHTTPHeaderField: "Accept")
    //        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
    //
    //        print("🚀 Creating booking at: \(url.absoluteString)")
    //
    //        session.dataTask(with: request) { data, response, error in
    //            DispatchQueue.main.async {
    //                if let error = error {
    //                    print("❌ Booking network error: \(error.localizedDescription)")
    //                    completion(.failure(.unknown(error)))
    //                    return
    //                }
    //
    //                guard let httpResponse = response as? HTTPURLResponse else {
    //                    print("❌ Invalid booking response")
    //                    completion(.failure(.badResponse))
    //                    return
    //                }
    //
    //                print("📡 Booking response status: \(httpResponse.statusCode)")
    //
    //                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
    //                    print("❌ Booking authentication failed")
    //                    completion(.failure(.authenticationRequired))
    //                    return
    //                }
    //
    //                guard let data = data else {
    //                    print("❌ No booking data received")
    //                    completion(.failure(.badResponse))
    //                    return
    //                }
    //
    //                if let jsonString = String(data: data, encoding: .utf8) {
    //                    print("📋 Raw booking response:")
    //                    print(jsonString)
    //                }
    //
    //                if (200...299).contains(httpResponse.statusCode) {
    //                    do {
    //                        let booking = try JSONDecoder().decode(Bookings.self, from: data)
    //                        print("✅ Booking created successfully: \(booking.id)")
    //                        completion(.success(booking))
    //                    } catch {
    //                        print("❌ Booking JSON decoding error: \(error)")
    //                        print("⚠️ Attempting to extract booking ID from raw response")
    //
    //                        if let jsonObject = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
    //                           let bookingId = jsonObject["id"] as? String {
    //                            print("✅ Extracted real booking ID: \(bookingId)")
    //                            let fallbackBooking = self.createFallbackBooking(
    //                                bookingId: bookingId,
    //                                spotId: spotId,
    //                                userId: userId,
    //                                lotId: lotId,
    //                                vehicleNumber: vehicleNumber ?? "UNKNOWN",
    //                                checkInTime: checkInTime,
    //                                checkOutTime: checkOutTime
    //                            )
    //                            completion(.success(fallbackBooking))
    //                        } else {
    //                            print("❌ Could not extract booking ID from response")
    //                            completion(.failure(.decodingError(error)))
    //                        }
    //                    }
    //                } else {
    //                    print("❌ Booking server error: \(httpResponse.statusCode)")
    //                    completion(.failure(.serverError(httpResponse.statusCode)))
    //                }
    //            }
    //        }.resume()
    //    }
    // ✅ CREATE BOOKING - Send as JSON body, not query params
    //    func createBooking(spotId: String, userId: String, lotId: String,
    //                       vehicleNumber: String, checkInTime: String, checkOutTime: String,
    //                       completion: @escaping (Result<Bookings, APIError>) -> Void) {
    //
    //        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/create") else {
    //            completion(.failure(.badURL))
    //            return
    //        }
    //
    //        var request = URLRequest(url: url)
    //        request.httpMethod = "POST"
    //        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    //
    //        // ✅ Add JWT token
    //        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/create")
    //
    //        // ✅ Create JSON body (NOT query parameters!)
    //        let body: [String: Any] = [
    //            "spotId": spotId,
    //            "lotId": lotId,
    //            "checkInTime": checkInTime,
    //            "checkOutTime": checkOutTime,
    //            "vehicleNumber": vehicleNumber
    //        ]
    //
    //        do {
    //            request.httpBody = try JSONSerialization.data(withJSONObject: body)
    //            print("📤 Creating booking:")
    //            print("   Spot: \(spotId)")
    //            print("   Vehicle: \(vehicleNumber)")
    //            print("   Check-in: \(checkInTime)")
    //            print("   Check-out: \(checkOutTime)")
    //        } catch {
    //            completion(.failure(.unknown(error)))
    //            return
    //        }
    //
    //        session.dataTask(with: request) { data, response, error in
    //            DispatchQueue.main.async {
    //                if let error = error {
    //                    print("❌ Network error: \(error)")
    //                    completion(.failure(.networkError(error)))
    //                    return
    //                }
    //
    //                guard let httpResponse = response as? HTTPURLResponse else {
    //                    completion(.failure(.badResponse))
    //                    return
    //                }
    //
    //                print("📡 Booking Response: \(httpResponse.statusCode)")
    //
    //                guard (200...299).contains(httpResponse.statusCode) else {
    //                    print("❌ Server error: \(httpResponse.statusCode)")
    //                    completion(.failure(.serverError(httpResponse.statusCode)))
    //                    return
    //                }
    //
    //                guard let data = data else {
    //                    completion(.failure(.noData))
    //                    return
    //                }
    //
    //                do {
    //                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
    //                    print("✅ Booking created: \(booking.id)")
    //                    completion(.success(booking))
    //                } catch {
    //                    print("❌ Decode error: \(error)")
    //                    completion(.failure(.decodingError(error)))
    //                }
    //            }
    //        }.resume()
    //    }
    // ✅ MARK: - Booking API Methods (Matching Backend Controller)
    
    // ✅ Create Booking
    func createBooking(spotId: String, userId: String, lotId: String,
                       vehicleNumber: String, checkInTime: String, checkOutTime: String,
                       completion: @escaping (Result<Bookings, APIError>) -> Void) {
        
        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/create") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/create")
        
        let body: [String: Any] = [
            "spotId": spotId,
            "lotId": lotId,
            "checkInTime": checkInTime,
            "checkOutTime": checkOutTime,
            "vehicleNumber": vehicleNumber
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            print("📤 Creating booking at: \(url.absoluteString)")
        } catch {
            completion(.failure(.unknown(error)))
            return
        }
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Response: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Booking created: \(booking.id)")
                    completion(.success(booking))
                } catch {
                    print("❌ Decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    // ✅ Fetch All Bookings for User (GET /api/bookings/{userId}/all)
    func fetchUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/all") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/all")
        
        print("📡 Fetching bookings from: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Booking fetch response: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 403 {
                    print("❌ 403 Forbidden - JWT token issue")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    let bookings = try JSONDecoder().decode([Bookings].self, from: data)
                    print("✅ Fetched \(bookings.count) bookings")
                    for booking in bookings {
                        print("   - \(booking.id): \(booking.status)")
                    }
                    completion(.success(bookings))
                } catch {
                    print("❌ Decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    // ✅ Get Booking by ID (GET /api/bookings/{userId}/{bookingId})
    func getBooking(userId: String, bookingId: String,
                    completion: @escaping (Result<Bookings, APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/\(bookingId)") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/\(bookingId)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode),
                      let data = data else {
                    completion(.failure(.badResponse))
                    return
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    completion(.success(booking))
                } catch {
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    // ✅ Cancel Booking (POST /api/bookings/{userId}/{bookingId}/cancel)
    // ✅ In APIService.swift - Make sure this exists
    // ✅ KEEP ONLY THIS ONE - Delete any other cancelBooking methods
    // ✅ KEEP ONLY THIS ONE - Delete both old ones
    func cancelBooking(userId: String, bookingId: String,
                       completion: @escaping (Result<Void, APIError>) -> Void) {
        // ✅ Backend endpoint from your controller: POST /api/bookings/{userId}/{bookingId}/cancel
        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/\(bookingId)/cancel") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/\(bookingId)/cancel")
        
        print("🗑️ Cancelling booking at: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Bad response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Cancel response: \(httpResponse.statusCode)")
                
                // ✅ Accept both 200-299 and 204 No Content
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                print("✅ Booking cancelled successfully")
                completion(.success(()))
            }
        }.resume()
    }
    
    
    
    // ✅ Extend Booking (PUT /api/bookings/{userId}/{bookingId}/extend)
    func extendBooking(userId: String, bookingId: String, newCheckOutTime: String,
                       completion: @escaping (Result<Bookings, APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/\(bookingId)/extend") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/\(bookingId)/extend")
        
        let body: [String: String] = ["newCheckOutTime": newCheckOutTime]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            completion(.failure(.unknown(error)))
            return
        }
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode),
                      let data = data else {
                    completion(.failure(.badResponse))
                    return
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Booking extended")
                    completion(.success(booking))
                } catch {
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    // ✅ Get Booking History (GET /api/bookings/{userId}/all/history)
    func getBookingHistory(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/bookings/\(userId)/all/history") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        addAuthHeader(to: &request, endpoint: "/bookings/\(userId)/all/history")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode),
                      let data = data else {
                    completion(.failure(.badResponse))
                    return
                }
                
                do {
                    let bookings = try JSONDecoder().decode([Bookings].self, from: data)
                    print("✅ Fetched booking history: \(bookings.count) bookings")
                    completion(.success(bookings))
                } catch {
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    
    private func createFallbackBooking(
        bookingId: String,
        spotId: String,
        userId: String,
        lotId: String,
        vehicleNumber: String,
        checkInTime: String,
        checkOutTime: String
    ) -> Bookings {
        let fallbackData: [String: Any] = [
            "id": bookingId,
            "spotId": spotId,
            "userId": userId,
            "lotId": lotId,
            "status": "PENDING",
            "vehicleNumbers": vehicleNumber,
            "checkInTime": checkInTime,
            "checkOutTime": checkOutTime,
            "totalHours": 1.0,
            "totalAmount": 5.0,
            "amount": 5.0,
            "qrCodeScanned": false
        ]
        
        do {
            let data = try JSONSerialization.data(withJSONObject: fallbackData)
            let booking = try JSONDecoder().decode(Bookings.self, from: data)
            print("✅ Created fallback booking with ID: \(bookingId)")
            return booking
        } catch {
            print("❌ Failed to create fallback booking: \(error)")
            fatalError("Critical error creating fallback booking")
        }
    }
    
    // MARK: - Additional Booking Operations
    
    func confirmBooking(userId: String, bookingId: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/confirm"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid confirm booking URL")
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        addAuthHeader(to: &request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        print("🚀 Confirming booking at: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Confirm booking network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid confirm booking response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Confirm booking response status: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Confirm booking authentication failed")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Confirm booking server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No confirm booking data received")
                    completion(.failure(.noData))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw confirm booking response:")
                    print(jsonString)
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Booking confirmed successfully: \(booking.id) - Status: \(booking.status)")
                    completion(.success(booking))
                } catch {
                    print("❌ Confirm booking decoding error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    //    func cancelBooking(userId: String, bookingId: String, completion: @escaping (Result<Bool, APIError>) -> Void) {
    //        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)/cancel") else {
    //            completion(.failure(.badURL))
    //            return
    //        }
    //
    //        var request = URLRequest(url: url)
    //        request.httpMethod = "POST"
    //        addAuthHeader(to: &request)
    //
    //        session.dataTask(with: request) { _, response, error in
    //            DispatchQueue.main.async {
    //                if let error = error {
    //                    completion(.failure(.unknown(error)))
    //                    return
    //                }
    //
    //                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 {
    //                    completion(.success(true))
    //                } else {
    //                    completion(.failure(.badResponse))
    //                }
    //            }
    //        }.resume()
    //    }
    
    func validateQRCheckIn(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/validate-qr-checkin"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        addAuthHeader(to: &request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let body: [String: String] = ["qrCode": qrCode]
        request.httpBody = try? JSONEncoder().encode(body)
        
        print("🚀 Step 1: Validating QR code")
        print("   URL: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Validation response status: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
                        print("📋 Error response: \(errorMsg)")
                    }
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data in response")
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    let validationResult = try JSONDecoder().decode(QrValidationResult.self, from: data)
                    print("✅ Validation Result:")
                    print("   Valid: \(validationResult.valid)")
                    print("   Message: \(validationResult.message ?? "nil")")
                    if let penalty = validationResult.penalty {
                        print("   Penalty: ₹\(penalty)")
                    }
                    
                    if validationResult.valid {
                        print("🚀 Step 2: Performing check-in")
                        self.performCheckIn(userId: userId, bookingId: bookingId, qrCode: qrCode, completion: completion)
                    } else {
                        print("❌ QR validation failed: \(validationResult.message ?? "Unknown error")")
                        completion(.failure(.serverError(400)))
                    }
                } catch {
                    print("❌ Failed to decode validation result: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    func validateQRCheckOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/validate-qr-checkout"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        addAuthHeader(to: &request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let body: [String: String] = ["qrCode": qrCode]
        request.httpBody = try? JSONEncoder().encode(body)
        
        print("🚀 Step 1: Validating QR code for checkout")
        print("   URL: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Validation response status: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
                        print("📋 Error response: \(errorMsg)")
                    }
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data in response")
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    let validationResult = try JSONDecoder().decode(QrValidationResult.self, from: data)
                    print("✅ Validation Result:")
                    print("   Valid: \(validationResult.valid)")
                    print("   Message: \(validationResult.message ?? "nil")")
                    if let penalty = validationResult.penalty {
                        print("   Penalty: ₹\(penalty)")
                    }
                    
                    if validationResult.valid {
                        print("🚀 Step 2: Performing checkout")
                        self.performCheckOut(userId: userId, bookingId: bookingId, qrCode: qrCode, completion: completion)
                    } else {
                        print("❌ QR validation failed: \(validationResult.message ?? "Unknown error")")
                        completion(.failure(.serverError(400)))
                    }
                } catch {
                    print("❌ Failed to decode validation result: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    private func performCheckOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/checkout"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid checkout URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        addAuthHeader(to: &request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let body: [String: String] = ["qrCode": qrCode]
        request.httpBody = try? JSONEncoder().encode(body)
        
        print("   URL: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Checkout network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid checkout response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Checkout response status: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
                        print("📋 Checkout error: \(errorMsg)")
                    }
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No checkout data")
                    completion(.failure(.noData))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw checkout response:")
                    print(jsonString)
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Checkout completed successfully!")
                    print("   Status: \(booking.status)")
                    print("   Check-out Time: \(booking.checkOutTime ?? "nil")")
                    print("   Total Amount: ₹\(booking.totalAmount)")
                    completion(.success(booking))
                } catch {
                    print("❌ Checkout decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    private func performCheckIn(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/checkin"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid check-in URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        addAuthHeader(to: &request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let body: [String: String] = ["qrCode": qrCode]
        request.httpBody = try? JSONEncoder().encode(body)
        
        print("   URL: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Check-in network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid check-in response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Check-in response status: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
                        print("📋 Check-in error: \(errorMsg)")
                    }
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No check-in data")
                    completion(.failure(.noData))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw check-in response:")
                    print(jsonString)
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Check-in completed successfully!")
                    print("   Status: \(booking.status)")
                    print("   Check-in Time: \(booking.checkInTime ?? "nil")")
                    completion(.success(booking))
                } catch {
                    print("❌ Check-in decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    func getPenaltyInfo(userId: String, bookingId: String, completion: @escaping (Result<Double, APIError>) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)/penalty") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        addAuthHeader(to: &request)
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(.unknown(error)))
                    return
                }
                
                guard let data = data,
                      let penalty = try? JSONDecoder().decode(Double.self, from: data) else {
                    completion(.failure(.badResponse))
                    return
                }
                
                completion(.success(penalty))
            }
        }.resume()
    }
    
    func updateBookingStatus(userId: String, bookingId: String, status: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let bodyDict: [String: Any] = ["status": status]
        guard let bodyData = try? JSONSerialization.data(withJSONObject: bodyDict) else {
            completion(.failure(.badResponse))
            return
        }
        
        makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)", method: "PUT", body: bodyData, completion: completion)
    }
    
    func deleteBooking(userId: String, bookingId: String, completion: @escaping (Result<Bool, APIError>) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        addAuthHeader(to: &request)
        
        session.dataTask(with: request) { _, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(.unknown(error)))
                    return
                }
                
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 {
                    completion(.success(true))
                } else {
                    completion(.failure(.badResponse))
                }
            }
        }.resume()
    }
    
    // MARK: - Configuration
    //    func fetchParkingConfig(completion: @escaping (Result<ParkingConfig, APIError>) -> Void) {
    //        makeRequest(endpoint: "/config/parking-rates", completion: completion)
    //    }
    
    // MARK: - Wallet Methods
    func fetchWallet(userId: String, completion: @escaping (Result<Wallet, APIError>) -> Void) {
        print("📡 Fetching wallet for user: \(userId)")
        makeRequest(endpoint: "/users/\(userId)/wallet", completion: completion)
    }
    
    func fetchWalletTransactions(userId: String, completion: @escaping (Result<[Transactions], APIError>) -> Void) {
        print("📡 Fetching wallet transactions for user: \(userId)")
        makeRequest(endpoint: "/users/\(userId)/wallet/transactions", completion: completion)
    }
    
    func topUpWallet(userId: String, amount: Double, completion: @escaping (Result<Wallet, APIError>) -> Void) {
        print("📡 Topping up wallet for user: \(userId), amount: \(amount)")
        
        let bodyDict: [String: Any] = ["amount": amount]
        guard let bodyData = try? JSONSerialization.data(withJSONObject: bodyDict) else {
            print("❌ Failed to serialize wallet topup data")
            completion(.failure(.badResponse))
            return
        }
        
        makeRequest(endpoint: "/users/\(userId)/wallet/topup", method: "POST", body: bodyData, completion: completion)
    }
    
    // MARK: - Helper Methods
    private func getCurrentUserId() -> String? {
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            return userId
        }
        
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            UserDefaults.standard.set(user.id, forKey: "currentUserId")
            return user.id
        }
        
        return nil
    }
    
    // In APIService.swift
    
    // ✅ GET /api/users/{userId}/vehicles
    // ✅ FIXED: Handle response as dictionary
    func fetchUserVehicles(userId: String, completion: @escaping (Result<[String], APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/vehicles") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // ✅ Add JWT token
        addAuthHeader(to: &request, endpoint: "/users/\(userId)/vehicles")
        
        print("📥 Fetching vehicles for user: \(userId)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Response: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                
                // ✅ Debug: Print raw response
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw response: \(jsonString)")
                }
                
                do {
                    // ✅ FIXED: Decode as dictionary first, then extract vehicleNumbers
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let vehicleNumbers = json["vehicleNumbers"] as? [String] {
                        print("✅ Fetched \(vehicleNumbers.count) vehicles: \(vehicleNumbers)")
                        completion(.success(vehicleNumbers))
                    } else {
                        // ✅ Fallback: Try to decode as direct array
                        let vehicles = try JSONDecoder().decode([String].self, from: data)
                        print("✅ Fetched \(vehicles.count) vehicles (direct array)")
                        completion(.success(vehicles))
                    }
                } catch {
                    print("❌ Decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    
    // ✅ PUT /api/users/{userId}/add-vehicles
    func addVehicleToBackend(userId: String, vehicleNumber: String, completion: @escaping (Result<[String], APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/add-vehicles") else {
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = authViewModel?.jwtToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let body: [String] = [vehicleNumber]
        
        do {
            request.httpBody = try JSONEncoder().encode(body)
        } catch {
            completion(.failure(.unknown(error)))
            return
        }
        
        print("🚗 Adding vehicle to backend: \(vehicleNumber)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Response status: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    let vehicles = try JSONDecoder().decode([String].self, from: data)
                    print("✅ Backend returned \(vehicles.count) vehicles")
                    completion(.success(vehicles))
                } catch {
                    print("❌ Decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    
    
    // MARK: - Core Network Method
    private func makeRequest<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        queryItems: [URLQueryItem]? = nil,
        body: Data? = nil,
        completion: @escaping (Result<T, APIError>) -> Void
    ) {
        guard var components = URLComponents(string: Self.backendBaseURL + endpoint) else {
            print("❌ Invalid URL components for endpoint: \(endpoint)")
            completion(.failure(.badURL))
            return
        }
        
        components.queryItems = queryItems
        
        guard let url = components.url else {
            print("❌ Failed to create URL for endpoint: \(endpoint)")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        addAuthHeader(to: &request)
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        if let body = body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        } else {
            request.setValue("application/json", forHTTPHeaderField: "Accept")
        }
        
        print("🚀 \(method) request to: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Request failed: \(error.localizedDescription)")
                    completion(.failure(.unknown(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid HTTP response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Response status: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Authentication failed")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data received")
                    completion(.failure(.badResponse))
                    return
                }
                
                do {
                    let result = try JSONDecoder().decode(T.self, from: data)
                    print("✅ Successfully decoded response")
                    completion(.success(result))
                } catch {
                    print("❌ JSON decoding failed: \(error)")
                    if let jsonString = String(data: data, encoding: .utf8) {
                        print("📋 Raw response: \(jsonString)")
                    }
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    func extendBooking(
        userId: String,
        bookingId: String,
        newCheckOutTime: String,
        completion: @escaping (Result<Bookings, Error>) -> Void
    ) {
        let urlString = "\(APIService.backendBaseURL)/users/\(userId)/bookings/\(bookingId)/extend"
        
        guard let url = URL(string: urlString) else {
            completion(.failure(APIError.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        addAuthHeader(to: &request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "newCheckOutTime": newCheckOutTime
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            completion(.failure(error))
            return
        }
        
        print("🔄 Extending booking: \(bookingId) to new checkout: \(newCheckOutTime)")
        
        session.dataTask(with: request) { data, response, error in
            if let error = error {
                print("❌ Extend booking error: \(error)")
                completion(.failure(error))
                return
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                completion(.failure(APIError.badResponse))
                return
            }
            
            guard (200...299).contains(httpResponse.statusCode) else {
                print("❌ Server returned status code: \(httpResponse.statusCode)")
                completion(.failure(APIError.serverError(httpResponse.statusCode)))
                return
            }
            
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            
            do {
                let updatedBooking = try JSONDecoder().decode(Bookings.self, from: data)
                print("✅ Booking extended successfully")
                completion(.success(updatedBooking))
            } catch {
                print("❌ Decode error: \(error)")
                completion(.failure(error))
            }
        }.resume()
    }
    // In APIService.swift new function taaki nayi vehicle apne londe daal ske apne ape mein
    
    
    //    func addVehicleToBackend(userId: String, vehicleNumber: String, completion: @escaping (Result<[String], APIError>) -> Void) {
    //        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/add-vehicles") else {
    //            completion(.failure(.invalidURL))
    //            return
    //        }
    //
    //        var request = URLRequest(url: url)
    //        request.httpMethod = "PUT"
    //        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    //
    //        // ✅ Add JWT token for authentication
    //        if let token = authViewModel?.jwtToken {
    //            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    //        }
    //
    //        // ✅ Send as array (your backend expects List<String>)
    //        let body: [String] = [vehicleNumber]
    //
    //        do {
    //            request.httpBody = try JSONEncoder().encode(body)
    //        } catch {
    //            completion(.failure(.unknown(error)))
    //            return
    //        }
    //
    //        print("🚗 Adding vehicle to backend: \(vehicleNumber)")
    //        print("   URL: \(url.absoluteString)")
    //
    //        session.dataTask(with: request) { data, response, error in
    //            DispatchQueue.main.async {
    //                if let error = error {
    //                    print("❌ Network error: \(error)")
    //                    completion(.failure(.networkError(error)))
    //                    return
    //                }
    //
    //                guard let httpResponse = response as? HTTPURLResponse else {
    //                    print("❌ Invalid response")
    //                    completion(.failure(.badResponse))
    //                    return
    //                }
    //
    //                print("📡 Response status: \(httpResponse.statusCode)")
    //
    //                guard (200...299).contains(httpResponse.statusCode) else {
    //                    print("❌ Server error: \(httpResponse.statusCode)")
    //                    completion(.failure(.serverError(httpResponse.statusCode)))
    //                    return
    //                }
    //
    //                guard let data = data else {
    //                    print("❌ No data")
    //                    completion(.failure(.noData))
    //                    return
    //                }
    //
    //                do {
    //                    // ✅ Backend returns List<String> of all vehicles
    //                    let vehicles = try JSONDecoder().decode([String].self, from: data)
    //                    print("✅ Vehicle added successfully. Total vehicles: \(vehicles)")
    //                    completion(.success(vehicles))
    //                } catch {
    //                    print("❌ Decode error: \(error)")
    //                    completion(.failure(.decodingError(error)))
    //                }
    //            }
    //        }.resume()
    //    }
    // In APIService.swift
    
    func fetchUserProfileFromBackend(userId: String, completion: @escaping (Result<Users, APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)") else {
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        // Add JWT token
        if let token = authViewModel?.jwtToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        print("📥 Fetching user profile from backend: \(userId)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error")
                    completion(.failure(.badResponse))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data")
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    let user = try JSONDecoder().decode(Users.self, from: data)
                    print("✅ User profile decoded successfully")
                    print("   Vehicles count: \(user.vehicleNumbers?.count ?? 0)")
                    completion(.success(user))
                } catch {
                    print("❌ Decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    // ✅ FIXED: Now accepts userId parameter
    func getBookingById(userId: String, bookingId: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        // ✅ Use the format: GET /api/users/{userId}/bookings/{bookingId}
        let endpoint = "/users/\(userId)/bookings/\(bookingId)"
        
        guard let url = URL(string: "\(APIService.backendBaseURL)\(endpoint)") else {
            print("❌ Invalid URL")
            print("   Base: \(APIService.backendBaseURL)")
            print("   Endpoint: \(endpoint)")
            completion(.failure(APIError.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(to: &request)
        
        print("\n========== GET BOOKING BY ID ==========")
        print("📍 Full URL: \(url.absoluteString)")
        print("👤 User ID: \(userId)")
        print("🔑 Booking ID: \(bookingId)")
        print("=======================================\n")
        
        session.dataTask(with: request) { data, response, error in
            if let error = error {
                print("❌ Network error: \(error.localizedDescription)")
                DispatchQueue.main.async {
                    completion(.failure(error))
                }
                return
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                DispatchQueue.main.async {
                    completion(.failure(APIError.badResponse))
                }
                return
            }
            
            print("📊 Status: \(httpResponse.statusCode)")
            
            guard let data = data else {
                print("❌ No data")
                DispatchQueue.main.async {
                    completion(.failure(APIError.noData))
                }
                return
            }
            
            if let jsonString = String(data: data, encoding: .utf8) {
                print("📄 Response: \(jsonString)")
            }
            
            if (200...299).contains(httpResponse.statusCode) {
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("\n✅ Booking fetched successfully!")
                    print("   Booking ID: \(booking.id)")
                    print("   User ID: \(booking.userId)")
                    print("   Status: \(booking.status)")
                    print("   Vehicle: \(booking.vehicleNumber ?? "N/A")")
                    print("=======================================\n")
                    
                    DispatchQueue.main.async {
                        completion(.success(booking))
                    }
                } catch {
                    print("❌ Decode error: \(error)")
                    DispatchQueue.main.async {
                        completion(.failure(error))
                    }
                }
            } else {
                let errorMsg = String(data: data, encoding: .utf8) ?? "Unknown error"
                print("❌ HTTP Error \(httpResponse.statusCode): \(errorMsg)")
                DispatchQueue.main.async {
                    completion(.failure(APIError.serverError(httpResponse.statusCode)))
                }
            }
        }.resume()
    }
    
    // ✅ Add Vehicle to Backend
    // ✅ Add Vehicle to Backend
    // ✅ Make sure this is also correct
    func addVehiclesToBackend(userId: String, vehicleNumbers: [String],
                              completion: @escaping (Result<[String], APIError>) -> Void) {
        guard let url = URL(string: "\(Self.backendBaseURL)/users/\(userId)/add-vehicles") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // ✅ Add JWT token
        addAuthHeader(to: &request, endpoint: "/users/\(userId)/add-vehicles")
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: vehicleNumbers)
            print("📤 Sending vehicles: \(vehicleNumbers)")
        } catch {
            completion(.failure(.unknown(error)))
            return
        }
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Response: \(httpResponse.statusCode)")
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                
                do {
                    // ✅ FIXED: Handle dictionary response
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let vehicles = json["vehicleNumbers"] as? [String] {
                        print("✅ Vehicles saved: \(vehicles)")
                        completion(.success(vehicles))
                    } else {
                        completion(.failure(.decodingError(NSError())))
                    }
                } catch {
                    print("❌ Decode error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    struct CheckInRequestDto: Codable {
        let mode: String
        let qrCode: String?
        let vehicleNumber: String?
        let pin: String?
        
        enum CodingKeys: String, CodingKey {
            case mode, qrCode, vehicleNumber, pin
        }
        
        init(mode: String, qrCode: String? = nil, vehicleNumber: String? = nil, pin: String? = nil) {
            self.mode = mode
            self.qrCode = qrCode
            self.vehicleNumber = vehicleNumber
            self.pin = pin
        }
    }
    
    // ✅ Check-in for operator (VEHICLE_NUMBER)
    func checkInOperator(vehicleNumber: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        print("🚀 === CHECK-IN API CALL STARTED ===")
        print("📍 Vehicle Number: \(vehicleNumber)")
        
        // Construct URL
        let endpoint = "/bookings/checkin"
        let fullURL = "\(APIService.backendBaseURL)\(endpoint)"
        print("🌐 Full URL: \(fullURL)")
        
        guard let url = URL(string: fullURL) else {
            print("❌ Invalid URL: \(fullURL)")
            completion(.failure(NSError(domain: "URLError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"])))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        print("✅ HTTP Method: POST")
        print("✅ Content-Type: application/json")
        
        // Add auth header
        addAuthHeader(to: &request, endpoint: endpoint)
        
        // Log all headers (mask JWT for security)
        print("📋 Request Headers:")
        request.allHTTPHeaderFields?.forEach { key, value in
            if key.lowercased() == "authorization" {
                let maskedToken = String(value.prefix(20)) + "..."
                print("   \(key): \(maskedToken)")
            } else {
                print("   \(key): \(value)")
            }
        }
        
        // Create request DTO
        let dto = CheckInRequestDto(
            mode: "VEHICLE_NUMBER",
            qrCode: nil,
            vehicleNumber: vehicleNumber,
            pin: nil
        )
        print("📦 Request DTO created:")
        print("   mode: VEHICLE_NUMBER")
        print("   vehicleNumber: \(vehicleNumber)")
        print("   qrCode: nil")
        print("   pin: nil")
        
        // Encode to JSON
        guard let jsonData = try? JSONEncoder().encode(dto) else {
            print("❌ Failed to encode CheckInRequestDto")
            completion(.failure(NSError(domain: "EncodingError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode check-in request"])))
            return
        }
        
        // Print the exact JSON being sent
        if let jsonString = String(data: jsonData, encoding: .utf8) {
            print("📤 REQUEST BODY JSON:")
            print(jsonString)
        }
        
        print("📊 Request body size: \(jsonData.count) bytes")
        
        request.httpBody = jsonData
        
        print("🚀 Sending network request...")
        print("⏰ Request sent at: \(Date())")
        
        // Make the API call
        let startTime = Date()
        session.dataTask(with: request) { data, response, error in
            let endTime = Date()
            let responseTime = endTime.timeIntervalSince(startTime)
            print("⏱️ Response received in \(String(format: "%.2f", responseTime))s")
            
            // Handle network errors
            if let error = error {
                print("❌ NETWORK ERROR:")
                print("   Description: \(error.localizedDescription)")
                print("   Domain: \((error as NSError).domain)")
                print("   Code: \((error as NSError).code)")
                completion(.failure(error))
                return
            }
            
            // Check HTTP response
            if let httpResponse = response as? HTTPURLResponse {
                print("📡 HTTP RESPONSE:")
                print("   Status Code: \(httpResponse.statusCode)")
                print("   Status Description: \(HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode))")
                
                // Log response headers
                print("📋 Response Headers:")
                httpResponse.allHeaderFields.forEach { key, value in
                    print("   \(key): \(value)")
                }
                
                // Handle rate limiting (429)
                if httpResponse.statusCode == 429 {
                    print("⚠️ RATE LIMIT EXCEEDED!")
                    let rateLimitError = NSError(
                        domain: "APIError",
                        code: 429,
                        userInfo: [NSLocalizedDescriptionKey: "Rate limit exceeded. Please wait."]
                    )
                    completion(.failure(rateLimitError))
                    return
                }
                
                // Handle error status codes (400-599)
                if httpResponse.statusCode >= 400 {
                    let errorMessage = String(data: data ?? Data(), encoding: .utf8) ?? "Unknown error"
                    print("❌ SERVER ERROR RESPONSE:")
                    print("   Status: \(httpResponse.statusCode)")
                    print("   Body: \(errorMessage)")
                    
                    let apiError = NSError(
                        domain: "APIError",
                        code: httpResponse.statusCode,
                        userInfo: [NSLocalizedDescriptionKey: errorMessage]
                    )
                    completion(.failure(apiError))
                    return
                }
            } else {
                print("⚠️ No HTTP response available")
            }
            
            // Validate data
            guard let data = data else {
                print("❌ NO DATA received from server")
                completion(.failure(NSError(domain: "NoDataError", code: 2, userInfo: [NSLocalizedDescriptionKey: "No data received from server"])))
                return
            }
            
            print("📥 Response data size: \(data.count) bytes")
            
            // Print raw response
            if let jsonString = String(data: data, encoding: .utf8) {
                print("📥 RAW RESPONSE BODY:")
                print(jsonString)
            }
            
            // Decode response
            print("🔄 Attempting to decode Bookings object...")
            do {
                let decoder = JSONDecoder()
                let booking = try decoder.decode(Bookings.self, from: data)
                print("✅ CHECK-IN SUCCESSFUL!")
                print("   Booking ID: \(booking.id ?? "nil")")
                print("   Vehicle Number: \(booking.vehicleNumber ?? "nil")")
                print("   Status: \(booking.status ?? "nil")")
                print("   Check-in Time: \(booking.checkInTime?.description ?? "nil")")
                print("🎉 === CHECK-IN API CALL COMPLETED ===")
                completion(.success(booking))
            } catch let decodingError as DecodingError {
                print("❌ DECODING ERROR:")
                switch decodingError {
                case .keyNotFound(let key, let context):
                    print("   Missing key: '\(key.stringValue)'")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                case .typeMismatch(let type, let context):
                    print("   Type mismatch for type: \(type)")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                case .valueNotFound(let type, let context):
                    print("   Value not found for type: \(type)")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                case .dataCorrupted(let context):
                    print("   Data corrupted")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                @unknown default:
                    print("   Unknown decoding error: \(decodingError)")
                }
                completion(.failure(decodingError))
            } catch {
                print("❌ UNEXPECTED ERROR during decoding:")
                print("   \(error.localizedDescription)")
                completion(.failure(error))
            }
        }.resume()
    }
    
    // ✅ Check-out for operator (VEHICLE_NUMBER)
    func checkOutOperator(vehicleNumber: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        print("🚀 === CHECK-OUT API CALL STARTED ===")
        print("📍 Vehicle Number: \(vehicleNumber)")
        
        // Construct URL
        let endpoint = "/bookings/checkout"
        let fullURL = "\(APIService.backendBaseURL)\(endpoint)"
        print("🌐 Full URL: \(fullURL)")
        
        guard let url = URL(string: fullURL) else {
            print("❌ Invalid URL: \(fullURL)")
            completion(.failure(NSError(domain: "URLError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"])))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        print("✅ HTTP Method: POST")
        print("✅ Content-Type: application/json")
        
        // Add auth header
        addAuthHeader(to: &request, endpoint: endpoint)
        
        // Log all headers (mask JWT for security)
        print("📋 Request Headers:")
        request.allHTTPHeaderFields?.forEach { key, value in
            if key.lowercased() == "authorization" {
                let maskedToken = String(value.prefix(20)) + "..."
                print("   \(key): \(maskedToken)")
            } else {
                print("   \(key): \(value)")
            }
        }
        
        // Create request DTO
        let dto = CheckInRequestDto(
            mode: "VEHICLE_NUMBER",
            qrCode: nil,
            vehicleNumber: vehicleNumber,
            pin: nil
        )
        print("📦 Request DTO created:")
        print("   mode: VEHICLE_NUMBER")
        print("   vehicleNumber: \(vehicleNumber)")
        print("   qrCode: nil")
        print("   pin: nil")
        
        // Encode to JSON
        guard let jsonData = try? JSONEncoder().encode(dto) else {
            print("❌ Failed to encode CheckInRequestDto")
            completion(.failure(NSError(domain: "EncodingError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode check-out request"])))
            return
        }
        
        // Print the exact JSON being sent
        if let jsonString = String(data: jsonData, encoding: .utf8) {
            print("📤 REQUEST BODY JSON:")
            print(jsonString)
        }
        
        print("📊 Request body size: \(jsonData.count) bytes")
        
        request.httpBody = jsonData
        
        print("🚀 Sending network request...")
        print("⏰ Request sent at: \(Date())")
        
        // Make the API call
        let startTime = Date()
        session.dataTask(with: request) { data, response, error in
            let endTime = Date()
            let responseTime = endTime.timeIntervalSince(startTime)
            print("⏱️ Response received in \(String(format: "%.2f", responseTime))s")
            
            // Handle network errors
            if let error = error {
                print("❌ NETWORK ERROR:")
                print("   Description: \(error.localizedDescription)")
                print("   Domain: \((error as NSError).domain)")
                print("   Code: \((error as NSError).code)")
                completion(.failure(error))
                return
            }
            
            // Check HTTP response
            if let httpResponse = response as? HTTPURLResponse {
                print("📡 HTTP RESPONSE:")
                print("   Status Code: \(httpResponse.statusCode)")
                print("   Status Description: \(HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode))")
                
                // Log response headers
                print("📋 Response Headers:")
                httpResponse.allHeaderFields.forEach { key, value in
                    print("   \(key): \(value)")
                }
                
                // Handle rate limiting (429)
                if httpResponse.statusCode == 429 {
                    print("⚠️ RATE LIMIT EXCEEDED!")
                    let rateLimitError = NSError(
                        domain: "APIError",
                        code: 429,
                        userInfo: [NSLocalizedDescriptionKey: "Rate limit exceeded. Please wait."]
                    )
                    completion(.failure(rateLimitError))
                    return
                }
                
                // Handle error status codes (400-599)
                if httpResponse.statusCode >= 400 {
                    let errorMessage = String(data: data ?? Data(), encoding: .utf8) ?? "Unknown error"
                    print("❌ SERVER ERROR RESPONSE:")
                    print("   Status: \(httpResponse.statusCode)")
                    print("   Body: \(errorMessage)")
                    
                    let apiError = NSError(
                        domain: "APIError",
                        code: httpResponse.statusCode,
                        userInfo: [NSLocalizedDescriptionKey: errorMessage]
                    )
                    completion(.failure(apiError))
                    return
                }
            } else {
                print("⚠️ No HTTP response available")
            }
            
            // Validate data
            guard let data = data else {
                print("❌ NO DATA received from server")
                completion(.failure(NSError(domain: "NoDataError", code: 2, userInfo: [NSLocalizedDescriptionKey: "No data received from server"])))
                return
            }
            
            print("📥 Response data size: \(data.count) bytes")
            
            // Print raw response
            if let jsonString = String(data: data, encoding: .utf8) {
                print("📥 RAW RESPONSE BODY:")
                print(jsonString)
            }
            
            // Decode response
            print("🔄 Attempting to decode Bookings object...")
            do {
                let decoder = JSONDecoder()
                let booking = try decoder.decode(Bookings.self, from: data)
                print("✅ CHECK-OUT SUCCESSFUL!")
                print("   Booking ID: \(booking.id ?? "nil")")
                print("   Vehicle Number: \(booking.vehicleNumber ?? "nil")")
                print("   Status: \(booking.status ?? "nil")")
                print("   Check-out Time: \(booking.checkOutTime?.description ?? "nil")")
                print("   Total Amount: ₹\(booking.totalAmount ?? 0.0)")
                print("🎉 === CHECK-OUT API CALL COMPLETED ===")
                completion(.success(booking))
            } catch let decodingError as DecodingError {
                print("❌ DECODING ERROR:")
                switch decodingError {
                case .keyNotFound(let key, let context):
                    print("   Missing key: '\(key.stringValue)'")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                case .typeMismatch(let type, let context):
                    print("   Type mismatch for type: \(type)")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                case .valueNotFound(let type, let context):
                    print("   Value not found for type: \(type)")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                case .dataCorrupted(let context):
                    print("   Data corrupted")
                    print("   Context: \(context.debugDescription)")
                    print("   Coding path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> "))")
                @unknown default:
                    print("   Unknown decoding error: \(decodingError)")
                }
                completion(.failure(decodingError))
            } catch {
                print("❌ UNEXPECTED ERROR during decoding:")
                print("   \(error.localizedDescription)")
                completion(.failure(error))
            }
        }.resume()
    }
}

        

