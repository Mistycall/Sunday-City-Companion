package com.sundaycity.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sundaycity.companion.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Ink = Color(0xFF172431)
private val Teal = Color(0xFF087F8C)
private val Gold = Color(0xFFE4AC48)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SundayCompanionApp() } }
}

class CompanionViewModel(private val dao: CompanionDao) : ViewModel() {
    var sandboxEnabled by mutableStateOf(true)
        private set
    fun updateSandboxEnabled(enabled: Boolean) { sandboxEnabled = enabled }
    val vehicles = dao.observeVehicles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = dao.observeGoals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rooms = dao.observeRooms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun furniture(roomId: Long): Flow<List<FurnitureItem>> = dao.observeFurniture(roomId)
    fun addVehicle(name: String, category: String) = viewModelScope.launch { if (name.isNotBlank()) dao.addVehicle(GarageVehicle(name = name.trim(), category = category)) }
    fun deleteVehicle(vehicle: GarageVehicle) = viewModelScope.launch { dao.deleteVehicle(vehicle) }
    fun addGoal(title: String) = viewModelScope.launch { if (title.isNotBlank()) dao.addGoal(PlayerGoal(title = title.trim())) }
    fun completeGoal(goal: PlayerGoal, checked: Boolean) = viewModelScope.launch { dao.setGoalCompleted(goal.id, checked) }
    fun deleteGoal(goal: PlayerGoal) = viewModelScope.launch { dao.deleteGoal(goal) }
    fun addRoom(name: String, style: String) = viewModelScope.launch { dao.addRoom(HouseRoom(name = name, style = style)) }
    fun deleteRoom(room: HouseRoom) = viewModelScope.launch { dao.deleteRoomAndFurniture(room) }
    fun addFurniture(roomId: Long, name: String, category: String, emoji: String) = viewModelScope.launch {
        val existing = dao.observeFurniture(roomId).first()
        val occupied = existing.map { it.x to it.y }.toSet()
        val slot = (0..24).firstOrNull { index -> (index % 5 to index / 5) !in occupied }
        if (slot != null) dao.addFurniture(FurnitureItem(roomId = roomId, name = name, category = category, emoji = emoji, x = slot % 5, y = slot / 5))
    }
    fun deleteFurniture(item: FurnitureItem) = viewModelScope.launch { dao.deleteFurniture(item) }
    fun moveFurniture(item: FurnitureItem, dx: Int, dy: Int) = viewModelScope.launch {
        val newX = (item.x + dx).coerceIn(0, 4)
        val newY = (item.y + dy).coerceIn(0, 4)
        val occupied = dao.observeFurniture(item.roomId).first().any { it.id != item.id && it.x == newX && it.y == newY }
        if (!occupied) dao.moveFurniture(item.id, newX, newY)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SundayCompanionApp() {
    val context = LocalContext.current
    val vm: CompanionViewModel = viewModel(factory = viewModelFactory { initializer { CompanionViewModel(CompanionDatabase.get(context).companionDao()) } })
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Garage", "Casa", "Obiettivi", "Wallet", "Guide")
    val icons = listOf(Icons.Default.Home, Icons.Default.DirectionsCar, Icons.Default.Chair, Icons.Default.EmojiEvents, Icons.Default.AccountBalanceWallet, Icons.Default.MenuBook)
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Gold, background = Color(0xFFF2F5F7), surface = Color.White, onBackground = Ink, onSurface = Ink)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { CenterAlignedTopAppBar(title = { Text("SUNDAY CITY  •  COMPANION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold) }) },
            bottomBar = { NavigationBar { tabs.forEachIndexed { i, label -> NavigationBarItem(selected = selected == i, onClick = { selected = i }, icon = { Icon(icons[i], label) }, label = { Text(label) }) } } }
        ) { padding ->
            when (selected) {
                0 -> HomeScreen(vm, Modifier.padding(padding))
                1 -> GarageScreen(vm, Modifier.padding(padding))
                2 -> HouseScreen(vm, Modifier.padding(padding))
                3 -> GoalsScreen(vm, Modifier.padding(padding))
                4 -> WalletScreen(vm, Modifier.padding(padding))
                else -> GuidesScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun HomeScreen(vm: CompanionViewModel, modifier: Modifier = Modifier) {
    val vehicles by vm.vehicles.collectAsState()
    val goals by vm.goals.collectAsState()
    val rooms by vm.rooms.collectAsState()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("La tua città, organizzata.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Un mondo personale, indipendente dal gioco originale.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Garage", vehicles.size.toString(), Modifier.weight(1f))
            SummaryCard("Stanze", rooms.size.toString(), Modifier.weight(1f))
            SummaryCard("Obiettivi", "${goals.count { it.completed }}/${goals.size}", Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ULTIMATE SANDBOX", color = Gold, fontWeight = FontWeight.Bold)
                Text("Costruisci la tua casa. Colleziona veicoli. Sperimenta senza limiti.", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Valute e oggetti sono simulati esclusivamente in questa app.", color = Color(0xFFD2DCE2), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("I tuoi strumenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        listOf("Casa e arredamento" to "Crea stanze e posiziona mobili.", "Wallet Sandbox" to "Budget di prova illimitato.", "Garage virtuale" to "Organizza la tua collezione.", "Obiettivi" to "Tieni traccia dei tuoi traguardi.").forEach { (title, body) ->
            Card { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    } }
}

@Composable
fun GarageScreen(vm: CompanionViewModel, modifier: Modifier = Modifier) {
    val vehicles by vm.vehicles.collectAsState()
    var showDialog by remember { mutableStateOf(false) }; var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Auto") }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Garage virtuale", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Catalogo personale, non collegato ai veicoli del gioco.", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { showDialog = true }) { Icon(Icons.Default.Add, null); Text(" Aggiungi") }
        }
        if (vehicles.isEmpty()) EmptyState("Il garage è vuoto", "Aggiungi il primo veicolo alla tua raccolta.")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vehicles, key = { it.id }) { v -> Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = Teal)
                Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(v.name, fontWeight = FontWeight.SemiBold); Text(v.category, style = MaterialTheme.typography.bodySmall) }
                IconButton(onClick = { vm.deleteVehicle(v) }) { Icon(Icons.Default.DeleteOutline, "Elimina") }
            } } }
        }
    }
    if (showDialog) AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Nuovo veicolo") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome veicolo") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Auto", "Moto", "Altro").forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) } }
        }
    }, confirmButton = { TextButton(onClick = { vm.addVehicle(name, category); name = ""; showDialog = false }) { Text("Salva") } }, dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annulla") } })
}

@Composable
fun HouseScreen(vm: CompanionViewModel, modifier: Modifier = Modifier) {
    val rooms by vm.rooms.collectAsState()
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var showRoomDialog by remember { mutableStateOf(false) }
    var roomName by remember { mutableStateOf("Soggiorno") }
    var style by remember { mutableStateOf("Moderno") }
    val selectedRoom = rooms.firstOrNull { it.id == selectedRoomId } ?: rooms.firstOrNull()
    val furnitureFlow = selectedRoom?.let { vm.furniture(it.id) } ?: flowOf(emptyList())
    val furniture by furnitureFlow.collectAsState(initial = emptyList())
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("La mia casa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Designer 2D personale • contenuti sandbox", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { showRoomDialog = true }) { Icon(Icons.Default.Add, null); Text(" Stanza") }
        }
        if (rooms.isEmpty()) EmptyState("La casa è ancora vuota", "Crea la prima stanza per iniziare ad arredare.")
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { rooms.forEach { r -> FilterChip(selected = selectedRoom?.id == r.id, onClick = { selectedRoomId = r.id }, label = { Text(r.name) }) } }
            selectedRoom?.let { room ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F0EC))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("${room.name} • ${room.style}", fontWeight = FontWeight.Bold)
                        Text("Pianta schematica 5 × 5", style = MaterialTheme.typography.labelMedium)
                        for (y in 0..4) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            for (x in 0..4) {
                                val item = furniture.firstOrNull { it.x == x && it.y == y }
                                Box(Modifier.weight(1f).aspectRatio(1f).background(if (item != null) Color(0xFFD5E2D8) else Color(0xFFF8FAF8), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Text(item?.emoji ?: "·", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
                FurnitureEditor(vm, room.id, furniture)
                OutlinedButton(onClick = { vm.deleteRoom(room); selectedRoomId = null }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteOutline, null); Text(" Elimina stanza") }
            }
        }
    }
    if (showRoomDialog) AlertDialog(onDismissRequest = { showRoomDialog = false }, title = { Text("Crea una stanza") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(roomName, { roomName = it }, label = { Text("Nome stanza") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Moderno", "Lusso", "Minimal").forEach { s -> FilterChip(selected = style == s, onClick = { style = s }, label = { Text(s) }) } }
        }
    }, confirmButton = { TextButton(onClick = { if (roomName.isNotBlank()) vm.addRoom(roomName.trim(), style); showRoomDialog = false }) { Text("Crea") } }, dismissButton = { TextButton(onClick = { showRoomDialog = false }) { Text("Annulla") } })
}

@Composable
fun FurnitureEditor(vm: CompanionViewModel, roomId: Long, items: List<FurnitureItem>) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showCatalog by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.id == selectedId }
    val catalog = listOf(Triple("Divano", "Seduta", "🛋️"), Triple("Letto", "Camera", "🛏️"), Triple("Tavolo", "Arredo", "🪑"), Triple("Lampada", "Luce", "💡"), Triple("Pianta", "Decorazione", "🪴"), Triple("TV", "Elettronica", "📺"), Triple("Quadro", "Decorazione", "🖼️"), Triple("Armadio", "Contenitore", "🚪"), Triple("Tappeto", "Decorazione", "🟫"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Arredamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Button(onClick = { showCatalog = true }) { Icon(Icons.Default.Add, null); Text(" Mobile") } }
        if (items.isEmpty()) Text("Apri il catalogo per arredare questa stanza.", style = MaterialTheme.typography.bodySmall)
        items.forEach { item ->
            Card(onClick = { selectedId = item.id }) { Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.emoji, style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).padding(start = 8.dp)) { Text(item.name, fontWeight = FontWeight.Medium); Text("Posizione ${item.x + 1}, ${item.y + 1}", style = MaterialTheme.typography.bodySmall) }
                IconButton(onClick = { vm.deleteFurniture(item); if (selectedId == item.id) selectedId = null }) { Icon(Icons.Default.DeleteOutline, "Rimuovi") }
            } }
        }
        if (selected != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sposta ${selected.name}", fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { vm.moveFurniture(selected, 0, -1) }) { Icon(Icons.Default.KeyboardArrowUp, "Su") }
                    Row { IconButton(onClick = { vm.moveFurniture(selected, -1, 0) }) { Icon(Icons.Default.KeyboardArrowLeft, "Sinistra") }; IconButton(onClick = { vm.moveFurniture(selected, 1, 0) }) { Icon(Icons.Default.KeyboardArrowRight, "Destra") } }
                    IconButton(onClick = { vm.moveFurniture(selected, 0, 1) }) { Icon(Icons.Default.KeyboardArrowDown, "Giù") }
                }
            }
        }
    }
    if (showCatalog) AlertDialog(onDismissRequest = { showCatalog = false }, title = { Text("Catalogo arredamento · Sandbox") }, text = {
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) { items(catalog) { (n, c, e) -> TextButton(onClick = { vm.addFurniture(roomId, n, c, e); showCatalog = false }) { Text("$e   $n", modifier = Modifier.fillMaxWidth()) } } }
    }, confirmButton = { TextButton(onClick = { showCatalog = false }) { Text("Chiudi") } })
}

@Composable
fun GoalsScreen(vm: CompanionViewModel, modifier: Modifier = Modifier) {
    val goals by vm.goals.collectAsState()
    var title by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("I tuoi obiettivi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Aggiungi un traguardo") }, singleLine = true, trailingIcon = { IconButton(onClick = { vm.addGoal(title); title = "" }) { Icon(Icons.Default.Add, "Aggiungi") } })
        if (goals.isEmpty()) EmptyState("Nessun obiettivo", "Scrivi un traguardo e premi + per aggiungerlo.")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(goals, key = { it.id }) { goal ->
            Card { Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = goal.completed, onCheckedChange = { vm.completeGoal(goal, it) })
                Text(goal.title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                IconButton(onClick = { vm.deleteGoal(goal) }) { Icon(Icons.Default.DeleteOutline, "Elimina") }
            } }
        } }
    }
}

@Composable
fun WalletScreen(vm: CompanionViewModel, modifier: Modifier = Modifier) {
    val infinite = vm.sandboxEnabled
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Sandbox Wallet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Valute dimostrative utilizzabili solo nel companion: non sono trasferibili a Sunday City.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("PORTAFOGLIO LIBERO", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon(Icons.Default.AllInclusive, null, tint = Gold) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WalletBalance("🪙", "Soldi", if (infinite) "∞" else "0", Modifier.weight(1f))
                    WalletBalance("💎", "Diamanti", if (infinite) "∞" else "0", Modifier.weight(1f))
                }
                Text(if (infinite) "MODALITÀ SANDBOX ATTIVA" else "MODALITÀ SANDBOX DISATTIVA", color = Gold, style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Risorse illimitate", fontWeight = FontWeight.SemiBold); Text("Simulazione indipendente dal gioco.", style = MaterialTheme.typography.bodySmall) }
         Switch(checked = infinite, onCheckedChange = { vm.updateSandboxEnabled(it) })  ("Il catalogo della casa è dimostrativo: nessun acquisto reale.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun WalletBalance(emoji: String, title: String, amount: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF2B3B49))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$emoji  $title", color = Color(0xFFDCE6EB), style = MaterialTheme.typography.labelMedium)
            Text(amount, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun GuidesScreen(modifier: Modifier = Modifier) {
    val guides = listOf(
        "Organizza i progressi" to "Dividi i traguardi grandi in piccoli obiettivi verificabili.",
        "Progetta la casa" to "Crea una stanza, scegli uno stile e aggiungi mobili. Seleziona un mobile per spostarlo con le frecce.",
        "Usa la Sandbox" to "Soldi e diamanti infiniti sono valute dimostrative valide soltanto dentro Companion.",
        "Gestisci il garage" to "Usa nomi e categorie coerenti per ritrovare i veicoli.",
        "Privacy" to "Non inserire password o codici: il companion non richiede login."
    )
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Guide e consigli", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(guides) { (title, body) -> Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
