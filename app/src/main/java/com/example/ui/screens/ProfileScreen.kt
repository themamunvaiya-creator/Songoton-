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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.OrgConstants
import com.example.data.model.OrgSummary
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.OrgDropdownField
import com.example.ui.components.formatBdt
import com.example.ui.components.formatCountBn
import com.example.ui.components.shareTextViaSheet

@Composable
fun ProfileScreen(
    userPhotoUrl: String?,
    fallbackName: String,
    fallbackEmail: String?,
    profileState: UiState<UserProfile?>,
    summary: OrgSummary,
    isSubmitting: Boolean,
    onUpdateProfile: (displayName: String, designation: String, branchName: String) -> Unit,
    shareReportText: () -> String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    var showEditDialog by remember { mutableStateOf(false) }

    val displayName = profile?.displayName?.ifBlank { fallbackName } ?: fallbackName
    val designation = profile?.designation?.ifBlank { "কার্যনির্বাহী সদস্য" } ?: "কার্যনির্বাহী সদস্য"
    val branchName = profile?.branchName?.ifBlank { OrgConstants.ORG_FULL_NAME } ?: OrgConstants.ORG_FULL_NAME
    val email = profile?.email?.ifBlank { fallbackEmail.orEmpty() } ?: fallbackEmail.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
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
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(displayName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = designation,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        enabled = !isSubmitting,
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("তথ্য ও পদবি হালনাগাদ")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Mosque,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = OrgConstants.ORG_FULL_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "লক্ষ্য ও উদ্দেশ্য: ${OrgConstants.ORG_MOTTO}। স্থানীয় যুব সমাজকে নৈতিকতা, শিক্ষা, দাওয়াহ ও মানবকল্যাণমূলক কাজে ঐক্যবদ্ধ করা।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ইউনিট/শাখা: $branchName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("সারসংক্ষেপ ও ক্লাউড অবস্থা", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "• নিবন্ধিত সদস্য: ${formatCountBn(summary.totalMembers)} জন (সক্রিয় ${formatCountBn(summary.activeMembers)} জন)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• বর্তমান তহবিল: ${formatBdt(summary.balanceBdt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• আসন্ন কার্যক্রম: ${formatCountBn(summary.upcomingEventsCount)} টি • জরুরি নোটিশ: ${formatCountBn(summary.urgentNoticesCount)} টি",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Firebase Firestore ক্লাউডে রিয়েল-টাইম সংযুক্ত",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    shareTextViaSheet(
                        context,
                        "${OrgConstants.ORG_FULL_NAME} — সারসংক্ষেপ",
                        shareReportText()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_share_report_button")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("সংগঠনের সম্পূর্ণ রিপোর্ট শেয়ার করুন")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.sign_out))
            }
        }
    }

    if (showEditDialog) {
        var nameInput by remember { mutableStateOf(displayName) }
        var roleInput by remember { mutableStateOf(designation) }
        var branchInput by remember { mutableStateOf(branchName) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("প্রোফাইল সম্পাদনা") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; error = null },
                        label = { Text("আপনার নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OrgDropdownField(
                        label = "সংগঠনে পদবি",
                        selectedValue = roleInput,
                        options = OrgConstants.MEMBER_ROLES,
                        onSelected = { roleInput = it }
                    )
                    OutlinedTextField(
                        value = branchInput,
                        onValueChange = { branchInput = it; error = null },
                        label = { Text("শাখা / ইউনিট *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (nameInput.trim().isEmpty() || branchInput.trim().isEmpty()) {
                            error = "নাম ও শাখা পূরণ করুন।"
                            return@Button
                        }
                        onUpdateProfile(nameInput.trim(), roleInput, branchInput.trim())
                        showEditDialog = false
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
