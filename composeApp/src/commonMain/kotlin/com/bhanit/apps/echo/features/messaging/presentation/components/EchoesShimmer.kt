package com.bhanit.apps.echo.features.messaging.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
fun EchoesShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            EchoCard(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerEffect(modifier = Modifier.size(12.dp).clip(CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            ShimmerEffect(modifier = Modifier.width(100.dp).height(16.dp).clip(RoundedCornerShape(4.dp)))
                            Spacer(Modifier.height(4.dp))
                            ShimmerEffect(modifier = Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                        }
                        Spacer(Modifier.weight(1f))
                        ShimmerEffect(modifier = Modifier.width(40.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                    }
                    Spacer(Modifier.height(16.dp))
                    ShimmerEffect(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}
