package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.EventStatus
import com.example.data.model.FundTransaction
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.Notice
import com.example.data.model.NoticePriority
import com.example.data.model.OrgConstants
import com.example.data.model.OrgEvent
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OrgRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Firebase-এ প্রবেশ করতে প্রথমে Google দিয়ে সাইন ইন করুন।")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "সাইন-ইন করা ব্যবহারকারী পরিবর্তিত হয়েছে।" }
        }
        return uid
    }

    private fun <T> Flow<T>.handleSnapshotErrors(uid: String, op: OperationType, path: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .handleSnapshotErrors(uid, OperationType.GET, path)
        )
    }

    fun observeMembers(userId: String): Flow<List<Member>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/members"
        emitAll(
            db.collection("users").document(uid).collection("members")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Member::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeTransactions(userId: String): Flow<List<FundTransaction>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FundTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeEvents(userId: String): Flow<List<OrgEvent>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/events"
        emitAll(
            db.collection("users").document(uid).collection("events")
                .snapshots()
                .map { snap ->
                    snap.toObjects(OrgEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeNotices(userId: String): Flow<List<Notice>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/notices"
        emitAll(
            db.collection("users").document(uid).collection("notices")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Notice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?
    ): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "সংগঠনের সদস্য" }.take(80)
            val safeEmail = email?.trim()?.take(120)

            if (!existingSnap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "designation" to "কার্যনির্বাহী সদস্য",
                    "branchName" to OrgConstants.ORG_FULL_NAME.take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!safeEmail.isNullOrEmpty()) {
                    payload["email"] = safeEmail
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        designation: String,
        branchName: String
    ): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanName = displayName.trim()
            val cleanDesignation = designation.trim()
            val cleanBranch = branchName.trim()
            require(cleanName.length in 1..80) { "নাম ১ থেকে ৮০ অক্ষরের মধ্যে হতে হবে।" }
            require(cleanDesignation.length in 1..50) { "পদবি ১ থেকে ৫০ অক্ষরের মধ্যে হতে হবে।" }
            require(cleanBranch.length in 1..100) { "শাখার নাম ১ থেকে ১০০ অক্ষরের মধ্যে হতে হবে।" }

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "designation" to cleanDesignation,
                    "branchName" to cleanBranch,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveMember(
        userId: String,
        memberId: String?,
        fullName: String,
        role: String,
        phone: String,
        bloodGroup: String,
        area: String,
        activeStatus: MemberStatus,
        monthlyFeeBdt: Long
    ): Result<Unit> {
        var docPath = "users/$userId/members"
        return try {
            val uid = requireUserId(userId)
            val cleanName = fullName.trim()
            val cleanRole = role.trim()
            val cleanPhone = phone.trim()
            val cleanBlood = bloodGroup.trim().ifBlank { "অজানা" }
            val cleanArea = area.trim()

            require(cleanName.length in 2..80) { "সদস্যের নাম কমপক্ষে ২ অক্ষরের হতে হবে (সর্বোচ্চ ৮০)।" }
            require(cleanRole.length in 2..50) { "পদবি কমপক্ষে ২ অক্ষরের হতে হবে।" }
            require(cleanPhone.length <= 20) { "ফোন নম্বর সর্বোচ্চ ২০ অক্ষরের হতে পারবে।" }
            require(cleanBlood.length <= 16) { "রক্তের গ্রুপ সঠিক নয়।" }
            require(cleanArea.length <= 80) { "এলাকার নাম সর্বোচ্চ ৮০ অক্ষরের হতে পারবে।" }
            require(monthlyFeeBdt in 0L..1000000L) { "মাসিক চাঁদা ০ থেকে ১০,০০,০০০ টাকার মধ্যে হতে হবে।" }

            val collection = db.collection("users").document(uid).collection("members")
            if (memberId.isNullOrBlank()) {
                val docRef = collection.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "fullName" to cleanName,
                        "role" to cleanRole,
                        "phone" to cleanPhone,
                        "bloodGroup" to cleanBlood,
                        "area" to cleanArea,
                        "activeStatus" to activeStatus.name,
                        "monthlyFeeBdt" to monthlyFeeBdt,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = collection.document(memberId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "role" to cleanRole,
                        "phone" to cleanPhone,
                        "bloodGroup" to cleanBlood,
                        "area" to cleanArea,
                        "activeStatus" to activeStatus.name,
                        "monthlyFeeBdt" to monthlyFeeBdt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (memberId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMemberStatus(userId: String, memberId: String, newStatus: MemberStatus): Result<Unit> {
        var docPath = "users/$userId/members/$memberId"
        return try {
            val uid = requireUserId(userId)
            require(memberId.isNotBlank()) { "সদস্য আইডি পাওয়া যায়নি।" }
            val docRef = db.collection("users").document(uid).collection("members").document(memberId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "activeStatus" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMember(userId: String, memberId: String): Result<Unit> {
        var docPath = "users/$userId/members/$memberId"
        return try {
            val uid = requireUserId(userId)
            require(memberId.isNotBlank()) { "সদস্য আইডি পাওয়া যায়নি।" }
            val docRef = db.collection("users").document(uid).collection("members").document(memberId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveTransaction(
        userId: String,
        transactionId: String?,
        title: String,
        txType: TransactionType,
        category: String,
        amountBdt: Long,
        memberName: String,
        dateLabel: String,
        note: String
    ): Result<Unit> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            val cleanMember = memberName.trim()
            val cleanDate = dateLabel.trim()
            val cleanNote = note.trim()

            require(cleanTitle.length in 2..100) { "বিবরণ কমপক্ষে ২ অক্ষরের হতে হবে (সর্বোচ্চ ১০০)।" }
            require(cleanCategory.length in 1..50) { "খাত নির্বাচন করুন।" }
            require(amountBdt in 1L..10000000L) { "টাকার পরিমাণ ০-এর বেশি হতে হবে।" }
            require(cleanMember.length <= 80) { "সদস্য/দাতার নাম সর্বোচ্চ ৮০ অক্ষরের হতে পারবে।" }
            require(cleanDate.length <= 30) { "তারিখ সর্বোচ্চ ৩০ অক্ষরের হতে পারবে।" }
            require(cleanNote.length <= 300) { "নোট সর্বোচ্চ ৩০০ অক্ষরের হতে পারবে।" }

            val collection = db.collection("users").document(uid).collection("transactions")
            if (transactionId.isNullOrBlank()) {
                val docRef = collection.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "txType" to txType.name,
                        "category" to cleanCategory,
                        "amountBdt" to amountBdt,
                        "memberName" to cleanMember,
                        "dateLabel" to cleanDate,
                        "note" to cleanNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = collection.document(transactionId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "txType" to txType.name,
                        "category" to cleanCategory,
                        "amountBdt" to amountBdt,
                        "memberName" to cleanMember,
                        "dateLabel" to cleanDate,
                        "note" to cleanNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (transactionId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        var docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireUserId(userId)
            require(transactionId.isNotBlank()) { "লেনদেন আইডি পাওয়া যায়নি।" }
            val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveEvent(
        userId: String,
        eventId: String?,
        title: String,
        category: String,
        eventDate: String,
        venue: String,
        description: String
    ): Result<Unit> {
        var docPath = "users/$userId/events"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            val cleanDate = eventDate.trim()
            val cleanVenue = venue.trim()
            val cleanDesc = description.trim()

            require(cleanTitle.length in 2..100) { "কার্যক্রমের শিরোনাম কমপক্ষে ২ অক্ষরের হতে হবে।" }
            require(cleanCategory.length in 1..50) { "ক্যাটাগরি নির্বাচন করুন।" }
            require(cleanDate.length in 1..40) { "তারিখ ও সময় লিখুন।" }
            require(cleanVenue.length in 1..100) { "স্থান লিখুন।" }
            require(cleanDesc.length <= 500) { "বিস্তারিত বিবরণ সর্বোচ্চ ৫০০ অক্ষরের হতে পারবে।" }

            val collection = db.collection("users").document(uid).collection("events")
            if (eventId.isNullOrBlank()) {
                val docRef = collection.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "category" to cleanCategory,
                        "eventDate" to cleanDate,
                        "venue" to cleanVenue,
                        "description" to cleanDesc,
                        "status" to EventStatus.UPCOMING.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = collection.document(eventId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to cleanCategory,
                        "eventDate" to cleanDate,
                        "venue" to cleanVenue,
                        "description" to cleanDesc,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (eventId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markEventCompleted(userId: String, eventId: String): Result<Unit> {
        var docPath = "users/$userId/events/$eventId"
        return try {
            val uid = requireUserId(userId)
            require(eventId.isNotBlank()) { "কার্যক্রম আইডি পাওয়া যায়নি।" }
            val docRef = db.collection("users").document(uid).collection("events").document(eventId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to EventStatus.COMPLETED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(userId: String, eventId: String): Result<Unit> {
        var docPath = "users/$userId/events/$eventId"
        return try {
            val uid = requireUserId(userId)
            require(eventId.isNotBlank()) { "কার্যক্রম আইডি পাওয়া যায়নি।" }
            val docRef = db.collection("users").document(uid).collection("events").document(eventId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveNotice(
        userId: String,
        noticeId: String?,
        title: String,
        message: String,
        priority: NoticePriority,
        authorName: String
    ): Result<Unit> {
        var docPath = "users/$userId/notices"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanMessage = message.trim()
            val cleanAuthor = authorName.trim()

            require(cleanTitle.length in 2..100) { "নোটিশের শিরোনাম কমপক্ষে ২ অক্ষরের হতে হবে।" }
            require(cleanMessage.length in 2..600) { "নোটিশের বার্তা কমপক্ষে ২ অক্ষরের হতে হবে (সর্বোচ্চ ৬০০)।" }
            require(cleanAuthor.length <= 80) { "প্রচারকারীর নাম সর্বোচ্চ ৮০ অক্ষরের হতে পারবে।" }

            val collection = db.collection("users").document(uid).collection("notices")
            if (noticeId.isNullOrBlank()) {
                val docRef = collection.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "message" to cleanMessage,
                        "priority" to priority.name,
                        "authorName" to cleanAuthor,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = collection.document(noticeId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "message" to cleanMessage,
                        "priority" to priority.name,
                        "authorName" to cleanAuthor,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (noticeId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(userId: String, noticeId: String): Result<Unit> {
        var docPath = "users/$userId/notices/$noticeId"
        return try {
            val uid = requireUserId(userId)
            require(noticeId.isNotBlank()) { "নোটিশ আইডি পাওয়া যায়নি।" }
            val docRef = db.collection("users").document(uid).collection("notices").document(noticeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
