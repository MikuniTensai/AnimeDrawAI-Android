package com.doyouone.drawai.data.repository

import com.doyouone.drawai.data.model.ImageReport
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class ReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val reportsCollection = firestore.collection("reports")
    
    suspend fun reportImage(
        imageId: String,
        prompt: String,
        negativePrompt: String,
        workflow: String,
        imageUrl: String,
        reportReason: String,
        reportedBy: String
    ): Result<String> {
        return try {
            val reportId = reportsCollection.document().id
            val report = ImageReport(
                id = reportId,
                imageId = imageId,
                prompt = prompt,
                negativePrompt = negativePrompt,
                workflow = workflow,
                imageUrl = imageUrl,
                reportReason = reportReason,
                reportedAt = System.currentTimeMillis(),
                reportedBy = reportedBy,
                status = "pending"
            )
            
            reportsCollection.document(reportId).set(report).await()
            Log.d("ReportRepository", "Image reported successfully: $reportId")
            Result.success(reportId)
        } catch (e: Exception) {
            Log.e("ReportRepository", "Failed to report image", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllReports(): Result<List<ImageReport>> {
        return try {
            val snapshot = reportsCollection
                .orderBy("reportedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ImageReport::class.java)
            }
            
            Log.d("ReportRepository", "Retrieved ${reports.size} reports")
            Result.success(reports)
        } catch (e: Exception) {
            Log.e("ReportRepository", "Failed to get reports", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> {
        return try {
            reportsCollection.document(reportId)
                .update("status", status)
                .await()
            
            Log.d("ReportRepository", "Report status updated: $reportId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ReportRepository", "Failed to update report status", e)
            Result.failure(e)
        }
    }
}