// Verify parking data in database
print("=== PARKING DATABASE VERIFICATION ===\n");

print("📊 Collections:");
print("  Parking Lots: " + db.parking_lots.countDocuments());
print("  Parking Spots: " + db.parking_spots.countDocuments());
print("");

print("🅿️  PARKING LOTS:");
print("─".repeat(60));
db.parking_lots.find({}).forEach(lot => {
    print(`  ${lot.name}`);
    print(`    📍 ${lot.address}`);
    print(`    🚗 ${lot.availableSpots}/${lot.totalSpots} spots available`);
    print(`    💰 ₹${lot.pricePerHour}/hour`);
    print(`    ⏰ ${lot.operatingHours}`);
    print("");
});

print("🚗 PARKING SPOTS BY LOT:");
print("─".repeat(60));
const lots = db.parking_lots.find({}).toArray();
lots.forEach(lot => {
    const total = db.parking_spots.countDocuments({ lotId: lot.id });
    const available = db.parking_spots.countDocuments({ lotId: lot.id, available: true });
    const occupied = total - available;
    print(`  ${lot.name} (${lot.id}):`);
    print(`    Total: ${total} spots`);
    print(`    Available: ${available} spots`);
    print(`    Occupied: ${occupied} spots`);

    // Show sample spots
    const samples = db.parking_spots.find({ lotId: lot.id }).limit(5).toArray();
    print(`    Sample spots: ${samples.map(s => s.spotId).join(', ')}...`);
    print("");
});

print("✅ Database verification complete!");
print("\n💡 Tip: Use Google Sign-In in the app to view these parking lots.");
