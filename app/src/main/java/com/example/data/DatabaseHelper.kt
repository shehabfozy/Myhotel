package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Core Data Models
data class Floor(val id: Long, val name: String)

data class Room(
    val id: Long,
    val floorId: Long,
    val roomNumber: String,
    val roomType: String,
    val price: Double,
    val status: String // "AVAILABLE", "RESERVED"
)

data class GuestCompanion(
    val name: String,
    val idNumber: String,
    val idType: String,
    val phone: String
)

data class Booking(
    val id: Long,
    val roomId: Long,
    val roomNumber: String,
    val roomType: String,
    val guestName: String,
    val idType: String,
    val idNumber: String,
    val phone: String,
    val companionsCount: Int,
    val companions: List<GuestCompanion>,
    val nightsCount: Int, // -1 for "rest/استراحة"
    val pricePerNight: Double,
    val amountPaid: Double,
    val amountRemaining: Double,
    val bookingDate: String, // YYYY-MM-DD
    val checkInTime: Long,
    val checkOutTime: Long,
    val status: String, // "ACTIVE", "COMPLETED"
    val idPhotoBase64: String? = null
)

data class FinancialTransaction(
    val id: Long,
    val type: String, // "INCOME", "EXPENSE"
    val category: String,
    val amount: Double,
    val description: String,
    val date: String, // YYYY-MM-DD
    val timestamp: Long,
    val bookingId: Long? = null
)

data class AppUser(
    val id: Long,
    val username: String,
    val role: String, // "ADMIN", "RECEPTIONIST"
    val fullname: String
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "hotel_management.db"
        private const val DATABASE_VERSION = 1

        // Tables
        private const val TABLE_FLOORS = "floors"
        private const val TABLE_ROOMS = "rooms"
        private const val TABLE_BOOKINGS = "bookings"
        private const val TABLE_FINANCIALS = "financials"
        private const val TABLE_USERS = "users"
        private const val TABLE_SETTINGS = "settings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create floors table
        db.execSQL("""
            CREATE TABLE $TABLE_FLOORS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL
            )
        """)

        // Create rooms table
        db.execSQL("""
            CREATE TABLE $TABLE_ROOMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                floor_id INTEGER NOT NULL,
                room_number TEXT NOT NULL,
                room_type TEXT NOT NULL,
                price REAL NOT NULL,
                status TEXT NOT NULL DEFAULT 'AVAILABLE',
                FOREIGN KEY(floor_id) REFERENCES $TABLE_FLOORS(id) ON DELETE CASCADE
            )
        """)

        // Create bookings table
        db.execSQL("""
            CREATE TABLE $TABLE_BOOKINGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                room_id INTEGER NOT NULL,
                guest_name TEXT NOT NULL,
                id_type TEXT NOT NULL,
                id_number TEXT NOT NULL,
                phone TEXT NOT NULL,
                companions_count INTEGER NOT NULL DEFAULT 0,
                companions_json TEXT, -- JSON array of companions
                nights_count INTEGER NOT NULL, -- -1 represents short rest/استراحة
                price_per_night REAL NOT NULL,
                amount_paid REAL NOT NULL,
                amount_remaining REAL NOT NULL,
                booking_date TEXT NOT NULL,
                check_in_time INTEGER NOT NULL,
                check_out_time INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                id_photo_base64 TEXT,
                FOREIGN KEY(room_id) REFERENCES $TABLE_ROOMS(id)
            )
        """)

        // Create financials table
        db.execSQL("""
            CREATE TABLE $TABLE_FINANCIALS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL, -- INCOME / EXPENSE
                category TEXT NOT NULL,
                amount REAL NOT NULL,
                description TEXT,
                date TEXT NOT NULL, -- YYYY-MM-DD
                timestamp INTEGER NOT NULL,
                booking_id INTEGER,
                FOREIGN KEY(booking_id) REFERENCES $TABLE_BOOKINGS(id) ON DELETE SET NULL
            )
        """)

        // Create users table for permissions
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL, -- ADMIN / RECEPTIONIST
                fullname TEXT NOT NULL
            )
        """)

        // Create settings table
        db.execSQL("""
            CREATE TABLE $TABLE_SETTINGS (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)

        // Seed initial data
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FINANCIALS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROOMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FLOORS")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // 1. App Users
        db.execSQL("INSERT INTO $TABLE_USERS (username, password, role, fullname) VALUES ('admin', 'admin123', 'ADMIN', 'المدير العام')")
        db.execSQL("INSERT INTO $TABLE_USERS (username, password, role, fullname) VALUES ('reception', '123', 'RECEPTIONIST', 'موظف الاستقبال')")

        // 2. Default Settings
        db.execSQL("INSERT INTO $TABLE_SETTINGS (key, value) VALUES ('hotel_name', 'فندق الجوهرة الرياض')")
        db.execSQL("INSERT INTO $TABLE_SETTINGS (key, value) VALUES ('currency', 'ر.س')")
        db.execSQL("INSERT INTO $TABLE_SETTINGS (key, value) VALUES ('checkout_hour', '12')")
        db.execSQL("INSERT INTO $TABLE_SETTINGS (key, value) VALUES ('rest_duration_hours', '3')")

        // 3. Floors
        val f1 = db.insert(TABLE_FLOORS, null, ContentValues().apply { put("name", "الطابق الأرضي") })
        val f2 = db.insert(TABLE_FLOORS, null, ContentValues().apply { put("name", "الطابق الأول") })
        val f3 = db.insert(TABLE_FLOORS, null, ContentValues().apply { put("name", "الطابق الثاني") })

        // 4. Rooms
        val roomsData = listOf(
            // Floor Ground (1)
            Triple("101", "غرفة مفردة", 150.0),
            Triple("102", "غرفة مفردة", 150.0),
            Triple("103", "غرفة مزدوجة", 250.0),
            Triple("104", "غرفة مزدوجة", 250.0),
            // Floor 1 (2)
            Triple("201", "أستوديو فاخر", 350.0),
            Triple("202", "أستوديو فاخر", 350.0),
            Triple("203", "شقة غرفتين وصالة", 500.0),
            Triple("204", "شقة غرفتين وصالة", 500.0),
            // Floor 2 (3)
            Triple("301", "جناح ملكي", 800.0),
            Triple("302", "جناح ملكي", 800.0),
            Triple("303", "شقة رئاسية ثلاث غرف", 1200.0)
        )

        for ((index, room) in roomsData.withIndex()) {
            val floorId = when {
                index < 4 -> f1
                index < 8 -> f2
                else -> f3
            }
            db.insert(TABLE_ROOMS, null, ContentValues().apply {
                put("floor_id", floorId)
                put("room_number", room.first)
                put("room_type", room.second)
                put("price", room.third)
                put("status", "AVAILABLE")
            })
        }

        // 5. Seed some past and active bookings for beautiful reports on first boot
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val yesterdayMillis = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val yesterdayStr = sdf.format(Date(yesterdayMillis))
        val threeDaysAgoMillis = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000
        val threeDaysAgoStr = sdf.format(Date(threeDaysAgoMillis))

        // Complete past booking
        val cvPast = ContentValues().apply {
            put("room_id", 1) // room 101
            put("guest_name", "فيصل بن خالد")
            put("id_type", "هوية وطنية")
            put("id_number", "1029481029")
            put("phone", "0501234567")
            put("companions_count", 0)
            put("companions_json", "[]")
            put("nights_count", 2)
            put("price_per_night", 150.0)
            put("amount_paid", 300.0)
            put("amount_remaining", 0.0)
            put("booking_date", threeDaysAgoStr)
            put("check_in_time", threeDaysAgoMillis)
            put("check_out_time", yesterdayMillis)
            put("status", "COMPLETED")
        }
        val pastBookingId = db.insert(TABLE_BOOKINGS, null, cvPast)

        // Past booking financials
        db.insert(TABLE_FINANCIALS, null, ContentValues().apply {
            put("type", "INCOME")
            put("category", "حجوزات")
            put("amount", 300.0)
            put("description", "حجز غرفة 101 - فيصل بن خالد")
            put("date", threeDaysAgoStr)
            put("timestamp", threeDaysAgoMillis)
            put("booking_id", pastBookingId)
        })

        // Seed an expense
        db.insert(TABLE_FINANCIALS, null, ContentValues().apply {
            put("type", "EXPENSE")
            put("category", "صيانة ونظافة")
            put("amount", 80.0)
            put("description", "شراء منظفات ومستلزمات نظافة دورية")
            put("date", yesterdayStr)
            put("timestamp", yesterdayMillis)
        })

        db.insert(TABLE_FINANCIALS, null, ContentValues().apply {
            put("type", "EXPENSE")
            put("category", "فواتير وضيافة")
            put("amount", 120.0)
            put("description", "فاتورة مياه الخدمة")
            put("date", todayStr)
            put("timestamp", System.currentTimeMillis())
        })
    }

    // --- FLOORS OPERATIONS ---
    fun getAllFloors(): List<Floor> {
        val list = mutableListOf<Floor>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FLOORS ORDER BY id ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Floor(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addFloor(name: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply { put("name", name) }
        return db.insert(TABLE_FLOORS, null, cv)
    }

    // --- ROOMS OPERATIONS ---
    fun getAllRooms(): List<Room> {
        val list = mutableListOf<Room>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ROOMS ORDER BY room_number ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Room(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("floor_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("room_number")),
                    cursor.getString(cursor.getColumnIndexOrThrow("room_type")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    cursor.getString(cursor.getColumnIndexOrThrow("status"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addRoom(floorId: Long, roomNumber: String, roomType: String, price: Double): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("floor_id", floorId)
            put("room_number", roomNumber)
            put("room_type", roomType)
            put("price", price)
            put("status", "AVAILABLE")
        }
        return db.insert(TABLE_ROOMS, null, cv)
    }

    fun updateRoomStatus(roomId: Long, status: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("status", status) }
        db.update(TABLE_ROOMS, cv, "id = ?", arrayOf(roomId.toString()))
    }

    // --- BOOKING OPERATIONS ---
    fun createBooking(
        roomId: Long,
        guestName: String,
        idType: String,
        idNumber: String,
        phone: String,
        companions: List<GuestCompanion>,
        nightsCount: Int,
        pricePerNight: Double,
        amountPaid: Double,
        amountRemaining: Double,
        checkInTime: Long,
        checkOutTime: Long,
        idPhotoBase64: String?
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Build companion JSON
            val compsArray = JSONArray()
            for (comp in companions) {
                val json = JSONObject().apply {
                    put("name", comp.name)
                    put("id_number", comp.idNumber)
                    put("id_type", comp.idType)
                    put("phone", comp.phone)
                }
                compsArray.put(json)
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDateStr = sdf.format(Date(checkInTime))

            // 1. Insert booking
            val cv = ContentValues().apply {
                put("room_id", roomId)
                put("guest_name", guestName)
                put("id_type", idType)
                put("id_number", idNumber)
                put("phone", phone)
                put("companions_count", companions.size)
                put("companions_json", compsArray.toString())
                put("nights_count", nightsCount)
                put("price_per_night", pricePerNight)
                put("amount_paid", amountPaid)
                put("amount_remaining", amountRemaining)
                put("booking_date", todayDateStr)
                put("check_in_time", checkInTime)
                put("check_out_time", checkOutTime)
                put("status", "ACTIVE")
                put("id_photo_base64", idPhotoBase64)
            }
            val bookingId = db.insert(TABLE_BOOKINGS, null, cv)

            // 2. Mark Room as RESERVED
            val cvRoom = ContentValues().apply { put("status", "RESERVED") }
            db.update(TABLE_ROOMS, cvRoom, "id = ?", arrayOf(roomId.toString()))

            // 3. Add to Financials (Income)
            if (amountPaid > 0) {
                // Get room number for description
                var roomNumStr = "مجهولة"
                val cursor = db.rawQuery("SELECT room_number FROM $TABLE_ROOMS WHERE id = ?", arrayOf(roomId.toString()))
                if (cursor.moveToFirst()) {
                    roomNumStr = cursor.getString(0)
                }
                cursor.close()

                val cvFin = ContentValues().apply {
                    put("type", "INCOME")
                    put("category", "حجوزات")
                    put("amount", amountPaid)
                    put("description", "حجز غرفة $roomNumStr النزيل: $guestName")
                    put("date", todayDateStr)
                    put("timestamp", checkInTime)
                    put("booking_id", bookingId)
                }
                db.insert(TABLE_FINANCIALS, null, cvFin)
            }

            db.setTransactionSuccessful()
            return bookingId
        } finally {
            db.endTransaction()
        }
    }

    // Update remaining payment for booking
    fun payRemainingAmount(bookingId: Long, amountToPay: Double, guestName: String, roomNumber: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT amount_paid, amount_remaining FROM $TABLE_BOOKINGS WHERE id = ?", arrayOf(bookingId.toString()))
            if (!cursor.moveToFirst()) {
                cursor.close()
                return false
            }
            val paidNow = cursor.getDouble(0)
            val ramainingNow = cursor.getDouble(1)
            cursor.close()

            if (amountToPay > ramainingNow) return false

            val newPaid = paidNow + amountToPay
            val newRemaining = ramainingNow - amountToPay

            val cv = ContentValues().apply {
                put("amount_paid", newPaid)
                put("amount_remaining", newRemaining)
            }
            db.update(TABLE_BOOKINGS, cv, "id = ?", arrayOf(bookingId.toString()))

            // Insert finance record
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDateStr = sdf.format(Date())
            val cvFin = ContentValues().apply {
                put("type", "INCOME")
                put("category", "حجوزات (دفعة متبقية)")
                put("amount", amountToPay)
                put("description", "دفعة متبقية - غرفة $roomNumber للنزيل: $guestName")
                put("date", todayDateStr)
                put("timestamp", System.currentTimeMillis())
                put("booking_id", bookingId)
            }
            db.insert(TABLE_FINANCIALS, null, cvFin)

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun completeAndReleaseBooking(bookingId: Long, roomId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Mark booking as COMPLETED
            val cvBooking = ContentValues().apply { put("status", "COMPLETED") }
            db.update(TABLE_BOOKINGS, cvBooking, "id = ?", arrayOf(bookingId.toString()))

            // 2. Mark room as AVAILABLE
            val cvRoom = ContentValues().apply { put("status", "AVAILABLE") }
            db.update(TABLE_ROOMS, cvRoom, "id = ?", arrayOf(roomId.toString()))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getActiveBookings(): List<Booking> {
        return getBookingsByQuery("SELECT b.*, r.room_number, r.room_type FROM $TABLE_BOOKINGS b JOIN $TABLE_ROOMS r ON b.room_id = r.id WHERE b.status = 'ACTIVE'")
    }

    fun getCompletedBookings(): List<Booking> {
        return getBookingsByQuery("SELECT b.*, r.room_number, r.room_type FROM $TABLE_BOOKINGS b JOIN $TABLE_ROOMS r ON b.room_id = r.id WHERE b.status = 'COMPLETED'")
    }

    fun getAllBookings(): List<Booking> {
        return getBookingsByQuery("SELECT b.*, r.room_number, r.room_type FROM $TABLE_BOOKINGS b JOIN $TABLE_ROOMS r ON b.room_id = r.id ORDER BY b.check_in_time DESC")
    }

    fun getActiveBookingForRoom(roomId: Long): Booking? {
        val list = getBookingsByQuery("SELECT b.*, r.room_number, r.room_type FROM $TABLE_BOOKINGS b JOIN $TABLE_ROOMS r ON b.room_id = r.id WHERE b.room_id = $roomId AND b.status = 'ACTIVE'")
        return if (list.isNotEmpty()) list[0] else null
    }

    private fun getBookingsByQuery(sql: String): List<Booking> {
        val list = mutableListOf<Booking>()
        val db = readableDatabase
        val cursor = db.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do {
                // Parse companions
                val comps = mutableListOf<GuestCompanion>()
                val jsonStr = cursor.getString(cursor.getColumnIndexOrThrow("companions_json"))
                if (!jsonStr.isNullOrEmpty()) {
                    try {
                        val arr = JSONArray(jsonStr)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            comps.add(GuestCompanion(
                                obj.optString("name", ""),
                                obj.optString("id_number", ""),
                                obj.optString("id_type", ""),
                                obj.optString("phone", "")
                            ))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                list.add(Booking(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("room_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("room_number")),
                    cursor.getString(cursor.getColumnIndexOrThrow("room_type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("guest_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("id_type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("id_number")),
                    cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("companions_count")),
                    comps,
                    cursor.getInt(cursor.getColumnIndexOrThrow("nights_count")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("price_per_night")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("amount_paid")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("amount_remaining")),
                    cursor.getString(cursor.getColumnIndexOrThrow("booking_date")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("check_in_time")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("check_out_time")),
                    cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    cursor.getString(cursor.getColumnIndexOrThrow("id_photo_base64"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- FINANCIAL OPERATIONS ---
    fun addFinancialTransaction(type: String, category: String, amount: Double, description: String, timestamp: Long): Long {
        val db = writableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(Date(timestamp))
        val cv = ContentValues().apply {
            put("type", type)
            put("category", category)
            put("amount", amount)
            put("description", description)
            put("date", dateStr)
            put("timestamp", timestamp)
        }
        return db.insert(TABLE_FINANCIALS, null, cv)
    }

    fun getAllFinancialTransactions(): List<FinancialTransaction> {
        val list = mutableListOf<FinancialTransaction>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FINANCIALS ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(FinancialTransaction(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    if (cursor.isNull(cursor.getColumnIndexOrThrow("booking_id"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("booking_id"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteFinancialTransaction(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_FINANCIALS, "id = ?", arrayOf(id.toString()))
    }

    // --- USERS MANAGEMENT ---
    fun login(username: String, password: String): AppUser? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ? AND password = ?", arrayOf(username, password))
        var user: AppUser? = null
        if (cursor.moveToFirst()) {
            user = AppUser(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getString(cursor.getColumnIndexOrThrow("role")),
                cursor.getString(cursor.getColumnIndexOrThrow("fullname"))
            )
        }
        cursor.close()
        return user
    }

    fun addUser(username: String, password: String, role: String, fullname: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("username", username)
            put("password", password)
            put("role", role)
            put("fullname", fullname)
        }
        return db.insert(TABLE_USERS, null, cv)
    }

    fun getAllUsers(): List<AppUser> {
        val list = mutableListOf<AppUser>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, username, role, fullname FROM $TABLE_USERS", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(AppUser(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    cursor.getString(cursor.getColumnIndexOrThrow("fullname"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteUser(id: Long): Boolean {
        // Prevent deleting admin id = 1
        if (id == 1L) return false
        val db = writableDatabase
        return db.delete(TABLE_USERS, "id = ?", arrayOf(id.toString())) > 0
    }

    // --- SETTINGS OPERATIONS ---
    fun getSetting(key: String, defaultValue: String): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT value FROM $TABLE_SETTINGS WHERE key = ?", arrayOf(key))
        var value = defaultValue
        if (cursor.moveToFirst()) {
            value = cursor.getString(0)
        }
        cursor.close()
        return value
    }

    fun saveSetting(key: String, value: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
