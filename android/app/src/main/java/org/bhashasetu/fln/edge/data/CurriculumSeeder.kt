package org.bhashasetu.fln.edge.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Seeds the Room database with NIPUN curriculum data on first launch.
 */
object CurriculumSeeder {
    private const val TAG = "CurriculumSeeder"

    fun seedIfEmpty(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = NipunDatabase.getDatabase(context)
            val dao = db.nipunDao()

            val existing = dao.getMilestonesByGrade(2)
            if (existing.isNotEmpty()) {
                Log.d(TAG, "Database already seeded")
                return@launch
            }

            Log.d(TAG, "Seeding curriculum data...")

            val milestones = listOf(
                NipunMilestone(
                    milestoneId = "FLN-02-MATH-001",
                    developmentalGoal = 1,
                    competencyTextHindi = "बच्चे 1 से 9 तक की संख्याओं को पहचानते और लिखते हैं",
                    competencyTextSantali = "बच्चारे 1 खोन 9 तायें सानिञ ते चिञ्हेयँ आर लिखेयँ",
                    gradeLevel = 2,
                    subject = "mathematics"
                ),
                NipunMilestone(
                    milestoneId = "FLN-02-MATH-002",
                    developmentalGoal = 1,
                    competencyTextHindi = "बच्चे जोड़ और घटाव कर सकते हैं",
                    competencyTextSantali = "बच्चारे जोड़ आर घटाव ते गामिया",
                    gradeLevel = 2,
                    subject = "mathematics"
                ),
                NipunMilestone(
                    milestoneId = "FLN-02-MATH-003",
                    developmentalGoal = 2,
                    competencyTextHindi = "बच्चे आकार और रंग पहचानते हैं",
                    competencyTextSantali = "बच्चारे आकार आर राङ ते चिञ्हेयँ",
                    gradeLevel = 2,
                    subject = "mathematics"
                )
            )

            milestones.forEach { dao.insertMilestone(it) }

            val lessons = listOf(
                LessonScript(
                    lessonId = "LESSON-001",
                    milestoneId = "FLN-02-MATH-001",
                    titleHindi = "संख्याओं का परिचय",
                    titleSantali = "सानिञ रे ञुतुम",
                    contentHindi = "आज हम संख्याओं के बारे में सीखेंगे। एक, दो, तीन...",
                    contentSantali = "आज आमिसित सानिञ रे चेनेयँ। एतबार, बार, तिन...",
                    audioPath = "",
                    gradeLevel = 2,
                    subject = "mathematics"
                ),
                LessonScript(
                    lessonId = "LESSON-002",
                    milestoneId = "FLN-02-MATH-002",
                    titleHindi = "जोड़ सीखें",
                    titleSantali = "जोड़ चेनेयँ",
                    contentHindi = "दो और तीन जोड़ो। पांच होता है।",
                    contentSantali = "बार आर तिन जोड़ो। सेतार गामिया।",
                    audioPath = "",
                    gradeLevel = 2,
                    subject = "mathematics"
                )
            )

            lessons.forEach { dao.insertLesson(it) }

            val worksheets = listOf(
                Worksheet(
                    worksheetId = "WS-001",
                    milestoneId = "FLN-02-MATH-001",
                    titleHindi = "संख्या पहचान कार्यपत्रिका",
                    titleSantali = "सानिञ चिञ्हाक् कार्यपत्रिका",
                    questionsJson = """{"questions": [{"q": "यह कितना है? 🍎🍎🍎", "a": "3"}, {"q": "यह कितना है? 🌟🌟", "a": "2"}]}""",
                    gradeLevel = 2,
                    subject = "mathematics"
                )
            )

            worksheets.forEach { dao.insertWorksheet(it) }

            Log.d(TAG, "Curriculum seeded successfully")
        }
    }
}
