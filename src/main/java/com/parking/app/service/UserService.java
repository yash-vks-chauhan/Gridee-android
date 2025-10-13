package com.parking.app.service;
import com.parking.app.config.JwtUtil;
import com.parking.app.model.Users;
import com.parking.app.model.ParkingLot;
import com.parking.app.repository.ParkingLotRepository;
import com.parking.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ParkingLotRepository parkingLotRepository;
    @Autowired
    private JwtUtil jwtUtil;
    // Email Regex (simple, you may replace with a more robust pattern)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@[\\w-\\.]+\\.[a-z]{2,}$", Pattern.CASE_INSENSITIVE);

    // Phone Regex (for example, 10-15 digits)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

    public String authenticateAndGetToken(String emailOrPhone, String plainPassword) {
        Users user = authenticate(emailOrPhone, plainPassword);
        if (user != null) {
            // Use user ID and role for token, as in AuthController
            return jwtUtil.generateToken(user.getId(), user.getRole().name());
        }
        return null;
    }

    // Create User with strong validation
    public Users createUser(Users user, String parkingLotName) {
        if (user == null) throw new IllegalArgumentException("User data is required.");
        if (user.getRole() == null) {
            user.setRole(Users.Role.USER);
        } else {
            user.setRole(user.getRole());
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) throw new IllegalArgumentException("Name is required.");

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) throw new IllegalArgumentException("Email is required.");
        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) throw new IllegalArgumentException("Invalid email format.");

        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) throw new IllegalArgumentException("Phone is required.");
        if (!PHONE_PATTERN.matcher(user.getPhone()).matches()) throw new IllegalArgumentException("Invalid phone format.");
        if (parkingLotName == null || parkingLotName.trim().isEmpty()) {
            throw new IllegalArgumentException("Parking lot name is required.");
        }

        // Fetch parking lot by name
        ParkingLot lot = parkingLotRepository.findByName(parkingLotName);
        if (lot == null) {
            throw new IllegalArgumentException("Parking lot not found");
        }

        user.setParkingLotId(lot.getId());
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) throw new IllegalArgumentException("Password is required.");

        // Check duplicates
        if (userRepository.findByEmail(user.getEmail()).isPresent()) throw new IllegalArgumentException("Email already registered.");
        if (userRepository.findByPhone(user.getPhone()).isPresent()) throw new IllegalArgumentException("Phone already registered.");

        // Hash password (input is plain password in passwordHash field)
        String hashedPassword = BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);

        // Initialize fields
        user.setWalletCoins(0);
        user.setFirstUser(true);
        user.setCreatedAt(new java.util.Date());

        return userRepository.save(user);
    }


    // Authenticate user by email or phone and plain password
    public Users authenticate(String emailOrPhone, String plainPassword) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }
        System.out.println(plainPassword);
        Users user = userRepository.findByEmail(emailOrPhone).orElse(null);
        if (user == null) user = userRepository.findByPhone(emailOrPhone).orElse(null);
        if (user != null && BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    public Users getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }
    public java.util.Optional<Users> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Users updateUser(String id, Users userDetails) {
        Users existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null) return null;

        // Update only non-null/non-empty fields
        if (userDetails.getName() != null && !userDetails.getName().trim().isEmpty()) {
            existingUser.setName(userDetails.getName());
        }

        if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(userDetails.getEmail()).matches()) {
                throw new IllegalArgumentException("Invalid email format.");
            }
            // Check email uniqueness if changed
            if (!userDetails.getEmail().equals(existingUser.getEmail()) &&
                    userRepository.findByEmail(userDetails.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email already registered.");
            }
            existingUser.setEmail(userDetails.getEmail());
        }

        if (userDetails.getPhone() != null && !userDetails.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(userDetails.getPhone()).matches()) {
                throw new IllegalArgumentException("Invalid phone format.");
            }
            // Check phone uniqueness if changed
            if (!userDetails.getPhone().equals(existingUser.getPhone()) &&
                    userRepository.findByPhone(userDetails.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone already registered.");
            }
            existingUser.setPhone(userDetails.getPhone());
        }


        if (userDetails.getPasswordHash() != null && !userDetails.getPasswordHash().isEmpty()) {
            String hashedPassword = BCrypt.hashpw(userDetails.getPasswordHash(), BCrypt.gensalt());
            existingUser.setPasswordHash(hashedPassword);
        }

        return userRepository.save(existingUser);
    }
    public Users addUserVehicles(String userId, List<String> vehicleNumbers) {
        Users existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) return null;
        if (vehicleNumbers == null || vehicleNumbers.isEmpty()) {
            throw new IllegalArgumentException("Vehicle numbers list cannot be empty.");
        }
        List<String> currentVehicles = existingUser.getVehicleNumbers();
        if (currentVehicles == null) {
            currentVehicles = new java.util.ArrayList<>();
        }
        for (String v : vehicleNumbers) {
            if (v != null && !v.trim().isEmpty() && !currentVehicles.contains(v.trim())) {
                currentVehicles.add(v.trim());
            }
        }
        existingUser.setVehicleNumbers(currentVehicles);
        return userRepository.save(existingUser);
    }
// In UserService.java



    // Returns the parking lot id for a given name, or throws if not found
//    public String getParkingLotIdByName(String name) {
//        return parkingLotRepository.findByName(name)
//                .orElseThrow(() -> new IllegalArgumentException("))
//                .getId();
//    }





    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}