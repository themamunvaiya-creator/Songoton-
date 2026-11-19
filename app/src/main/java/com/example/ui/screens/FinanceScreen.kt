package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.model.FundTransaction
import com.example.data.model.OrgConstants
import com.example.data.model.OrgSummary
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.ui.components.OrgDropdownField
import com.example.ui.components.OrgEmptyState
import com.example.ui.components.OrgErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.ui.components.shareTextViaSheet
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(
    summary: OrgSummary,
    transactionsState: UiState<List<FundTransaction>>,
    isSubmitting: Boolean,
    onOpenAddDialog: () -> Unit,
    onEditTransaction: (FundTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    shareReportText: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var typeFilter by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE
    var deleteCandidate by remember { mutableStateOf<FundTransaction?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Summary Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "নীট তহবিল স্থিতি",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatBdt(summary.balanceBdt),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = {
                            shareTextViaSheet(
                                context,
                                "${OrgConstants.ORG_FULL_NAME} — তহবিল রিপোর্ট",
                                shareReportText()
                            )
                        },
                        modifier = Modifier.testTag("finance_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "তহবিল রিপোর্ট শেয়ার করুন",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusPill(
                        text = "মোট আয়: ${formatBdt(summary.totalIncomeBdt)}",
                        containerColor = IncomeContainer,
                        contentColor = IncomeEmerald
                    )
                    StatusPill(
                        text = "মোট ব্যয়: ${formatBdt(summary.totalExpenseBdt)}",
                        containerColor = ExpenseContainer,
                        contentColor = ExpenseCrimson
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = typeFilter == "ALL",
                onClick = { typeFilter = "ALL" },
                label = { Text("সকল হিসাব") }
            )
            FilterChip(
                selected = typeFilter == "INCOME",
                onClick = { typeFilter = "INCOME" },
                label = { Text("আয় ও চাঁদা") }
            )
            FilterChip(
                selected = typeFilter == "EXPENSE",
                onClick = { typeFilter = "EXPENSE" },
                label = { Text("ব্যয় ও সেবা") }
            )
        }

        when (transactionsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                OrgErrorBanner(message = transactionsState.message)
            }
            is UiState.Success -> {
                val filtered = remember(transactionsState.data, typeFilter) {
                    when (typeFilter) {
                        "INCOME" -> transactionsState.data.filter { it.typeEnum() == TransactionType.INCOME }
                        "EXPENSE" -> transactionsState.data.filter { it.typeEnum() == TransactionType.EXPENSE }
                        else -> transactionsState.data
                    }
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        OrgEmptyState(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "কোনো লেনদেন পাওয়া যায়নি",
                            subtitle = "সদস্যদের মাসিক চাঁদা, অনুদান অথবা সংগঠনের ব্যয়ের হিসাব যুক্ত করুন।",
                            actionLabel = if (transactionsState.data.isEmpty()) "প্রথম হিসাব যুক্ত করুন" else null,
                            onAction = if (transactionsState.data.isEmpty()) onOpenAddDialog else null
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { tx ->
                            TransactionCard(
                                tx = tx,
                                enabled = !isSubmitting,
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { deleteCandidate = tx }
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
            title = { Text("হিসাব মুছে ফেলবেন?") },
            text = { Text("'${target.title}' (${formatBdt(target.amountBdt)}) মুছে ফেলা হবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(target.id)
                        deleteCandidate = null
                    }
                ) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun TransactionCard(
    tx: FundTransaction,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = tx.typeEnum() == TransactionType.INCOME
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_card_${tx.id}"),
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) IncomeContainer else ExpenseContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = tx.typeEnum().labelBn,
                        tint = if (isIncome) IncomeEmerald else ExpenseCrimson
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tx.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = buildString {
                            append(tx.category)
                            if (tx.memberName.isNotBlank()) append(" • ${tx.memberName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = (if (isIncome) "+" else "-") + formatBdt(tx.amountBdt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) IncomeEmerald else ExpenseCrimson
                )
            }

            if (tx.note.isNotBlank() || tx.dateLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                if (tx.dateLabel.isNotBlank()) {
                    Text(
                        text = "তারিখ: ${tx.dateLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (tx.note.isNotBlank()) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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

@Composable
fun TransactionEditorDialog(
    initialTx: FundTransaction?,
    memberNames: List<String>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        txType: TransactionType,
        category: String,
        amountBdt: Long,
        memberName: String,
        dateLabel: String,
        note: String
    ) -> Unit
) {
    val defaultDate = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }
    var txType by remember(initialTx) { mutableStateOf(initialTx?.typeEnum() ?: TransactionType.INCOME) }
    var title by remember(initialTx) { mutableStateOf(initialTx?.title.orEmpty()) }
    var category by remember(initialTx) {
        mutableStateOf(
            initialTx?.category ?: OrgConstants.INCOME_CATEGORIES.first()
        )
    }
    var amountText by remember(initialTx) {
        mutableStateOf(initialTx?.amountBdt?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var memberName by remember(initialTx) { mutableStateOf(initialTx?.memberName.orEmpty()) }
    var dateLabel by remember(initialTx) { mutableStateOf(initialTx?.dateLabel?.ifBlank { defaultDate } ?: defaultDate) }
    var note by remember(initialTx) { mutableStateOf(initialTx?.note.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val categories = if (txType == TransactionType.INCOME) {
        OrgConstants.INCOME_CATEGORIES
    } else {
        OrgConstants.EXPENSE_CATEGORIES
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTx == null) "নতুন আয়/ব্যয় হিসাব" else "হিসাব সংশোধন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TransactionType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                            selected = txType == type,
                            onClick = {
                                txType = type
                                category = if (type == TransactionType.INCOME) {
                                    OrgConstants.INCOME_CATEGORIES.first()
                                } else {
                                    OrgConstants.EXPENSE_CATEGORIES.first()
                                }
                            }
                        ) {
                            Text(type.labelBn)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("বিবরণ / শিরোনাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_title_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { s -> amountText = s.filter { it.isDigit() }; validationError = null },
                        label = { Text("টাকা (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("tx_amount_input")
                    )
                    OutlinedTextField(
                        value = dateLabel,
                        onValueChange = { dateLabel = it },
                        label = { Text("তারিখ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OrgDropdownField(
                    label = "খাত / ক্যাটাগরি",
                    selectedValue = category,
                    options = categories,
                    onSelected = { category = it }
                )

                if (memberNames.isNotEmpty()) {
                    OrgDropdownField(
                        label = "সদস্য / দাতা (ঐচ্ছিক)",
                        selectedValue = memberName.ifBlank { "নির্বাচন করুন" },
                        options = listOf("নির্বাচন করুন") + memberNames,
                        onSelected = { picked -> memberName = if (picked == "নির্বাচন করুন") "" else picked }
                    )
                } else {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("সদস্য / দাতার নাম (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("অতিরিক্ত নোট") },
                    maxLines = 2,
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
                    val cleanTitle = title.trim()
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (cleanTitle.length < 2) {
                        validationError = "কমপক্ষে ২ অক্ষরের বিবরণ লিখুন।"
                        return@Button
                    }
                    if (amount <= 0L) {
                        validationError = "০-এর বেশি সঠিক টাকার পরিমাণ লিখুন।"
                        return@Button
                    }
                    onSave(
                        cleanTitle,
                        txType,
                        category,
                        amount,
                        memberName.trim(),
                        dateLabel.trim(),
                        note.trim()
                    )
                },
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text(if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("বাতিল") }
        }
    )
}
