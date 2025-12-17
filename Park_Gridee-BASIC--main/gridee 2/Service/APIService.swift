





import Foundation

enum APIError: Error {
    case badURL
    case badResponse
    case decodingError(Error)
    case serverError(Int)
    case unknown(Error)
    case authenticationRequired
    case invalidURL              // ✅ ADD THIS
    case networkError(Error)
    case noData

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
        case .networkError(_):
            return "Network error"
        case .noData:
            return "No data returned"
        }
    }
}

class SSLAllowingSessionDelegate: NSObject, URLSessionDelegate {
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        
        let authMethod = challenge.protectionSpace.authenticationMethod
        
        // Handle SSL certificate challenges (for HTTPS)
        if authMethod == NSURLAuthenticationMethodServerTrust {
            if let serverTrust = challenge.protectionSpace.serverTrust {
                print("🔐 Accepting SSL certificate for: \(challenge.protectionSpace.host)")
                completionHandler(.useCredential, URLCredential(trust: serverTrust))
                return
            }
        }
        
        // Handle Basic Authentication challenges
        if authMethod == NSURLAuthenticationMethodHTTPBasic {
            if let username = UserDefaults.standard.string(forKey: "api_username"),
               let password = UserDefaults.standard.string(forKey: "api_password") {
                print("🔐 Using stored credentials for Basic Auth")
                let credential = URLCredential(user: username, password: password, persistence: .forSession)
                completionHandler(.useCredential, credential)
                return
            }
        }
        
        completionHandler(.performDefaultHandling, nil)
    }
}

class APIService {
    static let shared = APIService()
//    static let backendBaseURL = "https://10.223.73.57:8443/api"
//    static let backendBaseURL = "https://ec2-3-109-153-101.ap-south-1.compute.amazonaws.com:8443/api"
    static let backendBaseURL = "https://10.241.234.164:8443/api"
//    static let backendBaseURL = "https://10.241.234.164:8443/api"
    
    
    let session: URLSession
    
    private let apiUsername = "rajeev"
    private let apiPassword = "parking"
    
    private init() {
        UserDefaults.standard.set(apiUsername, forKey: "api_username")
        UserDefaults.standard.set(apiPassword, forKey: "api_password")
        
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        
        self.session = URLSession(configuration: config, delegate: SSLAllowingSessionDelegate(), delegateQueue: nil)
        
        print("🚀 APIService initialized with credentials: \(apiUsername)")
    }

    // MARK: - Authentication Helper
    private func createBasicAuthHeader() -> String {
        let loginString = "\(apiUsername):\(apiPassword)"
        guard let loginData = loginString.data(using: .utf8) else {
            print("❌ Failed to create auth data")
            return ""
        }
        let base64LoginString = loginData.base64EncodedString()
        return "Basic \(base64LoginString)"
    }

    // MARK: - Static Methods
    static func fetchParkingSpots(completion: @escaping ([ParkingSpot]?, Error?) -> Void) {
        shared.getParkingSpots(completion: completion)
    }

    // MARK: - Users
    func fetchUsers(completion: @escaping (Result<[Users], APIError>) -> Void) {
        makeRequest(endpoint: "/users", completion: completion)
    }

    func registerUser(user: Users, password: String, completion: @escaping (Result<Users, APIError>) -> Void) {
        let bodyDict: [String: Any] = [
            "name": user.name,
            "email": user.email,
            "phone": user.phone,
            "vehicleNumber": user.vehicleNumber ?? "",
            "passwordHash": password
        ]

        guard let bodyData = try? JSONSerialization.data(withJSONObject: bodyDict) else {
            print("❌ Failed to serialize registration data")
            completion(.failure(.badResponse))
            return
        }

        makeRequest(endpoint: "/users/register", method: "POST", body: bodyData, completion: completion)
    }

    // MARK: - Parking Spots
    private func getParkingSpots(completion: @escaping ([ParkingSpot]?, Error?) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/parking-spots") else {
            print("❌ Invalid parking spots URL")
            completion(nil, APIError.badURL)
            return
        }

        var request = URLRequest(url: url)
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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
    
    func fetchAvailableSpots(
        lotId: String,
        startTime: Date,
        endTime: Date,
        completion: @escaping (Result<[ParkingSpot], APIError>) -> Void
    ) {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        formatter.timeZone = TimeZone.current
        
        let startTimeString = formatter.string(from: startTime)
        let endTimeString = formatter.string(from: endTime)
        
        guard let encodedStartTime = startTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let encodedEndTime = endTimeString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            completion(.failure(.badURL))
            return
        }
        
        let endpoint = "/parking-spots/available?lotId=\(lotId)&startTime=\(encodedStartTime)&endTime=\(encodedEndTime)"
        
        print("📡 Fetching available spots:")
        print("   Endpoint: \(endpoint)")
        
        makeRequest(endpoint: endpoint, method: "POST", completion: completion)
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

    func fetchUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        print("📡 Fetching ALL bookings and filtering for user: \(userId)")
        
        guard let url = URL(string: Self.backendBaseURL + "/bookings") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
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
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(.noData))
                    return
                }
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw bookings response:")
                    print(jsonString)
                }
                
                // ✅ Parse JSON manually and skip bookings without _id
                do {
                    guard let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
                        completion(.failure(.badResponse))
                        return
                    }
                    
                    var validBookings: [Bookings] = []
                    
                    for (index, bookingDict) in jsonArray.enumerated() {
                        // ✅ Only process bookings with _id
                        guard bookingDict["id"] != nil else {
                            print("⚠️ Skipping booking at index \(index) - missing _id")
                            continue
                        }
                        
                        // Convert back to JSON data
                        let bookingData = try JSONSerialization.data(withJSONObject: bookingDict)
                        
                        // Try to decode
                        if let booking = try? JSONDecoder().decode(Bookings.self, from: bookingData) {
                            if booking.userId == userId {
                                validBookings.append(booking)
                                print("✅ Booking: \(booking.id), Status: \(booking.status)")
                            }
                        } else {
                            print("⚠️ Failed to decode booking at index \(index)")
                        }
                    }
                    
                    print("✅ Found \(validBookings.count) valid bookings for user \(userId)")
                    completion(.success(validBookings))
                    
                } catch {
                    print("❌ JSON parsing error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }

    
    func fetchAllUserBookings(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        makeRequest(endpoint: "/users/\(userId)/all-bookings", completion: completion)
    }
    
    func fetchBookingHistory(userId: String, completion: @escaping (Result<[Bookings], APIError>) -> Void) {
        makeRequest(endpoint: "/users/\(userId)/all-bookings/history", completion: completion)
    }
    
    func fetchBookingById(userId: String, bookingId: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)", completion: completion)
    }
    func createBooking(
        spotId: String,
        userId: String,
        lotId: String,
        vehicleNumber: String?,
        checkInTime: String,
        checkOutTime: String,
        completion: @escaping (Result<Bookings, APIError>) -> Void
    ) {
        guard let currentUserId = getCurrentUserId(), userId == currentUserId else {
            print("❌ User ID mismatch: \(userId) vs \(getCurrentUserId() ?? "nil")")
            completion(.failure(.authenticationRequired))
            return
        }
        
        guard !spotId.isEmpty, !userId.isEmpty, !lotId.isEmpty else {
            print("❌ Missing required booking parameters")
            completion(.failure(.badResponse))
            return
        }
        
        var queryItems = [
            URLQueryItem(name: "spotId", value: spotId),
            URLQueryItem(name: "userId", value: userId),
            URLQueryItem(name: "lotId", value: lotId),
            URLQueryItem(name: "checkInTime", value: checkInTime),
            URLQueryItem(name: "checkOutTime", value: checkOutTime)
        ]
        
        if let vehicle = vehicleNumber, !vehicle.isEmpty {
            queryItems.append(URLQueryItem(name: "vehicleNumber", value: vehicle))
        }
        
        guard var components = URLComponents(string: Self.backendBaseURL + "/users/\(userId)/bookings/start") else {
            print("❌ Failed to create booking URL components")
            completion(.failure(.badURL))
            return
        }
        
        components.queryItems = queryItems
        
        guard let url = components.url else {
            print("❌ Failed to create final booking URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        print("🚀 Creating booking at: \(url.absoluteString)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Booking network error: \(error.localizedDescription)")
                    completion(.failure(.unknown(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid booking response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Booking response status: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Booking authentication failed")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                guard let data = data else {
                    print("❌ No booking data received")
                    completion(.failure(.badResponse))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw booking response:")
                    print(jsonString)
                }
                
                if (200...299).contains(httpResponse.statusCode) {
                    do {
                        let booking = try JSONDecoder().decode(Bookings.self, from: data)
                        print("✅ Booking created successfully: \(booking.id)")
                        completion(.success(booking))
                    } catch {
                        print("❌ Booking JSON decoding error: \(error)")
                        print("⚠️ Attempting to extract booking ID from raw response")
                        
                        // ✅ Try to extract the real booking ID from the JSON response
                        if let jsonObject = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                           let bookingId = jsonObject["id"] as? String ?? jsonObject["id"] as? String {
                            print("✅ Extracted real booking ID: \(bookingId)")
                            let fallbackBooking = self.createFallbackBooking(
                                bookingId: bookingId,  // ✅ Use REAL ID from server
                                spotId: spotId,
                                userId: userId,
                                lotId: lotId,
                                vehicleNumber: vehicleNumber ?? "UNKNOWN",
                                checkInTime: checkInTime,
                                checkOutTime: checkOutTime
                            )
                            completion(.success(fallbackBooking))
                        } else {
                            print("❌ Could not extract booking ID from response")
                            completion(.failure(.decodingError(error)))
                        }
                    }
                } else {
                    print("❌ Booking server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                }
                
            }
            
        }.resume()
    }

    private func createFallbackBooking(
        bookingId: String,  // ✅ CHANGED: Accept real booking ID as parameter
        spotId: String,
        userId: String,
        lotId: String,
        vehicleNumber: String,
        checkInTime: String,
        checkOutTime: String
    ) -> Bookings {
        let fallbackData: [String: Any] = [
            "id": bookingId,  // ✅ CHANGED: Use real ID instead of UUID().uuidString
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
    
    // ✅ ADD THIS METHOD
    func confirmBooking(userId: String, bookingId: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/confirm"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid confirm booking URL")
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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


    func cancelBooking(userId: String, bookingId: String, completion: @escaping (Result<Bool, APIError>) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)/cancel") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
        
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

    func checkIn(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        // ✅ Validate inputs
        guard !userId.isEmpty, !bookingId.isEmpty, !qrCode.isEmpty else {
            print("❌ Missing required check-in parameters")
            completion(.failure(.badURL))
            return
        }
        
        // ✅ Clean the inputs
        let cleanUserId = userId.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanBookingId = bookingId.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanQrCode = qrCode.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // ✅ Encode the QR code for URL
        guard let encodedQrCode = cleanQrCode.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            print("❌ Failed to encode QR code")
            completion(.failure(.badURL))
            return
        }
        
        let endpoint = "/users/\(cleanUserId)/bookings/\(cleanBookingId)/checkin?qrCode=\(encodedQrCode)"
        
        print("🔍 Checking in:")
        print("   User ID: \(cleanUserId)")
        print("   Booking ID: \(cleanBookingId)")
        print("   QR Code: \(cleanQrCode)")
        print("   Endpoint: \(endpoint)")
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Failed to create check-in URL")
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        
        print("🚀 Sending check-in request to: \(url.absoluteString)")
        
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
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Check-in authentication failed")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                if httpResponse.statusCode == 400 {
                    print("❌ Check-in bad request (400)")
                    if let data = data, let errorString = String(data: data, encoding: .utf8) {
                        print("📋 Error response: \(errorString)")
                    }
                    completion(.failure(.serverError(400)))
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Check-in server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No check-in data received")
                    completion(.failure(.noData))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw check-in response:")
                    print(jsonString)
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Check-in successful!")
                    print("   Booking ID: \(booking.id)")
                    print("   Status: \(booking.status)")
                    completion(.success(booking))
                } catch {
                    print("❌ Check-in decoding error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }


    func checkOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let queryItems = [URLQueryItem(name: "qrCode", value: qrCode)]
        makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)/checkout", method: "POST", queryItems: queryItems, completion: completion)
    }

    // ✅ NEW: Validate QR Check-in
    // ✅ FIXED: Validate QR Check-in (matches your backend endpoint)
    // ✅ CORRECT: Validate QR Check-in with userId in path
    // ✅ COMPLETE: Validate QR Check-in
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
//        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("application/json", forHTTPHeaderField: "Accept")
//        
//        let body: [String: String] = ["qrCode": qrCode]
//        request.httpBody = try? JSONEncoder().encode(body)
//        
//        print("🚀 Validating QR check-in:")
//        print("   URL: \(url.absoluteString)")
//        print("   Booking ID: \(bookingId)")
//        print("   QR Code: \(qrCode)")
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
//                print("📡 Response status: \(httpResponse.statusCode)")
//                
//                guard (200...299).contains(httpResponse.statusCode) else {
//                    if let data = data, let errorMsg = String(data: data, encoding: .utf8) {
//                        print("📋 Error response: \(errorMsg)")
//                    }
//                    completion(.failure(.serverError(httpResponse.statusCode)))
//                    return
//                }
//                
//                // ✅ Check-in succeeded! Now fetch the updated booking from the list endpoint
//                print("✅ Check-in request successful")
//                print("🔄 Fetching updated booking data...")
//                
//                // ✅ Fetch from the CORRECT endpoint
//                self.makeRequest(endpoint: "/users/\(userId)/bookings/\(bookingId)", method: "GET", completion: completion)
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
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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
                
                // ✅ Decode validation result
                do {
                    let validationResult = try JSONDecoder().decode(QrValidationResult.self, from: data)
                    print("✅ Validation Result:")
                    print("   Valid: \(validationResult.valid)")
                    print("   Message: \(validationResult.message ?? "nil")")
                    if let penalty = validationResult.penalty {
                        print("   Penalty: ₹\(penalty)")
                    }
                    
                    if validationResult.valid {
                        // ✅ Step 2: Call check-in API
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
    // ✅ Validate QR Check-out
    func validateQRCheckOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/validate-qr-checkout"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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

    // ✅ Perform actual check-out after validation
    private func performCheckOut(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/checkout"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid checkout URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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


    // ✅ NEW: Perform actual check-in after validation
    private func performCheckIn(userId: String, bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let endpoint = "/users/\(userId)/bookings/\(bookingId)/checkin"
        
        guard let url = URL(string: APIService.backendBaseURL + endpoint) else {
            print("❌ Invalid check-in URL")
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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
                
                // ✅ Print raw response
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Raw check-in response:")
                    print(jsonString)
                }
                
                // ✅ Decode booking response
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

    // ✅ NEW: Validate QR Check-out
    func validateQRCheckOut(bookingId: String, qrCode: String, completion: @escaping (Result<Bookings, APIError>) -> Void) {
        let urlString = "\(APIService.backendBaseURL)/api/bookings/\(bookingId)/validate-qr-checkout"
        
        guard let url = URL(string: urlString) else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: String] = ["qrCode": qrCode]
        request.httpBody = try? JSONEncoder().encode(body)
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(.networkError(error)))
                return
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                completion(.failure(.badResponse))
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
                let booking = try JSONDecoder().decode(Bookings.self, from: data)
                completion(.success(booking))
            } catch {
                completion(.failure(.decodingError(error)))
            }
        }.resume()
    }


    func getPenaltyInfo(userId: String, bookingId: String, completion: @escaping (Result<Double, APIError>) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/users/\(userId)/bookings/\(bookingId)/penalty") else {
            completion(.failure(.badURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
        
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
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
        
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
    func fetchParkingConfig(completion: @escaping (Result<ParkingConfig, APIError>) -> Void) {
        makeRequest(endpoint: "/config/parking-rates", completion: completion)
    }


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

    func fetchUserVehicles(userId: String, completion: @escaping (Result<[String], APIError>) -> Void) {
        print("📡 Fetching vehicles for user: \(userId)")
        
        makeRequest(endpoint: "/users/\(userId)", completion: { (result: Result<Users, APIError>) in
            switch result {
            case .success(let user):
                let vehicles = user.safeVehicleNumbers
                if vehicles.isEmpty {
                    let defaultVehicle = "USER_\(String(userId.prefix(6)))"
                    print("⚠️ No vehicles found, using default: \(defaultVehicle)")
                    completion(.success([defaultVehicle]))
                } else {
                    print("✅ Found \(vehicles.count) vehicles: \(vehicles)")
                    completion(.success(vehicles))
                }
            case .failure(let error):
                print("❌ Failed to fetch user vehicles: \(error)")
                completion(.failure(error))
            }
        })
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
        request.setValue(createBasicAuthHeader(), forHTTPHeaderField: "Authorization")
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
    // ✅ UPDATED: Match backend API expecting newCheckOutTime
    func extendBooking(
        userId: String,
        bookingId: String,
        newCheckOutTime: String, // ✅ ISO8601 string
        completion: @escaping (Result<Bookings, Error>) -> Void
    ) {
        let urlString = "\(APIService.backendBaseURL)/users/\(userId)/bookings/\(bookingId)/extend"
        
        guard let url = URL(string: urlString) else {
            completion(.failure(APIError.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"  // ✅ Your backend uses PUT
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "newCheckOutTime": newCheckOutTime  // ✅ Match backend parameter
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


}
// ✅ NEW: Extend Booking Time
