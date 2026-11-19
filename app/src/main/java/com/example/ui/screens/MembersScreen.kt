package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.OrgConstants
import com.example.data.model.UiState
import com.example.ui.components.OrgDropdownField
import com.example.ui.components.OrgEmptyState
import com.example.ui.components.OrgErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.ui.components.formatCountBn
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald

@Composable
fun MembersScreen(
    membersState: UiState<List<Member>>,
    isSubmitting: Boolean,
    onOpenAddDialog: () -> Unit,
    onEditMember: (Member) -> Unit,
    onToggleStatus: (Member) -> Unit,
    onDeleteMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, INACTIVE
    var bloodFilter by remember { mutableStateOf("সকল") }
    var deleteCandidate by remember { mutableStateOf<Member?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("নাম, পদবি, এলাকা বা রক্তের গ্রুপ খুঁজুন") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "খুঁজুন") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("members_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("সকল") }
                )
                FilterChip(
                    selected = statusFilter == "ACTIVE",
                    onClick = { statusFilter = "ACTIVE" },
                    label = { Text("সক্রিয়") }
                )
                FilterChip(
                    selected = statusFilter == "INACTIVE",
                    onClick = { statusFilter = "INACTIVE" },
                    label = { Text("নিষ্ক্রিয়") }
                )
                listOf("সকল", "O+", "A+", "B+", "AB+", "O-", "A-", "B-").forEach { bg ->
                    if (bg != "সকল") {
                        FilterChip(
                            selected = bloodFilter == bg,
                            onClick = { bloodFilter = if (bloodFilter == bg) "সকল" else bg },
                            label = { Text("রক্ত: $bg") }
                        )
                    }
                }
            }
        }

        when (membersState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                OrgErrorBanner(message = membersState.message)
            }
            is UiState.Success -> {
                val filtered = remember(membersState.data, searchQuery, statusFilter, bloodFilter) {
                    val q = searchQuery.trim().lowercase()
                    membersState.data.filter { m ->
                        val matchesStatus = when (statusFilter) {
                            "ACTIVE" -> m.statusEnum() == MemberStatus.ACTIVE
                            "INACTIVE" -> m.statusEnum() == MemberStatus.INACTIVE
                            else -> true
                        }
                        val matchesBlood = bloodFilter == "সকল" || m.bloodGroup.equals(bloodFilter, ignoreCase = true)
                        val matchesQuery = q.isEmpty() ||
                            m.fullName.lowercase().contains(q) ||
                            m.role.lowercase().contains(q) ||
                            m.area.lowercase().contains(q) ||
                            m.bloodGroup.lowercase().contains(q) ||
                            m.phone.contains(q)
                        matchesStatus && matchesBlood && matchesQuery
                    }
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        OrgEmptyState(
                            icon = Icons.Rounded.Groups,
                            title = if (membersState.data.isEmpty()) "কোনো সদস্য যুক্ত করা হয়নি" else "মিলে যাওয়া কোনো সদস্য পাওয়া যায়নি",
                            subtitle = if (membersState.data.isEmpty()) {
                                "পূর্ব হারবাং মুকিয়াঘোনা ইসলামিক যুব সংগঠনের সদস্যদের তথ্য ও রক্তের গ্রুপ সংরক্ষণ করুন।"
                            } else {
                                "অন্য নাম বা ফিল্টার দিয়ে আবার খুঁজুন।"
                            },
                            actionLabel = if (membersState.data.isEmpty()) "প্রথম সদস্য যোগ করুন" else null,
                            onAction = if (membersState.data.isEmpty()) onOpenAddDialog else null
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "মোট ${formatCountBn(filtered.size)} জন সদস্য",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(filtered, key = { it.id }) { member ->
                            MemberCard(
                                member = member,
                                enabled = !isSubmitting,
                                onCall = {
                                    if (member.phone.isNotBlank()) {
                                        val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone}"))
                                        context.startActivity(dial)
                                    }
                                },
                                onEdit = { onEditMember(member) },
                                onToggleStatus = { onToggleStatus(member) },
                                onDelete = { deleteCandidate = member }
                            )
                        }
                    }
                }
            }
        }
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("সদস্য মুছে ফেলবেন?") },
            text = { Text("${target.fullName}-এর তথ্য স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMember(target.id)
                        deleteCandidate = null
                    }
                ) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun MemberCard(
    member: Member,
    enabled: Boolean,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = member.statusEnum() == MemberStatus.ACTIVE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.fullName.take(1).ifBlank { "স" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusPill(
                    text = member.statusEnum().labelBn,
                    containerColor = if (isActive) IncomeContainer else ExpenseContainer,
                    contentColor = if (isActive) IncomeEmerald else ExpenseCrimson
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.bloodGroup.isNotBlank() && member.bloodGroup != "অজানা") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bloodtype,
                            contentDescription = "রক্তের গ্রুপ",
                            tint = ExpenseCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = member.bloodGroup,
                            style = MaterialTheme.typography.labelLarge,
                            color = ExpenseCrimson
                        )
                    }
                }
                if (member.area.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "এলাকা",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = member.area,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "চাঁদা: ${formatBdt(member.monthlyFeeBdt)}/মাস",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleStatus, enabled = enabled) {
                    Text(if (isActive) "নিষ্ক্রিয় করুন" else "সক্রিয় করুন")
                }
                Row {
                    if (member.phone.isNotBlank()) {
                        IconButton(onClick = onCall) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "ফোন করুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onEdit, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "সংশোধন করুন",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberEditorDialog(
    initialMember: Member?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        fullName: String,
        role: String,
        phone: String,
        bloodGroup: String,
        area: String,
        activeStatus: MemberStatus,
        monthlyFeeBdt: Long
    ) -> Unit
) {
    var fullName by remember(initialMember) { mutableStateOf(initialMember?.fullName.orEmpty()) }
    var role by remember(initialMember) { mutableStateOf(initialMember?.role ?: "সাধারণ সদস্য") }
    var phone by remember(initialMember) { mutableStateOf(initialMember?.phone.orEmpty()) }
    var bloodGroup by remember(initialMember) { mutableStateOf(initialMember?.bloodGroup ?: "অজানা") }
    var area by remember(initialMember) { mutableStateOf(initialMember?.area ?: "মুকিয়াঘোনা") }
    var isActive by remember(initialMember) {
        mutableStateOf(initialMember?.statusEnum() != MemberStatus.INACTIVE)
    }
    var monthlyFeeText by remember(initialMember) {
        mutableStateOf((initialMember?.monthlyFeeBdt ?: 100L).toString())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialMember == null) "নতুন সদস্য যুক্ত করুন" else "সদস্যের তথ্য সংশোধন")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; validationError = null },
                    label = { Text("পূর্ণ নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_name_input")
                )
                OrgDropdownField(
                    label = "সাংগঠনিক পদবি",
                    selectedValue = role,
                    options = OrgConstants.MEMBER_ROLES,
                    onSelected = { role = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OrgDropdownField(
                        label = "রক্ত",
                        selectedValue = bloodGroup,
                        options = OrgConstants.BLOOD_GROUPS,
                        onSelected = { bloodGroup = it },
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("গ্রাম/পাড়া") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthlyFeeText,
                        onValueChange = { ch -> monthlyFeeText = ch.filter { it.isDigit() } },
                        label = { Text("মাসিক চাঁদা (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সক্রিয় সদস্য", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
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
                    val cleanName = fullName.trim()
                    val fee = monthlyFeeText.toLongOrNull() ?: 0L
                    if (cleanName.length < 2) {
                        validationError = "অনুগ্রহ করে কমপক্ষে ২ অক্ষরের নাম লিখুন।"
                        return@Button
                    }
                    onSave(
                        cleanName,
                        role,
                        phone.trim(),
                        bloodGroup,
                        area.trim(),
                        if (isActive) MemberStatus.ACTIVE else MemberStatus.INACTIVE,
                        fee
                    )
                },
                modifier = Modifier.testTag("save_member_button")
            ) {
                Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("বাতিল")
            }
        }
    )
}
