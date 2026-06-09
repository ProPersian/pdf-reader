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
    var showAddCategoryDialog by remember { mutableStateOf(false) }

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
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 6.dp)
            ) {
                // Simplified Minimal Header
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
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = "مطالعه راحت، سبک و سازماندهی شده",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF74777F)
                        )
                    }
                    IconButton(
                        onClick = {
                            showAddCategoryDialog = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFF1F3F5),
                            contentColor = Color(0xFF1B1B1F)
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = "افزودن دسته‌بندی جدید",
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
                            color = Color(0xFF74777F)
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "جستجو",
                            tint = Color(0xFF1B1B1F)
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Clear, 
                                    contentDescription = "پاک کردن",
                                    tint = Color(0xFF1B1B1F)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F3F5),
                        unfocusedContainerColor = Color(0xFFF1F3F5),
                        disabledContainerColor = Color(0xFFF1F3F5),
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
                .background(Color.White)
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
                    color = if (isAllSelected) Color(0xFF1B1B1F) else Color(0xFFF1F3F5),
                    contentColor = if (isAllSelected) Color.White else Color(0xFF44474E),
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "همه",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                categories.forEach { category ->
                    val isSelected = selectedCategory == category.name
                    
                    Surface(
                        onClick = { viewModel.selectCategory(category.name) },
                        color = if (isSelected) Color(0xFF1B1B1F) else Color(0xFFF1F3F5),
                        contentColor = if (isSelected) Color.White else Color(0xFF44474E),
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
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Standard default system categories cannot be deleted, custom can
                            val defaultCategories = listOf("شعر و ادبیات", "داستان و رمان", "آموزشی و درسی", "سایر")
                            if (category.name !in defaultCategories) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف دسته‌بندی",
                                    tint = if (isSelected) Color(0xFF001D35) else Color(0xFF74777F),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            viewModel.deleteCategory(category.name)
                                            if (selectedCategory == category.name) {
                                                viewModel.selectCategory("همه")
                                            }
                                            Toast.makeText(context, "دسته‌بندی ${category.name} حذف شد.", Toast.LENGTH_SHORT).show()
                                        }
                                )
                            }
                        }
                    }
                }
            }

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

    // dialog: Add Custom Category
    if (showAddCategoryDialog) {
        var categoryNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = "افزودن دسته‌بندی جدید",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "نامی برای دسته‌بندی و طبقه‌بندی کتاب‌های پی‌دی‌اف بنویسید:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { categoryNameInput = it },
                        placeholder = { Text("مثال: دانشگاه، کاری، رمان") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryNameInput.isNotBlank()) {
                            viewModel.addCategory(categoryNameInput.trim())
                            showAddCategoryDialog = false
                            Toast.makeText(context, "دسته‌بندی جدید اضافه شد.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("انصراف")
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
        "شعر و ادبیات" -> Pair(Color(0xFFEADDFF), Color(0xFF21005D))
        "داستان و رمان" -> Pair(Color(0xFFFFDAD6), Color(0xFF410002))
        "آموزشی و درسی" -> Pair(Color(0xFFD1E4FF), Color(0xFF001D35))
        else -> Pair(Color(0xFFE2F1E8), Color(0xFF1A3020))
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
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECEFF1))
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
                            tint = Color(0xFF74777F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("مطالعه کتاب") },
                            onClick = {
                                showMenu = false
                                onOpen()
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.MenuBook, 
                                    contentDescription = null,
                                    tint = Color(0xFF1B1B1F)
                                ) 
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (book.isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها")
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (book.isFavorite) Color.Red else Color(0xFF1B1B1F)
                                )
                            }
                        )
                        HorizontalDivider()
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
                color = Color(0xFF1B1B1F),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Category Badge Tag wrapped like chip button
            Box(
                modifier = Modifier
                    .background(
                        iconBg.copy(alpha = 0.5f),
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
                        color = Color(0xFF74777F)
                    )
                    Text(
                        text = "${(progressPercent * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D35)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    color = Color(0xFF001D35),
                    trackColor = Color(0xFFE1E2EC),
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
                    color = Color(0xFF74777F).copy(alpha = 0.8f)
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
