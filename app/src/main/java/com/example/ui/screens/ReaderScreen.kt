package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewStream
import com.example.data.model.PdfBook
import com.example.ui.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.activeBookPages.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    val isPageLoading by viewModel.isPageLoading.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()
    val isVerticalLayout by viewModel.isVerticalLayout.collectAsState()

    val book = activeBook ?: return

    // Reading Paper Theme Custom styling variables
    val (schemeBackground, schemeCard, schemeText) = when (readingTheme) {
        "Sepia" -> Triple(
            Color(0xFFFAF2E1), // Soft old cream yellow background
            Color(0xFFFFFDF5), // Inside page card warm white
            Color(0xFF3C2F15)  // Coffee brown text color
        )
        "Dark" -> Triple(
            Color(0xFF121212), // Deep black background
            Color(0xFF1E1E1E), // Dark grey page card
            Color(0xFFE0E0E0)  // Bright text
        )
        else -> Triple(
            Color(0xFFF3F4F9), // Pure clean light blue/grey backdrop to match Professional Polish background
            Color(0xFFFFFFFF), // Crisp white paper
            Color(0xFF1A1A1A)  // Dark body text
        )
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showPageNoteDialog by remember { mutableStateOf(false) }
    val notes by viewModel.allNotes.collectAsState()
    val pageNote = remember(notes, book.id, currentPage) {
        notes.find { it.bookId == book.id && it.pageNumber == (currentPage + 1) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(schemeBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Reading Header (TopBar)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = schemeCard,
                    tonalElevation = 6.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left buttons: Actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت",
                                    tint = schemeText
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Book title
                            Column {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = schemeText,
                                    maxLines = 1
                                )
                                Text(
                                    text = book.category,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = schemeText.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Right buttons: View options
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Toggle reading layout (Continuous Scroll / Page-by-Page)
                            IconButton(onClick = { 
                                viewModel.setVerticalLayout(!isVerticalLayout) 
                                scale = 1f
                                offset = Offset.Zero
                            }) {
                                Icon(
                                    imageVector = if (isVerticalLayout) Icons.Default.MenuBook else Icons.Default.SwapVert,
                                    contentDescription = "تغییر حالت نمایش",
                                    tint = schemeText
                                )
                            }

                            // Bookmark this page
                            IconButton(onClick = { viewModel.toggleBookmark() }) {
                                val isBookmarked = viewModel.isPageBookmarked(currentPage)
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "نشانه‌گذاری صفحه",
                                    tint = if (isBookmarked) Color(0xFFD4AF37) else schemeText
                                )
                            }

                            // Page Note Button
                            IconButton(onClick = { showPageNoteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "یادداشت صفحه",
                                    tint = if (pageNote != null) Color(0xFF3B82F6) else schemeText
                                )
                            }

                            // Toggle theme (Light -> Sepia -> Dark)
                            IconButton(onClick = {
                                val nextTheme = when (readingTheme) {
                                    "Light" -> "Sepia"
                                    "Sepia" -> "Dark"
                                    else -> "Light"
                                }
                                viewModel.setReadingTheme(nextTheme)
                            }) {
                                Icon(
                                    imageVector = when (readingTheme) {
                                        "Light" -> Icons.Filled.LightMode
                                        "Sepia" -> Icons.Filled.Palette
                                        else -> Icons.Filled.DarkMode
                                    },
                                    contentDescription = "تم صفحه",
                                    tint = schemeText
                                )
                            }

                            // Reset Zoom / Zoom toggle
                            IconButton(onClick = { 
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 1.6f
                                }
                            }) {
                                Icon(
                                    imageVector = if (scale > 1f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                    contentDescription = if (scale > 1f) "کوچک‌نمایی" else "بزرگنمایی",
                                    tint = schemeText
                                )
                            }
                        }
                    }
                }
            }

            // PDF Render Area Viewport
            if (isVerticalLayout) {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
                val visibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                
                LaunchedEffect(visibleItemIndex) {
                    if (visibleItemIndex in 0 until totalPages && visibleItemIndex != currentPage) {
                        viewModel.changePage(visibleItemIndex)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(schemeBackground)
                        .clickable { isFullScreen = !isFullScreen }
                        .onSizeChanged { viewportSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(totalPages) { pageIndex ->
                            PdfPageRowItem(
                                pageIndex = pageIndex,
                                viewModel = viewModel,
                                schemeCard = schemeCard,
                                isBookmarked = viewModel.isPageBookmarked(pageIndex)
                            )
                        }
                    }

                    // Full screen continuous exit overlay
                    if (isFullScreen) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "نمایش پیوسته  |  جهت بازگشت لمس کنید",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isFullScreen = !isFullScreen },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                var isMultiTouch = false
                                var totalDragX = 0f
                                var swipeTriggered = false
                                
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size > 1) {
                                        isMultiTouch = true
                                    }
                                    
                                    val panChange = event.calculatePan()
                                    val zoomChange = event.calculateZoom()
                                    
                                    if (scale > 1f || zoomChange != 1f) {
                                        val prevScale = scale
                                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            val width = if (viewportSize.width > 0) viewportSize.width.toFloat() else 1080f
                                            val height = if (viewportSize.height > 0) viewportSize.height.toFloat() else 1920f
                                            val maxActiveX = (scale - 1f) * (width / 2f)
                                            val maxActiveY = (scale - 1f) * (height / 2f)
                                            offset = Offset(
                                                x = (offset.x + panChange.x).coerceIn(-maxActiveX, maxActiveX),
                                                y = (offset.y + panChange.y).coerceIn(-maxActiveY, maxActiveY)
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                        event.changes.forEach { if (it.pressed) it.consume() }
                                    } else {
                                        if (!isMultiTouch && !swipeTriggered && panChange.x != 0f) {
                                            totalDragX += panChange.x
                                            val threshold = 120f // pixels swipe distance
                                            if (totalDragX > threshold) {
                                                swipeTriggered = true
                                                if (currentPage > 0) {
                                                    viewModel.changePage(currentPage - 1)
                                                }
                                            } else if (totalDragX < -threshold) {
                                                swipeTriggered = true
                                                if (currentPage + 1 < totalPages) {
                                                    viewModel.changePage(currentPage + 1)
                                                }
                                            }
                                        }
                                        event.changes.forEach { if (it.pressed) it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPageLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = if (readingTheme == "Dark") Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "درحال آماده‌سازی صفحه...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = schemeText.copy(alpha = 0.8f)
                            )
                        }
                    } else if (pageBitmap != null) {
                        Card(
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "صفحه پی‌دی‌اف ${currentPage + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Text(
                            text = "خطا در رندر این صفحه از کتاب PDF",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Full screen exit indicator overlay
                    if (isFullScreen) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "حالت تمام‌صفحه  |  جهت بازگشت لمس کنید",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Elegant Bottom controller (Page Slider and Pager actions)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = schemeCard,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Page label and navigation indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Page button (Left Side) -> goes to page - 1
                                IconButton(
                                    onClick = { viewModel.changePage(currentPage - 1) },
                                    enabled = currentPage > 0,
                                    modifier = Modifier.testTag("reader_prev_page")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "صفحه قبل",
                                        tint = if (currentPage > 0) schemeText else schemeText.copy(alpha = 0.3f)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "صفحه ${currentPage + 1} از $totalPages",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        color = schemeText,
                                        textAlign = TextAlign.Center
                                    )
                                    // Bookmark text list if bookmarked
                                    if (viewModel.isPageBookmarked(currentPage)) {
                                        Text(
                                            text = "★ نشانه‌گذاری شده",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFD4AF37),
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Next Page button (Right Side) -> goes to page + 1
                                IconButton(
                                    onClick = { viewModel.changePage(currentPage + 1) },
                                    enabled = currentPage + 1 < totalPages,
                                    modifier = Modifier.testTag("reader_next_page")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "صفحه بعد",
                                        tint = if (currentPage + 1 < totalPages) schemeText else schemeText.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fast scrubber Scroller
                            if (totalPages > 1) {
                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = { viewModel.changePage(it.toInt()) },
                                    valueRange = 0f..(totalPages - 1).toFloat(),
                                    steps = if (totalPages > 2) totalPages - 2 else 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (readingTheme == "Dark") Color.White else MaterialTheme.colorScheme.primary,
                                        activeTrackColor = (if (readingTheme == "Dark") Color.White else MaterialTheme.colorScheme.primary).copy(alpha = 0.8f),
                                        inactiveTrackColor = schemeText.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .testTag("reader_scrubber")
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPageNoteDialog) {
            var noteInput by remember(pageNote) { mutableStateOf(pageNote?.content ?: "") }
            AlertDialog(
                onDismissRequest = { showPageNoteDialog = false },
                title = {
                    Text(
                        text = "یادداشت صفحه ${currentPage + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = schemeText,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = schemeCard,
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "یادداشت خود را برای این صفحه بنویسید:",
                            fontSize = 13.sp,
                            color = schemeText.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text("مثلا: یادداشت من...", fontSize = 13.sp, color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = schemeText,
                                focusedTextColor = schemeText,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = schemeText.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            singleLine = false,
                            maxLines = 6
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveNote(
                                bookId = book.id,
                                bookTitle = book.title,
                                pageNumber = currentPage + 1,
                                content = noteInput
                            )
                            showPageNoteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("ذخیره", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPageNoteDialog = false }) {
                        Text("انصراف", color = schemeText.copy(alpha = 0.7f))
                    }
                }
            )
        }
    }
}

@Composable
fun PdfPageRowItem(
    pageIndex: Int,
    viewModel: BookViewModel,
    schemeCard: Color,
    isBookmarked: Boolean
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pageIndex) {
        isLoading = true
        pageBitmap = viewModel.renderPageToBitmap(pageIndex)
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .onSizeChanged { itemSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val width = if (itemSize.width > 0) itemSize.width.toFloat() else 1080f
                        val height = if (itemSize.height > 0) itemSize.height.toFloat() else 1920f
                        val maxActiveX = (scale - 1f) * (width / 2f)
                        val maxActiveY = (scale - 1f) * (height / 2f)
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxActiveX, maxActiveX),
                            y = (offset.y + pan.y).coerceIn(-maxActiveY, maxActiveY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .fillMaxWidth()
                .heightIn(min = 360.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (pageBitmap != null) {
                Image(
                    bitmap = pageBitmap!!.asImageBitmap(),
                    contentDescription = "صفحه ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Text(
                    text = "خطا در بارگذاری صفحه",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
