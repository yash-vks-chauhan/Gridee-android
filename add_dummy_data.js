// MongoDB script to add dummy data for parking locations, spots, and bookings
// Run this script using: mongosh parkingdb add_dummy_data.js

// Use the parkingdb database
use parkingdb;

// Clear existing data (optional - comment out if you want to keep existing data)
// db.parking_lots.deleteMany({});
// db.parking_spots.deleteMany({});
// db.bookings.deleteMany({});

print("Adding dummy parking lots...");

// Add parking lots
const lot1 = db.parking_lots.insertOne({
    name: "TP Avenue Parking",
    location: "T.P. Scheme, Bhopal",
    address: "Near MP Nagar, Bhopal, Madhya Pradesh 462001",
    totalSpots: 100,
    availableSpots: 85,
    latitude: 23.2599,
    longitude: 77.4126
});

const lot2 = db.parking_lots.insertOne({
    name: "DB City Mall Parking",
    location: "DB City Mall, Bhopal",
    address: "Zone-1, Maharana Pratap Nagar, Bhopal, Madhya Pradesh 462011",
    totalSpots: 200,
    availableSpots: 150,
    latitude: 23.2156,
    longitude: 77.4304
});

const lot3 = db.parking_lots.insertOne({
    name: "New Market Parking",
    location: "New Market, Bhopal",
    address: "New Market, Bhopal, Madhya Pradesh 462001",
    totalSpots: 50,
    availableSpots: 30,
    latitude: 23.2640,
    longitude: 77.4068
});

print("Parking lots added successfully!");
print("Lot 1 ID: " + lot1.insertedId);
print("Lot 2 ID: " + lot2.insertedId);
print("Lot 3 ID: " + lot3.insertedId);

print("\nAdding dummy parking spots...");

// Add parking spots for lot 1
const spot1 = db.parking_spots.insertOne({
    lotId: lot1.insertedId.toString(),
    zoneName: "TP Avenue - Zone A",
    capacity: 50,
    available: 45,
    status: "available",
    bookingRate: 20.0,
    checkInPenaltyRate: 5.0,
    checkOutPenaltyRate: 10.0,
    description: "Ground floor parking, near entrance"
});

const spot2 = db.parking_spots.insertOne({
    lotId: lot1.insertedId.toString(),
    zoneName: "TP Avenue - Zone B",
    capacity: 50,
    available: 40,
    status: "available",
    bookingRate: 15.0,
    checkInPenaltyRate: 5.0,
    checkOutPenaltyRate: 10.0,
    description: "First floor parking, covered area"
});

// Add parking spots for lot 2
const spot3 = db.parking_spots.insertOne({
    lotId: lot2.insertedId.toString(),
    zoneName: "DB City Mall - Basement 1",
    capacity: 100,
    available: 80,
    status: "available",
    bookingRate: 25.0,
    checkInPenaltyRate: 5.0,
    checkOutPenaltyRate: 10.0,
    description: "Basement parking with lift access"
});

const spot4 = db.parking_spots.insertOne({
    lotId: lot2.insertedId.toString(),
    zoneName: "DB City Mall - Basement 2",
    capacity: 100,
    available: 70,
    status: "available",
    bookingRate: 20.0,
    checkInPenaltyRate: 5.0,
    checkOutPenaltyRate: 10.0,
    description: "Lower basement parking"
});

// Add parking spots for lot 3
const spot5 = db.parking_spots.insertOne({
    lotId: lot3.insertedId.toString(),
    zoneName: "New Market - Main Area",
    capacity: 50,
    available: 30,
    status: "available",
    bookingRate: 30.0,
    checkInPenaltyRate: 5.0,
    checkOutPenaltyRate: 10.0,
    description: "Open air parking in city center"
});

print("Parking spots added successfully!");
print("Spot 1 ID: " + spot1.insertedId);
print("Spot 2 ID: " + spot2.insertedId);
print("Spot 3 ID: " + spot3.insertedId);
print("Spot 4 ID: " + spot4.insertedId);
print("Spot 5 ID: " + spot5.insertedId);

// Get a user ID (assuming you have at least one user in the database)
print("\nFetching user ID...");
const user = db.users.findOne({});
const userId = user ? user._id.toString() : "default_user_id";
print("Using user ID: " + userId);

print("\nAdding dummy bookings...");

// Helper function to create dates
function createDate(daysOffset, hoursOffset = 0) {
    const date = new Date();
    date.setDate(date.getDate() + daysOffset);
    date.setHours(date.getHours() + hoursOffset);
    return date;
}

// Add 2 PENDING bookings
const pendingBooking1 = db.bookings.insertOne({
    userId: userId,
    lotId: lot1.insertedId.toString(),
    spotId: spot1.insertedId.toString(),
    status: "pending",
    amount: 60.0,
    qrCode: "QR-PENDING-001",
    checkInTime: createDate(1, 2), // Tomorrow, 2 hours from now
    checkOutTime: createDate(1, 5), // Tomorrow, 5 hours from now
    createdAt: createDate(0),
    vehicleNumber: "MP09AB1234",
    qrCodeScanned: false,
    autoCompleted: false
});

const pendingBooking2 = db.bookings.insertOne({
    userId: userId,
    lotId: lot2.insertedId.toString(),
    spotId: spot3.insertedId.toString(),
    status: "pending",
    amount: 75.0,
    qrCode: "QR-PENDING-002",
    checkInTime: createDate(2, 10), // Day after tomorrow, 10 AM
    checkOutTime: createDate(2, 14), // Day after tomorrow, 2 PM
    createdAt: createDate(0),
    vehicleNumber: "MP09XY5678",
    qrCodeScanned: false,
    autoCompleted: false
});

// Add 2 COMPLETED bookings
const completedBooking1 = db.bookings.insertOne({
    userId: userId,
    lotId: lot1.insertedId.toString(),
    spotId: spot2.insertedId.toString(),
    status: "completed",
    amount: 45.0,
    qrCode: "QR-COMPLETED-001",
    checkInTime: createDate(-2, 9), // 2 days ago, 9 AM
    checkOutTime: createDate(-2, 12), // 2 days ago, 12 PM
    createdAt: createDate(-2),
    vehicleNumber: "MP09CD9012",
    qrCodeScanned: true,
    actualCheckInTime: createDate(-2, 9),
    autoCompleted: false
});

const completedBooking2 = db.bookings.insertOne({
    userId: userId,
    lotId: lot3.insertedId.toString(),
    spotId: spot5.insertedId.toString(),
    status: "completed",
    amount: 90.0,
    qrCode: "QR-COMPLETED-002",
    checkInTime: createDate(-5, 14), // 5 days ago, 2 PM
    checkOutTime: createDate(-5, 17), // 5 days ago, 5 PM
    createdAt: createDate(-5),
    vehicleNumber: "MP09EF3456",
    qrCodeScanned: true,
    actualCheckInTime: createDate(-5, 14),
    autoCompleted: true
});

// Add 2 CANCELLED bookings
const cancelledBooking1 = db.bookings.insertOne({
    userId: userId,
    lotId: lot2.insertedId.toString(),
    spotId: spot4.insertedId.toString(),
    status: "cancelled",
    amount: 50.0,
    qrCode: "QR-CANCELLED-001",
    checkInTime: createDate(-1, 8), // Yesterday, 8 AM
    checkOutTime: createDate(-1, 11), // Yesterday, 11 AM
    createdAt: createDate(-2),
    vehicleNumber: "MP09GH7890",
    qrCodeScanned: false,
    autoCompleted: false
});

const cancelledBooking2 = db.bookings.insertOne({
    userId: userId,
    lotId: lot1.insertedId.toString(),
    spotId: spot1.insertedId.toString(),
    status: "cancelled",
    amount: 40.0,
    qrCode: "QR-CANCELLED-002",
    checkInTime: createDate(-3, 15), // 3 days ago, 3 PM
    checkOutTime: createDate(-3, 18), // 3 days ago, 6 PM
    createdAt: createDate(-4),
    vehicleNumber: "MP09IJ2345",
    qrCodeScanned: false,
    autoCompleted: false
});

// Add 2 ACTIVE bookings (currently in use)
const activeBooking1 = db.bookings.insertOne({
    userId: userId,
    lotId: lot2.insertedId.toString(),
    spotId: spot3.insertedId.toString(),
    status: "active",
    amount: 100.0,
    qrCode: "QR-ACTIVE-001",
    checkInTime: createDate(0, -2), // Today, 2 hours ago
    checkOutTime: createDate(0, 2), // Today, 2 hours from now
    createdAt: createDate(0, -3),
    vehicleNumber: "MP09KL6789",
    qrCodeScanned: true,
    actualCheckInTime: createDate(0, -2),
    autoCompleted: false
});

const activeBooking2 = db.bookings.insertOne({
    userId: userId,
    lotId: lot3.insertedId.toString(),
    spotId: spot5.insertedId.toString(),
    status: "active",
    amount: 60.0,
    qrCode: "QR-ACTIVE-002",
    checkInTime: createDate(0, -1), // Today, 1 hour ago
    checkOutTime: createDate(0, 1), // Today, 1 hour from now
    createdAt: createDate(0, -2),
    vehicleNumber: "MP09MN0123",
    qrCodeScanned: true,
    actualCheckInTime: createDate(0, -1),
    autoCompleted: false
});

print("\nBookings added successfully!");
print("Pending bookings: 2");
print("Completed bookings: 2");
print("Cancelled bookings: 2");
print("Active bookings: 2");

print("\n=== Summary ===");
print("Total parking lots: " + db.parking_lots.countDocuments());
print("Total parking spots: " + db.parking_spots.countDocuments());
print("Total bookings: " + db.bookings.countDocuments());

print("\n=== Booking Status Breakdown ===");
print("Pending: " + db.bookings.countDocuments({status: "pending"}));
print("Active: " + db.bookings.countDocuments({status: "active"}));
print("Completed: " + db.bookings.countDocuments({status: "completed"}));
print("Cancelled: " + db.bookings.countDocuments({status: "cancelled"}));

print("\nDummy data added successfully!");
print("\nTo view the data, run:");
print("  db.parking_lots.find().pretty()");
print("  db.parking_spots.find().pretty()");
print("  db.bookings.find().pretty()");
