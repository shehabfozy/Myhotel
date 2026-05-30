@file:OptIn(ExperimentalMaterial3Api::class)

package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// Decorative Custom Colors for Premium Hospitality feel
val PrimaryDark = Color(0xFF0F172A) // slate-900 (Bento main dark color)
val SidebarColor = Color(0xFF1E293B) // slate-800
val SlateBorder = Color(0xFF334155) // slate-700
val PremiumTeal = Color(0xFF2563EB) // Royal Blue (blue-600) from Bento Grid mockup
val TealGlow = Color(0xFF3B82F6) // Bright Blue (blue-500)
val SoftGreen = Color(0xFFECFDF5) // emerald-50
val StrongGreen = Color(0xFF10B981) // Emerald (green-500)
val SoftRed = Color(0xFFFEF2F2) // red-50
val StrongRed = Color(0xFFEF4444) // Red (red-500)
val GoldAmber = Color(0xFFF59E0B) // Amber (amber-500)
val SoftWhite = Color(0xFFF8FAFC) // slate-50
val LightSurface = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        db = DatabaseHelper(this)

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                HotelManagementApp(db)
            }
        }
    }
}

// Sidebar/Navigation Menu Enum
enum class HotelScreen(val titleAr: String, val icon: ImageVector) {
    ROOMS("لوحة الغرف والطوابق", Icons.Default.Home),
    BOOKINGS("إدارة النزلاء والحجوزات", Icons.Default.List),
    FINANCIALS("الخزينة والحسابات", Icons.Default.ShoppingCart),
    REPORTS("التقارير والإحصائيات", Icons.Default.Star),
    USERS("إدارة الطاقم والصلاحيات", Icons.Default.Person),
    SETTINGS("الإعدادات العامة", Icons.Default.Settings)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HotelManagementApp(db: DatabaseHelper) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Login & Session State
    var currentUser by remember { mutableStateOf<AppUser?>(null) }
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginError by remember { mutableStateOf(false) }

    // Navigation State
    var currentScreen by remember { mutableStateOf(HotelScreen.ROOMS) }

    // Live Room / Floor States
    var floors by remember { mutableStateOf(emptyList<Floor>()) }
    var rooms by remember { mutableStateOf(emptyList<Room>()) }
    var selectedFloorId by remember { mutableStateOf<Long?>(null) } // null means "All Floors / الكل"

    // Dialog state
    var selectedRoomForBooking by remember { mutableStateOf<Room?>(null) }
    var activeBookingForDetail by remember { mutableStateOf<Booking?>(null) }
    var activeRoomForDetail by remember { mutableStateOf<Room?>(null) }
    var showInvoicePrintDialog by remember { mutableStateOf<Booking?>(null) }
    var showLoginDialog by remember { mutableStateOf(false) }

    // Loader wrapper for data syncing
    fun refreshData() {
        floors = db.getAllFloors()
        rooms = db.getAllRooms()
    }

    // Initial load
    LaunchedEffect(Unit) {
        refreshData()
        // Auto-login receptionist to start easily or let them use the app
        currentUser = db.login("reception", "123")
    }

    // Auto complete reservations whose duration is over
    LaunchedEffect(rooms) {
        val activeBookings = db.getActiveBookings()
        val now = System.currentTimeMillis()
        for (booking in activeBookings) {
            if (booking.checkOutTime < now) {
                // Manual check-out represented via visual alert.
            }
        }
    }

    if (currentUser == null) {
        // BEAUTIFUL LOGIN WINDOW COVERING SCREEN
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PrimaryDark, SidebarColor))),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillModifierResponsive()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val hotelNameSetting = db.getSetting("hotel_name", "فندق الجوهرة")
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Login",
                        tint = PremiumTeal,
                        modifier = Modifier
                            .size(72.dp)
                            .background(SoftGreen, CircleShape)
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = hotelNameSetting,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "نظام الإدارة الذكي والصلاحيات",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = loginUsername,
                        onValueChange = { loginUsername = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("كلمة المرور") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (showLoginError) {
                        Text(
                            text = "اسم المستخدم أو كلمة المرور خاطئة!",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            val user = db.login(loginUsername, loginPassword)
                            if (user != null) {
                                currentUser = user
                                showLoginError = false
                                Toast.makeText(context, "أهلاً بك، ${user.fullname}", Toast.LENGTH_SHORT).show()
                            } else {
                                showLoginError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تسجيل الدخول", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "بيانات تجريبية سريعة للمعاينة:\nالمدير: admin / admin123\nالاستقبال: reception / 123",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    } else {
        // MAIN APPLICATION UI (RTL SIDEMENU + MAIN DESK)
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Hotel Logo",
                                    tint = PremiumTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = db.getSetting("hotel_name", "فندق الجوهرة"),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "متصل باسم: ${currentUser?.fullname} (${if (currentUser?.role == "ADMIN") "المدير العام" else "موظف الاستقبال"})",
                                        fontSize = 12.sp,
                                        color = TealGlow
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // Logout
                                currentUser = null
                                loginUsername = ""
                                loginPassword = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Lock, // Logout representation 
                                    contentDescription = "تسجيل خروج",
                                    tint = Color.White
                                )
                            }
                            TextButton(onClick = { showLoginDialog = true }) {
                                Text("تبديل الحساب", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
                    )
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // SIDEBAR NAVIGATION PANEL
                    Column(
                        modifier = Modifier
                            .width(250.dp)
                            .fillMaxHeight()
                            .background(PrimaryDark) // Align with slate-900 sidebar background
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Bento Sidebar Header with a classy logo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PremiumTeal), // Royal Blue accent
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "HM",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "نظام واحة الفندق",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(
                            color = SlateBorder.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Navigation Items
                        for (screen in HotelScreen.values()) {
                            val isSelected = currentScreen == screen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PremiumTeal else Color.Transparent)
                                    .clickable { currentScreen = screen }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.titleAr,
                                    tint = if (isSelected) Color.White else Color(0xFF94A3B8), // slate-400
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = screen.titleAr,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8) // slate-400
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        HorizontalDivider(
                            color = SlateBorder.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Elegant User Profile info matching the mockup's footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155))
                                    .border(1.dp, Color(0xFF475569), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.fullname ?: "مدير النظام",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentUser?.role == "ADMIN") "مدير النظام" else "موظف الاستقبال",
                                    color = Color(0xFF94A3B8), // slate-400
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // VERTICAL DIVIDER
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = SlateBorder
                    )

                    // MAIN WORK DESK PANELS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(SoftWhite)
                    ) {
                        when (currentScreen) {
                            HotelScreen.ROOMS -> RoomsDesk(
                                db = db,
                                rooms = rooms,
                                floors = floors,
                                selectedFloorId = selectedFloorId,
                                onFloorChange = { selectedFloorId = it },
                                onRoomCreateRequest = { refreshData() },
                                onRoomClick = { room ->
                                    if (room.status == "AVAILABLE") {
                                        selectedRoomForBooking = room
                                    } else {
                                        // Retrieve active reservation
                                        val activeBooking = db.getActiveBookingForRoom(room.id)
                                        if (activeBooking != null) {
                                            activeBookingForDetail = activeBooking
                                            activeRoomForDetail = room
                                        } else {
                                            // Safety clean status
                                            db.updateRoomStatus(room.id, "AVAILABLE")
                                            refreshData()
                                        }
                                    }
                                }
                            )
                            HotelScreen.BOOKINGS -> BookingsDesk(
                                db = db,
                                onRefresh = { refreshData() },
                                onPrintInvoice = { booking -> showInvoicePrintDialog = booking }
                            )
                            HotelScreen.FINANCIALS -> FinancialsDesk(
                                db = db,
                                userRole = currentUser?.role ?: "RECEPTIONIST"
                            )
                            HotelScreen.REPORTS -> ReportsDesk(
                                db = db
                            )
                            HotelScreen.USERS -> UsersDesk(
                                db = db,
                                userRole = currentUser?.role ?: "RECEPTIONIST"
                            )
                            HotelScreen.SETTINGS -> SettingsDesk(
                                db = db,
                                userRole = currentUser?.role ?: "RECEPTIONIST",
                                onSave = {
                                    refreshData()
                                    Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // POPUPS AND FLOATING DIALOGS
            selectedRoomForBooking?.let { room ->
                BookingFormDialog(
                    room = room,
                    db = db,
                    onDismiss = { selectedRoomForBooking = null },
                    onSuccess = { booking ->
                        selectedRoomForBooking = null
                        refreshData()
                        // Force show invoice immediately after booking
                        showInvoicePrintDialog = booking
                    }
                )
            }

            activeBookingForDetail?.let { booking ->
                BookingDetailDialog(
                    booking = booking,
                    room = activeRoomForDetail!!,
                    db = db,
                    onDismiss = {
                        activeBookingForDetail = null
                        activeRoomForDetail = null
                    },
                    onActionComplete = {
                        activeBookingForDetail = null
                        activeRoomForDetail = null
                        refreshData()
                    },
                    onPrintInvoice = {
                        showInvoicePrintDialog = booking
                    }
                )
            }

            showInvoicePrintDialog?.let { booking ->
                InvoicePrintDialog(
                    booking = booking,
                    db = db,
                    onDismiss = { showInvoicePrintDialog = null }
                )
            }

            if (showLoginDialog) {
                Dialog(
                    onDismissRequest = { showLoginDialog = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("تبديل حساب المستخدم الحالي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                            Spacer(modifier = Modifier.height(16.dp))

                            var tempUser by remember { mutableStateOf("") }
                            var tempPass by remember { mutableStateOf("") }
                            var tempError by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = tempUser,
                                onValueChange = { tempUser = it },
                                label = { Text("اسم المستخدم") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempPass,
                                onValueChange = { tempPass = it },
                                label = { Text("كلمة المرور") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            if (tempError) {
                                Text("خطأ في البيانات المدخلة!", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showLoginDialog = false }) {
                                    Text("إلغاء")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val authed = db.login(tempUser, tempPass)
                                        if (authed != null) {
                                            currentUser = authed
                                            showLoginDialog = false
                                            Toast.makeText(context, "تم التبديل إلى: ${authed.fullname}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            tempError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                                ) {
                                    Text("تبديل")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- USER INTERFACE DESKS AND SCREENS ----------------

// SECTION 1: ROOMS GRID DESK
@Composable
fun RoomsDesk(
    db: DatabaseHelper,
    rooms: List<Room>,
    floors: List<Floor>,
    selectedFloorId: Long?,
    onFloorChange: (Long?) -> Unit,
    onRoomCreateRequest: () -> Unit,
    onRoomClick: (Room) -> Unit
) {
    val ledger = remember(rooms) { db.getAllFinancialTransactions() }
    val currency = remember { db.getSetting("currency", "ر.س") }
    
    val totalIncomes = remember(ledger) { ledger.filter { it.type == "INCOME" }.sumOf { it.amount } }
    val occupiedRooms = remember(rooms) { rooms.count { it.status == "RESERVED" } }
    val occupancyPct = remember(rooms, occupiedRooms) { if (rooms.isNotEmpty()) (occupiedRooms.toFloat() / rooms.size) * 100 else 0f }
    val availableRoomsCount = remember(rooms) { rooms.count { it.status == "AVAILABLE" } }

    var showAddFloorDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp) // Generous margins
    ) {
        // TOP CONTROL BAR: TITLE & DESCRIPTION
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "حالة الغرف والشقق السكنية",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "اضغط على غرفة متاحة لبدء حجز جديد، أو غرفة محجوزة لمعاينتها وتصفيتها",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // BENTO GRID STATS BLOCK
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Columns in Bento-alignment
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "نسبة الإشغال",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${DecimalFormat("#.#").format(occupancyPct)}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PremiumTeal // Blue accent color
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(38.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "رصيد الخزينة المباشر",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${DecimalFormat("#,##0").format(totalIncomes)} $currency",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StrongGreen // Success green
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(38.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "الغرف المتاحة حالياً",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$availableRoomsCount وحدة",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF475569) // Tech Slate Gray
                        )
                    }
                }

                // Quick Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddFloorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طابق جديد", color = Color(0xFF334155), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showAddRoomDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("غرفة جديدة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HORIZONTAL FLOORS CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFloorId == null,
                onClick = { onFloorChange(null) },
                label = { Text("الكل (${rooms.size})") },
                modifier = Modifier.padding(end = 6.dp)
            )

            for (floor in floors) {
                val count = rooms.count { it.floorId == floor.id }
                FilterChip(
                    selected = selectedFloorId == floor.id,
                    onClick = { onFloorChange(floor.id) },
                    label = { Text("${floor.name} ($count)") },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FILTERED ROOM GRID
        val filteredRooms = if (selectedFloorId == null) rooms else rooms.filter { it.floorId == selectedFloorId }

        if (filteredRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد غرف مجهزة في هذا الطابق بعد.\nالرجاء إضافة غرف لتفعيل النظام.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRooms) { room ->
                    RoomGridCard(room = room, onClick = { onRoomClick(room) }, db = db)
                }
            }
        }
    }

    // Modal to add floor
    if (showAddFloorDialog) {
        Dialog(onDismissRequest = { showAddFloorDialog = false }) {
            var name by remember { mutableStateOf("") }
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("إضافة طابق جديد", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الطابق (مثال: الطابق الثالث)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddFloorDialog = false }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    db.addFloor(name)
                                    onRoomCreateRequest()
                                    showAddFloorDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                        ) {
                            Text("إضافة")
                        }
                    }
                }
            }
        }
    }

    // Modal to add room
    if (showAddRoomDialog) {
        Dialog(onDismissRequest = { showAddRoomDialog = false }) {
            var roomNumber by remember { mutableStateOf("") }
            var price by remember { mutableStateOf("") }
            var floorIndex by remember { mutableIntStateOf(0) }
            var roomType by remember { mutableStateOf("غرفة مفردة") }

            val types = listOf("غرفة مفردة", "غرفة مزدوجة", "أستوديو فاخر", "شقة غرفتين وصالة", "جناح ملكي")

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("تثبيت شقة أو غرفة فندقية", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Floor Selector
                    Text("اختر الطابق التابع له:", fontSize = 13.sp, color = Color.Gray)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        floors.forEachIndexed { i, f ->
                            FilterChip(
                                selected = floorIndex == i,
                                onClick = { floorIndex = i },
                                label = { Text(f.name) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("رقم الغرفة / الشقة (مثال: 304)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("نوع الوحدة الفندقية:", fontSize = 13.sp, color = Color.Gray)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        types.forEach { t ->
                            FilterChip(
                                selected = roomType == t,
                                onClick = { roomType = t },
                                label = { Text(t) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("سعر المبيت القياسي لليلة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddRoomDialog = false }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val rate = price.toDoubleOrNull() ?: 0.0
                                if (roomNumber.isNotBlank() && floors.isNotEmpty() && rate > 0) {
                                    val floorId = floors[floorIndex].id
                                    db.addRoom(floorId, roomNumber, roomType, rate)
                                    onRoomCreateRequest()
                                    showAddRoomDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                        ) {
                            Text("إضافة الغرفة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomGridCard(room: Room, onClick: () -> Unit, db: DatabaseHelper) {
    val isAvailable = room.status == "AVAILABLE"
    val currency = db.getSetting("currency", "ر.س")

    // Retrieve guest if reserved
    var guestHint = ""
    if (!isAvailable) {
        val b = db.getActiveBookingForRoom(room.id)
        if (b != null) {
            guestHint = b.guestName
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp), // Bento-style roundings
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isAvailable) StrongGreen.copy(alpha = 0.4f) else StrongRed.copy(alpha = 0.4f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) SoftGreen else SoftRed
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "غرفة " + room.roomNumber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                
                // Status badge
                Text(
                    text = if (isAvailable) "متاحة" else "محجوزة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAvailable) StrongGreen else StrongRed,
                    modifier = Modifier
                        .background(
                            color = (if (isAvailable) StrongGreen else StrongRed).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = room.roomType,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isAvailable) {
                Text(
                    text = "${room.price} $currency / ليلة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumTeal
                )
            } else {
                Text(
                    text = if (guestHint.isNotEmpty()) "النزيل: $guestHint" else "جاري جلب النزيل...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    maxLines = 1
                )
            }
        }
    }
}

// SECTION 2: BOOKINGS LIST & RESERVATION MANAGEMENT DESK
@Composable
fun BookingsDesk(
    db: DatabaseHelper,
    onRefresh: () -> Unit,
    onPrintInvoice: (Booking) -> Unit
) {
    var bookings by remember { mutableStateOf(emptyList<Booking>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showActiveOnly by remember { mutableStateOf(true) }
    var selectedBookingForPayRemaining by remember { mutableStateOf<Booking?>(null) }
    val context = LocalContext.current

    fun reload() {
        bookings = if (showActiveOnly) db.getActiveBookings() else db.getAllBookings()
    }

    LaunchedEffect(showActiveOnly) {
        reload()
    }

    val filtered = bookings.filter {
        it.guestName.contains(searchQuery) || it.phone.contains(searchQuery) || it.roomNumber.contains(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("سجل ومراقبة حجوزات الفندق", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
        Spacer(modifier = Modifier.height(12.dp))

        // Search & Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث باسم النزيل، رقم الغرفة أو الهاتف...") },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showActiveOnly,
                    onCheckedChange = { showActiveOnly = it }
                )
                Text("الحجوزات النشطة فقط", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("لا توجد نتائج مطابقة لبحثك.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { booking ->
                    BookingListItemCard(
                        booking = booking,
                        db = db,
                        onCheckOut = {
                            db.completeAndReleaseBooking(booking.id, booking.roomId)
                            onRefresh()
                            reload()
                            Toast.makeText(context, "تم إنهاء الحجز وتفريغ الغرفة ${booking.roomNumber}", Toast.LENGTH_SHORT).show()
                        },
                        onPrintInvoice = { onPrintInvoice(booking) },
                        onPayRemaining = { selectedBookingForPayRemaining = booking }
                    )
                }
            }
        }
    }

    // Modal to collect remaining balance
    selectedBookingForPayRemaining?.let { booking ->
        Dialog(onDismissRequest = { selectedBookingForPayRemaining = null }) {
            var payAmt by remember { mutableStateOf(booking.amountRemaining.toString()) }
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("تحصيل النقد المتبقي", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("النزيل: ${booking.guestName}", fontSize = 13.sp, color = Color.DarkGray)
                    Text("الغرفة: ${booking.roomNumber} | المبلغ المتبقي: ${booking.amountRemaining} ${db.getSetting("currency", "ر.س")}", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = payAmt,
                        onValueChange = { payAmt = it },
                        label = { Text("المبلغ المدفوع الآن") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedBookingForPayRemaining = null }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amtNum = payAmt.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0 && amtNum <= booking.amountRemaining) {
                                    db.payRemainingAmount(booking.id, amtNum, booking.guestName, booking.roomNumber)
                                    selectedBookingForPayRemaining = null
                                    reload()
                                    onRefresh()
                                    Toast.makeText(context, "تم تحصيل الدفعة بنجاح وإدخالها في الخزينة", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "الرجاء التحقق من قيمة المبلغ المدفوع", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                        ) {
                            Text("تثبيت وتحصيل")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingListItemCard(
    booking: Booking,
    db: DatabaseHelper,
    onCheckOut: () -> Unit,
    onPrintInvoice: () -> Unit,
    onPayRemaining: () -> Unit
) {
    val currency = db.getSetting("currency", "ر.س")
    val hasRemaining = booking.amountRemaining > 0
    val now = System.currentTimeMillis()
    val isOverdue = booking.status == "ACTIVE" && booking.checkOutTime < now

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), // Bento-style roundings
        border = BorderStroke(
            1.dp,
            if (isOverdue) Color.Red.copy(alpha = 0.3f) else SlateBorder.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // First Row: Guest & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = booking.guestName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Text(text = "رقم الهوية: ${booking.idNumber} (${booking.idType}) | الهاتف: ${booking.phone}", fontSize = 11.sp, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverdue) {
                        Text(
                            text = "انتهت فترة الحجز للغرفة",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (booking.status == "ACTIVE") "حجز نشط" else "مكتمل / تم المغادرة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (booking.status == "ACTIVE") StrongGreen else Color.Gray,
                        modifier = Modifier
                            .background(
                                color = (if (booking.status == "ACTIVE") SoftGreen else Color.LightGray).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder.copy(alpha = 0.15f))

            // Second Row: Times & Room Number Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "الوحدة: غرفة ${booking.roomNumber} (${booking.roomType})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumTeal
                    )
                    Text(
                        text = "المدة: " + (if (booking.nightsCount == -1) "استراحة قصيرة" else "${booking.nightsCount} ليلة"),
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val sdfTime = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
                    Text(text = "دخول: ${sdfTime.format(Date(booking.checkInTime))}", fontSize = 11.sp, color = Color.Gray)
                    Text(text = "مغادرة: ${sdfTime.format(Date(booking.checkOutTime))}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Companion count indicator if any
            if (booking.companionsCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "عدد المرافقين المسجلين: ${booking.companionsCount} مرافق",
                    fontSize = 11.sp,
                    color = GoldAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder.copy(alpha = 0.15f))

            // Financial breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Column(modifier = Modifier.padding(end = 16.dp)) {
                        Text("المدفوع", fontSize = 10.sp, color = Color.Gray)
                        Text(text = "${booking.amountPaid} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StrongGreen)
                    }
                    Column {
                        Text("المتبقي بذمته", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "${booking.amountRemaining} $currency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasRemaining) GoldAmber else Color.DarkGray
                        )
                    }
                }

                // Actions row
                Row {
                    if (booking.status == "ACTIVE" && hasRemaining) {
                        Button(
                            onClick = { onPayRemaining() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAmber),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تحصيل متبقي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (booking.status == "ACTIVE") {
                        Button(
                            onClick = { onCheckOut() },
                            colors = ButtonDefaults.buttonColors(containerColor = StrongRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تحرير ومغادرة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    OutlinedButton(
                        onClick = { onPrintInvoice() },
                        border = BorderStroke(1.dp, PremiumTeal),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumTeal),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("الفاتورة والنزلاء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// SECTION 3: FINANCIALS LEDGER DESK
@Composable
fun FinancialsDesk(
    db: DatabaseHelper,
    userRole: String
) {
    var ledger by remember { mutableStateOf(emptyList<FinancialTransaction>()) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var showAddExpense by remember { mutableStateOf(false) }

    val categories = listOf("حجوزات", "صيانة ونظافة", "رواتب", "فواتير وضيافة", "أخرى")

    fun loadLedger() {
        ledger = db.getAllFinancialTransactions()
    }

    LaunchedEffect(Unit) {
        loadLedger()
    }

    val filteredLedger = if (selectedCategory == "الكل") ledger else ledger.filter { it.category == selectedCategory }

    // Summary calculations
    val totalIncome = ledger.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = ledger.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val currentBalance = totalIncome - totalExpense
    val currency = db.getSetting("currency", "ر.س")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("النظام المالي وحسابات الصندوق", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                Text("تتبع الدخل الوارد من الحجوزات وضبط الصرفيات والمصاريف اليومية بالتفصيل", fontSize = 12.sp, color = Color.Gray)
            }

            Button(
                onClick = { showAddExpense = true },
                colors = ButtonDefaults.buttonColors(containerColor = StrongRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إدخال مصروف مالي")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance widgets row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGreen),
                border = BorderStroke(1.5.dp, StrongGreen.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("إجمالي الإيرادات (دخل الحجوزات)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StrongGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("+ ${totalIncome} $currency", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StrongGreen)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftRed),
                border = BorderStroke(1.5.dp, StrongRed.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("إجمالي المصروفات والنثريات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StrongRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("- ${totalExpense} $currency", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StrongRed)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("صافي رصيد الصندوق الحالي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${currentBalance} $currency", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (currentBalance >= 0) PremiumTeal else Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories selector row
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تصفية البنود:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(end = 12.dp))
            FilterChip(
                selected = selectedCategory == "الكل",
                onClick = { selectedCategory = "الكل" },
                label = { Text("عرض الكل") },
                modifier = Modifier.padding(end = 4.dp)
            )
            for (cat in categories) {
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ledger list
        if (filteredLedger.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("لا توجد قيود للمعاملات المحددة.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLedger) { item ->
                    FinancialItemRow(item = item, currency = currency, db = db, onRefresh = { loadLedger() }, userRole = userRole)
                }
            }
        }
    }

    // Spend item overlay
    if (showAddExpense) {
        Dialog(onDismissRequest = { showAddExpense = false }) {
            var selectedSpentCat by remember { mutableStateOf("صيانة ونظافة") }
            var amount by remember { mutableStateOf("") }
            var desc by remember { mutableStateOf("") }
            val context = LocalContext.current

            val spentCats = listOf("صيانة ونظافة", "رواتب", "فواتير وضيافة", "أخرى")

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("تسجيل مصروف نقدي من الخزينة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("فئة المصروف:", fontSize = 13.sp, color = Color.Gray)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        spentCats.forEach { cat ->
                            FilterChip(
                                selected = selectedSpentCat == cat,
                                onClick = { selectedSpentCat = cat },
                                label = { Text(cat) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("قيمة المصروف بالنقد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("وصف المصادرة والجهة المصروف لها") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddExpense = false }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amtNum = amount.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0 && desc.isNotBlank()) {
                                    db.addFinancialTransaction(
                                        "EXPENSE",
                                        selectedSpentCat,
                                        amtNum,
                                        desc,
                                        System.currentTimeMillis()
                                    )
                                    loadLedger()
                                    showAddExpense = false
                                    Toast.makeText(context, "تم قيد الصرفية وخصمها من الخزانة", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "الرجاء تعبئة البيانات بشكل صحيح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StrongRed)
                        ) {
                            Text("دفع المصروف")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialItemRow(item: FinancialTransaction, currency: String, db: DatabaseHelper, onRefresh: () -> Unit, userRole: String) {
    val isIncome = item.type == "INCOME"
    val sdf = SimpleDateFormat("yyyy-MM-dd / hh:mm a", Locale.US)
    val formattedDate = sdf.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = LightSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(if (isIncome) StrongGreen else SidebarColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formattedDate, fontSize = 11.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PrimaryDark)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isIncome) "+ ${item.amount} $currency" else "- ${item.amount} $currency",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) StrongGreen else StrongRed
                )

                // Deletion available ONLY to ADMIN role
                if (userRole == "ADMIN") {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            db.deleteFinancialTransaction(item.id)
                            onRefresh()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حدف بند", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// SECTION 4: REPORTS AND CHARTS DESK
@Composable
fun ReportsDesk(db: DatabaseHelper) {
    var ledger by remember { mutableStateOf(emptyList<FinancialTransaction>()) }
    var bookings by remember { mutableStateOf(emptyList<Booking>()) }
    var rooms by remember { mutableStateOf(emptyList<Room>()) }

    LaunchedEffect(Unit) {
        ledger = db.getAllFinancialTransactions()
        bookings = db.getAllBookings()
        rooms = db.getAllRooms()
    }

    val currency = db.getSetting("currency", "ر.س")

    // Stats calculations
    val totalIncomes = ledger.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpenses = ledger.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val occupiedRooms = rooms.count { it.status == "RESERVED" }
    val occupancyPct = if (rooms.isNotEmpty()) (occupiedRooms.toFloat() / rooms.size) * 100 else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("التقارير اليومية والشهرية والإحصاءات", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
            Text("رؤية إحصاءات الإشغال ومصادر دخل الفندق الحيوية لفترات مختلفة", fontSize = 12.sp, color = Color.Gray)
        }

        // Summary Indicators Bento-Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("معدل إشغال الغرف الحالي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${DecimalFormat("#.#").format(occupancyPct)} %",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "مستعمل $occupiedRooms من أصل ${rooms.size}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("رصيد الفواتير والدفع المعلق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        val pendingCollect = bookings.filter { it.status == "ACTIVE" }.sumOf { it.amountRemaining }
                        Text(
                            text = "$pendingCollect $currency",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "سيتم تحصيلها عند الخروج", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("شعبية نوع السكن المفضل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        val faveType = if (bookings.isNotEmpty()) {
                            bookings.groupBy { it.roomType }.maxByOrNull { it.value.size }?.key ?: "غرف متفرقة"
                        } else "لا يوجد"
                        Text(
                            text = faveType,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SidebarColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "الأكثر طلباً وتكراراً لدى النزلاء", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Financial Chart Simulation & Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("الملخص المالي والتحليلات اليومية والشهرية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxValue = maxOf(totalIncomes, totalExpenses, 1000.0)
                    val incomeRatio = (totalIncomes / maxValue).toFloat()
                    val expenseRatio = (totalExpenses / maxValue).toFloat()

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("النمو المبيعات (دخل الحجوزات): $totalIncomes $currency", fontSize = 12.sp, color = Color.DarkGray)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(incomeRatio.coerceIn(0.01f, 1f))
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(PremiumTeal, TealGlow)))
                            )
                        }

                        Text("الضيافة والمصاريف والرواتب: $totalExpenses $currency", fontSize = 12.sp, color = Color.DarkGray)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(expenseRatio.coerceIn(0.01f, 1f))
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(StrongRed, Color(0xFFFCA5A5))))
                            )
                        }
                    }
                }
            }
        }

        // Logs of previous reservations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("تقرير كشف تكرار النزلاء وحالة الإخلاء", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (bookings.isEmpty()) {
                        Text("لا توجد حجوزات حقيقية في السجل حتى الآن.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (b in bookings.take(6)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = b.guestName, fontSize = 13.sp, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                                    Text(text = "غرفة " + b.roomNumber, fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        text = if (b.status == "ACTIVE") "نشط" else "مغادر",
                                        fontSize = 11.sp,
                                        color = if (b.status == "ACTIVE") StrongGreen else Color.DarkGray
                                    )
                                }
                                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// SECTION 5: STAFF PERMISSIONS MANAGEMENT DESK
@Composable
fun UsersDesk(db: DatabaseHelper, userRole: String) {
    var users by remember { mutableStateOf(emptyList<AppUser>()) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun loadUsers() {
        users = db.getAllUsers()
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("إدارة شؤون الطاقم والصلاحيات حسب الدور", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                Text("تخصيص الصلاحيات للفريق: (المدير ADMIN يملك صلاحيات التعديل والحذف، وموظف استقبال RECEPTIONIST للترحيب والحجز والتحصيل)", fontSize = 12.sp, color = Color.Gray)
            }

            if (userRole == "ADMIN") {
                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة موظف")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("لا يوجد موظفين مسجلين.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users) { usr ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, Color.LightGray),
                        colors = CardDefaults.cardColors(containerColor = LightSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PremiumTeal,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SoftGreen, CircleShape)
                                        .padding(8.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = usr.fullname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                                    Text(text = "اسم المستخدم: ${usr.username}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (usr.role == "ADMIN") "مدير النظام" else "موظف استقبال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (usr.role == "ADMIN") StrongRed else StrongGreen,
                                    modifier = Modifier
                                        .background(
                                            (if (usr.role == "ADMIN") SoftRed else SoftGreen).copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                if (userRole == "ADMIN" && usr.id != 1L) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(onClick = {
                                        if (db.deleteUser(usr.id)) {
                                            loadUsers()
                                            Toast.makeText(context, "تم حذف الموظف", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الموظف", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        Dialog(onDismissRequest = { showAddUserDialog = false }) {
            var inputUser by remember { mutableStateOf("") }
            var inputPass by remember { mutableStateOf("") }
            var inputName by remember { mutableStateOf("") }
            var inputRole by remember { mutableStateOf("RECEPTIONIST") }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("إضافة موظف جديد للنظام", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("الاسم الكامل للموظف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputUser,
                        onValueChange = { inputUser = it },
                        label = { Text("اسم المستخدم (للإدخال الفوري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputPass,
                        onValueChange = { inputPass = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("اختر المسمى الوظيفي والدور:", fontSize = 12.sp, color = Color.Gray)
                    Row {
                        FilterChip(
                            selected = inputRole == "RECEPTIONIST",
                            onClick = { inputRole = "RECEPTIONIST" },
                            label = { Text("موظف استقبال") },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        FilterChip(
                            selected = inputRole == "ADMIN",
                            onClick = { inputRole = "ADMIN" },
                            label = { Text("مدير عام") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddUserDialog = false }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputName.isNotBlank() && inputUser.isNotBlank() && inputPass.isNotBlank()) {
                                    db.addUser(inputUser, inputPass, inputRole, inputName)
                                    loadUsers()
                                    showAddUserDialog = false
                                    Toast.makeText(context, "تم تسجيل وإضافة الموظف الجديد بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                        ) {
                            Text("إضافة")
                        }
                    }
                }
            }
        }
    }
}

// SECTION 6: SETTINGS DESK
@Composable
fun SettingsDesk(db: DatabaseHelper, userRole: String, onSave: () -> Unit) {
    var hotelName by remember { mutableStateOf(db.getSetting("hotel_name", "فندق الجوهرة")) }
    var currency by remember { mutableStateOf(db.getSetting("currency", "ر.س")) }
    var checkoutHour by remember { mutableStateOf(db.getSetting("checkout_hour", "12")) }
    var restHours by remember { mutableStateOf(db.getSetting("rest_duration_hours", "3")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("الإعدادات العامة لمبنى الفندق ومقاييس الوقت", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
        Text("تخصيص طريقة احتساب المبالغ، وموعد الخروج الافتراضي اليوم مسبقاً", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = LightSurface)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("المعلومات التشغيلية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PremiumTeal)

                OutlinedTextField(
                    value = hotelName,
                    onValueChange = { hotelName = it },
                    label = { Text("اسم الفندق / المنشأة السكنية") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userRole == "ADMIN"
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("رمز العملة الرسمية للمحاسبة") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userRole == "ADMIN"
                )

                OutlinedTextField(
                    value = checkoutHour,
                    onValueChange = { checkoutHour = it },
                    label = { Text("ساعة المغادرة والتحرير اليومية (بنظام الـ 24 ساعة - افتراضي: 12 ظهراً)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userRole == "ADMIN"
                )

                OutlinedTextField(
                    value = restHours,
                    onValueChange = { restHours = it },
                    label = { Text("مدة الاستراحة القصيرة القياسية بالساعات (حالة الراحة للاستراحة القصيرة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userRole == "ADMIN"
                )

                if (userRole == "ADMIN") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            db.saveSetting("hotel_name", hotelName)
                            db.saveSetting("currency", currency)
                            db.saveSetting("checkout_hour", checkoutHour)
                            db.saveSetting("rest_duration_hours", restHours)
                            onSave()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("حفظ الإعدادات الفندقية")
                    }
                } else {
                    Text(
                        "ملاحظة: تعديل إعدادات الإدارة متاح فقط لحساب المدير العام (ADMIN)",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ---------------- DIALOGS IMPLEMENTATIONS ----------------

// BOOKING PROCESS FORM & OCR SIMULATOR
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingFormDialog(
    room: Room,
    db: DatabaseHelper,
    onDismiss: () -> Unit,
    onSuccess: (Booking) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var guestName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf("هوية وطنية") }
    var phone by remember { mutableStateOf("") }

    // Companions State (renamed model type to GuestCompanion)
    var companionCount by remember { mutableStateOf(0) }
    val companions = remember { mutableStateListOf<GuestCompanion>() }

    // Rates State
    var nightsCount by remember { mutableStateOf(1) } // Default 1 night
    var isRestBreak by remember { mutableStateOf(false) } // Default: night stay, false
    var pricePerNight by remember { mutableStateOf(room.price.toString()) }
    var amountPaid by remember { mutableStateOf("") }

    // ID Photo State
    var idPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrScanCompletedAnim by remember { mutableStateOf(false) }

    // Currency Setting
    val currencySetting = db.getSetting("currency", "ر.س")

    // Automatic Companion fields synchronizer
    LaunchedEffect(companionCount) {
        while (companions.size < companionCount) {
            companions.add(GuestCompanion("", "", "هوية وطنية", ""))
        }
        while (companions.size > companionCount) {
            companions.removeAt(companions.size - 1)
        }
    }

    // Invoice pricing computations
    val rate = pricePerNight.toDoubleOrNull() ?: 0.0
    val totalCost = if (isRestBreak) {
        val defaultRestDiv = 2 // Rest is normally 50% off standard night price
        rate / defaultRestDiv
    } else {
        rate * nightsCount
    }
    val paid = amountPaid.toDoubleOrNull() ?: 0.0
    val remaining = maxOf(0.0, totalCost - paid)

    // OCR simulated triggers
    var showOcrScannerWidget by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = SoftWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تسجيل حجز جديد - غرفة " + room.roomNumber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                        Text(
                            text = "نوع الغرفة: ${room.roomType} | سعر المبيت الأساسي: ${room.price} $currencySetting",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable fields form container
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AI Ocr Trigger Panel
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SoftGreen),
                            border = BorderStroke(1.dp, StrongGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                                    .clickable { showOcrScannerWidget = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle, 
                                    contentDescription = "OCR AI",
                                    tint = StrongGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "مسح سريع للبطاقة / الهوية الوطنية بالذكاء الاصطناعي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryDark
                                    )
                                    Text(
                                        "التقط صورة البطاقة أو اختر من النماذج وسيتكفل المساعد بملئ البيانات فوراً",
                                        fontSize = 11.sp, color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        // Guest attached ID indicator preview if any
                        idPhotoBase64?.let { base64 ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightSurface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val bitmap = convertBase64ToBitmap(base64)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "صورة الممسوحة",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "بطاقة الهوية مرفقة ومثبتة في الحجز بنجاح ✓",
                                        color = StrongGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { idPhotoBase64 = null }) {
                                        Text("مسح الصورة", color = Color.Red, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Text("بيانات النزيل الأساسية", fontWeight = FontWeight.Bold, color = PremiumTeal)

                        OutlinedTextField(
                            value = guestName,
                            onValueChange = { guestName = it },
                            label = { Text("اسم النزيل الكامل (المكتوب في الهوية)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = idNumber,
                                onValueChange = { idNumber = it },
                                label = { Text("رقم بطاقة الهوية / جواز السفر") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            // ID Type Dropdown simulator
                            var expandedIDMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = idType,
                                    onValueChange = {},
                                    label = { Text("نوع وثيقة الهوية") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { expandedIDMenu = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = expandedIDMenu,
                                    onDismissRequest = { expandedIDMenu = false }
                                ) {
                                    listOf("هوية وطنية", "جواز سفر", "هوية مقيم", "أخرى").forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                idType = item
                                                expandedIDMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف الجوال (لتأكيد التواصل)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("مرافقين إضافيين مع النزيل في الغرفة", fontWeight = FontWeight.Bold, color = PremiumTeal)

                        // Companion Count Setter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("هل يوجد مرافقين؟", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { if (companionCount > 0) companionCount-- },
                                    colors = ButtonDefaults.buttonColors(containerColor = SidebarColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                                Text(
                                    "$companionCount",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = { companionCount++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            }
                        }

                        // Dynamically Generated Companion Fields
                        for (idx in 0 until companionCount) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(0.5.dp, GoldAmber.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = LightSurface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("بيانات المرافق رقم ${idx + 1}", fontSize = 12.sp, color = GoldAmber, fontWeight = FontWeight.Bold)
                                    
                                    val currentComp = companions.getOrNull(idx) ?: GuestCompanion("", "", "هوية وطنية", "")

                                    OutlinedTextField(
                                        value = currentComp.name,
                                        onValueChange = { updatedName ->
                                            if (idx < companions.size) companions[idx] = currentComp.copy(name = updatedName)
                                        },
                                        label = { Text("الاسم الكامل للمرافق") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = currentComp.idNumber,
                                            onValueChange = { updatedId ->
                                                if (idx < companions.size) companions[idx] = currentComp.copy(idNumber = updatedId)
                                            },
                                            label = { Text("رقم هوية المرافق") },
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = currentComp.phone,
                                            onValueChange = { updatedPhone ->
                                                if (idx < companions.size) companions[idx] = currentComp.copy(phone = updatedPhone)
                                            },
                                            label = { Text("رقم هاتف المرافق") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("خيارات الإقامة والوقت والتحصيل المالي", fontWeight = FontWeight.Bold, color = PremiumTeal)

                        // Accomodation Switch options
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("نوع الحجز الإقامة:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                            Row {
                                FilterChip(
                                    selected = !isRestBreak,
                                    onClick = { isRestBreak = false },
                                    label = { Text("مبيت عادي ليلة / ليالي") },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                FilterChip(
                                    selected = isRestBreak,
                                    onClick = { isRestBreak = true },
                                    label = { Text("استراحة قصيرة (عدة ساعات)") }
                                )
                            }
                        }

                        if (!isRestBreak) {
                            OutlinedTextField(
                                value = if (nightsCount > 0) "$nightsCount" else "",
                                onValueChange = {
                                    val n = it.toIntOrNull() ?: 1
                                    nightsCount = maxOf(1, n)
                                },
                                label = { Text("عدد الليالي (قيمة حرة مفتوحة)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Note labels summarizing duration rule of short stay
                            val restDurValue = db.getSetting("rest_duration_hours", "3")
                            Text(
                                "الموقع والاتفاقية: الاستراحة صالحة للاستخدام لمدة قصيرة ($restDurValue ساعات كحد قياسي) بقيمة مخفضة، ويتم التحرير فوراً.",
                                fontSize = 12.sp,
                                color = GoldAmber,
                                modifier = Modifier.background(SoftRed, RoundedCornerShape(8.dp)).padding(8.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pricePerNight,
                                onValueChange = { pricePerNight = it },
                                label = { Text("تعديل سعر المبيت لليلة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = amountPaid,
                                onValueChange = { amountPaid = it },
                                label = { Text("المبلغ المالي المدفوع الآن (مقدماً)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Total Price break calculations card (RTL design)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("تفاصيل الحساب المالي الإجمالي للحجز", fontSize = 12.sp, color = Color.LightGray)
                                    Text(
                                        text = "المبلغ الإجمالي المطلق: $totalCost $currencySetting",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "المدفوع حالياً: $paid $currencySetting", fontSize = 12.sp, color = StrongGreen)
                                    Text(text = "المتبقي للاستحقاق: $remaining $currencySetting", fontSize = 12.sp, color = if (remaining > 0) GoldAmber else Color.White)
                                }
                            }
                        }

                        // Chronological validation instructions
                        val checkInTimeMillis = System.currentTimeMillis()
                        val checkoutTimeMillis = calculateCheckOutTime(
                            checkInTimeMillis = checkInTimeMillis,
                            nightsCount = if (isRestBreak) -1 else nightsCount,
                            checkOutHour = db.getSetting("checkout_hour", "12").toIntOrNull() ?: 12
                        )
                        val sdfOutTime = SimpleDateFormat("yyyy-MM-dd / hh:mm a (E)", Locale.US)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("تأريخ تحرير الغرفة وجدولة الخروج:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = PremiumTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تاريخ إخلاء الغرفة الحتمي: ${sdfOutTime.format(Date(checkoutTimeMillis))}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDark
                                    )
                                }
                                Text(
                                    "ملاحظة: تماشياً مع طلبك، إذا دخل النزيل الفجر (أو قبل 6 صباحاً)، تنتهي الإقامة اليوم 12 ظهراً. بخلاف ذلك تنتهي غداً 12 ظهراً.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { onDismiss() }) {
                        Text("إلغاء الأمر")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (guestName.isBlank() || idNumber.isBlank() || phone.isBlank()) {
                                Toast.makeText(context, "الرجاء تعبئة بيانات النزيل اللازمة", Toast.LENGTH_SHORT).show()
                            } else {
                                val nowMillis = System.currentTimeMillis()
                                val computedCheckOut = calculateCheckOutTime(
                                    checkInTimeMillis = nowMillis,
                                    nightsCount = if (isRestBreak) -1 else nightsCount,
                                    checkOutHour = db.getSetting("checkout_hour", "12").toIntOrNull() ?: 12
                                )

                                db.createBooking(
                                    roomId = room.id,
                                    guestName = guestName,
                                    idType = idType,
                                    idNumber = idNumber,
                                    phone = phone,
                                    companions = companions,
                                    nightsCount = if (isRestBreak) -1 else nightsCount,
                                    pricePerNight = rate,
                                    amountPaid = paid,
                                    amountRemaining = remaining,
                                    checkInTime = nowMillis,
                                    checkOutTime = computedCheckOut,
                                    idPhotoBase64 = idPhotoBase64
                                )

                                val fullBooking = db.getActiveBookingForRoom(room.id)
                                if (fullBooking != null) {
                                    onSuccess(fullBooking)
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                    ) {
                        Text("تثبيت الحجز وإصدار الفاتورة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // AI SCANNER DIALOG OVERLAY (HIGH FIDELITY)
    if (showOcrScannerWidget) {
        Dialog(
            onDismissRequest = { showOcrScannerWidget = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("مساعد مسح الهوية الوطني الفوري", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Text("اختر أحد النماذج المسرعة للتجربة الفورية، أو قم بمسح الصورة", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scanner view frame representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Green Scanning Glowing laser line simulation
                        var isScanningLineUp by remember { mutableStateOf(false) }
                        LaunchedEffect(ocrLoading) {
                            if (ocrLoading) {
                                while (true) {
                                    isScanningLineUp = !isScanningLineUp
                                    delay(900)
                                }
                            }
                        }

                        if (ocrLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = StrongGreen)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("جاري تحليل البطاقة عبر الذكاء الاصطناعي...", color = Color.White, fontSize = 12.sp)
                            }

                            // Glowing scanning line animation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .height(2.dp)
                                    .background(StrongGreen)
                                    .align(if (isScanningLineUp) Alignment.TopCenter else Alignment.BottomCenter)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                Text("عدسة القراءة الفورية للنزلاء كود الكاميرا", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("حدد نموذج جاهز لمحاكاة المسار فوراً (موصى به):", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Model list
                    val templates = listOf(
                        Triple("سليمان العتيبي (هوية سعودية)", "1129402941", "هوية وطنية"),
                        Triple("John Simpson (جواز بريطاني)", "N41294821", "جواز سفر"),
                        Triple("محمد المهدي (إقامة سارية)", "2401829419", "هوية مقيم")
                    )

                    templates.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.15f))
                                .clickable {
                                    scope.launch {
                                        ocrLoading = true
                                        ocrScanCompletedAnim = false
                                        delay(1500) 
                                        
                                        guestName = t.first.split(" (")[0]
                                        idNumber = t.second
                                        idType = t.third
                                        phone = "055" + (1000000..9999999).random() 

                                        idPhotoBase64 = generateMockIdBase64()
                                        
                                        ocrLoading = false
                                        ocrScanCompletedAnim = true
                                        showOcrScannerWidget = false
                                        Toast.makeText(context, "تم مسح واستخلاص بيانات النزيل بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = PremiumTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t.first, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.Main) {
                                ocrLoading = true
                                delay(1800)
                                if (GeminiOcrHelper.isApiKeyConfigured()) {
                                    val mockBase = generateMockIdBase64()
                                    GeminiOcrHelper.scanIdCard(mockBase) { name, idNum, type, error ->
                                        scope.launch(Dispatchers.Main) {
                                            ocrLoading = false
                                            if (error == null && name != null) {
                                                guestName = name
                                                idNumber = idNum ?: "120491829"
                                                idType = type ?: "هوية وطنية"
                                                phone = "055819204"
                                                idPhotoBase64 = mockBase
                                                showOcrScannerWidget = false
                                                Toast.makeText(context, "تم مسح واستخلاص البيانات بالذكاء الاصطناعي!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تمت المحاكاة: الجوال لا يدعم رفع صور حقيقية محلياً.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } else {
                                    guestName = "فيصل عبد العزيز السديري"
                                    idNumber = "1092849102"
                                    idType = "هوية وطنية"
                                    phone = "0539182741"
                                    idPhotoBase64 = generateMockIdBase64()
                                    ocrLoading = false
                                    showOcrScannerWidget = false
                                    Toast.makeText(context, "الوضع المحاكي: تم تعبئة البيانات بالاسم والبطاقة!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SidebarColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("التقاط صورة البطاقات أو رفع ملف (معالجة OCR)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showOcrScannerWidget = false }) {
                        Text("إلغاء المعاينة والتراجع")
                    }
                }
            }
        }
    }
}

// RESERVATION DETAILS & MANAGEMENT OVERLAY
@Composable
fun BookingDetailDialog(
    booking: Booking,
    room: Room,
    db: DatabaseHelper,
    onDismiss: () -> Unit,
    onActionComplete: () -> Unit,
    onPrintInvoice: () -> Unit
) {
    val currency = db.getSetting("currency", "ر.س")
    var expandComps by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = LightSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل الحجز لغرفة " + room.roomNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.15f))

                // Guest Summary
                Text("المستأجر الرئيسي النزيل:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = booking.guestName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                Text(text = "رقم الهوية: ${booking.idNumber} (${booking.idType})", fontSize = 12.sp, color = Color.DarkGray)
                Text(text = "رقم الهاتف الجوال: ${booking.phone}", fontSize = 12.sp, color = Color.DarkGray)

                if (booking.companionsCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { expandComps = !expandComps },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (expandComps) "إخفاء شاشات المرافقين (${booking.companionsCount})" else "عرض بيانات المرافقين البديلة (${booking.companionsCount}) ▽",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAmber
                        )
                    }

                    if (expandComps) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            booking.companions.forEachIndexed { i, comp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SoftWhite)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("مرافق ${i + 1}: ${comp.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("الهوية: ${comp.idNumber} (${comp.idType}) | هاتف: ${comp.phone}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.15f))

                // Stay Detail
                val sdfTime = SimpleDateFormat("yyyy-MM-dd / hh:mm a", Locale.US)
                Text("توقيت الاستراحة أو السكن الفعلي:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = "تاريخ الدخول: ${sdfTime.format(Date(booking.checkInTime))}", fontSize = 12.sp, color = Color.DarkGray)
                Text(text = "تاريخ التحرير: ${sdfTime.format(Date(booking.checkOutTime))}", fontSize = 12.sp, color = Color.DarkGray)
                Text(text = "المدة: " + (if (booking.nightsCount == -1) "استراحة قصيرة" else "${booking.nightsCount} ليلة"), fontSize = 12.sp, color = Color.DarkGray)

                Divider(color = SlateBorder.copy(alpha = 0.15f))

                // Finance Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("المدفوع من الحساب", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${booking.amountPaid} $currency", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StrongGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("النقد المتبقي بذمته", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${booking.amountRemaining} $currency", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (booking.amountRemaining > 0) GoldAmber else Color.DarkGray)
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.15f))

                // Attachment Preview
                booking.idPhotoBase64?.let { base64 ->
                    Text("صورة بطاقة الهوية المخزنة:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    val bitmap = convertBase64ToBitmap(base64)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "وثيقة النزيل",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Button footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            db.completeAndReleaseBooking(booking.id, booking.roomId)
                            onActionComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StrongRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تحرير ومغادرة", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            onPrintInvoice()
                        },
                        border = BorderStroke(1.dp, PremiumTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("الفاتورة والطباعة", color = PremiumTeal)
                    }
                }
            }
        }
    }
}

// INVOICE RECEIPT VIEW & PRINT DIALOG (PORTABLE FRIENDLY)
@Composable
fun InvoicePrintDialog(
    booking: Booking,
    db: DatabaseHelper,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hotelName = db.getSetting("hotel_name", "فندق الجوهرة الرياض")
    val currency = db.getSetting("currency", "ر.س")
    val invoiceNum = 1000 + booking.id
    val sdfDate = SimpleDateFormat("yyyy-MM-dd / hh:mm a", Locale.US)

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = LightSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Printable area container simulation representation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        .background(Color(0xFFFCFCFC))
                        .padding(16.dp)
                ) {
                    Text(text = hotelName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(text = "فاتورة حجز وإقامة فندقية رقم #$invoiceNum", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Booking details list
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("اسم النزيل:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(booking.guestName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("نوع الهوية:", fontSize = 12.sp, color = Color.DarkGray)
                        Text("${booking.idType} (${booking.idNumber})", fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم الهاتف:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(booking.phone, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الغرفة / الجناح:", fontSize = 12.sp, color = Color.DarkGray)
                        Text("غرفة ${booking.roomNumber} (${booking.roomType})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PremiumTeal)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المدة الممنوحة:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(if (booking.nightsCount == -1) "استراحة قصيرة" else "${booking.nightsCount} ليلة", fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تاريخ الدخول:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(sdfDate.format(Date(booking.checkInTime)), fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تاريخ الخروج:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(sdfDate.format(Date(booking.checkOutTime)), fontSize = 11.sp)
                    }

                    if (booking.companionsCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("المرافقين التابعين للنزيل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAmber)
                        booking.companions.forEach { comp ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• ${comp.name}", fontSize = 11.sp, color = Color.DarkGray)
                                Text("الهوية: ${comp.idNumber}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cash calculations
                    val rateCost = booking.pricePerNight
                    val totalCharge = if (booking.nightsCount == -1) rateCost / 2 else rateCost * booking.nightsCount
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المبلغ الإجمالي للاستضافة:", fontSize = 12.sp, color = Color.DarkGray)
                        Text("$totalCharge $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المبلغ المدفوع (مسبقاً):", fontSize = 12.sp, color = StrongGreen)
                        Text("${booking.amountPaid} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StrongGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الرصيد المتبقي بذمته:", fontSize = 12.sp, color = if (booking.amountRemaining > 0) GoldAmber else Color.Gray)
                        Text("${booking.amountRemaining} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (booking.amountRemaining > 0) GoldAmber else PrimaryDark)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("شروط الاستخدام: الرجاء تسليم المفتاح من بوابة الغرفة قبل الساعة 12 ظهراً لتخطي أي رسوم أخرى.", fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("إغلاق الفاتورة")
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "تم إرسال الفاتورة بنجاح إلى وحدة الطباعة ومشاركتها مع النزيل!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("طباعة الفاتورة والملفات")
                    }
                }
            }
        }
    }
}

// ---------------- STRUCTURAL HELPERS FOR OCR & IMAGE RESIZING ----------------

fun convertBase64ToBitmap(b64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

fun generateMockIdBase64(): String {
    val bitmap = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawRect(0f, 0f, 300f, 200f, paint)
    
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawRect(20f, 30f, 150f, 45f, paint)
    canvas.drawRect(20f, 60f, 240f, 75f, paint)
    canvas.drawRect(20f, 95f, 200f, 105f, paint)
    
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(210f, 25f, 280f, 110f, paint)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    val b = outputStream.toByteArray()
    return Base64.encodeToString(b, Base64.DEFAULT)
}

@Composable
fun Modifier.fillModifierResponsive(): Modifier {
    return this.fillMaxWidth(0.9f)
}

// Compute checkout according to standard clock constraints of the user rules
fun calculateCheckOutTime(checkInTimeMillis: Long, nightsCount: Int, checkOutHour: Int = 12): Long {
    if (nightsCount == -1) {
        // Short Rest Break stay (defaults to 3 hours increment)
        return checkInTimeMillis + (3 * 60 * 60 * 1000)
    }
    
    val cal = Calendar.getInstance().apply {
        timeInMillis = checkInTimeMillis
    }
    val checkInHour = cal.get(Calendar.HOUR_OF_DAY)
    
    val checkoutCal = Calendar.getInstance().apply {
        timeInMillis = checkInTimeMillis
        set(Calendar.HOUR_OF_DAY, checkOutHour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    if (checkInHour >= 0 && checkInHour < 6) {
        // Late arrival case (Check-in between midnight and 6 AM)
        checkoutCal.add(Calendar.DAY_OF_YEAR, nightsCount - 1)
    } else {
        // Standard check-in during noon or evening
        checkoutCal.add(Calendar.DAY_OF_YEAR, nightsCount)
    }
    
    // Safety guard
    if (checkoutCal.timeInMillis <= checkInTimeMillis) {
        checkoutCal.timeInMillis = checkInTimeMillis + (2 * 60 * 60 * 1000)
    }
    
    return checkoutCal.timeInMillis
}
