package app.kreate.android.themed.common.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.kreate.android.R
import app.kreate.android.utils.ProxyEntry
import app.kreate.android.utils.ProxyManager
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import it.fast4x.rimusic.ui.components.themed.DialogTextButton
import it.fast4x.rimusic.ui.components.themed.textFieldColors
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Proxy

/**
 * Add/remove/reorder [ProxyEntry] list, modeled on the shape of
 * [it.fast4x.rimusic.ui.components.themed.StringListDialog] but for structured entries: each
 * row also shows a reachability dot (refreshed via [ProxyManager.probeAll] whenever the list
 * changes) and marks the entry [ProxyManager] is currently sticking to.
 */
@Composable
fun ProxyListDialog( onDismiss: () -> Unit ) {
    var entries by remember { mutableStateOf( ProxyManager.list() ) }
    var reachability by remember { mutableStateOf<Map<ProxyEntry, Boolean>>( emptyMap() ) }
    var showAddDialog by remember { mutableStateOf( false ) }
    var removingEntry by remember { mutableStateOf<ProxyEntry?>( null ) }

    LaunchedEffect( entries ) {
        reachability = withContext( Dispatchers.IO ) { ProxyManager.probeAll() }
    }

    fun reorder( from: Int, to: Int ) {
        val updated = entries.toMutableList().apply { add( to, removeAt( from ) ) }
        ProxyManager.setList( updated )
        entries = updated
    }

    Dialog( onDismissRequest = onDismiss ) {
        Column(
            modifier = Modifier
                .padding( all = 10.dp )
                .background( color = colorPalette().background1, shape = RoundedCornerShape(8.dp) )
                .padding( vertical = 16.dp )
                .defaultMinSize( Dp.Unspecified, 190.dp )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicText(
                    text = stringResource( R.string.proxy_list ),
                    style = typography().s.semiBold,
                    modifier = Modifier.padding( vertical = 8.dp, horizontal = 24.dp )
                )
                DialogTextButton(
                    text = stringResource( R.string.add_proxy ),
                    primary = true,
                    onClick = { showAddDialog = true }
                )
            }

            Spacer( modifier = Modifier.height(5.dp) )

            if( entries.isEmpty() )
                BasicText(
                    text = stringResource( R.string.no_proxies_configured ),
                    style = typography().xs.medium.color( colorPalette().textDisabled ),
                    modifier = Modifier.padding( horizontal = 24.dp, vertical = 8.dp )
                )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEachIndexed { index, entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding( horizontal = 24.dp, vertical = 4.dp )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip( CircleShape )
                                .background(
                                    when( reachability[entry] ) {
                                        true  -> Color(0xFF4CAF50)
                                        false -> Color(0xFFF44336)
                                        null  -> colorPalette().textDisabled
                                    }
                                )
                        )

                        BasicText(
                            text = entry.toString(),
                            style =
                                if( index == ProxyManager.activeIndex() )
                                    typography().xs.semiBold
                                else
                                    typography().xs.medium,
                            modifier = Modifier.weight(1f)
                        )

                        if( index == ProxyManager.activeIndex() )
                            Icon(
                                painter = painterResource( R.drawable.checked_filled ),
                                contentDescription = null,
                                tint = colorPalette().accent,
                                modifier = Modifier.size(16.dp)
                            )

                        if( index > 0 )
                            Icon(
                                painter = painterResource( R.drawable.chevron_up ),
                                contentDescription = null,
                                tint = colorPalette().text,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { reorder( index, index - 1 ) }
                            )
                        if( index < entries.lastIndex )
                            Icon(
                                painter = painterResource( R.drawable.chevron_down ),
                                contentDescription = null,
                                tint = colorPalette().text,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { reorder( index, index + 1 ) }
                            )

                        Icon(
                            painter = painterResource( R.drawable.trash ),
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { removingEntry = entry }
                        )
                    }
                }
            }
        }
    }

    if( showAddDialog )
        AddProxyDialog(
            onDismiss = { showAddDialog = false },
            onAdd = {
                ProxyManager.addEntry( it )
                entries = ProxyManager.list()
                showAddDialog = false
            }
        )

    removingEntry?.let { entry ->
        ConfirmationDialog(
            text = stringResource( R.string.remove_proxy_confirmation ),
            onDismiss = { removingEntry = null },
            onConfirm = {
                ProxyManager.removeEntry( entry )
                entries = ProxyManager.list()
                removingEntry = null
            }
        )
    }
}

@Composable
private fun AddProxyDialog(
    onDismiss: () -> Unit,
    onAdd: (ProxyEntry) -> Unit
) {
    var scheme by remember { mutableStateOf( Proxy.Type.HTTP ) }
    var host by remember { mutableStateOf( "" ) }
    var port by remember { mutableStateOf( "" ) }
    var error by remember { mutableStateOf( "" ) }
    val valueCannotBeEmpty = stringResource( R.string.value_cannot_be_empty )

    Dialog( onDismissRequest = onDismiss ) {
        Column(
            modifier = Modifier
                .padding( all = 10.dp )
                .background( color = colorPalette().background1, shape = RoundedCornerShape(8.dp) )
                .padding( vertical = 16.dp, horizontal = 24.dp )
        ) {
            BasicText(
                text = stringResource( R.string.add_proxy ),
                style = typography().s.semiBold,
                modifier = Modifier.padding( vertical = 8.dp )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding( vertical = 8.dp )
            ) {
                listOf( Proxy.Type.HTTP, Proxy.Type.SOCKS ).forEach { type ->
                    val selected = scheme == type
                    BasicText(
                        text = type.name,
                        style =
                            if( selected ) typography().xs.semiBold.color( colorPalette().onAccent )
                            else typography().xs.medium,
                        modifier = Modifier
                            .clip( RoundedCornerShape(36.dp) )
                            .background( if( selected ) colorPalette().accent else Color.Transparent )
                            .clickable { scheme = type }
                            .padding( horizontal = 16.dp, vertical = 8.dp )
                    )
                }
            }

            TextField(
                value = host,
                onValueChange = { host = it },
                placeholder = { Text( stringResource( R.string.proxy_host ) ) },
                colors = textFieldColors( colorPalette(), error ),
                keyboardOptions = KeyboardOptions( keyboardType = KeyboardType.Uri ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer( modifier = Modifier.height(8.dp) )

            TextField(
                value = port,
                onValueChange = { port = it.filter( Char::isDigit ) },
                placeholder = { Text( stringResource( R.string.proxy_port ) ) },
                colors = textFieldColors( colorPalette(), error ),
                keyboardOptions = KeyboardOptions( keyboardType = KeyboardType.Number ),
                modifier = Modifier.fillMaxWidth()
            )

            if( error.isNotEmpty() )
                BasicText(
                    text = error,
                    style = typography().xs.medium.color( Color.Red ),
                    modifier = Modifier.padding( top = 8.dp )
                )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding( top = 8.dp )
            ) {
                DialogTextButton(
                    text = stringResource( android.R.string.cancel ),
                    onClick = onDismiss
                )
                DialogTextButton(
                    text = stringResource( R.string.confirm ),
                    primary = true,
                    onClick = {
                        val portInt = port.toIntOrNull()
                        when {
                            host.isBlank()      -> error = valueCannotBeEmpty
                            portInt == null     -> error = valueCannotBeEmpty
                            else                -> onAdd( ProxyEntry( scheme.name, host, portInt ) )
                        }
                    }
                )
            }
        }
    }
}
