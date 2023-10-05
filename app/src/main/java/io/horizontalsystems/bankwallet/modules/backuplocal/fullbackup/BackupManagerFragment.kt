package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.core.findNavController

class BackupManagerFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            FullBackupNavHost(fragmentNavController = findNavController())
        }
    }

}


@Composable
private fun FullBackupNavHost(fragmentNavController: NavController) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "backup_manager",
    ) {
        composable("backup_manager") {
            BackupManagerScreen(
                onBackClick = {
                    fragmentNavController.popBackStack()
                },
                onRestoreBackup = {
//                    fragmentNavController.authorizedAction {
//                        fragmentNavController.slideFromRight(R.id.backupLocalFragment/*, BackupLocalFragment.prepareParams(account.id)*/)
//                    }

//                    navController.navigate("restore_backup")
                },
                onCreateBackup = {
                    fragmentNavController.authorizedAction {
                        fragmentNavController.slideFromRight(R.id.backupLocalFragment)
                    }
                }
            )
        }

        composablePage("restore_backup") {

        }

        composablePage("create_backup") {

        }

    }
}

@Composable
private fun BackupManagerScreen(
    onBackClick: () -> Unit,
    onRestoreBackup: () -> Unit,
    onCreateBackup: () -> Unit,
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.BackupManager_Title),
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                },
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Spacer(modifier = Modifier.height(12.dp))
            CellUniversalLawrenceSection(
                buildList {
                    add {
                        RowUniversal(onClick = onRestoreBackup) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(R.drawable.ic_download_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                            body_jacob(text = stringResource(R.string.BackupManager_RestoreBackup))
                        }
                    }

                    add {
                        RowUniversal(onClick = onCreateBackup) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                            body_jacob(text = stringResource(R.string.BackupManager_CreateBackup))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Composable
fun CreateBackupScreen() {

}

@Composable
fun RestoreBackupScreen() {

}


