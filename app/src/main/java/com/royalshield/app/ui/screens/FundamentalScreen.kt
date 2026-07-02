package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundamentalScreen(onBackPressed: () -> Unit) {
    val courses = remember {
        listOf(
            FundamentalCourse("Phishing 101", "Learn to identify fake emails and scams.", "10 min", Color(0xFFF44336)),
            FundamentalCourse("Password Security", "Create unbreakable passwords.", "15 min", Color(0xFF4CAF50)),
            FundamentalCourse("Safe Browsing", "Spot malicious websites instantly.", "12 min", Color(0xFF2196F3)),
            FundamentalCourse("Social Engineering", "How hackers manipulate trust.", "20 min", Color(0xFFFF9800)),
            FundamentalCourse("2FA Mastery", "Double lock your accounts.", "8 min", Color(0xFF9C27B0))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cybersecurity Fundamentals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Master the Basics",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Essential knowledge to keep your digital life secure.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            items(courses) { course ->
                FundamentalCourseCard(course)
            }
        }
    }
}

data class FundamentalCourse(val title: String, val description: String, val duration: String, val color: Color)

@Composable
fun FundamentalCourseCard(course: FundamentalCourse) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(course.color)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(course.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(course.description, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = Color.White.copy(alpha=0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(course.duration, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                }
            }
        }
    }
}
