package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.EventStatus
import com.example.data.model.Notice
import com.example.data.model.NoticePriority
import com.example.data.model.OrgConstants
import com.example.data.model.OrgEvent
import com.example.data.model.UiState
import com.example.ui.components.OrgDropdownField
import com.example.ui.components.OrgEmptyState
import com.example.ui.components.OrgErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.shareTextViaSheet
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.UrgentAmber
import com.example.ui.theme.UrgentContainer

@Composable
fun ActivitiesScreen(
    eventsState: UiState<List<OrgEvent>>,
    noticesState: UiState<List<Notice>>,
    isSubmitting: Boolean,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onOpenAddEvent: () -> Unit,
    onEditEvent: (OrgEvent) -> Unit,
    onMarkEventCompleted: (String) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onOpenAddNotice: () -> Unit,
    onEditNotice: (Notice) -> Unit,
    onDeleteNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deleteEventTarget by remember { mutableStateOf<OrgEvent?>(null) }
    var deleteNoticeTarget by remember { mutableStateOf<Notice?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            listOf("কার্যক্রমসমূহ", "নোটিশ বোর্ড").forEachIndexed { idx, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(idx, 2),
                    selected = selectedTab == idx,
                    onClick = { onTabChange(idx) },
                    modifier = Modifier.testTag(if (idx == 0) "tab_events" else "tab_notices")
                ) {
                    Text(label)
                }
            }
        }

        if (selectedTab == 0) {
            when (eventsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.padding(16.dp)) { OrgErrorBanner(eventsState.message) }
                is UiState.Success -> {
                    if (eventsState.data.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            OrgEmptyState(
                                icon = Icons.Rounded.Event,
                                title = "কোনো কার্যক্রম যোগ করা হয়নি",
                                subtitle = "মাহফিল, ত্রাণ বিতরণ, পরিচ্ছন্নতা অভিযান বা সভার সময়সূচি যুক্ত করুন।",
                                actionLabel = "কার্যক্রম যোগ করুন",
                                onAction = onOpenAddEvent
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(eventsState.data, key = { it.id }) { event ->
                                val isCompleted = event.statusEnum() == EventStatus.COMPLETED
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StatusPill(
                                                text = event.category,
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            StatusPill(
                                                text = event.statusEnum().labelBn,
                                                containerColor = if (isCompleted) IncomeContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = if (isCompleted) IncomeEmerald else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(event.title, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Schedule,
                                                contentDescription = "সময়",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(event.eventDate, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Place,
                                                contentDescription = "স্থান",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(event.venue, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (event.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = event.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isCompleted) {
                                                OutlinedButton(
                                                    onClick = { onMarkEventCompleted(event.id) },
                                                    enabled = !isSubmitting
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("সম্পন্ন করুন")
                                                }
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Lock,
                                                        contentDescription = "সম্পন্ন",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = IncomeEmerald
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "সম্পন্ন ও সংরক্ষিত",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = IncomeEmerald
                                                    )
                                                }
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        shareTextViaSheet(
                                                            context,
                                                            event.title,
                                                            "🕌 ${OrgConstants.ORG_FULL_NAME}\n📅 ${event.title}\n🕒 ${event.eventDate}\n📍 ${event.venue}\n${event.description}"
                                                        )
                                                    }
                                                ) {
                                                    Icon(Icons.Rounded.Share, contentDescription = "শেয়ার করুন")
                                                }
                                                if (!isCompleted) {
                                                    IconButton(onClick = { onEditEvent(event) }, enabled = !isSubmitting) {
                                                        Icon(Icons.Rounded.Edit, contentDescription = "সংশোধন করুন")
                                                    }
                                                }
                                                IconButton(onClick = { deleteEventTarget = event }, enabled = !isSubmitting) {
                                                    Icon(
                                                        Icons.Rounded.Delete,
                                                        contentDescription = "মুছুন",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (noticesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.padding(16.dp)) { OrgErrorBanner(noticesState.message) }
                is UiState.Success -> {
                    if (noticesState.data.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            OrgEmptyState(
                                icon = Icons.Rounded.Campaign,
                                title = "নোটিশ বোর্ড খালি",
                                subtitle = "সংগঠনের সদস্যদের উদ্দেশ্যে জরুরি বা সাধারণ নোটিশ প্রকাশ করুন।",
                                actionLabel = "নোটিশ প্রকাশ করুন",
                                onAction = onOpenAddNotice
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(noticesState.data, key = { it.id }) { notice ->
                                val isUrgent = notice.priorityEnum() == NoticePriority.URGENT
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StatusPill(
                                                text = notice.priorityEnum().labelBn,
                                                containerColor = if (isUrgent) UrgentContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isUrgent) UrgentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (notice.authorName.isNotBlank()) {
                                                Text(
                                                    text = "প্রচার: ${notice.authorName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(notice.title, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notice.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    shareTextViaSheet(
                                                        context,
                                                        notice.title,
                                                        "📢 ${OrgConstants.ORG_FULL_NAME}\n[${notice.priorityEnum().labelBn}] ${notice.title}\n\n${notice.message}"
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Share, contentDescription = "শেয়ার করুন")
                                            }
                                            IconButton(onClick = { onEditNotice(notice) }, enabled = !isSubmitting) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "সংশোধন করুন")
                                            }
                                            IconButton(onClick = { deleteNoticeTarget = notice }, enabled = !isSubmitting) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "মুছুন",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteEventTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteEventTarget = null },
            title = { Text("কার্যক্রম মুছে ফেলবেন?") },
            text = { Text("'${target.title}' স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                TextButton(onClick = { onDeleteEvent(target.id); deleteEventTarget = null }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteEventTarget = null }) { Text("বাতিল") }
            }
        )
    }

    deleteNoticeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteNoticeTarget = null },
            title = { Text("নোটিশ মুছে ফেলবেন?") },
            text = { Text("'${target.title}' স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                TextButton(onClick = { onDeleteNotice(target.id); deleteNoticeTarget = null }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteNoticeTarget = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
fun EventEditorDialog(
    initialEvent: OrgEvent?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, eventDate: String, venue: String, description: String) -> Unit
) {
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var category by remember(initialEvent) {
        mutableStateOf(initialEvent?.category ?: OrgConstants.EVENT_CATEGORIES.first())
    }
    var eventDate by remember(initialEvent) { mutableStateOf(initialEvent?.eventDate.orEmpty()) }
    var venue by remember(initialEvent) { mutableStateOf(initialEvent?.venue ?: "পূর্ব হারবাং মুকিয়াঘোনা") }
    var description by remember(initialEvent) { mutableStateOf(initialEvent?.description.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent == null) "নতুন কার্যক্রম যোগ করুন" else "কার্যক্রম সংশোধন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("কার্যক্রমের শিরোনাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                )
                OrgDropdownField(
                    label = "ধরন / ক্যাটাগরি",
                    selectedValue = category,
                    options = OrgConstants.EVENT_CATEGORIES,
                    onSelected = { category = it }
                )
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it; validationError = null },
                    label = { Text("তারিখ ও সময় *") },
                    placeholder = { Text("যেমন: শুক্রবার, বাদ আসর") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_date_input")
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it; validationError = null },
                    label = { Text("স্থান *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিস্তারিত বিবরণ") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (title.trim().length < 2) {
                        validationError = "কমপক্ষে ২ অক্ষরের শিরোনাম লিখুন।"
                        return@Button
                    }
                    if (eventDate.trim().isEmpty() || venue.trim().isEmpty()) {
                        validationError = "তারিখ ও স্থান অবশ্যই লিখতে হবে।"
                        return@Button
                    }
                    onSave(title.trim(), category, eventDate.trim(), venue.trim(), description.trim())
                },
                modifier = Modifier.testTag("save_event_button")
            ) {
                Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("বাতিল") }
        }
    )
}

@Composable
fun NoticeEditorDialog(
    initialNotice: Notice?,
    defaultAuthorName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, message: String, priority: NoticePriority, authorName: String) -> Unit
) {
    var title by remember(initialNotice) { mutableStateOf(initialNotice?.title.orEmpty()) }
    var message by remember(initialNotice) { mutableStateOf(initialNotice?.message.orEmpty()) }
    var priority by remember(initialNotice) { mutableStateOf(initialNotice?.priorityEnum() ?: NoticePriority.NORMAL) }
    var authorName by remember(initialNotice) { mutableStateOf(initialNotice?.authorName ?: defaultAuthorName) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNotice == null) "নতুন নোটিশ প্রকাশ করুন" else "নোটিশ সংশোধন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    NoticePriority.entries.forEachIndexed { index, p ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, NoticePriority.entries.size),
                            selected = priority == p,
                            onClick = { priority = p }
                        ) {
                            Text(p.labelBn)
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("নোটিশের শিরোনাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("notice_title_input")
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; validationError = null },
                    label = { Text("মূল বার্তা *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("notice_message_input")
                )
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("প্রচারকারী") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (title.trim().length < 2 || message.trim().length < 2) {
                        validationError = "শিরোনাম ও বার্তা কমপক্ষে ২ অক্ষরের হতে হবে।"
                        return@Button
                    }
                    onSave(title.trim(), message.trim(), priority, authorName.trim())
                },
                modifier = Modifier.testTag("save_notice_button")
            ) {
                Text(if (isSubmitting) "প্রকাশ হচ্ছে..." else "প্রকাশ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("বাতিল") }
        }
    )
}
