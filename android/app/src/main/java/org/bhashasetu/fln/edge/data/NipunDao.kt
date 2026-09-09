package org.bhashasetu.fln.edge.data

import androidx.room.*

@Dao
interface NipunDao {
    @Query("SELECT * FROM nipun_milestones WHERE gradeLevel = :grade")
    fun getMilestonesByGrade(grade: Int): List<NipunMilestone>

    @Query("SELECT * FROM extractive_resources WHERE associatedMilestoneId = :milestoneId")
    fun getResourcesByMilestone(milestoneId: String): List<ExtractiveResource>

    @Query("SELECT * FROM lesson_scripts WHERE gradeLevel = :grade AND subject = :subject")
    fun getLessons(grade: Int, subject: String): List<LessonScript>

    @Query("SELECT * FROM worksheets WHERE gradeLevel = :grade AND subject = :subject")
    fun getWorksheets(grade: Int, subject: String): List<Worksheet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMilestone(milestone: NipunMilestone)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResource(resource: ExtractiveResource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLesson(lesson: LessonScript)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorksheet(worksheet: Worksheet)
}
