package org.bhashasetu.fln.edge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nipun_milestones")
data class NipunMilestone(
    @PrimaryKey val milestoneId: String,
    val developmentalGoal: Int,
    val competencyTextHindi: String,
    val competencyTextSantali: String,
    val gradeLevel: Int,
    val subject: String
)

@Entity(tableName = "extractive_resources")
data class ExtractiveResource(
    @PrimaryKey val resourceId: String,
    val associatedMilestoneId: String,
    val keywordToken: String,
    val summaryTextHindi: String,
    val summaryTextSantali: String,
    val assetPathFlashcard: String,
    val assetPathAudio: String
)

@Entity(tableName = "lesson_scripts")
data class LessonScript(
    @PrimaryKey val lessonId: String,
    val milestoneId: String,
    val titleHindi: String,
    val titleSantali: String,
    val contentHindi: String,
    val contentSantali: String,
    val audioPath: String,
    val gradeLevel: Int,
    val subject: String
)

@Entity(tableName = "worksheets")
data class Worksheet(
    @PrimaryKey val worksheetId: String,
    val milestoneId: String,
    val titleHindi: String,
    val titleSantali: String,
    val questionsJson: String,
    val gradeLevel: Int,
    val subject: String
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey val flashcardId: String,
    val milestoneId: String,
    val conceptCategory: String,
    val hindiLabel: String,
    val santaliLabel: String,
    val imagePath: String,
    val audioPath: String,
    val gradeLevel: Int
)

@Entity(tableName = "phrasebook")
data class PhrasebookEntry(
    @PrimaryKey val phraseId: String,
    val hindiPhrase: String,
    val santaliPhrase: String,
    val category: String,
    val isHighConfidence: Boolean = true
)
