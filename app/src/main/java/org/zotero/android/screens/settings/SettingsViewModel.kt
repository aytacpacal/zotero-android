package org.zotero.android.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val defaults: Defaults,
) : BaseViewModel2<SettingsViewState, SettingsViewEffect>(SettingsViewState()) {

    fun init() = initOnce {
        updateState {
            copy(customDirectoryUri = defaults.getCustomAttachmentDirectoryUri())
        }
    }

    fun onDone() {
        triggerEffect(SettingsViewEffect.OnBack)
    }

    fun openPrivacyPolicy() {
        val uri = "https://www.zotero.org/support/privacy?app=1"
        triggerEffect(SettingsViewEffect.OpenWebpage(uri))
    }

    fun openSupportAndFeedback() {
        val uri = "https://forums.zotero.org/"
        triggerEffect(SettingsViewEffect.OpenWebpage(uri))
    }

    fun onCustomDirectorySelected(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val uriString = uri.toString()
        defaults.setCustomAttachmentDirectoryUri(uriString)
        updateState { copy(customDirectoryUri = uriString) }
    }

    fun onClearCustomDirectory() {
        defaults.setCustomAttachmentDirectoryUri(null)
        updateState { copy(customDirectoryUri = null) }
    }
}

internal data class SettingsViewState(
    val placeholder: String = "",
    val customDirectoryUri: String? = null,
) : ViewState

internal sealed class SettingsViewEffect : ViewEffect {
    object OnBack : SettingsViewEffect()
    data class OpenWebpage(val url: String) : SettingsViewEffect()
}
