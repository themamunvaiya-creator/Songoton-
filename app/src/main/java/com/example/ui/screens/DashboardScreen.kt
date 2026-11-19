package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.EventStatus
import com.example.data.model.FundTransaction
import com.example.data.model.Notice
import com.example.data.model.NoticePriority
import com.example.data.model.OrgConstants
import com.example.data.model.OrgEvent
import com.example.data.model.OrgSummary
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.OrgEmptyState
import com.example.ui.components.OrgErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.ui.components.formatCountBn
import com.example.ui.components.shareTextViaSheet
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.UrgentAmber
import com.example.ui.theme.UrgentContainer

@Composable
fun DashboardScreen(
    userPhotoUrl: String?,
    profileState: UiState<UserProfile?>,
    summary: OrgSummary,
    noticesState: UiState<List<Notice>>,
    eventsState: UiState<List<OrgEvent>>,
    transactionsState: UiState<List<FundTransaction>>,
    onNavigateMembers: () -> Unit,
    onNavigateFinance: () -> Unit,
    onNavigateActivities: () -> Unit,
    onQuickAddMember: () -> Unit,
    onQuickAddTransaction: () -> Unit,
    onQuickAddEvent: () -> Unit,
    onQuickAddNotice: () -> Unit,
    shareReportText: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Organization Hero Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_org_banner),
                        contentDescription = stringResource(id = R.string.cd_org_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66092E1C),
                                        Color(0xE0092E1C)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                shareTextViaSheet(
                                    context,
                                    OrgConstants.ORG_FULL_NAME,
                                    shareReportText()
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("dashboard_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(id = R.string.cd_share_report),
                                tint = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = stringResource(id = R.string.cd_user_avatar),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = stringResource(id = R.string.cd_user_avatar),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = OrgConstants.ORG_FULL_NAME,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (profile != null) {
                                "${profile.displayName} • ${profile.designation}"
                            } else {
                                OrgConstants.ORG_MOTTO
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 2. Asymmetric Financial Overview Hero
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateFinance)
                    .testTag("dashboard_balance_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সংগঠনের বর্তমান তহবিল",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "সক্রিয় সদস্য: ${formatCountBn(summary.activeMembers)} জন",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatBdt(summary.balanceBdt),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = "আয়",
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "মোট আয়/চাঁদা",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = formatBdt(summary.totalIncomeBdt),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = "ব্যয়",
                                        tint = Color(0xFFFECACA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "মোট ব্যয়/সেবা",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = formatBdt(summary.totalExpenseBdt),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Actions 2x2 Grid
        item {
            Text(
                text = "দ্রুত কার্যক্রম",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Rounded.GroupAdd,
                    label = "সদস্য যোগ",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onQuickAddMember,
                    testTag = "quick_add_member",
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    label = "আয়/ব্যয় এন্ট্রি",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onQuickAddTransaction,
                    testTag = "quick_add_tx",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Rounded.Event,
                    label = "নতুন কার্যক্রম",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onQuickAddEvent,
                    testTag = "quick_add_event",
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    icon = Icons.Rounded.Campaign,
                    label = "নোটিশ প্রকাশ",
                    containerColor = UrgentContainer,
                    contentColor = UrgentAmber,
                    onClick = onQuickAddNotice,
                    testTag = "quick_add_notice",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Recent Notices Preview
        item {
            SectionHeaderRow(
                title = "সাম্প্রতিক নোটিশ",
                actionText = "সব দেখুন",
                onAction = onNavigateActivities
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (noticesState) {
                is UiState.Loading -> Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> OrgErrorBanner(message = noticesState.message)
                is UiState.Success -> {
                    val topNotices = noticesState.data.take(2)
                    if (topNotices.isEmpty()) {
                        OrgEmptyState(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "এখনো কোনো নোটিশ প্রকাশিত হয়নি",
                            subtitle = "সদস্যদের জন্য সভার সময় বা জরুরি ঘোষণা যুক্ত করুন।",
                            actionLabel = "নোটিশ লিখুন",
                            onAction = onQuickAddNotice
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topNotices.forEach { notice ->
                                val isUrgent = notice.priorityEnum() == NoticePriority.URGENT
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateActivities),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = notice.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            StatusPill(
                                                text = notice.priorityEnum().labelBn,
                                                containerColor = if (isUrgent) UrgentContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isUrgent) UrgentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notice.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Upcoming Events Preview
        item {
            SectionHeaderRow(
                title = "আসন্ন কার্যক্রম",
                actionText = "সব দেখুন",
                onAction = onNavigateActivities
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (eventsState) {
                is UiState.Loading -> {}
                is UiState.Error -> OrgErrorBanner(message = eventsState.message)
                is UiState.Success -> {
                    val upcoming = eventsState.data.filter { it.statusEnum() == EventStatus.UPCOMING }.take(2)
                    if (upcoming.isEmpty()) {
                        OrgEmptyState(
                            icon = Icons.Rounded.Event,
                            title = "কোনো আসন্ন কার্যক্রম নেই",
                            subtitle = "দাওয়াহ, সমাজসেবা বা ক্রীড়া কর্মসূচি যোগ করুন।",
                            actionLabel = "কার্যক্রম যোগ করুন",
                            onAction = onQuickAddEvent
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            upcoming.forEach { ev ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateActivities),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Event,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${ev.eventDate} • ${ev.venue}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // 6. Recent Transactions Preview
        item {
            SectionHeaderRow(
                title = "সাম্প্রতিক লেনদেন",
                actionText = "হিসাব খাতা",
                onAction = onNavigateFinance
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (transactionsState) {
                is UiState.Loading -> {}
                is UiState.Error -> OrgErrorBanner(message = transactionsState.message)
                is UiState.Success -> {
                    val recentTx = transactionsState.data.take(3)
                    if (recentTx.isEmpty()) {
                        OrgEmptyState(
                            icon = Icons.Rounded.Groups,
                            title = "তহবিল খাতা এখনো খালি",
                            subtitle = "মাসিক চাঁদা, অনুদান বা খরচের হিসাব যুক্ত করুন।",
                            actionLabel = "হিসাব যোগ করুন",
                            onAction = onQuickAddTransaction
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentTx.forEach { tx ->
                                val isIncome = tx.typeEnum() == TransactionType.INCOME
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateFinance),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${tx.category}${if (tx.dateLabel.isNotBlank()) " • ${tx.dateLabel}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusPill(
                                            text = (if (isIncome) "+" else "-") + formatBdt(tx.amountBdt),
                                            containerColor = if (isIncome) IncomeContainer else ExpenseContainer,
                                            contentColor = if (isIncome) IncomeEmerald else ExpenseCrimson
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

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(68.dp)
            .testTag(testTag),
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onAction) {
            Text(actionText)
        }
    }
}
