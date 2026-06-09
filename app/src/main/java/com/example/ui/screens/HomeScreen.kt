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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val books by viewModel.filteredBooks.collectAsState()
    val categories by viewModel.dbCategories.collectAsState()

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${books.size} کتاب پیدا شد",
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
                        // Action buttons to add book
                        Button(
                            onClick = {
                                viewModel.prepopulateSampleBooksIfEmpty(context)
                                Toast.makeText(context, "کتاب‌های نمونه ادبیات فارسی اضافه شدند!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بارگذاری کتاب‌های نمونه (دیوان حافظ و شازده کوچولو)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                            }
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
                        val defaultCategories = listOf("شعر و ادبیات", "داستان و رمان", "آموزشی و درسی", "سایر")
                        categories.forEach { category ->
                            val isDefault = category.name in defaultCategories
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
                                                if (editingCategoryName.isNotBlank() && editingCategoryName.trim() !in defaultCategories) {
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
                                            text = category.name + (if (isDefault) " (پیش‌فرض)" else ""),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Right
                                        )

                                        if (!isDefault) {
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
}

// Widget represent pdf books on bookshelf
@Composable
fun BookShelfItem(
    book: PdfBook,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
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
