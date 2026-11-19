package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOut
import com.example.data.model.FundTransaction
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OrgConstants
import com.example.data.model.OrgEvent
import com.example.data.model.UiState
import com.example.data.repository.OrgRepository
import com.example.ui.screens.ActivitiesScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EventEditorDialog
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.MemberEditorDialog
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.NoticeEditorDialog
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionEditorDialog
import com.example.ui.theme.SongotonTheme
import com.example.ui.viewmodel.OrgViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_MEMBERS = "members"
const val ROUTE_FINANCE = "finance"
const val ROUTE_ACTIVITIES = "activities"
const val ROUTE_PROFILE = "profile"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SongotonTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedOrgShell(
                user = user,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedOrgShell(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val viewModel: OrgViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                OrgViewModel(
                    repository = OrgRepository(db),
                    currentUserId = user.uid,
                    initialDisplayName = user.displayName ?: "সংগঠনের সদস্য",
                    initialEmail = user.email
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val noticesState by viewModel.noticesState.collectAsStateWithLifecycle()
    val summary by viewModel.summaryState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    var activitiesTab by remember { mutableIntStateOf(0) } // 0 = Events, 1 = Notices
    var memberEditorTarget by remember { mutableStateOf<Pair<Boolean, Member?>>(false to null) }
    var txEditorTarget by remember { mutableStateOf<Pair<Boolean, FundTransaction?>>(false to null) }
    var eventEditorTarget by remember { mutableStateOf<Pair<Boolean, OrgEvent?>>(false to null) }
    var noticeEditorTarget by remember { mutableStateOf<Pair<Boolean, Notice?>>(false to null) }

    val destinations = remember {
        listOf(
            NavDestination(ROUTE_DASHBOARD, "হোম", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            NavDestination(ROUTE_MEMBERS, "সদস্য", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_members"),
            NavDestination(ROUTE_FINANCE, "তহবিল", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_finance"),
            NavDestination(ROUTE_ACTIVITIES, "কার্যক্রম", Icons.Filled.Event, Icons.Outlined.Event, "nav_activities"),
            NavDestination(ROUTE_PROFILE, "প্রোফাইল", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val topTitle = when (currentRoute) {
        ROUTE_MEMBERS -> "সদস্য তালিকা ও রক্তদান"
        ROUTE_FINANCE -> "সংগঠনের তহবিল ও হিসাব"
        ROUTE_ACTIVITIES -> if (activitiesTab == 0) "কার্যক্রম ও উদ্যোগ" else "নোটিশ বোর্ড"
        ROUTE_PROFILE -> "প্রোফাইল ও সংগঠন"
        else -> OrgConstants.ORG_FULL_NAME
    }

    val showFab = currentRoute in setOf(ROUTE_MEMBERS, ROUTE_FINANCE, ROUTE_ACTIVITIES)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = topTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(
                        onClick = {
                            when (currentRoute) {
                                ROUTE_MEMBERS -> memberEditorTarget = true to null
                                ROUTE_FINANCE -> txEditorTarget = true to null
                                ROUTE_ACTIVITIES -> {
                                    if (activitiesTab == 0) {
                                        eventEditorTarget = true to null
                                    } else {
                                        noticeEditorTarget = true to null
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("main_add_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.cd_add_action)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            userPhotoUrl = user.photoUrl?.toString(),
                            profileState = profileState,
                            summary = summary,
                            noticesState = noticesState,
                            eventsState = eventsState,
                            transactionsState = transactionsState,
                            onNavigateMembers = { navigateTo(ROUTE_MEMBERS) },
                            onNavigateFinance = { navigateTo(ROUTE_FINANCE) },
                            onNavigateActivities = { navigateTo(ROUTE_ACTIVITIES) },
                            onQuickAddMember = { memberEditorTarget = true to null },
                            onQuickAddTransaction = { txEditorTarget = true to null },
                            onQuickAddEvent = {
                                activitiesTab = 0
                                eventEditorTarget = true to null
                            },
                            onQuickAddNotice = {
                                activitiesTab = 1
                                noticeEditorTarget = true to null
                            },
                            shareReportText = { viewModel.buildShareableReportText() }
                        )
                    }

                    composable(ROUTE_MEMBERS) {
                        MembersScreen(
                            membersState = membersState,
                            isSubmitting = isSubmitting,
                            onOpenAddDialog = { memberEditorTarget = true to null },
                            onEditMember = { member -> memberEditorTarget = true to member },
                            onToggleStatus = { member -> viewModel.toggleMemberStatus(member) },
                            onDeleteMember = { id -> viewModel.deleteMember(id) }
                        )
                    }

                    composable(ROUTE_FINANCE) {
                        FinanceScreen(
                            summary = summary,
                            transactionsState = transactionsState,
                            isSubmitting = isSubmitting,
                            onOpenAddDialog = { txEditorTarget = true to null },
                            onEditTransaction = { tx -> txEditorTarget = true to tx },
                            onDeleteTransaction = { id -> viewModel.deleteTransaction(id) },
                            shareReportText = { viewModel.buildShareableReportText() }
                        )
                    }

                    composable(ROUTE_ACTIVITIES) {
                        ActivitiesScreen(
                            eventsState = eventsState,
                            noticesState = noticesState,
                            isSubmitting = isSubmitting,
                            selectedTab = activitiesTab,
                            onTabChange = { activitiesTab = it },
                            onOpenAddEvent = { eventEditorTarget = true to null },
                            onEditEvent = { ev -> eventEditorTarget = true to ev },
                            onMarkEventCompleted = { id -> viewModel.markEventCompleted(id) },
                            onDeleteEvent = { id -> viewModel.deleteEvent(id) },
                            onOpenAddNotice = { noticeEditorTarget = true to null },
                            onEditNotice = { n -> noticeEditorTarget = true to n },
                            onDeleteNotice = { id -> viewModel.deleteNotice(id) }
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            userPhotoUrl = user.photoUrl?.toString(),
                            fallbackName = user.displayName ?: "সংগঠনের সদস্য",
                            fallbackEmail = user.email,
                            profileState = profileState,
                            summary = summary,
                            isSubmitting = isSubmitting,
                            onUpdateProfile = { name, designation, branch ->
                                viewModel.updateProfile(name, designation, branch)
                            },
                            shareReportText = { viewModel.buildShareableReportText() },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }

    if (memberEditorTarget.first) {
        MemberEditorDialog(
            initialMember = memberEditorTarget.second,
            isSubmitting = isSubmitting,
            onDismiss = { memberEditorTarget = false to null },
            onSave = { name, role, phone, blood, area, status, fee ->
                viewModel.saveMember(
                    memberId = memberEditorTarget.second?.id,
                    fullName = name,
                    role = role,
                    phone = phone,
                    bloodGroup = blood,
                    area = area,
                    activeStatus = status,
                    monthlyFeeBdt = fee,
                    onSuccess = { memberEditorTarget = false to null }
                )
            }
        )
    }

    if (txEditorTarget.first) {
        val memberNames = (membersState as? UiState.Success)?.data.orEmpty().map { it.fullName }
        TransactionEditorDialog(
            initialTx = txEditorTarget.second,
            memberNames = memberNames,
            isSubmitting = isSubmitting,
            onDismiss = { txEditorTarget = false to null },
            onSave = { title, type, cat, amount, memberName, dateLabel, note ->
                viewModel.saveTransaction(
                    transactionId = txEditorTarget.second?.id,
                    title = title,
                    txType = type,
                    category = cat,
                    amountBdt = amount,
                    memberName = memberName,
                    dateLabel = dateLabel,
                    note = note,
                    onSuccess = { txEditorTarget = false to null }
                )
            }
        )
    }

    if (eventEditorTarget.first) {
        EventEditorDialog(
            initialEvent = eventEditorTarget.second,
            isSubmitting = isSubmitting,
            onDismiss = { eventEditorTarget = false to null },
            onSave = { title, cat, date, venue, desc ->
                viewModel.saveEvent(
                    eventId = eventEditorTarget.second?.id,
                    title = title,
                    category = cat,
                    eventDate = date,
                    venue = venue,
                    description = desc,
                    onSuccess = { eventEditorTarget = false to null }
                )
            }
        )
    }

    if (noticeEditorTarget.first) {
        val profile = (profileState as? UiState.Success)?.data
        val defaultAuthor = profile?.designation?.ifBlank { user.displayName ?: "প্রশাসক" }
            ?: (user.displayName ?: "প্রশাসক")
        NoticeEditorDialog(
            initialNotice = noticeEditorTarget.second,
            defaultAuthorName = defaultAuthor,
            isSubmitting = isSubmitting,
            onDismiss = { noticeEditorTarget = false to null },
            onSave = { title, message, priority, author ->
                viewModel.saveNotice(
                    noticeId = noticeEditorTarget.second?.id,
                    title = title,
                    message = message,
                    priority = priority,
                    authorName = author,
                    onSuccess = { noticeEditorTarget = false to null }
                )
            }
        )
    }
}
