package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.PdfBook
import com.example.ui.viewmodel.BookViewModel
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val books by viewModel.filteredBooks.collectAsState()
    val categories by viewModel.dbCategories.collectAsState()
    val notes by viewModel.allNotes.collectAsState()

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showManageNotesDialog by remember { mutableStateOf(false) }

    var activeSubPage by remember { mutableStateOf("bookshelf") } // "bookshelf" or "notes"
    val selectedSamplePresets = remember { mutableStateMapOf("hafez" to true, "prince" to true, "javascript" to false) }
    var bookToChangeCategory by remember { mutableStateOf<com.example.data.model.PdfBook?>(null) }

    // State for book import form
    var bookTitleInput by remember { mutableStateOf("") }
    var bookCategorySelection by remember { mutableStateOf("سایر") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            // Try of extracting display filename
            selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "سند_منتخب.pdf"
            if (bookTitleInput.isBlank()) {
                bookTitleInput = selectedFileName.substringBeforeLast(".pdf").replace("_", " ")
            }
        }
    }

    if (activeSubPage == "notes") {
        var notesSearchQuery by remember { mutableStateOf("") }
        var editingNoteId by remember { mutableStateOf<Long?>(null) }
        var editingNoteText by remember { mutableStateOf("") }

        val filteredNotes = remember(notes, notesSearchQuery) {
            if (notesSearchQuery.isBlank()) {
                notes
            } else {
                notes.filter { 
                    it.content.contains(notesSearchQuery, ignoreCase = true) || 
                    it.bookTitle.contains(notesSearchQuery, ignoreCase = true)
                }
            }
        }

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = Color(0xFF0F172A),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Button (aligned left in RTL)
                            IconButton(
                                onClick = { activeSubPage = "bookshelf" },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "بازگشت به کتابخانه",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Screen Title (aligned right in RTL)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "دفترچه یادداشت‌های من",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "تمام نکات و یادداشت‌هایی که هنگام مطالعه ثبت کرده‌اید",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Notes Search box with RTL support
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                        OutlinedTextField(
                            value = notesSearchQuery,
                            onValueChange = { notesSearchQuery = it },
                            placeholder = { 
                                Text(
                                    "جستجو در یادداشت‌ها یا عنوان کتاب...", 
                                    color = Color(0xFF64748B), 
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                ) 
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right, color = Color.White),
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Filled.Search, 
                                    contentDescription = "جستجو", 
                                    tint = Color(0xFF64748B) 
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF0F172A))
            ) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    if (filteredNotes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(92.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (notesSearchQuery.isNotBlank()) "یادداشتی با این عبارت جستجو یافت نشد." else "هنوز هیچ یادداشتی ثبت نشده است.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (notesSearchQuery.isNotBlank()) "کلمات دیگری را برای جستجو امتحان کنید." else "هنگام مطالعه کتاب‌ها، با فشردن آیکون یادداشت در پایین صفحه می‌توانید برای هر صفحه نکته یا یادداشت بنویسید.",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            filteredNotes.forEach { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Delete and Edit actions on the left
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (editingNoteId == note.id) {
                                                    IconButton(
                                                        onClick = {
                                                            if (editingNoteText.isNotBlank()) {
                                                                viewModel.updateNoteContent(note, editingNoteText)
                                                                editingNoteId = null
                                                            }
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = "تایید", tint = Color.Green, modifier = Modifier.size(20.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { editingNoteId = null },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "لغو", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                    }
                                                } else {
                                                    IconButton(
                                                        onClick = {
                                                            editingNoteId = note.id
                                                            editingNoteText = note.content
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteNoteById(note.id)
                                                            Toast.makeText(context, "یادداشت با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }

                                            // Book Link on the right
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            val targetBook = viewModel.getBookById(note.bookId)
                                                            if (targetBook != null) {
                                                                viewModel.openBookAtPage(context, targetBook, note.pageNumber - 1)
                                                            } else {
                                                                Toast.makeText(context, "کتاب این یادداشت یافت نشد.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                    .background(Color(0xFF0F172A))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${note.bookTitle} (صفحه ${formatPersianNumber(note.pageNumber)})",
                                                    color = Color(0xFF60A5FA),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Right
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Book,
                                                    contentDescription = "کتاب",
                                                    tint = Color(0xFF60A5FA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (editingNoteId == note.id) {
                                            OutlinedTextField(
                                                value = editingNoteText,
                                                onValueChange = { editingNoteText = it },
                                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right, color = Color.White),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedTextColor = Color.White,
                                                    focusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF3B82F6),
                                                    unfocusedBorderColor = Color.Gray,
                                                    focusedContainerColor = Color(0xFF0F172A),
                                                    unfocusedContainerColor = Color(0xFF0F172A)
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = false,
                                                maxLines = 4
                                            )
                                        } else {
                                            Text(
                                                text = note.content,
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 14.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp),
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color(0xFF0F172A), // Premium Slate Dark Blue background
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(bottom = 6.dp)
            ) {
                // Simplified Minimal Header styled for Dark Blue premium
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "کتابخانه من",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFFF8FAFC) // Silver White text
                        )
                        Text(
                            text = "مطالعه راحت، سبک و سازماندهی شده",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF94A3B8) // Muted metallic steel blue
                        )
                    }
                    Row(
                        modifier = Modifier.wrapContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // NOTES BUTTON
                        IconButton(
                            onClick = {
                                activeSubPage = "notes"
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF3B82F6) // Accent highlight
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Assignment,
                                contentDescription = "یادداشت‌های من",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // ACTIONS/CATEGORIES BUTTON
                        IconButton(
                            onClick = {
                                showManageCategoriesDialog = true
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = "مدیریت دسته‌بندی‌ها",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Simplified Search input capsule
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("book_search_bar"),
                    placeholder = { 
                        Text(
                            "جستجو در بین کتاب‌ها...", 
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8).copy(alpha = 0.8f)
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "جستجو",
                            tint = Color(0xFF94A3B8)
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Clear, 
                                    contentDescription = "پاک کردن",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        disabledContainerColor = Color(0xFF1E293B),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Reset input states before showing Dialog
                    bookTitleInput = ""
                    selectedFileUri = null
                    selectedFileName = ""
                    bookCategorySelection = if (categories.isNotEmpty()) categories.first().name else "سایر"
                    showAddBookDialog = true
                },
                icon = { 
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                text = { Text("افزودن PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = Color(0xFFD3E3FD),
                contentColor = Color(0xFF001D35),
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("add_pdf_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(paddingValues)
        ) {
            // Horizontal sliding Categories filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Virtual main "همه" category
                val isAllSelected = selectedCategory == "همه"
                
                Surface(
                    onClick = { viewModel.selectCategory("همه") },
                    color = if (isAllSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                    contentColor = if (isAllSelected) Color.White else Color(0xFF94A3B8),
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "همه",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // Virtual premium "★ علاقه‌مندی‌ها" category chip
                val isFavSelected = selectedCategory == "★ علاقه‌مندی‌ها"
                Surface(
                    onClick = { viewModel.selectCategory("★ علاقه‌مندی‌ها") },
                    color = if (isFavSelected) Color(0xFF3B82F6) else Color(0xFF1E293B),
                    contentColor = if (isFavSelected) Color.White else Color(0xFF94A3B8),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "★ علاقه‌مندی‌ها",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                categories.forEach { category ->
                    val isSelected = selectedCategory == category.name
                    
                    Surface(
                        onClick = { viewModel.selectCategory(category.name) },
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                        contentColor = if (isSelected) Color.White else Color(0xFF94A3B8),
                        shape = RoundedCornerShape(12.dp),
                        border = null,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Interactive sort order selector row
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatPersianNumber(books.size)} کتاب پیدا شد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )

                var showSortMenu by remember { mutableStateOf(false) }
                val currentSortOption by viewModel.sortOption.collectAsState()
                
                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        color = Color(0xFF1E293B),
                        contentColor = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF3B82F6)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ترتیب: ${currentSortOption.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = option.displayName, 
                                        color = if (currentSortOption == option) Color(0xFF3B82F6) else Color(0xFFF1F5F9),
                                        fontWeight = if (currentSortOption == option) FontWeight.Bold else FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PDF Book shelf Grid
            if (books.isEmpty()) {
                // Beautiful interactive Empty states configuration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "قفسه کتاب خالی است!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "هیچ کتابی با فیلتر جستجو سازگار نیست." else "هنوز هیچ کتابی را وارد قفسه نکرده‌اید. کتاب‌های خود را طبقه‌بندی کرده و فورا مطالعه را آغاز کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (searchQuery.isEmpty()) {
                        // Action card with checklist to add books
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "بارگذاری کتاب‌های نمونه پیشنهادی",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFF1F5F9),
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = "کتاب‌های باکیفیت و استاندارد زیر را برای آشنایی با ابزارهای خوانش، تیک زده و اضافه کنید:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                // Preset Item 1
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSamplePresets["hafez"] = !(selectedSamplePresets["hafez"] ?: false) }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "شعر و ادبیات (حافظ شیرازی)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF60A5FA)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "غزلیات حافظ شیرازی",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Checkbox(
                                            checked = selectedSamplePresets["hafez"] ?: false,
                                            onCheckedChange = { selectedSamplePresets["hafez"] = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF3B82F6),
                                                uncheckedColor = Color(0xFF475569)
                                            )
                                        )
                                    }
                                }

                                // Preset Item 2
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSamplePresets["prince"] = !(selectedSamplePresets["prince"] ?: false) }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "داستان و رمان (به قلم اگزوپری)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF34D399)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "داستان شازده کوچولو",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Checkbox(
                                            checked = selectedSamplePresets["prince"] ?: false,
                                            onCheckedChange = { selectedSamplePresets["prince"] = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF3B82F6),
                                                uncheckedColor = Color(0xFF475569)
                                            )
                                        )
                                    }
                                }

                                // Preset Item 3
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSamplePresets["javascript"] = !(selectedSamplePresets["javascript"] ?: false) }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "آموزشی و درسی",
                                        fontSize = 11.sp,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "راهنمای سریع جاوااسکریپت",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Checkbox(
                                            checked = selectedSamplePresets["javascript"] ?: false,
                                            onCheckedChange = { selectedSamplePresets["javascript"] = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF3B82F6),
                                                uncheckedColor = Color(0xFF475569)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                var isImportingPreset by remember { mutableStateOf(false) }

                                Button(
                                    onClick = {
                                        val selectedIds = selectedSamplePresets.filter { it.value }.map { it.key }
                                        if (selectedIds.isEmpty()) {
                                            Toast.makeText(context, "لطفاً حداقل یک کتاب را برای بارگذاری انتخاب کنید.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isImportingPreset = true
                                            viewModel.importSelectedSampleBooks(context, selectedIds) {
                                                isImportingPreset = false
                                                Toast.makeText(context, "کتاب‌های انتخاب شده به قفسه کتابخانه شما اضافه شدند!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isImportingPreset,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(45.dp)
                                ) {
                                    if (isImportingPreset) {
                                         CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                         Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                         Spacer(modifier = Modifier.width(8.dp))
                                         Text("بارگذاری کتاب‌های انتخاب شده", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                bookTitleInput = ""
                                selectedFileUri = null
                                selectedFileName = ""
                                bookCategorySelection = if (categories.isNotEmpty()) categories.first().name else "سایر"
                                showAddBookDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("وارد کردن فایل پی‌دی‌اف دلخواه", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookShelfItem(
                            book = book,
                            onOpen = { viewModel.openBook(context, book) },
                            onToggleFavorite = { viewModel.toggleFavorite(book) },
                            onDelete = {
                                viewModel.deleteBook(context, book)
                                Toast.makeText(context, "کتاب '${book.title}' با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                            },
                            onChangeCategory = { bookToChangeCategory = book }
                        )
                    }
                }
            }
        }
    }

    // dialog: Manage Categories
    if (showManageCategoriesDialog) {
        var categoryNameInput by remember { mutableStateOf("") }
        var editingCategoryId by remember { mutableStateOf<Long?>(null) }
        var editingCategoryName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManageCategoriesDialog = false },
            title = {
                Text(
                    text = "مدیریت دسته‌بندی‌ها",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF1F5F9),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color(0xFF1E293B), // Premium Slate Dark Blue background
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    Text(
                        text = "دسته‌بندی‌های شما در این بخش قابل ویرایش و حذف هستند. در صورت حذف دسته‌بندی، کتاب‌های متعلق به آن به دسته‌بندی «سایر» منتقل خواهند شد.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // List of categories scrollable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isEditing = editingCategoryId == category.id

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editingCategoryName,
                                            onValueChange = { editingCategoryName = it },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedTextColor = Color.White,
                                                focusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFF3B82F6),
                                                unfocusedBorderColor = Color.Gray
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .padding(end = 8.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (editingCategoryName.isNotBlank()) {
                                                    viewModel.renameCategoryAndRemapBooks(category, editingCategoryName.trim())
                                                    editingCategoryId = null
                                                    Toast.makeText(context, "نام دسته‌بندی با موفقیت تغییر کرد.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "تایید", tint = Color.Green)
                                        }
                                        IconButton(onClick = { editingCategoryId = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "لغو", tint = Color.Red)
                                        }
                                    } else {
                                        Text(
                                            text = category.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Right
                                        )

                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                editingCategoryId = category.id
                                                editingCategoryName = category.name
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "ویرایش نام", tint = Color(0xFF60A5FA))
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCategoryAndRemapBooks(category)
                                                Toast.makeText(context, "دسته‌بندی ${category.name} حذف شد.", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف دسته‌بندی", tint = Color(0xFFF87171))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Add category input at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            placeholder = { Text("نام دسته‌بندی جدید...", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .padding(end = 8.dp)
                        )
                        Button(
                            onClick = {
                                if (categoryNameInput.isNotBlank()) {
                                    val name = categoryNameInput.trim()
                                    viewModel.addCategory(name)
                                    categoryNameInput = ""
                                    Toast.makeText(context, "دسته‌بندی $name اضافه شد.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("افزودن", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showManageCategoriesDialog = false }
                ) {
                    Text("بستن", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // dialog: Manage Notes (دفترچه یادداشت)
    if (showManageNotesDialog) {
        var editingNoteId by remember { mutableStateOf<Long?>(null) }
        var editingNoteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManageNotesDialog = false },
            title = {
                Text(
                    text = "دفترچه یادداشت‌های من",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF1F5F9),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color(0xFF1E293B), // Premium Slate Dark Blue background
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "لیست تمام یادداشت‌های شما در کتابخانه. برای رفتن مستقیم به صفحه هر یادداشت، روی عنوان کتاب کلیک کنید.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (notes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هنوز یادداشتی ثبت نشده است. هنگام مطالعه کتاب‌ها، می‌توانید برای هر صفحه نکته یا یادداشت بنویسید.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            notes.forEach { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Jump to book page button
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            val targetBook = viewModel.getBookById(note.bookId)
                                                            if (targetBook != null) {
                                                                viewModel.openBookAtPage(context, targetBook, note.pageNumber - 1)
                                                                showManageNotesDialog = false
                                                            } else {
                                                                Toast.makeText(context, "کتاب این یادداشت یافت نشد.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Book,
                                                    contentDescription = "کتاب",
                                                    tint = Color(0xFF60A5FA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${note.bookTitle} (صفحه ${note.pageNumber})",
                                                    color = Color(0xFF60A5FA),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Right
                                                )
                                            }

                                            // Actions
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (editingNoteId == note.id) {
                                                    IconButton(
                                                        onClick = {
                                                            if (editingNoteText.isNotBlank()) {
                                                                viewModel.updateNoteContent(note, editingNoteText)
                                                                editingNoteId = null
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = "تایید", tint = Color.Green, modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { editingNoteId = null },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "لغو", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                    }
                                                } else {
                                                    IconButton(
                                                        onClick = {
                                                            editingNoteId = note.id
                                                            editingNoteText = note.content
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteNoteById(note.id)
                                                            Toast.makeText(context, "یادداشت با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (editingNoteId == note.id) {
                                            OutlinedTextField(
                                                value = editingNoteText,
                                                onValueChange = { editingNoteText = it },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedTextColor = Color.White,
                                                    focusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF3B82F6),
                                                    unfocusedBorderColor = Color.Gray
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = false,
                                                maxLines = 4
                                            )
                                        } else {
                                            Text(
                                                text = note.content,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp),
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showManageNotesDialog = false }
                ) {
                    Text("بستن", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // dialog: Add PDF Book
    if (showAddBookDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookDialog = false },
            title = {
                Text(
                    text = "وارد کردن کتاب PDF جدید",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Step 1: Browse PDF File selection
                    Text(
                        text = "۱. فایل پی‌دی‌اف مورد نظر را انتخاب کنید:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedFileUri == null) {
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("انتخاب فایل از حافظه گوشی", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedFileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedFileUri = null
                                        selectedFileName = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف فایل انتخابی")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 2: Book Title
                    Text(
                        text = "۲. عنوان کتاب:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookTitleInput,
                        onValueChange = { bookTitleInput = it },
                        placeholder = { Text("مثال: فیزیک هالیدی") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 3: Classify / Category selection
                    Text(
                        text = "۳. طبقه‌بندی و دسته‌بندی کتاب:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Simple Scrollable selector row for dialogue category pick
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEach { category ->
                            val isSelectedInDialog = bookCategorySelection == category.name
                            InputChip(
                                selected = isSelectedInDialog,
                                onClick = { bookCategorySelection = category.name },
                                label = { Text(category.name) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        if (categories.isEmpty()) {
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = { Text("سایر") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedFileUri
                        if (uri == null) {
                            Toast.makeText(context, "لطفاً ابتدا یک فایل PDF انتخاب کنید.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val title = bookTitleInput.trim()
                        if (title.isBlank()) {
                            Toast.makeText(context, "عنوان کتاب نمی‌تواند خالی باشد.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Trigger copying and saving
                        viewModel.importPdf(context, uri, title, bookCategorySelection)
                        showAddBookDialog = false
                        Toast.makeText(context, "کتاب '$title' درحال قرارگیری در قفسه...", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("افزودن به قفسه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // dialog: Change Book Category
    if (bookToChangeCategory != null) {
        val targetBook = bookToChangeCategory!!
        var customCategoryName by remember { mutableStateOf("") }
        var selectedCatName by remember { mutableStateOf(targetBook.category) }

        AlertDialog(
            onDismissRequest = { bookToChangeCategory = null },
            title = {
                Text(
                    text = "تغییر دسته‌بندی کتاب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF1F5F9),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color(0xFF1E293B),
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "دسته‌بندی مورد نظر را برای کتاب '${targetBook.title}' انتخاب کنید یا یک دسته‌بندی جدید بسازید:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // List existing categories as radio-like clickable chips or rows
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCatName == category.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A))
                                    .clickable { 
                                        selectedCatName = category.name 
                                        customCategoryName = ""
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = category.name,
                                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Textfield for new custom category
                    Text(
                        text = "یا یک دسته‌بندی جدید بنویسید:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = { 
                            customCategoryName = it 
                            if (it.isNotBlank()) {
                                selectedCatName = "" // clear choice to prefer typed
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right, color = Color.White),
                        placeholder = { 
                            Text(
                                "نام دسته‌بندی جدید...", 
                                color = Color.Gray, 
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCategory = if (customCategoryName.isNotBlank()) customCategoryName.trim() else selectedCatName
                        if (finalCategory.isNotBlank()) {
                            viewModel.changeBookCategory(targetBook, finalCategory)
                            Toast.makeText(context, "دسته‌بندی کتاب ویرایش شد.", Toast.LENGTH_SHORT).show()
                            bookToChangeCategory = null
                        } else {
                            Toast.makeText(context, "لطفاً یک دسته‌بندی را انتخاب یا تایپ کنید.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("ویرایش", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToChangeCategory = null }) {
                    Text("انصراف", color = Color(0xFF94A3B8))
                }
            }
        )
    }
    }
}

// Widget represent pdf books on bookshelf
@Composable
fun BookShelfItem(
    book: PdfBook,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onChangeCategory: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    // Dynamic color container configs based on category to give the premium catalog feeling
    val (iconBg, iconFg) = when (book.category) {
        "شعر و ادبیات" -> Pair(Color(0xFF312E81), Color(0xFFC7D2FE)) // indigo deep variations
        "داستان و رمان" -> Pair(Color(0xFF881337), Color(0xFFFECDD3)) // rose variations
        "آموزشی و درسی" -> Pair(Color(0xFF1E3A8A), Color(0xFFBFDBFE)) // blue variations
        else -> Pair(Color(0xFF334155), Color(0xFFE2E8F0)) // slate variations
    }

    val iconImage = when (book.category) {
        "شعر و ادبیات" -> Icons.Default.AutoAwesome
        "داستان و رمان" -> Icons.Default.MenuBook
        "آموزشی و درسی" -> Icons.Default.School
        else -> Icons.Default.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("book_card_${book.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B) // slate dark card background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)) // clean premium edge border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Document book file icon and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large styled document icon utilizing category themes
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconImage,
                        contentDescription = null,
                        tint = iconFg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Dropdown Context Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "کنترل بیشتر",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("مطالعه کتاب", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onOpen()
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.MenuBook, 
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                ) 
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (book.isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                                    color = Color.White
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (book.isFavorite) Color.Red else Color(0xFF94A3B8)
                                )
                            }
                        )
                        if (onChangeCategory != null) {
                            DropdownMenuItem(
                                text = { Text("تغییر دسته‌بندی", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onChangeCategory()
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Folder, 
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    ) 
                                }
                            )
                        }
                        HorizontalDivider(color = Color(0xFF334155))
                        DropdownMenuItem(
                            text = { Text("حذف کتاب", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Book Title with strong professional layout placement
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                color = Color(0xFFF1F5F9), // Silver White
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Category Badge Tag wrapped like chip button
            Box(
                modifier = Modifier
                    .background(
                        iconBg.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = book.category,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = iconFg
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reading Progress Bar Indicator matching the elegant styling
            val progressPercent = if (book.totalPages > 0) {
                ((book.lastPageRead + 1).toFloat() / book.totalPages.toFloat())
            } else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صفحه ${book.lastPageRead + 1} از ${book.totalPages}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${(progressPercent * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF334155),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (book.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "موردعلاقه",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = formatFileSize(book.fileSize),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8).copy(alpha = 0.8f)
                )
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}

fun formatPersianNumber(number: Int): String {
    val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return number.toString().map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}

fun formatPersianNumber(str: String): String {
    val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return str.map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}
