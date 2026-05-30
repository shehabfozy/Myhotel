import React, { useState, useEffect, useMemo, useRef } from 'react';
import { 
  Home, 
  List, 
  Coins, 
  BarChart3, 
  Users, 
  Settings as SettingsIcon, 
  LogOut, 
  Plus, 
  UserPlus, 
  Search, 
  Camera, 
  UserCheck, 
  Download, 
  Check, 
  Trash2, 
  Sparkles, 
  TrendingUp, 
  Printer, 
  X, 
  Calendar,
  Building,
  DollarSign,
  AlertCircle
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  BarChart as RechartsBarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  PieChart as RechartsPieChart, 
  Pie, 
  Cell, 
  Legend,
  AreaChart,
  Area
} from 'recharts';
import { 
  LocalDatabase, 
  Floor, 
  Room, 
  Booking, 
  FinancialTransaction, 
  AppUser, 
  Settings, 
  GuestCompanion, 
  getTodayDateString 
} from './db';

export default function App() {
  // Load local persistence tables
  const [dbState, setDbState] = useState(() => LocalDatabase.load());
  const [currentUser, setCurrentUser] = useState<AppUser | null>(() => {
    const saved = localStorage.getItem('hotel_current_user');
    return saved ? JSON.parse(saved) : null;
  });

  // Navigation Screen State
  const [currentScreen, setCurrentScreen] = useState<'DASHBOARD' | 'BOOKINGS' | 'FINANCIALS' | 'REPORTS' | 'USERS' | 'SETTINGS'>('DASHBOARD');

  // Input states for form submissions
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginError, setLoginError] = useState('');

  // Active Floor and Room lists states
  const { users, settings, floors, rooms, bookings, transactions } = dbState;

  // Active floor filter in ROOMS screen
  const [selectedFloorId, setSelectedFloorId] = useState<number | 'ALL'>('ALL');
  const [selectedRoomStatus, setSelectedRoomStatus] = useState<string | 'ALL'>('ALL');

  // Modals visibility states
  const [showAddFloorModal, setShowAddFloorModal] = useState(false);
  const [showAddRoomModal, setShowAddRoomModal] = useState(false);
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [bookingRoom, setBookingRoom] = useState<Room | null>(null);
  const [showRoomDetailsModal, setShowRoomDetailsModal] = useState(false);
  const [selectedRoomDetails, setSelectedRoomDetails] = useState<Room | null>(null);

  // New Floor Form state
  const [newFloorName, setNewFloorName] = useState('');

  // New Room Form state
  const [newRoomNumber, setNewRoomNumber] = useState('');
  const [newRoomType, setNewRoomType] = useState('غرفة مفردة');
  const [newRoomPrice, setNewRoomPrice] = useState('150');
  const [newRoomFloorId, setNewRoomFloorId] = useState<number>(1);

  // New Booking Form states
  const [bookingGuestName, setBookingGuestName] = useState('');
  const [bookingIdType, setBookingIdType] = useState('هوية وطنية');
  const [bookingIdNumber, setBookingIdNumber] = useState('');
  const [bookingPhone, setBookingPhone] = useState('');
  const [bookingNationality, setBookingNationality] = useState('سعودي');
  const [bookingNights, setBookingNights] = useState('2'); // -1 for short rest / استراحة
  const [bookingPrice, setBookingPrice] = useState('');
  const [bookingPaid, setBookingPaid] = useState('');
  const [bookingIdPhoto, setBookingIdPhoto] = useState<string | null>(null);

  // Compact Guest Companion state
  const [newCompName, setNewCompName] = useState('');
  const [newCompIdType, setNewCompIdType] = useState('هوية وطنية');
  const [newCompIdNum, setNewCompIdNum] = useState('');
  const [newCompPhone, setNewCompPhone] = useState('');
  const [tempCompanions, setTempCompanions] = useState<GuestCompanion[]>([]);

  // Search states on Bookings and Financials
  const [bookingSearchQuery, setBookingSearchQuery] = useState('');
  const [bookingStatusFilter, setBookingStatusFilter] = useState<'ALL' | 'ACTIVE' | 'COMPLETED'>('ALL');
  const [financialSearchQuery, setFinancialSearchQuery] = useState('');
  const [financialTypeFilter, setFinancialTypeFilter] = useState<'ALL' | 'INCOME' | 'EXPENSE'>('ALL');

  // Add Income/Expense financial state
  const [showAddFinancialModal, setShowAddFinancialModal] = useState(false);
  const [finType, setFinType] = useState<'INCOME' | 'EXPENSE'>('INCOME');
  const [finCategory, setFinCategory] = useState('تأجير');
  const [finAmount, setFinAmount] = useState('');
  const [finDescription, setFinDescription] = useState('');

  // Staff additions state
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [newStaffUsername, setNewStaffUsername] = useState('');
  const [newStaffPassword, setNewStaffPassword] = useState('');
  const [newStaffFullname, setNewStaffFullname] = useState('');
  const [newStaffRole, setNewStaffRole] = useState<'ADMIN' | 'RECEPTIONIST'>('RECEPTIONIST');

  // Settings states
  const [editHotelName, setEditHotelName] = useState(settings.hotel_name);
  const [editCurrency, setEditCurrency] = useState(settings.currency);
  const [editCheckoutHour, setEditCheckoutHour] = useState(settings.checkout_hour);
  const [editRestHours, setEditRestHours] = useState(settings.rest_duration_hours);

  // Webcam scanning mock/real states
  const [scannerStatus, setScannerStatus] = useState<'IDLE' | 'SCANNING' | 'SUCCESS' | 'ERROR'>('IDLE');
  const [scannerMsg, setScannerMsg] = useState('');
  const [scannerCameraActive, setScannerCameraActive] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Sync to database
  const saveStateToLocalStorage = (
    updatedUsers = users,
    updatedSettings = settings,
    updatedFloors = floors,
    updatedRooms = rooms,
    updatedBookings = bookings,
    updatedTransactions = transactions
  ) => {
    LocalDatabase.saveAll(updatedUsers, updatedSettings, updatedFloors, updatedRooms, updatedBookings, updatedTransactions);
    setDbState({
      users: updatedUsers,
      settings: updatedSettings,
      floors: updatedFloors,
      rooms: updatedRooms,
      bookings: updatedBookings,
      transactions: updatedTransactions
    });
  };

  // Run initial state update
  useEffect(() => {
    if (dbState.floors.length === 0) {
      setDbState(LocalDatabase.load());
    }
  }, []);

  // Update Settings from database upon loading
  useEffect(() => {
    setEditHotelName(settings.hotel_name);
    setEditCurrency(settings.currency);
    setEditCheckoutHour(settings.checkout_hour);
    setEditRestHours(settings.rest_duration_hours);
  }, [settings]);

  // Handle Login submission
  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    const user = users.find(u => u.username === loginUsername.trim().toLowerCase());
    // For demo purposes and as requested, direct credentials work
    if (user && (loginPassword === 'admin123' || loginPassword === '123' || loginPassword === 'admin')) {
      setCurrentUser(user);
      localStorage.setItem('hotel_current_user', JSON.stringify(user));
      setLoginError('');
    } else {
      setLoginError('اسم المستخدم أو كلمة المرور خاطئة. جرب admin/admin123');
    }
  };

  // Switch Quick Logins for tester speed!
  const triggerQuickLogin = (role: 'ADMIN' | 'RECEPTIONIST') => {
    if (role === 'ADMIN') {
      const adminUser = users.find(u => u.role === 'ADMIN') || { id: 1, username: 'admin', role: 'ADMIN', fullname: 'المدير العام (أحمد)' };
      setCurrentUser(adminUser);
      localStorage.setItem('hotel_current_user', JSON.stringify(adminUser));
    } else {
      const recepUser = users.find(u => u.role === 'RECEPTIONIST') || { id: 2, username: 'reception', role: 'RECEPTIONIST', fullname: 'موظف الاستقبال' };
      setCurrentUser(recepUser);
      localStorage.setItem('hotel_current_user', JSON.stringify(recepUser));
    }
  };

  // Sign out
  const handleLogout = () => {
    setCurrentUser(null);
    localStorage.removeItem('hotel_current_user');
  };

  // Form submit handles
  const handleAddFloor = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFloorName.trim()) return;
    const nextId = floors.length > 0 ? Math.max(...floors.map(f => f.id)) + 1 : 1;
    const newFloor: Floor = { id: nextId, name: newFloorName.trim() };
    const updated = [...floors, newFloor];
    saveStateToLocalStorage(users, settings, updated);
    setNewFloorName('');
    setShowAddFloorModal(false);
  };

  const handleAddRoom = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRoomNumber.trim() || !newRoomPrice.trim()) return;
    const nextId = rooms.length > 0 ? Math.max(...rooms.map(r => r.id)) + 1 : 1;
    const newRoom: Room = {
      id: nextId,
      floorId: Number(newRoomFloorId),
      roomNumber: newRoomNumber.trim(),
      roomType: newRoomType,
      price: Number(newRoomPrice),
      status: "AVAILABLE"
    };
    const updated = [...rooms, newRoom];
    saveStateToLocalStorage(users, settings, floors, updated);
    setNewRoomNumber('');
    setShowAddRoomModal(false);
  };

  // Prepare booking screen price defaults when room selected
  const openNewBooking = (room: Room) => {
    setBookingRoom(room);
    setBookingGuestName('');
    setBookingIdNumber('');
    setBookingPhone('');
    setBookingPrice(String(room.price));
    setBookingNights('2');
    setBookingPaid(String(room.price * 2));
    setTempCompanions([]);
    setBookingIdPhoto(null);
    setScannerStatus('IDLE');
    setShowBookingModal(true);
  };

  useEffect(() => {
    if (bookingRoom) {
      const nights = Number(bookingNights);
      const prc = Number(bookingPrice) || 0;
      if (nights === -1) {
        // short rest / استراحة
        const restPrice = Math.round(prc * 0.5); // 50% price for short rest
        setBookingPaid(String(restPrice));
      } else {
        setBookingPaid(String(prc * nights));
      }
    }
  }, [bookingNights, bookingPrice, bookingRoom]);

  // Handle companion adds
  const addCompanionToTempList = () => {
    if (!newCompName.trim() || !newCompIdNum.trim()) return;
    const newComp: GuestCompanion = {
      name: newCompName.trim(),
      idType: newCompIdType,
      idNumber: newCompIdNum.trim(),
      phone: newCompPhone.trim()
    };
    setTempCompanions([...tempCompanions, newComp]);
    setNewCompName('');
    setNewCompIdNum('');
    setNewCompPhone('');
  };

  const removeCompanionFromTempList = (idx: number) => {
    setTempCompanions(tempCompanions.filter((_, i) => i !== idx));
  };

  // Create booking submit
  const handleCreateBooking = (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookingRoom || !bookingGuestName.trim() || !bookingIdNumber.trim()) return;

    const nights = Number(bookingNights);
    const prc = Number(bookingPrice) || 0;
    const totalPrice = nights === -1 ? Math.round(prc * 0.5) : prc * nights;
    const paid = Number(bookingPaid) || 0;
    const remaining = Math.max(0, totalPrice - paid);

    const nextBookId = bookings.length > 0 ? Math.max(...bookings.map(b => b.id)) + 1 : 1;
    const newBooking: Booking = {
      id: nextBookId,
      roomId: bookingRoom.id,
      roomNumber: bookingRoom.roomNumber,
      roomType: bookingRoom.roomType,
      guestName: bookingGuestName,
      idType: bookingIdType,
      idNumber: bookingIdNumber,
      phone: bookingPhone,
      companionsCount: tempCompanions.length,
      companions: tempCompanions,
      nightsCount: nights,
      pricePerNight: prc,
      amountPaid: paid,
      amountRemaining: remaining,
      bookingDate: getTodayDateString(),
      checkInTime: Date.now(),
      checkOutTime: nights === -1 ? Date.now() + 3 * 60 * 60 * 1000 : Date.now() + nights * 24 * 60 * 60 * 1000,
      status: "ACTIVE",
      idPhotoBase64: bookingIdPhoto || undefined,
      nationality: bookingNationality
    };

    // Mark room as RESERVED
    const updatedRooms = rooms.map(r => r.id === bookingRoom.id ? { ...r, status: "RESERVED" as const } : r);
    const updatedBookings = [...bookings, newBooking];

    // Record dynamic entry into finances
    const nextTxId = transactions.length > 0 ? Math.max(...transactions.map(t => t.id)) + 1 : 1;
    const newTx: FinancialTransaction = {
      id: nextTxId,
      type: "INCOME",
      category: "حجز غرفة",
      amount: paid,
      description: `حملة حجز الغرفة ${bookingRoom.roomNumber} - النزيل ${bookingGuestName}`,
      date: getTodayDateString(),
      timestamp: Date.now(),
      bookingId: nextBookId
    };

    const updatedTxs = paid > 0 ? [...transactions, newTx] : transactions;

    saveStateToLocalStorage(users, settings, floors, updatedRooms, updatedBookings, updatedTxs);
    setShowBookingModal(false);
    setBookingRoom(null);
  };

  // Checkout room
  const handleCheckoutRoom = (bookingId: number, completePaymentNow: boolean) => {
    const b = bookings.find(x => x.id === bookingId);
    if (!b) return;

    // Mark booking as completed
    const updatedBookings = bookings.map(x => x.id === bookingId ? { ...x, status: "COMPLETED" as const, amountRemaining: 0, amountPaid: x.amountPaid + (completePaymentNow ? x.amountRemaining : 0) } : x);

    // Mark room as CLEANING
    const updatedRooms = rooms.map(r => r.id === b.roomId ? { ...r, status: "CLEANING" as const } : r);

    // Record any final pending financial transaction if paid
    let updatedTxs = transactions;
    if (completePaymentNow && b.amountRemaining > 0) {
      const nextTxId = transactions.length > 0 ? Math.max(...transactions.map(t => t.id)) + 1 : 1;
      const checkoutTx: FinancialTransaction = {
        id: nextTxId,
        type: "INCOME",
        category: "حجز غرفة",
        amount: b.amountRemaining,
        description: `تسوية الدفع المتبقي للغرفة ${b.roomNumber} عند المغادرة للنزيل ${b.guestName}`,
        date: getTodayDateString(),
        timestamp: Date.now(),
        bookingId: b.id
      };
      updatedTxs = [...transactions, checkoutTx];
    }

    saveStateToLocalStorage(users, settings, floors, updatedRooms, updatedBookings, updatedTxs);
    setShowRoomDetailsModal(false);
    setSelectedRoomDetails(null);
  };

  // Change room cleaning state to available
  const markRoomClean = (roomId: number) => {
    const updatedRooms = rooms.map(r => r.id === roomId ? { ...r, status: "AVAILABLE" as const } : r);
    saveStateToLocalStorage(users, settings, floors, updatedRooms);
  };

  // Handle direct accounting ledger submissions (Income/Expense dialog)
  const handleAddFinancial = (e: React.FormEvent) => {
    e.preventDefault();
    if (!finAmount.trim() || !finCategory.trim()) return;

    const nextId = transactions.length > 0 ? Math.max(...transactions.map(t => t.id)) + 1 : 1;
    const newTx: FinancialTransaction = {
      id: nextId,
      type: finType,
      category: finCategory,
      amount: Number(finAmount),
      description: finDescription.trim() || finCategory,
      date: getTodayDateString(),
      timestamp: Date.now()
    };

    const updated = [...transactions, newTx];
    saveStateToLocalStorage(users, settings, floors, rooms, bookings, updated);
    setFinAmount('');
    setFinDescription('');
    setShowAddFinancialModal(false);
  };

  // User staff add
  const handleAddStaff = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newStaffUsername.trim() || !newStaffPassword.trim() || !newStaffFullname.trim()) return;

    const nextId = users.length > 0 ? Math.max(...users.map(u => u.id)) + 1 : 1;
    const newUser: AppUser = {
      id: nextId,
      username: newStaffUsername.trim().toLowerCase(),
      role: newStaffRole,
      fullname: newStaffFullname.trim()
    };

    const updated = [...users, newUser];
    saveStateToLocalStorage(updated);
    setNewStaffUsername('');
    setNewStaffPassword('');
    setNewStaffFullname('');
    setShowAddUserModal(false);
  };

  // Remove staff
  const handleRemoveStaff = (id: number) => {
    if (id === currentUser?.id) return alert('لا يمكنك حذف حسابك الحالي!');
    const updated = users.filter(u => u.id !== id);
    saveStateToLocalStorage(updated);
  };

  // Update global settings
  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    const updatedSettings: Settings = {
      hotel_name: editHotelName,
      currency: editCurrency,
      checkout_hour: editCheckoutHour,
      rest_duration_hours: editRestHours
    };
    saveStateToLocalStorage(users, updatedSettings);
    alert('تم حفظ الإعدادات بنجاح!');
  };

  // Web camera setup for capture scanning
  const startCamera = async () => {
    setScannerCameraActive(true);
    setScannerStatus('SCANNING');
    setScannerMsg('يرجى وضع بطاقة الهوية أمام الكاميرا...');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
      }
    } catch (err) {
      console.error(err);
      setScannerStatus('ERROR');
      setScannerMsg('تعذر تشغيل كاميرا الويب. يمكنك استخدام البطاقات الجاهزة للتجربة!');
    }
  };

  const stopCamera = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      const stream = videoRef.current.srcObject as MediaStream;
      stream.getTracks().forEach(track => track.stop());
      videoRef.current.srcObject = null;
    }
    setScannerCameraActive(false);
  };

  // Call the backend endpoint to capture and run the Gemini OCR
  const captureAndScan = async () => {
    try {
      if (!videoRef.current || !canvasRef.current) return;
      const video = videoRef.current;
      const canvas = canvasRef.current;
      canvas.width = video.videoWidth || 640;
      canvas.height = video.videoHeight || 480;
      const context = canvas.getContext('2d');
      if (context) {
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.8);
        setBookingIdPhoto(dataUrl);
        stopCamera();

        setScannerStatus('SCANNING');
        setScannerMsg('يقوم الذكاء الاصطناعي الآن بمسح الفاتورة واستخراج البيانات...');

        const response = await fetch('/api/scan-id', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ base64Image: dataUrl })
        });

        if (!response.ok) {
          throw new Error('فشل معالجة بطاقة الهوية من السيرفر. ربما مفتاح API ليس مهيئاً بشكل كامل.');
        }

        const scanResult = await response.json();
        if (scanResult.error) {
          throw new Error(scanResult.error);
        }

        // Fill form with AI results!
        setBookingGuestName(scanResult.name || '');
        setBookingIdNumber(scanResult.id_number || '');
        setBookingIdType(scanResult.id_type || 'هوية وطنية');
        if (scanResult.nationality) setBookingNationality(scanResult.nationality);

        setScannerStatus('SUCCESS');
        setScannerMsg(`تم التعرف بنجاح على: ${scanResult.name}`);
      }
    } catch (error: any) {
      console.error(error);
      setScannerStatus('ERROR');
      setScannerMsg(error.message || 'حدث خطأ أثناء مسح بطاقة الهوية. يمكنك ملء الخانات يدوياً.');
    }
  };

  // Preset Card samples for users to click and test instantly! Let's build beautiful demo values
  const triggerPresetScanDemo = async (presetType: 'SAUDI' | 'RESIDENT' | 'PASSPORT') => {
    setScannerStatus('SCANNING');
    setScannerMsg('يقوم الذكاء الاصطناعي (Gemini 3.5 Flash) بالتعرف على بطاقة الهوية المحاكية...');
    
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1500));

    let imgBase = ""; // We can put a small colorful decorative mock or let it send a standard dummy image to backend
    let dummyName = "خالد بن مساعد آل سعود";
    let dummyId = "1028374655";
    let dummyType = "هوية وطنية";
    let dummyNat = "سعودي";

    if (presetType === 'RESIDENT') {
      dummyName = "جون دو جونسون";
      dummyId = "2498571029";
      dummyType = "هوية مقيم";
      dummyNat = "أمريكي";
    } else if (presetType === 'PASSPORT') {
      dummyName = "سارة أحمد الجابر";
      dummyId = "F3729103";
      dummyType = "جواز سفر";
      dummyNat = "كويتي";
    }

    setBookingGuestName(dummyName);
    setBookingIdNumber(dummyId);
    setBookingIdType(dummyType);
    setBookingNationality(dummyNat);
    
    // Set a stylish placeholder ID card icon base
    setBookingIdPhoto("PRESET");
    setScannerStatus('SUCCESS');
    setScannerMsg(`تم محاكاة المسح بنجاح! تم استخراج: ${dummyName}`);
  };

  // Memoized stats & summaries
  const stats = useMemo(() => {
    const totalRooms = rooms.length;
    const reservedRooms = rooms.filter(r => r.status === 'RESERVED').length;
    const cleaningRooms = rooms.filter(r => r.status === 'CLEANING').length;
    const availableRooms = rooms.filter(r => r.status === 'AVAILABLE').length;
    const occupancyPercentage = totalRooms > 0 ? Math.round((reservedRooms / totalRooms) * 100) : 0;

    // Today's incomes inside database transactions
    const totalRevenue = transactions.filter(t => t.type === 'INCOME').reduce((sum, t) => sum + t.amount, 0);
    const totalExpense = transactions.filter(t => t.type === 'EXPENSE').reduce((sum, t) => sum + t.amount, 0);
    const balance = totalRevenue - totalExpense;

    return {
      totalRooms,
      reservedRooms,
      cleaningRooms,
      availableRooms,
      occupancyPercentage,
      totalRevenue,
      totalExpense,
      balance
    };
  }, [rooms, transactions]);

  // Filtered rooms depending on selections
  const filteredRooms = useMemo(() => {
    return rooms.filter(r => {
      const matchFloor = selectedFloorId === 'ALL' || r.floorId === selectedFloorId;
      const matchStatus = selectedRoomStatus === 'ALL' || r.status === selectedRoomStatus;
      return matchFloor && matchStatus;
    });
  }, [rooms, selectedFloorId, selectedRoomStatus]);

  // Active bookings list details matching rooms
  const activeBookingsMap = useMemo(() => {
    const active = bookings.filter(b => b.status === 'ACTIVE');
    const map: Record<number, Booking> = {};
    active.forEach(b => {
      map[b.roomId] = b;
    });
    return map;
  }, [bookings]);

  // Logged-in view wrapper
  if (!currentUser) {
    return (
      <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-4">
        {/* Glowing Background */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl"></div>
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-emerald-600/10 rounded-full blur-3xl"></div>

        <div className="w-full max-w-md bg-slate-800 border border-slate-700 rounded-3xl p-8 shadow-2xl relative z-10 text-white">
          <div className="text-center mb-8">
            <div className="w-16 h-16 bg-blue-600 rounded-2xl mx-auto flex items-center justify-center text-xl font-black italic shadow-lg mb-4">
              HM
            </div>
            <h1 className="text-2xl font-bold">تسجيل الدخول للنظام الفندقي</h1>
            <p className="text-slate-400 text-sm mt-2">نظام واحة الفندقية لإدارة الحجوزات والسكن</p>
          </div>

          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-slate-300 text-sm font-semibold mb-2">اسم المستخدم</label>
              <input 
                type="text" 
                placeholder="أدخل اسم المستخدم (مثال: admin أو reception)"
                value={loginUsername}
                onChange={(e) => setLoginUsername(e.target.value)}
                className="w-full bg-slate-900/60 border border-slate-700 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm text-right font-semibold"
                required
              />
            </div>

            <div>
              <label className="block text-slate-300 text-sm font-semibold mb-2">كلمة المرور</label>
              <input 
                type="password" 
                placeholder="أدخل كلمة المرور (مثال: admin123 أو 123)"
                value={loginPassword}
                onChange={(e) => setLoginPassword(e.target.value)}
                className="w-full bg-slate-900/60 border border-slate-700 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm text-right"
                required
              />
            </div>

            {loginError && (
              <div className="bg-red-500/15 border border-red-500/30 text-red-200 p-3 rounded-xl text-xs flex items-center gap-2">
                <AlertCircle size={14} className="shrink-0" />
                <span>{loginError}</span>
              </div>
            )}

            <button 
              type="submit"
              className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-xl font-bold shadow-lg transition-colors text-sm"
            >
              تسجيل الدخول
            </button>
          </form>

          <div className="mt-8 border-t border-slate-700 pt-6">
            <p className="text-center text-xs text-slate-400 mb-4">الدخول السريع التجريبي للتقييم والمراجعة:</p>
            <div className="grid grid-cols-2 gap-3">
              <button 
                onClick={() => triggerQuickLogin('ADMIN')}
                className="bg-slate-700 hover:bg-slate-600 text-white py-2.5 px-4 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2"
              >
                👤 لوحة المدير العام
              </button>
              <button 
                onClick={() => triggerQuickLogin('RECEPTIONIST')}
                className="bg-slate-700 hover:bg-slate-600 text-white py-2.5 px-4 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2"
              >
                🏨 طاقم الاستقبال
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full bg-slate-50 flex overflow-hidden font-sans text-slate-800" dir="rtl">
      {/* SIDEBAR NAVIGATION PANEL */}
      <aside className="w-64 bg-slate-900 text-white flex flex-col shrink-0 border-l border-slate-800">
        <div className="p-6 border-b border-slate-800 flex items-center gap-3">
          <span className="w-9 h-9 bg-blue-600 rounded-xl flex items-center justify-center text-sm font-black italic shadow">HM</span>
          <div className="text-right">
            <h1 className="text-base font-extrabold text-white leading-tight">{settings.hotel_name}</h1>
            <p className="text-[10px] text-blue-400 font-bold">بوابة الإدارة الذكية</p>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1.5 overflow-y-auto">
          {[
            { id: 'DASHBOARD', title: 'لوحة التحكم والغرف', icon: Home },
            { id: 'BOOKINGS', title: 'إدارة النزلاء والحجوزات', icon: List },
            { id: 'FINANCIALS', title: 'الخزينة والحسابات', icon: Coins },
            { id: 'REPORTS', title: 'التقارير والإحصائيات', icon: BarChart3 },
            { id: 'USERS', title: 'إدارة الطاقم والصلاحيات', icon: Users },
            { id: 'SETTINGS', title: 'الإعدادات العامة', icon: SettingsIcon },
          ].map((item) => {
            const IconComponent = item.icon;
            const isSelected = currentScreen === item.id;
            return (
              <button 
                key={item.id}
                onClick={() => setCurrentScreen(item.id as any)}
                className={`w-full flex items-center gap-3 p-3 rounded-xl text-sm font-semibold text-right transition-all leading-none ${
                  isSelected 
                    ? 'bg-blue-600 text-white font-bold shadow-lg shadow-blue-900/40' 
                    : 'text-slate-400 hover:bg-slate-800/60 hover:text-white'
                }`}
              >
                <IconComponent size={18} className={isSelected ? 'text-white' : 'text-slate-400'} />
                <span>{item.title}</span>
              </button>
            );
          })}
        </nav>

        {/* Footer Sidebar with user profile info */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center shrink-0">
              <span className="text-sm font-bold text-blue-400">
                {currentUser.role === 'ADMIN' ? 'مدير' : 'موظف'}
              </span>
            </div>
            <div className="text-sm overflow-hidden text-right">
              <p className="text-white font-bold truncate leading-none mb-1">{currentUser.fullname}</p>
              <p className="text-xs text-slate-500 font-medium">
                {currentUser.role === 'ADMIN' ? 'المدير العام' : 'موظف الاستقبال'}
              </p>
            </div>
            <button 
              onClick={handleLogout}
              className="mr-auto text-slate-400 hover:text-red-400 p-1 rounded-lg transition-colors"
              title="تسجيل خروج"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>

      {/* MAIN SCREEN INTERFACE */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* TOP STATUS HEADER BAR */}
        <header className="h-16 bg-white border-b border-slate-200 px-6 flex justify-between items-center shrink-0">
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-500 font-semibold">{getTodayDateString()}</span>
            <span className="bg-slate-100 text-slate-600 text-xs px-2.5 py-1 rounded-full font-bold">بث مباشر للنظام</span>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-left text-xs">
              <p className="text-slate-400">مستخدم حالي</p>
              <span className="text-slate-800 font-extrabold">{currentUser.fullname}</span>
            </div>
          </div>
        </header>

        {/* MAIN BODY SCROLLABLE CANVAS */}
        <div className="flex-1 overflow-y-auto p-6 bg-slate-50">
          {currentScreen === 'DASHBOARD' && (
            <div className="space-y-6">
              {/* TOP HEADER CONTROLS */}
              <div className="flex justify-between items-center flex-wrap gap-4">
                <div>
                  <h2 className="text-2xl font-extrabold text-slate-800">حالة الغرف والشقق السكنية</h2>
                  <p className="text-sm text-slate-500 mt-1">نظرة عامة على الإشغال والتسجيل ومخطط الطوابق المباشر</p>
                </div>

                <div className="flex gap-2">
                  {currentUser.role === 'ADMIN' && (
                    <>
                      <button 
                        onClick={() => setShowAddFloorModal(true)}
                        className="bg-slate-200 hover:bg-slate-300 text-slate-700 px-4 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shadow-sm"
                      >
                        <Plus size={14} />
                        طابق جديد
                      </button>
                      <button 
                        onClick={() => setShowAddRoomModal(true)}
                        className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shadow-md shadow-blue-600/20"
                      >
                        <Plus size={14} />
                        غرفة جديدة
                      </button>
                    </>
                  )}
                </div>
              </div>

              {/* BENTO STATS SECTION */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Stat 1 */}
                <div className="bg-white p-6 rounded-3xl border border-slate-100 shadow-sm flex flex-col justify-between">
                  <div className="flex justify-between items-start">
                    <span className="text-sm font-semibold text-slate-400">نسبة الإشغال</span>
                    <span className="bg-blue-50 text-blue-600 text-xs px-2 py-0.5 rounded-full font-bold">معدل نشط</span>
                  </div>
                  <div className="mt-4">
                    <h3 className="text-3xl font-black text-blue-600 leading-none">{stats.occupancyPercentage}%</h3>
                    <div className="w-full bg-slate-100 h-2 rounded-full mt-3 overflow-hidden">
                      <div className="bg-blue-600 h-full rounded-full transition-all duration-500" style={{ width: `${stats.occupancyPercentage}%` }}></div>
                    </div>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-2 font-medium">النزلاء المتواجدون حالياً: {stats.reservedRooms} شقة وغرفة</p>
                </div>

                {/* Stat 2 */}
                <div className="bg-white p-6 rounded-3xl border border-slate-100 shadow-sm flex flex-col justify-between">
                  <div className="flex justify-between items-start">
                    <span className="text-sm font-semibold text-slate-400">رصيد الخزينة المباشر</span>
                    <span className="bg-emerald-50 text-emerald-600 text-xs px-2 py-0.5 rounded-full font-bold">حالة الصندوق</span>
                  </div>
                  <div className="mt-4">
                    <h3 className="text-3xl font-black text-emerald-600 leading-none">{stats.balance} {settings.currency}</h3>
                    <div className="w-full bg-slate-100 h-2 rounded-full mt-3 overflow-hidden">
                      <div className="bg-emerald-500 h-full rounded-full transition-all duration-500" style={{ width: `75%` }}></div>
                    </div>
                  </div>
                  <p className="text-[11px] text-emerald-600 mt-2 font-bold">مجموع الإيرادات: +{stats.totalRevenue} {settings.currency}</p>
                </div>

                {/* Stat 3 */}
                <div className="bg-white p-6 rounded-3xl border border-slate-100 shadow-sm flex flex-col justify-between">
                  <div className="flex justify-between items-start">
                    <span className="text-sm font-semibold text-slate-400">الغرف المتاحة حالياً</span>
                    <span className="bg-slate-100 text-slate-600 text-xs px-2 py-0.5 rounded-full font-bold">جاهز للتأجير</span>
                  </div>
                  <div className="mt-4">
                    <h3 className="text-3xl font-black text-slate-700 leading-none">{stats.availableRooms} وحدة</h3>
                    <div className="w-full bg-slate-100 h-2 rounded-full mt-3 overflow-hidden">
                      <div className="bg-slate-400 h-full rounded-full transition-all duration-500" style={{ width: `${(stats.availableRooms / (stats.totalRooms || 1)) * 100}%` }}></div>
                    </div>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-2 font-medium">تحت الصيانة أو التنظيف العاجل: {stats.cleaningRooms} وحدة</p>
                </div>
              </div>

              {/* DUAL COLS BENTO GRID (Main layout mirroring bento mock design) */}
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
                
                {/* 1. ROOM GRID MAP CONTAINER (8 Columns wide) */}
                <div className="lg:col-span-8 bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex flex-col">
                  <div className="flex justify-between items-center flex-wrap gap-4 mb-6 pb-4 border-b border-slate-100">
                    <div>
                      <h3 className="font-extrabold text-lg text-slate-800">خريطة ومخطط الغرف الطبيعي</h3>
                      <p className="text-xs text-slate-400 mt-0.5">اختر طابقاً أو تصفح الغرف مباشرة للتحقق أو الحجز الفوري</p>
                    </div>

                    <div className="flex gap-1.5 flex-wrap">
                      <button 
                        onClick={() => setSelectedFloorId('ALL')}
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold leading-none transition-all ${selectedFloorId === 'ALL' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
                      >
                        الكل
                      </button>
                      {floors.map(f => (
                        <button 
                          key={f.id}
                          onClick={() => setSelectedFloorId(f.id)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-bold leading-none transition-all ${selectedFloorId === f.id ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
                        >
                          {f.name}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Room status categories guidelines */}
                  <div className="flex gap-4 text-[11px] text-slate-400 font-bold mb-4">
                    <span className="flex items-center gap-1.5">
                      <span className="w-3.5 h-3.5 bg-emerald-50 border border-emerald-300 rounded-md"></span>
                      متاح ({rooms.filter(r => r.status === 'AVAILABLE').length})
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="w-3.5 h-3.5 bg-red-50 border border-red-300 rounded-md"></span>
                      محجوز / مسكون ({rooms.filter(r => r.status === 'RESERVED').length})
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="w-3.5 h-3.5 bg-amber-50 border border-amber-300 rounded-md"></span>
                      قيد التنظيف والتعقيم ({rooms.filter(r => r.status === 'CLEANING').length})
                    </span>
                  </div>

                  {/* Floor sections */}
                  <div className="space-y-6">
                    {(selectedFloorId === 'ALL' ? floors : floors.filter(f => f.id === selectedFloorId)).map(floor => {
                      const floorRooms = rooms.filter(r => r.floorId === floor.id);

                      return (
                        <div key={floor.id} className="space-y-3">
                          <h4 className="text-xs font-extrabold text-slate-400">{floor.name}</h4>
                          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                            {floorRooms.map(room => {
                              const activeBooking = activeBookingsMap[room.id];
                              let bgStyle = "bg-emerald-50/50 border-emerald-500 hover:bg-emerald-100/50 text-emerald-800";
                              let statusBadge = "متاح";

                              if (room.status === 'RESERVED') {
                                bgStyle = "bg-red-50/50 border-red-500 hover:bg-red-100/50 text-red-800";
                                statusBadge = activeBooking ? activeBooking.guestName : "مشغولة";
                              } else if (room.status === 'CLEANING') {
                                bgStyle = "bg-amber-50/50 border-amber-400 hover:bg-amber-100/50 text-amber-800";
                                statusBadge = "قيد التنظيف";
                              }

                              return (
                                <button
                                  key={room.id}
                                  onClick={() => {
                                    if (room.status === 'AVAILABLE') {
                                      openNewBooking(room);
                                    } else {
                                      setSelectedRoomDetails(room);
                                      setShowRoomDetailsModal(true);
                                    }
                                  }}
                                  className={`p-4 rounded-2xl border-2 text-right transition-all transform hover:-translate-y-0.5 focus:outline-none flex flex-col justify-between h-24 ${bgStyle}`}
                                >
                                  <div className="w-full flex justify-between items-center">
                                    <span className="text-lg font-extrabold">{room.roomNumber}</span>
                                    <span className="text-[10px] font-extrabold opacity-75">{room.roomType}</span>
                                  </div>
                                  <div className="w-full">
                                    <p className="text-xs font-extrabold truncate">{statusBadge}</p>
                                    <p className="text-[11px] font-mono font-bold opacity-80 mt-1">{room.price} {settings.currency} / ليلة</p>
                                  </div>
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* 2. FAST ID SCANNER (4 Columns wide in Grid) */}
                <div className="lg:col-span-4 bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex flex-col">
                  <div className="flex items-center gap-2 mb-4">
                    <span className="w-8 h-8 bg-blue-100 rounded-xl flex items-center justify-center text-blue-600">
                      <Sparkles size={16} />
                    </span>
                    <div>
                      <h4 className="font-extrabold text-base text-slate-800">مسح بطاقة الهوية الذكي</h4>
                      <p className="text-[11px] text-slate-400 font-semibold leading-tight">مسح واستخراج بيانات الضيف بالذكاء الاصطناعي</p>
                    </div>
                  </div>

                  {/* ID Scanner Box */}
                  <div className="border-2 border-dashed border-slate-200 rounded-2xl bg-slate-50/50 p-4 text-center flex flex-col items-center justify-center relative overflow-hidden min-h-[190px]">
                    {scannerCameraActive ? (
                      <div className="w-full relative">
                        <video ref={videoRef} className="w-full h-40 object-cover rounded-xl border border-slate-300" playsInline></video>
                        <div className="absolute inset-0 border-2 border-blue-500 rounded-xl m-2 pointer-events-none border-dashed opacity-75"></div>
                        <button 
                          onClick={captureAndScan}
                          className="mt-3 w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-xl text-xs font-extrabold shadow-md"
                        >
                          تأكيد الالتقاط والمسح الراديوي 📸
                        </button>
                      </div>
                    ) : bookingIdPhoto ? (
                      <div className="text-center w-full">
                        <div className="w-20 h-14 bg-blue-100 rounded-lg mx-auto flex items-center justify-center text-blue-600 text-lg shadow-sm border border-blue-200">
                          {bookingIdPhoto === "PRESET" ? "🪪 بطاقة" : "📸 صورة"}
                        </div>
                        <p className="text-xs text-slate-500 font-bold mt-2">تم تجهيز وبناء صورة البطاقة بنجاح!</p>
                        <div className="mt-3 flex gap-2 justify-center">
                          <button 
                            onClick={() => { setBookingIdPhoto(null); setScannerStatus('IDLE'); }}
                            className="bg-slate-200 text-slate-700 px-3 py-1.5 rounded-lg text-[10px] font-bold"
                          >
                            مسح الصورة
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <div className="w-20 h-12 bg-slate-200 rounded-lg mb-3 flex items-center justify-center text-slate-400 font-bold text-xs border border-slate-300">
                          صورة البطاقة
                        </div>
                        <p className="text-xs text-slate-500 mb-4 leading-relaxed px-2">ضع هويتك أمام الكاميرا أو استخدم الأزرار السريعة بالأسفل للملء الآلي بالذكاء الاصطناعي للاختبار!</p>
                        <button 
                          onClick={startCamera}
                          className="w-full py-2.5 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-bold shadow-md"
                        >
                          بدء الكاميرا للمسح المباشر 📸
                        </button>
                      </>
                    )}

                    {/* Hidden canvas for video grabbing */}
                    <canvas ref={canvasRef} className="hidden"></canvas>
                  </div>

                  {/* Status Banner */}
                  {scannerStatus !== 'IDLE' && (
                    <div className={`mt-3 p-3 rounded-xl text-xs font-bold text-right ${
                      scannerStatus === 'SCANNING' ? 'bg-blue-50 text-blue-700 border border-blue-200/50' :
                      scannerStatus === 'SUCCESS' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200/50' :
                      'bg-red-50 text-red-700 border border-red-200/50'
                    }`}>
                      <p className="leading-tight flex items-center gap-1.5">
                        {scannerStatus === 'SCANNING' && <span className="animate-spin w-2 h-2 border-2 border-blue-600 border-t-transparent rounded-full"></span>}
                        <span>{scannerMsg}</span>
                      </p>
                    </div>
                  )}

                  {/* Demo presets triggers */}
                  <div className="mt-4 pt-4 border-t border-slate-100">
                    <p className="text-[11px] font-extrabold text-slate-400 mb-2">بطاقات هوية محاكية للتجريب بدون كاميرا (كبسة واحدة):</p>
                    <div className="grid grid-cols-3 gap-2">
                      <button 
                        onClick={() => triggerPresetScanDemo('SAUDI')}
                        className="bg-slate-100 hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 border border-transparent p-2 rounded-xl text-[10px] font-bold transition-all text-slate-600"
                      >
                        🇸🇦 هوية وطنية
                      </button>
                      <button 
                        onClick={() => triggerPresetScanDemo('RESIDENT')}
                        className="bg-slate-100 hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 border border-transparent p-2 rounded-xl text-[10px] font-bold transition-all text-slate-600"
                      >
                        🇺🇸 هوية مقيم
                      </button>
                      <button 
                        onClick={() => triggerPresetScanDemo('PASSPORT')}
                        className="bg-slate-100 hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 border border-transparent p-2 rounded-xl text-[10px] font-bold transition-all text-slate-600"
                      >
                        🇰🇼 جواز سفر
                      </button>
                    </div>
                  </div>

                  {/* Live filled values preview */}
                  {(bookingGuestName || bookingIdNumber) && (
                    <div className="mt-4 p-3 bg-slate-50 border border-slate-100 rounded-2xl text-[11px] space-y-2">
                      <h5 className="font-extrabold text-slate-400 border-b border-slate-200/60 pb-1 flex justify-between items-center">
                        <span>البيانات المستخرجة:</span>
                        <span className="text-[9px] bg-blue-100 text-blue-700 rounded px-1">جاهز للملء</span>
                      </h5>
                      <div className="grid grid-cols-2 gap-2 text-slate-700 font-extrabold">
                        <div>
                          <p className="text-[9px] text-slate-400 font-bold">الاسم الكامل</p>
                          <p className="truncate text-slate-800">{bookingGuestName}</p>
                        </div>
                        <div>
                          <p className="text-[9px] text-slate-400 font-bold">رقم الهوية</p>
                          <p className="font-mono">{bookingIdNumber}</p>
                        </div>
                      </div>
                      <p className="text-[10px] text-blue-600 font-bold">💡 انقر على أي غرفة &quot;متاحة&quot; لتفتح نافذة الحجز معبأة تلقائياً!</p>
                    </div>
                  )}
                </div>

                {/* 3. RECENT DEPOSITS LEDGER (8 Columns wide bottom) */}
                <div className="lg:col-span-8 bg-slate-900 rounded-3xl p-6 shadow-xl text-white relative overflow-hidden min-h-[220px]">
                  <div className="relative z-10">
                    <div className="flex justify-between items-center mb-4 pb-2 border-b border-slate-800">
                      <div>
                        <h4 className="font-bold text-base">العمليات والمدفوعات الأخيرة</h4>
                        <p className="text-xs text-slate-400 mt-0.5">سجل التدفق والتحصيلات المصرفية المباشرة بصندوق الاستقبال</p>
                      </div>
                      <span className="bg-emerald-500/20 text-emerald-400 text-[10px] px-2 py-0.5 rounded-full font-mono font-bold animate-pulse">
                        LIVE●
                      </span>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-xs text-right">
                        <thead className="text-slate-400 border-b border-slate-850">
                          <tr>
                            <th className="pb-2 text-right">الوصف</th>
                            <th className="pb-2 text-center">النوع</th>
                            <th className="pb-2 text-right">المبلغ</th>
                            <th className="pb-2 text-right">التاريخ</th>
                          </tr>
                        </thead>
                        <tbody className="text-slate-350 divide-y divide-slate-800/50">
                          {transactions.slice(-4).reverse().map(tx => (
                            <tr key={tx.id} className="hover:bg-slate-800/20">
                              <td className="py-2.5 font-semibold text-white">{tx.description}</td>
                              <td className="py-2.5 text-center">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  tx.type === 'INCOME' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'
                                }`}>
                                  {tx.type === 'INCOME' ? 'دخل' : 'مصروف'}
                                </span>
                              </td>
                              <td className={`py-2.5 font-bold ${tx.type === 'INCOME' ? 'text-emerald-400' : 'text-red-400'}`}>
                                {tx.type === 'INCOME' ? '+' : '-'}{tx.amount} {settings.currency}
                              </td>
                              <td className="py-2.5 font-mono text-[10px] text-slate-400">{tx.date}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                  <div className="absolute -right-10 -bottom-10 w-40 h-40 bg-blue-500/10 rounded-full blur-3xl"></div>
                </div>

                {/* 4. FINANCIAL GOALS (4 Columns wide bottom) */}
                <div className="lg:col-span-4 bg-white rounded-3xl p-6 shadow-sm border border-slate-100 h-full flex flex-col justify-between">
                  <div>
                    <div className="flex justify-between items-start mb-4">
                      <h4 className="font-bold text-slate-650 text-sm">أداء الميزانية والهدف</h4>
                      <span className="text-[10px] bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full font-bold">تقرير شهري</span>
                    </div>

                    <div className="mt-2 space-y-4">
                      <div>
                        <div className="flex justify-between text-xs font-bold text-slate-600 mb-1">
                          <span>الميزانية المحققة ({Math.round((stats.balance / 50000) * 100)}%)</span>
                          <span>50,000 {settings.currency}</span>
                        </div>
                        <div className="w-full bg-slate-100 h-3 rounded-full overflow-hidden border border-slate-200">
                          <div 
                            className="h-full bg-blue-600 rounded-full transition-all duration-500" 
                            style={{ width: `${Math.min(100, (stats.balance / 50000) * 100)}%` }}
                          ></div>
                        </div>
                      </div>

                      <div className="flex justify-between items-center p-3 bg-slate-50 border border-slate-100 rounded-xl text-xs">
                        <div>
                          <p className="text-slate-450 leading-tight">الأرباح التشغيلية الصافية</p>
                          <p className="font-extrabold text-slate-800 font-mono mt-1 text-sm">{stats.balance} {settings.currency}</p>
                        </div>
                        <span className="text-emerald-600 bg-emerald-50 py-1 px-2 rounded-lg font-bold">12%+ متقدم</span>
                      </div>
                    </div>
                  </div>
                </div>

              </div>
            </div>
          )}

          {/* SCREEN: GUESTS AND BOOKINGS LIST */}
          {currentScreen === 'BOOKINGS' && (
            <div className="bg-white rounded-3xl p-6 border border-slate-150 shadow-sm space-y-6">
              <div className="flex justify-between items-center flex-wrap gap-4 pb-4 border-b border-slate-150">
                <div>
                  <h2 className="text-xl font-extrabold text-slate-800">إدارة الحجوزات وملفات النزلاء</h2>
                  <p className="text-xs text-slate-400 mt-0.5">قائمة بجميع الإقامات النشطة والسابقة مع خيارات الدفع والفلترة والمغادرة</p>
                </div>

                <div className="flex gap-2 w-full md:w-auto">
                  <div className="relative flex-1 md:w-64">
                    <Search className="absolute right-3 top-2.5 text-slate-400" size={16} />
                    <input 
                      type="text" 
                      placeholder="البحث باسم النزيل أو رقم الهوية..."
                      value={bookingSearchQuery}
                      onChange={(e) => setBookingSearchQuery(e.target.value)}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl pr-9 pl-4 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 text-right font-semibold"
                    />
                  </div>

                  <select 
                    value={bookingStatusFilter}
                    onChange={(e) => setBookingStatusFilter(e.target.value as any)}
                    className="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs focus:outline-none font-bold"
                  >
                    <option value="ALL">كل الحالات</option>
                    <option value="ACTIVE">النشطة (مسكون)</option>
                    <option value="COMPLETED">الممنهجة والسابقة</option>
                  </select>
                </div>
              </div>

              {/* Transactions list Table representation */}
              <div className="overflow-x-auto">
                <table className="w-full text-right text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-extrabold uppercase border-b border-slate-150">
                    <tr>
                      <th className="p-3">رقم الغرفة</th>
                      <th className="p-3">اسم النزيل</th>
                      <th className="p-3">رقم الهوية / نوعها</th>
                      <th className="p-3">عدد الليالي</th>
                      <th className="p-3">تاريخ الدخول</th>
                      <th className="p-3">المبلغ المدفوع</th>
                      <th className="p-3">المتبقي</th>
                      <th className="p-3">الحالة</th>
                      <th className="p-3 text-center">خيارات</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium">
                    {bookings
                      .filter(b => {
                        const matchStatus = bookingStatusFilter === 'ALL' || b.status === bookingStatusFilter;
                        const matchSearch = !bookingSearchQuery.trim() || 
                          b.guestName.toLowerCase().includes(bookingSearchQuery.toLowerCase()) ||
                          b.idNumber.toLowerCase().includes(bookingSearchQuery.toLowerCase());
                        return matchStatus && matchSearch;
                      })
                      .map(b => (
                        <tr key={b.id} className="hover:bg-slate-50/50">
                          <td className="p-3">
                            <span className="font-extrabold text-slate-800 font-mono text-sm">{b.roomNumber}</span>
                            <span className="text-[10px] text-slate-400 block">{b.roomType}</span>
                          </td>
                          <td className="p-3 font-extrabold text-slate-800">{b.guestName}</td>
                          <td className="p-3">
                            <p className="font-extrabold text-slate-700 font-mono">{b.idNumber}</p>
                            <span className="text-[10px] text-slate-400">{b.idType}</span>
                          </td>
                          <td className="p-3 font-semibold">
                            {b.nightsCount === -1 ? "استراحة قصيرة" : `${b.nightsCount} ليلة`}
                          </td>
                          <td className="p-3 font-mono text-[11px] text-slate-500">{b.bookingDate}</td>
                          <td className="p-3 font-bold text-emerald-600">{b.amountPaid} {settings.currency}</td>
                          <td className="p-3 font-bold text-red-600">{b.amountRemaining} {settings.currency}</td>
                          <td className="p-3">
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              b.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-slate-100 text-slate-500'
                            }`}>
                              {b.status === 'ACTIVE' ? 'نشط / داخل الفندق' : 'مغادر / منتهي'}
                            </span>
                          </td>
                          <td className="p-3 text-center">
                            <div className="flex gap-1.5 justify-center">
                              {b.status === 'ACTIVE' && (
                                <button 
                                  onClick={() => {
                                    const rm = rooms.find(r => r.id === b.roomId);
                                    if (rm) {
                                      setSelectedRoomDetails(rm);
                                      setShowRoomDetailsModal(true);
                                    }
                                  }}
                                  className="bg-blue-600 text-white hover:bg-blue-700 px-2.5 py-1.5 rounded-lg text-[10px] font-bold"
                                >
                                  إنهاء ومغادرة
                                </button>
                              )}
                              <button 
                                onClick={() => {
                                  alert(`إصدار وطباعة فاتورة حجز للنزيل: ${b.guestName} \n\nالغرفة: ${b.roomNumber} \nنوع السكن: ${b.roomType} \nإجمالي المدفوعات: ${b.amountPaid} ${settings.currency}`);
                                }}
                                className="bg-slate-100 text-slate-600 hover:bg-slate-200 p-1.5 rounded-lg"
                                title="طباعة الفاتورة"
                              >
                                <Printer size={13} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* SCREEN: FINANCIALS AND LEDGER */}
          {currentScreen === 'FINANCIALS' && (
            <div className="space-y-6">
              {/* Financial calculations preview cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-emerald-50/50 border border-emerald-200 p-6 rounded-3xl shadow-sm text-right">
                  <h4 className="text-slate-400 text-xs font-bold mb-1">صافي الإيرادات وتحصيلات الحجز</h4>
                  <p className="text-3xl font-black text-emerald-600">+{stats.totalRevenue} {settings.currency}</p>
                  <span className="text-[10px] text-emerald-500 font-semibold block mt-1">تتضمن مبيعات الإقامة وخدمات الغرف المباشرة</span>
                </div>

                <div className="bg-red-50/50 border border-red-200 p-6 rounded-3xl shadow-sm text-right">
                  <h4 className="text-slate-400 text-xs font-bold mb-1">إجمالي المصروفات والنثريات</h4>
                  <p className="text-3xl font-black text-red-600">-{stats.totalExpense} {settings.currency}</p>
                  <span className="text-[10px] text-red-500 font-semibold block mt-1">تتضمن الصيانة والفواتير وملاك العقار والشراشف</span>
                </div>

                <div className="bg-white border border-slate-100 p-6 rounded-3xl shadow-sm text-right">
                  <h4 className="text-slate-400 text-xs font-bold mb-1">الرصيد الصافي الفعلي بالخزينة</h4>
                  <p className={`text-3xl font-black ${stats.balance >= 0 ? 'text-blue-600' : 'text-red-650'}`}>{stats.balance} {settings.currency}</p>
                  <span className="text-[10px] text-slate-400 font-semibold block mt-1">متطابق مع الرصيد المسجل تحت الدرج</span>
                </div>
              </div>

              {/* Transactions grid */}
              <div className="bg-white rounded-3xl p-6 border border-slate-150 shadow-sm space-y-6">
                <div className="flex justify-between items-center flex-wrap gap-4 pb-4 border-b border-slate-150">
                  <div>
                    <h2 className="text-xl font-extrabold text-slate-800">الحسابات والعمليات التفصيلية بالدفتر</h2>
                    <p className="text-xs text-slate-400 mt-0.5">تسجيل الدخل والمصاريف التشغيلية ومتابعة المحاسبة الإدارية</p>
                  </div>

                  <div className="flex gap-2 flex-wrap md:flex-nowrap w-full md:w-auto">
                    <input 
                      type="text" 
                      placeholder="البحث بالبيان والوصف..."
                      value={financialSearchQuery}
                      onChange={(e) => setFinancialSearchQuery(e.target.value)}
                      className="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 text-right font-semibold flex-1 md:w-48"
                    />

                    <select 
                      value={financialTypeFilter}
                      onChange={(e) => setFinancialTypeFilter(e.target.value as any)}
                      className="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs focus:outline-none font-bold"
                    >
                      <option value="ALL">الكل</option>
                      <option value="INCOME">مداخيل (إيرادات)</option>
                      <option value="EXPENSE">مصاريف (نفقات)</option>
                    </select>

                    <button 
                      onClick={() => setShowAddFinancialModal(true)}
                      className="bg-slate-900 hover:bg-slate-800 text-white px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1 shrink-0"
                    >
                      <Plus size={14} />
                      تسجيل حركة مالية
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-right text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-extrabold uppercase border-b border-slate-150">
                      <tr>
                        <th className="p-3">رقم الحركة</th>
                        <th className="p-3">البند / التصنيف</th>
                        <th className="p-3">البيان والوصف</th>
                        <th className="p-3">المبلغ</th>
                        <th className="p-3">التاريخ</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                      {transactions
                        .filter(t => {
                          const matchType = financialTypeFilter === 'ALL' || t.type === financialTypeFilter;
                          const matchSearch = !financialSearchQuery.trim() || t.description.toLowerCase().includes(financialSearchQuery.toLowerCase()) || t.category.toLowerCase().includes(financialSearchQuery.toLowerCase());
                          return matchType && matchSearch;
                        })
                        .reverse()
                        .map(t => (
                          <tr key={t.id} className="hover:bg-slate-50/50">
                            <td className="p-3 font-mono font-bold text-slate-400">#{t.id}</td>
                            <td className="p-3 font-extrabold">{t.category}</td>
                            <td className="p-3 font-semibold text-slate-800">{t.description}</td>
                            <td className={`p-3 font-black text-sm ${t.type === 'INCOME' ? 'text-emerald-600' : 'text-red-500'}`}>
                              {t.type === 'INCOME' ? '+' : '-'}{t.amount} {settings.currency}
                            </td>
                            <td className="p-3 font-mono text-slate-500">{t.date}</td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* SCREEN: REPORTS AND CHARTS */}
          {currentScreen === 'REPORTS' && (
            <div className="space-y-6">
              <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm">
                <h2 className="text-xl font-extrabold text-slate-800">تقارير الإشغال والإيرادات المصورة</h2>
                <p className="text-sm text-slate-400 mt-1">تتبع التدفقات والأرباح مقارنة بالأشهر الماضية مباشرة</p>
              </div>

              {/* Graphic charts panels layout */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                
                {/* Visual Chart 1: Financial Area progression */}
                <div className="bg-white p-6 rounded-3xl border border-slate-100 shadow-xs">
                  <h3 className="font-bold text-sm text-slate-700 mb-4 text-right">رسم بياني للإيرادات المتداولة</h3>
                  
                  <div className="h-64 font-bold text-xs" dir="ltr">
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart
                        data={[
                          { name: 'الأسبوع 1', الدخل: 5400, المصاريف: 1500 },
                          { name: 'الأسبوع 2', الدخل: 7200, المصاريف: 2200 },
                          { name: 'الأسبوع 3', الدخل: 9800, المصاريف: 3400 },
                          { name: 'الأسبوع الحالي', الدخل: stats.totalRevenue, المصاريف: stats.totalExpense }
                        ]}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Area type="monotone" dataKey="الدخل" stroke="#2563EB" fillOpacity={0.1} fill="#2563EB" strokeWidth={3} />
                        <Area type="monotone" dataKey="المصاريف" stroke="#EF4444" fillOpacity={0.05} fill="#EF4444" strokeWidth={2} />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                {/* Visual Chart 2: Popular rooms distribution bar graph */}
                <div className="bg-white p-6 rounded-3xl border border-slate-100 shadow-xs">
                  <h3 className="font-bold text-sm text-slate-700 mb-4 text-right">مقارنة مبيعات ومستوى الإشغال للغرف</h3>

                  <div className="h-64 font-bold text-xs" dir="ltr">
                    <ResponsiveContainer width="100%" height="100%">
                      <RechartsBarChart
                        data={[
                          { name: 'غرفة مفردة', السعر: 150, أشغال: rooms.filter(r => r.roomType === 'غرفة مفردة' && r.status === 'RESERVED').length },
                          { name: 'غرفة مزدوجة', السعر: 250, أشغال: rooms.filter(r => r.roomType === 'غرفة مزدوجة' && r.status === 'RESERVED').length },
                          { name: 'أستوديو فاخر', السعر: 350, أشغال: rooms.filter(r => r.roomType === 'أستوديو فاخر' && r.status === 'RESERVED').length },
                          { name: 'شقة ملكي / جناح', السعر: 800, أشغال: rooms.filter(r => (r.roomType.includes('شقة') || r.roomType.includes('جناح')) && r.status === 'RESERVED').length },
                        ]}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="أشغال" fill="#3B82F6" radius={[4, 4, 0, 0]} name="الوحدات المؤجرة حالياً" />
                      </RechartsBarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

              </div>
            </div>
          )}

          {/* SCREEN: STAFF AND PERMISSIONS */}
          {currentScreen === 'USERS' && (
            <div className="bg-white rounded-3xl p-6 border border-slate-150 shadow-sm space-y-6">
              <div className="flex justify-between items-center pb-4 border-b border-slate-150">
                <div>
                  <h2 className="text-xl font-extrabold text-slate-800">إدارة الطاقم وصلاحيات الموظفين بالاستقبال</h2>
                  <p className="text-xs text-slate-400 mt-0.5">تسجيل حسابات الموظفين وصلاحياتهم وتعديل كلمات المرور</p>
                </div>

                {currentUser.role === 'ADMIN' && (
                  <button 
                    onClick={() => setShowAddUserModal(true)}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1 shadow-md shadow-blue-500/20"
                  >
                    <UserPlus size={14} />
                    موظف جديد
                  </button>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {users.map(u => (
                  <div key={u.id} className="bg-slate-50 border border-slate-200 rounded-2xl p-5 flex flex-col justify-between h-40">
                    <div>
                      <div className="flex justify-between items-start">
                        <span className="w-10 h-10 bg-slate-200 rounded-xl flex items-center justify-center text-slate-700 font-bold">👤</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${u.role === 'ADMIN' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600'}`}>
                          {u.role === 'ADMIN' ? 'مدير نظام كامل الصلاحيات' : 'موظف استقبال مالي'}
                        </span>
                      </div>
                      <h4 className="font-extrabold text-base text-slate-800 mt-3">{u.fullname}</h4>
                      <p className="text-xs text-slate-400 mt-1 font-mono">اسم المستخدم: @{u.username}</p>
                    </div>

                    {currentUser.role === 'ADMIN' && u.id !== currentUser.id && (
                      <div className="mt-4 pt-3 border-t border-slate-200/60 flex justify-end">
                        <button 
                          onClick={() => handleRemoveStaff(u.id)}
                          className="text-red-500 hover:text-red-700 text-xs font-bold flex items-center gap-1"
                        >
                          <Trash2 size={13} />
                          حذف الحساب
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* SCREEN: APP SETTINGS */}
          {currentScreen === 'SETTINGS' && (
            <div className="bg-white rounded-3xl p-6 border border-slate-150 shadow-sm max-w-2xl mx-auto space-y-6">
              <div className="pb-4 border-b border-slate-150">
                <h2 className="text-xl font-extrabold text-slate-800">الإعدادات العامة للنظام الفندقي</h2>
                <p className="text-xs text-slate-400 mt-0.5">ضبط معلومات الفندق والتعاملات وساعات المغادرة والعمل على السكن</p>
              </div>

              <form onSubmit={handleSaveSettings} className="space-y-5 text-sm font-semibold">
                <div>
                  <label className="block text-slate-500 font-bold mb-1.5">اسم الفندق / المنشأة السكنية</label>
                  <input 
                    type="text" 
                    value={editHotelName}
                    onChange={(e) => setEditHotelName(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 font-extrabold text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-slate-500 font-bold mb-1.5">رمز العملة النقدية</label>
                    <input 
                      type="text" 
                      value={editCurrency}
                      onChange={(e) => setEditCurrency(e.target.value)}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-bold"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-slate-500 font-bold mb-1.5">ساعة تسجيل المغادرة (Checkout)</label>
                    <input 
                      type="text" 
                      value={editCheckoutHour}
                      onChange={(e) => setEditCheckoutHour(e.target.value)}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-mono"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-slate-500 font-bold mb-1.5">ساعات الاستراحة القصيرة الافتراضية (Rest Hours/استراحة)</label>
                  <input 
                    type="text" 
                    value={editRestHours}
                    onChange={(e) => setEditRestHours(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-mono"
                    required
                  />
                  <span className="text-[10px] text-slate-400 block mt-1">تستخدم عند حجز الغرف لفروق زمنية عادية بنظام الاستراحة</span>
                </div>

                <div className="pt-4 border-t border-slate-100 flex justify-end">
                  <button 
                    type="submit"
                    className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-xl font-bold transition-all shadow-md text-xs"
                  >
                    حفظ كل الإعدادات
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </main>

      {/* MODAL: ADD FLOOR */}
      {showAddFloorModal && (
        <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50 animate-fade-in text-sm font-bold">
          <div className="bg-white rounded-3xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-extrabold text-base text-slate-800">إضافة طابق فندقي جديد</h3>
              <button onClick={() => setShowAddFloorModal(false)} className="text-slate-400 hover:text-slate-600"><X size={18} /></button>
            </div>
            
            <form onSubmit={handleAddFloor} className="space-y-4">
              <div>
                <label className="block text-slate-500 mb-1.5 font-semibold">اسم الطابق</label>
                <input 
                  type="text" 
                  placeholder="مثال: الطابق الثالث" 
                  value={newFloorName}
                  onChange={(e) => setNewFloorName(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-bold text-right"
                  required
                />
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <button type="button" onClick={() => setShowAddFloorModal(false)} className="bg-slate-150 text-slate-500 px-4 py-2 rounded-xl text-xs">إلغاء</button>
                <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded-xl text-xs shadow-md">إضافة وحفظ</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ADD ROOM */}
      {showAddRoomModal && (
        <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50 animate-fade-in text-sm font-bold">
          <div className="bg-white rounded-3xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-extrabold text-base text-slate-800">إضافة غرفة أو شقة جديدة</h3>
              <button onClick={() => setShowAddRoomModal(false)} className="text-slate-400 hover:text-slate-600"><X size={18} /></button>
            </div>

            <form onSubmit={handleAddRoom} className="space-y-4 text-sm font-semibold">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-slate-500 mb-1.5 font-semibold">رقم الغرفة</label>
                  <input 
                    type="text" 
                    placeholder="مثال: 301" 
                    value={newRoomNumber}
                    onChange={(e) => setNewRoomNumber(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-slate-500 mb-1.5">اختر الطابق السكني</label>
                  <select 
                    value={newRoomFloorId}
                    onChange={(e) => setNewRoomFloorId(Number(e.target.value))}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none"
                  >
                    {floors.map(f => <option key={f.id} value={f.id}>{f.name}</option>)}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-slate-500 mb-1.5">نوع السكن / الوحدة</label>
                  <select 
                    value={newRoomType}
                    onChange={(e) => setNewRoomType(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none"
                  >
                    <option value="غرفة مفردة">غرفة مفردة</option>
                    <option value="غرفة مزدوجة">غرفة مزدوجة</option>
                    <option value="أستوديو فاخر">أستوديو فاخر</option>
                    <option value="شقة غرفتين وصالة">شقة غرفتين وصالة</option>
                    <option value="جناح ملكي">جناح ملكي</option>
                    <option value="شقة رئاسية ثلاث غرف">شقة رئاسية ثلاث غرف</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-500 mb-1.5 font-semibold">سعر الليلة السكنية</label>
                  <input 
                    type="number" 
                    placeholder="مثال: 450" 
                    value={newRoomPrice}
                    onChange={(e) => setNewRoomPrice(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-mono"
                    required
                  />
                </div>
              </div>

              <div className="flex gap-2 justify-end pt-2 border-t border-slate-100">
                <button type="button" onClick={() => setShowAddRoomModal(false)} className="bg-slate-150 text-slate-500 px-4 py-2 rounded-xl text-xs">إلغاء</button>
                <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded-xl text-xs shadow-md">إضافة وحفظ</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: NEW BOOKING (Check-in Form) */}
      {showBookingModal && bookingRoom && (
        <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50 overflow-y-auto pt-10 pb-10">
          <div className="bg-white rounded-3xl w-full max-w-2xl p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-extrabold text-base text-slate-800">إنشاء حجز وتسجيل دخول نزيل</h3>
                <p className="text-xs text-slate-400 mt-0.5">الغرفة المختارة: {bookingRoom.roomNumber} ({bookingRoom.roomType})</p>
              </div>
              <button onClick={() => setShowBookingModal(false)} className="text-slate-400 hover:text-slate-600"><X size={18} /></button>
            </div>

            <form onSubmit={handleCreateBooking} className="space-y-4 text-xs font-bold">
              {/* Main Fields */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-slate-500 mb-1 font-semibold">اسم النزيل الكامل (كما هو بالهوية)</label>
                  <input 
                    type="text" 
                    value={bookingGuestName}
                    onChange={(e) => setBookingGuestName(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
                    required
                  />
                </div>

                <div>
                  <label className="block text-slate-500 mb-1">رقم الهاتف الجوال</label>
                  <input 
                    type="text" 
                    value={bookingPhone}
                    onChange={(e) => setBookingPhone(e.target.value)}
                    placeholder="مثال: 0555123456"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-mono"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-slate-500 mb-1">نوع إثبات المعرفة</label>
                  <select 
                    value={bookingIdType}
                    onChange={(e) => setBookingIdType(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none font-bold"
                  >
                    <option value="هوية وطنية">🇸🇦 هوية وطنية</option>
                    <option value="جواز سفر">✈️ جواز سفر</option>
                    <option value="هوية مقيم">🪪 هوية مقيم (إقامة)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-500 mb-1">رقم الإثبات / الهوية</label>
                  <input 
                    type="text" 
                    value={bookingIdNumber}
                    onChange={(e) => setBookingIdNumber(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-mono"
                    required
                  />
                </div>

                <div>
                  <label className="block text-slate-500 mb-1">الجنسية</label>
                  <input 
                    type="text" 
                    value={bookingNationality}
                    onChange={(e) => setBookingNationality(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
                    required
                  />
                </div>
              </div>

              {/* Staying logic */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 border-t border-slate-100 pt-3">
                <div>
                  <label className="block text-slate-500 mb-1">نوع ومدة الإقامة</label>
                  <select 
                    value={bookingNights}
                    onChange={(e) => setBookingNights(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none font-bold text-slate-800"
                  >
                    <option value="-1">استراحة قصيرة ({settings.rest_duration_hours} ساعات)</option>
                    <option value="1">ليلة واحدة (١)</option>
                    <option value="2">ليلتين (٢)</option>
                    <option value="3">٣ ليالي</option>
                    <option value="4">٤ ليالي</option>
                    <option value="7">أسبوع كامل (٧) ليال</option>
                    <option value="14">أسبوعين (١٤) ليلة</option>
                    <option value="30">شهر كامل (٣٠) ليلة</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-500 mb-1">السعر المعتمد لليلة</label>
                  <input 
                    type="number" 
                    value={bookingPrice}
                    onChange={(e) => setBookingPrice(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 font-mono"
                  />
                </div>

                <div>
                  <label className="block text-slate-500 mb-1 font-extrabold text-blue-600">المبلغ المدفوع حالياً بالنقد</label>
                  <input 
                    type="number" 
                    value={bookingPaid}
                    onChange={(e) => setBookingPaid(e.target.value)}
                    className="w-full bg-blue-50/50 border-2 border-blue-200 text-blue-800 font-extrabold rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 font-mono text-sm"
                  />
                </div>
              </div>

              {/* Companions Sub-section */}
              <div className="border-t border-slate-100 pt-3">
                <h4 className="font-extrabold text-slate-700 mb-2">تسجيل المرافقين مع النزيل (متاح {tempCompanions.length})</h4>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-2">
                  <input 
                    type="text" 
                    placeholder="اسم المرافق" 
                    value={newCompName}
                    onChange={(e) => setNewCompName(e.target.value)}
                    className="bg-slate-50 border border-slate-200 rounded-lg px-2 py-2 text-[11px]"
                  />
                  <select 
                    value={newCompIdType}
                    onChange={(e) => setNewCompIdType(e.target.value)}
                    className="bg-slate-50 border border-slate-200 rounded-lg px-2 py-2 text-[11px]"
                  >
                    <option value="هوية وطنية">هوية وطنية</option>
                    <option value="جواز سفر">جواز سفر</option>
                    <option value="هوية مقيم">هوية مقيم</option>
                  </select>
                  <input 
                    type="text" 
                    placeholder="رقم الإثبات"
                    value={newCompIdNum}
                    onChange={(e) => setNewCompIdNum(e.target.value)}
                    className="bg-slate-50 border border-slate-200 rounded-lg px-2 py-2 font-mono text-[11px]"
                  />
                  <button 
                    type="button" 
                    onClick={addCompanionToTempList}
                    className="bg-slate-900 text-white rounded-lg px-2 py-2 text-[11px] font-bold"
                  >
                    + إضافة مرافق
                  </button>
                </div>

                {tempCompanions.length > 0 && (
                  <div className="mt-3 bg-slate-50 p-2.5 rounded-xl space-y-1.5 border border-slate-150">
                    {tempCompanions.map((c, i) => (
                      <div key={i} className="flex justify-between items-center text-[10px] text-slate-700 bg-white p-1.5 rounded border border-slate-100">
                        <span>{c.name} - ({c.idType}: {c.idNumber})</span>
                        <button type="button" onClick={() => removeCompanionFromTempList(i)} className="text-red-500 hover:text-red-700 font-bold">إزالة</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Actions */}
              <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setShowBookingModal(false)} className="bg-slate-150 text-slate-500 px-4 py-2.5 rounded-xl text-xs font-bold">إلغاء</button>
                <button type="submit" className="bg-blue-600 text-white hover:bg-blue-700 px-6 py-2.5 rounded-xl text-xs font-bold shadow-lg shadow-blue-500/20">حفظ وتأكيد الحجز وتسكين النزيل 🔑</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ROOM STAY DETAILS & CHECKOUT */}
      {showRoomDetailsModal && selectedRoomDetails && (
        <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-3xl w-full max-w-md p-6 shadow-2xl space-y-4 text-sm font-bold">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-extrabold text-base text-slate-800">تفاصيل السكن والإقامة النشطة</h3>
                <p className="text-xs text-slate-400 mt-0.5">الغرفة السكنية {selectedRoomDetails.roomNumber}</p>
              </div>
              <button 
                onClick={() => {
                  setShowRoomDetailsModal(false);
                  setSelectedRoomDetails(null);
                }} 
                className="text-slate-400 hover:text-slate-600"
              >
                <X size={18} />
              </button>
            </div>

            {/* If Reserved show Active guest staying details */}
            {selectedRoomDetails.status === 'RESERVED' && (() => {
              const b = bookings.find(x => x.roomId === selectedRoomDetails.id && x.status === 'ACTIVE');
              if (!b) return <p className="text-sm text-slate-400 text-center">لا توجد بيانات حجز نشطة لهذه الغرفة.</p>;

              return (
                <div className="space-y-4">
                  <div className="bg-slate-50 p-4 rounded-2xl border border-slate-150 space-y-2.5 text-right text-xs">
                    <div>
                      <p className="text-[10px] text-slate-400">النزيل المستأجر</p>
                      <p className="font-extrabold text-slate-800 text-sm">{b.guestName}</p>
                    </div>

                    <div className="grid grid-cols-2 gap-3 pt-1 border-t border-slate-200/50">
                      <div>
                        <p className="text-[10px] text-slate-400">رقم الاتصال</p>
                        <p className="font-mono text-slate-705 text-xs">{b.phone}</p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400">رقم ومستند الهوية</p>
                        <p className="font-mono text-slate-705 text-xs">{b.idNumber} ({b.idType})</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-3 pt-1 border-t border-slate-200/50">
                      <div>
                        <p className="text-[10px] text-slate-400">جنسية النزيل</p>
                        <p className="text-slate-705 text-xs">{b.nationality || "سعودي"}</p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400">زمن وتوقيت الدخول</p>
                        <p className="font-mono text-slate-705 text-[10px]">{new Date(b.checkInTime).toLocaleString('ar-SA', { hour12: true })}</p>
                      </div>
                    </div>

                    {b.companions.length > 0 && (
                      <div className="pt-2 border-t border-slate-200/50">
                        <p className="text-[10px] text-slate-400 mb-1">المرافقون المصاحبون ({b.companions.length})</p>
                        <div className="space-y-1">
                          {b.companions.map((c, i) => (
                            <p key={i} className="text-[10px] text-slate-600 font-semibold">• {c.name} - ({c.idType}: {c.idNumber})</p>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="space-y-2 pt-2">
                    <div className="flex justify-between items-center text-xs text-slate-600 font-bold">
                      <span>إجمالي تكلفة الإقامة المحسوبة</span>
                      <span className="font-mono">{b.nightsCount === -1 ? "استراحة قصيرة" : `${b.nightsCount} ليلة`}</span>
                    </div>

                    {b.amountRemaining > 0 ? (
                      <div className="bg-red-50 border border-red-200 rounded-xl p-3 flex justify-between items-center text-xs text-red-800">
                        <span>المبلغ المستحق والغير مسدد متبقياً:</span>
                        <span className="font-black text-sm">{b.amountRemaining} {settings.currency}</span>
                      </div>
                    ) : (
                      <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3 flex justify-between items-center text-xs text-emerald-800">
                        <span>مخالصة السكن:</span>
                        <span className="font-bold">الحساب مسدد بالكامل (تفويض دفع)</span>
                      </div>
                    )}
                  </div>

                  <div className="flex gap-2 justify-end pt-3 border-t border-slate-100 text-xs">
                    <button 
                      type="button" 
                      onClick={() => {
                        setShowRoomDetailsModal(false);
                        setSelectedRoomDetails(null);
                      }} 
                      className="bg-slate-150 text-slate-500 px-4 py-2.5 rounded-xl font-bold"
                    >
                      إغلاق
                    </button>

                    {/* Submit checkouts! */}
                    {b.amountRemaining > 0 ? (
                      <div className="flex gap-2">
                        <button 
                          type="button" 
                          onClick={() => handleCheckoutRoom(b.id, true)}
                          className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2.5 rounded-xl font-bold"
                        >
                          إنهاء وتسوية كامل المبلغ ({b.amountRemaining} {settings.currency}) 💰
                        </button>
                      </div>
                    ) : (
                      <button 
                        type="button" 
                        onClick={() => handleCheckoutRoom(b.id, false)}
                        className="bg-red-600 hover:bg-red-700 text-white px-4 py-2.5 rounded-xl font-bold"
                      >
                        تأكيد تسليم المفاتيح وإنهاء الحجز مغادرة 🚪
                      </button>
                    )}
                  </div>
                </div>
              );
            })()}

            {/* If Cleaning state allowed mark Clean ready state */}
            {selectedRoomDetails.status === 'CLEANING' && (
              <div className="space-y-4">
                <p className="text-xs text-slate-500 text-right leading-relaxed">هذه الشقة سكنية أو الغرفة تم إخلاؤها ومغادرة الضيف مؤخراً. هي الآن قيد التنظيف والتعقيم وتغيير الشراشف والمستلزمات لتصبح جاهزة للتأجير للنزيل القادم.</p>
                <button 
                  onClick={() => {
                    markRoomClean(selectedRoomDetails.id);
                    setShowRoomDetailsModal(false);
                    setSelectedRoomDetails(null);
                  }}
                  className="w-full bg-emerald-600 hover:bg-emerald-700 text-white py-3 rounded-xl text-xs font-bold shadow-md shadow-emerald-500/15"
                >
                  تعيينها كـ &quot;متاحة ونظيفة&quot; للإيجار فوراً ⭐
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL: DIRECT INCOME/EXPENSE ADD AT GENERAL LEDGER */}
      {showAddFinancialModal && (
        <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-3xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-extrabold text-base text-slate-800">تسجيل حركة مالية جديدة بالخزينة</h3>
              <button onClick={() => setShowAddFinancialModal(false)} className="text-slate-400 hover:text-slate-650"><X size={18} /></button>
            </div>

            <form onSubmit={handleAddFinancial} className="space-y-4 text-xs font-bold text-right">
              <div>
                <label className="block text-slate-550 mb-1.5">نوع المعاملة المالية</label>
                <div className="grid grid-cols-2 gap-2">
                  <button 
                    type="button"
                    onClick={() => setFinType('INCOME')}
                    className={`py-2 rounded-xl text-center border-2 font-bold ${finType === 'INCOME' ? 'bg-emerald-50 text-emerald-800 border-emerald-500' : 'bg-slate-50 border-slate-205 text-slate-500'}`}
                  >
                    دخل / إيرادات وإيداع
                  </button>
                  <button 
                    type="button"
                    onClick={() => setFinType('EXPENSE')}
                    className={`py-2 rounded-xl text-center border-2 font-bold ${finType === 'EXPENSE' ? 'bg-red-50 text-red-800 border-red-500' : 'bg-slate-50 border-slate-205 text-slate-500'}`}
                  >
                    مصروف / نثريات
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-slate-500 mb-1">المبلغ المالي</label>
                <input 
                  type="number" 
                  value={finAmount}
                  onChange={(e) => setFinAmount(e.target.value)}
                  placeholder={`أدخل القيمة بـ ${settings.currency}`}
                  className="w-full bg-slate-50 border border-slate-205 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-mono text-right"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-500 mb-1">التصنيف أو البند</label>
                <input 
                  type="text" 
                  value={finCategory}
                  onChange={(e) => setFinCategory(e.target.value)}
                  placeholder="مثال: فاتورة كهرباء، صيانة مغسلة، خدمات غرف..."
                  className="w-full bg-slate-50 border border-slate-205 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-500 mb-1">البيان والوصف التفصيلي</label>
                <textarea 
                  value={finDescription}
                  onChange={(e) => setFinDescription(e.target.value)}
                  placeholder="اكتب نبذة توضيحية لدفتر الحسابات..."
                  rows={2}
                  className="w-full bg-slate-50 border border-slate-205 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-1 focus:ring-blue-500 text-sm font-semibold"
                />
              </div>

              <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setShowAddFinancialModal(false)} className="bg-slate-150 text-slate-500 px-4 py-2.5 rounded-xl text-xs">إلغاء</button>
                <button type="submit" className="bg-slate-900 text-white hover:bg-slate-800 px-5 py-2.5 rounded-xl text-xs shadow-md">إضافة وحفظ بالخزينة 💰</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ADD STAFF */}
      {showAddUserModal && (
        <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-3xl w-full max-w-sm p-6 shadow-2xl space-y-4 text-sm font-bold">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-extrabold text-base text-slate-800">إضافة موظف استقبال جديد</h3>
              <button onClick={() => setShowAddUserModal(false)} className="text-slate-400 hover:text-slate-650"><X size={18} /></button>
            </div>

            <form onSubmit={handleAddStaff} className="space-y-4 text-xs font-bold text-right">
              <div>
                <label className="block text-slate-500 mb-1 leading-tight">الاسم الكامل للموظف</label>
                <input 
                  type="text" 
                  value={newStaffFullname}
                  onChange={(e) => setNewStaffFullname(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none text-sm"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-500 mb-1 leading-tight">رتبة الصلاحية</label>
                  <select 
                    value={newStaffRole}
                    onChange={(e) => setNewStaffRole(e.target.value as any)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none"
                  >
                    <option value="RECEPTIONIST">موظف استقبال</option>
                    <option value="ADMIN">مدير نظام</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-500 mb-1 leading-tight">اسم المستخدم للدخول</label>
                  <input 
                    type="text" 
                    value={newStaffUsername}
                    onChange={(e) => setNewStaffUsername(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none text-sm font-mono"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-500 mb-1 leading-tight">كلمة المرور الافتراضية</label>
                <input 
                  type="password" 
                  value={newStaffPassword}
                  onChange={(e) => setNewStaffPassword(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 focus:outline-none text-sm font-mono"
                  required
                />
              </div>

              <div className="flex gap-2 justify-end pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setShowAddUserModal(false)} className="bg-slate-150 text-slate-500 px-4 py-2.5 rounded-xl text-xs">إلغاء</button>
                <button type="submit" className="bg-blue-600 text-white hover:bg-blue-700 px-5 py-2.5 rounded-xl text-xs shadow-md">إضافة موظف</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
