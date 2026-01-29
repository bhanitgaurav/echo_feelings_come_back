package com.bhanit.apps.echo.features.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.ui.ShimmerEffect

@Composable
fun ProfileShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         Spacer(Modifier.height(40.dp))
         
         // Avatar
         ShimmerEffect(
             modifier = Modifier
                 .size(120.dp)
                 .clip(CircleShape)
         )
         
         Spacer(Modifier.height(24.dp))
         
         // Name & Email
         ShimmerEffect(modifier = Modifier.width(200.dp).height(24.dp).clip(RoundedCornerShape(4.dp)))
         Spacer(Modifier.height(8.dp))
         ShimmerEffect(modifier = Modifier.width(150.dp).height(16.dp).clip(RoundedCornerShape(4.dp)))
         
         Spacer(Modifier.height(40.dp))
         
         // Stats / Details Cards
         repeat(3) {
             EchoCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                 Row(
                     modifier = Modifier.padding(16.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     ShimmerEffect(modifier = Modifier.size(24.dp).clip(CircleShape))
                     Spacer(Modifier.width(16.dp))
                     Column {
                         ShimmerEffect(modifier = Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(4.dp)))
                     }
                 }
             }
         }
    }
}
