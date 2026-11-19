package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EventStatus
import com.example.data.model.FundTransaction
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.Notice
import com.example.data.model.NoticePriority
import com.example.data.model.OrgConstants
import com.example.data.model.OrgEvent
import com.example.data.model.OrgSummary
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.OrgRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrgViewModel(
    private val repository: OrgRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String?
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("প্রোফাইল লোড করতে সমস্যা হয়েছে")

    val membersState: StateFlow<UiState<List<Member>>> =
        repository.observeMembers(currentUserId).toUiState("সদস্য তালিকা লোড করতে সমস্যা হয়েছে")

    val transactionsState: StateFlow<UiState<List<FundTransaction>>> =
        repository.observeTransactions(currentUserId).toUiState("তহবিলের হিসাব লোড করতে সমস্যা হয়েছে")

    val eventsState: StateFlow<UiState<List<OrgEvent>>> =
        repository.observeEvents(currentUserId).toUiState("কার্যক্রম তালিকা লোড করতে সমস্যা হয়েছে")

    val noticesState: StateFlow<UiState<List<Notice>>> =
        repository.observeNotices(currentUserId).toUiState("নোটিশ বোর্ড লোড করতে সমস্যা হয়েছে")

    val summaryState: StateFlow<OrgSummary> = combine(
        membersState,
        transactionsState,
        eventsState,
        noticesState
    ) { membersUi, txUi, eventsUi, noticesUi ->
        val members = (membersUi as? UiState.Success)?.data.orEmpty()
        val transactions = (txUi as? UiState.Success)?.data.orEmpty()
        val events = (eventsUi as? UiState.Success)?.data.orEmpty()
        val notices = (noticesUi as? UiState.Success)?.data.orEmpty()

        val income = transactions.filter { it.typeEnum() == TransactionType.INCOME }.sumOf { it.amountBdt }
        val expense = transactions.filter { it.typeEnum() == TransactionType.EXPENSE }.sumOf { it.amountBdt }

        OrgSummary(
            totalMembers = members.size,
            activeMembers = members.count { it.statusEnum() == MemberStatus.ACTIVE },
            totalIncomeBdt = income,
            totalExpenseBdt = expense,
            balanceBdt = income - expense,
            upcomingEventsCount = events.count { it.statusEnum() == EventStatus.UPCOMING },
            urgentNoticesCount = notices.count { it.priorityEnum() == NoticePriority.URGENT }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OrgSummary()
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun updateProfile(displayName: String, designation: String, branchName: String) {
        runAction("প্রোফাইল সফলভাবে হালনাগাদ হয়েছে।") {
            repository.updateUserProfile(currentUserId, displayName, designation, branchName)
        }
    }

    fun saveMember(
        memberId: String?,
        fullName: String,
        role: String,
        phone: String,
        bloodGroup: String,
        area: String,
        activeStatus: MemberStatus,
        monthlyFeeBdt: Long,
        onSuccess: () -> Unit = {}
    ) {
        val msg = if (memberId == null) "নতুন সদস্য সফলভাবে যুক্ত হয়েছে।" else "সদস্যের তথ্য হালনাগাদ হয়েছে।"
        runAction(msg, onSuccess) {
            repository.saveMember(
                userId = currentUserId,
                memberId = memberId,
                fullName = fullName,
                role = role,
                phone = phone,
                bloodGroup = bloodGroup,
                area = area,
                activeStatus = activeStatus,
                monthlyFeeBdt = monthlyFeeBdt
            )
        }
    }

    fun toggleMemberStatus(member: Member) {
        val next = if (member.statusEnum() == MemberStatus.ACTIVE) MemberStatus.INACTIVE else MemberStatus.ACTIVE
        runAction("সদস্যের অবস্থা '${next.labelBn}' করা হয়েছে।") {
            repository.toggleMemberStatus(currentUserId, member.id, next)
        }
    }

    fun deleteMember(memberId: String) {
        runAction("সদস্য মুছে ফেলা হয়েছে।") {
            repository.deleteMember(currentUserId, memberId)
        }
    }

    fun saveTransaction(
        transactionId: String?,
        title: String,
        txType: TransactionType,
        category: String,
        amountBdt: Long,
        memberName: String,
        dateLabel: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val msg = if (transactionId == null) "তহবিলের হিসাব সংরক্ষিত হয়েছে।" else "হিসাব সংশোধন করা হয়েছে।"
        runAction(msg, onSuccess) {
            repository.saveTransaction(
                userId = currentUserId,
                transactionId = transactionId,
                title = title,
                txType = txType,
                category = category,
                amountBdt = amountBdt,
                memberName = memberName,
                dateLabel = dateLabel,
                note = note
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        runAction("হিসাবটি মুছে ফেলা হয়েছে।") {
            repository.deleteTransaction(currentUserId, transactionId)
        }
    }

    fun saveEvent(
        eventId: String?,
        title: String,
        category: String,
        eventDate: String,
        venue: String,
        description: String,
        onSuccess: () -> Unit = {}
    ) {
        val msg = if (eventId == null) "নতুন কার্যক্রম যুক্ত হয়েছে।" else "কার্যক্রমের তথ্য হালনাগাদ হয়েছে।"
        runAction(msg, onSuccess) {
            repository.saveEvent(
                userId = currentUserId,
                eventId = eventId,
                title = title,
                category = category,
                eventDate = eventDate,
                venue = venue,
                description = description
            )
        }
    }

    fun markEventCompleted(eventId: String) {
        runAction("কার্যক্রমটি সম্পন্ন হিসেবে চিহ্নিত হয়েছে।") {
            repository.markEventCompleted(currentUserId, eventId)
        }
    }

    fun deleteEvent(eventId: String) {
        runAction("কার্যক্রমটি মুছে ফেলা হয়েছে।") {
            repository.deleteEvent(currentUserId, eventId)
        }
    }

    fun saveNotice(
        noticeId: String?,
        title: String,
        message: String,
        priority: NoticePriority,
        authorName: String,
        onSuccess: () -> Unit = {}
    ) {
        val msg = if (noticeId == null) "নোটিশ প্রকাশিত হয়েছে।" else "নোটিশ হালনাগাদ হয়েছে।"
        runAction(msg, onSuccess) {
            repository.saveNotice(
                userId = currentUserId,
                noticeId = noticeId,
                title = title,
                message = message,
                priority = priority,
                authorName = authorName
            )
        }
    }

    fun deleteNotice(noticeId: String) {
        runAction("নোটিশ মুছে ফেলা হয়েছে।") {
            repository.deleteNotice(currentUserId, noticeId)
        }
    }

    fun buildShareableReportText(): String {
        val s = summaryState.value
        val events = (eventsState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.statusEnum() == EventStatus.UPCOMING }
            .take(3)
        val notices = (noticesState.value as? UiState.Success)?.data.orEmpty().take(3)

        return buildString {
            appendLine("🕌 ${OrgConstants.ORG_FULL_NAME}")
            appendLine("📍 ${OrgConstants.ORG_LOCATION}")
            appendLine("━━━━━━━━━━━━━━━━━━")
            appendLine("👥 মোট সদস্য: ${s.totalMembers} জন (সক্রিয়: ${s.activeMembers} জন)")
            appendLine("💰 মোট আয়/চাঁদা: ৳${s.totalIncomeBdt}")
            appendLine("💸 মোট ব্যয়/সেবা: ৳${s.totalExpenseBdt}")
            appendLine("🏦 বর্তমান তহবিল: ৳${s.balanceBdt}")
            if (events.isNotEmpty()) {
                appendLine()
                appendLine("📅 আসন্ন কার্যক্রম:")
                events.forEach { ev ->
                    appendLine("• ${ev.title} (${ev.eventDate} - ${ev.venue})")
                }
            }
            if (notices.isNotEmpty()) {
                appendLine()
                appendLine("📢 সাম্প্রতিক নোটিশ:")
                notices.forEach { n ->
                    appendLine("• [${n.priorityEnum().labelBn}] ${n.title}: ${n.message}")
                }
            }
        }
    }

    private fun runAction(
        successMessage: String,
        onSuccess: () -> Unit = {},
        block: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val result = block()
                result.fold(
                    onSuccess = {
                        _feedbackMessage.value = successMessage
                        onSuccess()
                    },
                    onFailure = { error ->
                        _feedbackMessage.value = error.localizedMessage ?: "অপারেশন সম্পন্ন করা যায়নি।"
                    }
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun <T> Flow<T>.toUiState(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private companion object {
        const val TAG = "OrgViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
