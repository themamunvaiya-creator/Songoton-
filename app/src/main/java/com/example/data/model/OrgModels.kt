package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MemberStatus(val labelBn: String) {
    ACTIVE("সক্রিয়"),
    INACTIVE("নিষ্ক্রিয়");

    companion object {
        fun fromRaw(raw: String?): MemberStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class TransactionType(val labelBn: String) {
    INCOME("আয় / জমা"),
    EXPENSE("ব্যয় / খরচ");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: INCOME
    }
}

enum class EventStatus(val labelBn: String) {
    UPCOMING("আসন্ন"),
    COMPLETED("সম্পন্ন");

    companion object {
        fun fromRaw(raw: String?): EventStatus =
            entries.firstOrNull { it.name == raw } ?: UPCOMING
    }
}

enum class NoticePriority(val labelBn: String) {
    URGENT("জরুরি"),
    NORMAL("সাধারণ");

    companion object {
        fun fromRaw(raw: String?): NoticePriority =
            entries.firstOrNull { it.name == raw } ?: NORMAL
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val designation: String = "সদস্য",
    val branchName: String = "পূর্ব হারবাং মুকিয়াঘোনা",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Member(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val role: String = "সাধারণ সদস্য",
    val phone: String = "",
    val bloodGroup: String = "অজানা",
    val area: String = "মুকিয়াঘোনা",
    val activeStatus: String = MemberStatus.ACTIVE.name,
    val monthlyFeeBdt: Long = 100L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): MemberStatus = MemberStatus.fromRaw(activeStatus)
}

@Keep
@IgnoreExtraProperties
data class FundTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val txType: String = TransactionType.INCOME.name,
    val category: String = "মাসিক চাঁদা",
    val amountBdt: Long = 0L,
    val memberName: String = "",
    val dateLabel: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun typeEnum(): TransactionType = TransactionType.fromRaw(txType)
}

@Keep
@IgnoreExtraProperties
data class OrgEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "দাওয়াহ ও ইলম",
    val eventDate: String = "",
    val venue: String = "পূর্ব হারবাং মুকিয়াঘোনা",
    val description: String = "",
    val status: String = EventStatus.UPCOMING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): EventStatus = EventStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val priority: String = NoticePriority.NORMAL.name,
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun priorityEnum(): NoticePriority = NoticePriority.fromRaw(priority)
}

data class OrgSummary(
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val totalIncomeBdt: Long = 0L,
    val totalExpenseBdt: Long = 0L,
    val balanceBdt: Long = 0L,
    val upcomingEventsCount: Int = 0,
    val urgentNoticesCount: Int = 0
)

object OrgConstants {
    const val ORG_FULL_NAME = "পূর্ব হারবাং মুকিয়াঘোনা ইসলামিক যুব সংগঠন"
    const val ORG_MOTTO = "একতা • সেবা • আদর্শ সমাজ গঠন"
    const val ORG_LOCATION = "পূর্ব হারবাং, মুকিয়াঘোনা, চকরিয়া, কক্সবাজার"

    val MEMBER_ROLES = listOf(
        "সভাপতি",
        "সহ-সভাপতি",
        "সাধারণ সম্পাদক",
        "যুগ্ম সম্পাদক",
        "কোষাধ্যক্ষ",
        "সাংগঠনিক সম্পাদক",
        "প্রচার সম্পাদক",
        "সমাজকল্যাণ সম্পাদক",
        "সাধারণ সদস্য"
    )

    val BLOOD_GROUPS = listOf("অজানা", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    val INCOME_CATEGORIES = listOf(
        "মাসিক চাঁদা",
        "বিশেষ অনুদান",
        "ইফতার ও মাহফিল তহবিল",
        "ত্রাণ ও যাকাত তহবিল",
        "অন্যান্য আয়"
    )

    val EXPENSE_CATEGORIES = listOf(
        "ত্রাণ ও মানবসেবা",
        "শিক্ষা ও দাওয়াহ",
        "ইফতার ও দোয়া মাহফিল",
        "ক্রীড়া ও যুব উন্নয়ন",
        "দাপ্তরিক ও প্রচার খরচ",
        "অন্যান্য ব্যয়"
    )

    val EVENT_CATEGORIES = listOf(
        "দাওয়াহ ও ইলম",
        "সমাজসেবা ও ত্রাণ",
        "যুব প্রশিক্ষণ",
        "পরিচ্ছন্নতা অভিযান",
        "সভা ও সম্মেলন"
    )
}
