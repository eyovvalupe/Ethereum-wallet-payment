package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant

class BottomSheetSelectorMultipleDialog(
    private val title: String,
    private val subtitle: String,
    private val icon: ImageSource,
    private val items: List<BottomSheetSelectorViewItem>,
    private val selectedIndexes: List<Int>,
    private val onItemsSelected: (List<Int>) -> Unit,
    private val onCancelled: (() -> Unit)?,
    private val warning: String?,
    private val notifyUnchanged: Boolean
) : BaseComposableBottomSheetFragment() {

    private val viewModel by viewModels<BottomSheetSelectorMultipleViewModel> {
        BottomSheetSelectorMultipleModule.Factory(selectedIndexes)
    }

    @Composable
    override fun BottomSheetScreen() {
        BottomSheetHeader(
            iconPainter = icon.painter(),
            title = title,
            subtitle = subtitle
        ) {
            BottomSheetContent(viewModel.selectedItems)
        }
    }

    @Composable
    private fun BottomSheetContent(selected: List<Int>) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        warning?.let {
            Box(
                modifier = Modifier.padding(horizontal = 21.dp, vertical = 12.dp)
            ) {
                TextImportant(it)
            }
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
        }
        items.forEachIndexed { index, item ->
            CellMultilineLawrence(borderBottom = true) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (selected.contains(index)) {
                                viewModel.onUnchecked(index)
                            } else {
                                viewModel.onChecked(index)
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.title,
                            style = ComposeAppTheme.typography.body,
                            color = ComposeAppTheme.colors.leah
                        )
                        Text(
                            text = item.subtitle,
                            style = ComposeAppTheme.typography.subhead2,
                            color = ComposeAppTheme.colors.grey
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    HsSwitch(
                        modifier = Modifier.padding(start = 5.dp),
                        checked = selected.contains(index),
                        onCheckedChange = { checked ->
                            if (checked) {
                                viewModel.onChecked(index)
                            } else {
                                viewModel.onUnchecked(index)
                            }
                        },
                    )
                }
            }
        }
        ButtonPrimaryYellow(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            title = getString(R.string.Button_Done),
            onClick = {
                if (notifyUnchanged || !equals(selectedIndexes, viewModel.selectedItems)) {
                    onItemsSelected(viewModel.selectedItems)
                }
                dismiss()
            },
            enabled = selected.isNotEmpty()
        )
    }

    private fun equals(list1: List<Int>, list2: List<Int>): Boolean {
        return (list1 - list2).isEmpty() && (list2 - list1).isEmpty()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.invoke()
    }

    override fun close() {
        super.close()
        onCancelled?.invoke()
    }

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            title: String,
            subtitle: String,
            icon: ImageSource,
            items: List<BottomSheetSelectorViewItem>,
            selected: List<Int>,
            onItemSelected: (List<Int>) -> Unit,
            onCancelled: (() -> Unit)? = null,
            warning: String? = null,
            notifyUnchanged: Boolean = false
        ) {
            BottomSheetSelectorMultipleDialog(
                title,
                subtitle,
                icon,
                items,
                selected,
                onItemSelected,
                onCancelled,
                warning,
                notifyUnchanged
            )
                .show(fragmentManager, "selector_dialog")
        }
    }

    data class Config(
        val icon: ImageSource,
        val title: String,
        val subtitle: String,
        val selectedIndexes: List<Int>,
        val viewItems: List<BottomSheetSelectorViewItem>,
        val description: String
    )
}

class BottomSheetSelectorMultipleViewModel(selectedIndexes: List<Int>) : ViewModel() {
    val selectedItems = mutableStateListOf<Int>().apply {
        addAll(selectedIndexes)
    }

    fun onChecked(index: Int) {
        selectedItems.add(index)
    }

    fun onUnchecked(index: Int) {
        selectedItems.remove(index)
    }
}

object BottomSheetSelectorMultipleModule {
    class Factory(private val selectedIndexes: List<Int>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BottomSheetSelectorMultipleViewModel(selectedIndexes) as T
        }
    }
}
