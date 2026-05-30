// Type Declarations
export interface Floor {
  id: number;
  name: string;
}

export interface Room {
  id: number;
  floorId: number;
  roomNumber: string;
  roomType: string;
  price: number;
  status: "AVAILABLE" | "RESERVED" | "CLEANING"; // Matches Kotlin status types
}

export interface GuestCompanion {
  name: string;
  idNumber: string;
  idType: string;
  phone: string;
}

export interface Booking {
  id: number;
  roomId: number;
  roomNumber: string;
  roomType: string;
  guestName: string;
  idType: string;
  idNumber: string;
  phone: string;
  companionsCount: number;
  companions: GuestCompanion[];
  nightsCount: number; // -1 represents short rest/استراحة
  pricePerNight: number;
  amountPaid: number;
  amountRemaining: number;
  bookingDate: string; // YYYY-MM-DD
  checkInTime: number; // Timestamp MS
  checkOutTime: number; // Timestamp MS
  status: "ACTIVE" | "COMPLETED"; // Matches Kotlin status types
  idPhotoBase64?: string;
  nationality?: string;
}

export interface FinancialTransaction {
  id: number;
  type: "INCOME" | "EXPENSE";
  category: string;
  amount: number;
  description: string;
  date: string; // YYYY-MM-DD
  timestamp: number;
  bookingId?: number | null;
}

export interface AppUser {
  id: number;
  username: string;
  role: "ADMIN" | "RECEPTIONIST";
  fullname: string;
}

export interface Settings {
  hotel_name: string;
  currency: string;
  checkout_hour: string;
  rest_duration_hours: string;
}

// Initial Seeds
const DEFAULT_USERS: AppUser[] = [
  { id: 1, username: "admin", role: "ADMIN", fullname: "المدير العام (أحمد)" },
  { id: 2, username: "reception", role: "RECEPTIONIST", fullname: "موظف الاستقبال" }
];

const DEFAULT_SETTINGS: Settings = {
  hotel_name: "فندق واحة اليمامة",
  currency: "ر.س",
  checkout_hour: "12",
  rest_duration_hours: "3"
};

const DEFAULT_FLOORS: Floor[] = [
  { id: 1, name: "الطابق الأرضي" },
  { id: 2, name: "الطابق الأول" },
  { id: 3, name: "الطابق الثاني" }
];

const DEFAULT_ROOMS: Room[] = [
  // Ground (Floor 1)
  { id: 1, floorId: 1, roomNumber: "101", roomType: "غرفة مفردة", price: 150, status: "AVAILABLE" },
  { id: 2, floorId: 1, roomNumber: "102", roomType: "غرفة مفردة", price: 150, status: "AVAILABLE" },
  { id: 3, floorId: 1, roomNumber: "103", roomType: "غرفة مزدوجة", price: 250, status: "AVAILABLE" },
  { id: 4, floorId: 1, roomNumber: "104", roomType: "غرفة مزدوجة", price: 250, status: "AVAILABLE" },
  // First Floor (Floor 2)
  { id: 5, floorId: 2, roomNumber: "201", roomType: "أستوديو فاخر", price: 350, status: "AVAILABLE" },
  { id: 6, floorId: 2, roomNumber: "202", roomType: "أستوديو فاخر", price: 350, status: "AVAILABLE" },
  { id: 7, floorId: 2, roomNumber: "203", roomType: "شقة غرفتين وصالة", price: 500, status: "AVAILABLE" },
  { id: 8, floorId: 2, roomNumber: "204", roomType: "شقة غرفتين وصالة", price: 500, status: "AVAILABLE" },
  // Second Floor (Floor 3)
  { id: 9, floorId: 3, roomNumber: "301", roomType: "جناح ملكي", price: 800, status: "AVAILABLE" },
  { id: 10, floorId: 3, roomNumber: "302", roomType: "جناح ملكي", price: 800, status: "RESERVED" }, // Seeded as reserved
  { id: 11, floorId: 3, roomNumber: "303", roomType: "شقة رئاسية ثلاث غرف", price: 1200, status: "AVAILABLE" }
];

// Helper to format Date as YYYY-MM-DD
export function getTodayDateString(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const todayStr = getTodayDateString();

// Seeded Bookings
const DEFAULT_BOOKINGS: Booking[] = [
  {
    id: 1,
    roomId: 10,
    roomNumber: "302",
    roomType: "جناح ملكي",
    guestName: "ياسر القحطاني",
    idType: "هوية وطنية",
    idNumber: "1028374655",
    phone: "0501234567",
    companionsCount: 1,
    companions: [{ name: "فهد القحطاني", idNumber: "1028374656", idType: "هوية وطنية", phone: "0507654321" }],
    nightsCount: 3,
    pricePerNight: 800,
    amountPaid: 2400,
    amountRemaining: 0,
    bookingDate: todayStr,
    checkInTime: Date.now() - 24 * 60 * 60 * 1000, // Checked in yesterday
    checkOutTime: Date.now() + 2 * 24 * 60 * 60 * 1000, // Due in 2 days
    status: "ACTIVE"
  }
];

// Seeded transactions matching default data
const DEFAULT_TRANSACTIONS: FinancialTransaction[] = [
  {
    id: 1,
    type: "INCOME",
    category: "حجز غرفة",
    amount: 2400,
    description: "حجز جناح 302 - ياسر القحطاني كامل القيمة",
    date: todayStr,
    timestamp: Date.now() - 24 * 60 * 60 * 1000,
    bookingId: 1
  },
  {
    id: 2,
    type: "EXPENSE",
    category: "نثريات ومشتريات",
    amount: 450,
    description: "فاتورة مغسلة شراشف ومستلزمات خارجية",
    date: todayStr,
    timestamp: Date.now() - 12 * 60 * 60 * 1000
  },
  {
    id: 3,
    type: "INCOME",
    category: "حجز غرفة",
    amount: 1500,
    description: "حجز غرفة سابق منتهي لضيف سابق",
    date: todayStr,
    timestamp: Date.now() - 3 * 24 * 60 * 60 * 1000
  }
];

// LocalDatabase helper wrap
export class LocalDatabase {
  static get<T>(key: string, defaultValue: T): T {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : defaultValue;
    } catch {
      return defaultValue;
    }
  }

  static set(key: string, value: any): void {
    localStorage.setItem(key, JSON.stringify(value));
  }

  // Load all tables or initialize seeds
  static load() {
    const users = this.get<AppUser[]>("hotel_users", DEFAULT_USERS);
    const settings = this.get<Settings>("hotel_settings", DEFAULT_SETTINGS);
    const floors = this.get<Floor[]>("hotel_floors", DEFAULT_FLOORS);
    const rooms = this.get<Room[]>("hotel_rooms", DEFAULT_ROOMS);
    const bookings = this.get<Booking[]>("hotel_bookings", DEFAULT_BOOKINGS);
    const transactions = this.get<FinancialTransaction[]>("hotel_transactions", DEFAULT_TRANSACTIONS);

    // Re-save to guarantee localStorage initialization
    this.saveAll(users, settings, floors, rooms, bookings, transactions);

    return { users, settings, floors, rooms, bookings, transactions };
  }

  static saveAll(
    users: AppUser[],
    settings: Settings,
    floors: Floor[],
    rooms: Room[],
    bookings: Booking[],
    transactions: FinancialTransaction[]
  ) {
    this.set("hotel_users", users);
    this.set("hotel_settings", settings);
    this.set("hotel_floors", floors);
    this.set("hotel_rooms", rooms);
    this.set("hotel_bookings", bookings);
    this.set("hotel_transactions", transactions);
  }
}
