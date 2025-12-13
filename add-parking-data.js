// MongoDB script to add sample parking data for Gridee app
// Run with: mongosh parkingdb add-parking-data.js

// Clear existing data (optional - comment out if you want to keep existing data)
db.parking_lots.deleteMany({});
db.parking_spots.deleteMany({});

print("Adding parking lots...");

// Add Parking Lots
const parkingLots = [
    {
        id: "LOT001",
        name: "City Center Mall",
        address: "123 Main Street, Downtown",
        latitude: 28.6139,
        longitude: 77.2090,
        totalSpots: 50,
        availableSpots: 45,
        pricePerHour: 50.0,
        operatingHours: "24/7",
        amenities: ["CCTV", "Security Guard", "EV Charging", "Covered Parking"],
        imageUrl: "https://example.com/city-center.jpg",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        id: "LOT002",
        name: "Airport Parking",
        address: "Terminal 3, IGI Airport",
        latitude: 28.5562,
        longitude: 77.1000,
        totalSpots: 100,
        availableSpots: 85,
        pricePerHour: 80.0,
        operatingHours: "24/7",
        amenities: ["CCTV", "Security Guard", "24/7 Access", "Shuttle Service"],
        imageUrl: "https://example.com/airport.jpg",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        id: "LOT003",
        name: "Metro Station Parking",
        address: "Rajiv Chowk Metro Station",
        latitude: 28.6328,
        longitude: 77.2197,
        totalSpots: 30,
        availableSpots: 20,
        pricePerHour: 30.0,
        operatingHours: "6:00 AM - 11:00 PM",
        amenities: ["CCTV", "Metro Access", "Covered Parking"],
        imageUrl: "https://example.com/metro.jpg",
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

const lotResult = db.parking_lots.insertMany(parkingLots);
print(`✅ Inserted ${lotResult.insertedIds.length} parking lots`);

print("\nAdding parking spots...");

// Add Parking Spots for each lot
const parkingSpots = [];

// City Center Mall - 50 spots (A1-A10, B1-B10, C1-C10, D1-D10, E1-E10)
for (let floor = 0; floor < 5; floor++) {
    const floorLetter = String.fromCharCode(65 + floor); // A, B, C, D, E
    for (let num = 1; num <= 10; num++) {
        parkingSpots.push({
            spotId: `${floorLetter}${num}`,
            lotId: "LOT001",
            lotName: "City Center Mall",
            floor: floor + 1,
            section: floorLetter,
            available: num <= 9, // First 9 spots available, last one occupied
            type: num <= 2 ? "disabled" : (num <= 4 ? "ev" : "regular"),
            pricePerHour: 50.0,
            createdAt: new Date(),
            updatedAt: new Date()
        });
    }
}

// Airport Parking - 100 spots (P1-001 to P1-050, P2-001 to P2-050)
for (let floor = 1; floor <= 2; floor++) {
    for (let num = 1; num <= 50; num++) {
        const spotNum = num.toString().padStart(3, '0');
        parkingSpots.push({
            spotId: `P${floor}-${spotNum}`,
            lotId: "LOT002",
            lotName: "Airport Parking",
            floor: floor,
            section: `P${floor}`,
            available: num <= 42 || floor === 2, // Some occupied on floor 1
            type: num % 10 === 0 ? "ev" : "regular",
            pricePerHour: 80.0,
            createdAt: new Date(),
            updatedAt: new Date()
        });
    }
}

// Metro Station Parking - 30 spots (M1-M30)
for (let num = 1; num <= 30; num++) {
    parkingSpots.push({
        spotId: `M${num}`,
        lotId: "LOT003",
        lotName: "Metro Station Parking",
        floor: 1,
        section: "Ground",
        available: num <= 20, // 20 available, 10 occupied
        type: num <= 3 ? "bike" : "regular",
        pricePerHour: 30.0,
        createdAt: new Date(),
        updatedAt: new Date()
    });
}

const spotResult = db.parking_spots.insertMany(parkingSpots);
print(`✅ Inserted ${spotResult.insertedIds.length} parking spots`);

// Print summary
print("\n" + "=".repeat(50));
print("📊 Database Summary:");
print("=".repeat(50));
print(`Parking Lots: ${db.parking_lots.countDocuments()}`);
print(`Parking Spots: ${db.parking_spots.countDocuments()}`);
print(`Available Spots: ${db.parking_spots.countDocuments({ available: true })}`);
print(`Occupied Spots: ${db.parking_spots.countDocuments({ available: false })}`);
print("");

// Show breakdown by lot
print("Breakdown by Parking Lot:");
parkingLots.forEach(lot => {
    const total = db.parking_spots.countDocuments({ lotId: lot.id });
    const available = db.parking_spots.countDocuments({ lotId: lot.id, available: true });
    print(`  • ${lot.name}: ${available}/${total} available - ₹${lot.pricePerHour}/hr`);
});

print("\n✅ Sample parking data added successfully!");
print("🚗 You can now test the Gridee app with this data.");
