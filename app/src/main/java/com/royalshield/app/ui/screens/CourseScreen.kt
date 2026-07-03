package com.royalshield.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.components.CyberButtonRound
import com.royalshield.app.ui.theme.SafeGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Data Models
data class Course(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val lessons: List<Lesson>,
    val instructor: String,
    val rating: Float,
    val studentsEnrolled: Int,
    val thumbnailRes: Int,
    val level: CourseLevel,
    val prerequisites: List<String> = emptyList(),
    val isLocked: Boolean = false,
    val category: String
)

data class Lesson(
    val id: String,
    val title: String,
    val duration: String,
    val type: LessonType,
    val content: String,
    val videoUrl: String? = null,
    val quiz: Quiz? = null,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = false
)

data class Quiz(
    val id: String,
    val title: String,
    val questions: List<Question>,
    val passingScore: Int = 70
)

data class Question(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

enum class LessonType {
    VIDEO, TEXT, QUIZ, PRACTICAL
}

enum class CourseLevel {
    BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
}

data class CourseProgress(
    val courseId: String,
    val lessonsCompleted: Int,
    val totalLessons: Int,
    val quizzesPassed: Int,
    val averageScore: Float,
    val lastAccessedLesson: String? = null,
    val certificateEarned: Boolean = false
)

data class Certificate(
    val courseId: String,
    val courseName: String,
    val studentName: String,
    val completionDate: String,
    val score: Float,
    val certificateId: String
)

data class CourseComment(
    val id: String,
    val courseId: String,
    val userName: String,
    val userAvatar: String,
    val comment: String,
    val rating: Float,
    val timestamp: String,
    val replies: List<CommentReply> = emptyList()
)

data class CommentReply(
    val id: String,
    val userName: String,
    val comment: String,
    val timestamp: String
)

data class LearningStats(
    val totalCoursesEnrolled: Int,
    val coursesCompleted: Int,
    val totalHoursLearned: Float,
    val averageScore: Float,
    val certificatesEarned: Int,
    val currentStreak: Int,
    val longestStreak: Int
)

// Simple ViewModel inline
class CourseViewModel {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()
    
    private val _courseProgress = MutableStateFlow<Map<String, CourseProgress>>(emptyMap())
    val courseProgress: StateFlow<Map<String, CourseProgress>> = _courseProgress.asStateFlow()
    
    private val _learningStats = MutableStateFlow(
        LearningStats(
            totalCoursesEnrolled = 0,
            coursesCompleted = 0,
            totalHoursLearned = 0f,
            averageScore = 0f,
            certificatesEarned = 0,
            currentStreak = 0,
            longestStreak = 0
        )
    )
    val learningStats: StateFlow<LearningStats> = _learningStats.asStateFlow()
    
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()
    
    private val _comments = MutableStateFlow<List<CourseComment>>(emptyList())
    val comments: StateFlow<List<CourseComment>> = _comments.asStateFlow()
    
    private val _certificates = MutableStateFlow<List<Certificate>>(emptyList())
    val certificates: StateFlow<List<Certificate>> = _certificates.asStateFlow()
    
    init {
        loadMockData()
    }
    
    private fun loadMockData() {
        _courses.value = listOf(
            Course(
                id = "course_1",
                title = "Cybersecurity Fundamentals",
                description = "Master the essential concepts of modern cybersecurity, from device hardening to network privacy.",
                duration = "4 hours",
                instructor = "Elite Security Team",
                rating = 4.9f,
                studentsEnrolled = 3500,
                thumbnailRes = com.royalshield.app.R.drawable.bg_course_bg_cyber,
                level = CourseLevel.BEGINNER,
                category = "Fundamentals",
                lessons = listOf(
                    Lesson("l1", "The Modern Threat Landscape", "15 min", LessonType.VIDEO, "Exploring zero-day exploits and state-sponsored attacks...", videoUrl = "https://www.youtube.com/watch?v=inWWhr5tnEA"),
                    Lesson("l2", "Device Hardening 101", "20 min", LessonType.TEXT, "Step-by-step guide to securing your Android device: disabling unused services, encrypting storage, and managing permissions."),
                    Lesson("l3", "Network Shielding", "25 min", LessonType.VIDEO, "Why public WiFi is dangerous and how to use the Royal VPN effectively.", videoUrl = "https://www.youtube.com/watch?v=7g-v6HRI86o"),
                    Lesson("l4", "Core Assessment", "15 min", LessonType.QUIZ, "", quiz = Quiz("q1", "Security Basics Quiz", 
                        listOf(
                            Question("q1_1", "What is the primary goal of MFA?", listOf("Speed", "Multiple layers of defense", "Decoration", "Storage"), 1, "MFA adds extra layers to verify identity."),
                            Question("q1_2", "Which protocol is most secure for VPNs?", listOf("PPTP", "L2TP", "WireGuard", "HTTP"), 2, "WireGuard is modern, fast, and extremely secure.")
                        )
                    ))
                )
            ),
            Course(
                id = "course_2",
                title = "AI & Voice Scam Protection",
                description = "Learn how to detect synthetic voices and deepfake scams targeting individuals and businesses.",
                duration = "3 hours",
                instructor = "AI Research Lab",
                rating = 5.0f,
                studentsEnrolled = 1200,
                thumbnailRes = com.royalshield.app.R.drawable.card_automation,
                level = CourseLevel.INTERMEDIATE,
                category = "AI Security",
                lessons = listOf(
                    Lesson("l1", "Deepfake Audio Analysis", "30 min", LessonType.VIDEO, "How AI models generate voices and what cadence errors to look for.", videoUrl = "https://www.youtube.com/watch?v=uK1_v67fXoM"),
                    Lesson("l2", "The 3-Second Verification Rule", "15 min", LessonType.TEXT, "The 'Challenge-Response' technique: Asking questions an AI wouldn't know."),
                    Lesson("l3", "Social Engineering via AI", "20 min", LessonType.VIDEO, "Real-world examples of AI-driven phishing.")
                )
            ),
            Course(
                id = "course_3",
                title = "Secure Communication Lab",
                description = "Advanced guide to PGP encryption, E2EE messaging, and metadata removal.",
                duration = "6 hours",
                instructor = "Privacy Ops",
                rating = 4.8f,
                studentsEnrolled = 950,
                thumbnailRes = com.royalshield.app.R.drawable.privacy_advisor_bg,
                level = CourseLevel.ADVANCED,
                category = "Privacy",
                lessons = listOf(
                    Lesson("l1", "PGP Mastery", "45 min", LessonType.VIDEO, "Setting up keys and signing messages."),
                    Lesson("l2", "Metadata Sanitization", "30 min", LessonType.PRACTICAL, "Removing EXIF data from photos and documents.")
                )
            )
        )
        
        _courseProgress.value = mapOf(
            "course_1" to CourseProgress("course_1", 2, 4, 0, 0f, "l2"),
            "course_3" to CourseProgress("course_3", 1, 2, 0, 0f, "l1")
        )
        
        _comments.value = listOf(
            CourseComment(
                "c1", "course_1", "Sarah M.", "", 
                "Excellent course! Very comprehensive and well-structured.", 
                5f, "2 days ago"
            )
        )
        
        updateLearningStats()
    }
    
    fun selectCourse(courseId: String) {
        _selectedCourse.value = _courses.value.find { it.id == courseId }
    }
    
    fun completeLesson(courseId: String, lessonId: String) {
        val currentProgress = _courseProgress.value[courseId] ?: return
        val updatedProgress = currentProgress.copy(
            lessonsCompleted = currentProgress.lessonsCompleted + 1,
            lastAccessedLesson = lessonId
        )
        _courseProgress.value = _courseProgress.value.toMutableMap().apply {
            put(courseId, updatedProgress)
        }
        updateLearningStats()
    }
    
    fun submitQuiz(courseId: String, quizId: String, score: Float) {
        val currentProgress = _courseProgress.value[courseId] ?: return
        val passed = score >= 70
        
        if (passed) {
            val updatedProgress = currentProgress.copy(
                quizzesPassed = currentProgress.quizzesPassed + 1,
                averageScore = (currentProgress.averageScore + score) / 2
            )
            _courseProgress.value = _courseProgress.value.toMutableMap().apply {
                put(courseId, updatedProgress)
            }
        }
        updateLearningStats()
    }
    
    fun addComment(courseId: String, comment: String, rating: Float) {
        val newComment = CourseComment(
            id = "comment_${System.currentTimeMillis()}",
            courseId = courseId,
            userName = "Current User",
            userAvatar = "",
            comment = comment,
            rating = rating,
            timestamp = "Just now"
        )
        _comments.value = _comments.value + newComment
    }
    
    private fun updateLearningStats() {
        val enrolled = _courseProgress.value.size
        val completed = _courseProgress.value.values.count { 
            it.lessonsCompleted >= (_courses.value.find { c -> c.id == it.courseId }?.lessons?.size ?: 0)
        }
        val avgScore = _courseProgress.value.values.map { it.averageScore }.average().toFloat()
        val totalHours = _courses.value.sumOf { 
            it.lessons.sumOf { lesson -> lesson.duration.replace(" min", "").toIntOrNull() ?: 0 }
        } / 60f
        
        _learningStats.value = LearningStats(
            totalCoursesEnrolled = enrolled,
            coursesCompleted = completed,
            totalHoursLearned = totalHours,
            averageScore = if (avgScore.isNaN()) 0f else avgScore,
            certificatesEarned = _certificates.value.size,
            currentStreak = 5,
            longestStreak = 12
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(onBackPressed: () -> Unit) {
    // Using remember to create viewModel instance
    val viewModel = remember { CourseViewModel() }
    val courses by viewModel.courses.collectAsState()
    val stats by viewModel.learningStats.collectAsState()
    val progress by viewModel.courseProgress.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    
    val selectedCourse = courses.find { it.id == selectedCourseId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = RoyalGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedCourseId == null) "Cyber Academy" else "Course Details", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedCourseId != null) {
                            selectedCourseId = null
                        } else {
                            onBackPressed()
                        }
                    }) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_back_gold),
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = RoyalGold
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedCourseId == null) {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = RoyalGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Courses") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("My Learning") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Certificates") }
                    )
                }
                
                when (selectedTab) {
                    0 -> CoursesListTab(courses, progress, onCourseClick = { id ->
                        selectedCourseId = id
                    })
                    1 -> MyLearningTab(stats, courses, progress)
                    2 -> CertificatesTab(viewModel)
                }
            } else {
                // Course Detail View
                selectedCourse?.let { course ->
                    CourseDetailView(
                        course = course,
                        progress = progress[course.id],
                        onBack = { selectedCourseId = null }
                    )
                }
            }
        }
    }
}

@Composable
fun CourseDetailView(course: Course, progress: CourseProgress?, onBack: () -> Unit) {
    var showIntroDialog by remember { mutableStateOf(false) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(course.title, style = MaterialTheme.typography.headlineSmall, color = RoyalGold, fontWeight = FontWeight.Bold)
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { showIntroDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = course.thumbnailRes,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            alpha = 0.5f
                        )
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Watch Introduction", 
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { 
                        // Start with first lesson
                        course.lessons.firstOrNull()?.let { firstLesson ->
                            selectedLesson = firstLesson
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("START COURSE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            
            item {
                Text("Description", style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(course.description, color = Color.White)
            }

            item {
                Text("Curriculum", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            items(course.lessons) { lesson ->
                LessonItem(
                    lesson = lesson,
                    onClick = {
                        if (!lesson.isLocked) {
                            selectedLesson = lesson
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "🔒 Complete previous lessons to unlock",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
        
        // Introduction Dialog - Outside LazyColumn
        if (showIntroDialog) {
            AlertDialog(
                onDismissRequest = { showIntroDialog = false },
                confirmButton = {
                    TextButton(onClick = { showIntroDialog = false }) {
                        Text("CLOSE", color = RoyalGold)
                    }
                },
                title = {
                    Text("Course Introduction", color = RoyalGold, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            "🎓 ${course.title}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            course.description,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "What you'll learn:",
                            color = RoyalGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        course.lessons.take(3).forEach { lesson ->
                            Text(
                                "• ${lesson.title}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                textContentColor = Color.White
            )
        }
        
        // Lesson Content Dialog - Outside LazyColumn
        selectedLesson?.let { lesson ->
            AlertDialog(
                onDismissRequest = { selectedLesson = null },
                confirmButton = {
                    TextButton(onClick = { selectedLesson = null }) {
                        Text("CLOSE", color = RoyalGold)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(lesson.type) {
                                LessonType.VIDEO -> Icons.Default.PlayCircle
                                LessonType.QUIZ -> Icons.Default.Quiz
                                else -> Icons.Default.Article
                            },
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lesson.title, color = RoyalGold, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            "⏱️ Duration: ${lesson.duration}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        when (lesson.type) {
                            LessonType.VIDEO -> {
                                var isPlaying by remember { mutableStateOf(false) }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                        .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPlaying && !lesson.videoUrl.isNullOrEmpty()) {
                                        // Real Video Player using WebView for flexibility (YouTube/Direct)
                                        androidx.compose.ui.viewinterop.AndroidView(
                                            factory = { ctx ->
                                                android.webkit.WebView(ctx).apply {
                                                    settings.javaScriptEnabled = true
                                                    settings.mediaPlaybackRequiresUserGesture = false
                                                    webViewClient = android.webkit.WebViewClient()
                                                    val videoHtml = if (lesson.videoUrl!!.contains("youtube.com") || lesson.videoUrl!!.contains("youtu.be")) {
                                                        val videoId = lesson.videoUrl!!.substringAfterLast("/").substringAfterLast("v=")
                                                        "<html><body style='margin:0;padding:0;background:black;'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/$videoId?autoplay=1' frameborder='0' allowfullscreen></iframe></body></html>"
                                                    } else {
                                                        "<html><body style='margin:0;padding:0;background:black;'><video width='100%' height='100%' controls autoplay><source src='${lesson.videoUrl}' type='video/mp4'></video></body></html>"
                                                    }
                                                    loadData(videoHtml, "text/html", "utf-8")
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // Placeholder with active background
                                        com.royalshield.app.ui.components.HolographicWaveBackground(
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CyberButtonRound(
                                                size = 80.dp,
                                                icon = Icons.Default.PlayArrow,
                                                onClick = { isPlaying = true }
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("NEURAL_LINK STREAM", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp)
                                            Text(if (lesson.videoUrl.isNullOrEmpty()) "Generating Academic Simulation..." else "Encrypted Feed Available", 
                                                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                    
                                    // Decorative Scanline Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isPlaying) 0.dp else 2.dp)
                                            .align(Alignment.TopCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(RoyalGold.copy(alpha = 0.5f), Color.Transparent)
                                                )
                                            )
                                    )
                                }
                            }
                            LessonType.QUIZ -> {
                                Surface(
                                    color = Color(0xFF252525),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("✅ Quiz: ${lesson.title}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Question 1: What is phishing?", color = Color.White)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("A) Fishing for data", color = Color.Gray, fontSize = 14.sp)
                                        Text("B) Email scam", color = Color.Gray, fontSize = 14.sp)
                                        Text("C) Network attack", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    lesson.content,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "📖 This lesson covers the fundamentals and best practices for understanding cybersecurity threats.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun LessonItem(lesson: Lesson, onClick: () -> Unit = {}) {
    Surface(
        color = Color(0xFF161616),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !lesson.isLocked, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when(lesson.type) {
                        LessonType.VIDEO -> Icons.Default.PlayCircle
                        LessonType.QUIZ -> Icons.Default.Quiz
                        else -> Icons.Default.Article
                    },
                    contentDescription = null,
                    tint = if (lesson.isLocked) Color.Gray else RoyalGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(lesson.title, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(lesson.duration, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Icon(
                imageVector = if (lesson.isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun AsyncImage(model: Any, contentDescription: String?, modifier: Modifier, contentScale: androidx.compose.ui.layout.ContentScale, alpha: Float) {
    // Substitute for missing AsyncImage if needed, using Image for local resources
    if (model is Int) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = model),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha
        )
    }
}

@Composable
fun CoursesListTab(
    courses: List<Course>,
    progress: Map<String, CourseProgress>,
    onCourseClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "AVAILABLE COURSES",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        items(courses) { course ->
            CourseCard(course, progress[course.id], onClick = { onCourseClick(course.id) })
        }
    }
}

@Composable
fun CourseCard(course: Course, progress: CourseProgress?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .clickable(onClick = if (course.isLocked) {{}} else onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1a1a2e),
                            Color(0xFF0f0f1e),
                            Color.Black
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA500),
                            Color(0xFFFFD700),
                            Color(0xFFC9A961),
                            Color(0xFFFFD700)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Background Image for Fundamentals course
            if (course.title.contains("Fundamentals", ignoreCase = true)) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.course_bg_cyber),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.4f
                )
                // Dark overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }
            
            // Background Image for Business courses
            if (course.title.contains("Business", ignoreCase = true)) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.course_bg_business),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.5f
                )
                // Subtle overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Section
                Column {
                    // Level Badge
                    Surface(
                        color = when (course.level) {
                            CourseLevel.BEGINNER -> Color(0xFF4CAF50)
                            CourseLevel.INTERMEDIATE -> Color(0xFFFFD700)
                            CourseLevel.ADVANCED -> Color(0xFFFF6B00)
                            CourseLevel.EXPERT -> Color(0xFFE91E63)
                        }.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            when (course.level) {
                                CourseLevel.BEGINNER -> Color(0xFF4CAF50)
                                CourseLevel.INTERMEDIATE -> Color(0xFFFFD700)
                                CourseLevel.ADVANCED -> Color(0xFFFF6B00)
                                CourseLevel.EXPERT -> Color(0xFFE91E63)
                            }
                        ),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    ) {
                        Text(
                            text = course.level.name,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = when (course.level) {
                                CourseLevel.BEGINNER -> Color(0xFF4CAF50)
                                CourseLevel.INTERMEDIATE -> Color(0xFFFFD700)
                                CourseLevel.ADVANCED -> Color(0xFFFF6B00)
                                CourseLevel.EXPERT -> Color(0xFFE91E63)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Title
                    Text(
                        text = course.title,
                        color = Color(0xFFFFD700),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        lineHeight = 28.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Description
                    Text(
                        text = course.description,
                        color = Color(0xFFB8B8B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                // Footer Section
                Column {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFFFD700).copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Instructor
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = course.instructor,
                                color = Color(0xFF9E9E9E),
                                fontSize = 12.sp
                            )
                        }
                        
                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = course.rating.toString(),
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Duration
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = course.duration,
                                color = Color(0xFF9E9E9E),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // Progress Bar (if enrolled)
                    if (progress != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Progress",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 11.sp
                                )
                                Text(
                                    "${(progress.lessonsCompleted.toFloat() / progress.totalLessons * 100).toInt()}%",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF2a2a3e))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.lessonsCompleted.toFloat() / progress.totalLessons)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFFFD700),
                                                    Color(0xFFFFA500)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
            
            // Lock Overlay
            if (course.isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "PREMIUM CONTENT",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyLearningTab(stats: LearningStats, courses: List<Course>, progress: Map<String, CourseProgress>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "LEARNING STATISTICS",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Courses", stats.totalCoursesEnrolled.toString(), Modifier.weight(1f))
                StatCard("Completed", stats.coursesCompleted.toString(), Modifier.weight(1f))
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Hours", String.format("%.1f", stats.totalHoursLearned), Modifier.weight(1f))
                StatCard("Avg Score", "${stats.averageScore.toInt()}%", Modifier.weight(1f))
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Certificates", stats.certificatesEarned.toString(), Modifier.weight(1f), Icons.Default.EmojiEvents)
                StatCard("Streak", "${stats.currentStreak} days", Modifier.weight(1f), Icons.Default.LocalFireDepartment)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "IN PROGRESS",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        items(courses.filter { progress.containsKey(it.id) }) { course ->
            CourseProgressCard(course, progress[course.id]!!)
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.TrendingUp
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = RoyalGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun CourseProgressCard(course: Course, progress: CourseProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = course.thumbnailRes),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(course.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${progress.lessonsCompleted}/${progress.totalLessons} lessons",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.lessonsCompleted.toFloat() / progress.totalLessons },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = RoyalGold,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun CertificatesTab(viewModel: CourseViewModel) {
    val certificates by viewModel.certificates.collectAsState()
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "MY CERTIFICATES",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        
        if (certificates.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No certificates yet",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Complete a course to earn your first certificate",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(certificates) { certificate ->
                CertificateCard(certificate)
            }
        }
    }
}

@Composable
fun CertificateCard(certificate: Certificate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            RoyalGold.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        null,
                        tint = RoyalGold,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text(
                        certificate.certificateId,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Certificate of Completion",
                    color = RoyalGold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    certificate.courseName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Awarded to", color = Color.Gray, fontSize = 11.sp)
                        Text(certificate.studentName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Score", color = Color.Gray, fontSize = 11.sp)
                        Text("${certificate.score.toInt()}%", color = RoyalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Completed on ${certificate.completionDate}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { /* Download certificate */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Certificate", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
